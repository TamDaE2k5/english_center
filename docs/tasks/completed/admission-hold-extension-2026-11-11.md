# Admission H06 — Gia hạn giữ chỗ — 11/11/2026

Kết quả: guard theo FR-ENR-05 và chống gửi lặp đã triển khai/kiểm chứng; **PARTIAL về chính sách tổng thể**, chưa tự quyết định override hết hạn, số lần/tổng giờ tối đa hoặc đổi thời lượng đã có.

Repository: `C:/Users/Admin/admission-work/english_center`, nhánh `feature/admission`, HEAD giữ `cbf5be7c2e956397cffe4e962a12c2acce197953`. Đầu lượt giữ17 file H01 chưa commit; snapshot/hash tại `C:/Users/Admin/admission-work/qa-admission-hold-20261111/before/`. Không reset/clean/discard/stash hoặc commit/push/merge. Không sửa Finance/Student/Learning, không E2E toàn hệ thống.

## Thay đổi

- Chỉ PENDING còn deadline > now được gia hạn; NULL/quá hạn/trạng thái đóng/lớp CANCELLED bị từ chối sau class lock + row refresh. Giữ slot của chính đơn khi lớp vừa đầy, chặn dữ liệu occupied vượt max.
- Precondition **bắt buộc** expected_hold_expires_at lấy từ GET response; dưới khóa, chỉ một yêu cầu cùng snapshot được tăng hạn. Gửi lặp/stale409 ADM_HOLD_CONFLICT; payload cũ thiếu snapshot400. Client/Admission phải nâng cùng bản sửa.
- Guard actor STAFF/ADMIN ngay ở service, ngoài guard HTTP. Lưu người/lý do/giờ/notes có timestamp; audit EXTENDED cùng transaction, REJECTED sau rollback. Lỗi audit không được để lại deadline đã tăng.
- Expiry dùng <=now, nhất quán với slot/deadline guard. PostgreSQL READ COMMITTED row recheck chống ghi EXPIRED đè deadline mới sau khi chờ transaction gia hạn.
- Frontend chỉ hiện/gọi gia hạn khi đủ quyền và snapshot có deadline/thời gian còn lại; giữ snapshot qua retry, disable đang gửi, không báo thành công khi API null/false.
- Migration mới chỉ thêm bảng audit Admission, không sửa dữ liệu cũ; phải áp dụng trước chạy code mới vì ddl-auto=none.

## File tác động trong lượt H06 (22)

Đường dẫn tính từ repository nêu trên; nhiều file đã có thay đổi H01 trước lượt này và được chỉnh tiếp có giới hạn:

1. `services/admission/backend/src/main/java/com/englishcenter/admission/common/exception/ErrorCode.java`
2. `services/admission/backend/src/main/java/com/englishcenter/admission/dto/enrollment/ExtendHoldRequest.java`
3. `services/admission/backend/src/main/java/com/englishcenter/admission/job/HoldSlotExpirationJob.java`
4. `services/admission/backend/src/main/java/com/englishcenter/admission/repository/EnrollmentRepository.java`
5. **Mới:** `services/admission/backend/src/main/java/com/englishcenter/admission/repository/HoldExtensionAuditRepository.java`
6. `services/admission/backend/src/main/java/com/englishcenter/admission/service/impl/EnrollmentServiceImpl.java`
7. **Mới:** `services/admission/backend/src/main/resources/db/admission-hold-extension-audit.sql`
8. `services/admission/backend/src/test/java/com/englishcenter/admission/service/AdmissionPostgresTest.java`
9. `services/admission/backend/src/test/java/com/englishcenter/admission/service/AdmissionRegressionTest.java`
10. `services/admission/backend/src/test/java/com/englishcenter/admission/service/EnrollmentServiceTest.java`
11. `services/admission/backend/src/test/java/com/englishcenter/admission/service/EnrollmentV1Test.java`
12. `services/admission/frontend/enrollments.html`
13. `services/admission/frontend/tests/regression.cjs`
14. `services/admission/frontend/tests/browser.cjs`
15. **Mới:** `services/admission/docs/hold-extension-qa.md`
16. `services/admission/docs/admission-quality-progress.md`
17. `services/admission/docs/admission-schema-spec.md`
18. `services/admission/docs/testing-v1.md`
19. `services/admission/docs/README.md`
20. `services/admission/tasks/API_CATALOG.md`
21. `docs/admission-bug-report.md`
22. **Mới:** `docs/tasks/completed/admission-hold-extension-2026-11-11.md`

