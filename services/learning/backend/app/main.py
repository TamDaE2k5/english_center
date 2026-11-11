from fastapi import FastAPI, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
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
    raw_str = str(errors)
    first_msg = (
        errors[0].get("msg", "Dữ liệu đầu vào không hợp lệ")
        if errors
        else "Dữ liệu đầu vào không hợp lệ"
    )

    if "LRN_EMPTY_FILE_URL" in raw_str or "file_url" in raw_str:
        err_code = "LRN_EMPTY_FILE_URL"
    elif "LRN_INVALID_WEIGHT" in raw_str or "weight" in raw_str:
        err_code = "LRN_INVALID_WEIGHT"
    elif "LRN_INVALID_SCORE" in raw_str or "score" in raw_str or "max_score" in raw_str:
        err_code = "LRN_INVALID_SCORE"
    else:
        err_code = "LRN_VALIDATION_ERROR"

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
    return {"status": "ok"}


app.include_router(materials.router)
app.include_router(grades.router)
app.include_router(reports.router)