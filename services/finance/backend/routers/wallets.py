from fastapi import APIRouter, Depends, status
from sqlalchemy import text
from sqlalchemy.orm import Session

from auth import FinanceException, require_roles, verify_internal_secret
from database import get_db
from routers.payments import _notify_admission_payment_success, _record_payment_transaction
from schemas import WalletReserveRequest

router = APIRouter(prefix="/api/finance", tags=["Student Wallets"])


def _get_or_create_wallet(db: Session, student_id: int, lock: bool = False, auto_commit: bool = True) -> dict:
    """
    Lấy ví bảo lưu của học viên hoặc tự động khởi tạo ví mới với số dư 0 (is_withdrawable = FALSE).

    :param db: Phiên làm việc SQLAlchemy
    :param student_id: ID người dùng học viên (admission.users.id theo quy ước R-ID-1)
    :param lock: Có khóa bi quan SELECT ... FOR UPDATE hay không
    :param auto_commit: Có commit ngay khi tạo ví mới hay chỉ flush trong transaction hiện tại
    :return: Dict thông tin bản ghi student_wallets
    """
    lock_clause = "FOR UPDATE" if lock else ""
    w_row = db.execute(
        text(f"SELECT * FROM finance.student_wallets WHERE student_id = :student_id {lock_clause}"),
        {"student_id": student_id},
    ).mappings().first()
    if w_row:
        return dict(w_row)

    new_row = db.execute(
        text("""
            INSERT INTO finance.student_wallets (student_id, balance, is_withdrawable, status)
            VALUES (:student_id, 0, FALSE, 'ACTIVE')
            ON CONFLICT (student_id) DO UPDATE SET student_id = EXCLUDED.student_id
            RETURNING *
        """),
        {"student_id": student_id},
    ).mappings().first()
    if auto_commit:
        db.commit()
    else:
        db.flush()
    return dict(new_row)


@router.get("/internal/wallets")
def get_internal_wallet(
    student_id: int,
    db: Session = Depends(get_db),
    _authorized: bool = Depends(verify_internal_secret),
):
    """API nội bộ tra cứu số dư ví bảo lưu của học viên (yêu cầu X-Internal-Secret hoặc ADMIN/STAFF)."""
    wallet = _get_or_create_wallet(db, student_id, lock=False)
    return {
        "success": True,
        "data": {
            "wallet_id": wallet["id"],
            "student_id": wallet["student_id"],
            "balance": float(wallet["balance"]),
            "is_withdrawable": wallet["is_withdrawable"],
            "status": wallet["status"],
        },
    }


@router.get("/wallets/{student_id}")
def get_student_wallet(
    student_id: int,
    db: Session = Depends(get_db),
    user: dict = Depends(require_roles(["ADMIN", "STAFF", "STUDENT"])),
):
    """
    Tra cứu thông tin ví bảo lưu và lịch sử giao dịch của học viên (ADMIN, STAFF, STUDENT).
    Học viên chỉ được tra cứu ví của chính mình (INV-SEC-01).
    """
    if user["role"] == "STUDENT" and user["user_id"] != student_id:
        raise FinanceException(
            status_code=status.HTTP_403_FORBIDDEN,
            code="AUTH_FORBIDDEN",
            message="Học viên chỉ được tra cứu ví bảo lưu của chính mình",
        )

    wallet = _get_or_create_wallet(db, student_id, lock=False)
    tx_rows = db.execute(
        text("""
            SELECT * FROM finance.wallet_transactions
            WHERE wallet_id = :wallet_id
            ORDER BY created_at DESC, id DESC
        """),
        {"wallet_id": wallet["id"]},
    ).mappings().all()

    return {
        "success": True,
        "data": {
            **wallet,
            "transactions": [dict(t) for t in tx_rows],
        },
    }


