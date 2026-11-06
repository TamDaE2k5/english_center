# TASK-FIN-51: Xác minh Công nợ & Số buổi Cấp 2 (Kế toán - PENDING_DIRECTOR)

- **Mã nhiệm vụ**: `TASK-FIN-51`
- **Phân kỳ**: `Phase 5: Quy trình Hoàn tiền 3 cấp (REF)`
- **Độ ưu tiên**: `P1`
- **Yêu cầu chức năng liên quan**: `FIN-F08` (Hoàn tiền 3 cấp - Xác minh)

---

## 1. Mục tiêu Nghiệp vụ & Kỹ thuật
Xây dựng endpoint `PUT /api/finance/refunds/{id}/verify` dành riêng cho Kế toán (`STAFF`):
1. Rà soát chứng từ thực thu trong `finance.payments`, đối soát công nợ đã hoàn thành.
2. Ghi nhận `verified_by_accountant_id`, `verified_at = NOW()` và ghi chú giải trình `accountant_note`.
3. Chuyển trạng thái yêu cầu từ `PENDING_ACCOUNTANT` sang `PENDING_DIRECTOR`.
4. Nếu phát hiện sai lệch, Kế toán có quyền từ chối (`REJECTED`) kèm lý do nghiệp vụ.

---

## 2. Tiêu chí Nghiệm thu
- [ ] Chỉ nhân viên có vai trò Kế toán mới được thực hiện xác minh.
- [ ] Không thể xác minh nếu yêu cầu đang ở trạng thái khác `PENDING_ACCOUNTANT`.
