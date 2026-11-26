import json
import logging
import os
import re
from typing import Dict, Optional, Tuple
import requests

from fastapi import APIRouter, Depends, status
from sqlalchemy import text
from sqlalchemy.orm import Session

from auth import FinanceException, INTERNAL_SECRET_KEY, require_roles
from database import get_db
from schemas import CashPaymentRequest, PaymentCreate, VietQRWebhookRequest

router = APIRouter(prefix="/api/finance", tags=["Payments & VietQR"])
logger = logging.getLogger("finance-service.payments")

ADMISSION_SERVICE_URL = os.getenv("ADMISSION_SERVICE_URL", "http://admission-service:8081")
VIETQR_BANK_BIN = os.getenv("VIETQR_BANK_BIN", "970436")
VIETQR_ACCOUNT_NO = os.getenv("VIETQR_ACCOUNT_NO", "0071000888999")
VIETQR_ACCOUNT_NAME = os.getenv("VIETQR_ACCOUNT_NAME", "TRUNG TAM ANH NGU ENGLISH CENTER")


def _notify_admission_payment_success(inv_row: dict, payment_row: dict):
    """
    Gửi webhook thông báo thanh toán đủ hóa đơn sang Admission Service để tự động duyệt đơn (TASK-ADM-45).
    Dùng event_id tất định theo payment_row['id'] để đảm bảo idempotency khi retry.
    """
    enrollment_id = inv_row.get("enrollment_id")
    if not enrollment_id:
        return
    paid_at_val = payment_row.get("paid_at")
    paid_at_iso = paid_at_val.isoformat() if hasattr(paid_at_val, "isoformat") else str(paid_at_val)
    payload = {
        "event_id": f"evt_pay_{payment_row['id']}",
        "enrollment_id": enrollment_id,
        "invoice_id": str(inv_row["invoice_code"]),
        "amount_paid": float(inv_row["paid_amount"]),
        "payment_method": payment_row["payment_method"],
        "transaction_ref": payment_row.get("transaction_ref") or payment_row.get("payment_code"),
        "invoice_status": inv_row.get("status", "PAID"),
        "paid_at": paid_at_iso,
    }
    try:
        res = requests.post(
            f"{ADMISSION_SERVICE_URL}/api/admission/webhooks/payment-success",
            json=payload,
            headers={"X-Internal-Secret": INTERNAL_SECRET_KEY},
            timeout=5,
        )
        if res.status_code >= 400:
            logger.warning(
                "Admission payment-success webhook trả về HTTP %s cho enrollment_id=%s: %s",
                res.status_code,
                enrollment_id,
                res.text[:300],
            )
    except Exception as exc:
        logger.warning(
            "Không thể gửi payment-success webhook sang Admission cho enrollment_id=%s: %s",
            enrollment_id,
            exc,
        )


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
    commit: bool = True,
) -> Tuple[Dict, Dict, bool]:
    """
    Ghi nhận giao dịch thu tiền trong DB transaction kèm khóa bi quan và idempotency (INV-FIN-02, INV-FIN-03).

    :param db: Phiên làm việc SQLAlchemy
    :param invoice_id: ID hóa đơn cần thanh toán
    :param amount: Số tiền giao dịch (> 0)
    :param payment_method: Phương thức thanh toán (CASH, VIETQR, WALLET_OFFSET, ...)
    :param installment_id: ID kỳ thanh toán chỉ định (tùy chọn)
    :param bank_transaction_id: Mã tham chiếu giao dịch duy nhất để chống ghi trùng
    :param qr_reference_code: Mã tham chiếu VietQR (nếu có)
    :param received_by_staff_id: ID nhân viên thu tiền
    :param webhook_payload: Dữ liệu gốc webhook ngân hàng
    :param commit: Có commit ngay và bắn webhook Admission hay để caller tự commit
    :return: Tuple (payment_dict, invoice_dict, is_duplicate)
    """
    clean_tx_ref = bank_transaction_id.strip() if bank_transaction_id and bank_transaction_id.strip() else None

    # Bước 1: Kiểm tra idempotency theo bank_transaction_id / transaction_ref (INV-FIN-02)
    if clean_tx_ref:
        existing_pay = db.execute(
            text("""
                SELECT * FROM finance.payments
                WHERE bank_transaction_id = :tx OR transaction_ref = :tx
                LIMIT 1
            """),
            {"tx": clean_tx_ref},
        ).mappings().first()
        if existing_pay:
            inv_existing = db.execute(
                text("SELECT * FROM finance.invoices WHERE id = :id"),
                {"id": existing_pay["invoice_id"]},
            ).mappings().first()
            return dict(existing_pay), dict(inv_existing), True

    # Bước 2: Khóa bi quan dòng hóa đơn để đảm bảo an toàn đồng thời khi gạch nợ
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

    remaining = round(float(inv["final_amount"]) - float(inv["paid_amount"]), 2)
    if (inv["status"] == "PAID" and remaining <= 0) or remaining <= 0:
        raise FinanceException(
            status_code=status.HTTP_409_CONFLICT,
            code="FIN_INVOICE_ALREADY_PAID",
            message=f"Hóa đơn {inv['invoice_code']} đã được thanh toán đủ",
        )

    effective_amount = min(round(float(amount), 2), remaining)
    if effective_amount <= 0:
        raise FinanceException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            code="FIN_INVALID_PAYMENT_AMOUNT",
            message="Số tiền thanh toán hợp lệ phải lớn hơn 0 (INV-FIN-01)",
        )

    # Bước 3: Phân bổ tiền thu vào kỳ thanh toán (ưu tiên kỳ chỉ định, sau đó tràn sang các kỳ kế tiếp)
    linked_inst_id = None
    open_insts = list(db.execute(
        text("""
            SELECT * FROM finance.installments
            WHERE invoice_id = :invoice_id AND status IN ('PENDING', 'PARTIAL', 'OVERDUE')
            ORDER BY installment_no ASC
            FOR UPDATE
        """),
        {"invoice_id": invoice_id},
    ).mappings().all())

    if installment_id is not None:
        inst_row = db.execute(
            text("""
                SELECT * FROM finance.installments
                WHERE id = :id AND invoice_id = :invoice_id
                FOR UPDATE
            """),
            {"id": installment_id, "invoice_id": invoice_id},
        ).mappings().first()
        if not inst_row:
            raise FinanceException(
                status_code=status.HTTP_404_NOT_FOUND,
                code="FIN_INSTALLMENT_NOT_FOUND",
                message=f"Không tìm thấy kỳ thanh toán #{installment_id} thuộc hóa đơn #{invoice_id}",
            )
        linked_inst_id = inst_row["id"]
        ordered_insts = [inst_row] + [r for r in open_insts if r["id"] != inst_row["id"]]
    else:
        ordered_insts = open_insts

    leftover = effective_amount
    for inst_row in ordered_insts:
        if leftover <= 0:
            break
        if linked_inst_id is None:
            linked_inst_id = inst_row["id"]
        inst_due = float(inst_row["amount_due"])
        inst_paid = float(inst_row["amount_paid"])
        inst_remaining = round(max(0.0, inst_due - inst_paid), 2)
        if inst_remaining <= 0:
            continue
        applied = min(leftover, inst_remaining)
        new_inst_paid = round(inst_paid + applied, 2)
        new_inst_status = "PAID" if new_inst_paid >= inst_due else "PARTIAL"
        db.execute(
            text("""
                UPDATE finance.installments
                SET amount_paid = :amount_paid,
                    status = :status,
                    paid_at = CASE WHEN :status = 'PAID' THEN CURRENT_TIMESTAMP ELSE paid_at END
                WHERE id = :id
            """),
            {"amount_paid": new_inst_paid, "status": new_inst_status, "id": inst_row["id"]},
        )
        leftover = round(leftover - applied, 2)

    # Bước 4: Tạo phiếu thu mới (trigger sync_payment_refs tự đồng bộ bank_transaction_id và transaction_ref)
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
            "installment_id": linked_inst_id,
            "payment_method": payment_method,
            "amount": effective_amount,
            "bank_transaction_id": clean_tx_ref,
            "qr_reference_code": qr_reference_code,
            "webhook_payload": json.dumps(webhook_payload) if webhook_payload else None,
            "received_by_staff_id": received_by_staff_id,
        },
    ).mappings().first()

    # Bước 5: Cập nhật tổng đã thu và trạng thái hóa đơn (INV-FIN-03: PAID chỉ khi paid >= final_amount)
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

    if commit:
        db.commit()
        if updated_inv["status"] == "PAID":
            _notify_admission_payment_success(dict(updated_inv), dict(pay_row))

    return dict(pay_row), dict(updated_inv), False


