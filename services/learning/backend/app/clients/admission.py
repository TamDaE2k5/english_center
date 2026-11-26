import time
from typing import Optional
import httpx
from fastapi import status
from ..config import settings
from ..deps import AppError, CurrentUser


def _get_with_retry(
    url: str,
    params: Optional[dict] = None,
    extra_headers: Optional[dict] = None,
) -> Optional[httpx.Response]:
    """
    Gửi HTTP GET tới service nội bộ với cơ chế retry tối đa HTTP_MAX_RETRIES lần (backoff lũy thừa).

    - Đầu vào: url đích, params query, extra_headers bổ sung (ví dụ Authorization).
    - Trả về: httpx.Response hoặc None nếu lỗi kết nối mạng sau các lần thử.
    """
    headers = {
        "X-Internal-Secret": settings.INTERNAL_SECRET_KEY,
        "Accept": "application/json",
    }
    if extra_headers:
        headers.update(extra_headers)

    for attempt in range(settings.HTTP_MAX_RETRIES):
        try:
            with httpx.Client(timeout=settings.HTTP_TIMEOUT_SECONDS) as client:
                return client.get(url, params=params, headers=headers)
        except httpx.RequestError:
            if attempt < settings.HTTP_MAX_RETRIES - 1:
                time.sleep(0.2 * (2**attempt))
    return None


def _unwrap_data(resp: httpx.Response):
    """Giải nén trường data từ cấu trúc chuẩn {success, data, message} của hệ thống."""
    payload = resp.json() if resp.content else {}
    if isinstance(payload, dict) and "data" in payload:
        return payload.get("data")
    return payload


def _is_missing_internal_route(resp: httpx.Response) -> bool:
    """
    Nhận diện trường hợp endpoint /internal/* chưa được triển khai ở Admission Service
    (Spring Boot trả 404/405 hoặc 500 NoResourceFoundException) để fallback sang API công khai tương ứng.
    """
    if resp.status_code in (status.HTTP_404_NOT_FOUND, status.HTTP_405_METHOD_NOT_ALLOWED, status.HTTP_501_NOT_IMPLEMENTED):
        try:
            body = resp.json()
            err_code = (body.get("error") or {}).get("code", "") if isinstance(body, dict) else ""
            # Nếu Admission đã trả mã lỗi nghiệp vụ cụ thể (ADM_CLASS_NOT_FOUND) thì KHÔNG fallback
            if err_code == "ADM_CLASS_NOT_FOUND":
                return False
        except ValueError:
            pass
        return True
    if resp.status_code >= 500:
        return True
    return False


