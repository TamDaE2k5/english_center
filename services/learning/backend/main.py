import os
from typing import List, Optional
from fastapi import FastAPI, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker

DB_USER = os.getenv("DB_USER", "postgres")
DB_PASSWORD = os.getenv("DB_PASSWORD", "postgres_secret_password")
DB_HOST = os.getenv("DB_HOST", "postgres")
DB_PORT = os.getenv("DB_PORT", "5432")
DB_NAME = os.getenv("DB_NAME", "english_center")

DATABASE_URL = f"postgresql://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}"

engine = create_engine(
    DATABASE_URL,
    connect_args={"options": "-csearch_path=learning,public"}
)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

app = FastAPI(title="Learning Microservice", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class MaterialCreate(BaseModel):
    class_id: int
    title: str
    description: Optional[str] = None
    material_type: str = "DOCUMENT"
    file_url: Optional[str] = None

class GradeCreate(BaseModel):
    class_id: int
    student_id: int
    assessment_name: str
    score: float
    max_score: float = 100.0
    feedback: Optional[str] = None

@app.get("/api/learning/health")
def health_check():
    return {
        "status": "UP",
        "service": "learning-service",
        "framework": "FastAPI (Python 3.11+)"
    }

@app.get("/api/learning/materials")
def get_materials(class_id: Optional[int] = None):
    with SessionLocal() as session:
        if class_id:
            query = text("SELECT * FROM learning.materials WHERE class_id = :class_id ORDER BY uploaded_at DESC")
            rows = session.execute(query, {"class_id": class_id}).mappings().all()
        else:
            query = text("SELECT * FROM learning.materials ORDER BY uploaded_at DESC")
            rows = session.execute(query).mappings().all()
        return {
            "success": True,
            "data": [dict(r) for r in rows]
        }

@app.post("/api/learning/materials", status_code=status.HTTP_201_CREATED)
def create_material(item: MaterialCreate):
    with SessionLocal() as session:
        query = text("""
            INSERT INTO learning.materials (class_id, title, description, material_type, file_url)
            VALUES (:class_id, :title, :description, :material_type, :file_url)
            RETURNING id, class_id, title, material_type, file_url
        """)
        row = session.execute(query, item.model_dump()).mappings().first()
        session.commit()
        return {
            "success": True,
            "message": "Đăng tải học liệu thành công",
            "data": dict(row)
        }

@app.get("/api/learning/grades")
def get_grades(class_id: Optional[int] = None, student_id: Optional[int] = None):
    with SessionLocal() as session:
        query_str = "SELECT * FROM learning.grades WHERE 1=1"
        params = {}
        if class_id:
            query_str += " AND class_id = :class_id"
            params["class_id"] = class_id
        if student_id:
            query_str += " AND student_id = :student_id"
            params["student_id"] = student_id
        query_str += " ORDER BY graded_at DESC"

        rows = session.execute(text(query_str), params).mappings().all()
        return {
            "success": True,
            "data": [dict(r) for r in rows]
        }

@app.post("/api/learning/grades", status_code=status.HTTP_201_CREATED)
def add_grade(grade: GradeCreate):
    with SessionLocal() as session:
        query = text("""
            INSERT INTO learning.grades (class_id, student_id, assessment_name, score, max_score, feedback)
            VALUES (:class_id, :student_id, :assessment_name, :score, :max_score, :feedback)
            RETURNING id, student_id, assessment_name, score
        """)
        row = session.execute(query, grade.model_dump()).mappings().first()
        session.commit()
        return {
            "success": True,
            "message": "Nhập điểm thành công",
            "data": dict(row)
        }
