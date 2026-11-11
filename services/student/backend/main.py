from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from utils import success_response
import models
from database import engine

app = FastAPI(title="Student Microservice", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# --- HEALTH CHECK (Chuẩn format) ---
@app.get("/api/student/health")
def health_check():
    return success_response(
        {"status": "UP", "service": "student-service", "framework": "FastAPI"}, 
        "Hệ thống Student Service đang hoạt động"
    )

# --- KẾT NỐI CÁC ROUTER ---
# Chúng ta sẽ import các router đã tách riêng ở đây
from routers import students, attendance

app.include_router(students.router, prefix="/api/student", tags=["Students"])
app.include_router(attendance.router, prefix="/api/student", tags=["Attendance"])

# Nối Router vào ứng dụng chính
from routers import students
app.include_router(students.router, prefix="/api/student", tags=["Students"])

from routers import attendance
app.include_router(attendance.router, prefix="/api/student", tags=["Attendance"])

from routers import status_requests
app.include_router(status_requests.router, prefix="/api/student", tags=["Status Requests"])