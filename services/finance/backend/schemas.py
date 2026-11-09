from datetime import date, datetime
from typing import Any, Dict, List, Optional
from pydantic import BaseModel, Field, model_validator


class TuitionFeeCreate(BaseModel):
    course_id: int
    course_name: str
    amount: float = Field(..., ge=0)
    currency: str = "VND"
    discount_pct: float = Field(0.0, ge=0, le=100)
    note: Optional[str] = None
    is_active: bool = True


class VoucherCreate(BaseModel):
    code: str
    campaign_name: str = "Chiến dịch ưu đãi học phí"
    discount_percent: int = Field(0, ge=0, le=100)
    discount_amount: float = Field(0.0, ge=0)
    min_order_value: float = Field(0.0, ge=0)
    max_discount_amount: Optional[float] = None
    max_uses: int = Field(100, gt=0)
    valid_from: Optional[datetime] = None
    expires_at: datetime
    is_active: bool = True


class VoucherValidateRequest(BaseModel):
    code: str
    order_amount: float = Field(..., ge=0)


class InvoiceItemCreate(BaseModel):
    course_id: Optional[int] = None
    item_type: str = "TUITION_PACKAGE"
    description: str = "Học phí khóa học (gói trọn khóa)"
    quantity: int = Field(1, gt=0)
    unit_price: float = Field(..., ge=0)


class InvoiceCreate(BaseModel):
    invoice_code: Optional[str] = None
    student_id: int
    course_id: int = 1
    enrollment_id: Optional[int] = None
    invoice_type: str = "COURSE_PACKAGE"
    amount: float = Field(..., ge=0)
    voucher_code: Optional[str] = None
    due_date: Optional[date] = None
    notes: Optional[str] = None


class InvoiceAutoGenerateRequest(BaseModel):
    enrollment_id: Optional[int] = None
    student_id: int
    course_id: int = 1
    invoice_type: str = "COURSE_PACKAGE"
    amount: Optional[float] = Field(default=None, ge=0)
    voucher_code: Optional[str] = None
    due_date: Optional[date] = None
    notes: Optional[str] = None
    items: Optional[List[InvoiceItemCreate]] = None
    installments_count: int = Field(1, ge=1, le=12)


class PaymentCreate(BaseModel):
    invoice_id: int
    installment_id: Optional[int] = None
    amount: float = Field(..., gt=0)
    payment_method: str = "BANK_TRANSFER"
    transaction_ref: Optional[str] = None
    bank_transaction_id: Optional[str] = None
    notes: Optional[str] = None


class CashPaymentRequest(BaseModel):
    invoice_id: int
    installment_id: Optional[int] = None
    amount: float = Field(..., gt=0)
    received_by_staff_id: Optional[int] = None
    notes: Optional[str] = None


class VietQRWebhookRequest(BaseModel):
    bank_transaction_id: str
    amount: float = Field(..., gt=0)
    content: Optional[str] = None
    qr_reference_code: Optional[str] = None
    invoice_id: Optional[int] = None
    paid_at: Optional[datetime] = None


class WalletReserveRequest(BaseModel):
    tx_type: str = "RESERVATION_CREDIT"
    amount: float = Field(..., gt=0)
    related_invoice_id: Optional[int] = None
    reason: str
    created_by_staff_id: Optional[int] = None

    @model_validator(mode="after")
    def validate_tx_type(self) -> "WalletReserveRequest":
        allowed = ("RESERVATION_CREDIT", "TUITION_OFFSET", "ADJUSTMENT")
        if self.tx_type not in allowed:
            raise ValueError(f"FIN_INVALID_WALLET_TX_TYPE: tx_type phải thuộc {allowed}")
        return self


class RefundCreateRequest(BaseModel):
    invoice_id: int
    student_id: Optional[int] = None
    class_id: Optional[int] = None
    total_sessions: Optional[int] = None
    attended_sessions: Optional[int] = None
    max_allowed_sessions: int = 3
    refundable_percent: int = Field(80, ge=0, le=100)
    deduction_amount: float = Field(0.0, ge=0)
    reason: str
    bank_name: Optional[str] = None
    bank_account_no: Optional[str] = None
    bank_account_holder: Optional[str] = None


class RefundVerifyRequest(BaseModel):
    accountant_note: str
    verified_by_accountant_id: Optional[int] = None


class RefundApproveRequest(BaseModel):
    is_approved: bool = True
    approved_refund_amount: Optional[float] = Field(default=None, ge=0)
    director_note: Optional[str] = None
    approved_by_director_id: Optional[int] = None


class RefundCompleteRequest(BaseModel):
    transaction_ref: str
