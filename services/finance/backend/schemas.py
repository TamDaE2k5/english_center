from datetime import date, datetime
from typing import List, Optional
from pydantic import BaseModel, Field, field_validator, model_validator


ALLOWED_INVOICE_TYPES = ("COURSE_PACKAGE", "STANDALONE_RETAKE", "MIXED")
ALLOWED_ITEM_TYPES = ("TUITION_PACKAGE", "RETAKE_FEE")
ALLOWED_PAYMENT_METHODS = ("CASH", "VIETQR", "WALLET_OFFSET", "BANK_TRANSFER", "CARD", "MOMO")
ALLOWED_WALLET_TX_TYPES = ("RESERVATION_CREDIT", "TUITION_OFFSET", "ADJUSTMENT")


class TuitionFeeCreate(BaseModel):
    """Schema tạo/cập nhật biểu phí chuẩn theo khóa học (FIN-01)."""

    course_id: int = Field(..., gt=0)
    course_name: str = Field(..., min_length=1, max_length=255)
    amount: float = Field(..., ge=0)
    currency: str = Field("VND", min_length=1, max_length=10)
    discount_pct: float = Field(0.0, ge=0, le=100)
    note: Optional[str] = None
    is_active: bool = True

    @field_validator("course_name", "currency")
    @classmethod
    def strip_required(cls, v: str) -> str:
        cleaned = v.strip()
        if not cleaned:
            raise ValueError("FIN_VALIDATION_ERROR: Giá trị chuỗi không được rỗng")
        return cleaned


class VoucherCreate(BaseModel):
    """Schema tạo chiến dịch mã giảm giá Voucher."""

    code: str = Field(..., min_length=1, max_length=50)
    campaign_name: str = Field("Chiến dịch ưu đãi học phí", min_length=1, max_length=150)
    discount_percent: int = Field(0, ge=0, le=100)
    discount_amount: float = Field(0.0, ge=0)
    min_order_value: float = Field(0.0, ge=0)
    max_discount_amount: Optional[float] = Field(default=None, ge=0)
    max_uses: int = Field(100, gt=0)
    valid_from: Optional[datetime] = None
    expires_at: datetime
    is_active: bool = True

    @field_validator("code", "campaign_name")
    @classmethod
    def strip_non_empty(cls, v: str) -> str:
        cleaned = v.strip()
        if not cleaned:
            raise ValueError("FIN_VALIDATION_ERROR: Mã hoặc tên chiến dịch voucher không được rỗng")
        return cleaned


class VoucherValidateRequest(BaseModel):
    """Schema kiểm tra tính hợp lệ và tính số tiền giảm của Voucher."""

    code: str = Field(..., min_length=1)
    order_amount: float = Field(..., ge=0)

    @field_validator("code")
    @classmethod
    def strip_code(cls, v: str) -> str:
        cleaned = v.strip()
        if not cleaned:
            raise ValueError("FIN_VALIDATION_ERROR: Mã voucher không được rỗng")
        return cleaned


class InvoiceItemCreate(BaseModel):
    """Schema chi tiết khoản mục trong hóa đơn học phí."""

    course_id: Optional[int] = Field(default=None, gt=0)
    item_type: str = "TUITION_PACKAGE"
    description: str = Field("Học phí khóa học (gói trọn khóa)", min_length=1, max_length=255)
    quantity: int = Field(1, gt=0)
    unit_price: float = Field(..., ge=0)

    @model_validator(mode="after")
    def validate_item_type(self) -> "InvoiceItemCreate":
        self.item_type = self.item_type.strip().upper()
        if self.item_type not in ALLOWED_ITEM_TYPES:
            raise ValueError(f"FIN_INVALID_ITEM_TYPE: item_type phải thuộc {ALLOWED_ITEM_TYPES}")
        return self