def verify_class_and_teacher(
    class_id: int,
    current_user: CurrentUser,
    forbid_completed: bool = False,
) -> dict:
    """
    Xác minh lớp học tồn tại và kiểm tra quyền phụ trách của giảng viên (INV-LRN-03 / INV-SEC-02).

    Các bước thực hiện:
    1. Gọi GET /api/admission/internal/classes/{class_id} kèm X-Internal-Secret.
    2. Nếu endpoint nội bộ chưa có (404/500), fallback sang GET /api/admission/classes/{class_id}.
    3. Nếu lớp không tồn tại (404) -> ném 404 LRN_CLASS_NOT_FOUND.
    4. Nếu Admission trả lỗi khác hoặc mất kết nối -> ném 503 LRN_ADMISSION_UNAVAILABLE (khi strict=true).
    5. Nếu current_user.role == 'TEACHER', đối chiếu teacher_id == current_user.user_id.
    6. Nếu forbid_completed=True và lớp có status == 'COMPLETED', chặn sửa/nhập điểm.
    """
    internal_url = f"{settings.ADMISSION_SERVICE_URL}/api/admission/internal/classes/{class_id}"
    resp = _get_with_retry(internal_url)

    # Bước 2: Fallback sang public endpoint nếu internal endpoint chưa được Admission triển khai
    if resp is not None and _is_missing_internal_route(resp):
        public_url = f"{settings.ADMISSION_SERVICE_URL}/api/admission/classes/{class_id}"
        resp = _get_with_retry(public_url)

    if resp is None:
        if not settings.STRICT_INTERNAL_VERIFY:
            return {
                "id": class_id,
                "teacher_id": current_user.user_id,
                "status": "IN_PROGRESS",
            }
        raise AppError(
            status.HTTP_503_SERVICE_UNAVAILABLE,
            "LRN_ADMISSION_UNAVAILABLE",
            "Không thể kết nối tới Admission Service để xác minh lớp học",
        )

    if resp.status_code == status.HTTP_404_NOT_FOUND:
        raise AppError(
            status.HTTP_404_NOT_FOUND,
            "LRN_CLASS_NOT_FOUND",
            f"Lớp học ID {class_id} không tồn tại ở Admission Service",
        )

    if resp.status_code >= 400:
        raise AppError(
            status.HTTP_503_SERVICE_UNAVAILABLE,
            "LRN_ADMISSION_UNAVAILABLE",
            f"Admission Service trả về lỗi HTTP {resp.status_code} khi xác minh lớp học",
        )

    class_data = _unwrap_data(resp)
    if not isinstance(class_data, dict):
        class_data = {}

    # Bước 5: Kiểm tra ranh giới giáo viên (INV-LRN-03 / INV-SEC-02)
    if current_user.role == "TEACHER":
        teacher_id = class_data.get("teacher_id")
        if teacher_id is None or int(teacher_id) != current_user.user_id:
            raise AppError(
                status.HTTP_403_FORBIDDEN,
                "LRN_FORBIDDEN_CLASS",
                "Giáo viên không được phân công phụ trách lớp học này (INV-LRN-03 / INV-SEC-02)",
            )

    # Bước 6: Chặn nhập/sửa điểm khi lớp đã kết thúc
    if forbid_completed and class_data.get("status") == "COMPLETED":
        raise AppError(
            status.HTTP_403_FORBIDDEN,
            "LRN_FORBIDDEN_CLASS",
            "Lớp học đã kết thúc (COMPLETED), bảng điểm đã bị khóa",
        )

    return class_data


def get_teacher_class_ids(teacher_user_id: int) -> Optional[set[int]]:
    """
    Lấy tập hợp class_id mà giảng viên (teacher_user_id) được phân công phụ trách.
    Dùng để cô lập dữ liệu khi TEACHER tra cứu bảng điểm/báo cáo mà không truyền class_id.
    """
    resp = _get_with_retry(f"{settings.ADMISSION_SERVICE_URL}/api/admission/classes")
    if resp is None or resp.status_code != status.HTTP_200_OK:
        return None

    data = _unwrap_data(resp)
    items = data.get("items") if isinstance(data, dict) and "items" in data else data
    if not isinstance(items, list):
        return None

    class_ids: set[int] = set()
    for item in items:
        if isinstance(item, dict) and item.get("id") is not None:
            t_id = item.get("teacher_id")
            if t_id is not None and int(t_id) == teacher_user_id:
                class_ids.add(int(item["id"]))
    return class_ids


def get_student_enrolled_class_ids(current_user: CurrentUser) -> Optional[set[int]]:
    """
    Lấy danh sách class_id mà học viên (admission.users.id) đã đăng ký APPROVED
    để giới hạn xem học liệu đúng lớp mình học (LRN-01).

    Ưu tiên gọi /api/admission/internal/enrollments; nếu endpoint nội bộ chưa có,
    fallback sang /api/admission/enrollments/my bằng JWT của chính học viên.
    """
    url = f"{settings.ADMISSION_SERVICE_URL}/api/admission/internal/enrollments"
    resp = _get_with_retry(url, params={"student_id": current_user.user_id, "status": "APPROVED"})

    if (resp is None or resp.status_code != status.HTTP_200_OK) and current_user.raw_token:
        my_url = f"{settings.ADMISSION_SERVICE_URL}/api/admission/enrollments/my"
        resp = _get_with_retry(
            my_url,
            extra_headers={"Authorization": f"Bearer {current_user.raw_token}"},
        )

    if resp is None or resp.status_code != status.HTTP_200_OK:
        return None

    data = _unwrap_data(resp)
    items = data.get("items") if isinstance(data, dict) and "items" in data else data
    if not isinstance(items, list):
        return None

    class_ids: set[int] = set()
    for item in items:
        if isinstance(item, dict) and item.get("class_id") is not None:
            if item.get("status", "APPROVED") == "APPROVED":
                class_ids.add(int(item["class_id"]))
    return class_ids
