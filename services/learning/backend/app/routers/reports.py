from typing import Optional
from fastapi import APIRouter, Depends, Query, status
from sqlalchemy import bindparam, text
from sqlalchemy.orm import Session
from ..clients.admission import get_teacher_class_ids, verify_class_and_teacher
from ..clients.student import resolve_student_id_by_user_id
from ..database import get_db
from ..deps import AppError, CurrentUser, get_current_user, require_role

router = APIRouter(prefix="/api/learning/reports", tags=["Academic Reports (LRN-03)"])


@router.get("/class-summary")
def get_class_summary_report(
    class_id: int = Query(..., gt=0),
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(require_role("ADMIN", "STAFF", "TEACHER")),
):
    """
    Lấy báo cáo tổng kết tỷ lệ Pass/Fail của lớp từ learning.v_class_pass_rate
    kèm danh sách điểm trung bình chuẩn hóa thang 100% từ learning.v_student_grade_summary.

    - Nếu TEACHER không phụ trách class_id -> 403 LRN_FORBIDDEN_CLASS.
    """
    if current_user.role == "TEACHER":
        verify_class_and_teacher(class_id, current_user)

    pass_rate_row = (
        db.execute(
            text(
                """
                SELECT class_id, total_students, pass_count, fail_count, pass_rate_percentage
                FROM learning.v_class_pass_rate
                WHERE class_id = :class_id
                """
            ),
            {"class_id": class_id},
        )
        .mappings()
        .first()
    )

    student_rows = (
        db.execute(
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
        )
        .mappings()
        .all()
    )

    student_summaries = [
        {
            "class_id": int(r["class_id"]),
            "student_id": int(r["student_id"]),
            "total_assessments": int(r["total_assessments"]),
            "weighted_avg_score": float(r["weighted_avg_score"] or 0.0),
            "average_percentage": float(r["average_percentage"] or 0.0),
            "result": str(r["result"]),
        }
        for r in student_rows
    ]

    report_data = {
        "class_id": class_id,
        "total_students": int(pass_rate_row["total_students"]) if pass_rate_row else 0,
        "pass_count": int(pass_rate_row["pass_count"]) if pass_rate_row else 0,
        "fail_count": int(pass_rate_row["fail_count"]) if pass_rate_row else 0,
        "pass_rate_percentage": (
            float(pass_rate_row["pass_rate_percentage"]) if pass_rate_row else 0.0
        ),
        "student_summaries": student_summaries,
    }

    return {
        "success": True,
        "data": report_data,
        "message": "Lấy báo cáo tổng kết lớp học thành công",
    }


@router.get("/student-summary")
def get_student_summary_report(
    class_id: Optional[int] = Query(default=None, gt=0),
    student_id: Optional[int] = Query(default=None, gt=0),
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(get_current_user),
):
    """
    Lấy kết quả tổng kết chuẩn hóa thang 100% theo học viên và/hoặc lớp.

    - STUDENT: Chỉ xem kết quả của chính mình (INV-LRN-02).
    - TEACHER: Chỉ xem kết quả trong các lớp mình phụ trách (INV-SEC-02).
    """
    allowed_teacher_classes: Optional[list[int]] = None

    # Bước 1: Kiểm tra phân quyền cô lập dữ liệu theo vai trò
    if current_user.role == "STUDENT":
        own_student_id = resolve_student_id_by_user_id(current_user)
        if student_id is not None and own_student_id is not None and student_id != own_student_id:
            raise AppError(
                status.HTTP_403_FORBIDDEN,
                "LRN_FORBIDDEN_STUDENT_GRADES",
                "Học viên không được phép truy cập kết quả học tập của học viên khác (INV-LRN-02)",
            )
        if own_student_id is None:
            if student_id is not None:
                raise AppError(
                    status.HTTP_403_FORBIDDEN,
                    "LRN_FORBIDDEN_STUDENT_GRADES",
                    "Học viên không được phép truy cập kết quả học tập của học viên khác (INV-LRN-02)",
                )
            return {
                "success": True,
                "data": [],
                "message": "Lấy kết quả tổng kết học viên thành công",
            }
        target_student_id = own_student_id
    else:
        if current_user.role == "TEACHER":
            if class_id is not None:
                verify_class_and_teacher(class_id, current_user)
            else:
                teacher_class_ids = get_teacher_class_ids(current_user.user_id)
                if teacher_class_ids is not None:
                    if not teacher_class_ids:
                        return {
                            "success": True,
                            "data": [],
                            "message": "Lấy kết quả tổng kết học viên thành công",
                        }
                    allowed_teacher_classes = sorted(teacher_class_ids)
        target_student_id = student_id

    # Bước 2: Truy vấn view learning.v_student_grade_summary với tham số ràng buộc an toàn
    sql = """
        SELECT class_id, student_id, total_assessments,
                weighted_avg_score, average_percentage, result
        FROM learning.v_student_grade_summary
        WHERE 1=1
    """
    params: dict = {}
    if target_student_id is not None:
        sql += " AND student_id = :student_id"
        params["student_id"] = target_student_id
    if class_id is not None:
        sql += " AND class_id = :class_id"
        params["class_id"] = class_id
    elif allowed_teacher_classes is not None:
        sql += " AND class_id IN :teacher_classes"
        params["teacher_classes"] = allowed_teacher_classes

    sql += " ORDER BY class_id ASC, student_id ASC"
    stmt = text(sql)
    if allowed_teacher_classes is not None:
        stmt = stmt.bindparams(bindparam("teacher_classes", expanding=True))

    rows = db.execute(stmt, params).mappings().all()

    summaries = [
        {
            "class_id": int(r["class_id"]),
            "student_id": int(r["student_id"]),
            "total_assessments": int(r["total_assessments"]),
            "weighted_avg_score": float(r["weighted_avg_score"] or 0.0),
            "average_percentage": float(r["average_percentage"] or 0.0),
            "result": str(r["result"]),
        }
        for r in rows
    ]

    return {
        "success": True,
        "data": summaries,
        "message": "Lấy kết quả tổng kết học viên thành công",
    }
