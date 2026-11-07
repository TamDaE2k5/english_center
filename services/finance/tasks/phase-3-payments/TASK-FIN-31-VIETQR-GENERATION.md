# TASK-FIN-31: API Sinh Mã VietQR Động Gắn với payment_ref_code Cho Học viên

- **Mã nhiệm vụ**: `TASK-FIN-31`
- **Phân kỳ**: `Phase 3: Thu tiền & Thanh toán Đa kênh (PAY)`
- **Độ ưu tiên**: `P1`
- **Yêu cầu chức năng liên quan**: `FIN-F06` (Sinh mã VietQR)

---

## 1. Mục tiêu Nghiệp vụ & Kỹ thuật
Xây dựng API `GET /api/finance/invoices/{id}/vietqr`:
1. Tra cứu thông tin kỳ thanh toán cần đóng gần nhất hoặc tổng số tiền hóa đơn.
2. Sinh chuỗi chuẩn VietQR (chuẩn NAPAS 247) bao gồm: Mã định danh ngân hàng trung tâm, số tài khoản nhận, số tiền chính xác (`amount_due` hoặc `remaining_amount`), và nội dung chuyển khoản bắt buộc chứa `payment_ref_code`.
3. Trả về cả chuỗi payload VietQR và URL tạo ảnh QR phục vụ hiển thị trên giao diện thanh toán.

---

## 2. Tiêu chí Nghiệm thu
- [ ] Học viên mở modal thanh toán thấy mã QR hiển thị tức thì.
- [ ] Nội dung chuyển khoản chứa chính xác `payment_ref_code` để ngân hàng đối soát.
