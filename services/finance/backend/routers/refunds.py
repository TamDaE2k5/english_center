import os
import random
from datetime import datetime
from typing import Optional, Tuple
import requests

from fastapi import APIRouter, Depends, status
from sqlalchemy import text
from sqlalchemy.orm import Session

from auth import FinanceException, INTERNAL_SECRET_KEY, get_current_user, require_roles
from database import get_db
from schemas import (
    ClassCancellationRequest,
    RefundApproveRequest,
    RefundCompleteRequest,
    RefundCreateRequest,
    RefundVerifyRequest,
)


router = APIRouter(prefix="/api/finance", tags=["Refunds"])

STUDENT_SERVICE_URL = os.getenv("STUDENT_SERVICE_URL", "http://student-service:8082")


def _fetch_attendance_snapshot(student_id: int, class_id: Optional[int]) -> Tuple[int, int]:

    if not class_id:
        return 24, 0
    try:
        res = requests.get(
            f"{STUDENT_SERVICE_URL}/api/student/internal/attendance/summary",
            params={"student_id": student_id, "class_id": class_id},
            headers={"X-Internal-Secret": INTERNAL_SECRET_KEY},
            timeout=3,
        )
        if res.status_code == 200:
            body = res.json()
            data = body.get("data", body)
            if isinstance(data, dict):
                total = int(data.get("total_sessions") or 24)
                attended = int(data.get("attended_sessions") or 0)
                return total, attended
    except Exception:
        pass
    return 24, 1


@router.get("/refunds")
def list_refund_requests(
    status_filter: Optional[str] = None,
    student_id: Optional[int] = None,
    db: Session = Depends(get_db),
    user: dict = Depends(get_current_user),
):
    conditions = []
    params = {}
    if user["role"] == "STUDENT":
        conditions.append("student_id = :student_id")
        params["student_id"] = user["user_id"]
    elif student_id is not None:
        conditions.append("student_id = :student_id")
        params["student_id"] = student_id

    if status_filter:
        conditions.append("status = :status")
        params["status"] = status_filter.upper()

    where_sql = ("WHERE " + " AND ".join(conditions)) if conditions else ""
    rows = db.execute(
        text(f"SELECT * FROM finance.refund_requests {where_sql} ORDER BY requested_at DESC, id DESC"),
        params,
    ).mappings().all()
    return {
        "success": True,
        "data": [dict(r) for r in rows],
    }


