from decimal import Decimal
from typing import Optional
from fastapi import APIRouter, Depends, Query, status
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session
from ..clients.admission import verify_class_and_teacher
from ..clients.student import resolve_student_id_by_user_id, verify_student_studying
from ..database import get_db
from ..deps import AppError, CurrentUser, get_current_user, require_role
from ..models import Grade
from ..schemas import GradeCreate, GradeResponse, GradeUpdate

router = APIRouter(prefix="/api/learning/grades", tags=["Grades (LRN-02)"])


@router.get("")
def list_grades(
    class_id: Optional[int] = Query(default=None, gt=0),
    student_id: Optional[int] = Query(default=None, gt=0),
    assessment_name: Optional[str] = Query(default=None),
    page: int = Query(default=1, ge=1),
    size: int = Query(default=20, ge=1, le=100),
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(get_current_user),
):
    query = db.query(Grade)

    # INV-LRN-02: STUDENT chỉ xem điểm của chính mình; ánh xạ user_id -> student.students.id (R-ID-1)
    if current_user.role == "STUDENT":
        own_student_id = resolve_student_id_by_user_id(current_user)
        if student_id is not None and own_student_id is not None and student_id != own_student_id:
            raise AppError(
                status.HTTP_403_FORBIDDEN,
                "LRN_FORBIDDEN_STUDENT_GRADES",
                "Học viên không được phép truy cập bảng điểm của học viên khác (INV-LRN-02)",
            )
        if own_student_id is None:
            if student_id is not None:
                raise AppError(
                    status.HTTP_403_FORBIDDEN,
                    "LRN_FORBIDDEN_STUDENT_GRADES",
                    "Học viên không được phép truy cập bảng điểm của học viên khác (INV-LRN-02)",
                )
            # UC-LRN-02.2: Nếu user chưa được sync sang Student Service -> trả danh sách rỗng
            return {
                "success": True,
                "data": {
                    "items": [],
                    "total": 0,
                    "page": page,
                    "size": size,
                    "total_pages": 0,
                },
                "message": "Lấy danh sách điểm thành công",
            }
        query = query.filter(Grade.student_id == own_student_id)
    elif student_id is not None:
        query = query.filter(Grade.student_id == student_id)

    if class_id is not None:
        query = query.filter(Grade.class_id == class_id)
    if assessment_name and assessment_name.strip():
        query = query.filter(
            Grade.assessment_name.ilike(f"%{assessment_name.strip()}%")
        )

    total = query.count()
    items = (
        query.order_by(Grade.graded_at.desc(), Grade.id.desc())
        .offset((page - 1) * size)
        .limit(size)
        .all()
    )
    total_pages = (total + size - 1) // size if total > 0 else 0

    return {
        "success": True,
        "data": {
            "items": [
                GradeResponse.model_validate(g).model_dump(mode="json") for g in items
            ],
            "total": total,
            "page": page,
            "size": size,
            "total_pages": total_pages,
        },
        "message": "Lấy danh sách điểm thành công",
    }


@router.post("", status_code=status.HTTP_201_CREATED)
def create_grade(
    payload: GradeCreate,
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(require_role("ADMIN", "STAFF", "TEACHER")),
):
    # 1. Kiểm tra INV-LRN-01 & weight
    if payload.max_score <= 0 or payload.score < 0 or payload.score > payload.max_score:
        raise AppError(
            status.HTTP_400_BAD_REQUEST,
            "LRN_INVALID_SCORE",
            "Điểm số phải thỏa mãn 0 <= score <= max_score và max_score > 0 (INV-LRN-01)",
        )
    if payload.weight is not None and (payload.weight <= 0 or payload.weight > 1):
        raise AppError(
            status.HTTP_400_BAD_REQUEST,
            "LRN_INVALID_WEIGHT",
            "Trọng số weight phải nằm trong khoảng (0, 1]",
        )

    # 2. Xác minh lớp tồn tại & nếu role == TEACHER phải dạy lớp này (INV-LRN-03 / INV-SEC-02)
    verify_class_and_teacher(payload.class_id, current_user, forbid_completed=True)

    # 3. Xác minh học viên tồn tại và đang STUDYING bên Student Service
    verify_student_studying(payload.student_id)

    # 4. Kiểm tra trùng đầu điểm (INV-LRN-06)
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

    new_grade = Grade(
        class_id=payload.class_id,
        student_id=payload.student_id,
        assessment_name=payload.assessment_name,
        score=payload.score,
        max_score=payload.max_score,
        weight=payload.weight,
        feedback=payload.feedback,
        graded_by=current_user.user_id,
    )
    db.add(new_grade)
    try:
        db.commit()
    except IntegrityError:
        db.rollback()
        raise AppError(
            status.HTTP_409_CONFLICT,
            "LRN_DUPLICATE_ASSESSMENT",
            f"Đầu điểm '{payload.assessment_name}' đã tồn tại cho học viên trong lớp này (INV-LRN-06)",
        )
    db.refresh(new_grade)

    return {
        "success": True,
        "data": GradeResponse.model_validate(new_grade).model_dump(mode="json"),
        "message": "Nhập điểm đánh giá thành công",
    }


@router.put("/{grade_id}")
def update_grade(
    grade_id: int,
    payload: GradeUpdate,
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(require_role("ADMIN", "STAFF", "TEACHER")),
):
    # 1. Tìm bản ghi theo grade_id (UC-LRN-02.3)
    grade = db.query(Grade).filter(Grade.id == grade_id).first()
    if not grade:
        raise AppError(
            status.HTTP_404_NOT_FOUND,
            "LRN_GRADE_NOT_FOUND",
            f"Không tìm thấy bản ghi điểm theo ID {grade_id}",
        )

    # 2. Kiểm tra INV-LRN-01 & weight
    target_max = (
        payload.max_score
        if payload.max_score is not None
        else Decimal(str(grade.max_score))
    )
    if target_max <= 0 or payload.score < 0 or payload.score > target_max:
        raise AppError(
            status.HTTP_400_BAD_REQUEST,
            "LRN_INVALID_SCORE",
            "Điểm số phải thỏa mãn 0 <= score <= max_score và max_score > 0 (INV-LRN-01)",
        )
    if payload.weight is not None and (payload.weight <= 0 or payload.weight > 1):
        raise AppError(
            status.HTTP_400_BAD_REQUEST,
            "LRN_INVALID_WEIGHT",
            "Trọng số weight phải nằm trong khoảng (0, 1]",
        )

    # 3. Xác minh lớp & giáo viên phụ trách lớp (INV-LRN-03)
    verify_class_and_teacher(grade.class_id, current_user, forbid_completed=True)

    # 4. Cập nhật bản ghi trong transaction
    grade.score = payload.score
    grade.max_score = target_max
    if "weight" in payload.model_fields_set:
        grade.weight = payload.weight
    if "feedback" in payload.model_fields_set:
        grade.feedback = payload.feedback
    grade.graded_by = current_user.user_id

    try:
        db.commit()
    except IntegrityError:
        db.rollback()
        raise AppError(
            status.HTTP_400_BAD_REQUEST,
            "LRN_INVALID_SCORE",
            "Dữ liệu cập nhật vi phạm ràng buộc điểm số",
        )
    db.refresh(grade)

    return {
        "success": True,
        "data": GradeResponse.model_validate(grade).model_dump(mode="json"),
        "message": "Cập nhật điểm đánh giá thành công",
    }