"""
auth.py - Xac thuc JWT Bearer Token va Xac thuc noi bo X-Internal-Secret cho Student Service.
Tuan thu quy tac bao mat INV-SEC-01, INV-SEC-02, INV-SEC-03.
"""

import hmac
import os
from dataclasses import dataclass
from typing import Optional

from fastapi import Depends, Header, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jose import ExpiredSignatureError, JWTError, jwt

# Doc cau hinh bao mat tu bien moi truong (INV-SEC-03)
JWT_SECRET = os.getenv(
    "JWT_SECRET",
    "super_secret_jwt_key_english_center_2026_change_in_production",
)
JWT_ALGORITHM = os.getenv("JWT_ALGORITHM", "HS256")
INTERNAL_SECRET_KEY = os.getenv("INTERNAL_SECRET_KEY", "dev-internal-secret-key")

bearer_scheme = HTTPBearer(auto_error=False)


class StudentException(Exception):
    """
    Ngoai le nghiep vu chuan cua Student Service.
    Duoc bat tap trung trong main.py de tra ve JSON chuan {success: false, error: {code, message}}.
    """

    def __init__(self, status_code: int, code: str, message: str):
        self.status_code = status_code
        self.code = code
        self.message = message
        super().__init__(message)


@dataclass
class CurrentUser:
    """
    Thong tin nguoi dung duoc giai ma tu JWT Access Token.
    """

    user_id: int
    email: str
    role: str
    full_name: Optional[str] = None
    raw_token: Optional[str] = None


def get_current_user(
    credentials: Optional[HTTPAuthorizationCredentials] = Depends(bearer_scheme),
) -> CurrentUser:
    """
    Xac thuc JWT Access Token tu header Authorization: Bearer <token>.

    Tham so:
        credentials: Thong tin Bearer token tu HTTP header.
    Tra ve:
        Doi tuong CurrentUser chua user_id, email, role, full_name.
    Ngoai le:
        StudentException(401) neu thieu token, token het han hoac sai chu ky.
    """
    if not credentials or credentials.scheme.lower() != "bearer" or not credentials.credentials:
        raise StudentException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            code="AUTH_UNAUTHORIZED",
            message="Yeu cau dang nhap (thieu Authorization Bearer token)",
        )

    token = credentials.credentials
    try:
        payload = jwt.decode(token, JWT_SECRET, algorithms=[JWT_ALGORITHM])
    except ExpiredSignatureError:
        raise StudentException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            code="AUTH_TOKEN_EXPIRED",
            message="Phien dang nhap da het han, vui long dang nhap lai",
        )
    except JWTError:
        raise StudentException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            code="AUTH_INVALID_TOKEN",
            message="Token xac thuc khong hop le",
        )

    # Tu choi neu client gui nham Refresh Token thay cho Access Token
    if payload.get("token_type") == "REFRESH":
        raise StudentException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            code="AUTH_INVALID_TOKEN",
            message="Khong the su dung Refresh Token de truy cap API",
        )

    raw_user_id = payload.get("user_id") if payload.get("user_id") is not None else payload.get("sub")
    try:
        user_id = int(raw_user_id)
    except (TypeError, ValueError):
        raise StudentException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            code="AUTH_INVALID_TOKEN",
            message="Dinh danh nguoi dung trong token khong hop le",
        )

    role = str(payload.get("role", "")).upper()
    if not role:
        raise StudentException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            code="AUTH_INVALID_TOKEN",
            message="Token thieu thong tin vai tro nguoi dung",
        )

    return CurrentUser(
        user_id=user_id,
        email=str(payload.get("email") or ""),
        role=role,
        full_name=payload.get("full_name") or payload.get("name"),
        raw_token=token,
    )


def require_roles(*allowed_roles: str):
    """
    Dependency kiem tra vai tro (RBAC) cua nguoi dung hien tai.

    Tham so:
        allowed_roles: Danh sach vai tro duoc phep truy cap (vd: 'ADMIN', 'STAFF').
    Tra ve:
        Ham dependency tra ve CurrentUser neu hop le, hoac nem loi 403 STU_FORBIDDEN.
    """
    allowed = {r.upper() for r in allowed_roles}

    def role_checker(user: CurrentUser = Depends(get_current_user)) -> CurrentUser:
        if user.role not in allowed:
            raise StudentException(
                status_code=status.HTTP_403_FORBIDDEN,
                code="STU_FORBIDDEN",
                message=f"Vai tro {user.role} khong co quyen thuc hien thao tac nay",
            )
        return user

    return role_checker


def verify_internal_secret(
    x_internal_secret: Optional[str] = Header(default=None, alias="X-Internal-Secret"),
) -> bool:
    """
    Xac thuc khoa bi mat giao tiep noi bo giua cac microservice qua header X-Internal-Secret.

    Tham so:
        x_internal_secret: Gia tri header X-Internal-Secret do service goi truyen sang.
    Tra ve:
        True neu hop le.
    Ngoai le:
        StudentException(403) neu thieu hoac sai khoa bi mat noi bo.
    """
    if not x_internal_secret or not INTERNAL_SECRET_KEY:
        raise StudentException(
            status_code=status.HTTP_403_FORBIDDEN,
            code="AUTH_INVALID_INTERNAL_SECRET",
            message="Thieu thong tin xac thuc noi bo X-Internal-Secret",
        )

    if not hmac.compare_digest(x_internal_secret, INTERNAL_SECRET_KEY):
        raise StudentException(
            status_code=status.HTTP_403_FORBIDDEN,
            code="AUTH_INVALID_INTERNAL_SECRET",
            message="Thong tin xac thuc noi bo X-Internal-Secret khong hop le",
        )
    return True
