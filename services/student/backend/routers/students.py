"""
routers/students.py - Cac endpoint quan ly ho so hoc vien va danh sach thanh vien lop hoc.
Tuan thu phan quyen INV-SEC-01 (co lap ho so hoc vien), INV-STU-02, INV-STU-03, INV-STU-05.
"""

from typing import Optional

from fastapi import APIRouter, Depends, Query, status
from sqlalchemy import or_
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from auth import CurrentUser, StudentException, get_current_user, require_roles
from clients import get_class_approved_enrollments, get_class_info
from database import get_db
from models import Student
from schemas import StudentCreate, StudentResponse, StudentUpdate
from utils import error_response, generate_student_code, success_response

router = APIRouter()


@router.get("/students")
def get_students(
    search: Optional[str] = Query(default=None, description="Tim theo ten, email hoac ma hoc vien"),
    status_filter: Optional[str] = Query(default=None, alias="status", description="Loc theo trang thai hoc vu"),
    user_id: Optional[int] = Query(default=None, gt=0, description="Loc theo user_id"),
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(require_roles("ADMIN", "STAFF", "STUDENT")),
):
    """
    Lay danh sach ho so hoc vien.
    - ADMIN, STAFF: Xem toan bo danh sach, ho tro tim kiem theo ten/email/ma HV va loc theo status.
    - STUDENT (INV-SEC-01): Chi duoc xem duy nhat ho so cua chinh minh (theo user_id trong JWT).
    """
    query = db.query(Student)

    # Buoc 1: Ap dung co lap du lieu neu nguoi goi la STUDENT (INV-SEC-01)
    if current_user.role == "STUDENT":
        if user_id is not None and user_id != current_user.user_id:
            raise StudentException(
                status_code=status.HTTP_403_FORBIDDEN,
                code="STU_FORBIDDEN",
                message="Hoc vien khong duoc phep xem ho so cua hoc vien khac",
            )
        query = query.filter(Student.user_id == current_user.user_id)
    else:
        if user_id is not None:
            query = query.filter(Student.user_id == user_id)

    # Buoc 2: Loc theo trang thai neu co
    if status_filter:
        query = query.filter(Student.status == status_filter.strip().upper())

    # Buoc 3: Tim kiem theo ho ten, email hoac student_code
    if search:
        kw = f"%{search.strip()}%"
        query = query.filter(
            or_(
                Student.full_name.ilike(kw),
                Student.email.ilike(kw),
                Student.student_code.ilike(kw),
            )
        )

    students = query.order_by(Student.id.asc()).all()
    data = [StudentResponse.model_validate(s).model_dump() for s in students]
    return success_response(data, "Lay danh sach hoc vien thanh cong")


@router.get("/students/me")
def get_my_student_profile(
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(get_current_user),
):
    """
    Lay ho so hoc vien cua chinh nguoi dung dang dang nhap thong qua user_id trong JWT (R-ID-1).
    """
    student = db.query(Student).filter(Student.user_id == current_user.user_id).first()
    if not student:
        return error_response(
            "STU_NOT_FOUND",
            "Tai khoan nay chua co ho so hoc vien trong he thong",
            status.HTTP_404_NOT_FOUND,
        )
    return success_response(
        StudentResponse.model_validate(student).model_dump(),
        "Lay ho so ca nhan thanh cong",
    )


@router.get("/students/{student_id}")
def get_student_by_id(
    student_id: int,
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(require_roles("ADMIN", "STAFF", "STUDENT")),
):
    """
    Lay chi tiet mot ho so hoc vien theo ID.
    INV-SEC-01: Neu la STUDENT thi student.user_id phai trung current_user.user_id.
    """
    student = db.query(Student).filter(Student.id == student_id).first()
    if not student:
        return error_response("STU_NOT_FOUND", "Khong tim thay ho so hoc vien", status.HTTP_404_NOT_FOUND)

    if current_user.role == "STUDENT" and student.user_id != current_user.user_id:
        raise StudentException(
            status_code=status.HTTP_403_FORBIDDEN,
            code="STU_FORBIDDEN",
            message="Ban chi duoc phep xem ho so cua chinh minh (INV-SEC-01)",
        )

    return success_response(
        StudentResponse.model_validate(student).model_dump(),
        "Lay chi tiet ho so hoc vien thanh cong",
    )


