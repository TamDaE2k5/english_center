# TASK-FIN-52: Phê duyệt Hoàn tiền Cấp 3 (Giám đốc - APPROVED/REJECTED)

- **Mã nhiệm vụ**: `TASK-FIN-52`
- **Phân kỳ**: `Phase 5: Quy trình Hoàn tiền 3 cấp (REF)`
- **Độ ưu tiên**: `P0 (Bắt buộc)`
- **Yêu cầu chức năng liên quan**: `FIN-F08` (Hoàn tiền 3 cấp - Phê duyệt)

---

## 1. Mục tiêu Nghiệp vụ & Kỹ thuật
Xây dựng endpoint `PUT /api/finance/refunds/{id}/approve` dành riêng cho Giám đốc (`ADMIN`):
1. Giám đốc kiểm duyệt hồ sơ hoàn tiền sau khi đã qua bước xác minh của Kế toán.
2. Quyết định phê duyệt:
   - Chấp thuận (`APPROVED`): Chốt số tiền hoàn thực tế `approved_refund_amount` (có thể điều chỉnh nhỏ so với mức đề xuất) và ghi chú `director_note`.
   - Từ chối (`REJECTED`): Ghi rõ lý do bác bỏ đơn hoàn tiền.
3. Ghi nhận `approved_by_director_id` và `approved_at = NOW()`.

---

## 2. Tiêu chí Nghiệm thu
- [ ] Chỉ người dùng có role `ADMIN` mới có quyền phê duyệt.
- [ ] Không thể duyệt trực tiếp nếu chưa qua bước xác minh của Kế toán (State Machine Guard).
