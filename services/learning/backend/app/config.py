import os
from dataclasses import dataclass


def _build_database_url() -> str:
    explicit_url = os.getenv("DATABASE_URL")
    if explicit_url:
        return explicit_url
    db_user = os.getenv("DB_USER", "postgres")
    db_password = os.getenv("DB_PASSWORD", "postgres_secret_password")
    db_host = os.getenv("DB_HOST", "localhost")
    db_port = os.getenv("DB_PORT", "5432")
    db_name = os.getenv("DB_NAME", "english_center")
    return f"postgresql://{db_user}:{db_password}@{db_host}:{db_port}/{db_name}"


@dataclass(frozen=True)
class Settings:
    SERVICE_NAME: str = "learning-service"
    PORT: int = int(os.getenv("PORT_LEARNING", os.getenv("PORT", "8083")))
    DATABASE_URL: str = _build_database_url()
    JWT_SECRET: str = os.getenv(
        "JWT_SECRET",
        "super_secret_jwt_key_english_center_2026_change_in_production",
    )
    JWT_ALGORITHM: str = os.getenv("JWT_ALGORITHM", "HS256")
    INTERNAL_SECRET_KEY: str = os.getenv("INTERNAL_SECRET_KEY", "dev-internal-secret-key")
    ADMISSION_SERVICE_URL: str = os.getenv(
        "ADMISSION_SERVICE_URL", "http://admission-service:8081"
    )
    STUDENT_SERVICE_URL: str = os.getenv(
        "STUDENT_SERVICE_URL", "http://student-service:8082"
    )
    HTTP_TIMEOUT_SECONDS: float = float(os.getenv("HTTP_TIMEOUT_SECONDS", "5.0"))
    HTTP_MAX_RETRIES: int = int(os.getenv("HTTP_MAX_RETRIES", "3"))
    STRICT_INTERNAL_VERIFY: bool = (
        os.getenv("STRICT_INTERNAL_VERIFY", "false").lower() == "true"
    )


settings = Settings()