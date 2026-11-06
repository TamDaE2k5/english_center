# TASK-FIN-50: Tạo Yêu cầu Hoàn tiền Cấp 1 (Tư vấn viên) & Kiểm tra Điều kiện Số buổi từ Learning

- **Mã nhiệm vụ**: `TASK-FIN-50`
- **Phân kỳ**: `Phase 5: Quy trình Hoàn tiền 3 cấp (REF)`
- **Độ ưu tiên**: `P0 (Bắt buộc)`
- **Yêu cầu chức năng liên quan**: `FIN-F08` (Hoàn tiền 3 cấp - Khởi tạo)
- **Quy tắc bất biến bảo vệ**: `INV-FIN-07` (Chặn hoàn tiền theo số buổi thực học)

---

## 1. Mục tiêu Nghiệp vụ & Kỹ thuật
Xây dựng endpoint `POST /api/finance/refunds` dành cho Tư vấn viên (`STAFF`):
1. Nhận thông tin: `invoice_id`, `class_id`, `reason`, thông tin tài khoản nhận tiền (`bank_name`, `bank_account_no`, `bank_account_holder`).
2. Gọi Learning Service lấy snapshot dữ liệu điểm danh: `total_sessions` và `attended_sessions`.
3. Kiểm tra điều kiện bất biến `INV-FIN-07`:
   - Nếu `attended_sessions > max_allowed_sessions` (mặc định > 3 buổi): **Chặn ngay lập tức** với lỗi HTTP 400 (`FIN_REFUND_LIMIT_EXCEEDED`).
4. Nếu đủ điều kiện, tính toán số tiền hoàn đề xuất `requested_refund_amount` dựa trên tỷ lệ hợp đồng sau khi trừ phí giáo trình/tài liệu (`deduction_amount`).
5. Tạo bản ghi trong `finance.refund_requests` với trạng thái ban đầu `PENDING_ACCOUNTANT`.

---

## 2. Tiêu chí Nghiệm thu
- [ ] Học viên đã học quá 3 buổi bị chặn hoàn tiền và nhận thông báo lỗi chi tiết.
- [ ] Snapshot số tiền thanh toán `paid_amount_snapshot` và số buổi học được lưu lại chính xác.
