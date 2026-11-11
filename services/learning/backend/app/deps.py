from dataclasses import dataclass
from typing import Optional
import jwt
from fastapi import Depends, Header, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from .config import settings

bearer_scheme = HTTPBearer(auto_error=False)


class AppError(Exception):
    def __init__(self, status_code: int, code: str, message: str):
        self.status_code = status_code
        self.code = code
        self.message = message
        super().__init__(message)


@dataclass
class CurrentUser:
    user_id: int
    email: str
    role: str
    full_name: Optional[str] = None
    student_id: Optional[int] = None


def get_current_user(
    credentials: Optional[HTTPAuthorizationCredentials] = Depends(bearer_scheme),
) -> CurrentUser:
    if not credentials or credentials.scheme.lower() != "bearer":
        raise AppError(
            status.HTTP_401_UNAUTHORIZED,
            "AUTH_UNAUTHORIZED",
            "Thiếu hoặc sai định dạng Authorization Bearer token",
        )

    token = credentials.credentials
    try:
        payload = jwt.decode(
            token,
            settings.JWT_SECRET,
            algorithms=[settings.JWT_ALGORITHM],
        )
    except jwt.ExpiredSignatureError:
        raise AppError(
            status.HTTP_401_UNAUTHORIZED,
            "AUTH_TOKEN_EXPIRED",
            "Phiên đăng nhập đã hết hạn",
        )
    except jwt.InvalidTokenError:
        raise AppError(
            status.HTTP_401_UNAUTHORIZED,
            "AUTH_INVALID_TOKEN",
            "Token xác thực không hợp lệ",
        )

    raw_uid = payload.get("sub") if payload.get("sub") is not None else payload.get("user_id")
    try:
        user_id = int(raw_uid)
    except (TypeError, ValueError):
        raise AppError(
            status.HTTP_401_UNAUTHORIZED,
            "AUTH_INVALID_TOKEN",
            "Định danh người dùng (sub) trong JWT không hợp lệ",
        )

    role = str(payload.get("role", "")).upper()
    raw_student_id = payload.get("student_id")
    student_id = int(raw_student_id) if raw_student_id is not None else None

    return CurrentUser(
        user_id=user_id,
        email=str(payload.get("email", "")),
        role=role,
        full_name=payload.get("name") or payload.get("full_name"),
        student_id=student_id,
    )


def require_role(*allowed_roles: str):
    allowed = {r.upper() for r in allowed_roles}

    def dependency(user: CurrentUser = Depends(get_current_user)) -> CurrentUser:
        if user.role not in allowed:
            raise AppError(
                status.HTTP_403_FORBIDDEN,
                "LRN_FORBIDDEN_CLASS",
                f"Vai trò {user.role} không có quyền thực hiện thao tác này",
            )
        return user

    return dependency


def verify_internal_secret(
    x_internal_secret: Optional[str] = Header(default=None, alias="X-Internal-Secret"),
):
    if not x_internal_secret or x_internal_secret != settings.INTERNAL_SECRET_KEY:
        raise AppError(
            status.HTTP_403_FORBIDDEN,
            "AUTH_INVALID_INTERNAL_SECRET",
            "Header X-Internal-Secret không hợp lệ hoặc bị thiếu",
        )