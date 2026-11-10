# BẢNG THEO DÕI TIẾN ĐỘ THỰC HIỆN TASKS (FINANCE MODULE CHECKLIST)

> **Hướng dẫn sử dụng**:
> - Đánh dấu `[x]` khi hoàn thành một task và kiểm tra bộ test bất biến + regression đạt `OK`.
> - Ghi rõ **Người thực hiện (Assignee)** và **Ngày hoàn thành** để báo cáo tiến độ Sprint.
> - Luôn chạy bộ kiểm thử chất lượng trước khi tick:
>   ```bash
>   python -m unittest discover -s tests/invariants -p "test_*.py" -v
>   python -m unittest discover -s services/finance/backend/tests -p "test_*.py" -v
>   node --test services/finance/frontend/tests/regression.cjs
>   ```

---

## 1. Bảng Tổng Hợp Trạng Thái Phân Kỳ

| Phân kỳ | Số lượng Task | Hoàn thành | Tỷ lệ (%) | Trạng thái |
|---|:---:|:---:|:---:|:---:|
| **Phase 0: Nền móng Kỹ thuật & Hạ tầng (INF)** | 3 | 3 | 100% | Hoàn thành |
| **Phase 1: Biểu phí & Chiến dịch Voucher (VOU)** | 2 | 2 | 100% | Hoàn thành |
| **Phase 2: Hóa đơn & Lịch Đóng phí Nhiều đợt (INV/INS)** | 3 | 3 | 100% | Hoàn thành |
| **Phase 3: Thu tiền & Thanh toán Đa kênh (PAY)** | 3 | 3 | 100% | Hoàn thành |
| **Phase 4: Ví Bảo lưu Học viên (WAL)** | 2 | 2 | 100% | Hoàn thành |
| **Phase 5: Quy trình Hoàn tiền 3 cấp (REF)** | 4 | 4 | 100% | Hoàn thành |
| **Phase 6: Báo cáo Thông minh BI & Dashboard (BI)** | 3 | 3 | 100% | Hoàn thành |
| **Phase 7: Giao diện Người dùng Frontend (UI)** | 4 | 4 | 100% | Hoàn thành |
| **TỔNG CỘNG** | **24 Tasks** | **24** | **100%** | **Hoàn thành 100%** |

---

## 2. Chi Tiết Checklist Từng Nhiệm Vụ

### Phase 0: Nền móng Kỹ thuật & Hạ tầng (INF)
- [x] **`TASK-FIN-00`**: Thiết lập Nền tảng FastAPI, Pydantic v2 snake_case, JWT Middleware & Exception Handler
  - *File chi tiết*: [`phase-0-foundation/TASK-FIN-00-PROJECT-FOUNDATION.md`](phase-0-foundation/TASK-FIN-00-PROJECT-FOUNDATION.md)
  - *Invariant*: `INV-SYS-01`
  - *Assignee*: `Chi & AI Assistant` | *Ngày xong*: `2026-11-09`
- [x] **`TASK-FIN-01`**: Khởi tạo Schema PostgreSQL 9 Bảng, 2 BI Views, Ràng buộc DDL & Seed Data
  - *File chi tiết*: [`phase-0-foundation/TASK-FIN-01-SCHEMA-MIGRATION.md`](phase-0-foundation/TASK-FIN-01-SCHEMA-MIGRATION.md)
  - *Invariant*: `INV-SYS-02`, `INV-FIN-01`
  - *Assignee*: `Chi & AI Assistant` | *Ngày xong*: `2026-11-09`
- [x] **`TASK-FIN-02`**: Cấu hình HTTP Clients Tích hợp Liên Service (Admission & Student Service)
  - *File chi tiết*: [`phase-0-foundation/TASK-FIN-02-INTERSERVICE-CLIENTS.md`](phase-0-foundation/TASK-FIN-02-INTERSERVICE-CLIENTS.md)
  - *Invariant*: `INV-SYS-02`
  - *Assignee*: `Chi & AI Assistant` | *Ngày xong*: `2026-11-10`

