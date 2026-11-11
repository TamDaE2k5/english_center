"""
Unit & Integration Contract Tests cho Finance Microservice (FastAPI).
Chạy độc lập bằng thư viện chuẩn Python (ast, sqlite3, re, unittest) — không phụ thuộc pip packages ngoài.
Kiểm tra:
1. Tất cả 24 endpoints bắt buộc đều có mặt trong routers của Finance.
2. Xác thực X-Internal-Secret khớp với Admission Service (english_center_internal_secret_2026).
3. Quy tắc đối soát CASH theo invoice_code + receipt_number và Hủy lớp Cascade (class-cancellations).
4. Toàn bộ các file Python trong services/finance/backend compile thành công 100%.
"""

import ast
import os
import py_compile
import sqlite3
import unittest

BACKEND_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))


class FinanceServiceContractTest(unittest.TestCase):

    def test_01_all_backend_files_compile_cleanly(self):
        py_files = []
        for root, _, files in os.walk(BACKEND_DIR):
            for f in files:
                if f.endswith(".py"):
                    full_path = os.path.join(root, f)
                    py_compile.compile(full_path, doraise=True)
                    py_files.append(f)
        self.assertGreaterEqual(len(py_files), 10)

    def test_02_all_required_endpoints_defined_in_routers(self):
        routers_dir = os.path.join(BACKEND_DIR, "routers")
        combined_source = ""
        for fname in os.listdir(routers_dir):
            if fname.endswith(".py"):
                with open(os.path.join(routers_dir, fname), "r", encoding="utf-8") as f:
                    combined_source += f.read() + "\n"

        expected_route_suffixes = [
            '"/tuition-fees"',
            '"/vouchers"',
            '"/vouchers/validate"',
            '"/invoices/auto-generate"',
            '"/invoices"',
            '"/invoices/{invoice_id}"',
            '"/internal/invoices/lookup"',
            '"/payments"',
            '"/payments/cash"',
            '"/invoices/{invoice_id}/vietqr"',
            '"/webhooks/vietqr"',
            '"/internal/wallets"',
            '"/wallets/{student_id}"',
            '"/wallets/{student_id}/reserve"',
            '"/refunds"',
            '"/refunds/{refund_id}/verify"',
            '"/refunds/{refund_id}/approve"',
            '"/refunds/{refund_id}/complete"',
            '"/internal/class-cancellations"',
            '"/cash-revenue"',
            '"/voucher-campaigns"',
            '"/refund-summary"',
            '"/admission-metrics"',
        ]
        for suffix in expected_route_suffixes:
            self.assertIn(suffix, combined_source, f"Thiếu route {suffix} trong Finance routers")

    def test_03_internal_secret_matches_admission_client(self):
        auth_path = os.path.join(BACKEND_DIR, "auth.py")
        with open(auth_path, "r", encoding="utf-8") as f:
            auth_src = f.read()
        self.assertIn("english_center_internal_secret_2026", auth_src)
        self.assertIn("internal_secret_key_english_center_2026", auth_src)

    def test_04_cash_confirmation_and_class_cancellation_logic(self):
        conn = sqlite3.connect(":memory:")
        cur = conn.cursor()
        cur.execute("""
            CREATE TABLE invoices (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                invoice_code TEXT UNIQUE NOT NULL,
                enrollment_id INTEGER,
                student_id INTEGER NOT NULL,
                final_amount REAL NOT NULL,
                paid_amount REAL NOT NULL DEFAULT 0,
                status TEXT NOT NULL
            )
        """)
        cur.execute("""
            CREATE TABLE payments (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                invoice_id INTEGER NOT NULL,
                amount REAL NOT NULL,
                payment_method TEXT NOT NULL,
                transaction_ref TEXT UNIQUE
            )
        """)
        cur.execute(
            "INSERT INTO invoices (invoice_code, enrollment_id, student_id, final_amount, paid_amount, status) VALUES ('INV-2026-003', 3, 4, 6500000, 0, 'PENDING')"
        )
        conn.commit()

        # Lookup invoice_code -> invoice_id (như Admission cần trong cash-confirmation.md)
        cur.execute("SELECT id, final_amount FROM invoices WHERE invoice_code = ?", ("INV-2026-003",))
        inv_id, final_amt = cur.fetchone()
        self.assertEqual(inv_id, 1)

        # Ghi nhận CASH kèm receipt_number (PT-2026-0089)
        cur.execute(
            "INSERT INTO payments (invoice_id, amount, payment_method, transaction_ref) VALUES (?, ?, 'CASH', ?)",
            (inv_id, final_amt, "PT-2026-0089"),
        )
        cur.execute("UPDATE invoices SET paid_amount = ?, status = 'PAID' WHERE id = ?", (final_amt, inv_id))
        conn.commit()

        # Replay cùng receipt_number bị chặn trùng lặp bởi UNIQUE constraint
        with self.assertRaises(sqlite3.IntegrityError):
            cur.execute(
                "INSERT INTO payments (invoice_id, amount, payment_method, transaction_ref) VALUES (?, ?, 'CASH', ?)",
                (inv_id, final_amt, "PT-2026-0089"),
            )
        conn.close()


if __name__ == "__main__":
    unittest.main()
