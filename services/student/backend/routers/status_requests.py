"""
routers/status_requests.py - Cac endpoint quan ly yeu cau hoc vu (bao luu, chuyen lop, bo hoc).
Tuan thu INV-SEC-01 (co lap du lieu hoc vien) va may trang thai status_requests.
"""

from datetime import datetime, timezone
from typing import Optional

from fastapi import APIRouter, Body, Depends, Query, status
from sqlalchemy.orm import Session

from auth import CurrentUser, StudentException, require_roles
from database import get_db
from models import StatusRequest, Student
from schemas import StatusRequestCreate, StatusRequestResponse, StatusRequestReview
from utils import error_response, success_response

router = APIRouter()


@router.get("/status-requests")
def get_status_requests(
    status_filter: Optional[str] = Query(default=None, alias="status", description="Loc theo trang thai PENDING/APPROVED/REJECTED"),
    student_id: Optional[int] = Query(default=None, gt=0, description="Loc theo ID hoc vien"),
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(require_roles("ADMIN", "STAFF", "STUDENT")),
):
    """
    Lay danh sach yeu cau hoc vu.
    - ADMIN, STAFF: Xem tat ca yeu cau, co the loc theo status hoac student_id.
    - STUDENT (INV-SEC-01): Chi xem yeu cau cua chinh minh.
    """
    query = db.query(StatusRequest, Student.full_name, Student.student_code).join(
        Student, StatusRequest.student_id == Student.id
    )

    if current_user.role == "STUDENT":
        own_student = db.query(Student).filter(Student.user_id == current_user.user_id).first()
        if not own_student:
            return success_response([], "Lay danh sach yeu cau thanh cong")
        if student_id is not None and student_id != own_student.id:
            raise StudentException(
                status_code=status.HTTP_403_FORBIDDEN,
                code="STU_FORBIDDEN",
                message="Hoc vien khong duoc phep xem yeu cau hoc vu cua hoc vien khac",
            )
        query = query.filter(StatusRequest.student_id == own_student.id)
    elif student_id is not None:
        query = query.filter(StatusRequest.student_id == student_id)

    if status_filter:
        query = query.filter(StatusRequest.status == status_filter.strip().upper())

    results = query.order_by(StatusRequest.id.desc()).all()

    data = []
    for req, student_name, student_code in results:
        item = StatusRequestResponse.model_validate(req).model_dump()
        item["student_name"] = student_name
        item["student_code"] = student_code
        data.append(item)

    return success_response(data, "Lay danh sach yeu cau thanh cong")


@router.post("/status-requests", status_code=status.HTTP_201_CREATED)
def create_status_request(
    req_in: StatusRequestCreate,
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(require_roles("ADMIN", "STAFF", "STUDENT")),
):
    """
    Gui yeu cau hoc vu moi (RESERVE, TRANSFER, DROP).
    - STUDENT: Tu dong anh xa theo ho so cua minh; cam gui ho cho hoc vien khac (INV-SEC-01).
    - ADMIN, STAFF: Bat buoc truyen student_id hop le.
    """
    # Buoc 1: Xac dinh va kiem tra quyen doi voi hoc vien muc tieu
    if current_user.role == "STUDENT":
        student = db.query(Student).filter(Student.user_id == current_user.user_id).first()
        if not student:
            return error_response(
                "STU_NOT_FOUND",
                "Tai khoan chua co ho so hoc vien de gui yeu cau",
                status.HTTP_404_NOT_FOUND,
            )
        if req_in.student_id is not None and req_in.student_id != student.id:
            raise StudentException(
                status_code=status.HTTP_403_FORBIDDEN,
                code="STU_FORBIDDEN",
                message="Hoc vien khong duoc phep tao yeu cau cho hoc vien khac (INV-SEC-01)",
            )
    else:
        if req_in.student_id is None:
            return error_response(
                "STU_VALIDATION_ERROR",
                "Truong 'student_id': Bat buoc cung cap student_id khi Staff/Admin tao yeu cau",
                status.HTTP_400_BAD_REQUEST,
            )
        student = db.query(Student).filter(Student.id == req_in.student_id).first()
        if not student:
            return error_response("STU_NOT_FOUND", "Hoc vien khong ton tai", status.HTTP_404_NOT_FOUND)

    # Buoc 2: Kiem tra logic nghiep vu chuyen lop
    if (
        req_in.request_type == "TRANSFER"
        and req_in.class_id is not None
        and req_in.target_class_id is not None
        and req_in.class_id == req_in.target_class_id
    ):
        return error_response(
            "STU_INVALID_TRANSFER",
            "Lop chuyen den phai khac lop hien tai",
            status.HTTP_400_BAD_REQUEST,
        )

    # Buoc 3: Tao ban ghi yeu cau o trang thai PENDING
    new_req = StatusRequest(
        student_id=student.id,
        request_type=req_in.request_type,
        reason=req_in.reason,
        class_id=req_in.class_id,
        target_class_id=req_in.target_class_id,
        status="PENDING",
    )
    db.add(new_req)
    db.commit()
    db.refresh(new_req)

    payload = StatusRequestResponse.model_validate(new_req).model_dump()
    payload["student_name"] = student.full_name
    payload["student_code"] = student.student_code
    return success_response(
        payload,
        "Gui yeu cau thanh cong, vui long cho Giao vu duyet",
        status.HTTP_201_CREATED,
    )


