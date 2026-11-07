import os
import base64 # Thêm thư viện này
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from jose import jwt, JWTError

# Lấy secret từ env
raw_secret = os.getenv("JWT_SECRET", "super_secret_jwt_key_english_center_2026_change_in_production")
# Ép sang Base64 giống cách Spring Boot thường làm
JWT_SECRET = base64.b64encode(raw_secret.encode()).decode() 

ALGORITHM = "HS256"
security = HTTPBearer()

def get_current_user(credentials: HTTPAuthorizationCredentials = Depends(security)):
    token = credentials.credentials
    try:
        # In ra để debug nếu cần
        # print(f"Verifying token with secret: {JWT_SECRET}")
        payload = jwt.decode(token, JWT_SECRET, algorithms=[ALGORITHM])
        return {
            "user_id": payload.get("sub"),
            "email": payload.get("email"),
            "role": payload.get("role"),
            "name": payload.get("name")
        }
    except JWTError as e:
        print(f"JWT ERROR: {str(e)}") # In ra lỗi chi tiết
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="AUTH_TOKEN_EXPIRED: Token không hợp lệ hoặc đã hết hạn",
        )