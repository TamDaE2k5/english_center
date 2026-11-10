from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session
from sqlalchemy.exc import IntegrityError
from typing import Optional

from database import get_db
from models import Attendance, Student
from schemas import AttendanceCreate, AttendanceResponse
from utils import success_response, error_response

router = APIRouter()

# ==========================================
# API 1: ĐIỂM DANH HỌC VIÊN (POST)
# ==========================================
@router.post("/attendance", status_code=status.HTTP_201_CREATED)
def create_attendance(att_in: AttendanceCreate, db: Session = Depends(get_db)):
    # 1. Kiểm tra xem học viên có tồn tại không (Tránh lỗi khóa ngoại)
    student = db.query(Student).filter(Student.id == att_in.student_id).first()
    if not student:
        return error_response("STU_NOT_FOUND", "Học viên không tồn tại trong hệ thống", 404)

    # 2. Khởi tạo bản ghi điểm danh mới
    new_att = Attendance(
        class_id=att_in.class_id,
        student_id=att_in.student_id,
        session_date=att_in.session_date,
        status=att_in.status,
        notes=att_in.notes
    )
    
    # 3. Lưu vào Database và BẮT LỖI TRÙNG LẶP
    try:
        db.add(new_att)
        db.commit()
        db.refresh(new_att)
        
        # Trả về thành công
        return success_response(
            AttendanceResponse.model_validate(new_att).model_dump(),
            "Điểm danh thành công",
            201
        )
        
    except IntegrityError:
        # NẾU VI PHẠM RÀNG BUỘC UNIQUE (class_id, student_id, session_date)
        db.rollback()
        return error_response(
            "STU_DUPLICATE_ATTENDANCE", 
            "Học viên này đã được điểm danh trong lớp vào ngày này rồi!", 
            400
        )

# ==========================================
# API 2: LẤY LỊCH SỬ ĐIỂM DANH (GET)
# ==========================================
@router.get("/attendance")
def get_attendance(class_id: Optional[int] = None, db: Session = Depends(get_db)):
    # Join bảng Attendance và Student để lấy được tên học viên
    query = db.query(Attendance, Student.full_name).join(Student, Attendance.student_id == Student.id)
    
    # Nếu Frontend truyền class_id lên thì lọc theo lớp đó
    if class_id:
        query = query.filter(Attendance.class_id == class_id)
        
    results = query.order_by(Attendance.session_date.desc()).all()
    
    # Định dạng lại dữ liệu trả về
    data = []
    for att, student_name in results:
        item = AttendanceResponse.model_validate(att).model_dump()
        item["student_name"] = student_name 
        data.append(item)
        
    return success_response(data, "Lấy lịch sử điểm danh thành công")