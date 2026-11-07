# TASK-FIN-22: API Tra cứu Danh sách & Chi tiết Hóa đơn, Khoản mục và Lịch sử Đóng tiền

- **Mã nhiệm vụ**: `TASK-FIN-22`
- **Phân kỳ**: `Phase 2: Hóa đơn & Lịch Đóng phí Nhiều đợt (INV/INS)`
- **Độ ưu tiên**: `P1`
- **Yêu cầu chức năng liên quan**: `FIN-F04` (Tra cứu hóa đơn)

---

## 1. Mục tiêu Nghiệp vụ & Kỹ thuật
Cung cấp API tra cứu dữ liệu hóa đơn linh hoạt:
1. `GET /api/finance/invoices`: Danh sách hóa đơn có bộ lọc theo `student_id`, `status` (`PENDING`, `PARTIAL`, `PAID`, `CANCELLED`, `REFUNDED`), phân trang chuẩn.
2. `GET /api/finance/invoices/{id}`: Trả về đầy đủ thông tin hóa đơn, danh sách `invoice_items`, danh sách các kỳ `installments` và lịch sử các khoản thanh toán trong `payments`.
3. Phân quyền: Học viên (`STUDENT`) chỉ xem được hóa đơn của chính mình; `STAFF` và `ADMIN` xem được toàn bộ.

---

## 2. Tiêu chí Nghiệm thu
- [ ] Học viên không xem được hóa đơn của học viên khác (HTTP 403).
- [ ] Response chi tiết trả về đầy đủ items và kỳ thanh toán.
