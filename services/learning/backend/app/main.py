from fastapi import FastAPI, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from .routers import grades, materials
from .security import AppError

app = FastAPI(
    title="English Center - Learning & Assessment Service",
    description="Phân hệ Quản lý Học liệu, Bảng điểm Trọng số & Báo cáo Học lực (Port 8083)",
    version="1.0.0",
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


@app.exception_handler(RequestValidationError)
async def validation_error_handler(request: Request, exc: RequestValidationError):
    errors = exc.errors()
    first_msg = errors[0].get("msg", "Dữ liệu đầu vào không hợp lệ") if errors else "Dữ liệu không hợp lệ"
    err_code = "LRN_VALIDATION_ERROR"
    if "score" in str(errors) or "max_score" in str(errors):
        err_code = "LRN_INVALID_SCORE"
    elif "file_url" in str(errors):
        err_code = "LRN_EMPTY_FILE_URL"
    elif "weight" in str(errors):
        err_code = "LRN_INVALID_WEIGHT"

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


@app.get("/health")
def health_check():
    return {
        "success": True,
        "data": {"service": "learning-service", "port": 8083, "status": "UP"},
        "message": "Learning service is healthy",
    }


app.include_router(materials.router)
app.include_router(grades.router)