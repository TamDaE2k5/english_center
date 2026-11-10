import os
from datetime import date, timedelta
from typing import Dict, List, Optional, Tuple
import requests
from fastapi import APIRouter, Depends, status
from sqlalchemy import text
from sqlalchemy.orm import Session

from auth import FinanceException, INTERNAL_SECRET_KEY, get_current_user, require_roles
from database import get_db
from routers.vouchers import calculate_voucher_discount
from schemas import InvoiceAutoGenerateRequest, InvoiceCreate

router = APIRouter(prefix="/api/finance", tags=["Invoices"])

ADMISSION_SERVICE_URL = os.getenv("ADMISSION_SERVICE_URL", "http://admission-service:8081")


def _fetch_course_tuition(db: Session, course_id: int) -> Tuple[float, str]:
    tf_row = db.execute(
        text("SELECT amount, course_name FROM finance.tuition_fees WHERE course_id = :course_id AND is_active = TRUE"),
        {"course_id": course_id},
    ).mappings().first()
    if tf_row and float(tf_row["amount"]) > 0:
        return float(tf_row["amount"]), tf_row["course_name"] or f"Khóa học #{course_id}"

    try:
        res = requests.get(
            f"{ADMISSION_SERVICE_URL}/api/admission/internal/courses/{course_id}",
            headers={"X-Internal-Secret": INTERNAL_SECRET_KEY},
            timeout=3,
        )
        if res.status_code == 200:
            body = res.json()
            course_data = body.get("data", body)
            if isinstance(course_data, dict) and course_data.get("tuition_amount") is not None:
                return float(course_data["tuition_amount"]), course_data.get("name", f"Khóa học #{course_id}")
    except Exception:
        pass

    return 6500000.0, f"Khóa học #{course_id}"


def _apply_voucher_with_lock(db: Session, voucher_code: Optional[str], amount: float) -> Tuple[Optional[int], float]:
    if not voucher_code:
        return None, 0.0
    code_upper = voucher_code.strip().upper()
    v_row = db.execute(
        text("SELECT * FROM finance.vouchers WHERE code = :code FOR UPDATE"),
        {"code": code_upper},
    ).mappings().first()
    if not v_row:
        raise FinanceException(
            status_code=status.HTTP_404_NOT_FOUND,
            code="FIN_VOUCHER_NOT_FOUND",
            message=f"Không tìm thấy mã voucher {code_upper}",
        )
    discount_applied = calculate_voucher_discount(dict(v_row), amount)
    db.execute(
        text("UPDATE finance.vouchers SET current_uses = current_uses + 1 WHERE id = :id"),
        {"id": v_row["id"]},
    )
    return int(v_row["id"]), discount_applied


