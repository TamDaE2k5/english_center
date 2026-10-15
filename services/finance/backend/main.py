import os
from datetime import date
from typing import List, Optional
from fastapi import FastAPI, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker

DB_USER = os.getenv("DB_USER", "postgres")
DB_PASSWORD = os.getenv("DB_PASSWORD", "postgres_secret_password")
DB_HOST = os.getenv("DB_HOST", "postgres")
DB_PORT = os.getenv("DB_PORT", "5432")
DB_NAME = os.getenv("DB_NAME", "english_center")

DATABASE_URL = f"postgresql://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}"

engine = create_engine(
    DATABASE_URL,
    connect_args={"options": "-csearch_path=finance,public"}
)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

app = FastAPI(title="Finance Microservice", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class InvoiceCreate(BaseModel):
    invoice_code: str
    student_id: int
    enrollment_id: Optional[int] = None
    amount: float
    due_date: Optional[date] = None

class PaymentCreate(BaseModel):
    invoice_id: int
    amount: float
    payment_method: str = "BANK_TRANSFER"
    transaction_ref: Optional[str] = None

@app.get("/api/finance/health")
def health_check():
    return {
        "status": "UP",
        "service": "finance-service",
        "framework": "FastAPI (Python 3.11+)"
    }

@app.get("/api/finance/invoices")
def get_invoices(student_id: Optional[int] = None):
    with SessionLocal() as session:
        if student_id:
            query = text("SELECT * FROM finance.invoices WHERE student_id = :student_id ORDER BY created_at DESC")
            rows = session.execute(query, {"student_id": student_id}).mappings().all()
        else:
            query = text("SELECT * FROM finance.invoices ORDER BY created_at DESC")
            rows = session.execute(query).mappings().all()
        return {
            "success": True,
            "data": [dict(r) for r in rows]
        }

@app.post("/api/finance/invoices", status_code=status.HTTP_201_CREATED)
def create_invoice(inv: InvoiceCreate):
    with SessionLocal() as session:
        query = text("""
            INSERT INTO finance.invoices (invoice_code, student_id, enrollment_id, amount, status, due_date)
            VALUES (:invoice_code, :student_id, :enrollment_id, :amount, 'UNPAID', :due_date)
            RETURNING id, invoice_code, student_id, amount, status
        """)
        row = session.execute(query, inv.model_dump()).mappings().first()
        session.commit()
        return {
            "success": True,
            "message": "Tạo hóa đơn thành công",
            "data": dict(row)
        }

@app.get("/api/finance/payments")
def get_payments(invoice_id: Optional[int] = None):
    with SessionLocal() as session:
        if invoice_id:
            query = text("SELECT * FROM finance.payments WHERE invoice_id = :invoice_id ORDER BY paid_at DESC")
            rows = session.execute(query, {"invoice_id": invoice_id}).mappings().all()
        else:
            query = text("SELECT * FROM finance.payments ORDER BY paid_at DESC")
            rows = session.execute(query).mappings().all()
        return {
            "success": True,
            "data": [dict(r) for r in rows]
        }

@app.post("/api/finance/payments", status_code=status.HTTP_201_CREATED)
def make_payment(payment: PaymentCreate):
    with SessionLocal() as session:
        # Ghi nhận thanh toán
        query = text("""
            INSERT INTO finance.payments (invoice_id, amount, payment_method, transaction_ref)
            VALUES (:invoice_id, :amount, :payment_method, :transaction_ref)
            RETURNING id, invoice_id, amount, paid_at
        """)
        row = session.execute(query, payment.model_dump()).mappings().first()
        
        # Cập nhật hóa đơn thành PAID
        update_inv = text("UPDATE finance.invoices SET status = 'PAID' WHERE id = :invoice_id")
        session.execute(update_inv, {"invoice_id": payment.invoice_id})
        
        session.commit()
        return {
            "success": True,
            "message": "Ghi nhận thanh toán thành công",
            "data": dict(row)
        }