@router.get("/payments")
def get_payments(
    invoice_id: Optional[int] = None,
    db: Session = Depends(get_db),
    user: dict = Depends(require_roles(["ADMIN", "STAFF", "STUDENT"])),
):
    """
    Lấy danh sách phiếu thu thanh toán (ADMIN, STAFF, STUDENT).
    Học viên chỉ được xem phiếu thu của hóa đơn thuộc về mình (INV-SEC-01).
    """
    conditions = []
    params = {}

    if user["role"] == "STUDENT":
        conditions.append("i.student_id = :student_id")
        params["student_id"] = user["user_id"]

    if invoice_id is not None:
        conditions.append("p.invoice_id = :invoice_id")
        params["invoice_id"] = invoice_id

    where_sql = ("WHERE " + " AND ".join(conditions)) if conditions else ""
    rows = db.execute(
        text(f"""
            SELECT p.*
            FROM finance.payments p
            JOIN finance.invoices i ON p.invoice_id = i.id
            {where_sql}
            ORDER BY p.paid_at DESC, p.id DESC
        """),
        params,
    ).mappings().all()
    return {
        "success": True,
        "data": [dict(r) for r in rows],
    }


@router.post("/payments", status_code=status.HTTP_201_CREATED)
def make_payment(
    payment: PaymentCreate,
    db: Session = Depends(get_db),
    user: dict = Depends(require_roles(["ADMIN", "STAFF", "INTERNAL"])),
):
    """Ghi nhận giao dịch thanh toán cho hóa đơn (ADMIN, STAFF, INTERNAL)."""
    tx_ref = payment.bank_transaction_id or payment.transaction_ref
    pay_row, inv_row, is_dup = _record_payment_transaction(
        db=db,
        invoice_id=payment.invoice_id,
        amount=payment.amount,
        payment_method=payment.payment_method,
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
    user: dict = Depends(require_roles(["ADMIN", "STAFF", "INTERNAL"])),
):
    """Ghi nhận phiếu thu tiền mặt tại quầy (CASH), hỗ trợ tìm theo invoice_id / invoice_code / enrollment_id."""
    target_invoice_id = payload.invoice_id
    if not target_invoice_id and payload.invoice_code:
        inv_lookup = db.execute(
            text("SELECT id FROM finance.invoices WHERE invoice_code = :code"),
            {"code": payload.invoice_code.strip()},
        ).mappings().first()
        if inv_lookup:
            target_invoice_id = inv_lookup["id"]
    if not target_invoice_id and payload.enrollment_id is not None:
        inv_lookup = db.execute(
            text("SELECT id FROM finance.invoices WHERE enrollment_id = :enrollment_id ORDER BY id DESC LIMIT 1"),
            {"enrollment_id": payload.enrollment_id},
        ).mappings().first()
        if inv_lookup:
            target_invoice_id = inv_lookup["id"]

    if not target_invoice_id:
        raise FinanceException(
            status_code=status.HTTP_404_NOT_FOUND,
            code="FIN_INVOICE_NOT_FOUND",
            message="Không tìm thấy hóa đơn từ invoice_id / invoice_code / enrollment_id",
        )

    staff_id = payload.received_by_staff_id or user.get("user_id") or 2
    tx_ref = payload.receipt_number or payload.transaction_ref
    pay_row, inv_row, is_dup = _record_payment_transaction(
        db=db,
        invoice_id=target_invoice_id,
        amount=payload.amount,
        payment_method="CASH",
        installment_id=payload.installment_id,
        bank_transaction_id=tx_ref,
        received_by_staff_id=staff_id,
    )
    return {
        "success": True,
        "message": "Chứng từ thu tiền mặt đã được đối soát trước đó (Idempotent)" if is_dup else "Ghi nhận phiếu thu tiền mặt tại quầy thành công",
        "data": {
            **pay_row,
            "invoice_id": inv_row["id"],
            "invoice_code": inv_row["invoice_code"],
            "enrollment_id": inv_row.get("enrollment_id"),
            "receipt_number": tx_ref or pay_row.get("payment_code"),
            "invoice_status": inv_row["status"],
            "invoice_paid_amount": float(inv_row["paid_amount"]),
            "invoice_remaining_amount": float(inv_row["remaining_amount"]),
            "is_idempotent": is_dup,
        },
    }