## Kiểm thử và bằng chứng

Log tại `C:/Users/Admin/admission-work/qa-admission-hold-20261111/`:

| Kiểm tra | Kết quả | Log |
|---|---|---|
| PG trước sửa: quá hạn/job chưa chạy, NULL, lớp hủy, lặp, service không actor |5/5 fail đúng lỗi, không error | red.log |
| Frontend trước sửa |2 tests mới fail,10 cũ pass | frontend-red.log |
| Toàn bộ Java JDK21.0.13/Maven3.9.9 |**200/200 pass,87 PostgreSQL16.15,0 skip**, gồm payload hợp lệ để xác minh đúng AUTH_UNAUTHORIZED/AUTH_FORBIDDEN ở service | java-final-guard.log (BUILD SUCCESS11:26:54); java-final.log trước đó cũng200 pass |
| Invariants |15/15 pass | invariants.log |
| Node |12/12 pass | frontend-node.log |
| Chrome headless/API fixtures |7 trang,BI STAFF/ADMIN/guest,CASH và hold snapshot+JWT pass | frontend-chrome.log |
| Migration thêm audit, chạy lại | Không xóa dữ liệu; CREATE TABLE IF NOT EXISTS | migration.log; migration-replay.log |
| git diff --check | Pass | kiểm tra trực tiếp cuối lượt |

15 PG tests mới bao phủ trạng thái/hạn/snapshot, quyền và validation, actor/audit, hai lần chủ ý với snapshot mới, capacity own slot, rollback khi audit lỗi, hai Staff cùng snapshot, hai thứ tự cạnh tranh expiry có barrier/row lock thật, cutoff đúng deadline, cạnh tranh lấy slot cuối và approval/cancel/cascade/payment. H01/H02/H03, cash, retry Student và BI đều thuộc regression suite cuối.

`java-first.log` có lỗi generic assertion trong test mới, đã sửa kiểu Integer. `java-verified.log` 199 tests có2 lỗi fixture/kỳ vọng test mới: thiếu class mock và cascade hợp lệ chủ động đặt deadline về lúc hủy. Đã bổ sung fixture/kiểm tra deadline hủy theo khoảng transaction cùng assertion audit/status/slot. Không sửa code cascade hoặc làm yếu guard. Unit test cũ về xin lại slot quá hạn chuyển kỳ vọng ADM_CLASS_FULL sang ADM_HOLD_EXPIRED theo FR, giữ never-save và thêm never-count-slot. Không coi các lượt fail là pass.

Khối callback H01 được đối chiếu với snapshot đầu lượt, không đổi nội dung; H02/H03 ClassService/RoomService/ClassRepository/RoomRepository không thay đổi so checkpoint. Các file H01 riêng (DTO/entity/repository/migration/báo cáo) được giữ nguyên. Constructor/test setup có thêm dependency audit gia hạn.

Cuối lượt working tree có **28 file:21 modified,7 untracked** (bao gồm H01); không có file baseline nào mất.6 file H01 riêng giữ nguyên SHA-256;11 file H01 còn lại được chỉnh tiếp trong phạm vi service/test/docs đã nêu, snapshot gốc vẫn còn ngoài repo. Không có file stage hoặc commit mới.

Proof độc lập Admission: Spring Boot HTTP/security/JPA/transactions/PostgreSQL và SMTP loopback thật. Finance/Student clients mock; Chrome dùng fixtures; invariants model/static/SQLite. Test expiry gọi service/SQL thật, cron dispatcher tắt trong fixture, không chứng nhận cron5 phút vận hành hoặc liên service thật.

## Cần quyết định/việc còn lại

- Chốt liệu có override hồi sinh đơn quá hạn/EXPIRED không; nếu có phải phối hợp slot và chu kỳ invoice/cash/receipt H01. Hiện không cho phép.
- Thống nhất24/48 với1–72 giờ, cap số lần và tổng thời gian nếu cần. Không tự áp đặt trong lượt này.
- Policy deadline NULL legacy, timezone/clock liên service; xác nhận cron vận hành và cập nhật mọi client gửi snapshot cùng migration trước deploy.
- Không sửa các nhóm audit khác hoặc thay kết luận V1. Chi tiết matrix: [hold-extension-qa.md](../../../services/admission/docs/hold-extension-qa.md). Dừng để người phụ trách review.
