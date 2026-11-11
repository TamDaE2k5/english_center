import time
from typing import Optional
import httpx
from fastapi import status
from ..config import settings
from ..deps import AppError, CurrentUser


def _get_with_retry(url: str, params: Optional[dict] = None) -> Optional[httpx.Response]:
    headers = {
        "X-Internal-Secret": settings.INTERNAL_SECRET_KEY,
        "Accept": "application/json",
    }
    for attempt in range(settings.HTTP_MAX_RETRIES):
        try:
            with httpx.Client(timeout=settings.HTTP_TIMEOUT_SECONDS) as client:
                return client.get(url, params=params, headers=headers)
        except httpx.RequestError:
            if attempt < settings.HTTP_MAX_RETRIES - 1:
                time.sleep(0.2 * (2**attempt))
    return None


def verify_student_studying(student_id: int) -> dict:
    """
    Gọi GET /api/student/internal/students/{student_id}
    Xác minh học viên tồn tại và có status == 'STUDYING'.
    Nếu không tồn tại hoặc khác STUDYING -> trả 404 LRN_STUDENT_NOT_FOUND theo bảng mã lỗi §9.4.
    """
    url = f"{settings.STUDENT_SERVICE_URL}/api/student/internal/students/{student_id}"
    resp = _get_with_retry(url)

    if resp is None:
        if not settings.STRICT_INTERNAL_VERIFY:
            return {"id": student_id, "status": "STUDYING"}
        raise AppError(
            status.HTTP_503_SERVICE_UNAVAILABLE,
            "LRN_STUDENT_NOT_FOUND",
            "Không thể kết nối tới Student Service để xác minh học viên",
        )

    if resp.status_code == status.HTTP_404_NOT_FOUND:
        raise AppError(
            status.HTTP_404_NOT_FOUND,
            "LRN_STUDENT_NOT_FOUND",
            f"Học viên ID {student_id} không tồn tại bên Student Service",
        )

    payload = resp.json() if resp.content else {}
    student_data = (
        payload.get("data")
        if isinstance(payload, dict) and "data" in payload
        else payload
    )
    if not isinstance(student_data, dict):
        raise AppError(
            status.HTTP_404_NOT_FOUND,
            "LRN_STUDENT_NOT_FOUND",
            f"Không đọc được thông tin học viên ID {student_id}",
        )

    stu_status = student_data.get("status")
    if stu_status and stu_status != "STUDYING":
        raise AppError(
            status.HTTP_404_NOT_FOUND,
            "LRN_STUDENT_NOT_FOUND",
            f"Học viên ID {student_id} hiện ở trạng thái '{stu_status}' (yêu cầu 'STUDYING')",
        )

    return student_data


def resolve_student_id_by_user_id(current_user: CurrentUser) -> Optional[int]:
    """
    Cầu nối R-ID-1 & INV-LRN-02:
    Gọi GET /api/student/internal/students?user_id={jwt.sub} để đổi admission.users.id -> student.students.id.
    Nếu user chưa được sync sang Student Service, trả None (để GET grades trả danh sách rỗng theo UC-LRN-02.2).
    """
    if current_user.student_id is not None:
        return current_user.student_id

    url = f"{settings.STUDENT_SERVICE_URL}/api/student/internal/students"
    resp = _get_with_retry(url, params={"user_id": current_user.user_id})

    if resp is None:
        if not settings.STRICT_INTERNAL_VERIFY:
            return current_user.user_id
        return None

    if resp.status_code == status.HTTP_404_NOT_FOUND:
        return None

    if resp.status_code == status.HTTP_200_OK:
        payload = resp.json() if resp.content else {}
        data = (
            payload.get("data")
            if isinstance(payload, dict) and "data" in payload
            else payload
        )
        if isinstance(data, dict):
            if "student_id" in data:
                return int(data["student_id"])
            if "id" in data:
                return int(data["id"])
            if "items" in data and isinstance(data["items"], list) and data["items"]:
                first = data["items"][0]
                return int(first.get("student_id") or first.get("id"))
        elif isinstance(data, list) and len(data) > 0:
            first = data[0]
            if isinstance(first, dict) and ("student_id" in first or "id" in first):
                return int(first.get("student_id") or first.get("id"))

    return None