from pydantic import BaseModel, EmailStr, Field
from typing import Optional, List
from datetime import date, datetime

# ==========================================
# 1. SCHEMAS CHO STUDENT (Hồ sơ học viên)
# ==========================================

# Lớp cơ sở chứa các trường chung
class StudentBase(BaseModel):
    full_name: str = Field(..., description="Họ và tên học viên")
    email: EmailStr = Field(..., description="Email (bắt buộc đúng định dạng)")
    phone: Optional[str] = Field(None, description="Số điện thoại")
    date_of_birth: Optional[date] = Field(None, description="Ngày sinh (YYYY-MM-DD)")
    address: Optional[str] = Field(None, description="Địa chỉ")

# Dùng khi Frontend gửi dữ liệu lên để tạo mới
class StudentCreate(StudentBase):
    user_id: Optional[int] = Field(None, description="ID tài khoản từ Admission (nếu có)")

# Dùng khi cập nhật (các trường đều có thể không cần gửi)
class StudentUpdate(BaseModel):
    full_name: Optional[str] = None
    phone: Optional[str] = None
    date_of_birth: Optional[date] = None
    address: Optional[str] = None
    status: Optional[str] = None

# Dùng để trả dữ liệu về cho Frontend (Bao gồm cả các trường do DB tự sinh)
class StudentResponse(StudentBase):
    id: int
    user_id: Optional[int]
    student_code: str
    status: str
    created_at: datetime
    updated_at: datetime

    # Cấu hình này cho phép Pydantic đọc dữ liệu trực tiếp từ SQLAlchemy Model
    model_config = {"from_attributes": True}


# ==========================================
# 2. SCHEMAS CHO ATTENDANCE (Điểm danh)
# ==========================================

class AttendanceCreate(BaseModel):
    class_id: int
    student_id: int
    session_date: date
    status: str = Field("PRESENT", description="PRESENT, ABSENT, LATE, EXCUSED")
    notes: Optional[str] = None

class AttendanceResponse(AttendanceCreate):
    id: int
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}


# ==========================================
# 3. SCHEMAS CHO STATUS_REQUESTS (Yêu cầu học vụ)
# ==========================================

class StatusRequestCreate(BaseModel):
    student_id: int
    request_type: str = Field(..., description="RESERVE, TRANSFER, DROP")
    reason: str = Field(..., description="Lý do gửi yêu cầu")
    class_id: Optional[int] = None
    target_class_id: Optional[int] = None

class StatusRequestResponse(StatusRequestCreate):
    id: int
    status: str
    reviewed_by: Optional[int] = None
    reviewed_note: Optional[str] = None
    created_at: datetime
    updated_at: datetime
    reviewed_at: Optional[datetime] = None

    model_config = {"from_attributes": True}