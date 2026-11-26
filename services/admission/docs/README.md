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
7. [**`admission-quality-progress.md`**](admission-quality-progress.md) — Kết quả QA mới nhất và đối chiếu 30 FR của riêng Admission.
8. [**`account-email-setup.md`**](account-email-setup.md) — Luồng tài khoản khách, SMTP và migration audit bắt buộc.
9. [**`cash-confirmation.md`**](cash-confirmation.md) — Yêu cầu xác nhận tiền mặt, audit, migration và phụ thuộc Finance (PARTIAL).

10. [**`student-sync-retry.md`**](student-sync-retry.md) — Outbox/retry Student, migration và giới hạn tích hợp thật (PARTIAL).

11. [**`bi-kpi-qa.md`**](bi-kpi-qa.md) — Matrix KPI BA/BI, công thức/nguồn, API/dashboard, QA và phụ thuộc Finance.

12. [**`room-booking-qa.md`**](room-booking-qa.md) — H02/H03 capacity/concurrent booking, khóa transaction và bằng chứng PostgreSQL.

13. [**`payment-callback-qa.md`**](payment-callback-qa.md) — H01 evidence/replay/journal/cycle guard, migration và phụ thuộc Finance (PARTIAL).

14. [**`hold-extension-qa.md`**](hold-extension-qa.md) — H06 gia hạn còn hạn, snapshot chống lặp, audit/migration, concurrency với expiry/callback và chính sách chưa chốt.
