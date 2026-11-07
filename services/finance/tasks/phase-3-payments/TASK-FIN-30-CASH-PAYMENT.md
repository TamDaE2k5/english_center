# TASK-FIN-30: Ghi nhận Phiếu thu Tiền mặt Tại quầy & Tự động Cập nhật Trạng thái PARTIAL/PAID

- **Mã nhiệm vụ**: `TASK-FIN-30`
- **Phân kỳ**: `Phase 3: Thu tiền & Thanh toán Đa kênh (PAY)`
- **Độ ưu tiên**: `P0 (Bắt buộc)`
- **Yêu cầu chức năng liên quan**: `FIN-F05` (Ghi nhận thanh toán tiền mặt)
- **Quy tắc bất biến bảo vệ**: `INV-FIN-01`, `INV-FIN-02` (Đẳng thức thanh toán)

---

## 1. Mục tiêu Nghiệp vụ & Kỹ thuật
Xây dựng API `POST /api/finance/payments/cash` dành riêng cho Kế toán:
1. Ghi nhận giao dịch vào `finance.payments` với `payment_method = 'CASH'`, `status = 'SUCCESS'`.
2. Khóa dòng hóa đơn bằng `SELECT ... FOR UPDATE`.
3. Cập nhật `paid_amount = paid_amount + amount` trong `finance.invoices`.
4. Nếu có gắn `installment_id`, cập nhật `amount_paid` của kỳ đó.
5. Cập nhật trạng thái kỳ và trạng thái hóa đơn:
   - `PAID` nếu `paid_amount >= final_amount`.
   - `PARTIAL` nếu `0 < paid_amount < final_amount`.
6. Tự động set `paid_at = NOW()` khi hoàn tất 100%.

---

## 2. Tiêu chí Nghiệm thu
- [ ] Không cho phép nạp số tiền `<= 0`.
- [ ] Không cho phép thu vượt quá số tiền còn lại của hóa đơn (`paid_amount <= final_amount`).
- [ ] Trạng thái hóa đơn chuyển đúng `PARTIAL` hoặc `PAID`.