@router.post("/refunds", status_code=status.HTTP_201_CREATED)
def create_refund_request(
    payload: RefundCreateRequest,
    db: Session = Depends(get_db),
    user: dict = Depends(require_roles(["ADMIN", "STAFF", "INTERNAL"])),
):
    inv = None
    if payload.invoice_id is not None:
        inv = db.execute(
            text("SELECT * FROM finance.invoices WHERE id = :id"),
            {"id": payload.invoice_id},
        ).mappings().first()
    elif payload.invoice_code:
        inv = db.execute(
            text("SELECT * FROM finance.invoices WHERE invoice_code = :code"),
            {"code": payload.invoice_code.strip()},
        ).mappings().first()
    elif payload.enrollment_id is not None:
        inv = db.execute(
            text("SELECT * FROM finance.invoices WHERE enrollment_id = :enrollment_id ORDER BY id DESC LIMIT 1"),
            {"enrollment_id": payload.enrollment_id},
        ).mappings().first()

    if not inv:
        raise FinanceException(
            status_code=status.HTTP_404_NOT_FOUND,
            code="FIN_INVOICE_NOT_FOUND",
            message="Không tìm thấy hóa đơn từ invoice_id / invoice_code / enrollment_id",
        )

    invoice_id = int(inv["id"])
    paid_snapshot = float(inv["paid_amount"])
    if paid_snapshot <= 0:
        raise FinanceException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            code="FIN_REFUND_NO_PAID_AMOUNT",
            message="Hóa đơn chưa có khoản thực thu nào để hoàn tiền",
        )

    student_id = payload.student_id or int(inv["student_id"])
    if payload.total_sessions is not None and payload.attended_sessions is not None:
        total_sessions = payload.total_sessions
        attended_sessions = payload.attended_sessions
    else:
        total_sessions, attended_sessions = _fetch_attendance_snapshot(student_id, payload.class_id)

    max_allowed = payload.max_allowed_sessions or 3
    if attended_sessions > max_allowed:
        raise FinanceException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            code="FIN_REFUND_EXCEEDS_MAX_SESSIONS",
            message=(
                f"Học viên đã học {attended_sessions}/{total_sessions} buổi, "
                f"vượt quá giới hạn tối đa {max_allowed} buổi được phép hoàn tiền (INV-FIN-07)"
            ),
        )

    refundable_pct = payload.refundable_percent
    deduction = round(float(payload.deduction_amount), 2)
    requested_refund = max(0.0, round(paid_snapshot * (refundable_pct / 100.0) - deduction, 2))
    refund_code = f"REF-{datetime.now().year}-{random.randint(1000, 9999)}"
    staff_id = user.get("user_id") or 2

    row = db.execute(
        text("""
            INSERT INTO finance.refund_requests
                (refund_code, invoice_id, student_id, class_id,
                 total_sessions, attended_sessions, max_allowed_sessions,
                 paid_amount_snapshot, refundable_percent, deduction_amount,
                 requested_refund_amount, reason, bank_name, bank_account_no,
                 bank_account_holder, status, requested_by_staff_id)
            VALUES
                (:refund_code, :invoice_id, :student_id, :class_id,
                 :total_sessions, :attended_sessions, :max_allowed_sessions,
                 :paid_amount_snapshot, :refundable_percent, :deduction_amount,
                 :requested_refund_amount, :reason, :bank_name, :bank_account_no,
                 :bank_account_holder, 'PENDING_ACCOUNTANT', :requested_by_staff_id)
            RETURNING *
        """),
        {
            "refund_code": refund_code,
            "invoice_id": invoice_id,
            "student_id": student_id,
            "class_id": payload.class_id,
            "total_sessions": total_sessions,
            "attended_sessions": attended_sessions,
            "max_allowed_sessions": max_allowed,
            "paid_amount_snapshot": paid_snapshot,
            "refundable_percent": refundable_pct,
            "deduction_amount": deduction,
            "requested_refund_amount": requested_refund,
            "reason": payload.reason,
            "bank_name": payload.bank_name,
            "bank_account_no": payload.bank_account_no,
            "bank_account_holder": payload.bank_account_holder,
            "requested_by_staff_id": staff_id,
        },
    ).mappings().first()
    db.commit()

    return {
        "success": True,
        "message": "Khởi tạo yêu cầu hoàn tiền cấp 1 (chờ Kế toán xác minh) thành công",
        "data": dict(row),
    }


