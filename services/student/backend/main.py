"""
main.py - Khoi tao FastAPI App cho Student Microservice (Port 8082).
Bao gom: CORS, Global Exception Handlers chuan JSON, Health Check va dang ky cac Router.
"""

from fastapi import FastAPI, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

from auth import StudentException
from routers import attendance, internal, status_requests, students
from utils import success_response

app = FastAPI(
    title="Student Microservice",
    description="Phan he Quan ly Ho so Hoc vien, Diem danh va Yeu cau Hoc vu (Schema: student, Port: 8082)",
    version="1.1.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.exception_handler(StudentException)
async def student_exception_handler(request: Request, exc: StudentException):
    """
    Bat ngoai le nghiep vu StudentException va tra ve JSON chuan {success: false, error: {code, message}}.
    """
    return JSONResponse(
        status_code=exc.status_code,
        content={
            "success": False,
            "error": {
                "code": exc.code,
                "message": exc.message,
            },
        },
    )


@app.exception_handler(StarletteHTTPException)
async def http_exception_handler(request: Request, exc: StarletteHTTPException):
    """
    Bat tat ca HTTPException mac dinh cua FastAPI/Starlette (404, 405, 401...) de dong bo dinh dang JSON.
    """
    status_code_map = {
        400: "STU_BAD_REQUEST",
        401: "AUTH_UNAUTHORIZED",
        403: "STU_FORBIDDEN",
        404: "STU_NOT_FOUND",
        405: "STU_METHOD_NOT_ALLOWED",
        409: "STU_CONFLICT",
        422: "STU_VALIDATION_ERROR",
    }
    error_code = status_code_map.get(exc.status_code, "STU_HTTP_ERROR")
    detail = exc.detail
    if isinstance(detail, dict) and "code" in detail and "message" in detail:
        error_code = str(detail["code"])
        message = str(detail["message"])
    else:
        message = str(detail) if detail else "Yeu cau khong hop le"

    return JSONResponse(
        status_code=exc.status_code,
        content={
            "success": False,
            "error": {
                "code": error_code,
                "message": message,
            },
        },
    )


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    """
    Bat loi kiem tra du lieu dau vao Pydantic va tra ve HTTP 400 chuan API Contract.
    """
    errors = exc.errors()
    first_error = errors[0] if errors else {}
    loc_parts = [str(part) for part in first_error.get("loc", []) if part != "body"]
    field_name = ".".join(loc_parts) if loc_parts else "payload"
    msg = str(first_error.get("msg", "Du lieu dau vao khong hop le"))
    if msg.startswith("Value error, "):
        msg = msg.replace("Value error, ", "", 1)

    return JSONResponse(
        status_code=status.HTTP_400_BAD_REQUEST,
        content={
            "success": False,
            "error": {
                "code": "STU_VALIDATION_ERROR",
                "message": f"Truong '{field_name}': {msg}",
            },
        },
    )


@app.exception_handler(Exception)
async def unhandled_exception_handler(request: Request, exc: Exception):
    """
    Bat loi he thong chua duoc xu ly, tuyet doi khong de lo Python StackTrace ra client.
    """
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={
            "success": False,
            "error": {
                "code": "STU_INTERNAL_ERROR",
                "message": "Da xay ra loi he thong tai Student Service",
            },
        },
    )


@app.get("/api/student/health")
def health_check():
    """
    Kiem tra tinh trang hoat dong cua Student Service.
    """
    return success_response(
        {"status": "UP", "service": "student-service", "framework": "FastAPI"},
        "He thong Student Service dang hoat dong",
    )


# Dang ky cac Router chinh (khong trung lap)
app.include_router(students.router, prefix="/api/student", tags=["Students"])
app.include_router(attendance.router, prefix="/api/student", tags=["Attendance"])
app.include_router(status_requests.router, prefix="/api/student", tags=["Status Requests"])
app.include_router(internal.router, prefix="/api/student", tags=["Internal"])
