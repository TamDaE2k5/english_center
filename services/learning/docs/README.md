
# Tài liệu Phân hệ Quản lý Học tập & Bảng điểm (`learning-service`)

- **Port nội bộ**: `8083` (Gateway route: `/api/learning/*`)
- **Database Schema**: `learning` (2 bảng, 2 views báo cáo, 22 cột, 2 triggers)

## Danh mục Tài liệu Chính thức

1. [**`learning-schema-spec.md`**](learning-schema-spec.md) — Đặc tả CSDL, View báo cáo & Quy tắc API liên service (sinh tự động từ PostgreSQL 16 `scripts/init-db.sql`).
2. [**`learning-ba-bi-spec.md`**](learning-ba-bi-spec.md) — Đặc tả Nghiệp vụ (BA) & Báo cáo Học lực (BI): Kho học liệu số, Sổ điểm trọng số và Tỷ lệ Pass/Fail.
3. [**`learning-tech-spec.md`**](learning-tech-spec.md) — Đặc tả Kỹ thuật & Thiết kế REST API (`/api/learning/...`), mã lỗi chuẩn và quy tắc gọi liên service.
4. [**`learning-uml-spec.md`**](learning-uml-spec.md) — Toàn bộ sơ đồ UML của phân hệ Learning (Use Case, Sequence, Activity, ERD & Views).
5. [**`tong_hop_learning_tinh_nang_va_schema.md`**](tong_hop_learning_tinh_nang_va_schema.md) — Bản tổng hợp tính năng nghiệp vụ, công thức điểm trọng số và thiết kế schema.
