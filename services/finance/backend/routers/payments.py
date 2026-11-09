import os
import re
import uuid
from datetime import datetime
from typing import Optional
import requests
from fastapi import APIRouter, Depends, status
from sqlalchemy import text
from sqlalchemy.orm import Session

from auth import FinanceException, INTERNAL_SECRET_KEY, get_current_user, require_roles
from database import get_db
from schemas import CashPaymentRequest, PaymentCreate, VietQRWebhookRequest

router = APIRouter(prefix="/api/finance", tags=["Payments & VietQR"])

ADMISSION_SERVICE_URL = os.getenv("ADMISSION_SERVICE_URL", "http://admission-service:8081")
VIETQR_BANK_BIN = os.getenv("VIETQR_BANK_BIN", "970436")
VIETQR_ACCOUNT_NO = os.getenv("VIETQR_ACCOUNT_NO", "0071000888999")
VIETQR_ACCOUNT_NAME = os.getenv("VIETQR_ACCOUNT_NAME", "TRUNG TAM ANH NGU ENGLISH CENTER")


def _notify_admission_payment_success(inv_row: dict, payment_row: dict):
    enrollment_id = inv_row.get("enrollment_id")
    if not enrollment_id:
        return
    paid_at_val = payment_row.get("paid_at")
    paid_at_iso = paid_at_val.isoformat() if hasattr(paid_at_val, "isoformat") else str(paid_at_val)
    payload = {
        "event_id": f"evt_pay_{payment_row['id']}_{uuid.uuid4().hex[:8]}",
        "enrollment_id": enrollment_id,
        "invoice_id": inv_row["id"],
        "invoice_code": inv_row["invoice_code"],
        "amount_paid": float(inv_row["paid_amount"]),
        "payment_method": payment_row["payment_method"],
        "paid_at": paid_at_iso,
    }
    try:
        requests.post(
            f"{ADMISSION_SERVICE_URL}/api/admission/webhooks/payment-success",
            json=payload,
            headers={"X-Internal-Secret": INTERNAL_SECRET_KEY},
            timeout=3,
        )
    except Exception:
        pass


def _record_payment_transaction(
    db: Session,
    invoice_id: int,
    amount: float,
    payment_method: str,
    installment_id: Optional[int] = None,
    bank_transaction_id: Optional[str] = None,
    qr_reference_code: Optional[str] = None,
    received_by_staff_id: Optional[int] = None,
    webhook_payload: Optional[dict] = None,
) -> tuple[dict, dict, bool]:
    if bank_transaction_id:
        existing_pay = db.execute(
            text("""
                SELECT * FROM finance.payments
                WHERE bank_transaction_id = :tx OR transaction_ref = :tx
                LIMIT 1
            """),
            {"tx": bank_transaction_id},
        ).mappings().first()
        if existing_pay:
            inv_existing = db.execute(
                text("SELECT * FROM finance.invoices WHERE id = :id"),
                {"id": existing_pay["invoice_id"]},
            ).mappings().first()
            return dict(existing_pay), dict(inv_existing), True

    inv = db.execute(
        text("SELECT * FROM finance.invoices WHERE id = :id FOR UPDATE"),
        {"id": invoice_id},
    ).mappings().first()
    if not inv:
        raise FinanceException(
            status_code=status.HTTP_404_NOT_FOUND,
            code="FIN_INVOICE_NOT_FOUND",
            message=f"Không tìm thấy hóa đơn #{invoice_id}",
        )

    if inv["status"] in ("CANCELLED", "REFUNDED"):
        raise FinanceException(
            status_code=status.HTTP_409_CONFLICT,
            code="FIN_INVOICE_IMMUTABLE",
            message=f"Không thể thu tiền cho hóa đơn ở trạng thái {inv['status']}",
        )
    if inv["status"] == "PAID" and float(inv["paid_amount"]) >= float(inv["final_amount"]):
        raise FinanceException(
            status_code=status.HTTP_409_CONFLICT,
            code="FIN_INVOICE_ALREADY_PAID",
            message=f"Hóa đơn {inv['invoice_code']} đã được thanh toán đủ",
        )

    remaining = round(float(inv["final_amount"]) - float(inv["paid_amount"]), 2)
    effective_amount = min(round(float(amount), 2), remaining) if remaining > 0 else round(float(amount), 2)

    target_inst_id = installment_id
    if not target_inst_id:
        inst = db.execute(
            text("""
                SELECT * FROM finance.installments
                WHERE invoice_id = :invoice_id AND status IN ('PENDING', 'PARTIAL', 'OVERDUE')
                ORDER BY installment_no ASC
                LIMIT 1
                FOR UPDATE
            """),
            {"invoice_id": invoice_id},
        ).mappings().first()
        if inst:
            target_inst_id = inst["id"]

    if target_inst_id:
        inst_row = db.execute(
            text("SELECT * FROM finance.installments WHERE id = :id FOR UPDATE"),
            {"id": target_inst_id},
        ).mappings().first()
        if inst_row:
            new_inst_paid = min(
                float(inst_row["amount_due"]),
                round(float(inst_row["amount_paid"]) + effective_amount, 2),
            )
            new_inst_status = "PAID" if new_inst_paid >= float(inst_row["amount_due"]) else "PARTIAL"
            db.execute(
                text("""
                    UPDATE finance.installments
                    SET amount_paid = :amount_paid,
                        status = :status,
                        paid_at = CASE WHEN :status = 'PAID' THEN CURRENT_TIMESTAMP ELSE paid_at END
                    WHERE id = :id
                """),
                {"amount_paid": new_inst_paid, "status": new_inst_status, "id": target_inst_id},
            )

    import json
    pay_row = db.execute(
        text("""
            INSERT INTO finance.payments
                (invoice_id, installment_id, payment_method, amount,
                 bank_transaction_id, transaction_ref, qr_reference_code,
                 webhook_payload, received_by_staff_id, status)
            VALUES
                (:invoice_id, :installment_id, :payment_method, :amount,
                 :bank_transaction_id, :bank_transaction_id, :qr_reference_code,
                 CAST(:webhook_payload AS JSONB), :received_by_staff_id, 'SUCCESS')
            RETURNING *
        """),
        {
            "invoice_id": invoice_id,
            "installment_id": target_inst_id,
            "payment_method": payment_method,
            "amount": effective_amount,
            "bank_transaction_id": bank_transaction_id,
            "qr_reference_code": qr_reference_code,
            "webhook_payload": json.dumps(webhook_payload) if webhook_payload else None,
            "received_by_staff_id": received_by_staff_id,
        },
    ).mappings().first()

    new_paid = min(float(inv["final_amount"]), round(float(inv["paid_amount"]) + effective_amount, 2))
    new_status = "PAID" if new_paid >= float(inv["final_amount"]) else "PARTIAL"
    updated_inv = db.execute(
        text("""
            UPDATE finance.invoices
            SET paid_amount = :paid_amount,
                status = :status,
                paid_at = CASE WHEN :status = 'PAID' THEN CURRENT_TIMESTAMP ELSE paid_at END
            WHERE id = :id
            RETURNING *
        """),
        {"paid_amount": new_paid, "status": new_status, "id": invoice_id},
    ).mappings().first()

    db.commit()

    if updated_inv["status"] == "PAID":
        _notify_admission_payment_success(dict(updated_inv), dict(pay_row))

    return dict(pay_row), dict(updated_inv), False


