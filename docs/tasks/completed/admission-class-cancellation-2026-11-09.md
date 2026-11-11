# Admission: hủy lớp và cascade — 09/11/2026

Nhánh `feature/admission`. Giữ các thay đổi QA có sẵn; không commit/push/merge, không sửa Finance, Student, Learning. Chỉ xử lý hủy lớp trong đợt này.

## Kết quả

Hủy lớp OPEN chưa khai giảng và thiếu sĩ số: PENDING chuyển CANCELLED, APPROVED chuyển CANCELLED_REFUND_PENDING. Ghi yêu cầu đối soát bền vững trong schema Admission, khóa lớp và đơn trong một transaction, chống lặp và rollback nếu ghi yêu cầu lỗi. Ngăn duyệt/hủy đơn ghi đè trạng thái chờ hoàn phí khi cạnh tranh.

**Chưa hoàn tất tích hợp refund tại Finance**: thiếu invoice_id/thông tin ngân hàng và endpoint Finance thực thi; bản ghi NEEDS_FINANCE_REVIEW chưa phải yêu cầu đã được Finance tiếp nhận. Không kiểm chứng PARTIAL, PAID chưa callback, hủy hóa đơn hoặc hoàn tiền thực tế. Chi tiết tại `services/admission/docs/class-cancellation.md`.

## File sửa trong đợt này

Các đường dẫn dưới đây tính từ repository root:

- `services/admission/backend/src/main/java/com/englishcenter/admission/controller/ClassController.java`
- `services/admission/backend/src/main/java/com/englishcenter/admission/service/ClassService.java`
- `services/admission/backend/src/main/java/com/englishcenter/admission/service/impl/ClassServiceImpl.java`
- `services/admission/backend/src/main/java/com/englishcenter/admission/service/impl/EnrollmentServiceImpl.java`
- `services/admission/backend/src/main/java/com/englishcenter/admission/repository/EnrollmentRepository.java`
- `services/admission/backend/src/main/resources/db/admission-class-cancellation.sql` (mới; đã áp dụng vào PostgreSQL QA)
- `services/admission/backend/src/test/java/com/englishcenter/admission/service/ClassCancellationTest.java` (mới)
- `services/admission/backend/src/test/java/com/englishcenter/admission/service/AdmissionPostgresTest.java`
- `services/admission/backend/src/test/java/com/englishcenter/admission/service/ClassServiceTest.java`
- `services/admission/backend/src/test/java/com/englishcenter/admission/service/EnrollmentServiceTest.java`
- `services/admission/docs/class-cancellation.md` (mới)
- `services/admission/docs/admission-quality-progress.md`
- `services/admission/docs/testing-v1.md`
- `docs/admission-bug-report.md`
- File báo cáo hoàn thành này.

## Kiểm thử thực tế

- Test tái hiện cascade fail trước sửa: `ClassCancellationTest`; log ngoài repository `C:\Users\Admin\admission-work\cancel-before.log`.
- Maven test bằng JDK 21, bật PostgreSQL 16.15 thật (DB QA riêng, cổng 55432): **131 tests, 0 failures/errors/skipped**, gồm **19 PostgreSQL tests**. Log cuối `C:\Users\Admin\admission-work\cancel-verified.log`.
- Thêm 1 unit test và 5 PostgreSQL tests: cascade/replay, rollback lỗi queue, hai lệnh hủy đồng thời, hủy tranh chấp duyệt, điều kiện ngày khai giảng/sĩ số.
- `python -m unittest discover -s tests/invariants -p "test_*.py" -v`: **15/15 pass**.
- `node services/admission/frontend/tests/regression.cjs`: **5/5 pass**.
- `git diff --check`: pass. Không chạy E2E toàn hệ thống hoặc Finance thật.

Fixture mock idempotency được cập nhật để yêu cầu khóa lớp thay vì cấm khóa; assertion APPROVED vẫn giữ. Một lượt test trung gian fail do Spring dịch ngoại lệ giả lập; dùng DataAccessResourceFailureException để mô phỏng lỗi repository thực tế và chạy lại toàn bộ thành công, giữ nguyên kiểm tra rollback.
