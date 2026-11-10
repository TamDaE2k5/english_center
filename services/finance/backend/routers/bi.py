from fastapi import APIRouter, Depends
from sqlalchemy import text
from sqlalchemy.orm import Session

from auth import require_roles
from database import get_db

router = APIRouter(prefix="/api/finance/bi", tags=["Finance BI"])


@router.get("/cash-revenue")
def get_bi_cash_revenue(
    db: Session = Depends(get_db),
    user: dict = Depends(require_roles(["ADMIN", "STAFF"])),
):
    rows = db.execute(
        text("SELECT * FROM finance.vw_bi_cash_revenue_monthly ORDER BY report_month DESC")
    ).mappings().all()
    return {
        "success": True,
        "data": [dict(r) for r in rows],
    }


@router.get("/voucher-campaigns")
def get_bi_voucher_campaigns(
    db: Session = Depends(get_db),
    user: dict = Depends(require_roles(["ADMIN", "STAFF"])),
):
    rows = db.execute(
        text("SELECT * FROM finance.vw_bi_voucher_campaigns ORDER BY voucher_id ASC")
    ).mappings().all()
    return {
        "success": True,
        "data": [dict(r) for r in rows],
    }


@router.get("/refund-summary")
def get_bi_refund_summary(
    db: Session = Depends(get_db),
    user: dict = Depends(require_roles(["ADMIN", "STAFF"])),
):
    wallet_stats = db.execute(
        text("""
            SELECT
                COUNT(id) AS total_wallets,
                COALESCE(SUM(balance), 0) AS total_reserved_balance
            FROM finance.student_wallets
            WHERE status = 'ACTIVE'
        """)
    ).mappings().first()

    refund_stats = db.execute(
        text("""
            SELECT
                COUNT(id) AS total_requests,
                COUNT(id) FILTER (WHERE status = 'PENDING_ACCOUNTANT') AS pending_accountant,
                COUNT(id) FILTER (WHERE status = 'PENDING_DIRECTOR') AS pending_director,
                COUNT(id) FILTER (WHERE status = 'APPROVED') AS approved_count,
                COUNT(id) FILTER (WHERE status = 'REJECTED') AS rejected_count,
                COUNT(id) FILTER (WHERE status = 'COMPLETED') AS completed_count,
                COALESCE(SUM(requested_refund_amount), 0) AS total_requested_amount,
                COALESCE(SUM(approved_refund_amount) FILTER (WHERE status IN ('APPROVED', 'COMPLETED')), 0) AS total_approved_amount,
                COALESCE(SUM(approved_refund_amount) FILTER (WHERE status = 'COMPLETED'), 0) AS total_disbursed_amount
            FROM finance.refund_requests
        """)
    ).mappings().first()

    return {
        "success": True,
        "data": {
            "wallets": dict(wallet_stats) if wallet_stats else {},
            "refunds": dict(refund_stats) if refund_stats else {},
        },
    }


@router.get("/admission-metrics")
def get_bi_admission_metrics(
    course_id: int = None,
    db: Session = Depends(get_db),
    user: dict = Depends(require_roles(["ADMIN", "STAFF", "INTERNAL"])),
):
    course_filter = "AND course_id = :course_id" if course_id is not None else ""
    params = {"course_id": course_id} if course_id is not None else {}

    pipeline_row = db.execute(
        text(f"""
            SELECT
                COUNT(id) AS pending_invoices_count,
                COALESCE(SUM(remaining_amount), 0) AS pipeline_outstanding_revenue
            FROM finance.invoices
            WHERE status IN ('PENDING', 'UNPAID', 'PARTIAL') {course_filter}
        """),
        params,
    ).mappings().first()

    realized_row = db.execute(
        text("""
            SELECT
                COALESCE(SUM(gross_cash_in), 0) AS gross_cash_in,
                COALESCE(SUM(total_refunded), 0) AS total_refunded,
                COALESCE(SUM(net_cash_revenue), 0) AS net_realized_revenue
            FROM finance.vw_bi_cash_revenue_monthly
        """)
    ).mappings().first()

    by_course_rows = db.execute(
        text("""
            SELECT
                course_id,
                COUNT(id) AS total_invoices,
                COALESCE(SUM(remaining_amount) FILTER (WHERE status IN ('PENDING', 'UNPAID', 'PARTIAL')), 0) AS pipeline_revenue,
                COALESCE(SUM(paid_amount) FILTER (WHERE status IN ('PAID', 'PARTIAL')), 0) AS realized_revenue
            FROM finance.invoices
            GROUP BY course_id
            ORDER BY course_id ASC
        """)
    ).mappings().all()

    return {
        "success": True,
        "data": {
            "pipeline_revenue": float(pipeline_row["pipeline_outstanding_revenue"]) if pipeline_row else 0.0,
            "pending_invoices_count": int(pipeline_row["pending_invoices_count"]) if pipeline_row else 0,
            "realized_revenue": float(realized_row["net_realized_revenue"]) if realized_row else 0.0,
            "gross_cash_in": float(realized_row["gross_cash_in"]) if realized_row else 0.0,
            "total_refunded": float(realized_row["total_refunded"]) if realized_row else 0.0,
            "courses_breakdown": [dict(r) for r in by_course_rows],
        },
    }

