# Tài liệu Phân hệ Tuyển sinh, Khảo thí & Định danh (`admission-service`)

- **Port nội bộ**: `8081` (Gateway route: `/auth/*`, `/api/admission/*`)
- **Database Schema**: `admission` (14 bảng, 167 cột, 19 triggers)

## Danh mục Tài liệu Chính thức
1. [**`admission-schema-spec.md`**](admission-schema-spec.md) — Đặc tả CSDL & Quy tắc API liên service (sinh tự động từ PostgreSQL 16 `scripts/init-db.sql`).
2. [**`admission-ba-bi-spec.md`**](admission-ba-bi-spec.md) — Đặc tả Yêu cầu Nghiệp vụ (BA) & Báo cáo Thông minh (BI) (bản chốt v3.6).
3. [**`admission-tech-spec.md`**](admission-tech-spec.md) — Đặc tả Kỹ thuật & Kiến trúc phần mềm.
4. [**`admission-uml-spec.md`**](admission-uml-spec.md) — Đặc tả Sơ đồ UML (Use Case, Activity, Sequence, State, Class, ERD).
5. [**`tong_hop_admission_tinh_nang_va_schema.md`**](tong_hop_admission_tinh_nang_va_schema.md) — Bản tổng hợp tính năng và thiết kế schema.
6. [**`../tasks/README.md`**](../tasks/README.md) — Kế hoạch phân kỳ, Checklist và Danh mục 55 API Endpoints (`API_CATALOG.md`).
