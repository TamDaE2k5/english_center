# Admission: sức chứa phòng/concurrent booking — 10/11/2026

Đã sửa H02/H03 của audit cuối trong phạm vi writer Admission và mẫu ngày BA có định nghĩa. Không sửa nhóm lỗi khác/Finance/Student/Learning, không E2E hệ thống hoặc commit/push/merge. Không có migration mới hoặc frontend change.

Đầu lượt103 file (48modified,55untracked), nhánh feature/admission. Snapshot tại C:/Users/Admin/admission-work/qa-admission-room-booking-20261110. Không mất file cũ. ClassService chỉ bổ sung khóa ở create/update; phần status/cascade sau đó giữ nguyên. Enrollment/cash/retry/mail source và migration cũ giữ nguyên.

## Kết quả thực tế

- Trước sửa red.log:5 PG tests fail vì không chặn giảm capacity hoặc transaction thứ hai hoàn tất trước commit đầu. red-extra.log:1 race update fail và1 fixture PATCH lỗi (không coi là bằng chứng bug runtime).
- Sửa root cause: khóa PostgreSQL transaction advisory (8081,1) trước read/check/class row lock ở create/update class, update/status room; MAX max_students bảo vệ capacity khi sửa phòng. Không chỉ synchronized trong JVM. Giữ code conflict/mã lỗi/biên giờ-ngày.
- Fixture PATCH lỗi do callRealMethod trên findById abstract của Spring Data; sửa đọc EntityManager thật cùng transaction và giữ nguyên assertion, không giả dữ liệu capacity.
- **171 Java tests,0 failures/errors/skipped**, JDK21; gồm **58 PostgreSQL16 integration tests** (48cũ+10mới). verified.log là kết quả cuối. Giữ test legacy slot/cancel/approve/cash/retry/BI/email/audit.
- **15/15 invariants;10/10 Node frontend;Chrome7 trang+BI roles pass** với API fixtures. Không gọi Finance/Student thật hoặc E2E toàn hệ thống. git diff --check pass.

## Tradeoff và giới hạn

Khóa chung serialize cả các thao tác ghi lịch khác phòng; chưa có benchmark/stress nhiều JVM. Cần READ COMMITTED/entry point transaction hiện tại, writer cùng database cùng khóa; SQL trực tiếp hoặc phiên bản Admission cũ không được bảo vệ bằng constraint DB mới. Không auto-heal/xóa lịch cũ. CUSTOM chưa có tập ngày cụ thể, cần chốt semantics riêng; không tự đổi thành lịch giả. Chi tiết10 test/policy allassigned classes ở services/admission/docs/room-booking-qa.md. Không đổi kết luận Admission V1 tổng thể thành feature-complete.

## File sửa/thêm

- `services/admission/backend/src/main/java/com/englishcenter/admission/repository/RoomRepository.java`
- `services/admission/backend/src/main/java/com/englishcenter/admission/service/impl/ClassServiceImpl.java`
- `services/admission/backend/src/main/java/com/englishcenter/admission/service/impl/RoomServiceImpl.java`
- `services/admission/docs/README.md`
- `docs/admission-bug-report.md`
- `services/admission/backend/src/test/java/com/englishcenter/admission/service/AdmissionPostgresTest.java`
- `services/admission/docs/admission-quality-progress.md`
- `services/admission/docs/room-booking-qa.md`
- `services/admission/docs/testing-v1.md`
- `docs/tasks/completed/admission-room-booking-2026-11-10.md` (báo cáo này).
