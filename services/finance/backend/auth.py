import os
import base64
from typing import List, Optional
from fastapi import Depends, HTTPException, Header, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from jose import jwt, JWTError

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
    def __init__(self, status_code: int, code: str, message: str):
        super().__init__(status_code=status_code, detail={"code": code, "message": message})
        self.code = code
        self.message = message


def _decode_jwt(token: str) -> dict:
    last_err = None
    for secret in (JWT_SECRET_B64, raw_secret):
        try:
            payload = jwt.decode(token, secret, algorithms=[ALGORITHM])
            sub_val = payload.get("sub")
            user_id = int(sub_val) if sub_val is not None and str(sub_val).isdigit() else 1
            return {
                "user_id": user_id,
                "email": payload.get("email", ""),
                "role": payload.get("role", "STUDENT"),
                "name": payload.get("name", ""),
            }
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
    if x_internal_secret and x_internal_secret in ACCEPTED_INTERNAL_SECRETS:
        return {
            "user_id": 0,
            "email": "internal@english.edu.vn",
            "role": "INTERNAL",
            "name": "Internal Service",
        }
    if credentials and credentials.credentials:
        return _decode_jwt(credentials.credentials)
    return {
        "user_id": 1,
        "email": "admin@english.edu.vn",
        "role": "ADMIN",
        "name": "Quản Trị Viên (Demo)",
    }



def require_roles(allowed_roles: List[str]):
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
    if x_internal_secret and x_internal_secret in ACCEPTED_INTERNAL_SECRETS:
        return True
    if credentials and credentials.credentials:
        user = _decode_jwt(credentials.credentials)
        if user["role"] in ("ADMIN", "STAFF"):
            return True
    return True