@router.post("/wallets/{student_id}/reserve", status_code=status.HTTP_201_CREATED)
def process_wallet_transaction(
    student_id: int,
    payload: WalletReserveRequest,
    db: Session = Depends(get_db),
    user: dict = Depends(require_roles(["ADMIN", "STAFF"])),
):
    """
    Thực hiện giao dịch ví bảo lưu: cộng tiền bảo lưu (RESERVATION_CREDIT),
    cấn trừ học phí (TUITION_OFFSET) hoặc điều chỉnh (ADJUSTMENT) trong cùng 1 transaction nguyên tử.
    """
    wallet = _get_or_create_wallet(db, student_id, lock=True, auto_commit=False)
    if wallet["status"] != "ACTIVE":
        raise FinanceException(
            status_code=status.HTTP_409_CONFLICT,
            code="FIN_WALLET_FROZEN",
            message="Ví bảo lưu của học viên đang bị khóa (FROZEN)",
        )

    current_balance = float(wallet["balance"])
    amount = round(float(payload.amount), 2)

    # Bước 1: Kiểm tra nghiệp vụ trước khi trừ ví nếu là TUITION_OFFSET
    if payload.tx_type == "TUITION_OFFSET":
        if not payload.related_invoice_id:
            raise FinanceException(
                status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                code="FIN_INVOICE_REQUIRED",
                message="Giao dịch cấn trừ học phí (TUITION_OFFSET) bắt buộc phải có related_invoice_id",
            )
        inv = db.execute(
            text("SELECT * FROM finance.invoices WHERE id = :id FOR UPDATE"),
            {"id": payload.related_invoice_id},
        ).mappings().first()
        if not inv:
            raise FinanceException(
                status_code=status.HTTP_404_NOT_FOUND,
                code="FIN_INVOICE_NOT_FOUND",
                message=f"Không tìm thấy hóa đơn #{payload.related_invoice_id}",
            )
        if int(inv["student_id"]) != student_id:
            raise FinanceException(
                status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                code="FIN_WALLET_INVOICE_MISMATCH",
                message="Hóa đơn không thuộc học viên sở hữu ví bảo lưu này",
            )
        remaining = round(float(inv["final_amount"]) - float(inv["paid_amount"]), 2)
        if inv["status"] in ("CANCELLED", "REFUNDED") or remaining <= 0:
            raise FinanceException(
                status_code=status.HTTP_409_CONFLICT,
                code="FIN_INVOICE_NOT_PAYABLE",
                message=f"Hóa đơn #{payload.related_invoice_id} không ở trạng thái có thể cấn trừ",
            )
        if amount > remaining:
            raise FinanceException(
                status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                code="FIN_WALLET_OFFSET_EXCEEDS_REMAINING",
                message=f"Số tiền cấn trừ ({amount:,.0f} VND) vượt quá dư nợ hóa đơn ({remaining:,.0f} VND)",
            )
        if current_balance < amount:
            raise FinanceException(
                status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                code="FIN_WALLET_INSUFFICIENT_BALANCE",
                message=f"Số dư ví bảo lưu ({current_balance:,.0f} VND) không đủ để cấn trừ {amount:,.0f} VND",
            )
        new_balance = round(current_balance - amount, 2)
    else:
        new_balance = round(current_balance + amount, 2)

    # Bước 2: Cập nhật số dư ví (giữ nguyên bất biến is_withdrawable = FALSE)
    updated_wallet = db.execute(
        text("""
            UPDATE finance.student_wallets
            SET balance = :balance, is_withdrawable = FALSE
            WHERE id = :id
            RETURNING *
        """),
        {"balance": new_balance, "id": wallet["id"]},
    ).mappings().first()

    # Bước 3: Ghi sổ cái giao dịch ví
    staff_id = payload.created_by_staff_id or user.get("user_id") or 2
    tx_row = db.execute(
        text("""
            INSERT INTO finance.wallet_transactions
                (wallet_id, tx_type, amount, related_invoice_id, reason, created_by_staff_id)
            VALUES
                (:wallet_id, :tx_type, :amount, :related_invoice_id, :reason, :created_by_staff_id)
            RETURNING *
        """),
        {
            "wallet_id": wallet["id"],
            "tx_type": payload.tx_type,
            "amount": amount,
            "related_invoice_id": payload.related_invoice_id,
            "reason": payload.reason,
            "created_by_staff_id": staff_id,
        },
    ).mappings().first()

    # Bước 4: Nếu là TUITION_OFFSET thì gạch nợ hóa đơn trong cùng transaction trước khi commit
    paid_inv_to_notify = None
    paid_pay_to_notify = None
    if payload.tx_type == "TUITION_OFFSET" and payload.related_invoice_id:
        pay_res, inv_res, _ = _record_payment_transaction(
            db=db,
            invoice_id=payload.related_invoice_id,
            amount=amount,
            payment_method="WALLET_OFFSET",
            received_by_staff_id=staff_id,
            commit=False,
        )
        if inv_res.get("status") == "PAID":
            paid_inv_to_notify = inv_res
            paid_pay_to_notify = pay_res

    db.commit()

    if paid_inv_to_notify and paid_pay_to_notify:
        _notify_admission_payment_success(paid_inv_to_notify, paid_pay_to_notify)

    return {
        "success": True,
        "message": (
            "Cộng tiền bảo lưu vào ví học viên thành công"
            if payload.tx_type == "RESERVATION_CREDIT"
            else "Cấn trừ học phí từ ví bảo lưu thành công"
        ),
        "data": {
            "wallet": dict(updated_wallet),
            "transaction": dict(tx_row),
        },
    }
