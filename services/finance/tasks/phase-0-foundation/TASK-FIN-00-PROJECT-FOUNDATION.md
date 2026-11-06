# TASK-FIN-00: Thiết lập Nền tảng FastAPI, Pydantic v2 snake_case, Global Exception Handler & Middleware JWT

- **Mã nhiệm vụ**: `TASK-FIN-00`
- **Phân kỳ**: `Phase 0: Nền móng & Hạ tầng (INF)`
- **Độ ưu tiên**: `P0 (Bắt buộc)`
- **Yêu cầu chức năng liên quan**: Toàn bộ hệ thống Finance (FIN-F01 đến FIN-F09)
- **Quy tắc bất biến bảo vệ**: `INV-SYS-01` (Quy ước định dạng `snake_case` toàn diện)

---

## 1. Mục tiêu Nghiệp vụ & Kỹ thuật
Xây dựng cấu trúc đa tầng chuẩn mực cho Finance Service trên nền Python 3.11 + FastAPI:
1. Cấu hình Pydantic v2 với `model_config = ConfigDict(alias_generator=to_snake, populate_by_name=True)` bảo đảm 100% Request và Response sử dụng định dạng `snake_case`.
2. Middleware kiểm tra và giải mã Stateless JWT (HS256) từ request header, trích xuất `user_id`, `role`, `email`.
3. Bộ bắt ngoại lệ tập trung xử lý các lỗi nghiệp vụ tài chính, validation và database, trả về cấu trúc response chuẩn, tuyệt đối không để lộ stack trace.

---

## 2. Phạm vi File Tác động
- `services/finance/backend/core/config.py`
- `services/finance/backend/core/security.py`
- `services/finance/backend/core/exceptions.py`
- `services/finance/backend/core/response.py`
- `services/finance/backend/main.py`

---

## 3. Đặc tả Kỹ thuật Chi tiết
- **Chuẩn phản hồi thành công**:
  ```json
  {
    "success": true,
    "message": "Thực hiện thành công",
    "data": { ... }
  }
  ```
- **Chuẩn phản hồi thất bại**:
  ```json
  {
    "success": false,
    "error": {
      "code": "FIN_VOUCHER_EXPIRED",
      "message": "Mã giảm giá đã hết hạn sử dụng"
    }
  }
  ```
- **Mã lỗi Finance chuẩn**:
  - `FIN_UNAUTHORIZED` (401), `FIN_FORBIDDEN` (403)
  - `FIN_INVOICE_NOT_FOUND` (404), `FIN_VOUCHER_NOT_FOUND` (404)
  - `FIN_INVOICE_ALREADY_PAID` (400), `FIN_VOUCHER_LIMIT_EXCEEDED` (400)
  - `FIN_TRANSACTION_DUPLICATE` (409), `FIN_REFUND_LIMIT_EXCEEDED` (400)

---

## 4. Tiêu chí Nghiệm thu
- [ ] Tất cả endpoint trả về đúng cấu trúc chuẩn `{"success": true/false, ...}`.
- [ ] Request payload camelCase hoặc snake_case đều được chuyển hóa về snake_case.
- [ ] Invariant test `INV-SYS-01` đạt kết quả `OK`.
