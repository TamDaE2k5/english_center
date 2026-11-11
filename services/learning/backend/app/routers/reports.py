from typing import Optional
from fastapi import APIRouter, Depends, Query, status
from sqlalchemy import text
from sqlalchemy.orm import Session
from ..clients.admission import verify_class_and_teacher
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
    # INV-LRN-02: Nếu là STUDENT, chỉ được xem kết quả tổng kết của chính mình
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
        if current_user.role == "TEACHER" and class_id is not None:
            verify_class_and_teacher(class_id, current_user)
        target_student_id = student_id

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

    sql += " ORDER BY class_id ASC, student_id ASC"
    rows = db.execute(text(sql), params).mappings().all()

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