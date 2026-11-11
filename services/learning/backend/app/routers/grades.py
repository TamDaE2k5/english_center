from decimal import Decimal
from typing import Optional
from fastapi import APIRouter, Depends, Query, status
from sqlalchemy import text
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session
from ..database import get_db
from ..internal_client import (
    resolve_student_id_for_user,
    verify_class_and_teacher,
    verify_student_studying,
)
from ..models import Grade
from ..schemas import GradeCreate, GradeResponse, GradeUpdate
from ..security import AppError, CurrentUser, get_current_user, require_roles

router = APIRouter(prefix="/api/learning", tags=["Grades & Academic Reports (LRN-02, LRN-03)"])


@router.get("/grades")
def list_grades(
    class_id: Optional[int] = Query(default=None, gt=0),
    student_id: Optional[int] = Query(default=None, gt=0),
    assessment_name: Optional[str] = Query(default=None),
    page: int = Query(default=1, ge=1),
    size: int = Query(default=50, ge=1, le=100),
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(get_current_user),
):
    query = db.query(Grade)

    # INV-LRN-02: Học viên chỉ được xem bảng điểm của chính mình
    if current_user.role == "STUDENT":
        own_student_id = resolve_student_id_for_user(current_user)
        if student_id is not None and student_id != own_student_id:
            raise AppError(
                status.HTTP_403_FORBIDDEN,
                "LRN_FORBIDDEN_STUDENT_GRADES",
                "Học viên không được phép xem bảng điểm của học viên khác (INV-LRN-02)",
            )
        query = query.filter(Grade.student_id == own_student_id)
    elif student_id is not None:
        query = query.filter(Grade.student_id == student_id)

    if class_id is not None:
        query = query.filter(Grade.class_id == class_id)
    if assessment_name:
        query = query.filter(Grade.assessment_name.ilike(f"%{assessment_name.strip()}%"))

    total_items = query.count()
    items = (
        query.order_by(Grade.class_id.asc(), Grade.student_id.asc(), Grade.graded_at.asc())
        .offset((page - 1) * size)
        .limit(size)
        .all()
    )
    total_pages = (total_items + size - 1) // size if total_items > 0 else 1

    return {
        "success": True,
        "data": [GradeResponse.model_validate(g).model_dump(mode="json") for g in items],
        "pagination": {
            "page": page,
            "size": size,
            "total_items": total_items,
            "total_pages": total_pages,
        },
        "message": "Lấy danh sách điểm thành công",
    }


@router.post("/grades", status_code=status.HTTP_201_CREATED)
def create_grade(
    payload: GradeCreate,
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(require_roles("ADMIN", "STAFF", "TEACHER")),
):
    # 1. Kiểm tra giáo viên có đúng dạy lớp này không (INV-LRN-03 / INV-SEC-02)
    verify_class_and_teacher(payload.class_id, current_user)

    # 2. Kiểm tra học viên tồn tại và đang STUDYING bên Student Service
    verify_student_studying(payload.student_id)

    # 3. Kiểm tra trùng đầu điểm (INV-LRN-06)
    existing = (
        db.query(Grade)
        .filter(
            Grade.class_id == payload.class_id,
            Grade.student_id == payload.student_id,
            Grade.assessment_name == payload.assessment_name,
        )
        .first()
    )
    if existing:
        raise AppError(
            status.HTTP_409_CONFLICT,
            "LRN_DUPLICATE_ASSESSMENT",
            f"Đầu điểm '{payload.assessment_name}' đã tồn tại cho học viên {payload.student_id} trong lớp {payload.class_id} (INV-LRN-06)",
        )

    grade = Grade(
        class_id=payload.class_id,
        student_id=payload.student_id,
        assessment_name=payload.assessment_name,
        score=payload.score,
        max_score=payload.max_score,
        weight=payload.weight if payload.weight is not None else Decimal("1.0"),
        feedback=payload.feedback,
        graded_by=current_user.user_id,
    )
    db.add(grade)
    try:
        db.commit()
    except IntegrityError:
        db.rollback()
        raise AppError(
            status.HTTP_409_CONFLICT,
            "LRN_DUPLICATE_ASSESSMENT",
            "Đầu điểm đã tồn tại hoặc vi phạm ràng buộc dữ liệu",
        )
    db.refresh(grade)

    return {
        "success": True,
        "data": GradeResponse.model_validate(grade).model_dump(mode="json"),
        "message": "Nhập điểm đánh giá thành công",
    }


