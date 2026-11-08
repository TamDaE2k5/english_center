# Tài liệu Phân hệ Quản lý Tài chính & Học phí (`finance-service`)

- **Port nội bộ**: `8084` (Gateway route: `/api/finance/*`)
- **Database Schema**: `finance` (9 bảng, 2 BI views, 126 cột, 2 sequences, 12 triggers)

## Danh mục Tài liệu Chính thức
1. [**`finance-schema-spec.md`**](finance-schema-spec.md) — Đặc tả CSDL, BI Views & Quy tắc API liên service (sinh tự động từ PostgreSQL 16 `scripts/init-db.sql`).
2. [**`finance-ba-bi-spec.md`**](finance-ba-bi-spec.md) — Đặc tả Yêu cầu Nghiệp vụ (BA) & Báo cáo Thông minh (BI).
3. [**`finance-tech-spec.md`**](finance-tech-spec.md) — Đặc tả Kỹ thuật, Giao dịch Đồng thời & API Contract.
4. [**`finance-uml-spec.md`**](finance-uml-spec.md) — Đặc tả Sơ đồ UML (Use Case, Activity, Sequence, State, Class, ERD).
5. [**`tong_hop_finance_tinh_nang_va_schema.md`**](tong_hop_finance_tinh_nang_va_schema.md) — Bản tổng hợp tính năng và thiết kế schema.
6. [**`../tasks/README.md`**](../tasks/README.md) — Kế hoạch phân kỳ, Checklist và Danh mục API Endpoints (`API_CATALOG.md`).
