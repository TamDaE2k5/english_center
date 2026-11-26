"""
database.py - Cau hinh ket noi PostgreSQL va vong doi Session cho Student Service.
Tuan thu INV-SYS-02: chi truy cap schema 'student' (kem 'public' cho cac ham he thong).
"""

import os

from dotenv import load_dotenv
from sqlalchemy import create_engine
from sqlalchemy.orm import declarative_base, sessionmaker

# Tai bien moi truong tu file .env neu ton tai
load_dotenv()


def _build_database_url() -> str:
    """
    Xay dung chuoi ket noi SQLAlchemy tu bien moi truong DATABASE_URL hoac DB_*.
    """
    env_url = os.getenv("DATABASE_URL")
    if env_url:
        if env_url.startswith("postgresql://"):
            return env_url.replace("postgresql://", "postgresql+psycopg2://", 1)
        return env_url

    db_user = os.getenv("DB_USER", "postgres")
    db_password = os.getenv("DB_PASSWORD", "postgres_secret_password")
    db_host = os.getenv("DB_HOST", "localhost")
    db_port = os.getenv("DB_PORT", "5432")
    db_name = os.getenv("DB_NAME", "english_center")
    return f"postgresql+psycopg2://{db_user}:{db_password}@{db_host}:{db_port}/{db_name}"


DATABASE_URL = _build_database_url()

# Khoi tao SQLAlchemy Engine voi search_path uu tien schema 'student'
engine = create_engine(
    DATABASE_URL,
    connect_args={"options": "-csearch_path=student,public"},
    pool_pre_ping=True,
)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()


def get_db():
    """
    Dependency cung cap SQLAlchemy Session cho moi HTTP request va dam bao dong sau khi xu ly.
    """
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