@router.put("/status-requests/{request_id}/approve")
def approve_request(
    request_id: int,
    review_in: Optional[StatusRequestReview] = Body(default=None),
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(require_roles("ADMIN", "STAFF")),
):
    """
    Giao vu hoac Quan tri vien duyet yeu cau hoc vu (PENDING -> APPROVED).
    Tu dong cap nhat trang thai hoc vien: RESERVE -> RESERVED, DROP -> DROPPED (INV-STU-02).
    """
    req = db.query(StatusRequest).filter(StatusRequest.id == request_id).first()
    if not req:
        return error_response("STU_REQUEST_NOT_FOUND", "Khong tim thay yeu cau nay", status.HTTP_404_NOT_FOUND)

    if req.status != "PENDING":
        return error_response(
            "STU_REQUEST_PROCESSED",
            f"Yeu cau nay da duoc xu ly o trang thai {req.status}",
            status.HTTP_400_BAD_REQUEST,
        )

    req.status = "APPROVED"
    req.reviewed_at = datetime.now(timezone.utc)
    req.reviewed_by = current_user.user_id
    if review_in and review_in.reviewed_note is not None:
        req.reviewed_note = review_in.reviewed_note

    student = db.query(Student).filter(Student.id == req.student_id).first()
    if student:
        if req.request_type == "RESERVE":
            student.status = "RESERVED"
        elif req.request_type == "DROP":
            student.status = "DROPPED"

    db.commit()
    db.refresh(req)
    payload = StatusRequestResponse.model_validate(req).model_dump()
    payload["student_name"] = student.full_name if student else None
    payload["student_code"] = student.student_code if student else None
    return success_response(payload, "Da duyet yeu cau va cap nhat trang thai hoc vien")


@router.put("/status-requests/{request_id}/reject")
def reject_request(
    request_id: int,
    review_in: Optional[StatusRequestReview] = Body(default=None),
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(require_roles("ADMIN", "STAFF")),
):
    """
    Giao vu hoac Quan tri vien tu choi yeu cau hoc vu (PENDING -> REJECTED).
    """
    req = db.query(StatusRequest).filter(StatusRequest.id == request_id).first()
    if not req:
        return error_response("STU_REQUEST_NOT_FOUND", "Khong tim thay yeu cau nay", status.HTTP_404_NOT_FOUND)

    if req.status != "PENDING":
        return error_response(
            "STU_REQUEST_PROCESSED",
            f"Yeu cau nay da duoc xu ly o trang thai {req.status}",
            status.HTTP_400_BAD_REQUEST,
        )

    req.status = "REJECTED"
    req.reviewed_at = datetime.now(timezone.utc)
    req.reviewed_by = current_user.user_id
    if review_in and review_in.reviewed_note is not None:
        req.reviewed_note = review_in.reviewed_note

    db.commit()
    db.refresh(req)
    student = db.query(Student).filter(Student.id == req.student_id).first()
    payload = StatusRequestResponse.model_validate(req).model_dump()
    payload["student_name"] = student.full_name if student else None
    payload["student_code"] = student.student_code if student else None
    return success_response(payload, "Da tu choi yeu cau hoc vu")
