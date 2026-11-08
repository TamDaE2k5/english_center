# Tài liệu Phân hệ Quản lý Học viên & Điểm danh (`student-service`)

- **Port nội bộ**: `8082` (Gateway route: `/api/student/*`, hỗ trợ alias `/api/v1/*`)
- **Database Schema**: `student` (3 bảng, 31 cột, 1 sequence, 3 triggers)

## Danh mục Tài liệu Chính thức
1. [**`student-schema-spec.md`**](student-schema-spec.md) — Đặc tả CSDL & Quy tắc API liên service (sinh tự động từ PostgreSQL 16 `scripts/init-db.sql`).
2. [**`student-ba-bi-spec.md`**](student-ba-bi-spec.md) — Đặc tả Yêu cầu Nghiệp vụ (Business Specification).
3. [**`student-tech-spec.md`**](student-tech-spec.md) — Đặc tả Kỹ thuật & API Endpoints (Technical Specification).
4. [**`student-uml-spec.md`**](student-uml-spec.md) — Đặc tả Sơ đồ UML (Use Case, Sequence, Activity, State).
