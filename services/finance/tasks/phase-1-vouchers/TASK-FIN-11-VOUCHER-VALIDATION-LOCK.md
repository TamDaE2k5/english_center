# TASK-FIN-11: Động cơ Kiểm tra Voucher & Khóa dòng SELECT FOR UPDATE Chống Race Condition

- **Mã nhiệm vụ**: `TASK-FIN-11`
- **Phân kỳ**: `Phase 1: Biểu phí & Chiến dịch Voucher (VOU)`
- **Độ ưu tiên**: `P0 (Bắt buộc)`
- **Yêu cầu chức năng liên quan**: `FIN-F02` (Kiểm tra tính hợp lệ Voucher)
- **Quy tắc bất biến bảo vệ**: `INV-FIN-04` (Chống Race Condition khi dùng Voucher)

---

## 1. Mục tiêu Nghiệp vụ & Kỹ thuật
Xây dựng logic kiểm tra và áp dụng Voucher trong Database Transaction:
1. Endpoint `POST /api/finance/vouchers/validate` kiểm tra tính hợp lệ, tính số tiền được chiết khấu dự kiến `discount_applied`.
2. Khi áp dụng Voucher vào hóa đơn, bắt buộc sử dụng `SELECT ... FOR UPDATE` trên bảng `finance.vouchers`.
3. Kiểm tra điều kiện bất biến: `is_active = TRUE`, `NOW() <= expires_at`, `current_uses < max_uses` và `order_amount >= min_order_value`.
4. Tăng `current_uses = current_uses + 1` một cách an toàn dưới tải đồng thời cao.

---

## 2. Tiêu chí Nghiệm thu
- [ ] Trả về `discount_applied` chính xác theo phần trăm hoặc số tiền cố định.
- [ ] Chặn tuyệt đối không cho áp dụng khi Voucher quá hạn hoặc đã hết lượt dùng (`current_uses >= max_uses`).
- [ ] Không bị lỗi race condition khi nhiều người dùng cùng submit mã voucher còn 1 lượt cuối cùng.
