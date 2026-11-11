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


def verify_class_and_teacher(
    class_id: int,
    current_user: CurrentUser,
    forbid_completed: bool = False,
) -> dict:
    """
    Gọi GET /api/admission/internal/classes/{class_id}:
    1. Xác minh lớp tồn tại (404 -> LRN_CLASS_NOT_FOUND).
    2. Nếu current_user.role == 'TEACHER', kiểm tra teacher_id == current_user.user_id (INV-LRN-03 / INV-SEC-02).
    3. Nếu forbid_completed=True và lớp có status == 'COMPLETED', chặn sửa/nhập điểm theo mục 7 schema-spec.
    """
    url = f"{settings.ADMISSION_SERVICE_URL}/api/admission/internal/classes/{class_id}"
    resp = _get_with_retry(url)

    if resp is None:
        if not settings.STRICT_INTERNAL_VERIFY:
            return {
                "id": class_id,
                "teacher_id": current_user.user_id,
                "status": "IN_PROGRESS",
            }
        raise AppError(
            status.HTTP_503_SERVICE_UNAVAILABLE,
            "LRN_CLASS_NOT_FOUND",
            "Không thể kết nối tới Admission Service để xác minh lớp học",
        )

    if resp.status_code == status.HTTP_404_NOT_FOUND:
        raise AppError(
            status.HTTP_404_NOT_FOUND,
            "LRN_CLASS_NOT_FOUND",
            f"Lớp học ID {class_id} không tồn tại ở Admission Service",
        )

    payload = resp.json() if resp.content else {}
    class_data = (
        payload.get("data")
        if isinstance(payload, dict) and "data" in payload
        else payload
    )
    if not isinstance(class_data, dict):
        class_data = {}

    if current_user.role == "TEACHER":
        teacher_id = class_data.get("teacher_id")
        if teacher_id is None or int(teacher_id) != current_user.user_id:
            raise AppError(
                status.HTTP_403_FORBIDDEN,
                "LRN_FORBIDDEN_CLASS",
                "Giáo viên không được phân công phụ trách lớp học này (INV-LRN-03 / INV-SEC-02)",
            )

    if forbid_completed and class_data.get("status") == "COMPLETED":
        raise AppError(
            status.HTTP_403_FORBIDDEN,
            "LRN_FORBIDDEN_CLASS",
            "Lớp học đã kết thúc (COMPLETED), bảng điểm đã bị khóa",
        )

    return class_data


def get_student_enrolled_class_ids(user_id: int) -> Optional[set[int]]:
    """
    Lấy danh sách class_id mà học viên (admission.users.id) đã đăng ký APPROVED
    để giới hạn xem học liệu đúng lớp mình học (LRN-01).
    """
    url = f"{settings.ADMISSION_SERVICE_URL}/api/admission/internal/enrollments"
    resp = _get_with_retry(url, params={"student_id": user_id, "status": "APPROVED"})
    if resp is None or resp.status_code != status.HTTP_200_OK:
        return None

    payload = resp.json() if resp.content else {}
    data = payload.get("data") if isinstance(payload, dict) else payload
    items = data.get("items") if isinstance(data, dict) and "items" in data else data
    if not isinstance(items, list):
        return None

    class_ids = set()
    for item in items:
        if isinstance(item, dict) and item.get("class_id") is not None:
            if item.get("status", "APPROVED") == "APPROVED":
                class_ids.add(int(item["class_id"]))
    return class_ids