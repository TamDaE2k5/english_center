# Admission Student retry — 10/11/2026

**PARTIAL**: phần Admission đã triển khai và kiểm chứng; chưa kiểm chứng Student thật. Chi tiết thiết kế, migration, công tắc vận hành, query REVIEW/EXHAUSTED và giới hạn ở services/admission/docs/student-sync-retry.md.

Đầu lượt 86 file (41 modified,45 untracked), nhánh feature/admission. Bản sao đầu lượt tại C:/Users/Admin/admission-work/qa-admission-retry-20261110; không mất file cũ. Không sửa code Student/Finance/Learning, không làm BI/E2E, không commit/push/merge. Code/migration cascade và tiền mặt giữ nguyên; PG test được bổ sung, assertion cũ không bị nới lỏng.

Đã thay helper HTTP inline bằng enqueue cùng transaction; worker có giới hạn 5 lần, backoff, lease/fencing, recovery sau crash và trạng thái đối soát. Idempotency theo user_id đã có trong hợp đồng; không tự thêm endpoint/header Student. Dispatcher mặc định tắt do endpoint thực tế chưa có. Timeout mơ hồ không tự replay khi chưa xác minh idempotency.

Test guard trước sửa fail (red.log). Kết quả cuối verified-legacy.log: **153 tests, 0 failures/errors/skipped**, JDK21/PostgreSQL16.15; trong đó **40 PG tests**, gồm 9 retry mới. **15 invariants,7 Node regressions pass**. Migration đã chạy và chạy lặp thành công. Test restart là instance worker mới đọc DB + lease expired, chưa restart JVM thật. Student client được mock trong PG, helper/outbox thực; client contract test dùng HTTP mock server. Không chứng nhận tạo hồ sơ Student thật.

Các lượt trung gian có lỗi fixture khi stub helper MANDATORY và lỗi class loading lúc build chồng nhau. Đã chuyển fault injection xuống repository, chạy lại tuần tự thành công, giữ nguyên assertion rollback; không sửa CSV/Export để né lỗi. Log java-final.log là lượt trung gian, verified-legacy.log là kết quả cuối.

## Các file thêm/sửa trong lượt này

- `services/admission/backend/src/main/java/com/englishcenter/admission/client/StudentServiceClient.java`
- `services/admission/backend/src/main/java/com/englishcenter/admission/helper/StudentServiceSyncHelper.java`
- `services/admission/backend/src/main/resources/application.properties`
- `services/admission/docs/README.md`
- `docs/admission-bug-report.md`
- `services/admission/backend/src/main/java/com/englishcenter/admission/repository/StudentSyncOutbox.java`
- `services/admission/backend/src/main/java/com/englishcenter/admission/service/StudentSyncRetryWorker.java`
- `services/admission/backend/src/main/resources/db/admission-student-sync.sql`
- `services/admission/backend/src/test/java/com/englishcenter/admission/service/AdmissionPostgresTest.java`
- `services/admission/backend/src/test/java/com/englishcenter/admission/service/StudentSyncGuardTest.java`
- `services/admission/docs/admission-quality-progress.md`
- `services/admission/docs/student-sync-retry.md`
- `services/admission/docs/testing-v1.md`
- `docs/tasks/completed/admission-student-retry-2026-11-10.md` (báo cáo này)
