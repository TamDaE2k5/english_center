# DANH MỤC API CATALOG CHI TIẾT
## PHÂN HỆ TÀI CHÍNH & HỌC PHÍ (FINANCE SERVICE)

> **Căn cứ**: [`../finance-tech-spec.md`](../finance-tech-spec.md)  
> **Prefix Gateway**: `/api/finance/...`  
> **Chuẩn dữ liệu**: JSON `snake_case`, Stateless JWT (HS256)

---

## 1. Biểu phí & Chiến dịch Voucher (VOU)

### FIN-F01: Tạo chiến dịch Voucher mới
- **Endpoint**: `POST /api/finance/vouchers`
- **Quyền**: `ADMIN`, `STAFF` (Kế toán)
- **Request Body**:
  ```json
  {
    "code": "SUMMER2026",
    "campaign_name": "Ưu đãi chào hè 2026",
    "discount_percent": 15,
    "discount_amount": 0,
    "min_order_value": 3000000,
    "max_discount_amount": 1000000,
    "max_uses": 200,
    "expires_at": "2026-08-31T23:59:59"
  }
  ```
- **Response**: `201 Created`

### FIN-F01: Danh sách chiến dịch Voucher
- **Endpoint**: `GET /api/finance/vouchers`
- **Quyền**: `ADMIN`, `STAFF`

### FIN-F02: Kiểm tra tính hợp lệ & Tính giảm giá Voucher
- **Endpoint**: `POST /api/finance/vouchers/validate`
- **Quyền**: `ADMIN`, `STAFF`, `STUDENT`
- **Request Body**:
  ```json
  {
    "code": "SUMMER2026",
    "order_amount": 5000000
  }
  ```
- **Response**:
  ```json
  {
    "success": true,
    "data": {
      "voucher_id": 1,
      "code": "SUMMER2026",
      "discount_applied": 750000,
      "final_amount": 4250000
    }
  }
  ```

---

## 2. Hóa đơn & Lịch Đóng phí Nhiều đợt (INV/INS)

### FIN-F03: Tự động sinh Hóa đơn khi Admission chốt đăng ký
- **Endpoint**: `POST /api/finance/invoices/auto-generate`
- **Quyền**: `ADMIN`, `STAFF` hoặc Gọi nội bộ (Internal Service)
- **Request Body**:
  ```json
  {
    "student_id": 101,
    "course_id": 5,
    "enrollment_id": 12,
    "invoice_type": "COURSE_PACKAGE",
    "amount": 6000000,
    "voucher_code": "SUMMER2026",
    "items": [
      {
        "course_id": 5,
        "item_type": "TUITION_PACKAGE",
        "description": "Khóa học IELTS Fighter Intensive 12 tuần",
        "quantity": 1,
        "unit_price": 6000000
      }
    ],
    "installments_count": 2
  }
  ```
- **Response**: `201 Created` (trả về `invoice_id`, `final_amount`, `payment_ref_code`)

### FIN-F04: Tra cứu danh sách Hóa đơn
- **Endpoint**: `GET /api/finance/invoices`
- **Query Params**: `student_id`, `status`, `page`, `page_size`
- **Quyền**: `ADMIN`, `STAFF`, `STUDENT`

### FIN-F04: Chi tiết Hóa đơn, Khoản mục & Lịch trả góp
- **Endpoint**: `GET /api/finance/invoices/{id}`
- **Quyền**: `ADMIN`, `STAFF`, `STUDENT`

---

## 3. Thu tiền & Gạch nợ VietQR (PAY)

### FIN-F05: Ghi nhận thu tiền mặt tại quầy (CASH)
- **Endpoint**: `POST /api/finance/payments/cash`
- **Quyền**: `ADMIN`, `STAFF` (Kế toán)
- **Request Body**:
  ```json
  {
    "invoice_id": 1,
    "installment_id": 1,
    "amount": 3000000,
    "notes": "Học viên đóng đợt 1 tại quầy CS1"
  }
  ```
- **Response**: `201 Created`

