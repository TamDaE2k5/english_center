import logging
from fastapi import FastAPI, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

from auth import FinanceException
from routers import bi, invoices, payments, refunds, vouchers, wallets

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(name)s: %(message)s")
logger = logging.getLogger("finance-service")

app = FastAPI(
    title="Finance Microservice",
    version="2.0.0",
    description="Phân hệ Quản lý Tài chính & Học phí (Schema: finance, Port: 8084)",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(vouchers.router)
app.include_router(invoices.router)
app.include_router(payments.router)
app.include_router(wallets.router)
app.include_router(refunds.router)
app.include_router(bi.router)


@app.exception_handler(FinanceException)
async def finance_exception_handler(request: Request, exc: FinanceException):
    """Bắt ngoại lệ nghiệp vụ FinanceException và trả về format JSON chuẩn."""
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
    """Bắt các lỗi HTTP chuẩn (404, 405, ...) và chuẩn hóa thành cấu trúc JSON thống nhất."""
    code_map = {
        400: "FIN_BAD_REQUEST",
        401: "AUTH_UNAUTHORIZED",
        403: "AUTH_FORBIDDEN",
        404: "FIN_NOT_FOUND",
        405: "FIN_METHOD_NOT_ALLOWED",
        409: "FIN_CONFLICT",
    }
    error_code = code_map.get(exc.status_code, "FIN_HTTP_ERROR")
    detail = exc.detail
    if isinstance(detail, dict) and "code" in detail and "message" in detail:
        error_code = detail["code"]
        message = detail["message"]
    else:
        message = str(detail) if detail else "Yêu cầu không hợp lệ"

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
    """Bắt lỗi validate Pydantic đầu vào và trả về thông báo lỗi chuẩn snake_case."""
    errors = exc.errors()
    if errors:
        first_err = errors[0]
        field = ".".join(str(loc) for loc in first_err.get("loc", []) if loc != "body")
        error_msg = first_err.get("msg", "Dữ liệu đầu vào không hợp lệ")
    else:
        field = ""
        error_msg = "Dữ liệu đầu vào không hợp lệ"

    error_code = "FIN_VALIDATION_ERROR"
    if "Value error," in error_msg:
        error_msg = error_msg.replace("Value error, ", "")
    if ":" in error_msg and error_msg.split(":")[0].startswith("FIN_"):
        parts = error_msg.split(":", 1)
        error_code = parts[0].strip()
        error_msg = parts[1].strip()
    elif field:
        error_msg = f"Trường '{field}': {error_msg}"

    return JSONResponse(
        status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
        content={
            "success": False,
            "error": {
                "code": error_code,
                "message": error_msg,
            },
        },
    )


@app.exception_handler(Exception)
async def unhandled_exception_handler(request: Request, exc: Exception):
    """Chặn rò rỉ Python stacktrace 500 ra client khi xảy ra lỗi hệ thống ngoài dự kiến."""
    logger.exception("Lỗi hệ thống chưa xử lý tại %s %s", request.method, request.url.path)
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={
            "success": False,
            "error": {
                "code": "FIN_INTERNAL_ERROR",
                "message": "Lỗi hệ thống nội bộ tại Finance Service",
            },
        },
    )


@app.get("/")
def root_health():
    """Endpoint kiểm tra trạng thái service tại root."""
    return {
        "status": "UP",
        "service": "finance-service",
        "port": 8084,
    }


@app.get("/api/finance/health")
def health_check():
    """Endpoint health check định tuyến qua Nginx Gateway theo chuẩn ApiResponse."""
    return {
        "success": True,
        "data": {
            "status": "UP",
            "service": "finance-service",
            "framework": "FastAPI (Python 3.11+)",
        },
        "message": "Finance Service đang hoạt động",
    }