---

### Phase 1: Biểu phí & Chiến dịch Voucher (VOU)
- [x] **`TASK-FIN-10`**: Quản lý Chiến dịch Voucher (Tạo mới, Cập nhật, Giới hạn lượt dùng & Hạn dùng)
  - *File chi tiết*: [`phase-1-vouchers/TASK-FIN-10-VOU-CAMPAIGN-CRUD.md`](phase-1-vouchers/TASK-FIN-10-VOU-CAMPAIGN-CRUD.md)
  - *Assignee*: `Chi & AI Assistant` | *Ngày xong*: `2026-11-09`
- [x] **`TASK-FIN-11`**: Động cơ Kiểm tra Voucher & Áp dụng Khóa dòng `SELECT FOR UPDATE` Chống Race Condition
  - *File chi tiết*: [`phase-1-vouchers/TASK-FIN-11-VOU-VALIDATION-LOCK.md`](phase-1-vouchers/TASK-FIN-11-VOU-VALIDATION-LOCK.md)
  - *Invariant*: `INV-FIN-04`
  - *Assignee*: `Chi & AI Assistant` | *Ngày xong*: `2026-11-09`

---

### Phase 2: Hóa đơn & Lịch Đóng phí Nhiều đợt (INV/INS)
- [x] **`TASK-FIN-20`**: Tự động Sinh Hóa đơn từ Admission (Gói học phí, Phụ phí học lại, Invoice Items)
  - *File chi tiết*: [`phase-2-invoices/TASK-FIN-20-INV-AUTO-GENERATE.md`](phase-2-invoices/TASK-FIN-20-INV-AUTO-GENERATE.md)
  - *Invariant*: `INV-FIN-01`
  - *Assignee*: `Chi & AI Assistant` | *Ngày xong*: `2026-11-09`
- [x] **`TASK-FIN-21`**: Cơ chế Chia đợt Đóng phí (Installments) & Sinh Mã Tham chiếu Duy nhất `payment_ref_code`
  - *File chi tiết*: [`phase-2-invoices/TASK-FIN-21-INS-INSTALLMENT-PLAN.md`](phase-2-invoices/TASK-FIN-21-INS-INSTALLMENT-PLAN.md)
  - *Invariant*: `INV-FIN-02`
  - *Assignee*: `Chi & AI Assistant` | *Ngày xong*: `2026-11-09`
- [x] **`TASK-FIN-22`**: API Tra cứu Danh sách & Chi tiết Hóa đơn, Khoản mục, Lịch sử Đóng tiền & Lookup Nội bộ
  - *File chi tiết*: [`phase-2-invoices/TASK-FIN-22-INV-QUERY-DETAIL.md`](phase-2-invoices/TASK-FIN-22-INV-QUERY-DETAIL.md)
  - *Assignee*: `Chi & AI Assistant` | *Ngày xong*: `2026-11-10`

---

### Phase 3: Thu tiền & Thanh toán Đa kênh (PAY)
- [x] **`TASK-FIN-30`**: Ghi nhận Phiếu thu Tiền mặt Tại quầy (Hỗ trợ `invoice_code`/`receipt_number`) & Tự động Cập nhật `PARTIAL`/`PAID`
  - *File chi tiết*: [`phase-3-payments/TASK-FIN-30-PAY-CASH-COUNTER.md`](phase-3-payments/TASK-FIN-30-PAY-CASH-COUNTER.md)
  - *Invariant*: `INV-FIN-03`
  - *Assignee*: `Chi & AI Assistant` | *Ngày xong*: `2026-11-10`
- [x] **`TASK-FIN-31`**: API Sinh Mã VietQR Động Gắn với `payment_ref_code` Cho Học viên
  - *File chi tiết*: [`phase-3-payments/TASK-FIN-31-PAY-VIETQR-GENERATOR.md`](phase-3-payments/TASK-FIN-31-PAY-VIETQR-GENERATOR.md)
  - *Assignee*: `Chi & AI Assistant` | *Ngày xong*: `2026-11-09`
