"""
schemas.py - Dinh nghia Pydantic v2 Schemas cho Request va Response chuan snake_case.
"""

from datetime import date, datetime
from typing import Literal, Optional

from pydantic import BaseModel, EmailStr, Field, field_validator

StudentStatusLiteral = Literal["STUDYING", "RESERVED", "DROPPED", "GRADUATED"]
AttendanceStatusLiteral = Literal["PRESENT", "ABSENT", "LATE", "EXCUSED"]
RequestTypeLiteral = Literal["RESERVE", "TRANSFER", "DROP"]


# ==========================================
# 1. SCHEMAS CHO STUDENT (Ho so hoc vien)
# ==========================================

class StudentBase(BaseModel):
    """Truong thong tin co ban cua ho so hoc vien."""

    full_name: str = Field(..., min_length=1, max_length=255, description="Ho va ten hoc vien")
    email: EmailStr = Field(..., description="Email duy nhat cua hoc vien")
    phone: Optional[str] = Field(None, max_length=50, description="So dien thoai lien he")
    date_of_birth: Optional[date] = Field(None, description="Ngay sinh (YYYY-MM-DD)")
    address: Optional[str] = Field(None, description="Dia chi thuong tru")

    @field_validator("full_name")
    @classmethod
    def validate_full_name(cls, v: str) -> str:
        cleaned = v.strip()
        if not cleaned:
            raise ValueError("Ho va ten khong duoc de trong")
        return cleaned


class StudentCreate(StudentBase):
    """Payload tao moi ho so hoc vien boi Admin/Staff."""

    user_id: Optional[int] = Field(None, gt=0, description="ID tai khoan tu Admission Service")
    status: StudentStatusLiteral = Field("STUDYING", description="Trang thai hoc vu (INV-STU-02)")


class StudentUpdate(BaseModel):
    """Payload cap nhat thong tin ho so hoc vien."""

    full_name: Optional[str] = Field(None, min_length=1, max_length=255)
    email: Optional[EmailStr] = None
    phone: Optional[str] = Field(None, max_length=50)
    date_of_birth: Optional[date] = None
    address: Optional[str] = None
    status: Optional[StudentStatusLiteral] = Field(None, description="STUDYING, RESERVED, DROPPED, GRADUATED")

    @field_validator("full_name")
    @classmethod
    def validate_optional_full_name(cls, v: Optional[str]) -> Optional[str]:
        if v is None:
            return None
        cleaned = v.strip()
        if not cleaned:
            raise ValueError("Ho va ten khong duoc de trong")
        return cleaned


class InternalStudentSyncRequest(BaseModel):
    """Payload dong bo ho so hoc vien tu Admission Service sang Student Service."""

    user_id: Optional[int] = Field(None, gt=0)
    full_name: str = Field(..., min_length=1, max_length=255)
    email: EmailStr
    phone: Optional[str] = Field(None, max_length=50)
    date_of_birth: Optional[date] = None
    address: Optional[str] = None

    @field_validator("full_name")
    @classmethod
    def validate_sync_name(cls, v: str) -> str:
        cleaned = v.strip()
        if not cleaned:
            raise ValueError("Ho va ten khong duoc de trong")
        return cleaned


class StudentResponse(StudentBase):
    """Du lieu ho so hoc vien tra ve cho Client."""

    id: int
    user_id: Optional[int] = None
    student_code: str
    status: str
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}


# ==========================================
# 2. SCHEMAS CHO ATTENDANCE (Diem danh)
# ==========================================

class AttendanceCreate(BaseModel):
    """Payload tao moi ban ghi diem danh theo buoi."""

    class_id: int = Field(..., gt=0, description="ID lop hoc ben Admission")
    student_id: int = Field(..., gt=0, description="ID hoc vien (student.students.id)")
    session_date: date = Field(..., description="Ngay hoc (YYYY-MM-DD)")
    status: AttendanceStatusLiteral = Field("PRESENT", description="PRESENT, ABSENT, LATE, EXCUSED")
    notes: Optional[str] = Field(None, description="Ghi chu cua giao vien")


class AttendanceUpdate(BaseModel):
    """Payload cap nhat trang thai hoac ghi chu diem danh."""

    status: Optional[AttendanceStatusLiteral] = Field(None, description="PRESENT, ABSENT, LATE, EXCUSED")
    notes: Optional[str] = None
    session_date: Optional[date] = None


class AttendanceResponse(BaseModel):
    """Du lieu diem danh tra ve cho Client."""

    id: int
    class_id: int
    student_id: int
    session_date: date
    status: str
    notes: Optional[str] = None
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}


# ==========================================
# 3. SCHEMAS CHO STATUS_REQUESTS (Yeu cau hoc vu)
# ==========================================

class StatusRequestCreate(BaseModel):
    """Payload gui yeu cau hoc vu (RESERVE, TRANSFER, DROP)."""

    student_id: Optional[int] = Field(None, gt=0, description="ID hoc vien; STUDENT co the bo qua de tu anh xa")
    request_type: RequestTypeLiteral = Field(..., description="RESERVE, TRANSFER, DROP")
    reason: str = Field(..., min_length=1, description="Ly do gui yeu cau")
    class_id: Optional[int] = Field(None, gt=0, description="Lop dang theo hoc")
    target_class_id: Optional[int] = Field(None, gt=0, description="Lop muon chuyen den (khi TRANSFER)")

    @field_validator("reason")
    @classmethod
    def validate_reason(cls, v: str) -> str:
        cleaned = v.strip()
        if not cleaned:
            raise ValueError("Ly do gui yeu cau khong duoc de trong")
        return cleaned


class StatusRequestReview(BaseModel):
    """Payload tuy chon khi Staff/Admin duyet hoac tu choi yeu cau hoc vu."""

    reviewed_note: Optional[str] = Field(None, description="Ghi chu cua nguoi xet duyet")


class StatusRequestResponse(BaseModel):
    """Du lieu yeu cau hoc vu tra ve cho Client."""

    id: int
    student_id: int
    request_type: str
    reason: str
    class_id: Optional[int] = None
    target_class_id: Optional[int] = None
    status: str
    reviewed_by: Optional[int] = None
    reviewed_note: Optional[str] = None
    created_at: datetime
    updated_at: datetime
    reviewed_at: Optional[datetime] = None

    model_config = {"from_attributes": True}
