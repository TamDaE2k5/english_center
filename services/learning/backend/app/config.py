import os

# Xây dựng URL kết nối từ các biến môi trường mà docker-compose truyền vào
DB_HOST = os.getenv("DB_HOST", "postgres")
DB_PORT = os.getenv("DB_PORT", "5432")
DB_NAME = os.getenv("DB_NAME", "english_center")
DB_USER = os.getenv("DB_USER", "postgres")
DB_PASSWORD = os.getenv("DB_PASSWORD", "postgres")

DATABASE_URL = os.getenv(
    "DATABASE_URL",
    f"postgresql://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}"
)

JWT_SECRET = os.getenv("JWT_SECRET", "super_secret_jwt_key_english_center_2026_change_in_production")
INTERNAL_SECRET = os.getenv("INTERNAL_SECRET", "internal_secret_key_change_in_prod")

ADMISSION_SERVICE_URL = os.getenv("ADMISSION_SERVICE_URL", "http://admission-service:8081")
STUDENT_SERVICE_URL = os.getenv("STUDENT_SERVICE_URL", "http://student-service:8082")