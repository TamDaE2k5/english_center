# Tài liệu Phân hệ Quản lý Tài chính & Học phí (`finance-service`)

- **Port nội bộ**: `8084` (Gateway route: `/api/finance/*`, Frontend: `/finance/*`)
- **Database Schema**: `finance` (9 bảng nghiệp vụ, 2 BI Views, sequences & triggers)

## Danh mục Tài liệu Chính thức
1. [**`finance-schema-spec.md`**](finance-schema-spec.md) — Đặc tả CSDL, Từ điển dữ liệu 9 bảng, 2 BI Views & Quy tắc cô lập Schema (`scripts/init-db.sql`).
2. [**`finance-ba-bi-spec.md`**](finance-ba-bi-spec.md) — Đặc tả Yêu cầu Nghiệp vụ (BA), Quy tắc bất biến (`INV-FIN-01` -> `INV-FIN-07`) & Báo cáo Thông minh (BI).
3. [**`finance-tech-spec.md`**](finance-tech-spec.md) — Đặc tả Kỹ thuật & Kiến trúc phần mềm (FastAPI, Stateless JWT, `X-Internal-Secret`, Khóa bi quan `SELECT FOR UPDATE`, Idempotency).
4. [**`finance-uml-spec.md`**](finance-uml-spec.md) — Đặc tả Sơ đồ UML (Use Case, Sequence Diagram VietQR/Admission, Sequence Hoàn tiền 3 cấp, State Machine, ERD).
5. [**`tong_hop_finance_tinh_nang_va_schema_ver2.md`**](tong_hop_finance_tinh_nang_va_schema_ver2.md) — Bản tổng hợp tính năng và thiết kế schema.
6. [**`../tasks/README.md`**](../tasks/README.md) — Kế hoạch phân kỳ 8 Phases, Checklist 24 Tasks (`CHECKLIST.md`) và Danh mục API Endpoints (`API_CATALOG.md`).
7. [**`finance-quality-progress.md`**](finance-quality-progress.md) — Tiến độ chất lượng QA, hợp đồng tích hợp liên phân hệ với Admission/Student và kết quả kiểm thử tự động.
