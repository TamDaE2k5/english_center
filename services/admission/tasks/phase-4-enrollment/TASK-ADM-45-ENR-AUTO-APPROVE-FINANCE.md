# TASK-ADM-45: Webhook Tiếp nhận Hóa đơn PAID từ Finance Service & Tự động Duyệt

- **Mã nhiệm vụ**: `TASK-ADM-45`
- **Phân kỳ**: `Phase 4: Tuyển sinh, Giữ chỗ 48h & Xét duyệt (ENR)`
- **Độ ưu tiên**: `P0 (Bắt buộc)`
- **Yêu cầu chức năng liên quan**: `FR-ENR-06`, `INV-ADM-01`
- **Quy tắc bất biến bảo vệ**: `INV-ADM-01`, `INV-SYS-01`, `INV-SYS-02`

---

## 1. Mục tiêu Nghiệp vụ & Kỹ thuật
Xây dựng cơ chế tiếp nhận sự kiện thanh toán bất đồng bộ (Webhook / Internal Event Callback) từ **Finance Service** (Port 8084):
1. **Tiếp nhận Webhook**:
   - Endpoint: `POST /api/admission/webhooks/payment-success`
   - Nhận thông báo khi hóa đơn học phí tương ứng của đơn đăng ký đã được thanh toán đầy đủ (`invoice_status = 'PAID'`).
2. **Xử lý Tự động Duyệt Đơn (Auto-Approve) & Đối Soát Idempotency (INV-ENR-02, INV-ENR-03)**:
   - Tra cứu bảng `admission.processed_finance_events` theo `finance_event_id`:
     - Nếu `finance_event_id` đã tồn tại $\rightarrow$ Đã xử lý trước đó, trả về HTTP 200 OK ngay lập tức (Idempotent - INV-ENR-02).
   - Tra cứu đơn đăng ký theo `enrollment_id`.
   - **Xử lý Thanh toán Đến Muộn (INV-ENR-03)**:
     - Nếu đơn đã ở trạng thái `EXPIRED` hoặc `CANCELLED`: **KHÔNG ĐƯỢC TỰ ĐỘNG CHUYỂN SANG APPROVED**.
     - Lưu sự kiện vào `admission.processed_finance_events` với ghi chú đối soát để nhân viên kế toán chuyển sang ví bảo lưu hoặc hoàn tiền.
   - **Bảo vệ Tính Bất Biến Sĩ Số (INV-ADM-01)**:
     - Trước khi chuyển `APPROVED`, bắt buộc khóa dòng lớp học (`SELECT FOR UPDATE`) và kiểm tra sĩ số `approvedCount < maxStudents`.
     - Nếu lớp đã đủ sĩ số trần: Trả về HTTP 422 Unprocessable Entity kèm mã `ADM_CLASS_FULL`.
   - Nếu đủ điều kiện duyệt:
     - Chuyển `enrollments.status = 'APPROVED'`, `approved_at = NOW()`, `approval_method = 'ONLINE_PAYMENT'`.
     - Chèn bản ghi vào `admission.processed_finance_events` (`enrollment_id`, `finance_event_id`, `paid_at`).
     - Kích hoạt quy trình đồng bộ hồ sơ sang Student Service (TASK-ADM-47).

---

## 2. Phạm vi File Tác động

### File tạo mới hoặc chỉnh sửa:
- `services/admission/backend/src/main/java/com/englishcenter/admission/controller/WebhookController.java`
- `services/admission/backend/src/main/java/com/englishcenter/admission/service/EnrollmentService.java`
- `services/admission/backend/src/main/java/com/englishcenter/admission/service/impl/EnrollmentServiceImpl.java`
- `services/admission/backend/src/main/java/com/englishcenter/admission/dto/webhook/PaymentSuccessWebhookRequest.java`

---

## 3. Đặc tả API Hợp đồng Webhook

### Request: `POST /api/admission/webhooks/payment-success`
```json
{
  "event_id": "EVT-FIN-987654",
  "enrollment_id": "ENR-2026-0089",
  "invoice_id": "INV-2026-0120",
  "amount_paid": 4500000.0,
  "paid_at": "2026-11-01T21:40:00",
  "transaction_ref": "TXN-MBBANK-20261101-098"
}
```

### Response Thành công (HTTP 200 OK):
```json
{
  "success": true,
  "data": {
    "enrollment_id": "ENR-2026-0089",
    "previous_status": "PENDING",
    "new_status": "APPROVED",
    "approved_at": "2026-11-01T21:40:02"
  },
  "message": "Xác nhận thanh toán và tự động duyệt đơn nhập học thành công"
}
```

---

## 4. Kế hoạch Triển khai (Step-by-step)
1. Tạo DTO `PaymentSuccessWebhookRequest` với các trường cần thiết.
2. Trong `WebhookController`:
   - Endpoint công khai trong mạng nội bộ (hoặc kiểm tra Header Secret Key giữa các service).
   - Gọi `enrollmentService.processPaymentSuccess(request)`.
3. Trong `EnrollmentServiceImpl`:
   - Kiểm tra `enrollment.getStatus() == EnrollmentStatus.APPROVED` $\rightarrow$ Trả về kết quả thành công ngay (Idempotent).
   - Gọi hàm duyệt an toàn `approveEnrollment(enrollment.getId())` (đã có Invariant Guard bảo vệ sĩ số trần).
4. Viết comment tiếng Việt chi tiết cho toàn bộ luồng callback.

---

## 5. Tiêu chí Nghiệm thu (Acceptance Criteria)
- [ ] Gửi webhook hợp lệ $\rightarrow$ Đơn chuyển sang `APPROVED`, `approved_at` được gán mốc thời gian hiện tại.
- [ ] Gửi lại webhook đó lần thứ hai $\rightarrow$ Vẫn trả về 200 OK mà không bị trùng lặp dữ liệu hay tăng sĩ số 2 lần.
- [ ] Nếu lớp học đã bị đầy trước khi tiền về $\rightarrow$ Invariant Guard `INV-ADM-01` chặn đứng việc chuyển sang `APPROVED`.