@router.get("/payments")
def get_payments(
    invoice_id: Optional[int] = None,
    db: Session = Depends(get_db),
    user: dict = Depends(get_current_user),
):
    if invoice_id:
        rows = db.execute(
            text("SELECT * FROM finance.payments WHERE invoice_id = :invoice_id ORDER BY paid_at DESC, id DESC"),
            {"invoice_id": invoice_id},
        ).mappings().all()
    else:
        rows = db.execute(
            text("SELECT * FROM finance.payments ORDER BY paid_at DESC, id DESC")
        ).mappings().all()
    return {
        "success": True,
        "data": [dict(r) for r in rows],
    }


@router.post("/payments", status_code=status.HTTP_201_CREATED)
def make_payment(
    payment: PaymentCreate,
    db: Session = Depends(get_db),
    user: dict = Depends(get_current_user),
):
    tx_ref = payment.bank_transaction_id or payment.transaction_ref
    method = payment.payment_method.upper()
    if method not in ("CASH", "VIETQR", "WALLET_OFFSET", "BANK_TRANSFER", "CARD", "MOMO"):
        method = "BANK_TRANSFER"

    pay_row, inv_row, is_dup = _record_payment_transaction(
        db=db,
        invoice_id=payment.invoice_id,
        amount=payment.amount,
        payment_method=method,
        installment_id=payment.installment_id,
        bank_transaction_id=tx_ref,
        received_by_staff_id=user.get("user_id") if user.get("role") in ("ADMIN", "STAFF") else None,
    )
    return {
        "success": True,
        "message": "Giao dịch đã được ghi nhận trước đó (Idempotent)" if is_dup else "Ghi nhận thanh toán thành công",
        "data": {
            **pay_row,
            "invoice_status": inv_row["status"],
            "invoice_paid_amount": float(inv_row["paid_amount"]),
            "invoice_remaining_amount": float(inv_row["remaining_amount"]),
        },
    }


@router.post("/payments/cash", status_code=status.HTTP_201_CREATED)
def record_cash_payment(
    payload: CashPaymentRequest,
    db: Session = Depends(get_db),
    user: dict = Depends(require_roles(["ADMIN", "STAFF"])),
):
    staff_id = payload.received_by_staff_id or user.get("user_id") or 2
    pay_row, inv_row, _ = _record_payment_transaction(
        db=db,
        invoice_id=payload.invoice_id,
        amount=payload.amount,
        payment_method="CASH",
        installment_id=payload.installment_id,
        received_by_staff_id=staff_id,
    )
    return {
        "success": True,
        "message": "Ghi nhận phiếu thu tiền mặt tại quầy thành công",
        "data": {
            **pay_row,
            "invoice_code": inv_row["invoice_code"],
            "invoice_status": inv_row["status"],
            "invoice_paid_amount": float(inv_row["paid_amount"]),
            "invoice_remaining_amount": float(inv_row["remaining_amount"]),
        },
    }


