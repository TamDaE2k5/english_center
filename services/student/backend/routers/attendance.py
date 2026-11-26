"""
routers/attendance.py - Cac endpoint quan ly diem danh theo buoi hoc.
Tuan thu INV-STU-01 (diem danh don nhat moi buoi/lop), INV-SEC-01 va INV-SEC-02.
"""

from datetime import date
from typing import Optional

from fastapi import APIRouter, Depends, Query, status
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from auth import CurrentUser, StudentException, require_roles
from clients import get_class_info, get_teacher_classes
from database import get_db
from models import Attendance, Student
from schemas import AttendanceCreate, AttendanceResponse, AttendanceUpdate
from utils import error_response, success_response

router = APIRouter()


@router.get("/attendance")
def get_attendance(
    class_id: Optional[int] = Query(default=None, gt=0, description="Loc theo ID lop hoc"),
    student_id: Optional[int] = Query(default=None, gt=0, description="Loc theo ID hoc vien"),
    session_date: Optional[date] = Query(default=None, description="Loc theo ngay hoc (YYYY-MM-DD)"),
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(require_roles("ADMIN", "STAFF", "TEACHER", "STUDENT")),
):
    """
    Lay lich su diem danh theo vai tro:
    - ADMIN, STAFF: Xem tat ca va loc tuy chon theo class_id, student_id, session_date.
    - TEACHER: Chi xem diem danh cac lop minh duoc phan cong giang day.
    - STUDENT: Chi xem diem danh cua chinh minh (INV-SEC-01).
    """
    query = db.query(Attendance, Student.full_name, Student.student_code).join(
        Student, Attendance.student_id == Student.id
    )

    # Buoc 1: Ap dung co lap du lieu theo vai tro
    if current_user.role == "STUDENT":
        own_student = db.query(Student).filter(Student.user_id == current_user.user_id).first()
        if not own_student:
            return success_response([], "Lay lich su diem danh thanh cong")
        if student_id is not None and student_id != own_student.id:
            raise StudentException(
                status_code=status.HTTP_403_FORBIDDEN,
                code="STU_FORBIDDEN",
                message="Hoc vien khong duoc xem lich su diem danh cua hoc vien khac",
            )
        query = query.filter(Attendance.student_id == own_student.id)

    elif current_user.role == "TEACHER":
        if class_id is not None:
            class_info = get_class_info(class_id)
            teacher_id = class_info.get("teacher_id")
            if teacher_id is None or int(teacher_id) != current_user.user_id:
                raise StudentException(
                    status_code=status.HTTP_403_FORBIDDEN,
                    code="STU_FORBIDDEN_CLASS",
                    message="Giao vien khong duoc phan cong phu trach lop hoc nay",
                )
        else:
            teacher_classes = get_teacher_classes(current_user.user_id)
            teacher_class_ids = [int(c["id"]) for c in teacher_classes if c.get("id") is not None]
            if not teacher_class_ids:
                return success_response([], "Lay lich su diem danh thanh cong")
            query = query.filter(Attendance.class_id.in_(teacher_class_ids))

    # Buoc 2: Bo loc tuy chon
    if class_id is not None:
        query = query.filter(Attendance.class_id == class_id)
    if student_id is not None and current_user.role != "STUDENT":
        query = query.filter(Attendance.student_id == student_id)
    if session_date is not None:
        query = query.filter(Attendance.session_date == session_date)

    results = query.order_by(Attendance.session_date.desc(), Attendance.id.desc()).all()

    data = []
    for att, student_name, student_code in results:
        item = AttendanceResponse.model_validate(att).model_dump()
        item["student_name"] = student_name
        item["student_code"] = student_code
        data.append(item)

    return success_response(data, "Lay lich su diem danh thanh cong")