### FIN-F06: Lấy thông tin mã VietQR động
- **Endpoint**: `GET /api/finance/invoices/{id}/vietqr`
- **Quyền**: `STUDENT`, `STAFF`
- **Response**: Chuỗi VietQR và hình ảnh QR động gắn kèm `payment_ref_code`.

### FIN-F06: Webhook Ngân hàng nhận thanh toán VietQR
- **Endpoint**: `POST /api/finance/webhooks/vietqr`
- **Quyền**: Webhook Signature / Cổng ngân hàng
- **Request Body**:
  ```json
  {
    "bank_transaction_id": "VIB-20261106-998231",
    "amount": 3000000,
    "content": "ECPAY INV1 INS1",
    "qr_reference_code": "INS-202611-001"
  }
  ```
- **Response**: `200 OK` (Xử lý Idempotency, không ghi nhận trùng).

---

## 4. Ví Bảo lưu Học viên (WAL)

### FIN-F07: Tra cứu số dư ví bảo lưu
- **Endpoint**: `GET /api/finance/wallets/{student_id}`
- **Quyền**: `ADMIN`, `STAFF`, `STUDENT`

### FIN-F07: Cộng tiền bảo lưu hoặc Cấn trừ học phí
- **Endpoint**: `POST /api/finance/wallets/{student_id}/reserve`
- **Quyền**: `ADMIN`, `STAFF` (Kế toán)
- **Request Body**:
  ```json
  {
    "tx_type": "RESERVATION_CREDIT",
    "amount": 2500000,
    "related_invoice_id": 1,
    "reason": "Học viên bảo lưu khóa học do việc đột xuất"
  }
  ```

---

## 5. Quy trình Hoàn tiền 3 cấp (REF)

### FIN-F08 (Cấp 1): Tư vấn viên khởi tạo yêu cầu hoàn tiền
- **Endpoint**: `POST /api/finance/refunds`
- **Quyền**: `STAFF` (Tư vấn viên)
- **Request Body**:
  ```json
  {
    "invoice_id": 1,
    "class_id": 10,
    "reason": "Chuyển nơi công tác, rút hồ sơ",
    "bank_name": "Vietcombank",
    "bank_account_no": "0071000123456",
    "bank_account_holder": "NGUYEN VAN A"
  }
  ```

### FIN-F08 (Cấp 2): Kế toán xác minh công nợ & buổi học
- **Endpoint**: `PUT /api/finance/refunds/{id}/verify`
- **Quyền**: `STAFF` (Kế toán)
- **Request Body**:
  ```json
  {
    "accountant_note": "Đã kiểm tra số buổi điểm danh (2/24 buổi), đủ điều kiện hoàn 80% sau trừ phí giáo trình"
  }
  ```

### FIN-F08 (Cấp 3): Giám đốc phê duyệt
- **Endpoint**: `PUT /api/finance/refunds/{id}/approve`
- **Quyền**: `ADMIN` (Giám đốc)
- **Request Body**:
  ```json
  {
    "is_approved": true,
    "approved_refund_amount": 3500000,
    "director_note": "Đồng ý phê duyệt hoàn tiền theo chính sách"
  }
  ```

### FIN-F08 (Hoàn tất): Kế toán ghi nhận giải ngân
- **Endpoint**: `PUT /api/finance/refunds/{id}/complete`
- **Quyền**: `STAFF` (Kế toán)
- **Request Body**:
  ```json
  {
    "transaction_ref": "UNC-VCB-778899"
  }
  ```

---

## 6. Báo cáo Thông minh BI (BI)

### FIN-F09: Báo cáo Doanh thu Thực thu Theo Tháng (Cash-basis)
- **Endpoint**: `GET /api/finance/bi/cash-revenue`
- **Quyền**: `ADMIN`, `STAFF`

### FIN-F09: Báo cáo Hiệu quả Chiến dịch Voucher
- **Endpoint**: `GET /api/finance/bi/voucher-campaigns`
- **Quyền**: `ADMIN`, `STAFF`

### FIN-F09: Báo cáo Tổng hợp Quỹ Bảo lưu & Tỷ lệ Hoàn phí
- **Endpoint**: `GET /api/finance/bi/refund-summary`
- **Quyền**: `ADMIN`, `STAFF`
