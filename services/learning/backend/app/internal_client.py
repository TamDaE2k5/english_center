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
    for attempt in range(3):
        try:
            with httpx.Client(timeout=5.0) as client:
                return client.get(url, params=params, headers=headers)
        except httpx.RequestError:
            if attempt < 2:
                time.sleep(0.2 * (2 ** attempt))
    return None


def verify_class_and_teacher(class_id: int, current_user: CurrentUser) -> Optional[dict]:
    """
    Gọi GET /api/admission/internal/classes/{class_id} để:
    1. Xác minh lớp tồn tại.
    2. Nếu current_user.role == 'TEACHER', đối chiếu teacher_id == current_user.user_id
       (INV-LRN-03 / INV-SEC-02).
    """
    url = f"{ADMISSION_SERVICE_URL}/api/admission/internal/classes/{class_id}"
    resp = _request_with_retry(url)

    if resp is None:
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


def verify_student_studying(student_id: int) -> dict:
    """
    Gọi GET /api/student/internal/students/{student_id} xác minh học viên tồn tại và đang STUDYING.
    Trả về dict student_data (bao gồm 'user_id' nếu Student Service cung cấp).
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
    return student_data if isinstance(student_data, dict) else {"id": student_id, "status": "STUDYING"}


def verify_student_enrolled_in_class(
    student_id: int,
    class_id: int,
    student_data: Optional[dict] = None,
) -> dict:
    """
    Xác minh học viên (student.students.id) có enrollment APPROVED trong lớp.

    Do Admission là chủ sở hữu dữ liệu enrollments (INV-SYS-02 / Data Ownership),
    Learning không truy cập DB trực tiếp mà gọi internal API:
        GET /api/admission/internal/enrollments/check?user_id={uid}&class_id={cid}

    Cần map student.students.id → admission.users.id qua cột 'user_id' trong student-data
    (do Student Service cung cấp ở endpoint /internal/students/{id}).

    Phụ thuộc:
        - Student Service phải trả 'user_id' trong response student data.
        - Admission Service phải cung cấp endpoint /internal/enrollments/check.
    """
    if student_data is None:
        student_data = verify_student_studying(student_id)

    user_id = student_data.get("user_id") if isinstance(student_data, dict) else None

    if user_id is None:
        if not STRICT_INTERNAL_VERIFY:
            # Dev mode: không thể map, bỏ qua để UI local hoạt động
            return {"enrolled": True, "status": "APPROVED"}
        raise AppError(
            status.HTTP_422_UNPROCESSABLE_ENTITY,
            "LRN_STUDENT_NO_USER_LINK",
            f"Học viên {student_id} chưa được liên kết với tài khoản người dùng (thiếu user_id)",
        )

    adm_resp = _request_with_retry(
        f"{ADMISSION_SERVICE_URL}/api/admission/internal/enrollments/check",
        params={"user_id": int(user_id), "class_id": class_id},
    )

    if adm_resp is None:
        if not STRICT_INTERNAL_VERIFY:
            return {"enrolled": True, "status": "APPROVED"}
        raise AppError(
            status.HTTP_503_SERVICE_UNAVAILABLE,
            "LRN_ADMISSION_UNAVAILABLE",
            "Không thể kết nối tới Admission Service để xác minh enrollment",
        )

    if adm_resp.status_code in (status.HTTP_404_NOT_FOUND, status.HTTP_405_METHOD_NOT_ALLOWED):
        # Endpoint chưa được Admission triển khai
        if not STRICT_INTERNAL_VERIFY:
            return {"enrolled": True, "status": "APPROVED"}
        raise AppError(
            status.HTTP_503_SERVICE_UNAVAILABLE,
            "LRN_ENROLLMENT_CHECK_UNAVAILABLE",
            "Admission Service chưa hỗ trợ kiểm tra enrollment",
        )

    payload = adm_resp.json() if adm_resp.content else {}
    data = payload.get("data") if isinstance(payload, dict) and "data" in payload else payload
    enrolled = bool(data.get("enrolled")) if isinstance(data, dict) else False

    if not enrolled:
        raise AppError(
            status.HTTP_422_UNPROCESSABLE_ENTITY,
            "LRN_STUDENT_NOT_ENROLLED",
            f"Học viên {student_id} không có enrollment APPROVED trong lớp {class_id}",
        )
    return data if isinstance(data, dict) else {"enrolled": True}


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

    # KHÔNG fallback về user_id (trộn namespace users.id vs students.id — vi phạm R-ID-1)
    raise AppError(
        status.HTTP_503_SERVICE_UNAVAILABLE,
        "LRN_STUDENT_RESOLVE_FAILED",
        "Không thể xác định hồ sơ học viên từ tài khoản đăng nhập. Vui lòng thử lại sau.",
    )