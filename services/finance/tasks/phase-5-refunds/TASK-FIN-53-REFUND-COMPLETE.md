# TASK-FIN-53: Ghi nhận Giải ngân (Kế toán - COMPLETED) & Khóa Bất biến Hóa đơn REFUNDED

- **Mã nhiệm vụ**: `TASK-FIN-53`
- **Phân kỳ**: `Phase 5: Quy trình Hoàn tiền 3 cấp (REF)`
- **Độ ưu tiên**: `P0 (Bắt buộc)`
- **Yêu cầu chức năng liên quan**: `FIN-F08` (Hoàn tất hoàn tiền)
- **Quy tắc bất biến bảo vệ**: `INV-FIN-01` (Bất biến hóa đơn đã hoàn tiền)

---

## 1. Mục tiêu Nghiệp vụ & Kỹ thuật
Xây dựng endpoint `PUT /api/finance/refunds/{id}/complete` cho Kế toán ghi nhận giải ngân:
1. Nhập mã ủy nhiệm chi/mã giao dịch ngân hàng chuyển tiền hoàn cho học viên.
2. Cập nhật `finance.refund_requests`:
   - `status = 'COMPLETED'`
   - `completed_at = NOW()`
3. Cập nhật hóa đơn liên quan trong `finance.invoices`:
   - `status = 'REFUNDED'`
4. Kích hoạt bảo vệ bất biến `INV-FIN-01`: Tuyệt đối không cho phép cập nhật tiền tệ hoặc xóa hóa đơn khi đã ở trạng thái `REFUNDED`.

---

## 2. Tiêu chí Nghiệm thu
- [ ] Yêu cầu chuyển sang `COMPLETED`, hóa đơn chuyển sang `REFUNDED`.
- [ ] Cố ý sửa tiền hoặc xóa hóa đơn đã hoàn bị từ chối với lỗi HTTP 400.