@router.post("/internal/class-cancellations")
def handle_class_cancellation_reconciliation(
    payload: ClassCancellationRequest,
    db: Session = Depends(get_db),
    user: dict = Depends(require_roles(["ADMIN", "STAFF", "INTERNAL"])),
):
    inv = None
    if payload.invoice_id is not None:
        inv = db.execute(
            text("SELECT * FROM finance.invoices WHERE id = :id FOR UPDATE"),
            {"id": payload.invoice_id},
        ).mappings().first()
    elif payload.invoice_code:
        inv = db.execute(
            text("SELECT * FROM finance.invoices WHERE invoice_code = :code FOR UPDATE"),
            {"code": payload.invoice_code.strip()},
        ).mappings().first()
    else:
        inv = db.execute(
            text("SELECT * FROM finance.invoices WHERE enrollment_id = :enrollment_id ORDER BY id DESC LIMIT 1 FOR UPDATE"),
            {"enrollment_id": payload.enrollment_id},
        ).mappings().first()

    if not inv:
        raise FinanceException(
            status_code=status.HTTP_404_NOT_FOUND,
            code="FIN_INVOICE_NOT_FOUND",
            message=f"Không tìm thấy hóa đơn cho enrollment_id={payload.enrollment_id}",
        )

    invoice_id = int(inv["id"])
    paid_amount = float(inv["paid_amount"] or 0)

    if paid_amount <= 0:
        if inv["status"] not in ("CANCELLED", "REFUNDED"):
            db.execute(
                text("UPDATE finance.invoices SET status = 'CANCELLED', notes = COALESCE(notes, '') || :note WHERE id = :id"),
                {"note": f" | Hủy lớp: {payload.reason}", "id": invoice_id},
            )
            db.execute(
                text("UPDATE finance.installments SET status = 'CANCELLED' WHERE invoice_id = :id AND status != 'PAID'"),
                {"id": invoice_id},
            )
            db.commit()
        return {
            "success": True,
            "message": "Đã hủy hóa đơn chưa phát sinh thực thu do hủy lớp",
            "data": {
                "enrollment_id": payload.enrollment_id,
                "invoice_id": invoice_id,
                "invoice_code": inv["invoice_code"],
                "reconciliation_action": "INVOICE_CANCELLED",
                "refund_request_id": None,
            },
        }

    existing_ref = db.execute(
        text("SELECT * FROM finance.refund_requests WHERE invoice_id = :invoice_id ORDER BY id DESC LIMIT 1"),
        {"invoice_id": invoice_id},
    ).mappings().first()
    if existing_ref:
        return {
            "success": True,
            "message": "Yêu cầu hoàn tiền do hủy lớp đã tồn tại (Idempotent)",
            "data": {
                "enrollment_id": payload.enrollment_id,
                "invoice_id": invoice_id,
                "invoice_code": inv["invoice_code"],
                "reconciliation_action": "REFUND_REQUEST_CREATED",
                "refund_request_id": existing_ref["id"],
                "refund_code": existing_ref["refund_code"],
                "status": existing_ref["status"],
                "is_idempotent": True,
            },
        }

    refund_code = f"REF-CANCEL-{invoice_id}-{payload.enrollment_id}"
    staff_id = payload.requested_by_staff_id or user.get("user_id") or 2
    ref_row = db.execute(
        text("""
            INSERT INTO finance.refund_requests
                (refund_code, invoice_id, student_id, class_id,
                 total_sessions, attended_sessions, max_allowed_sessions,
                 paid_amount_snapshot, refundable_percent, deduction_amount,
                 requested_refund_amount, reason, status, requested_by_staff_id)
            VALUES
                (:refund_code, :invoice_id, :student_id, :class_id,
                 24, 0, 3,
                 :paid_amount, 100, 0,
                 :paid_amount, :reason, 'PENDING_ACCOUNTANT', :staff_id)
            RETURNING *
        """),
        {
            "refund_code": refund_code,
            "invoice_id": invoice_id,
            "student_id": inv["student_id"],
            "class_id": payload.class_id,
            "paid_amount": paid_amount,
            "reason": payload.reason,
            "staff_id": staff_id,
        },
    ).mappings().first()
    db.commit()

    return {
        "success": True,
        "message": "Đã tạo yêu cầu hoàn tiền 100% do hủy lớp (Idempotent)",
        "data": {
            "enrollment_id": payload.enrollment_id,
            "invoice_id": invoice_id,
            "invoice_code": inv["invoice_code"],
            "reconciliation_action": "REFUND_REQUEST_CREATED",
            "refund_request_id": ref_row["id"],
            "refund_code": ref_row["refund_code"],
            "status": ref_row["status"],
            "is_idempotent": False,
        },
    }



@router.put("/refunds/{refund_id}/verify")
def verify_refund_request(
    refund_id: int,
    payload: RefundVerifyRequest,
    db: Session = Depends(get_db),
    user: dict = Depends(require_roles(["ADMIN", "STAFF"])),
):
    ref_row = db.execute(
        text("SELECT * FROM finance.refund_requests WHERE id = :id FOR UPDATE"),
        {"id": refund_id},
    ).mappings().first()
    if not ref_row:
        raise FinanceException(
            status_code=status.HTTP_404_NOT_FOUND,
            code="FIN_REFUND_NOT_FOUND",
            message=f"Không tìm thấy yêu cầu hoàn tiền #{refund_id}",
        )
    if ref_row["status"] != "PENDING_ACCOUNTANT":
        raise FinanceException(
            status_code=status.HTTP_409_CONFLICT,
            code="FIN_REFUND_INVALID_STATE",
            message=f"Chỉ có thể xác minh yêu cầu ở trạng thái PENDING_ACCOUNTANT (hiện tại: {ref_row['status']})",
        )

    accountant_id = payload.verified_by_accountant_id or user.get("user_id") or 2
    updated = db.execute(
        text("""
            UPDATE finance.refund_requests
            SET status = 'PENDING_DIRECTOR',
                verified_by_accountant_id = :accountant_id,
                verified_at = CURRENT_TIMESTAMP,
                accountant_note = :accountant_note
            WHERE id = :id
            RETURNING *
        """),
        {
            "accountant_id": accountant_id,
            "accountant_note": payload.accountant_note,
            "id": refund_id,
        },
    ).mappings().first()
    db.commit()

    return {
        "success": True,
        "message": "Kế toán xác minh cấp 2 thành công, đã chuyển Giám đốc phê duyệt",
        "data": dict(updated),
    }


