# Tiến độ Chất lượng & Tích hợp Liên Phân hệ Finance — 10/11/2026

## 1. Tổng quan Hoàn thiện Phân hệ Finance (`finance-service`)
- **Backend (`services/finance/backend/`)**: Hoàn thành 100% các nhóm API trên FastAPI (Python 3.11) kết nối schema `finance` (9 bảng + 2 BI views).
- **Frontend (`services/finance/frontend/`)**: Hoàn thành 3 trang giao diện (`tuition.html`, `invoices.html`, `bi-dashboard.html`) và bộ tiện ích chống XSS `js/finance-ui.js`.
- **Hợp đồng Tích hợp với Admission Service (`origin/feature/admission`)**:
  1. **Xác thực Nội bộ (`X-Internal-Secret`)**: Hỗ trợ đồng thời `english_center_internal_secret_2026` (mặc định trong `FinanceServiceClient.java` của Admission) và `internal_secret_key_english_center_2026`.
  2. **Sinh Hóa đơn Tự động (`POST /api/finance/invoices/auto-generate`)**: Nhận `{enrollment_id, student_id, course_id, voucher_code}` từ Admission, đảm bảo Idempotency theo `enrollment_id` và trả về `{invoice_id, invoice_code, status, amount, final_amount, payment_ref_code}`.
  3. **Tra cứu & Đối soát Mã Hóa đơn (`GET /api/finance/internal/invoices/lookup`)**: Cho phép Admission ánh xạ `invoice_code` hoặc `enrollment_id` sang `invoice_id`.
  4. **Đối soát Tiền mặt Tại Quầy (`POST /api/finance/payments/cash`)**: Hỗ trợ truyền trực tiếp `invoice_id`, `invoice_code` hoặc `enrollment_id` kèm mã chứng từ `receipt_number` (Idempotent qua `transaction_ref` / `bank_transaction_id`), khóa dòng `SELECT ... FOR UPDATE`, cập nhật `PARTIAL`/`PAID` và tự động bắn Webhook `POST /api/admission/webhooks/payment-success` sang Admission.
  5. **Đối soát Hủy lớp Cascade (`POST /api/finance/internal/class-cancellations`)**: Tự động hủy hóa đơn nếu `paid_amount = 0` hoặc khởi tạo yêu cầu hoàn tiền 100% (`PENDING_ACCOUNTANT`, idempotent) nếu học viên đã đóng tiền.
  6. **Cung cấp Chỉ số Doanh thu cho Admission BI (`GET /api/finance/bi/admission-metrics`)**: Trả về `pipeline_revenue` (dư nợ chờ thu `BI-REV-01`) và `realized_revenue` (doanh thu thực thu thuần `BI-REV-02`) kèm phân bổ theo `course_id`.

## 2. Kết quả Kiểm thử Tự động (Quality Gate)
- **System & Domain Invariant Tests (`tests/invariants`)**: `15/15 Tests OK`.
- **Finance Backend Contract & Schema Validation Tests (`services/finance/backend/tests/test_finance_api.py`)**: `5/5 Tests OK`.
- **Finance Frontend Node Regression Tests (`services/finance/frontend/tests/regression.cjs`)**: `3/3 Tests OK` (Kiểm tra escape HTML chống XSS trên `tuition.html`, `invoices.html`, `bi-dashboard.html`).

## 3. Lệnh Kiểm tra Nhanh
```powershell
python -m unittest discover -s tests/invariants -p "test_*.py" -v
python -m unittest discover -s services/finance/backend/tests -p "test_*.py" -v
node --test services/finance/frontend/tests/regression.cjs
```
