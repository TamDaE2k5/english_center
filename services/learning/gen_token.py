# File: gen_token.py
import jwt

SECRET = "super_secret_jwt_key_english_center_2026_change_in_production"

def create_token(user_id, role, student_id=None):
    payload = {
        "sub": str(user_id),
        "user_id": user_id,
        "role": role,
        "email": f"user{user_id}@test.com"
    }
    if student_id:
        payload["student_id"] = student_id
    return jwt.encode(payload, SECRET, algorithm="HS256")

print("=== ADMIN TOKEN ===")
print(create_token(user_id=1, role="ADMIN"))

print("\n=== TEACHER TOKEN ===")
print(create_token(user_id=2, role="TEACHER"))

print("\n=== STUDENT TOKEN (student_id=99) ===")
print(create_token(user_id=10, role="STUDENT", student_id=99))