# TASK-FIN-10: Quản lý Chiến dịch Voucher (Tạo mới, Cập nhật, Giới hạn lượt dùng & Hạn dùng)

- **Mã nhiệm vụ**: `TASK-FIN-10`
- **Phân kỳ**: `Phase 1: Biểu phí & Chiến dịch Voucher (VOU)`
- **Độ ưu tiên**: `P1`
- **Yêu cầu chức năng liên quan**: `FIN-F01` (Quản lý chiến dịch Voucher)

---

## 1. Mục tiêu Nghiệp vụ & Kỹ thuật
Cung cấp bộ REST API cho phép Admin và Kế toán quản lý các chiến dịch Voucher ưu đãi:
1. Tạo Voucher với hai hình thức: giảm phần trăm (`discount_percent`) hoặc giảm tiền mặt cố định (`discount_amount`).
2. Thiết lập điều kiện: giá trị đơn hàng tối thiểu (`min_order_value`), mức giảm tối đa (`max_discount_amount`), tổng lượt sử dụng tối đa (`max_uses`), và thời hạn hiệu lực (`expires_at`).
3. Truy vấn danh sách Voucher và lịch sử áp dụng.

---

## 2. Tiêu chí Nghiệm thu
- [ ] Endpoint `POST /api/finance/vouchers` tạo mới Voucher hợp lệ, từ chối khi mã code trùng lặp.
- [ ] Endpoint `GET /api/finance/vouchers` trả về danh sách có phân trang và trạng thái kích hoạt `is_active`.
