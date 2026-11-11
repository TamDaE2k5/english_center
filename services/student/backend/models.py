from sqlalchemy import Column, Integer, String, Date, Text, DateTime, CheckConstraint, UniqueConstraint, ForeignKey
from sqlalchemy.sql import func
from database import Base

class Student(Base):
    __tablename__ = 'students'
    __table_args__ = (
        CheckConstraint("status IN ('STUDYING', 'RESERVED', 'DROPPED', 'GRADUATED')", name='check_student_status'),
        {'schema': 'student'}
    )

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, nullable=True, unique=True) # Cầu nối R-ID-1
    
    # Đã bỏ server_default để code Python tự sinh mã, tránh lỗi 500 do thiếu Sequence
    student_code = Column(String(30), unique=True, nullable=True) 
    
    full_name = Column(String(255), nullable=False)
    email = Column(String(255), unique=True, nullable=False)
    phone = Column(String(50), nullable=True)
    date_of_birth = Column(Date, nullable=True)
    address = Column(Text, nullable=True)
    status = Column(String(50), nullable=False, default='STUDYING')
    
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())


class Attendance(Base):
    __tablename__ = 'attendance'
    __table_args__ = (
        UniqueConstraint('class_id', 'student_id', 'session_date', name='uq_attendance_class_student_date'), 
        CheckConstraint("status IN ('PRESENT', 'ABSENT', 'LATE', 'EXCUSED')", name='check_attendance_status'),
        {'schema': 'student'}
    )

    id = Column(Integer, primary_key=True, index=True)
    class_id = Column(Integer, nullable=False)
    student_id = Column(Integer, ForeignKey('student.students.id', ondelete='CASCADE'), nullable=False)
    session_date = Column(Date, nullable=False)
    status = Column(String(20), nullable=False, default='PRESENT')
    notes = Column(Text, nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())


class StatusRequest(Base):
    __tablename__ = 'status_requests'
    __table_args__ = (
        CheckConstraint("request_type IN ('RESERVE', 'TRANSFER', 'DROP')", name='check_req_type'),
        CheckConstraint("status IN ('PENDING', 'APPROVED', 'REJECTED')", name='check_req_status'),
        {'schema': 'student'}
    )

    id = Column(Integer, primary_key=True, index=True)
    student_id = Column(Integer, ForeignKey('student.students.id', ondelete='CASCADE'), nullable=False)
    request_type = Column(String(20), nullable=False)
    reason = Column(Text, nullable=False)
    class_id = Column(Integer, nullable=True)
    target_class_id = Column(Integer, nullable=True)
    status = Column(String(20), nullable=False, default='PENDING')
    reviewed_by = Column(Integer, nullable=True)
    reviewed_note = Column(Text, nullable=True)
    
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
    reviewed_at = Column(DateTime(timezone=True), nullable=True)