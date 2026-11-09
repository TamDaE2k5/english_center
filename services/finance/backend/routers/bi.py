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