@router.get("/invoices/{invoice_id}/vietqr")
def generate_vietqr(
    invoice_id: int,
    installment_id: Optional[int] = None,
    db: Session = Depends(get_db),
    user: dict = Depends(get_current_user),
):
    inv = db.execute(
        text("SELECT * FROM finance.invoices WHERE id = :id"),
        {"id": invoice_id},
    ).mappings().first()
    if not inv:
        raise FinanceException(
            status_code=status.HTTP_404_NOT_FOUND,
            code="FIN_INVOICE_NOT_FOUND",
            message=f"Không tìm thấy hóa đơn #{invoice_id}",
        )

    inst = None
    if installment_id:
        inst = db.execute(
            text("SELECT * FROM finance.installments WHERE id = :id AND invoice_id = :invoice_id"),
            {"id": installment_id, "invoice_id": invoice_id},
        ).mappings().first()
    else:
        inst = db.execute(
            text("""
                SELECT * FROM finance.installments
                WHERE invoice_id = :invoice_id AND status IN ('PENDING', 'PARTIAL', 'OVERDUE')
                ORDER BY installment_no ASC
                LIMIT 1
            """),
            {"invoice_id": invoice_id},
        ).mappings().first()

    if inst:
        amount_due = round(float(inst["amount_due"]) - float(inst["amount_paid"]), 2)
        payment_ref_code = inst["payment_ref_code"]
    else:
        amount_due = round(float(inv["remaining_amount"]), 2)
        payment_ref_code = inv["invoice_code"]

    qr_content = f"ECPAY {payment_ref_code}"
    qr_image_url = (
        f"https://img.vietqr.io/image/{VIETQR_BANK_BIN}-{VIETQR_ACCOUNT_NO}-compact2.png"
        f"?amount={int(amount_due)}&addInfo={qr_content.replace(' ', '%20')}"
        f"&accountName={VIETQR_ACCOUNT_NAME.replace(' ', '%20')}"
    )

    return {
        "success": True,
        "data": {
            "invoice_id": inv["id"],
            "invoice_code": inv["invoice_code"],
            "installment_id": inst["id"] if inst else None,
            "payment_ref_code": payment_ref_code,
            "amount_due": amount_due,
            "bank_bin": VIETQR_BANK_BIN,
            "account_no": VIETQR_ACCOUNT_NO,
            "account_name": VIETQR_ACCOUNT_NAME,
            "transfer_content": qr_content,
            "qr_image_url": qr_image_url,
        },
    }


@router.post("/webhooks/vietqr")
def handle_vietqr_webhook(
    payload: VietQRWebhookRequest,
    db: Session = Depends(get_db),
):
    existing = db.execute(
        text("""
            SELECT * FROM finance.payments
            WHERE bank_transaction_id = :tx OR transaction_ref = :tx
            LIMIT 1
        """),
        {"tx": payload.bank_transaction_id},
    ).mappings().first()
    if existing:
        return {
            "success": True,
            "message": "Giao dịch VietQR đã được xử lý trước đó (Idempotent)",
            "data": {
                **dict(existing),
                "is_idempotent": True,
            },
        }

    target_invoice_id = payload.invoice_id
    target_installment_id = None
    ref_code = payload.qr_reference_code

    if not ref_code and payload.content:
        match = re.search(r"(INV-\d{4}-\d+(?:-K\d+)?)", payload.content.upper())
        if match:
            ref_code = match.group(1)

    if ref_code and not target_invoice_id:
        inst = db.execute(
            text("SELECT * FROM finance.installments WHERE payment_ref_code = :ref"),
            {"ref": ref_code},
        ).mappings().first()
        if inst:
            target_invoice_id = inst["invoice_id"]
            target_installment_id = inst["id"]
        else:
            inv = db.execute(
                text("SELECT * FROM finance.invoices WHERE invoice_code = :code"),
                {"code": ref_code},
            ).mappings().first()
            if inv:
                target_invoice_id = inv["id"]

    if not target_invoice_id:
        raise FinanceException(
            status_code=status.HTTP_404_NOT_FOUND,
            code="FIN_WEBHOOK_INVOICE_NOT_FOUND",
            message="Không đối chiếu được hóa đơn từ mã tham chiếu VietQR",
        )

    pay_row, inv_row, is_dup = _record_payment_transaction(
        db=db,
        invoice_id=target_invoice_id,
        amount=payload.amount,
        payment_method="VIETQR",
        installment_id=target_installment_id,
        bank_transaction_id=payload.bank_transaction_id,
        qr_reference_code=ref_code,
        webhook_payload=payload.model_dump(mode="json"),
    )

    return {
        "success": True,
        "message": "Đối soát và gạch nợ tự động VietQR thành công",
        "data": {
            **pay_row,
            "invoice_code": inv_row["invoice_code"],
            "invoice_status": inv_row["status"],
            "invoice_paid_amount": float(inv_row["paid_amount"]),
            "invoice_remaining_amount": float(inv_row["remaining_amount"]),
            "is_idempotent": is_dup,
        },
    }
