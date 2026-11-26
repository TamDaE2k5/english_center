from datetime import datetime, timezone
from typing import Optional
from fastapi import APIRouter, Depends, status
from sqlalchemy import text
from sqlalchemy.orm import Session

from auth import FinanceException, require_roles
from database import get_db
from schemas import TuitionFeeCreate, VoucherCreate, VoucherValidateRequest

router = APIRouter(prefix="/api/finance", tags=["Tuition & Vouchers"])


def _to_naive_utc(dt: Optional[datetime]) -> Optional[datetime]:
    """Chuẩn hóa datetime có hoặc không có múi giờ về naive UTC để so sánh an toàn với PostgreSQL TIMESTAMP."""
    if dt is None:
        return None
    if dt.tzinfo is not None:
        return dt.astimezone(timezone.utc).replace(tzinfo=None)
    return dt


@router.get("/tuition-fees")
def get_tuition_fees(db: Session = Depends(get_db)):
    """Lấy danh mục biểu phí chuẩn theo khóa học (công khai cho tra cứu học phí)."""
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
    """Tạo mới hoặc cập nhật biểu phí cho một khóa học (ADMIN, STAFF)."""
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
    """Lấy danh sách chiến dịch Voucher (ADMIN, STAFF)."""
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
    """Tạo chiến dịch mã giảm giá Voucher mới (ADMIN, STAFF)."""
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

    # Bước 1: Chuẩn hóa thời gian về naive UTC để khớp cột TIMESTAMP của PostgreSQL
    valid_from = _to_naive_utc(payload.valid_from) or datetime.utcnow()
    expires_at = _to_naive_utc(payload.expires_at)
    if expires_at <= valid_from:
        raise FinanceException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            code="FIN_VOUCHER_INVALID_DATES",
            message="Thời điểm hết hạn (expires_at) phải lớn hơn thời điểm bắt đầu (valid_from)",
        )

    # Bước 2: Lưu bản ghi chiến dịch Voucher vào CSDL
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
            "expires_at": expires_at,
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
    """
    Kiểm tra toàn bộ điều kiện hiệu lực Voucher và tính số tiền được giảm.

    :param voucher_row: Dữ liệu bản ghi Voucher từ DB
    :param order_amount: Giá trị gốc của đơn hàng/hóa đơn
    :return: Số tiền được chiết khấu (không vượt quá order_amount)
    :raises FinanceException: 422 nếu voucher bị tắt, chưa bắt đầu, hết hạn, hết lượt hoặc chưa đạt đơn tối thiểu
    """
    # Bước 1: Kiểm tra trạng thái kích hoạt
    if not voucher_row["is_active"]:
        raise FinanceException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            code="FIN_VOUCHER_INACTIVE",
            message="Mã voucher đã ngừng hoạt động",
        )

    # Bước 2: Kiểm tra cửa sổ thời gian hiệu lực (valid_from <= now <= expires_at) theo UTC
    now = datetime.utcnow()
    valid_from = _to_naive_utc(voucher_row.get("valid_from"))
    if valid_from and valid_from > now:
        raise FinanceException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            code="FIN_VOUCHER_NOT_STARTED",
            message="Mã voucher chưa đến thời gian áp dụng",
        )
    expires_at = _to_naive_utc(voucher_row.get("expires_at"))
    if expires_at and expires_at < now:
        raise FinanceException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            code="FIN_VOUCHER_EXPIRED",
            message="Mã voucher đã hết hạn sử dụng",
        )

    # Bước 3: Kiểm tra hạn mức lượt sử dụng và giá trị đơn tối thiểu
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

    # Bước 4: Tính số tiền giảm theo phần trăm (có trần tối đa) hoặc giảm tiền mặt cố định
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
    user: dict = Depends(require_roles(["ADMIN", "STAFF", "STUDENT"])),
):
    """Kiểm tra tính hợp lệ của Voucher và tính số tiền giảm dự kiến (ADMIN, STAFF, STUDENT)."""
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
    final_amount = max(0.0, round(payload.order_amount - discount_applied, 2))
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
