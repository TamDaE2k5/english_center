"""
routers/internal.py - Cac endpoint giao tiep noi bo giua cac microservice (/api/student/internal/*).
Bao ve bat buoc bang header X-Internal-Secret theo quy tac R-INT-1.
Phuc vu:
1. Admission Service: POST /internal/students (dong bo idempotent ho so khi enrollment APPROVED)
2. Learning Service: GET /internal/students/{id} va GET /internal/students?user_id=...
3. Finance Service: GET /internal/attendance/summary?student_id=...&class_id=...
"""

from typing import Optional

from fastapi import APIRouter, Depends, Query, status
from sqlalchemy import func
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from auth import verify_internal_secret
from database import get_db
from models import Attendance, Student
from schemas import InternalStudentSyncRequest, StudentResponse
from utils import error_response, generate_student_code, success_response

router = APIRouter(prefix="/internal", dependencies=[Depends(verify_internal_secret)])


def _serialize_internal_student(student: Student) -> dict:
    """
    Chuan hoa du lieu hoc vien cho ca Admission (can student_id) va Learning/Finance (can id/student_id).
    """
    data = StudentResponse.model_validate(student).model_dump()
    data["student_id"] = student.id
    return data


@router.post("/students", status_code=status.HTTP_201_CREATED)
def sync_student_profile(
    payload: InternalStudentSyncRequest,
    db: Session = Depends(get_db),
):
    """
    Dong bo ho so hoc vien tu Admission Service khi don dang ky duoc APPROVED.
    Idempotent: Neu ho so voi user_id hoac email da ton tai, tra ve HTTP 200 OK cung du lieu hien co.
    """
    # Buoc 1: Tim kiem ho so da ton tai theo user_id hoac email
    existing = None
    if payload.user_id is not None:
        existing = db.query(Student).filter(Student.user_id == payload.user_id).first()
    if existing is None:
        existing = db.query(Student).filter(Student.email == payload.email).first()

    if existing:
        # Bo sung user_id neu ban ghi cu tim theo email nhung chua gan user_id
        updated = False
        if existing.user_id is None and payload.user_id is not None:
            existing.user_id = payload.user_id
            updated = True
        if existing.phone is None and payload.phone is not None:
            existing.phone = payload.phone
            updated = True
        if existing.date_of_birth is None and payload.date_of_birth is not None:
            existing.date_of_birth = payload.date_of_birth
            updated = True
        if updated:
            db.commit()
            db.refresh(existing)

        return success_response(
            _serialize_internal_student(existing),
            "Ho so hoc vien da ton tai (Idempotent)",
            status.HTTP_200_OK,
        )

    # Buoc 2: Tao moi ho so voi ma HV-YYYY-NNNN tu sequence
    new_student = Student(
        user_id=payload.user_id,
        student_code=generate_student_code(db),
        full_name=payload.full_name,
        email=payload.email,
        phone=payload.phone,
        date_of_birth=payload.date_of_birth,
        address=payload.address,
        status="STUDYING",
    )

    try:
        db.add(new_student)
        db.commit()
        db.refresh(new_student)
        return success_response(
            _serialize_internal_student(new_student),
            "Tao ho so hoc vien thanh cong",
            status.HTTP_201_CREATED,
        )
    except IntegrityError:
        db.rollback()
        # Xu ly truong hop race condition khi 2 request dong thoi tao cung 1 hoc vien
        retry_existing = None
        if payload.user_id is not None:
            retry_existing = db.query(Student).filter(Student.user_id == payload.user_id).first()
        if retry_existing is None:
            retry_existing = db.query(Student).filter(Student.email == payload.email).first()
        if retry_existing:
            return success_response(
                _serialize_internal_student(retry_existing),
                "Ho so hoc vien da ton tai (Idempotent)",
                status.HTTP_200_OK,
            )
        return error_response(
            "STU_DATABASE_ERROR",
            "Khong the dong bo ho so hoc vien",
            status.HTTP_409_CONFLICT,
        )


