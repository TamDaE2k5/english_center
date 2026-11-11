from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from auth import FinanceException
from routers import bi, invoices, payments, refunds, vouchers

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
app.include_router(wallets_router := __import__("routers.wallets", fromlist=["router"]).router)
app.include_router(refunds.router)
app.include_router(bi.router)


@app.exception_handler(FinanceException)
async def finance_exception_handler(request: Request, exc: FinanceException):
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
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    error_msg = exc.errors()[0].get("msg", "Dữ liệu đầu vào không hợp lệ")
    error_code = "FIN_VALIDATION_ERROR"
    if "Value error," in error_msg:
        error_msg = error_msg.replace("Value error, ", "")
    if ":" in error_msg and error_msg.split(":")[0].startswith("FIN_"):
        parts = error_msg.split(":", 1)
        error_code = parts[0].strip()
        error_msg = parts[1].strip()

    return JSONResponse(
        status_code=422,
        content={
            "success": False,
            "error": {
                "code": error_code,
                "message": error_msg,
            },
        },
    )


@app.get("/")
def root_health():
    return {
        "status": "UP",
        "service": "finance-service",
        "port": 8084,
    }


@app.get("/api/finance/health")
def health_check():
    return {
        "status": "UP",
        "service": "finance-service",
        "framework": "FastAPI (Python 3.11+)",
    }