@router.post("/students", status_code=status.HTTP_201_CREATED)
def create_student(
    student_in: StudentCreate,
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(require_roles("ADMIN", "STAFF")),
):
    """
    Tao moi ho so hoc vien (chi danh cho ADMIN, STAFF).
    Tu dong sinh ma hoc vien HV-YYYY-NNNN tu sequence PostgreSQL (INV-STU-05).
    """
    # Buoc 1: Kiem tra trung email (INV-STU-06)
    existing_email = db.query(Student).filter(Student.email == student_in.email).first()
    if existing_email:
        return error_response(
            "STU_EMAIL_EXISTED",
            "Email nay da duoc dang ky cho mot hoc vien khac",
            status.HTTP_409_CONFLICT,
        )

    # Buoc 2: Kiem tra trung user_id neu duoc truyen len
    if student_in.user_id is not None:
        existing_user = db.query(Student).filter(Student.user_id == student_in.user_id).first()
        if existing_user:
            return error_response(
                "STU_USER_ALREADY_LINKED",
                "Tai khoan user_id nay da duoc lien ket voi mot ho so hoc vien khac",
                status.HTTP_409_CONFLICT,
            )

    # Buoc 3: Sinh ma hoc vien bang sequence va luu DB
    new_student = Student(
        user_id=student_in.user_id,
        student_code=generate_student_code(db),
        full_name=student_in.full_name,
        email=student_in.email,
        phone=student_in.phone,
        date_of_birth=student_in.date_of_birth,
        address=student_in.address,
        status=student_in.status,
    )

    try:
        db.add(new_student)
        db.commit()
        db.refresh(new_student)
        return success_response(
            StudentResponse.model_validate(new_student).model_dump(),
            "Them hoc vien thanh cong",
            status.HTTP_201_CREATED,
        )
    except IntegrityError:
        db.rollback()
        return error_response(
            "STU_DATABASE_ERROR",
            "Du lieu hoc vien bi trung lap hoac khong hop le",
            status.HTTP_409_CONFLICT,
        )