class InvoiceCreate(BaseModel):
    """Schema tạo hóa đơn thủ công (INV-FIN-01: amount > 0)."""

    invoice_code: Optional[str] = Field(default=None, max_length=50)
    student_id: int = Field(..., gt=0)
    course_id: int = Field(1, gt=0)
    enrollment_id: Optional[int] = Field(default=None, gt=0)
    invoice_type: str = "COURSE_PACKAGE"
    amount: float = Field(..., gt=0)
    voucher_code: Optional[str] = None
    due_date: Optional[date] = None
    notes: Optional[str] = None

    @model_validator(mode="after")
    def validate_invoice_type(self) -> "InvoiceCreate":
        self.invoice_type = self.invoice_type.strip().upper()
        if self.invoice_type not in ALLOWED_INVOICE_TYPES:
            raise ValueError(f"FIN_INVALID_INVOICE_TYPE: invoice_type phải thuộc {ALLOWED_INVOICE_TYPES}")
        return self


class InvoiceAutoGenerateRequest(BaseModel):
    """Schema sinh hóa đơn tự động từ Admission hoặc giao diện quản trị."""

    enrollment_id: Optional[int] = Field(default=None, gt=0)
    student_id: int = Field(..., gt=0)
    course_id: int = Field(1, gt=0)
    invoice_type: str = "COURSE_PACKAGE"
    amount: Optional[float] = Field(default=None, gt=0)
    voucher_code: Optional[str] = None
    due_date: Optional[date] = None
    notes: Optional[str] = None
    items: Optional[List[InvoiceItemCreate]] = None
    installments_count: int = Field(1, ge=1, le=12)

    @model_validator(mode="after")
    def validate_invoice_type(self) -> "InvoiceAutoGenerateRequest":
        self.invoice_type = self.invoice_type.strip().upper()
        if self.invoice_type not in ALLOWED_INVOICE_TYPES:
            raise ValueError(f"FIN_INVALID_INVOICE_TYPE: invoice_type phải thuộc {ALLOWED_INVOICE_TYPES}")
        return self


class PaymentCreate(BaseModel):
    """Schema ghi nhận giao dịch thanh toán (INV-FIN-01: amount > 0)."""

    invoice_id: int = Field(..., gt=0)
    installment_id: Optional[int] = Field(default=None, gt=0)
    amount: float = Field(..., gt=0)
    payment_method: str = "BANK_TRANSFER"
    transaction_ref: Optional[str] = None
    bank_transaction_id: Optional[str] = None
    notes: Optional[str] = None

    @model_validator(mode="after")
    def validate_method(self) -> "PaymentCreate":
        self.payment_method = self.payment_method.strip().upper()
        if self.payment_method not in ALLOWED_PAYMENT_METHODS:
            raise ValueError(f"FIN_INVALID_PAYMENT_METHOD: payment_method phải thuộc {ALLOWED_PAYMENT_METHODS}")
        return self


class CashPaymentRequest(BaseModel):
    """Schema ghi nhận thu tiền mặt tại quầy (CASH)."""

    invoice_id: Optional[int] = Field(default=None, gt=0)
    invoice_code: Optional[str] = None
    enrollment_id: Optional[int] = Field(default=None, gt=0)
    installment_id: Optional[int] = Field(default=None, gt=0)
    amount: float = Field(..., gt=0)
    receipt_number: Optional[str] = None
    transaction_ref: Optional[str] = None
    received_by_staff_id: Optional[int] = Field(default=None, gt=0)
    notes: Optional[str] = None


class VietQRWebhookRequest(BaseModel):
    """Schema tiếp nhận Webhook đối soát thanh toán VietQR từ ngân hàng."""

    bank_transaction_id: str = Field(..., min_length=1, max_length=100)
    amount: float = Field(..., gt=0)
    content: Optional[str] = None
    qr_reference_code: Optional[str] = None
    invoice_id: Optional[int] = Field(default=None, gt=0)
    paid_at: Optional[datetime] = None

    @field_validator("bank_transaction_id")
    @classmethod
    def validate_bank_tx(cls, v: str) -> str:
        cleaned = v.strip()
        if not cleaned:
            raise ValueError("FIN_VALIDATION_ERROR: bank_transaction_id không được để trống")
        return cleaned