@router.get("/students")
def internal_find_students(
    user_id: Optional[int] = Query(default=None, gt=0),
    email: Optional[str] = Query(default=None),
    db: Session = Depends(get_db),
):
    """
    Tra cuu ho so hoc vien noi bo theo user_id (R-ID-1) hoac email, hoac lay danh sach.
    """
    if user_id is not None:
        student = db.query(Student).filter(Student.user_id == user_id).first()
        if not student:
            return error_response(
                "STU_NOT_FOUND",
                f"Khong tim thay ho so hoc vien cho user_id={user_id}",
                status.HTTP_404_NOT_FOUND,
            )
        return success_response(_serialize_internal_student(student), "Tra cuu ho so hoc vien thanh cong")

    if email is not None:
        student = db.query(Student).filter(Student.email == email.strip()).first()
        if not student:
            return error_response(
                "STU_NOT_FOUND",
                f"Khong tim thay ho so hoc vien cho email={email}",
                status.HTTP_404_NOT_FOUND,
            )
        return success_response(_serialize_internal_student(student), "Tra cuu ho so hoc vien thanh cong")

    students = db.query(Student).order_by(Student.id.asc()).all()
    return success_response(
        [_serialize_internal_student(s) for s in students],
        "Lay danh sach ho so hoc vien noi bo thanh cong",
    )


@router.get("/students/{student_id}")
def internal_get_student(
    student_id: int,
    db: Session = Depends(get_db),
):
    """
    Lay thong tin ho so hoc vien noi bo theo student.students.id (Learning Service dung de kiem tra STUDYING).
    """
    student = db.query(Student).filter(Student.id == student_id).first()
    if not student:
        return error_response(
            "STU_NOT_FOUND",
            f"Hoc vien ID {student_id} khong ton tai",
            status.HTTP_404_NOT_FOUND,
        )
    return success_response(_serialize_internal_student(student), "Lay thong tin hoc vien noi bo thanh cong")


@router.get("/attendance/summary")
def internal_attendance_summary(
    student_id: int = Query(..., gt=0, description="student.students.id (hoac user_id fallback theo R-ID-1)"),
    class_id: int = Query(..., gt=0, description="ID lop hoc"),
    user_id: Optional[int] = Query(default=None, gt=0, description="admission.users.id neu caller truyen theo R-ID-1"),
    db: Session = Depends(get_db),
):
    """
    Thong ke so buoi da tham gia cua hoc vien trong mot lop hoc cho Finance Service xet hoan tien (INV-FIN-07).
    """
    # Buoc 1: Giai quyet student_id theo R-ID-1 (ho tro ca student.students.id lan admission.users.id)
    resolved_student = None
    if user_id is not None:
        resolved_student = db.query(Student).filter(Student.user_id == user_id).first()
    else:
        resolved_student = db.query(Student).filter(Student.id == student_id).first()
        if not resolved_student:
            resolved_student = db.query(Student).filter(Student.user_id == student_id).first()

    target_student_id = resolved_student.id if resolved_student else student_id

    # Buoc 2: Dem so buoi diem danh cua hoc vien trong lop
    records = (
        db.query(Attendance)
        .filter(
            Attendance.class_id == class_id,
            Attendance.student_id == target_student_id,
        )
        .all()
    )

    present_count = sum(1 for r in records if r.status == "PRESENT")
    late_count = sum(1 for r in records if r.status == "LATE")
    absent_count = sum(1 for r in records if r.status == "ABSENT")
    excused_count = sum(1 for r in records if r.status == "EXCUSED")
    attended_sessions = present_count + late_count

    # Tong so buoi da mo diem danh cua lop (so ngay duy nhat trong bang attendance cua lop)
    total_class_sessions = (
        db.query(func.count(func.distinct(Attendance.session_date)))
        .filter(Attendance.class_id == class_id)
        .scalar()
        or 0
    )

    return success_response(
        {
            "student_id": target_student_id,
            "class_id": class_id,
            "total_sessions": int(total_class_sessions),
            "attended_sessions": int(attended_sessions),
            "present_count": int(present_count),
            "late_count": int(late_count),
            "absent_count": int(absent_count),
            "excused_count": int(excused_count),
        },
        "Lay thong ke diem danh thanh cong",
    )