@router.put("/students/{student_id}")
def update_student(
    student_id: int,
    student_in: StudentUpdate,
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(require_roles("ADMIN", "STAFF", "STUDENT")),
):
    """
    Cap nhat thong tin ho so hoc vien.
    - ADMIN, STAFF: Duoc cap nhat thong tin ca nhan va trang thai hoc vu.
    - STUDENT (INV-SEC-01): Chi duoc cap nhat thong tin ca nhan cua chinh minh, khong duoc tu doi status hoac email.
    """
    student = db.query(Student).filter(Student.id == student_id).first()
    if not student:
        return error_response("STU_NOT_FOUND", "Khong tim thay ho so hoc vien", status.HTTP_404_NOT_FOUND)

    # Buoc 1: Kiem tra quyen so huu neu la STUDENT
    if current_user.role == "STUDENT":
        if student.user_id != current_user.user_id:
            raise StudentException(
                status_code=status.HTTP_403_FORBIDDEN,
                code="STU_FORBIDDEN",
                message="Ban chi duoc phep cap nhat ho so cua chinh minh (INV-SEC-01)",
            )
        if student_in.status is not None and student_in.status != student.status:
            raise StudentException(
                status_code=status.HTTP_403_FORBIDDEN,
                code="STU_FORBIDDEN",
                message="Hoc vien khong co quyen tu thay doi trang thai hoc vu",
            )
        if student_in.email is not None and student_in.email != student.email:
            raise StudentException(
                status_code=status.HTTP_403_FORBIDDEN,
                code="STU_FORBIDDEN",
                message="Hoc vien khong co quyen tu thay doi email dinh danh",
            )

    # Buoc 2: Kiem tra trung email neu ADMIN/STAFF doi email
    if student_in.email is not None and student_in.email != student.email:
        dup = db.query(Student).filter(Student.email == student_in.email, Student.id != student_id).first()
        if dup:
            return error_response(
                "STU_EMAIL_EXISTED",
                "Email nay da duoc su dung boi hoc vien khac",
                status.HTTP_409_CONFLICT,
            )
        student.email = student_in.email

    # Buoc 3: Cap nhat cac truong duoc cung cap
    if student_in.full_name is not None:
        student.full_name = student_in.full_name
    if student_in.phone is not None:
        student.phone = student_in.phone
    if student_in.date_of_birth is not None:
        student.date_of_birth = student_in.date_of_birth
    if student_in.address is not None:
        student.address = student_in.address
    if student_in.status is not None and current_user.role in ("ADMIN", "STAFF"):
        student.status = student_in.status

    try:
        db.commit()
        db.refresh(student)
        return success_response(
            StudentResponse.model_validate(student).model_dump(),
            "Cap nhat ho so hoc vien thanh cong",
        )
    except IntegrityError:
        db.rollback()
        return error_response(
            "STU_DATABASE_ERROR",
            "Khong the cap nhat ho so do xung dot du lieu",
            status.HTTP_409_CONFLICT,
        )


@router.get("/classes/{class_id}/members")
def get_class_members(
    class_id: int,
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(require_roles("ADMIN", "STAFF", "TEACHER")),
):
    """
    Lay danh sach hoc vien chinh thuc cua mot lop hoc (INV-STU-03).
    Du lieu thanh vien lop duoc lay qua REST API tu Admission Service (enrollments status=APPROVED),
    sau do anh xa voi ho so trong student.students qua user_id (R-ID-1).
    """
    # Buoc 1: Xac minh lop ton tai va kiem tra giao vien phu trach neu nguoi goi la TEACHER
    class_info = get_class_info(class_id)
    if current_user.role == "TEACHER":
        teacher_id = class_info.get("teacher_id")
        if teacher_id is None or int(teacher_id) != current_user.user_id:
            raise StudentException(
                status_code=status.HTTP_403_FORBIDDEN,
                code="STU_FORBIDDEN_CLASS",
                message="Giao vien khong duoc phan cong phu trach lop hoc nay",
            )

    # Buoc 2: Goi Admission Service lay cac don dang ky APPROVED cua lop
    enrollments = get_class_approved_enrollments(class_id, raw_token=current_user.raw_token)
    user_ids = [int(e["student_id"]) for e in enrollments if e.get("student_id") is not None]

    # Buoc 3: Anh xa user_id sang student.students trong schema student
    students_by_user_id = {}
    if user_ids:
        matched_students = db.query(Student).filter(Student.user_id.in_(user_ids)).all()
        students_by_user_id = {s.user_id: s for s in matched_students if s.user_id is not None}

    members = []
    for enr in enrollments:
        uid = enr.get("student_id")
        if uid is None:
            continue
        uid_int = int(uid)
        stu = students_by_user_id.get(uid_int)
        members.append({
            "student_id": stu.id if stu else None,
            "user_id": uid_int,
            "student_code": stu.student_code if stu else None,
            "full_name": stu.full_name if stu else (enr.get("student_name") or f"User #{uid_int}"),
            "email": stu.email if stu else enr.get("student_email"),
            "phone": stu.phone if stu else None,
            "status": stu.status if stu else "STUDYING",
            "enrollment_id": enr.get("id") or enr.get("enrollment_id"),
            "enrolled_at": enr.get("enrolled_at"),
        })

    return success_response(members, "Lay danh sach thanh vien lop thanh cong")
