import os
from datetime import date
from typing import List, Optional
from fastapi import FastAPI, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker

# Config & Environment
DB_USER = os.getenv("DB_USER", "postgres")
DB_PASSWORD = os.getenv("DB_PASSWORD", "postgres_secret_password")
DB_HOST = os.getenv("DB_HOST", "postgres")
DB_PORT = os.getenv("DB_PORT", "5432")
DB_NAME = os.getenv("DB_NAME", "english_center")

DATABASE_URL = f"postgresql://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}"

# SQLAlchemy Setup (schema = student)
engine = create_engine(
    DATABASE_URL,
    connect_args={"options": "-csearch_path=student,public"}
)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

app = FastAPI(title="Student Microservice", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Pydantic Schemas
class StudentCreate(BaseModel):
    full_name: str
    email: str
    phone: Optional[str] = None
    date_of_birth: Optional[date] = None
    address: Optional[str] = None
    status: Optional[str] = "STUDYING"

class AttendanceCreate(BaseModel):
    class_id: int
    student_id: int
    session_date: date
    status: str = "PRESENT"
    notes: Optional[str] = None

# --- Endpoints ---

@app.get("/api/student/health")
def health_check():
    return {
        "status": "UP",
        "service": "student-service",
        "framework": "FastAPI (Python 3.11+)"
    }

@app.get("/api/student/students")
def get_students():
    with SessionLocal() as session:
        query = text("""
            SELECT id, user_id, full_name, email, phone, date_of_birth, address, status, created_at 
            FROM student.students 
            ORDER BY id ASC
        """)
        rows = session.execute(query).mappings().all()
        return {
            "success": True,
            "data": [dict(r) for r in rows]
        }

@app.post("/api/student/students", status_code=status.HTTP_201_CREATED)
def create_student(student: StudentCreate):
    with SessionLocal() as session:
        query = text("""
            INSERT INTO student.students (full_name, email, phone, date_of_birth, address, status)
            VALUES (:full_name, :email, :phone, :date_of_birth, :address, :status)
            RETURNING id, full_name, email, phone, status
        """)
        row = session.execute(query, student.model_dump()).mappings().first()
        session.commit()
        return {
            "success": True,
            "message": "Thêm học viên thành công",
            "data": dict(row)
        }

@app.get("/api/student/attendance")
def get_attendance(class_id: Optional[int] = None):
    with SessionLocal() as session:
        if class_id:
            query = text("""
                SELECT a.id, a.class_id, a.student_id, s.full_name as student_name, 
                       a.session_date, a.status, a.notes
                FROM student.attendance a
                JOIN student.students s ON a.student_id = s.id
                WHERE a.class_id = :class_id
                ORDER BY a.session_date DESC
            """)
            rows = session.execute(query, {"class_id": class_id}).mappings().all()
        else:
            query = text("""
                SELECT a.id, a.class_id, a.student_id, s.full_name as student_name, 
                       a.session_date, a.status, a.notes
                FROM student.attendance a
                JOIN student.students s ON a.student_id = s.id
                ORDER BY a.session_date DESC
            """)
            rows = session.execute(query).mappings().all()
        return {
            "success": True,
            "data": [dict(r) for r in rows]
        }