@router.put("/refunds/{refund_id}/approve")
def approve_refund_request(
    refund_id: int,
    payload: RefundApproveRequest,
    db: Session = Depends(get_db),
    user: dict = Depends(require_roles(["ADMIN"])),
):
    ref_row = db.execute(
        text("SELECT * FROM finance.refund_requests WHERE id = :id FOR UPDATE"),
        {"id": refund_id},
    ).mappings().first()
    if not ref_row:
        raise FinanceException(
            status_code=status.HTTP_404_NOT_FOUND,
            code="FIN_REFUND_NOT_FOUND",
            message=f"Không tìm thấy yêu cầu hoàn tiền #{refund_id}",
        )
    if ref_row["status"] != "PENDING_DIRECTOR":
        raise FinanceException(
            status_code=status.HTTP_409_CONFLICT,
            code="FIN_REFUND_INVALID_STATE",
            message=f"Chỉ Giám đốc mới được duyệt yêu cầu ở trạng thái PENDING_DIRECTOR (hiện tại: {ref_row['status']})",
        )

    director_id = payload.approved_by_director_id or user.get("user_id") or 1
    new_status = "APPROVED" if payload.is_approved else "REJECTED"
    approved_amount = (
        payload.approved_refund_amount
        if payload.approved_refund_amount is not None
        else float(ref_row["requested_refund_amount"])
    ) if payload.is_approved else 0.0

    updated = db.execute(
        text("""
            UPDATE finance.refund_requests
            SET status = :status,
                approved_refund_amount = :approved_amount,
                approved_by_director_id = :director_id,
                approved_at = CURRENT_TIMESTAMP,
                director_note = :director_note
            WHERE id = :id
            RETURNING *
        """),
        {
            "status": new_status,
            "approved_amount": approved_amount,
            "director_id": director_id,
            "director_note": payload.director_note,
            "id": refund_id,
        },
    ).mappings().first()
    db.commit()

    return {
        "success": True,
        "message": "Giám đốc đã phê duyệt yêu cầu hoàn tiền" if payload.is_approved else "Giám đốc đã từ chối yêu cầu hoàn tiền",
        "data": dict(updated),
    }


@router.put("/refunds/{refund_id}/complete")
def complete_refund_request(
    refund_id: int,
    payload: RefundCompleteRequest,
    db: Session = Depends(get_db),
    user: dict = Depends(require_roles(["ADMIN", "STAFF"])),
):
    ref_row = db.execute(
        text("SELECT * FROM finance.refund_requests WHERE id = :id FOR UPDATE"),
        {"id": refund_id},
    ).mappings().first()
    if not ref_row:
        raise FinanceException(
            status_code=status.HTTP_404_NOT_FOUND,
            code="FIN_REFUND_NOT_FOUND",
            message=f"Không tìm thấy yêu cầu hoàn tiền #{refund_id}",
        )
    if ref_row["status"] != "APPROVED":
        raise FinanceException(
            status_code=status.HTTP_409_CONFLICT,
            code="FIN_REFUND_INVALID_STATE",
            message=f"Chỉ có thể giải ngân yêu cầu đã được APPROVED (hiện tại: {ref_row['status']})",
        )

    updated = db.execute(
        text("""
            UPDATE finance.refund_requests
            SET status = 'COMPLETED',
                transaction_ref = :transaction_ref,
                completed_at = CURRENT_TIMESTAMP
            WHERE id = :id
            RETURNING *
        """),
        {"transaction_ref": payload.transaction_ref, "id": refund_id},
    ).mappings().first()

    db.execute(
        text("UPDATE finance.invoices SET status = 'REFUNDED' WHERE id = :invoice_id"),
        {"invoice_id": ref_row["invoice_id"]},
    )
    db.commit()

    return {
        "success": True,
        "message": "Hoàn tất giải ngân hoàn tiền và cập nhật trạng thái hóa đơn REFUNDED",
        "data": dict(updated),
    }
