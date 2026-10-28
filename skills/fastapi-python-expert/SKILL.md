---
name: fastapi-python-expert
description: >-
  Use this skill whenever developing, refactoring, or testing Python 3.11 FastAPI microservices
  (Student Service, Learning Service, Finance Service). Covers layered project structure,
  Pydantic v2 schemas, SQLAlchemy session lifecycle, global exception handling, snake_case API contracts,
  and invariant testing compliance.
---

# FastAPI & Python 3.11 Microservices Engineering Skill

## 1. Architectural Structure (Cấu trúc Phân tầng)

Áp dụng cho 3 service Python: `student-service` (:8082), `learning-service` (:8083), và `finance-service` (:8084).

```
services/{service_name}/
├── main.py                  # Khởi tạo FastAPI App, CORS, Exception Handlers, Router mounting
├── database.py              # Engine, SessionLocal, Dependency get_db()
├── routers/                 # Tầng Controller tiếp nhận HTTP requests
│   └── *.py
├── services/                # Tầng Logic nghiệp vụ cốt lõi (Business Logic & Invariants)
│   └── *.py
├── schemas/                 # Pydantic v2 Models (Request / Response validation)
│   └── *.py
├── models/                  # SQLAlchemy ORM Models ánh xạ bảng Schema riêng
│   └── *.py
└── crud/                    # Các hàm truy vấn Database cơ bản (Create, Read, Update, Delete)
    └── *.py
```

---

## 2. Pydantic v2 Schema & Response Contract

Toàn bộ payload request và response JSON tuân thủ chuẩn `snake_case`.

### Chuẩn hóa Schema:
```python
from pydantic import BaseModel, Field, ConfigDict
from typing import Generic, TypeVar, Optional, Any

T = TypeVar("T")

class ApiError(BaseModel):
    code: str
    message: str

class ApiResponse(BaseModel, Generic[T]):
    success: bool
    data: Optional[T] = None
    message: Optional[str] = None
    error: Optional[ApiError] = None

    model_config = ConfigDict(populate_by_name=True)

    @classmethod
    def ok(cls, data: Any = None, message: str = "Thao tác thành công"):
        return cls(success=True, data=data, message=message, error=None)

    @classmethod
    def fail(cls, code: str, message: str):
        return cls(success=false, data=None, message=None, error=ApiError(code=code, message=message))
```

---

## 3. Global Exception Handling (Bắt lỗi Tập trung)

Không bao giờ để lộ lỗi 500 kèm Python Traceback ra client. Xử lý tập trung trong `main.py`:

```python
from fastapi import FastAPI, Request, status
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
from starlette.exceptions import HTTPException as StarletteHTTPException

app = FastAPI(title="English Center Microservice")

@app.exception_handler(StarletteHTTPException)
async def http_exception_handler(request: Request, exc: StarletteHTTPException):
    """
    Bắt các lỗi HTTP chủ động ném ra trong quá trình xử lý nghiệp vụ.
    """
    error_code = "HTTP_ERROR"
    if exc.status_code == 404:
        error_code = "NOT_FOUND"
    elif exc.status_code == 400:
        error_code = "BAD_REQUEST"
    elif exc.status_code == 403:
        error_code = "FORBIDDEN"
    elif exc.status_code == 409:
        error_code = "CONFLICT"

    return JSONResponse(
        status_code=exc.status_code,
        content={
            "success": False,
            "error": {
                "code": getattr(exc, "code", error_code),
                "message": str(exc.detail)
            }
        }
    )

@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    """
    Bắt lỗi validate dữ liệu đầu vào không hợp lệ từ Pydantic.
    """
    first_error = exc.errors()[0] if exc.errors() else {}
    field = ".".join(str(loc) for loc in first_error.get("loc", []))
    msg = first_error.get("msg", "Dữ liệu không hợp lệ")

    return JSONResponse(
        status_code=status.HTTP_400_BAD_REQUEST,
        content={
            "success": False,
            "error": {
                "code": "VALIDATION_ERROR",
                "message": f"Trường '{field}': {msg}"
            }
        }
    )
```

---

## 4. Database Session Lifecycle & Isolation Rule

1. **Dependency Injection**:
   ```python
   def get_db():
       db = SessionLocal()
       try:
           yield db
       finally:
           db.close()
   ```
2. **Schema Isolation**:
   - Khai báo rõ schema trong model SQLAlchemy:
     ```python
     class Student(Base):
         __tablename__ = "students"
         __table_args__ = {"schema": "student"}
     ```
   - **Tuyệt đối không** viết truy vấn SQL JOIN chéo sang bảng của schema khác (`admission`, `learning`, `finance`).

---

## 5. Coding Principles (Đơn giản & Comment Chi tiết)

1. **KISS Principle**: Viết code trực diện, dễ đọc, không lạm dụng metaprogramming hay class inheritance đa cấp nếu không cần thiết.
2. **Comment từng bước**:
   ```python
   def record_payment(db: Session, invoice_id: int, amount: float, transaction_ref: str):
       # Bước 1: Kiểm tra xem transaction_ref đã từng được ghi nhận chưa (Chống lặp tiền INV-FIN-02)
       existing_payment = db.query(Payment).filter(
           Payment.transaction_ref == transaction_ref
       ).first()
       if existing_payment:
           raise HTTPException(
               status_code=400, 
               detail="Mã giao dịch đã tồn tại trong hệ thống (Duplicate Transaction)"
           )

       # Bước 2: Kiểm tra sự tồn tại của hóa đơn
       invoice = db.query(Invoice).filter(Invoice.id == invoice_id).first()
       if not invoice:
           raise HTTPException(status_code=404, detail="Không tìm thấy hóa đơn cần thanh toán")

       # Bước 3: Tạo bản ghi thanh toán mới
       ...
   ```
3. **Không emoji**: Giữ sạch log và code logic.

---

## 6. Checklist Kiểm tra Code

- [ ] Pydantic Model dùng 100% `snake_case`.
- [ ] Không có chuỗi raw SQL injection (dùng SQLAlchemy ORM hoặc parametrized query).
- [ ] Comment tiếng Việt giải thích từng bước logic nghiệp vụ.
- [ ] Chạy bộ kiểm thử Invariant Test pass 100%:
  ```bash
  python3 -m unittest discover -s tests/invariants -p "test_*.py" -v
  ```
