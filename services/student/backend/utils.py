"""
utils.py - Cac ham tien ich dung chung cho Student Service: chuan hoa JSON response va sinh ma hoc vien.
"""

from datetime import datetime
from typing import Any

from fastapi.encoders import jsonable_encoder
from fastapi.responses import JSONResponse
from sqlalchemy import text
from sqlalchemy.orm import Session


def success_response(data: Any, message: str = "Thao tac thanh cong", status_code: int = 200) -> JSONResponse:
    """
    Dong goi phan hoi HTTP thanh cong theo chuan API Contract {success, data, message}.
    """
    return JSONResponse(
        status_code=status_code,
        content={
            "success": True,
            "data": jsonable_encoder(data),
            "message": message,
        },
    )


def error_response(code: str, message: str, status_code: int = 400) -> JSONResponse:
    """
    Dong goi phan hoi loi nghiep vu theo chuan API Contract {success: false, error: {code, message}}.
    """
    return JSONResponse(
        status_code=status_code,
        content={
            "success": False,
            "error": {
                "code": code,
                "message": message,
            },
        },
    )


def generate_student_code(db: Session) -> str:
    """
    Sinh ma hoc vien duy nhat dang HV-YYYY-NNNN bang sequence PostgreSQL student.student_code_seq (INV-STU-05).
    Dam bao khong bao gio trung lap khi tao nhieu hoc vien dong thoi.
    """
    current_year = datetime.now().year
    seq_val = db.execute(text("SELECT nextval('student.student_code_seq')")).scalar()
    return f"HV-{current_year}-{int(seq_val):04d}"
