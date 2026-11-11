from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session
from datetime import datetime

from database import get_db
from models import StatusRequest, Student
from schemas import StatusRequestCreate, StatusRequestResponse
from utils import success_response, error_response

router = APIRouter()

# ==========================================
# API 1: LẤY DANH SÁCH YÊU CẦU (GET)
# ==========================================
@router.get("/status-requests")
def get_status_requests(db: Session = Depends(get_db)):
    # Lấy tất cả yêu cầu, sắp xếp mới nhất lên đầu
    requests = db.query(StatusRequest).order_by(StatusRequest.id.desc()).all()
    
    # Chuyển đổi sang dạng JSON (kèm theo tên học viên để Frontend dễ hiển thị)
    data = []
    for req in requests:
        student = db.query(Student).filter(Student.id == req.student_id).first()
        item = StatusRequestResponse.model_validate(req).model_dump()
        item["student_name"] = student.full_name if student else "Không xác định"
        data.append(item)
        
    return success_response(data, "Lấy danh sách yêu cầu thành công")

# ==========================================
# API 2: HỌC VIÊN GỬI YÊU CẦU (POST)
# ==========================================
@router.post("/status-requests", status_code=status.HTTP_201_CREATED)
def create_status_request(req_in: StatusRequestCreate, db: Session = Depends(get_db)):
    # Kiểm tra học viên có tồn tại không
    student = db.query(Student).filter(Student.id == req_in.student_id).first()
    if not student:
        return error_response("STU_NOT_FOUND", "Học viên không tồn tại", 404)
        
    new_req = StatusRequest(
        student_id=req_in.student_id,
        request_type=req_in.request_type,
        reason=req_in.reason,
        class_id=req_in.class_id,
        target_class_id=req_in.target_class_id
    )
    db.add(new_req)
    db.commit()
    db.refresh(new_req)
    
    return success_response(
        StatusRequestResponse.model_validate(new_req).model_dump(),
        "Gửi yêu cầu thành công, vui lòng chờ Giáo vụ duyệt",
        201
    )

# ==========================================
# API 3: GIÁO VỤ DUYỆT YÊU CẦU (PUT)
# ==========================================
@router.put("/status-requests/{id}/approve")
def approve_request(id: int, db: Session = Depends(get_db)):
    req = db.query(StatusRequest).filter(StatusRequest.id == id).first()
    if not req:
        return error_response("STU_REQUEST_NOT_FOUND", "Không tìm thấy yêu cầu này", 404)
        
    if req.status != 'PENDING':
        return error_response("STU_REQUEST_PROCESSED", "Yêu cầu này đã được xử lý rồi", 400)

    # Cập nhật trạng thái yêu cầu
    req.status = 'APPROVED'
    req.reviewed_at = datetime.now()
    req.reviewed_by = 1 # Tạm gán ID người duyệt là 1 (Staff)

    # Tự động cập nhật trạng thái học viên (Theo đúng logic của Leader)
    student = db.query(Student).filter(Student.id == req.student_id).first()
    if req.request_type == 'RESERVE':
        student.status = 'RESERVED'
    elif req.request_type == 'DROP':
        student.status = 'DROPPED'

    db.commit()
    return success_response(None, "Đã duyệt yêu cầu và cập nhật trạng thái học viên")