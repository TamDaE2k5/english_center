# ADR 004: Quy ước Định dạng API & Dữ liệu chuẩn snake_case

- **Trạng thái**: APPROVED
- **Ngày quyết định**: 2026-10-15
- **Người phê duyệt**: Project Lead & Team Architect

## Bối cảnh (Context)
Dự án có sự kết hợp giữa Java (thường dùng camelCase) và Python (thường dùng snake_case). Việc không đồng nhất gây ra lỗi parsing ở Frontend và cross-service HTTP calls.

## Quyết định (Decision)
- Toàn bộ JSON Keys trong Request Body, Response Data và Database Column names đều dùng **`snake_case`**.
- Spring Boot cấu hình tự động mapping qua `spring.jackson.property-naming-strategy=SNAKE_CASE`.
- FastAPI mặc định sử dụng Pydantic models với `snake_case`.

## Hệ quả (Consequences)
- Giao diện Frontend và các hàm helper trong `shared/js/api.js` hoạt động nhất quán, không phải map đổi tên trường dữ liệu.
