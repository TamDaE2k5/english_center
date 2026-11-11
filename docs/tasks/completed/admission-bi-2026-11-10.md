# Dashboard BI Admission — 10/11/2026

**PARTIAL** toàn hạng mục; chưa tổng nghiệm thu Admission V1. Matrix từng KPI/công thức/nguồn/điều kiện/trạng thái DONE-PARTIAL-BLOCKED tại `services/admission/docs/bi-kpi-qa.md`.

## Bảo toàn

Đầu lượt feature/admission có94 file chưa commit (43 modified,51 untracked). Snapshot và Git status đầu lượt tại `C:/Users/Admin/admission-work/qa-admission-bi-20261110`. Không mất file cũ; code cascade/cash/retry/email và toàn bộ migration trước giữ nguyên. Bổ sung BI tests vào AdmissionPostgresTest, không nới assertion nghiệp vụ cũ. Không reset/clean/discard/xóa dữ liệu cũ hoặc commit/push/merge; không sửa Finance/Student/Learning/shared.

## Kết quả

Đã sửa cohort test theo STARTED, distinct user conversion14 ngày, cohort đơn có mọi trạng thái và effective expiry, ranking volume, weighted occupancy, DAU/MAU theo lịch Việt Nam, failed rate/trends/role và signal6fail-reset. API có alias BA, validation và phân quyền; dashboard dùng dữ liệu API với loading/error/N/A/filter, chặn response cũ ghi đè, không hiển thị doanh thu giả.

Finance chưa có contract dữ liệu dư nợ/thu/hoàn nên BI-REV-01/02 null+UNAVAILABLE. Nhóm IELTS/TOEIC/Giao tiếp thiếu nguồn, chỉ có group theo level thật. Chưa có state/cycle/role history để phục dựng quá khứ; as_of client bị từ chối.14 ngày là đề xuất BA được công khai. Không tự đặt endpoint Finance, không chạy E2E toàn hệ thống.

## Kiểm thử

- Red: mẫu số0 trả0.0 thay vì null trước sửa; red.log.
- **161/161 Java tests pass bằng JDK21**, gồm **48 PostgreSQL16.15 integration tests**, 0 failures/errors/skipped. Log verified-final.log.
- **15/15 invariants**, **10/10 Node regressions**, **Chrome7 trang cùng BI STAFF/ADMIN/guest pass**; log invariants.log/frontend.log/browser-verified.log. Browser dùng API fixtures; không tuyên bố browser nối DB thật hoặc E2E hệ thống.
- Thêm8 PG BI tests cho mọi công thức/null Finance/empty, ranh giới ngày-tháng/14 ngày, duplicate-user, role/cảnh báo và HTTP/JWT/validation. Expected dataset độc lập: completion2/3=66.67%, user conversion1/1=100%, approved2/8=25%, expired2/8=25%, occupancy2/40=5%, failed1/4=25%. Fixture BI được rollback trong transaction test riêng, không xóa/reset dữ liệu có sẵn.
- 4 unit BI cũ (có assertion tiền thực thu=APPROVED×tuition và cohort sai) được viết lại theo contract mới. Không đổi invariant hoặc bỏ các regression ngoài BI. DTO cũ vẫn giữ file nhưng endpoint dùng response map mới; frontend và backend BI cần cập nhật cùng nhau.
- Lượt java-first.log lỗi fixture thiếu lịch lớp bắt buộc; đã sửa đúng schema, giữ expected. java-second.log160 pass; verified-final.log161 pass. Browser lần đầu ADMIN đổi role chưa đợi request mới; đã chờ đúng request, giữ assertion.
- `git diff --check` pass; không có migration BI mới.

## Các file sửa/thêm trong lượt này

- `services/admission/backend/src/main/java/com/englishcenter/admission/controller/BiAnalyticsController.java`
- `services/admission/backend/src/main/java/com/englishcenter/admission/service/BiAnalyticsService.java`
- `services/admission/backend/src/main/java/com/englishcenter/admission/service/impl/BiAnalyticsServiceImpl.java`
- `services/admission/backend/src/test/java/com/englishcenter/admission/service/BiAnalyticsServiceTest.java`
- `services/admission/docs/README.md`
- `services/admission/frontend/bi-dashboard.html`
- `services/admission/tasks/API_CATALOG.md`
- `docs/admission-bug-report.md`
- `services/admission/backend/src/main/java/com/englishcenter/admission/dto/bi/BiWindow.java`
- `services/admission/backend/src/main/java/com/englishcenter/admission/repository/AdmissionBiRepository.java`
- `services/admission/backend/src/test/java/com/englishcenter/admission/service/AdmissionPostgresTest.java`
- `services/admission/docs/admission-quality-progress.md`
- `services/admission/docs/bi-kpi-qa.md`
- `services/admission/docs/testing-v1.md`
- `services/admission/frontend/tests/browser.cjs`
- `services/admission/frontend/tests/regression.cjs`
- `docs/tasks/completed/admission-bi-2026-11-10.md` (báo cáo này).

Lượt cuối verified-final.log xác nhận lại 161 tests sau khi kiểm tra thứ tự ID cho các audit có timestamp trùng. Trạng thái cuối Git:103 file chưa commit (48 modified,55 untracked); lượt BI thêm/sửa17 file được liệt kê trên. Không commit/push/merge.