def _create_items_and_installments(
    db: Session,
    invoice_id: int,
    invoice_code: str,
    course_id: int,
    course_name: str,
    amount: float,
    final_amount: float,
    due_date: date,
    items: Optional[list] = None,
    installments_count: int = 1,
) -> List[Dict]:

    if items:
        for item in items:
            line_total = round(float(item.quantity) * float(item.unit_price), 2)
            db.execute(
                text("""
                    INSERT INTO finance.invoice_items
                        (invoice_id, course_id, item_type, description, quantity, unit_price, line_total)
                    VALUES
                        (:invoice_id, :course_id, :item_type, :description, :quantity, :unit_price, :line_total)
                """),
                {
                    "invoice_id": invoice_id,
                    "course_id": item.course_id or course_id,
                    "item_type": item.item_type,
                    "description": item.description,
                    "quantity": item.quantity,
                    "unit_price": item.unit_price,
                    "line_total": line_total,
                },
            )
    else:
        db.execute(
            text("""
                INSERT INTO finance.invoice_items
                    (invoice_id, course_id, item_type, description, quantity, unit_price, line_total)
                VALUES
                    (:invoice_id, :course_id, 'TUITION_PACKAGE', :description, 1, :unit_price, :line_total)
            """),
            {
                "invoice_id": invoice_id,
                "course_id": course_id,
                "description": f"Học phí trọn khóa {course_name}",
                "unit_price": amount,
                "line_total": amount,
            },
        )

    created_installments = []
    if final_amount > 0:
        count = max(1, installments_count)
        base_installment = round(final_amount / count, 2)
        allocated = 0.0
        for idx in range(1, count + 1):
            if idx < count:
                inst_amount = base_installment
                allocated += inst_amount
            else:
                inst_amount = round(final_amount - allocated, 2)

            ref_code = f"{invoice_code}-K{idx}"
            inst_due = due_date + timedelta(days=(idx - 1) * 30)
            inst_row = db.execute(
                text("""
                    INSERT INTO finance.installments
                        (invoice_id, installment_no, payment_ref_code, amount_due, amount_paid, due_date, status)
                    VALUES
                        (:invoice_id, :installment_no, :payment_ref_code, :amount_due, 0, :due_date, 'PENDING')
                    RETURNING *
                """),
                {
                    "invoice_id": invoice_id,
                    "installment_no": idx,
                    "payment_ref_code": ref_code,
                    "amount_due": inst_amount,
                    "due_date": inst_due,
                },
            ).mappings().first()
            created_installments.append(dict(inst_row))

    return created_installments


@router.post("/invoices/auto-generate", status_code=status.HTTP_201_CREATED)
def auto_generate_invoice(
    payload: InvoiceAutoGenerateRequest,
    db: Session = Depends(get_db),
    user: dict = Depends(require_roles(["ADMIN", "STAFF", "INTERNAL"])),
):
    if payload.enrollment_id is not None:
        existing = db.execute(
            text("SELECT * FROM finance.invoices WHERE enrollment_id = :enrollment_id ORDER BY id DESC LIMIT 1"),
            {"enrollment_id": payload.enrollment_id},
        ).mappings().first()
        if existing:
            inst_first = db.execute(
                text("SELECT payment_ref_code FROM finance.installments WHERE invoice_id = :id ORDER BY installment_no ASC LIMIT 1"),
                {"id": existing["id"]},
            ).mappings().first()
            return {
                "success": True,
                "message": "Hóa đơn cho đơn đăng ký này đã tồn tại (Idempotent)",
                "data": {
                    **dict(existing),
                    "invoice_id": existing["id"],
                    "payment_ref_code": inst_first["payment_ref_code"] if inst_first else existing["invoice_code"],
                },
            }

    course_tuition, course_name = _fetch_course_tuition(db, payload.course_id)
    amount = payload.amount if payload.amount is not None else course_tuition
    voucher_id, discount_applied = _apply_voucher_with_lock(db, payload.voucher_code, amount)
    final_amount = round(max(0.0, amount - discount_applied), 2)
    due_date = payload.due_date or (date.today() + timedelta(days=2))

    inv_row = db.execute(
        text("""
            INSERT INTO finance.invoices
                (student_id, course_id, enrollment_id, invoice_type, amount, voucher_id,
                 discount_applied, final_amount, paid_amount, status, due_date, notes)
            VALUES
                (:student_id, :course_id, :enrollment_id, :invoice_type, :amount, :voucher_id,
                 :discount_applied, :final_amount, 0, 'PENDING', :due_date, :notes)
            RETURNING *
        """),
        {
            "student_id": payload.student_id,
            "course_id": payload.course_id,
            "enrollment_id": payload.enrollment_id,
            "invoice_type": payload.invoice_type,
            "amount": amount,
            "voucher_id": voucher_id,
            "discount_applied": discount_applied,
            "final_amount": final_amount,
            "due_date": due_date,
            "notes": payload.notes,
        },
    ).mappings().first()

    installments = _create_items_and_installments(
        db=db,
        invoice_id=inv_row["id"],
        invoice_code=inv_row["invoice_code"],
        course_id=payload.course_id,
        course_name=course_name,
        amount=amount,
        final_amount=final_amount,
        due_date=due_date,
        items=payload.items,
        installments_count=payload.installments_count,
    )
    db.commit()

    payment_ref_code = installments[0]["payment_ref_code"] if installments else inv_row["invoice_code"]
    return {
        "success": True,
        "message": "Tự động sinh hóa đơn học phí thành công",
        "data": {
            **dict(inv_row),
            "invoice_id": inv_row["id"],
            "payment_ref_code": payment_ref_code,
            "installments": installments,
        },
    }


