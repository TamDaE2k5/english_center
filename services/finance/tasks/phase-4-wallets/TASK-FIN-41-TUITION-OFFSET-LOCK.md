# TASK-FIN-41: Giao dịch Cấn trừ Học phí Khóa học Tiếp theo (TUITION_OFFSET) & Khóa Rút Tiền Mặt

- **Mã nhiệm vụ**: `TASK-FIN-41`
- **Phân kỳ**: `Phase 4: Ví Bảo lưu Học viên (WAL)`
- **Độ ưu tiên**: `P0 (Bắt buộc)`
- **Yêu cầu chức năng liên quan**: `FIN-F07` (Cấn trừ học phí)
- **Quy tắc bất biến bảo vệ**: `INV-FIN-06` (Khóa rút tiền ví bảo lưu)

---

## 1. Mục tiêu Nghiệp vụ & Kỹ thuật
Xây dựng tính năng cấn trừ học phí từ ví bảo lưu cho hóa đơn mới:
1. Endpoint `POST /api/finance/wallets/{student_id}/reserve`:
   - Thực hiện giao dịch `tx_type = 'TUITION_OFFSET'`.
   - Trừ số dư ví `balance` của học viên (với khóa dòng `SELECT ... FOR UPDATE`).
   - Ghi nhận thanh toán vào `finance.payments` với `payment_method = 'WALLET_OFFSET'`, gắn với `related_invoice_id`.
   - Cập nhật số tiền đã thanh toán của hóa đơn mới.
2. Tuyệt đối không cho phép tạo giao dịch rút tiền mặt (`WITHDRAWAL`). Nếu cờ `is_withdrawable` là `FALSE`, từ chối mọi yêu cầu rút tiền trực tiếp.

---

## 2. Tiêu chí Nghiệm thu
- [ ] Không thể cấn trừ số tiền vượt quá số dư hiện có trong ví.
- [ ] Số tiền cấn trừ được hạch toán chính xác vào hóa đơn mới.
