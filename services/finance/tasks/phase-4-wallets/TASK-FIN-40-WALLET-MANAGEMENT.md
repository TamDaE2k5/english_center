# TASK-FIN-40: Quản lý Ví Bảo lưu Học viên & API Tra cứu Số dư Nội bộ

- **Mã nhiệm vụ**: `TASK-FIN-40`
- **Phân kỳ**: `Phase 4: Ví Bảo lưu Học viên (WAL)`
- **Độ ưu tiên**: `P1`
- **Yêu cầu chức năng liên quan**: `FIN-F07` (Ví bảo lưu học viên)
- **Quy tắc bất biến bảo vệ**: `INV-FIN-06` (Khóa rút tiền ví bảo lưu)

---

## 1. Mục tiêu Nghiệp vụ & Kỹ thuật
Xây dựng module ví bảo lưu học phí nội bộ:
1. Mỗi học viên sở hữu duy nhất 1 ví trong bảng `finance.student_wallets` (`student_id UNIQUE`).
2. Mặc định `is_withdrawable = FALSE` (không cho rút tiền mặt ra ngoài).
3. API `GET /api/finance/wallets/{student_id}`: Tra cứu số dư ví hiện tại và lịch sử giao dịch trong `finance.wallet_transactions`.
4. Hỗ trợ cộng tiền bảo lưu `RESERVATION_CREDIT` khi học viên tạm dừng khóa học vì lý do cá nhân.

---

## 2. Tiêu chí Nghiệm thu
- [ ] Số dư ví không bao giờ âm (`balance >= 0`).
- [ ] Mọi biến động số dư đều được ghi nhật ký đầy đủ trong `wallet_transactions`.