class WalletReserveRequest(BaseModel):
    """Schema giao dịch ví bảo lưu học viên (INV-FIN-06)."""

    tx_type: str = "RESERVATION_CREDIT"
    amount: float = Field(..., gt=0)
    related_invoice_id: Optional[int] = Field(default=None, gt=0)
    reason: str = Field(..., min_length=1)
    created_by_staff_id: Optional[int] = Field(default=None, gt=0)

    @model_validator(mode="after")
    def validate_tx_type(self) -> "WalletReserveRequest":
        self.tx_type = self.tx_type.strip().upper()
        if self.tx_type not in ALLOWED_WALLET_TX_TYPES:
            raise ValueError(f"FIN_INVALID_WALLET_TX_TYPE: tx_type phải thuộc {ALLOWED_WALLET_TX_TYPES}")
        if not self.reason.strip():
            raise ValueError("FIN_VALIDATION_ERROR: Lý do giao dịch ví không được rỗng")
        self.reason = self.reason.strip()
        return self


class RefundCreateRequest(BaseModel):
    """Schema khởi tạo yêu cầu hoàn tiền Cấp 1 (INV-FIN-07)."""

    invoice_id: Optional[int] = Field(default=None, gt=0)
    invoice_code: Optional[str] = None
    enrollment_id: Optional[int] = Field(default=None, gt=0)
    student_id: Optional[int] = Field(default=None, gt=0)
    class_id: Optional[int] = Field(default=None, gt=0)
    total_sessions: Optional[int] = Field(default=None, gt=0)
    attended_sessions: Optional[int] = Field(default=None, ge=0)
    max_allowed_sessions: int = Field(3, gt=0)
    refundable_percent: int = Field(80, ge=0, le=100)
    deduction_amount: float = Field(0.0, ge=0)
    reason: str = Field(..., min_length=1)
    bank_name: Optional[str] = None
    bank_account_no: Optional[str] = None
    bank_account_holder: Optional[str] = None


class ClassCancellationRequest(BaseModel):
    """Schema đối soát hủy lớp từ Admission Service."""

    enrollment_id: int = Field(..., gt=0)
    invoice_code: Optional[str] = None
    invoice_id: Optional[int] = Field(default=None, gt=0)
    class_id: Optional[int] = Field(default=None, gt=0)
    reason: str = "Hủy lớp do không đủ sĩ số tối thiểu"
    requested_by_staff_id: Optional[int] = Field(default=None, gt=0)


class RefundVerifyRequest(BaseModel):
    """Schema xác minh yêu cầu hoàn tiền Cấp 2 bởi Kế toán."""

    accountant_note: str = Field(..., min_length=1)
    verified_by_accountant_id: Optional[int] = Field(default=None, gt=0)


class RefundApproveRequest(BaseModel):
    """Schema phê duyệt hoặc từ chối hoàn tiền Cấp 3 bởi Giám đốc (ADMIN)."""

    is_approved: bool = True
    approved_refund_amount: Optional[float] = Field(default=None, ge=0)
    director_note: Optional[str] = None
    approved_by_director_id: Optional[int] = Field(default=None, gt=0)


class RefundCompleteRequest(BaseModel):
    """Schema xác nhận giải ngân hoàn tiền kèm mã chứng từ chuyển khoản."""

    transaction_ref: str = Field(..., min_length=1, max_length=100)

    @field_validator("transaction_ref")
    @classmethod
    def validate_ref(cls, v: str) -> str:
        cleaned = v.strip()
        if not cleaned:
            raise ValueError("FIN_VALIDATION_ERROR: transaction_ref không được để trống")
        return cleaned
