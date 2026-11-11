from datetime import datetime
from typing import Optional
from fastapi import APIRouter, Depends, status
from sqlalchemy import text
from sqlalchemy.orm import Session

from auth import FinanceException, get_current_user, require_roles
from database import get_db
from schemas import TuitionFeeCreate, VoucherCreate, VoucherValidateRequest

router = APIRouter(prefix="/api/finance", tags=["Tuition & Vouchers"])


@router.get("/tuition-fees")
def get_tuition_fees(db: Session = Depends(get_db)):
    rows = db.execute(
        text("SELECT * FROM finance.tuition_fees ORDER BY course_id ASC")
    ).mappings().all()
    return {
        "success": True,
        "data": [dict(r) for r in rows],
    }


@router.post("/tuition-fees", status_code=status.HTTP_201_CREATED)
def upsert_tuition_fee(
    payload: TuitionFeeCreate,
    db: Session = Depends(get_db),
    user: dict = Depends(require_roles(["ADMIN", "STAFF"])),
):
    query = text("""
        INSERT INTO finance.tuition_fees
            (course_id, course_name, amount, base_amount, currency, discount_pct, note, description, is_active)
        VALUES
            (:course_id, :course_name, :amount, :amount, :currency, :discount_pct, :note, :note, :is_active)
        ON CONFLICT (course_id) DO UPDATE SET
            course_name = EXCLUDED.course_name,
            amount = EXCLUDED.amount,
            base_amount = EXCLUDED.base_amount,
            currency = EXCLUDED.currency,
            discount_pct = EXCLUDED.discount_pct,
            note = EXCLUDED.note,
            description = EXCLUDED.description,
            is_active = EXCLUDED.is_active
        RETURNING *
    """)
    row = db.execute(query, payload.model_dump()).mappings().first()
    db.commit()
    return {
        "success": True,
        "message": "Cập nhật biểu phí khóa học thành công",
        "data": dict(row),
    }


@router.get("/vouchers")
def get_vouchers(
    is_active: Optional[bool] = None,
    db: Session = Depends(get_db),
    user: dict = Depends(require_roles(["ADMIN", "STAFF"])),
):
    if is_active is not None:
        rows = db.execute(
            text("SELECT * FROM finance.vouchers WHERE is_active = :is_active ORDER BY id DESC"),
            {"is_active": is_active},
        ).mappings().all()
    else:
        rows = db.execute(
            text("SELECT * FROM finance.vouchers ORDER BY id DESC")
        ).mappings().all()
    return {
        "success": True,
        "data": [dict(r) for r in rows],
    }


@router.post("/vouchers", status_code=status.HTTP_201_CREATED)
def create_voucher(
    payload: VoucherCreate,
    db: Session = Depends(get_db),
    user: dict = Depends(require_roles(["ADMIN", "STAFF"])),
):
    code_upper = payload.code.strip().upper()
    existing = db.execute(
        text("SELECT id FROM finance.vouchers WHERE code = :code"),
        {"code": code_upper},
    ).mappings().first()
    if existing:
        raise FinanceException(
            status_code=status.HTTP_409_CONFLICT,
            code="FIN_VOUCHER_CODE_EXISTS",
            message=f"Mã voucher {code_upper} đã tồn tại",
        )

    valid_from = payload.valid_from or datetime.now()
    if payload.expires_at <= valid_from:
        raise FinanceException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            code="FIN_VOUCHER_INVALID_DATES",
            message="Thời điểm hết hạn (expires_at) phải lớn hơn thời điểm bắt đầu (valid_from)",
        )

    query = text("""
        INSERT INTO finance.vouchers
            (code, campaign_name, discount_percent, discount_amount, min_order_value,
             max_discount_amount, max_uses, valid_from, expires_at, is_active)
        VALUES
            (:code, :campaign_name, :discount_percent, :discount_amount, :min_order_value,
             :max_discount_amount, :max_uses, :valid_from, :expires_at, :is_active)
        RETURNING *
    """)
    row = db.execute(
        query,
        {
            "code": code_upper,
            "campaign_name": payload.campaign_name,
            "discount_percent": payload.discount_percent,
            "discount_amount": payload.discount_amount,
            "min_order_value": payload.min_order_value,
            "max_discount_amount": payload.max_discount_amount,
            "max_uses": payload.max_uses,
            "valid_from": valid_from,
            "expires_at": payload.expires_at,
            "is_active": payload.is_active,
        },
    ).mappings().first()
    db.commit()
    return {
        "success": True,
        "message": "Tạo chiến dịch voucher thành công",
        "data": dict(row),
    }


def calculate_voucher_discount(voucher_row: dict, order_amount: float) -> float:
    if not voucher_row["is_active"]:
        raise FinanceException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            code="FIN_VOUCHER_INACTIVE",
            message="Mã voucher đã ngừng hoạt động",
        )
    now = datetime.now()
    expires_at = voucher_row["expires_at"]
    if expires_at and expires_at.replace(tzinfo=None) < now:
        raise FinanceException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            code="FIN_VOUCHER_EXPIRED",
            message="Mã voucher đã hết hạn sử dụng",
        )
    if voucher_row["current_uses"] >= voucher_row["max_uses"]:
        raise FinanceException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            code="FIN_VOUCHER_EXHAUSTED",
            message="Mã voucher đã hết lượt sử dụng",
        )
    min_order = float(voucher_row["min_order_value"] or 0)
    if order_amount < min_order:
        raise FinanceException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            code="FIN_VOUCHER_MIN_ORDER_NOT_MET",
            message=f"Giá trị đơn hàng tối thiểu để áp dụng voucher là {min_order:,.0f} VND",
        )

    discount_pct = int(voucher_row["discount_percent"] or 0)
    discount_amt = float(voucher_row["discount_amount"] or 0)
    if discount_pct > 0:
        disc = order_amount * (discount_pct / 100.0)
        max_disc = voucher_row["max_discount_amount"]
        if max_disc is not None and float(max_disc) > 0:
            disc = min(disc, float(max_disc))
    else:
        disc = discount_amt

    return min(round(disc, 2), order_amount)


@router.post("/vouchers/validate")
def validate_voucher(
    payload: VoucherValidateRequest,
    db: Session = Depends(get_db),
    user: dict = Depends(get_current_user),
):
    code_upper = payload.code.strip().upper()
    row = db.execute(
        text("SELECT * FROM finance.vouchers WHERE code = :code"),
        {"code": code_upper},
    ).mappings().first()
    if not row:
        raise FinanceException(
            status_code=status.HTTP_404_NOT_FOUND,
            code="FIN_VOUCHER_NOT_FOUND",
            message=f"Không tìm thấy mã voucher {code_upper}",
        )

    discount_applied = calculate_voucher_discount(dict(row), payload.order_amount)
    final_amount = max(0.0, payload.order_amount - discount_applied)
    return {
        "success": True,
        "message": "Mã voucher hợp lệ",
        "data": {
            "voucher_id": row["id"],
            "code": row["code"],
            "campaign_name": row["campaign_name"],
            "order_amount": payload.order_amount,
            "discount_applied": discount_applied,
            "final_amount": final_amount,
        },
    }
