# Admission: yêu cầu xác nhận tiền mặt — 10/11/2026

**PARTIAL.** Phần ghi nhận yêu cầu/audit/frontend Admission đã triển khai và kiểm thử; chưa có gửi yêu cầu hoặc xác nhận khoản CASH tại Finance thật. Chi tiết contract, API, migration và điều kiện DONE: `services/admission/docs/cash-confirmation.md`.

## Bảo toàn và phạm vi

Nhánh `feature/admission`; đầu lượt có 76 file thay đổi (38 modified, 38 untracked). Đã sao chép các file đó vào `C:\Users\Admin\admission-work\qa-admission-cash-20261110`, lưu git-status-before.txt; không mất file cũ. Code ClassServiceImpl, migration admission-class-cancellation.sql và ClassCancellationTest trùng byte với bản sao đầu lượt. Các test PostgreSQL cascade cũ được giữ nguyên, bổ sung ca tiền mặt trong cùng test class. Không reset/clean/discard/xóa dữ liệu hoặc commit/push/merge; không sửa Finance, Student, Learning hoặc shared. Không làm retry Student hoặc BI.

## Đã làm

- Đối chiếu BA FR-ENR-07/DEC-04 và FIN-F05: STAFF/ADMIN gửi yêu cầu, không tự đánh dấu đã thu hoặc duyệt khi chưa có xác nhận Finance.
- POST/PUT approve-manual trả 202 với yêu cầu NEEDS_FINANCE_CONFIRMATION. Đơn còn PENDING, không gọi Student sync. Lưu invoice_code, chứng từ, số tiền, người/thời điểm gửi và note ở Admission.
- Guard quyền, phương thức CASH, validation, hóa đơn/deadline/sĩ số/trạng thái lớp và đơn. Không hồi sinh CANCELLED hoặc CANCELLED_REFUND_PENDING, không duyệt PENDING quá hạn trước scheduler.
- Khóa lớp rồi đơn, UNIQUE và replay/conflict; transaction nguyên tử với audit. Audit từ chối REQUIRES_NEW sau rollback; audit lỗi không được báo thành công.
- Frontend CASH, thông báo chờ Finance/chưa gửi tự động, số tiền dạng chuỗi, trạng thái sau reload và chặn tạo mới khi đã có yêu cầu; giữ nhãn chờ hoàn phí.

## Kiểm thử

Test tái hiện trước sửa: PostgreSQL xác nhận approve-manual làm PENDING thành APPROVED khi chưa có Finance; gửi cùng chứng từ cho hai đơn cũng thành công. `red.log`: 8 test, 6 failures và 1 error (bảng audit chưa tồn tại), 1 pass; lỗi nghiệp vụ không bị nhầm với lỗi môi trường. Lượt đầu before-real.log lỗi fixture dùng sai tên cột; đã sửa fixture theo schema trước chạy red.log. Hai test Node mới fail trước sửa (frontend-before.log).

Kết quả cuối, JDK 21.0.13/Maven 3.9.9 và PostgreSQL 16.15 portable thật tại DB admission_qa_v1/cổng 55432:

| Bộ kiểm tra | Kết quả |
|---|---|
| Maven toàn bộ Java | **143 tests, 0 failures/errors/skipped** |
| PostgreSQL trong bộ Java | **31/31 pass** (19 cũ + 12 tiền mặt mới) |
| Invariants Python | **15/15 pass** |
| Node frontend regression | **7/7 pass** (5 cũ + 2 mới) |
| Chrome headless | **7 trang + guest pass**, thêm form CASH/JWT/202/chờ Finance với API fixtures |
| Migration mới | Tạo bảng/index thành công; chạy lặp thành công, giữ dữ liệu |
| Git diff check | Pass |

12 test PG mới kiểm tra: không duyệt trước Finance, replay/conflict/audit, quyền/validation, đơn kết thúc/quá hạn, đồng thời cùng đơn, chứng từ giữa hai lớp, tranh chấp cascade, callback có/thiếu secret và replay, audit lỗi rollback, null deadline/lớp đầy, PUT/alias/số tiền chính xác, tranh chấp callback. Các test cũ giữ chỗ, cascade, email/SMTP/audit và webhook vẫn pass.

Unit manual approval trước đây mong APPROVED ngay được đổi theo BA mới: PENDING và không gọi sync; kiểm tra ủy quyền đúng service. Đây là thay đổi tiêu chí nghiệp vụ có chủ đích, không giữ assertion chứng nhận hành vi lỗi. Không nới lỏng invariant/cascade hoặc xóa test cũ. Hai fixture tạo EnrollmentServiceImpl thủ công chỉ thêm mock dependency mới.