- [x] **`TASK-FIN-32`**: Webhook VietQR Đối soát Ngân hàng Thời gian Thực, Idempotency `bank_transaction_id` & Callback sang Admission
  - *File chi tiết*: [`phase-3-payments/TASK-FIN-32-PAY-WEBHOOK-RECONCILE.md`](phase-3-payments/TASK-FIN-32-PAY-WEBHOOK-RECONCILE.md)
  - *Invariant*: `INV-FIN-02`, `INV-FIN-05`
  - *Assignee*: `Chi & AI Assistant` | *Ngày xong*: `2026-11-10`

---

### Phase 4: Ví Bảo lưu Học viên (WAL)
- [x] **`TASK-FIN-40`**: Quản lý Ví Bảo lưu Học viên & API Tra cứu Số dư Nội bộ
  - *File chi tiết*: [`phase-4-wallets/TASK-FIN-40-WAL-RESERVATION-CREDIT.md`](phase-4-wallets/TASK-FIN-40-WAL-RESERVATION-CREDIT.md)
  - *Invariant*: `INV-FIN-06`
  - *Assignee*: `Chi & AI Assistant` | *Ngày xong*: `2026-11-09`
- [x] **`TASK-FIN-41`**: Giao dịch Cấn trừ Học phí Khóa học Tiếp theo (`TUITION_OFFSET`) & Khóa Rút Tiền Mặt
  - *File chi tiết*: [`phase-4-wallets/TASK-FIN-41-WAL-TUITION-OFFSET.md`](phase-4-wallets/TASK-FIN-41-WAL-TUITION-OFFSET.md)
  - *Invariant*: `INV-FIN-06`
  - *Assignee*: `Chi & AI Assistant` | *Ngày xong*: `2026-11-09`

---

### Phase 5: Quy trình Hoàn tiền 3 cấp (REF)
- [x] **`TASK-FIN-50`**: Tạo Yêu cầu Hoàn tiền Cấp 1 (Tư vấn viên), Đối soát Hủy lớp Cascade & Kiểm tra Điều kiện Số buổi
  - *File chi tiết*: [`phase-5-refunds/TASK-FIN-50-REF-CREATE-REQUEST.md`](phase-5-refunds/TASK-FIN-50-REF-CREATE-REQUEST.md)
  - *Invariant*: `INV-FIN-07`
  - *Assignee*: `Chi & AI Assistant` | *Ngày xong*: `2026-11-10`
- [x] **`TASK-FIN-51`**: Xác minh Công nợ & Số buổi Cấp 2 (Kế toán - `PENDING_DIRECTOR`)
  - *File chi tiết*: [`phase-5-refunds/TASK-FIN-51-REF-ACCOUNTANT-VERIFY.md`](phase-5-refunds/TASK-FIN-51-REF-ACCOUNTANT-VERIFY.md)
  - *Assignee*: `Chi & AI Assistant` | *Ngày xong*: `2026-11-09`
- [x] **`TASK-FIN-52`**: Phê duyệt Hoàn tiền Cấp 3 (Giám đốc - `APPROVED`/`REJECTED`)
  - *File chi tiết*: [`phase-5-refunds/TASK-FIN-52-REF-DIRECTOR-APPROVE.md`](phase-5-refunds/TASK-FIN-52-REF-DIRECTOR-APPROVE.md)
  - *Assignee*: `Chi & AI Assistant` | *Ngày xong*: `2026-11-09`
- [x] **`TASK-FIN-53`**: Ghi nhận Giải ngân (Kế toán - `COMPLETED`) & Khóa Bất biến Hóa đơn `REFUNDED`
  - *File chi tiết*: [`phase-5-refunds/TASK-FIN-53-REF-COMPLETE-DISBURSE.md`](phase-5-refunds/TASK-FIN-53-REF-COMPLETE-DISBURSE.md)
  - *Invariant*: `INV-FIN-01`, `INV-FIN-07`
  - *Assignee*: `Chi & AI Assistant` | *Ngày xong*: `2026-11-09`