@router.post("/attendance", status_code=status.HTTP_201_CREATED)
def create_attendance(
    att_in: AttendanceCreate,
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(require_roles("ADMIN", "STAFF", "TEACHER")),
):
    """
    Ghi nhan diem danh cho hoc vien trong mot buoi hoc (chi ADMIN, STAFF, TEACHER).
    INV-STU-01: Moi hoc vien chi co toi da 1 ban ghi diem danh trong 1 ngay cua 1 lop.
    """
    # Buoc 1: Neu la TEACHER, xac minh giao vien phu trach lop hoc qua Admission Service
    if current_user.role == "TEACHER":
        class_info = get_class_info(att_in.class_id)
        teacher_id = class_info.get("teacher_id")
        if teacher_id is None or int(teacher_id) != current_user.user_id:
            raise StudentException(
                status_code=status.HTTP_403_FORBIDDEN,
                code="STU_FORBIDDEN_CLASS",
                message="Giao vien khong duoc diem danh cho lop minh khong phu trach",
            )

    # Buoc 2: Kiem tra hoc vien co ton tai khong
    student = db.query(Student).filter(Student.id == att_in.student_id).first()
    if not student:
        return error_response("STU_NOT_FOUND", "Hoc vien khong ton tai trong he thong", status.HTTP_404_NOT_FOUND)

    # Buoc 3: Kiem tra trung lap diem danh truoc khi ghi (INV-STU-01)
    existing = (
        db.query(Attendance)
        .filter(
            Attendance.class_id == att_in.class_id,
            Attendance.student_id == att_in.student_id,
            Attendance.session_date == att_in.session_date,
        )
        .first()
    )
    if existing:
        return error_response(
            "STU_DUPLICATE_ATTENDANCE",
            "Hoc vien nay da duoc diem danh trong lop vao ngay nay roi!",
            status.HTTP_400_BAD_REQUEST,
        )

    # Buoc 4: Luu ban ghi diem danh moi
    new_att = Attendance(
        class_id=att_in.class_id,
        student_id=att_in.student_id,
        session_date=att_in.session_date,
        status=att_in.status,
        notes=att_in.notes,
    )

    try:
        db.add(new_att)
        db.commit()
        db.refresh(new_att)
        payload = AttendanceResponse.model_validate(new_att).model_dump()
        payload["student_name"] = student.full_name
        payload["student_code"] = student.student_code
        return success_response(payload, "Diem danh thanh cong", status.HTTP_201_CREATED)
    except IntegrityError:
        db.rollback()
        return error_response(
            "STU_DUPLICATE_ATTENDANCE",
            "Hoc vien nay da duoc diem danh trong lop vao ngay nay roi!",
            status.HTTP_400_BAD_REQUEST,
        )


@router.put("/attendance/{attendance_id}")
def update_attendance(
    attendance_id: int,
    att_in: AttendanceUpdate,
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(require_roles("ADMIN", "STAFF", "TEACHER")),
):
    """
    Cap nhat trang thai hoac ghi chu diem danh da ghi nhan (chi ADMIN, STAFF, TEACHER).
    """
    att = db.query(Attendance).filter(Attendance.id == attendance_id).first()
    if not att:
        return error_response("STU_ATTENDANCE_NOT_FOUND", "Khong tim thay ban ghi diem danh", status.HTTP_404_NOT_FOUND)

    # Kiem tra quyen giao vien phu trach lop
    if current_user.role == "TEACHER":
        class_info = get_class_info(att.class_id)
        teacher_id = class_info.get("teacher_id")
        if teacher_id is None or int(teacher_id) != current_user.user_id:
            raise StudentException(
                status_code=status.HTTP_403_FORBIDDEN,
                code="STU_FORBIDDEN_CLASS",
                message="Giao vien khong duoc sua diem danh cua lop minh khong phu trach",
            )

    if att_in.status is not None:
        att.status = att_in.status
    if att_in.notes is not None:
        att.notes = att_in.notes
    if att_in.session_date is not None:
        att.session_date = att_in.session_date

    try:
        db.commit()
        db.refresh(att)
        student = db.query(Student).filter(Student.id == att.student_id).first()
        payload = AttendanceResponse.model_validate(att).model_dump()
        payload["student_name"] = student.full_name if student else None
        payload["student_code"] = student.student_code if student else None
        return success_response(payload, "Cap nhat diem danh thanh cong")
    except IntegrityError:
        db.rollback()
        return error_response(
            "STU_DUPLICATE_ATTENDANCE",
            "Da ton tai ban ghi diem danh khac cho hoc vien vao ngay nay",
            status.HTTP_400_BAD_REQUEST,
        )
