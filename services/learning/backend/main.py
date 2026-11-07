from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
from routers import materials, grades

app = FastAPI(title="Learning Service API")

app.include_router(materials.router)
app.include_router(grades.router)

# Exception handler cho Pydantic validation (Chuẩn hóa output theo api-conventions.md)
@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    error_msg = exc.errors()[0].get("msg", "Dữ liệu không hợp lệ")
    # Bắt lỗi custom từ @model_validator
    if "Value error," in error_msg:
        error_msg = error_msg.replace("Value error, ", "")
    
    return JSONResponse(
        status_code=400,
        content={
            "success": False,
            "error": {
                "code": "LRN_VALIDATION_ERROR",
                "message": error_msg
            }
        }
    )

@app.get("/")
def health_check():
    return {"status": "ok", "service": "learning"}

@app.get("/api/learning/health")
def health_check():
    return {"status": "ok", "service": "learning"}