---

### Phase 6: Báo cáo Thông minh BI & Dashboard (BI)
- [x] **`TASK-FIN-60`**: API Báo cáo Doanh thu Thực thu Theo Tháng Cash-basis (`vw_bi_cash_revenue_monthly`)
  - *File chi tiết*: [`phase-6-bi/TASK-FIN-60-BI-CASH-REVENUE.md`](phase-6-bi/TASK-FIN-60-BI-CASH-REVENUE.md)
  - *Assignee*: `Chi & AI Assistant` | *Ngày xong*: `2026-11-09`
- [x] **`TASK-FIN-61`**: API Báo cáo Hiệu quả Chiến dịch Voucher (`vw_bi_voucher_campaigns`)
  - *File chi tiết*: [`phase-6-bi/TASK-FIN-61-BI-VOUCHER-CAMPAIGNS.md`](phase-6-bi/TASK-FIN-61-BI-VOUCHER-CAMPAIGNS.md)
  - *Assignee*: `Chi & AI Assistant` | *Ngày xong*: `2026-11-09`
- [x] **`TASK-FIN-62`**: API Báo cáo Công nợ Phải thu, Quỹ Bảo lưu & Chỉ số Doanh thu Liên thông Admission BI (`admission-metrics`)
  - *File chi tiết*: [`phase-6-bi/TASK-FIN-62-BI-REFUND-SUMMARY.md`](phase-6-bi/TASK-FIN-62-BI-REFUND-SUMMARY.md)
  - *Assignee*: `Chi & AI Assistant` | *Ngày xong*: `2026-11-10`

---

### Phase 7: Giao diện Người dùng Frontend (UI)
- [x] **`TASK-FIN-70`**: Giao diện Tra cứu & Quản lý Hóa đơn, Lịch trả góp (`invoices.html`)
  - *File chi tiết*: [`phase-7-frontend/TASK-FIN-70-UI-INVOICES-LIST.md`](phase-7-frontend/TASK-FIN-70-UI-INVOICES-LIST.md)
  - *Assignee*: `Chi & AI Assistant` | *Ngày xong*: `2026-11-09`
- [x] **`TASK-FIN-71`**: Giao diện Thu tiền mặt, Biểu phí, Chiến dịch Voucher & Quét mã VietQR (`tuition.html`, `invoices.html`)
  - *File chi tiết*: [`phase-7-frontend/TASK-FIN-71-UI-PAYMENT-VIETQR.md`](phase-7-frontend/TASK-FIN-71-UI-PAYMENT-VIETQR.md)
  - *Assignee*: `Chi & AI Assistant` | *Ngày xong*: `2026-11-09`
- [x] **`TASK-FIN-72`**: Giao diện Quản lý Ví bảo lưu & Bàn xử lý Yêu cầu Hoàn tiền 3 cấp (`invoices.html`)
  - *File chi tiết*: [`phase-7-frontend/TASK-FIN-72-UI-WALLET-REFUNDS.md`](phase-7-frontend/TASK-FIN-72-UI-WALLET-REFUNDS.md)
  - *Assignee*: `Chi & AI Assistant` | *Ngày xong*: `2026-11-09`
- [x] **`TASK-FIN-73`**: Dashboard Trực quan hóa Báo cáo BI Doanh thu & Voucher (`bi-dashboard.html`)
  - *File chi tiết*: [`phase-7-frontend/TASK-FIN-73-UI-BI-DASHBOARD.md`](phase-7-frontend/TASK-FIN-73-UI-BI-DASHBOARD.md)
  - *Assignee*: `Chi & AI Assistant` | *Ngày xong*: `2026-11-10`
