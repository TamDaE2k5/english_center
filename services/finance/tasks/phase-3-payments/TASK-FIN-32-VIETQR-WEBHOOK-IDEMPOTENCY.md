# TASK-FIN-32: Webhook VietQR Đối soát Ngân hàng Thời gian Thực & Idempotency bank_transaction_id

- **Mã nhiệm vụ**: `TASK-FIN-32`
- **Phân kỳ**: `Phase 3: Thu tiền & Thanh toán Đa kênh (PAY)`
- **Độ ưu tiên**: `P0 (Bắt buộc)`
- **Yêu cầu chức năng liên quan**: `FIN-F06` (Webhook VietQR)
- **Quy tắc bất biến bảo vệ**: `INV-FIN-05` (Idempotency gạch nợ VietQR)

---

## 1. Mục tiêu Nghiệp vụ & Kỹ thuật
Xây dựng endpoint tiếp nhận Webhook ngân hàng `POST /api/finance/webhooks/vietqr`:
1. Kiểm tra mã giao dịch ngân hàng `bank_transaction_id`:
   - Nếu `bank_transaction_id` đã tồn tại trong `finance.payments`: Ngay lập tức trả về `HTTP 200 OK`, tuyệt đối không ghi nhận payment lần 2 và không tăng tiền.
2. Trích xuất `payment_ref_code` từ nội dung chuyển khoản (`content`), tìm đúng bản ghi `installments` và `invoices`.
3. Trong 1 transaction:
   - Thêm bản ghi `finance.payments` với `payment_method = 'VIETQR'`.
   - Cập nhật số tiền đã thu và trạng thái `PAID`/`PARTIAL` cho hóa đơn.
4. Thông báo sang Admission Service (qua HTTP webhook) để tự động duyệt đơn đăng ký (`APPROVED`).

---

## 2. Tiêu chí Nghiệm thu
- [ ] Gửi Webhook trùng 2 lần, chỉ có đúng 1 payment được tạo.
- [ ] Invariant test `INV-FIN-02` (idempotency) chạy đạt `OK`.