@router.get("/invoices/{invoice_id}/vietqr")
def generate_vietqr(
    invoice_id: int,
    installment_id: Optional[int] = None,
    db: Session = Depends(get_db),
    user: dict = Depends(require_roles(["ADMIN", "STAFF", "STUDENT"])),
):
    """
    Sinh mã QR thanh toán VietQR động cho hóa đơn hoặc kỳ đóng (ADMIN, STAFF, STUDENT).
    Học viên chỉ được lấy mã QR cho hóa đơn của chính mình.
    """
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

    if user["role"] == "STUDENT" and inv["student_id"] != user["user_id"]:
        raise FinanceException(
            status_code=status.HTTP_403_FORBIDDEN,
            code="AUTH_FORBIDDEN",
            message="Bạn chỉ được tạo mã thanh toán VietQR cho hóa đơn của chính mình",
        )

    if inv["status"] in ("CANCELLED", "REFUNDED"):
        raise FinanceException(
            status_code=status.HTTP_409_CONFLICT,
            code="FIN_INVOICE_IMMUTABLE",
            message=f"Không thể tạo mã VietQR cho hóa đơn ở trạng thái {inv['status']}",
        )

    if inv["status"] == "PAID" or float(inv["remaining_amount"]) <= 0:
        raise FinanceException(
            status_code=status.HTTP_409_CONFLICT,
            code="FIN_INVOICE_ALREADY_PAID",
            message=f"Hóa đơn {inv['invoice_code']} đã được thanh toán đủ",
        )

    inst = None
    if installment_id:
        inst = db.execute(
            text("SELECT * FROM finance.installments WHERE id = :id AND invoice_id = :invoice_id"),
            {"id": installment_id, "invoice_id": invoice_id},
        ).mappings().first()
        if not inst:
            raise FinanceException(
                status_code=status.HTTP_404_NOT_FOUND,
                code="FIN_INSTALLMENT_NOT_FOUND",
                message=f"Không tìm thấy kỳ thanh toán #{installment_id} thuộc hóa đơn #{invoice_id}",
            )
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
        amount_due = min(
            round(float(inst["amount_due"]) - float(inst["amount_paid"]), 2),
            round(float(inv["remaining_amount"]), 2),
        )
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
    """
    Webhook nhận thông báo giao dịch VietQR từ cổng ngân hàng.
    Đảm bảo idempotency theo bank_transaction_id (INV-FIN-02 / INV-FIN-05).
    """
    existing = db.execute(
        text("""
            SELECT * FROM finance.payments
            WHERE bank_transaction_id = :tx OR transaction_ref = :tx
            LIMIT 1
        """),
        {"tx": payload.bank_transaction_id},
    ).mappings().first()
    if existing:
        inv_existing = db.execute(
            text("SELECT * FROM finance.invoices WHERE id = :id"),
            {"id": existing["invoice_id"]},
        ).mappings().first()
        return {
            "success": True,
            "message": "Giao dịch VietQR đã được xử lý trước đó (Idempotent)",
            "data": {
                **dict(existing),
                "invoice_code": inv_existing["invoice_code"] if inv_existing else None,
                "invoice_status": inv_existing["status"] if inv_existing else None,
                "invoice_paid_amount": float(inv_existing["paid_amount"]) if inv_existing else 0.0,
                "invoice_remaining_amount": float(inv_existing["remaining_amount"]) if inv_existing else 0.0,
                "is_idempotent": True,
            },
        }

    target_invoice_id = payload.invoice_id
    target_installment_id = None
    ref_code = payload.qr_reference_code.strip() if payload.qr_reference_code else None

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
