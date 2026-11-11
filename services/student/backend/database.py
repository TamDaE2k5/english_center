import os
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, declarative_base
from dotenv import load_dotenv

# Tải các biến môi trường từ file .env (nếu có)
load_dotenv()

# =====================================================================
# CẤU HÌNH KẾT NỐI DATABASE
# - os.getenv("TÊN_BIẾN", "giá_trị_mặc_định")
# - Nếu chạy trong Docker: Docker sẽ tự động truyền DB_HOST="postgres"
# - Nếu chạy trên máy (Uvicorn): Nó sẽ dùng giá trị mặc định là "localhost"
# =====================================================================
DB_USER = os.getenv("DB_USER", "postgres")
DB_PASSWORD = os.getenv("DB_PASSWORD", "postgres_secret_password")
DB_HOST = os.getenv("DB_HOST", "localhost") 
DB_PORT = os.getenv("DB_PORT", "5432")
DB_NAME = os.getenv("DB_NAME", "english_center")

DATABASE_URL = f"postgresql+psycopg2://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}"

# =====================================================================
# KHỞI TẠO ENGINE
# - Tham số connect_args giúp tự động tìm kiếm trong schema 'student' trước
# =====================================================================
engine = create_engine(
    DATABASE_URL,
    connect_args={"options": "-csearch_path=student,public"}
)

# Tạo session để làm việc với DB
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

# Base class cho các Model (để định nghĩa bảng)
Base = declarative_base()

# =====================================================================
# HÀM DEPENDENCY: Dùng để mở/đóng kết nối DB trong mỗi API request
# =====================================================================
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()