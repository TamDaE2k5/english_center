from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session
from sqlalchemy.exc import IntegrityError
import random
from datetime import datetime

from database import get_db
from models import Student
from schemas import StudentCreate, StudentResponse
from utils import success_response, error_response

router = APIRouter()

# ==========================================
# API 1: LẤY DANH SÁCH HỌC VIÊN (GET)
# ==========================================
@router.get("/students")
def get_students(db: Session = Depends(get_db)):
    students = db.query(Student).order_by(Student.id.asc()).all()
    data = [StudentResponse.model_validate(s).model_dump() for s in students]
    return success_response(data, "Lấy danh sách học viên thành công")

# ==========================================
# API 2: THÊM HỌC VIÊN MỚI (POST)
# ==========================================
@router.post("/students", status_code=status.HTTP_201_CREATED)
def create_student(student_in: StudentCreate, db: Session = Depends(get_db)):
    # 1. Kiểm tra Email đã tồn tại chưa
    existing_student = db.query(Student).filter(Student.email == student_in.email).first()
    if existing_student:
        return error_response("STU_EMAIL_EXISTED", "Email này đã được đăng ký cho một học viên khác", 400)
    
    # 2. Khởi tạo đối tượng Học viên
    new_student = Student(
        full_name=student_in.full_name,
        email=student_in.email,
        phone=student_in.phone,
        date_of_birth=student_in.date_of_birth,
        address=student_in.address,
        user_id=student_in.user_id
    )
    
    # 3. TỰ ĐỘNG SINH MÃ HỌC VIÊN BẰNG PYTHON
    # Format: HV-2026-xxxx (x là số ngẫu nhiên từ 1000-9999)
    current_year = datetime.now().year
    random_number = random.randint(1000, 9999)
    new_student.student_code = f"HV-{current_year}-{random_number}"
    
    # 4. Lưu vào Database
    try:
        db.add(new_student)
        db.commit()
        db.refresh(new_student)
        
        return success_response(
            StudentResponse.model_validate(new_student).model_dump(), 
            "Thêm học viên thành công", 
            201
        )
    except IntegrityError:
        db.rollback()
        return error_response("STU_DATABASE_ERROR", "Lỗi lưu dữ liệu vào Database (Có thể trùng mã HV)", 500)