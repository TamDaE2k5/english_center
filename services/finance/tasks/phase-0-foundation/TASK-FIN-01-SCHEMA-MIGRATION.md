# TASK-FIN-01: Khởi tạo Schema PostgreSQL 8 Bảng, 2 BI Views, Ràng buộc DDL & Seed Data

- **Mã nhiệm vụ**: `TASK-FIN-01`
- **Phân kỳ**: `Phase 0: Nền móng & Hạ tầng (INF)`
- **Độ ưu tiên**: `P0 (Bắt buộc)`
- **Yêu cầu chức năng liên quan**: Cơ sở dữ liệu phân hệ Finance (Finance Module 2.0)
- **Quy tắc bất biến bảo vệ**: `INV-FIN-03` (Cô lập Schema), `INV-SYS-02` (4 Schema độc lập)

---

## 1. Mục tiêu Nghiệp vụ & Kỹ thuật
Triển khai toàn văn mã DDL PostgreSQL cho schema `finance`:
1. Tạo 8 bảng nghiệp vụ: `vouchers`, `invoices`, `invoice_items`, `installments`, `payments`, `student_wallets`, `wallet_transactions`, `refund_requests`.
2. Tạo 2 BI Views: `finance.vw_bi_cash_revenue_monthly` và `finance.vw_bi_voucher_campaigns`.
3. Áp dụng các ràng buộc toàn vẹn: CHECK math (`final_amount = amount - discount_applied`), ràng buộc giới hạn lượt dùng Voucher, và Logical FKs tới các phân hệ khác.

---

## 2. Tiêu chí Nghiệm thu
- [ ] Schema `finance` được khởi tạo thành công với 8 bảng và 2 views.
- [ ] Chèn dữ liệu mẫu cho vouchers, invoices, installments kiểm tra không phát sinh lỗi.
- [ ] Invariant test `INV-SYS-02` chạy thành công 100%.
