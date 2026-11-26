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
    Gửi HTTP GET tới Student Service với retry tối đa HTTP_MAX_RETRIES lần (backoff lũy thừa).

    - Đầu vào: url đích, params query, extra_headers bổ sung.
    - Trả về: httpx.Response hoặc None nếu không thể kết nối mạng.
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
    """Giải nén trường data từ response chuẩn của Student Service."""
    payload = resp.json() if resp.content else {}
    if isinstance(payload, dict) and "data" in payload:
        return payload.get("data")
    return payload


def _is_missing_internal_route(resp: httpx.Response) -> bool:
    """
    Kiểm tra xem Student Service trả 404/405 do chưa định nghĩa route /internal/students*
    (FastAPI trả {"detail": "Not Found"}) hay 404 nghiệp vụ ({"success": false, "error": ...}).
    """
    if resp.status_code == status.HTTP_405_METHOD_NOT_ALLOWED:
        return True
    if resp.status_code == status.HTTP_404_NOT_FOUND:
        try:
            body = resp.json()
            if isinstance(body, dict) and "error" in body:
                return False
        except ValueError:
            pass
        return True
    return False


def _fetch_students_list(current_user: Optional[CurrentUser] = None) -> Optional[list]:
    """Fallback lấy danh sách học viên từ GET /api/student/students khi route internal chưa sẵn sàng."""
    extra_headers = {}
    if current_user and current_user.raw_token:
        extra_headers["Authorization"] = f"Bearer {current_user.raw_token}"

    resp = _get_with_retry(
        f"{settings.STUDENT_SERVICE_URL}/api/student/students",
        extra_headers=extra_headers or None,
    )
    if resp is None or resp.status_code != status.HTTP_200_OK:
        return None
    data = _unwrap_data(resp)
    items = data.get("items") if isinstance(data, dict) and "items" in data else data
    return items if isinstance(items, list) else None


def verify_student_studying(
    student_id: int,
    current_user: Optional[CurrentUser] = None,
) -> dict:
    """
    Xác minh hồ sơ học viên tồn tại và đang ở trạng thái STUDYING bên Student Service.

    Các bước thực hiện:
    1. Gọi GET /api/student/internal/students/{student_id} kèm X-Internal-Secret.
    2. Nếu route /internal/* chưa tồn tại ở Student Service, fallback tra cứu từ GET /api/student/students.
    3. Nếu không tìm thấy hoặc trạng thái != 'STUDYING' -> ném 404 LRN_STUDENT_NOT_FOUND.
    """
    url = f"{settings.STUDENT_SERVICE_URL}/api/student/internal/students/{student_id}"
    resp = _get_with_retry(url)

    student_data = None
    if resp is None:
        if not settings.STRICT_INTERNAL_VERIFY:
            return {"id": student_id, "status": "STUDYING"}
        raise AppError(
            status.HTTP_503_SERVICE_UNAVAILABLE,
            "LRN_STUDENT_SERVICE_UNAVAILABLE",
            "Không thể kết nối tới Student Service để xác minh học viên",
        )

    if _is_missing_internal_route(resp):
        students_list = _fetch_students_list(current_user)
        if students_list is None:
            raise AppError(
                status.HTTP_503_SERVICE_UNAVAILABLE,
                "LRN_STUDENT_SERVICE_UNAVAILABLE",
                "Không thể tra cứu danh sách học viên từ Student Service",
            )
        for item in students_list:
            if isinstance(item, dict) and item.get("id") is not None and int(item["id"]) == student_id:
                student_data = item
                break
        if student_data is None:
            raise AppError(
                status.HTTP_404_NOT_FOUND,
                "LRN_STUDENT_NOT_FOUND",
                f"Học viên ID {student_id} không tồn tại bên Student Service",
            )
    elif resp.status_code == status.HTTP_404_NOT_FOUND:
        raise AppError(
            status.HTTP_404_NOT_FOUND,
            "LRN_STUDENT_NOT_FOUND",
            f"Học viên ID {student_id} không tồn tại bên Student Service",
        )
    elif resp.status_code >= 400:
        raise AppError(
            status.HTTP_503_SERVICE_UNAVAILABLE,
            "LRN_STUDENT_SERVICE_UNAVAILABLE",
            f"Student Service trả về lỗi HTTP {resp.status_code}",
        )
    else:
        student_data = _unwrap_data(resp)

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
    Ánh xạ admission.users.id (jwt.sub) -> student.students.id thông qua Student Service.

    Các bước thực hiện:
    1. Nếu JWT đã có claim student_id thì dùng trực tiếp.
    2. Gọi GET /api/student/internal/students?user_id={user_id}.
    3. Nếu route internal chưa có, fallback sang GET /api/student/students và lọc theo user_id.
    4. Tuyệt đối KHÔNG trả về current_user.user_id làm student_id (tránh trộn namespace R-ID-1).
    """
    if current_user.student_id is not None:
        return current_user.student_id

    url = f"{settings.STUDENT_SERVICE_URL}/api/student/internal/students"
    resp = _get_with_retry(url, params={"user_id": current_user.user_id})

    if resp is not None and resp.status_code == status.HTTP_200_OK:
        data = _unwrap_data(resp)
        if isinstance(data, dict):
            if "student_id" in data and data["student_id"] is not None:
                return int(data["student_id"])
            if "id" in data and data["id"] is not None:
                return int(data["id"])
            if "items" in data and isinstance(data["items"], list) and data["items"]:
                first = data["items"][0]
                if isinstance(first, dict) and (first.get("student_id") or first.get("id")):
                    return int(first.get("student_id") or first.get("id"))
        elif isinstance(data, list) and len(data) > 0:
            first = data[0]
            if isinstance(first, dict) and (first.get("student_id") or first.get("id")):
                return int(first.get("student_id") or first.get("id"))
        return None

    # Nếu endpoint /internal/students chưa được triển khai -> fallback tra cứu theo user_id
    if resp is None or _is_missing_internal_route(resp):
        students_list = _fetch_students_list(current_user)
        if students_list:
            for item in students_list:
                if (
                    isinstance(item, dict)
                    and item.get("user_id") is not None
                    and int(item["user_id"]) == current_user.user_id
                    and item.get("id") is not None
                ):
                    return int(item["id"])

    return None
