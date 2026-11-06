# TASK-FIN-71: Bàn Giao dịch Thu tiền mặt & Quét mã VietQR (tuition.html)

- **Mã nhiệm vụ**: `TASK-FIN-71`
- **Phân kỳ**: `Phase 7: Giao diện Người dùng Frontend (UI)`
- **Độ ưu tiên**: `P1`
- **Yêu cầu chức năng liên quan**: UI Phân hệ Finance (`tuition.html`)

---

## 1. Mục tiêu Nghiệp vụ & Kỹ thuật
Nâng cấp trang `services/finance/frontend/tuition.html`:
1. Modal Thu tiền mặt tại quầy dành cho Kế toán: Nhập số tiền thu, chọn kỳ thanh toán, in/hiển thị biên lai.
2. Modal Quét mã VietQR dành cho Học viên & Tư vấn viên: Hiển thị mã QR động, nút sao chép số tài khoản và nội dung chuyển khoản `payment_ref_code`.
3. Cơ chế tự động polling hoặc lắng nghe WebSocket để tự động đổi badge `PAID` ngay khi nhận Webhook thành công.

---

## 2. Tiêu chí Nghiệm thu
- [ ] Quét mã QR thanh toán hiển thị đúng thông tin và cập nhật trạng thái tức thời.