@router.post("/invoices", status_code=status.HTTP_201_CREATED)
def create_invoice(
    inv: InvoiceCreate,
    db: Session = Depends(get_db),
    user: dict = Depends(require_roles(["ADMIN", "STAFF", "INTERNAL"])),
):
    voucher_id, discount_applied = _apply_voucher_with_lock(db, inv.voucher_code, inv.amount)
    final_amount = round(max(0.0, inv.amount - discount_applied), 2)
    due_date = inv.due_date or (date.today() + timedelta(days=7))

    if inv.invoice_code:
        dup = db.execute(
            text("SELECT id FROM finance.invoices WHERE invoice_code = :code"),
            {"code": inv.invoice_code},
        ).mappings().first()
        if dup:
            raise FinanceException(
                status_code=status.HTTP_409_CONFLICT,
                code="FIN_INVOICE_CODE_EXISTS",
                message=f"Mã hóa đơn {inv.invoice_code} đã tồn tại",
            )
        inv_row = db.execute(
            text("""
                INSERT INTO finance.invoices
                    (invoice_code, student_id, course_id, enrollment_id, invoice_type,
                     amount, voucher_id, discount_applied, final_amount, status, due_date, notes)
                VALUES
                    (:invoice_code, :student_id, :course_id, :enrollment_id, :invoice_type,
                     :amount, :voucher_id, :discount_applied, :final_amount, 'UNPAID', :due_date, :notes)
                RETURNING *
            """),
            {
                "invoice_code": inv.invoice_code,
                "student_id": inv.student_id,
                "course_id": inv.course_id,
                "enrollment_id": inv.enrollment_id,
                "invoice_type": inv.invoice_type,
                "amount": inv.amount,
                "voucher_id": voucher_id,
                "discount_applied": discount_applied,
                "final_amount": final_amount,
                "due_date": due_date,
                "notes": inv.notes,
            },
        ).mappings().first()
    else:
        inv_row = db.execute(
            text("""
                INSERT INTO finance.invoices
                    (student_id, course_id, enrollment_id, invoice_type,
                     amount, voucher_id, discount_applied, final_amount, status, due_date, notes)
                VALUES
                    (:student_id, :course_id, :enrollment_id, :invoice_type,
                     :amount, :voucher_id, :discount_applied, :final_amount, 'UNPAID', :due_date, :notes)
                RETURNING *
            """),
            {
                "student_id": inv.student_id,
                "course_id": inv.course_id,
                "enrollment_id": inv.enrollment_id,
                "invoice_type": inv.invoice_type,
                "amount": inv.amount,
                "voucher_id": voucher_id,
                "discount_applied": discount_applied,
                "final_amount": final_amount,
                "due_date": due_date,
                "notes": inv.notes,
            },
        ).mappings().first()

    _create_items_and_installments(
        db=db,
        invoice_id=inv_row["id"],
        invoice_code=inv_row["invoice_code"],
        course_id=inv.course_id,
        course_name=f"Khóa học #{inv.course_id}",
        amount=inv.amount,
        final_amount=final_amount,
        due_date=due_date,
        items=None,
        installments_count=1,
    )
    db.commit()
    return {
        "success": True,
        "message": "Tạo hóa đơn thành công",
        "data": dict(inv_row),
    }


