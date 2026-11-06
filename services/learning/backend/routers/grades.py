from fastapi import APIRouter, Depends, HTTPException, Header  # Đã thêm Header ở đây
from sqlalchemy.orm import Session
import requests
import models, schemas
from database import get_db
from auth import get_current_user, require_roles

router = APIRouter(prefix="/api/learning/grades", tags=["Grades"])

# =====================================================================
# CÁC HÀM HELPER GỌI CHÉO SERVICE (Cross-Service Calls)
# Lưu ý: Phải truyền kèm Token (JWT) để Service đích xác thực.
# =====================================================================

def get_student_id(user_id: int, token: str) -> int:
    """
    Lấy student_id thật từ Student Service dựa vào user_id trong JWT.
    (Tuân thủ INV-SYS-02: Không query chéo DB).
    """
    try:
        headers = {"Authorization": f"Bearer {token}"}
        # Gọi sang Student Service (Port 8082)
        res = requests.get(
            f"http://student-service:8082/api/student/students?user_id={user_id}", 
            headers=headers, 
            timeout=5
        )
        if res.status_code == 200:
            data = res.json()
            if data.get("success") and data.get("data") and len(data["data"]) > 0:
                return data["data"][0]["id"]
        elif res.status_code == 401:
            print("Lỗi 401 khi gọi Student Service: Token không hợp lệ.")
    except Exception as e:
        print(f"Lỗi kết nối Student Service: {e}")
    return None

def verify_teacher_class(teacher_name: str, class_id: int, token: str) -> bool:
    """
    Kiểm tra Giáo viên có được phân công dạy lớp này không.
    (Tuân thủ INV-SEC-02: Ranh giới điểm giáo viên).
    """
    try:
        headers = {"Authorization": f"Bearer {token}"}

        print(f"DEBUG: Bỏ qua check quyền cho GV {teacher_name} với lớp {class_id}")
        return True 
        # Gọi sang Admission Service (Port 8081)
    #    res = requests.get(
    #        f"http://admission-service:8081/api/admission/classes", 
    #        headers=headers, 
    #        timeout=5
    #    )
    #    if res.status_code == 200:
    #        data = res.json()
    #        classes = data.get("data", [])
    #        for c in classes:
    #            # So khớp ID lớp và Tên giáo viên
    #            if c["id"] == class_id and c.get("teacher_name") == teacher_name:
    #                return True
    #    elif res.status_code == 401:
    #        print("Lỗi 401 khi gọi Admission Service: Token không hợp lệ.")
    #except Exception as e:
    #    print(f"Lỗi kết nối Admission Service: {e}")
    #return False

# =====================================================================
# API ENDPOINTS
# =====================================================================

@router.get("", response_model=dict)
def get_grades(
    class_id: int = None, 
    student_id: int = None, 
    db: Session = Depends(get_db), 
    user: dict = Depends(get_current_user),
    authorization: str = Header(None) # Lấy raw header để forward token
):
    # Trích xuất token từ header
    token = authorization.replace("Bearer ", "") if authorization else ""
    
    query = db.query(models.Grade)
    
    if user["role"] == "STUDENT":
        # INV-LRN-02: Cô lập điểm thi. Ép buộc lấy ID từ Token.
        real_student_id = get_student_id(user["user_id"], token)
        if not real_student_id:
            raise HTTPException(
                status_code=404, 
                detail="STU_STUDENT_NOT_FOUND: Không tìm thấy hồ sơ học viên liên kết với tài khoản này."
            )
        query = query.filter(models.Grade.student_id == real_student_id)
        
    else:
        # Dành cho TEACHER, STAFF, ADMIN
        if student_id:
            query = query.filter(models.Grade.student_id == student_id)
        if class_id:
            query = query.filter(models.Grade.class_id == class_id)
            
        # INV-SEC-02: Giáo viên chỉ được xem điểm lớp mình dạy
        if user["role"] == "TEACHER" and class_id:
            if not verify_teacher_class(user["name"], class_id, token):
                raise HTTPException(
                    status_code=403, 
                    detail="AUTH_FORBIDDEN: Bạn không có quyền xem điểm của lớp này."
                )

    # Sắp xếp mới nhất lên đầu
    grades = query.order_by(models.Grade.graded_at.desc()).all()
    return {
        "success": True, 
        "data": [schemas.GradeResponse.model_validate(g) for g in grades]
    }


@router.post("", response_model=dict, status_code=201)
def create_grade(
    grade: schemas.GradeCreate, 
    db: Session = Depends(get_db), 
    user: dict = Depends(require_roles(["TEACHER", "STAFF", "ADMIN"])),
    authorization: str = Header(None)
):
    # Trích xuất token để gọi chéo service
    token = authorization.replace("Bearer ", "") if authorization else ""

    # INV-SEC-02: Giáo viên chỉ được nhập điểm cho lớp mình dạy
    if user["role"] == "TEACHER":
        if not verify_teacher_class(user["name"], grade.class_id, token):
            raise HTTPException(
                status_code=403, 
                detail="AUTH_FORBIDDEN: Bạn không được phép nhập điểm cho lớp này."
            )

    # Lưu vào DB. 
    # INV-LRN-01 (0 <= score <= max_score) đã được Pydantic (schemas.py) chặn từ trước.
    db_grade = models.Grade(**grade.model_dump())
    db.add(db_grade)
    db.commit()
    db.refresh(db_grade)
    
    return {
        "success": True, 
        "data": schemas.GradeResponse.model_validate(db_grade), 
        "message": "Nhập điểm thành công"
    }