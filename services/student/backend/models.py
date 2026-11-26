"""
models.py - Dinh nghia cac ORM Model thuoc schema 'student' khop 100% voi scripts/init-db.sql.
Bao gom 3 bang: student.students, student.attendance, student.status_requests.
"""

from sqlalchemy import CheckConstraint, Column, Date, DateTime, ForeignKey, Integer, String, Text, UniqueConstraint
from sqlalchemy.sql import func

from database import Base


class Student(Base):
    """
    Bang ho so hoc vien (student.students).
    Cau noi dinh danh R-ID-1: user_id la Logical FK tro toi admission.users(id).
    """

    __tablename__ = "students"
    __table_args__ = (
        CheckConstraint(
            "status IN ('STUDYING', 'RESERVED', 'DROPPED', 'GRADUATED')",
            name="students_status_check",
        ),
        {"schema": "student"},
    )

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, nullable=True, index=True)
    student_code = Column(String(30), unique=True, nullable=False)
    full_name = Column(String(255), nullable=False)
    email = Column(String(255), unique=True, nullable=False)
    phone = Column(String(50), nullable=True)
    date_of_birth = Column(Date, nullable=True)
    address = Column(Text, nullable=True)
    status = Column(String(50), nullable=False, default="STUDYING")

    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)


class Attendance(Base):
    """
    Bang diem danh theo buoi hoc (student.attendance).
    INV-STU-01: Rang buoc UNIQUE(class_id, student_id, session_date) ngan diem danh lap trong ngay.
    """

    __tablename__ = "attendance"
    __table_args__ = (
        UniqueConstraint("class_id", "student_id", "session_date", name="attendance_class_id_student_id_session_date_key"),
        CheckConstraint(
            "status IN ('PRESENT', 'ABSENT', 'LATE', 'EXCUSED')",
            name="attendance_status_check",
        ),
        {"schema": "student"},
    )

    id = Column(Integer, primary_key=True, index=True)
    class_id = Column(Integer, nullable=False)
    student_id = Column(Integer, ForeignKey("student.students.id", ondelete="CASCADE"), nullable=False)
    session_date = Column(Date, nullable=False)
    status = Column(String(20), nullable=False, default="PRESENT")
    notes = Column(Text, nullable=True)

    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)


class StatusRequest(Base):
    """
    Bang yeu cau hoc vu cua hoc vien (student.status_requests): bao luu, chuyen lop, bo hoc.
    """

    __tablename__ = "status_requests"
    __table_args__ = (
        CheckConstraint(
            "request_type IN ('RESERVE', 'TRANSFER', 'DROP')",
            name="status_requests_request_type_check",
        ),
        CheckConstraint(
            "status IN ('PENDING', 'APPROVED', 'REJECTED')",
            name="status_requests_status_check",
        ),
        {"schema": "student"},
    )

    id = Column(Integer, primary_key=True, index=True)
    student_id = Column(Integer, ForeignKey("student.students.id", ondelete="CASCADE"), nullable=False)
    request_type = Column(String(20), nullable=False)
    reason = Column(Text, nullable=False)
    class_id = Column(Integer, nullable=True)
    target_class_id = Column(Integer, nullable=True)
    status = Column(String(20), nullable=False, default="PENDING")
    reviewed_by = Column(Integer, nullable=True)
    reviewed_note = Column(Text, nullable=True)

    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)
    reviewed_at = Column(DateTime(timezone=True), nullable=True)
