from fastapi import FastAPI, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException
from .deps import AppError
from .routers import grades, materials, reports

app = FastAPI(
    title="English Center - Learning Service",
    description="Phân hệ Đào tạo & Kết quả Học tập (learning-service :8083)",
    version="1.1.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.exception_handler(AppError)
async def app_error_handler(request: Request, exc: AppError):
    """Chuyển đổi AppError nghiệp vụ sang cấu trúc JSON chuẩn của hệ thống."""
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
    """Chuẩn hóa các lỗi HTTP mặc định của FastAPI/Starlette (404, 405, 401...) sang JSON chuẩn."""
    code_map = {
        400: "LRN_BAD_REQUEST",
        401: "AUTH_UNAUTHORIZED",
        403: "LRN_FORBIDDEN",
        404: "LRN_NOT_FOUND",
        405: "LRN_METHOD_NOT_ALLOWED",
        409: "LRN_CONFLICT",
    }
    return JSONResponse(
        status_code=exc.status_code,
        content={
            "success": False,
            "error": {
                "code": code_map.get(exc.status_code, "LRN_HTTP_ERROR"),
                "message": str(exc.detail),
            },
        },
    )


@app.exception_handler(RequestValidationError)
async def validation_error_handler(request: Request, exc: RequestValidationError):
    """
    Chuẩn hóa lỗi validate Pydantic (400 Bad Request) và ánh xạ đúng mã lỗi LRN_*.
    Chỉ kiểm tra trường loc và msg của từng lỗi thay vì chuỗi hóa cả dữ liệu input.
    """
    errors = exc.errors()
    first_error = errors[0] if errors else {}
    first_msg = str(first_error.get("msg", "Dữ liệu đầu vào không hợp lệ"))
    if first_msg.startswith("Value error, "):
        first_msg = first_msg[len("Value error, "):]

    err_code = "LRN_VALIDATION_ERROR"
    for err in errors:
        locs = {str(part) for part in err.get("loc", ())}
        msg = str(err.get("msg", ""))
        if "LRN_EMPTY_FILE_URL" in msg or "file_url" in locs:
            err_code = "LRN_EMPTY_FILE_URL"
            break
        if "LRN_INVALID_WEIGHT" in msg or "weight" in locs:
            err_code = "LRN_INVALID_WEIGHT"
            break
        if "LRN_INVALID_SCORE" in msg or "score" in locs or "max_score" in locs:
            err_code = "LRN_INVALID_SCORE"
            break

    return JSONResponse(
        status_code=status.HTTP_400_BAD_REQUEST,
        content={
            "success": False,
            "error": {
                "code": err_code,
                "message": first_msg,
            },
        },
    )


@app.exception_handler(Exception)
async def unhandled_exception_handler(request: Request, exc: Exception):
    """Chặn toàn bộ ngoại lệ chưa xử lý để không làm lộ Python traceback ra client."""
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={
            "success": False,
            "error": {
                "code": "LRN_INTERNAL_ERROR",
                "message": "Đã xảy ra lỗi nội bộ tại Learning Service, vui lòng thử lại sau",
            },
        },
    )


@app.get("/health")
def health_check():
    """Healthcheck nội bộ dùng cho docker-compose / kiểm tra trực tiếp cổng 8083."""
    return {"status": "ok"}


@app.get("/api/learning/health")
def api_health_check():
    """Healthcheck qua API Gateway (/api/learning/health) trả về cấu trúc JSON chuẩn."""
    return {
        "success": True,
        "data": {"status": "UP", "service": "learning-service", "framework": "FastAPI"},
        "message": "Hệ thống Learning Service đang hoạt động",
    }


app.include_router(materials.router)
app.include_router(grades.router)
app.include_router(reports.router)
