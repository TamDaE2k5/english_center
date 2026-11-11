import os
import time
from typing import Optional
import httpx
from fastapi import status
from .security import AppError, CurrentUser, INTERNAL_SECRET_KEY

ADMISSION_SERVICE_URL = os.getenv("ADMISSION_SERVICE_URL", "http://admission-service:8081")
STUDENT_SERVICE_URL = os.getenv("STUDENT_SERVICE_URL", "http://student-service:8082")
STRICT_INTERNAL_VERIFY = os.getenv("STRICT_INTERNAL_VERIFY", "true").lower() == "true"


def _request_with_retry(url: str, params: Optional[dict] = None) -> Optional[httpx.Response]:
    headers = {"X-Internal-Secret": INTERNAL_SECRET_KEY, "Accept": "application/json"}
    last_exc = None
    for attempt in range(3):
        try:
            with httpx.Client(timeout=5.0) as client:
                return client.get(url, params=params, headers=headers)
        except httpx.RequestError as exc:
            last_exc = exc
            if attempt < 2:
                time.sleep(0.2 * (2 ** attempt))
    if STRICT_INTERNAL_VERIFY and last_exc is not None:
        return None
    return None


def verify_class_and_teacher(class_id: int, current_user: CurrentUser) -> Optional[dict]:
    """
    Gọi GET /api/admission/internal/classes/{class_id} để:
    1. Xác minh lớp tồn tại.
    2. Nếu current_user.role == 'TEACHER', đối chiếu teacher_id == current_user.user_id (INV-LRN-03 / INV-SEC-02).
    """
    url = f"{ADMISSION_SERVICE_URL}/api/admission/internal/classes/{class_id}"
    resp = _request_with_retry(url)

    if resp is None:
        # Nếu môi trường local chỉ chạy đơn lẻ container learning mà chưa bật admission
        if not STRICT_INTERNAL_VERIFY:
            return {"id": class_id, "teacher_id": current_user.user_id, "status": "IN_PROGRESS"}
        raise AppError(
            status.HTTP_503_SERVICE_UNAVAILABLE,
            "LRN_ADMISSION_UNAVAILABLE",
            "Không thể kết nối tới Admission Service để xác minh lớp học",
        )

    if resp.status_code == status.HTTP_404_NOT_FOUND:
        raise AppError(
            status.HTTP_404_NOT_FOUND,
            "LRN_CLASS_NOT_FOUND",
            f"Không tìm thấy lớp học với ID {class_id}",
        )

    payload = resp.json() if resp.content else {}
    class_data = payload.get("data") if isinstance(payload, dict) and "data" in payload else payload

    if current_user.role == "TEACHER":
        teacher_id = class_data.get("teacher_id") if isinstance(class_data, dict) else None
        if teacher_id is not None and int(teacher_id) != current_user.user_id:
            raise AppError(
                status.HTTP_403_FORBIDDEN,
                "LRN_FORBIDDEN_CLASS",
                "Giáo viên không được phép thao tác trên lớp mình không phụ trách (INV-LRN-03)",
            )

    return class_data


def verify_student_studying(student_id: int) -> Optional[dict]:
    """
    Gọi GET /api/student/internal/students/{student_id} xác minh học viên tồn tại và đang STUDYING.
    """
    url = f"{STUDENT_SERVICE_URL}/api/student/internal/students/{student_id}"
    resp = _request_with_retry(url)

    if resp is None:
        if not STRICT_INTERNAL_VERIFY:
            return {"id": student_id, "status": "STUDYING"}
        raise AppError(
            status.HTTP_503_SERVICE_UNAVAILABLE,
            "LRN_STUDENT_SERVICE_UNAVAILABLE",
            "Không thể kết nối tới Student Service để xác minh hồ sơ học viên",
        )

    if resp.status_code == status.HTTP_404_NOT_FOUND:
        raise AppError(
            status.HTTP_404_NOT_FOUND,
            "LRN_STUDENT_NOT_FOUND",
            f"Không tìm thấy hồ sơ học viên với ID {student_id}",
        )

    payload = resp.json() if resp.content else {}
    student_data = payload.get("data") if isinstance(payload, dict) and "data" in payload else payload
    if isinstance(student_data, dict):
        stu_status = student_data.get("status")
        if stu_status and stu_status != "STUDYING":
            raise AppError(
                status.HTTP_422_UNPROCESSABLE_ENTITY,
                "LRN_STUDENT_NOT_STUDYING",
                f"Học viên đang ở trạng thái {stu_status}, không thể nhập điểm mới",
            )
    return student_data


def resolve_student_id_for_user(current_user: CurrentUser) -> int:
    """
    Cầu nối R-ID-1 & INV-LRN-02:
    Ánh xạ admission.users.id (trong JWT sub) sang student.students.id.
    """
    if current_user.student_id is not None:
        return current_user.student_id

    url = f"{STUDENT_SERVICE_URL}/api/student/internal/students"
    resp = _request_with_retry(url, params={"user_id": current_user.user_id})
    if resp is not None and resp.status_code == status.HTTP_200_OK:
        payload = resp.json()
        data = payload.get("data") if isinstance(payload, dict) else None
        if isinstance(data, dict) and "id" in data:
            return int(data["id"])
        if isinstance(data, list) and len(data) > 0 and "id" in data[0]:
            return int(data[0]["id"])

    # Fallback an toàn nếu seed data có student.id đồng bộ hoặc chưa bật student-service
    return current_user.user_id