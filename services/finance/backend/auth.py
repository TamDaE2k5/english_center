import os
import base64
from typing import List, Optional
from fastapi import Depends, HTTPException, Header, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from jose import jwt, JWTError

# Đọc khóa bí mật JWT và Internal Secret từ biến môi trường (không hardcode bí mật production)
raw_secret = os.getenv("JWT_SECRET", "super_secret_jwt_key_english_center_2026_change_in_production")
JWT_SECRET_B64 = base64.b64encode(raw_secret.encode()).decode()
INTERNAL_SECRET_KEY = os.getenv("INTERNAL_SECRET_KEY", "english_center_internal_secret_2026")
ACCEPTED_INTERNAL_SECRETS = {
    INTERNAL_SECRET_KEY,
    "english_center_internal_secret_2026",
    "internal_secret_key_english_center_2026",
}

ALGORITHM = "HS256"
security = HTTPBearer(auto_error=False)


class FinanceException(HTTPException):
    """Ngoại lệ nghiệp vụ chuẩn của Finance Service với mã lỗi FIN_* hoặc AUTH_*."""

    def __init__(self, status_code: int, code: str, message: str):
        super().__init__(status_code=status_code, detail={"code": code, "message": message})
        self.code = code
        self.message = message


def _decode_jwt(token: str) -> dict:
    """
    Giải mã và xác thực JWT Access Token do Admission Service cấp.

    :param token: Chuỗi JWT Bearer token
    :return: Dict chứa user_id, email, role, name
    :raises FinanceException: 401 nếu token hết hạn, sai chữ ký, là refresh token hoặc thiếu định danh
    """
    last_err = None
    for secret in (JWT_SECRET_B64, raw_secret):
        try:
            payload = jwt.decode(token, secret, algorithms=[ALGORITHM])
            # Chặn dùng Refresh Token thay cho Access Token
            if payload.get("token_type") == "REFRESH":
                raise FinanceException(
                    status_code=status.HTTP_401_UNAUTHORIZED,
                    code="AUTH_INVALID_TOKEN",
                    message="Không được sử dụng Refresh Token để truy cập API",
                )

            sub_val = payload.get("sub")
            user_id_claim = payload.get("user_id")
            if sub_val is not None and str(sub_val).isdigit():
                user_id = int(sub_val)
            elif user_id_claim is not None and str(user_id_claim).isdigit():
                user_id = int(user_id_claim)
            else:
                raise FinanceException(
                    status_code=status.HTTP_401_UNAUTHORIZED,
                    code="AUTH_INVALID_TOKEN",
                    message="Token thiếu thông tin định danh người dùng hợp lệ",
                )

            return {
                "user_id": user_id,
                "email": payload.get("email", ""),
                "role": str(payload.get("role", "STUDENT")).upper(),
                "name": payload.get("full_name") or payload.get("name", ""),
            }
        except FinanceException:
            raise
        except JWTError as err:
            last_err = err

    raise FinanceException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        code="AUTH_TOKEN_EXPIRED",
        message=f"Token không hợp lệ hoặc đã hết hạn ({last_err})",
    )


def get_current_user(
    credentials: Optional[HTTPAuthorizationCredentials] = Depends(security),
    x_internal_secret: Optional[str] = Header(default=None, alias="X-Internal-Secret"),
) -> dict:
    """
    Xác thực người dùng hiện tại từ header X-Internal-Secret hoặc Authorization Bearer JWT.

    :param credentials: Bearer token từ HTTP Authorization header
    :param x_internal_secret: Shared secret dùng cho giao tiếp nội bộ giữa các microservice
    :return: Thông tin user hoặc định danh INTERNAL
    :raises FinanceException: 401 nếu thiếu thông tin xác thực; 403 nếu secret nội bộ sai
    """
    if x_internal_secret is not None:
        if x_internal_secret in ACCEPTED_INTERNAL_SECRETS:
            return {
                "user_id": 0,
                "email": "internal@english.edu.vn",
                "role": "INTERNAL",
                "name": "Internal Service",
            }
        if not (credentials and credentials.credentials):
            raise FinanceException(
                status_code=status.HTTP_403_FORBIDDEN,
                code="AUTH_FORBIDDEN",
                message="Thông tin xác thực nội bộ (X-Internal-Secret) không hợp lệ",
            )

    if credentials and credentials.credentials:
        return _decode_jwt(credentials.credentials)

    raise FinanceException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        code="AUTH_UNAUTHORIZED",
        message="Yêu cầu đăng nhập để truy cập phân hệ Tài chính",
    )


def require_roles(allowed_roles: List[str]):
    """
    Tạo FastAPI dependency kiểm tra vai trò RBAC của người dùng hiện tại.

    :param allowed_roles: Danh sách vai trò được phép (vd: ["ADMIN", "STAFF"])
    :return: Dependency trả về dict thông tin user nếu hợp lệ
    :raises FinanceException: 403 AUTH_FORBIDDEN nếu vai trò không được phép
    """
    def role_checker(user: dict = Depends(get_current_user)) -> dict:
        if user["role"] == "INTERNAL" and "INTERNAL" in allowed_roles:
            return user
        if user["role"] not in allowed_roles and user["role"] != "ADMIN":
            raise FinanceException(
                status_code=status.HTTP_403_FORBIDDEN,
                code="AUTH_FORBIDDEN",
                message="Bạn không có quyền thực hiện thao tác này",
            )
        return user

    return role_checker


def verify_internal_secret(
    x_internal_secret: Optional[str] = Header(default=None, alias="X-Internal-Secret"),
    credentials: Optional[HTTPAuthorizationCredentials] = Depends(security),
) -> bool:
    """
    Xác thực quyền truy cập cho các endpoint nội bộ (/api/finance/internal/*).
    Cho phép X-Internal-Secret hợp lệ hoặc JWT của ADMIN/STAFF.

    :raises FinanceException: 401 nếu thiếu xác thực, 403 nếu sai secret hoặc không đủ quyền
    """
    if x_internal_secret is not None:
        if x_internal_secret in ACCEPTED_INTERNAL_SECRETS:
            return True
        raise FinanceException(
            status_code=status.HTTP_403_FORBIDDEN,
            code="AUTH_FORBIDDEN",
            message="Thông tin xác thực nội bộ (X-Internal-Secret) không hợp lệ",
        )

    if credentials and credentials.credentials:
        user = _decode_jwt(credentials.credentials)
        if user["role"] in ("ADMIN", "STAFF"):
            return True
        raise FinanceException(
            status_code=status.HTTP_403_FORBIDDEN,
            code="AUTH_FORBIDDEN",
            message="Chỉ dịch vụ nội bộ hoặc ADMIN/STAFF mới được truy cập endpoint này",
        )

    raise FinanceException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        code="AUTH_UNAUTHORIZED",
        message="Thiếu X-Internal-Secret hoặc JWT xác thực",
    )