@router.put("/grades/{grade_id}")
def update_grade(
    grade_id: int,
    payload: GradeUpdate,
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(require_roles("ADMIN", "STAFF", "TEACHER")),
):
    grade = db.query(Grade).filter(Grade.id == grade_id).first()
    if not grade:
        raise AppError(
            status.HTTP_404_NOT_FOUND,
            "LRN_GRADE_NOT_FOUND",
            f"Không tìm thấy bản ghi điểm với ID {grade_id}",
        )

    verify_class_and_teacher(grade.class_id, current_user)

    new_max = payload.max_score if payload.max_score is not None else Decimal(str(grade.max_score))
    if new_max <= 0 or payload.score < 0 or payload.score > new_max:
        raise AppError(
            status.HTTP_400_BAD_REQUEST,
            "LRN_INVALID_SCORE",
            "Điểm số phải thỏa mãn 0 <= score <= max_score (INV-LRN-01)",
        )
    if payload.weight is not None and (payload.weight <= 0 or payload.weight > 1):
        raise AppError(
            status.HTTP_400_BAD_REQUEST,
            "LRN_INVALID_WEIGHT",
            "Trọng số weight phải nằm trong khoảng (0, 1]",
        )

    grade.score = payload.score
    grade.max_score = new_max
    if payload.weight is not None:
        grade.weight = payload.weight
    if payload.feedback is not None:
        grade.feedback = payload.feedback
    grade.graded_by = current_user.user_id

    db.commit()
    db.refresh(grade)

    return {
        "success": True,
        "data": GradeResponse.model_validate(grade).model_dump(mode="json"),
        "message": "Cập nhật điểm đánh giá thành công",
    }


@router.get("/reports/class-summary")
def get_class_summary_report(
    class_id: int = Query(..., gt=0),
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(require_roles("ADMIN", "STAFF", "TEACHER")),
):
    if current_user.role == "TEACHER":
        verify_class_and_teacher(class_id, current_user)

    pass_rate_row = db.execute(
        text(
            """
            SELECT class_id, total_students, pass_count, fail_count, pass_rate_percentage
            FROM learning.v_class_pass_rate
            WHERE class_id = :class_id
            """
        ),
        {"class_id": class_id},
    ).mappings().first()

    student_rows = db.execute(
        text(
            """
            SELECT class_id, student_id, total_assessments,
                   weighted_avg_score, average_percentage, result
            FROM learning.v_student_grade_summary
            WHERE class_id = :class_id
            ORDER BY average_percentage DESC, student_id ASC
            """
        ),
        {"class_id": class_id},
    ).mappings().all()

    summaries = [
        {
            "class_id": int(r["class_id"]),
            "student_id": int(r["student_id"]),
            "total_assessments": int(r["total_assessments"]),
            "weighted_avg_score": float(r["weighted_avg_score"] or 0),
            "average_percentage": float(r["average_percentage"] or 0),
            "result": r["result"],
        }
        for r in student_rows
    ]

    report_data = {
        "class_id": class_id,
        "total_students": int(pass_rate_row["total_students"]) if pass_rate_row else 0,
        "pass_count": int(pass_rate_row["pass_count"]) if pass_rate_row else 0,
        "fail_count": int(pass_rate_row["fail_count"]) if pass_rate_row else 0,
        "pass_rate_percentage": float(pass_rate_row["pass_rate_percentage"]) if pass_rate_row else 0.0,
        "student_summaries": summaries,
    }

    return {
        "success": True,
        "data": report_data,
        "message": "Lấy báo cáo tổng kết lớp học thành công",
    }


@router.get("/reports/student-summary")
def get_student_summary_report(
    class_id: Optional[int] = Query(default=None, gt=0),
    student_id: Optional[int] = Query(default=None, gt=0),
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(get_current_user),
):
    if current_user.role == "STUDENT":
        target_student_id = resolve_student_id_for_user(current_user)
        if student_id is not None and student_id != target_student_id:
            raise AppError(
                status.HTTP_403_FORBIDDEN,
                "LRN_FORBIDDEN_STUDENT_GRADES",
                "Học viên chỉ được xem kết quả tổng kết của chính mình (INV-LRN-02)",
            )
    else:
        if student_id is None:
            raise AppError(
                status.HTTP_400_BAD_REQUEST,
                "LRN_MISSING_STUDENT_ID",
                "Vui lòng cung cấp student_id cần tra cứu",
            )
        target_student_id = student_id

    sql = """
        SELECT class_id, student_id, total_assessments,
               weighted_avg_score, average_percentage, result
        FROM learning.v_student_grade_summary
        WHERE student_id = :student_id
    """
    params = {"student_id": target_student_id}
    if class_id is not None:
        sql += " AND class_id = :class_id"
        params["class_id"] = class_id

    rows = db.execute(text(sql), params).mappings().all()
    data = [
        {
            "class_id": int(r["class_id"]),
            "student_id": int(r["student_id"]),
            "total_assessments": int(r["total_assessments"]),
            "weighted_avg_score": float(r["weighted_avg_score"] or 0),
            "average_percentage": float(r["average_percentage"] or 0),
            "result": r["result"],
        }
        for r in rows
    ]

    return {
        "success": True,
        "data": data,
        "message": "Lấy kết quả tổng kết học tập thành công",
    }