Log nằm ngoài repo tại `C:\Users\Admin\admission-work\qa-admission-cash-20261110`: red.log, java-first.log (139 tests pass), java-final.log (**143 pass**), invariants.log, frontend-before.log, frontend-final.log, browser.log, migration-replay.log. Java dùng JAVA_HOME JDK 21, ADMISSION_PG_TEST=true và ADMISSION_QA_JDBC_URL trỏ DB QA riêng; password lấy từ file QA ngoài repo. Finance/Student integration ports vẫn mock, không chạy E2E toàn hệ thống.

## File thêm/sửa trong lượt này (24)

Đường dẫn tính từ repository root, không phải toàn bộ diff tích lũy các lượt trước:

1. `services/admission/backend/src/main/java/com/englishcenter/admission/common/exception/ErrorCode.java`
2. `services/admission/backend/src/main/java/com/englishcenter/admission/controller/EnrollmentController.java`
3. `services/admission/backend/src/main/java/com/englishcenter/admission/dto/enrollment/EnrollmentResponse.java`
4. `services/admission/backend/src/main/java/com/englishcenter/admission/dto/enrollment/ManualApproveRequest.java`
5. `services/admission/backend/src/main/java/com/englishcenter/admission/dto/enrollment/CashConfirmationResponse.java` (mới)
6. `services/admission/backend/src/main/java/com/englishcenter/admission/repository/CashConfirmationRepository.java` (mới)
7. `services/admission/backend/src/main/java/com/englishcenter/admission/service/CashConfirmationService.java` (mới)
8. `services/admission/backend/src/main/java/com/englishcenter/admission/service/CashConfirmationTransactions.java` (mới)
9. `services/admission/backend/src/main/java/com/englishcenter/admission/service/EnrollmentService.java`
10. `services/admission/backend/src/main/java/com/englishcenter/admission/service/impl/EnrollmentServiceImpl.java`
11. `services/admission/backend/src/main/resources/db/admission-cash-confirmation.sql` (mới)
12. `services/admission/backend/src/test/java/com/englishcenter/admission/service/AdmissionPostgresTest.java`
13. `services/admission/backend/src/test/java/com/englishcenter/admission/service/EnrollmentServiceTest.java`
14. `services/admission/backend/src/test/java/com/englishcenter/admission/service/AdmissionRegressionTest.java`
15. `services/admission/backend/src/test/java/com/englishcenter/admission/service/EnrollmentV1Test.java`
16. `services/admission/frontend/enrollments.html`
17. `services/admission/frontend/tests/regression.cjs`
18. `services/admission/frontend/tests/browser.cjs`
19. `services/admission/docs/README.md`
20. `services/admission/docs/cash-confirmation.md` (mới)
21. `services/admission/docs/admission-quality-progress.md`
22. `services/admission/docs/testing-v1.md`
23. `docs/admission-bug-report.md`
24. File báo cáo hoàn thành này (mới).

## Còn tồn tại/phụ thuộc Finance

Chưa có POST payments/cash thực thi đúng catalog, mapping invoice_code -> invoice_id và hợp đồng chuyển yêu cầu nội bộ an toàn/idempotent. Không gọi endpoint payments generic để giả hoàn tất. Chưa có transport/acknowledgment hoặc worker gửi tự động; chưa ghi nhận khoản CASH, in phiếu thu hoặc xác nhận số tiền/dư nợ từ Finance thật.

Callback cũ có secret/event id và guard Admission nhưng chưa chứng minh phương thức CASH/chứng từ, cũng chưa đối soát đủ số tiền/dư nợ. Vì vậy không tự đặt MANUAL_CASH dựa trên yêu cầu còn chờ; callback vẫn dùng ONLINE_PAYMENT legacy. PARTIAL, tiền đã nhận nhưng callback chưa về, thời điểm thu tiền thực tế và giải ngân/hoàn tiền cần Finance xác minh. Request bị framework chặn trước service không có audit tiền mặt; audit từ chối nghiệp vụ trong service đã được kiểm chứng.

Migration mới bắt buộc áp dụng vào DB đích trước khi dùng tính năng/đọc danh sách đã bổ sung trạng thái yêu cầu. Kết luận riêng hạng mục **PARTIAL**, không tuyên bố Finance thật hoặc Admission V1 toàn bộ đã nghiệm thu. Dừng sau hoàn thành kiểm thử để người dùng xem kết quả.