@router.get("/invoices")
def get_invoices(
    student_id: Optional[int] = None,
    enrollment_id: Optional[int] = None,
    invoice_code: Optional[str] = None,
    status_filter: Optional[str] = None,
    status: Optional[str] = None,
    db: Session = Depends(get_db),
    user: dict = Depends(get_current_user),
):
    effective_status = status or status_filter
    conditions = []
    params = {}

    if user["role"] == "STUDENT":
        conditions.append("student_id = :student_id")
        params["student_id"] = user["user_id"]
    elif student_id is not None:
        conditions.append("student_id = :student_id")
        params["student_id"] = student_id

    if enrollment_id is not None:
        conditions.append("enrollment_id = :enrollment_id")
        params["enrollment_id"] = enrollment_id

    if invoice_code:
        conditions.append("invoice_code = :invoice_code")
        params["invoice_code"] = invoice_code.strip()

    if effective_status:
        conditions.append("status = :status")
        params["status"] = effective_status.upper()

    where_sql = ("WHERE " + " AND ".join(conditions)) if conditions else ""
    rows = db.execute(
        text(f"SELECT * FROM finance.invoices {where_sql} ORDER BY created_at DESC, id DESC"),
        params,
    ).mappings().all()
    return {
        "success": True,
        "data": [dict(r) for r in rows],
    }


@router.get("/internal/invoices/lookup")
def lookup_internal_invoice(
    invoice_code: Optional[str] = None,
    enrollment_id: Optional[int] = None,
    invoice_id: Optional[int] = None,
    db: Session = Depends(get_db),
    user: dict = Depends(require_roles(["ADMIN", "STAFF", "INTERNAL"])),
):
    inv = None
    if invoice_id is not None:
        inv = db.execute(
            text("SELECT * FROM finance.invoices WHERE id = :id"),
            {"id": invoice_id},
        ).mappings().first()
    elif invoice_code:
        inv = db.execute(
            text("SELECT * FROM finance.invoices WHERE invoice_code = :code"),
            {"code": invoice_code.strip()},
        ).mappings().first()
    elif enrollment_id is not None:
        inv = db.execute(
            text("SELECT * FROM finance.invoices WHERE enrollment_id = :enrollment_id ORDER BY id DESC LIMIT 1"),
            {"enrollment_id": enrollment_id},
        ).mappings().first()

    if not inv:
        raise FinanceException(
            status_code=404,
            code="FIN_INVOICE_NOT_FOUND",
            message="Không tìm thấy hóa đơn tương ứng với invoice_code/enrollment_id",
        )

    return {
        "success": True,
        "data": {
            **dict(inv),
            "invoice_id": inv["id"],
        },
    }



@router.get("/invoices/{invoice_id}")
def get_invoice_detail(
    invoice_id: int,
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

    if user["role"] == "STUDENT" and inv["student_id"] != user["user_id"]:
        raise FinanceException(
            status_code=status.HTTP_403_FORBIDDEN,
            code="AUTH_FORBIDDEN",
            message="Bạn chỉ được xem hóa đơn của chính mình",
        )

    items = db.execute(
        text("SELECT * FROM finance.invoice_items WHERE invoice_id = :id ORDER BY id ASC"),
        {"id": invoice_id},
    ).mappings().all()
    installments = db.execute(
        text("SELECT * FROM finance.installments WHERE invoice_id = :id ORDER BY installment_no ASC"),
        {"id": invoice_id},
    ).mappings().all()
    payments = db.execute(
        text("SELECT * FROM finance.payments WHERE invoice_id = :id ORDER BY paid_at DESC, id DESC"),
        {"id": invoice_id},
    ).mappings().all()

    return {
        "success": True,
        "data": {
            **dict(inv),
            "items": [dict(x) for x in items],
            "installments": [dict(x) for x in installments],
            "payments": [dict(x) for x in payments],
        },
    }
