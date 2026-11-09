from fastapi import APIRouter, Depends, status
from sqlalchemy import text
from sqlalchemy.orm import Session

from auth import FinanceException, get_current_user, require_roles, verify_internal_secret
from database import get_db
from routers.payments import _record_payment_transaction
from schemas import WalletReserveRequest

router = APIRouter(prefix="/api/finance", tags=["Student Wallets"])


def _get_or_create_wallet(db: Session, student_id: int, lock: bool = False) -> dict:
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
    db.commit()
    return dict(new_row)


@router.get("/internal/wallets")
def get_internal_wallet(
    student_id: int,
    db: Session = Depends(get_db),
    _authorized: bool = Depends(verify_internal_secret),
):
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
    user: dict = Depends(get_current_user),
):
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
    wallet = _get_or_create_wallet(db, student_id, lock=True)
    if wallet["status"] != "ACTIVE":
        raise FinanceException(
            status_code=status.HTTP_409_CONFLICT,
            code="FIN_WALLET_FROZEN",
            message="Ví bảo lưu của học viên đang bị khóa (FROZEN)",
        )

    current_balance = float(wallet["balance"])
    amount = round(float(payload.amount), 2)

    if payload.tx_type == "RESERVATION_CREDIT":
        new_balance = round(current_balance + amount, 2)
    elif payload.tx_type == "TUITION_OFFSET":
        if current_balance < amount:
            raise FinanceException(
                status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                code="FIN_WALLET_INSUFFICIENT_BALANCE",
                message=f"Số dư ví bảo lưu ({current_balance:,.0f} VND) không đủ để cấn trừ {amount:,.0f} VND",
            )
        new_balance = round(current_balance - amount, 2)
    else:
        new_balance = round(current_balance + amount, 2)

    updated_wallet = db.execute(
        text("""
            UPDATE finance.student_wallets
            SET balance = :balance, is_withdrawable = FALSE
            WHERE id = :id
            RETURNING *
        """),
        {"balance": new_balance, "id": wallet["id"]},
    ).mappings().first()

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

    db.commit()

    if payload.tx_type == "TUITION_OFFSET" and payload.related_invoice_id:
        _record_payment_transaction(
            db=db,
            invoice_id=payload.related_invoice_id,
            amount=amount,
            payment_method="WALLET_OFFSET",
            received_by_staff_id=staff_id,
        )

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
