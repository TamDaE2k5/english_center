> Bản lưu lịch sử audit ngày 10/11/2026, trước đợt sửa sức chứa/booking. H02/H03 đã được xử lý sau audit; xem [báo cáo cập nhật](../services/admission/docs/room-booking-qa.md). Đây không phải tuyên bố Admission V1 hoàn thành. Log và backup cục bộ không được đính kèm.

# Tổng rà soát riêng Admission V1 — 10/11/2026

**KẾT LUẬN: ADMISSION V1 NOT COMPLETE.**

Các luồng lõi đã có nhiều bản sửa và regression tốt, nhưng còn chức năng/luồng sử dụng chưa hoàn tất và một số khoảng trống nghiệp vụ trực tiếp thuộc Admission. Không thể đổi kết luận chỉ vì 161 tests pass. Đây không phải nghiệm thu toàn hệ thống, không yêu cầu E2E toàn hệ thống để chứng minh các thiếu sót Admission nêu dưới.

## 1. Phạm vi và bảo toàn

- Repository C:/Users/Admin/admission-work/english_center, feature/admission, HEAD84af6e91b315461b9eb6d17f964de924286ca066.
-103 file chưa commit:48 modified,55 untracked. Lượt này **không sửa bất kỳ source/test/migration/docs nào trong repository**. Không reset/clean/discard, commit/push/merge hoặc sửa Finance/Student/Learning. Báo cáo/log được lưu ngoài repository trong thư mục QA final audit.
- Manifest SHA256389 file tracked/untracked tại source-before.json; kiểm tra preservation.json xác nhận không đổi source, Git status giữ nguyên. Build/test có thể tạo output ignored và fixture trong DB QA riêng; không dùng DB vận hành.
- Đã rà BA/BI v3.6 (toàn bộ30 FR, ma trận quyền, vòng đời,9 KPI, AT01–12, DEC01–10), schema/DDL init-db, API catalog/controller, technical spec, UML use cases/state machines; đối chiếu code/frontend/test và các báo cáo QA/cash/cascade/retry/BI hiện có.
- Bảng tiến độ cũ còn mô tả ACA-04 “chưa cascade” và số125/143 tests ở các đoạn lịch sử. Không lấy những dòng cũ đó làm trạng thái hiện tại. Báo cáo này không tự sửa tài liệu nguồn theo yêu cầu chỉ kiểm tra.

## 2. Cách phân loại

DONE: hành vi của FR trong phạm vi ghi rõ có bằng chứng code/data/API/test phù hợp. Không đồng nghĩa đã kiểm thử mọi môi trường hoặc nghiệm thu liên service. PARTIAL: có một phần nhưng thiếu workflow, quy tắc hoặc bằng chứng quan trọng; ghi rõ lỗi đã thấy hay chỉ là thiếu kiểm chứng. BLOCKED: phần còn lại phụ thuộc contract/data chưa có. MISSING: chưa thấy thành phần trong repository; ở đây dùng cho các thành phần con, không gán cả FR MISSING khi backend của FR đã tồn tại.

Đường dẫn backend trong bảng tính từ `services/admission/backend/src/main/java/com/englishcenter/admission/`; tên test là file trong `services/admission/backend/src/test/java/com/englishcenter/admission/`. PG = AdmissionPostgresTest; UI = services/admission/frontend. Nguồn FR là admission-ba-bi-spec.md mục4; không tự thêm class_shifts V2, XLSX bắt buộc hoặc DAU mọi hoạt động vào scope V1.

## 3. Matrix toàn bộ30 FR

| FR | Trạng thái | Backend và API | Database, frontend và test | Việc còn lại / giới hạn |
|---|---|---|---|---|
| FR-IAM-01 | PARTIAL | AccountLifecycleService, UserServiceImpl; POST auth/register/verify-email/setup-password, enrollments | users, password_reset_tokens; account.html + PG emailSetupAndPasswordRecoveryAreSingleUse | Tạo STUDENT/email-hash/purpose chống trùng đã có. Tiếp nhận tại quầy tạo user nhưng không tự gửi invite; cần chốt DEC-01 và luồng kích hoạt tài khoản do Staff tạo. |
| FR-IAM-02 | PARTIAL | UserController, UserServiceImpl.createUser; POST users ADMIN | users, auth_audit_logs; UserServiceTest, PG creatingStaffPersistsUserAndAudit; chưa có trang nhân sự | Phone/gender/DOB không bắt buộc trong DTO dù FR yêu cầu nhập; must_change_password cho Staff/Teacher chưa được cưỡng chế. UI quản trị nhân sự MISSING. |
| FR-IAM-03 | PARTIAL | AuthController/AuthServiceImpl/JwtTokenProvider; POST login/refresh/logout | refresh_tokens; AuthServiceTest, PG login; account.js chỉ set access token/user | Backend cấp 15m/7d có bằng chứng. Frontend không dùng refresh_token; shared logout chỉ xóa local session, không gọi thu hồi backend. Backend DONE, phiên UI chưa đầy đủ. |
| FR-IAM-04 | PARTIAL | AuthServiceImpl.refreshToken + RefreshTokenRepository.findByToken PESSIMISTIC_WRITE | refresh_tokens; AuthServiceTest rotation/replay; frontend chưa nối refresh | Rotation/replay/transaction có code/test; còn thiếu PG hai refresh đồng thời và sử dụng thực ở UI. Hash refresh là khuyến nghị hardening trong BA, không tự coi nó là yêu cầu bắt buộc duy nhất làm fail V1. |
| FR-IAM-05 | DONE | UserController PATCH users/{id}/status; JwtAuthenticationFilter đọc user hiện tại | users.is_active; UserServiceTest; PG disabledUserCannotUsePreviouslyIssuedAccessToken | Khóa tức thời access JWT trong RIÊNG Admission đã kiểm chứng. Chưa chứng nhận Gateway/service khác kiểm tra trạng thái tương tự. |
| FR-IAM-06 | DONE | AccountLifecycleService + AccountMailService; forgot/reset/change-password | password_reset_tokens hash/purpose/15m; account.html/js; PG+SMTP loopback thật, expiry/replay/reset tests | Chức năng Admission có bằng chứng. SMTP nhà cung cấp/HTTPS URL và cấu hình triển khai là gate vận hành riêng; không tuyên bố thư ngoài QA đã đến hộp thư. |
| FR-IAM-07 | PARTIAL | AuthServiceImpl/UserServiceImpl/AuditLogController; auth events, GET audit-logs ADMIN | auth_audit_logs + migration audit; AuditLogServiceTest/AuthServiceTest, PG failedLoginAuditSurvivesBusinessRejection | Backend các event được triển khai/test, nhưng logout UI không gọi backend nên chưa sinh LOGOUT cho luồng sử dụng đó. UI audit chuyên biệt MISSING; cần nối workflow, test lỗi storage/trusted proxy. |
| FR-FAC-01 | PARTIAL | RoomController/RoomServiceImpl; POST/GET/PUT/PATCH rooms | rooms unique code/capacity>0; rooms.html create/list/status; RoomServiceTest/Chrome smoke | UI sửa đầy đủ chưa có. Inactive thay xóa vật lý phải ghi rõ policy. Chưa kiểm chứng HTTP/DB toàn CRUD; giảm capacity có vấn đề nêu H02. |
| FR-FAC-02 | DONE | DayPattern, ClassServiceImpl validation; tạo/sửa classes | classes.day_pattern/start_time/end_time; form classes.html presets; ConflictEngineTest/ClassServiceTest | Preset và start<end có bằng chứng. Không đánh MISSING vì chưa có class_shifts: BA nói bảng CRUD ca là đề xuất V2, phải chốt DEC-05. |
| FR-FAC-03 | PARTIAL | ClassServiceImpl.createClass/updateClass kiểm tra max_students<=capacity | classes.room_id/rooms.capacity; ClassServiceTest | Guard lúc lưu lớp có; RoomServiceImpl.updateRoom hạ capacity không tái kiểm tra lớp đang gán. Cần bảo vệ cả hai chiều và concurrency. |
| FR-ACA-01 | PARTIAL | CourseController/CourseServiceImpl; GET/POST/PUT/DELETE courses | courses + prerequisite_course_id; courses.html create/list/status/delete; CourseServiceTest | API/model có; UI sửa khóa/tiên quyết chưa đầy đủ. Cần nghiệm thu chuỗi prerequisite và policy khóa/ẩn các course đang được dùng. |
| FR-ACA-02 | PARTIAL | ClassController/ClassServiceImpl create/update/status/cancel | classes/rooms/users; classes.html tạo/list/lookup teacher; ClassServiceTest, PG Staff lookup | Form tạo và backend có. UI sửa lớp/chuyển trạng thái/hủy lớp MISSING; cạnh tranh đặt lịch chưa được bảo đảm/test. |
| FR-ACA-03 | PARTIAL | ConflictEngine + ClassRepository.findConflictingRoomClasses/TeacherClasses | classes lịch ngày/giờ/phòng/teacher; ConflictEngineTest, ClassServiceTest | Chặn xung đột đã tồn tại có unit evidence. Check-then-save không khóa tài nguyên chung cho hai create/update; chưa có PG test concurrency. H03 là rủi ro, chưa tái hiện runtime lượt này. |
| FR-ACA-04 | PARTIAL | ClassServiceImpl.cancelClass và updateClassStatus; PUT classes/{id}/cancel | classes/enrollments/class_cancellation_requests; ClassCancellationTest, PG cascade/replay/rollback/concurrency | Cascade local DONE: CANCELLED/CANCELLED_REFUND_PENDING giữ đúng. Chưa có UI hủy và gửi/ACK yêu cầu hoàn phí Finance; handoff chỉ NEEDS_FINANCE_REVIEW. |
| FR-ACA-05 | DONE | ClassServiceImpl.getClassCapacity + EnrollmentRepository countOccupiedSlots | APPROVED + PENDING deadline tương lai; GET classes/{id}/capacity, available-slots; ClassServiceTest/PG last-slot | Công thức và cạnh tranh giữ chỗ đã kiểm chứng. NULL deadline legacy cần policy; không mở rộng DONE thành load test nhiều máy. |
| FR-TST-01 | PARTIAL | TestManagementController/ServiceImpl; CRUD sets, duration_minutes | test_sets; TestManagementServiceTest; test.html chỉ danh mục/làm bài | Backend quản lý bộ đề có. UI quản trị ngân hàng đề MISSING; chưa có workflow Học vụ/Admin biên soạn hoàn chỉnh. |
| FR-TST-02 | PARTIAL | TestManagementController/ServiceImpl questions + TakeTestResponse | test_questions audio_url/passage_text/skill/weight/options; unit TestManagement/Placement; Chrome question_text | Backend trường/ngân hàng câu hỏi có. UI biên soạn câu hỏi/media MISSING; phát audio thực/CORS chưa kiểm chứng bằng chứng hiện tại. |
| FR-TST-03 | PARTIAL | PublicPlacementTestService, PlacementTestServiceImpl; POST public/start/submit | test_attempts expires_at/status; test.html countdown; PG capability/deadline, PlacementPublicApiTest | Server chặn quá hạn/mạo danh/submit lại. Frontend tính expires_at-started_at thay vì thời gian còn lại tại nhận response; auto-submit sau deadline có thể bị server từ chối. H05. |
| FR-TST-04 | DONE | PlacementTestServiceImpl.submitTest; server chấm theo đáp án/weight | test_attempts/test_submissions JSONB; test.html kết quả; PlacementTestServiceTest/AdmissionRegressionTest + PG roundtrip | Chấm một lần, tách ba kỹ năng, không nhận điểm client đã có proof. Chốt policy bài trắng/auto-finalize timeout riêng; không coi quyết định đó đã triển khai. |
| FR-TST-05 | DONE | RecommendationServiceImpl/RecommendationRuleController; score -> course + lớp còn chỗ | test_recommendation_rules/courses/classes; test.html quickEnroll(class_id); RecommendationServiceTest + Node class_id | Có bằng chứng rule matching/overlap, payload recommendation và nút dùng ID thật. UI quản trị rule không tự suy là đã có. |
| FR-ENR-01 | PARTIAL | EnrollmentController/ServiceImpl; POST enrollments STUDENT/STAFF/ADMIN | users/enrollments; enrollments.html Staff, test.html quickEnroll; EnrollmentServiceTest/PG self ownership | Tạo đa kênh và bảo vệ chính chủ có. Student chưa có UI đăng ký không qua test/tra cứu đầy đủ; mời tài khoản mới tại quầy thiếu. Chu kỳ tái đăng ký cần xử lý H01. |
| FR-ENR-02 | DONE | createEnrollment/approveEnrollment khóa class row trước count/write | classes/enrollments; tests concurrentRegistrationCannotOversellLastSlot + EnrollmentServiceTest | PG hai transaction tranh slot cuối pass; không đánh đồng với lịch phòng/teacher concurrency khác H03. |
| FR-ENR-03 | PARTIAL | createEnrollment hold=server now+48h, unique user-class; GET /enrollments/my và /enrollments/{id} | enrollments deadline/enrolled_at; EnrollmentServiceTest + PG hold; UI Staff có countdown | Core hold/API chính chủ có. Trang enrollments.html chỉ Staff/Admin: UI Student theo dõi toàn bộ đơn/hạn MISSING. Policy NULL deadline/reuse và lịch sử chưa chốt. |
| FR-ENR-04 | PARTIAL | HoldSlotExpirationJob cron5m -> expireOverdueEnrollments; count bỏ hold hết hạn ngay | enrollments + repository bulk expiry; EnrollmentV1Test/EnrollmentServiceTest; PG overdue Pending | Realtime release/guard có proof. PG integration tắt scheduler; chưa có bằng chứng cron thật chuyển DB khi không có callback. Đây là thiếu kiểm chứng, không khẳng định job hỏng. |
| FR-ENR-05 | PARTIAL | extendHold; POST enrollments/{id}/extend-hold STAFF/ADMIN | hold_extended_by_user_id/reason/expiry; modal Staff, EnrollmentServiceTest/EnrollmentV1Test | Code cho PENDING quá deadline xin lại slot nếu còn chỗ. Khác FR yêu cầu còn hiệu lực; sửa hoặc phê duyệt lại policy trước nghiệm thu. |
| FR-ENR-06 | PARTIAL | WebhookController/payment-success + InternalSecretFilter + processPaymentSuccess | processed_finance_events/enrollments; WebhookSecurityTest, EnrollmentV1Test, PG late/replay | Nguồn gửi, slot/deadline và replay đã test. Chưa đối chiếu PAID/invoice/amount/cycle/paid_at; REJECTED/refund-pending chưa có journal ngoại lệ như CANCELLED/EXPIRED. H01 là việc Admission phải làm, không chỉ chờ Finance. |
| FR-ENR-07 | BLOCKED | CashConfirmationService/Transactions; POST/PUT approve-manual ->202 | cash_confirmation_requests/audit; Staff UI, 12 PG cash cases + Node/Chrome | Local request/validation/audit DONE; không có gửi/ACK Finance CASH, thiếu invoice_id mapping/contract. Admission vẫn cần nối transport/ACK sau khi Finance chốt; không xác nhận khoản tiền thật. |
| FR-ENR-08 | PARTIAL | StudentServiceSyncHelper MANDATORY/outbox + StudentSyncRetryWorker; scheduled disabled mặc định | student_sync_outbox + enrollments flags; 9 PG retry cases/StudentSyncGuardTest/client tests | Local durability/backoff/lease/fencing/review DONE. Endpoint/internal idempotency Student thật thiếu; chưa restart JVM thật, chưa có quản trị requeue có audit. Không tuyên bố đã tạo hồ sơ ngoài mock. |
| FR-OPS-01 | PARTIAL | ExportController/ExportServiceImpl; GET exports/classes/{id}/roster | enrollments/users/classes; classes.html download JWT; ExportServiceTest/CsvExportSecurityTest/Chrome | CSV là định dạng hợp lệ của FR (không bắt buộc thêm XLSX). Nhưng cột mã học viên tự dựng STU-{user_id}, không phải mã Student: H04. Cần HTTP/PG export ownership/data acceptance. |
| FR-OPS-02 | PARTIAL | ExportController/ExportServiceImpl; GET exports/tests/{id}/results | test_submissions/test_sets/users; ExportServiceTest/CsvExportSecurityTest | CSV backend có bằng chứng; UI xuất theo bộ đề chưa có, HTTP/PG luồng export còn thiếu. Không đánh fail chỉ vì thiếu XLSX: BA cho Excel HOẶC CSV. |

Tổng phân loại30 FR theo tiêu chí trên: PARTIAL=22, DONE=7, BLOCKED=1. Không dùng tỷ lệ này như phần trăm readiness; mức độ nghiêm trọng và điều kiện hoàn tất quan trọng hơn số hàng DONE.

## 4. Matrix BA/BI bổ sung

| KPI / hạng mục | Trạng thái | Bằng chứng và phần chưa đạt |
|---|---|---|
| BI-FUN-01 | DONE cho snapshot hiện tại | AdmissionBiRepository.funnel đếm SUBMITTED/STARTED cùng cohort, PG2/3=66.67%; nhiều attempt cùng user không làm lẫn đơn vị. Cohort test toàn trung tâm vì không có course binding. |
| BI-FUN-02 | PARTIAL | distinct user + EXISTS đăng ký trong14 ngày, biConversionWindowBoundary kiểm tra đúng14 ngày và+1µs, Chrome N/A. DEC07 chưa thấy biên bản chốt trong repo; không coi14 ngày là quyết định đã duyệt. |
| BI-FUN-03 | DONE cho dữ liệu hiện hành | approved/total new current enrollment records, PG2/8=25%. Không suy thành tiền thu; không phục dựng cohort trước lần tái sử dụng enrolled_at. |
| BI-FUN-04 | DONE cho snapshot hiện tại | EXPIRED + PENDING quá deadline, PG2/8=25%, không chờ job. Cancel/refund-pending nằm mẫu số nhưng không bị gọi expired. |
| BI-REV-01 | BLOCKED | Dư nợ hóa đơn Finance chưa có contract/source. API null+UNAVAILABLE, dashboard Chưa khả dụng; không nhân tuition. |
| BI-REV-02 | BLOCKED | Thu thật trừ hoàn thật/phân bổ course-enrollment cần Finance. APPROVED/CASH request/callback marker không thay nguồn tiền. |
| BI-OCC-01 | DONE | Cùng lớp OPEN/IN_PROGRESS: tổng approved/tổng max, PG2/40=5%; loại lớp canceled100capacity; group level thật. |
| BI-USR-01 | DONE cho người đăng nhập | distinct LOGIN_SUCCESS, ngày/tháng lịch Asia/Ho_Chi_Minh; PG ranh giới31/1–1/2 và login trùng; role theo users.role hiện tại. Không phải DAU mọi hoạt động. |
| BI-SEC-01 | DONE cho tỷ lệ | LOGIN_FAILED/(SUCCESS+FAILED), không đếm refresh, PG1/4=25%; ADMIN-only. Signal>5 liên tiếp10m có test reset/same timestamp ID. |
| Top Course Volume | DONE | COUNT enrollment theo course, lọc kỳ/course/channel, tie-break code/id, limit; trạng thái hủy vẫn hiện riêng, không xếp hạng theo tiền giả. |
| Nhóm IELTS/TOEIC/Giao tiếp | BLOCKED do nguồn Admission | courses chỉ có level BEGINNER/INTERMEDIATE/ADVANCED; không có danh mục nhóm sản phẩm chuẩn. Mapping này là việc/policy Admission, không phải chờ Finance. |
| Historical as_of/cycle/role | MISSING nguồn lịch sử, PARTIAL chức năng | API công bố CURRENT_STATE_ONLY và từ chối as_of client. Reuse enrollment mất cohort cũ; audit không có role-at-event. Phải chốt V1 chỉ snapshot hay đầu tư history; không thể backfill quá khứ bằng suy diễn. |

Nguồn code: AdmissionBiRepository, BiAnalyticsServiceImpl, BiWindow, BiAnalyticsController; UI bi-dashboard.html. Test8 PG BI, BiAnalyticsServiceTest,3 Node BI và Chrome STAFF/ADMIN/guest. Backend/frontend BI contract đã đổi cùng nhau; DTO cũ trên disk không có nghĩa endpoint cũ vẫn phục vụ schema cũ. Xem bi-kpi-qa.md để biết response chi tiết.

## 5. Phát hiện quan trọng — báo cáo trước, chưa sửa

Các nhận định dưới là **rà soát tĩnh đã đối chiếu code**, không tự gắn nhãn đã có exploit/runtime repro. Bộ161 tests hiện tại không chứng minh những nhánh thiếu test là an toàn. Đề xuất đầu tiên của đợt sửa tiếp theo là viết regression tái hiện từng ca trước khi sửa.

### H01 — HIGH: tính hợp lệ và thế hệ giao dịch callback (Admission chịu trách nhiệm)

Bằng chứng: EnrollmentServiceImpl.java:128 tái dùng bản ghi/đổi enrolled_at nhưng không xóa invoice_code;160–167 chỉ đổi invoice nếu generateInvoice thành công, lỗi Finance bị giữ lại;245–303 processPaymentSuccess không đối chiếu PAID/amount/invoice ownership/cycle, paid_at chỉ lưu,293 có thể ghi invoiceCode từ payload. PaymentSuccessWebhookRequest không có ràng buộc trạng thái PAID hoặc amount dương. Shared secret chứng minh nguồn gửi, không chứng minh khoản thu hợp lệ cho đơn hiện tại. Event replay đã tồn tại trả theo enrollment_id mới từ payload, không so binding event đã lưu.

Kịch bản cần tái hiện: hủy đơn trong lớp còn OPEN -> đăng ký lại cùng user/class -> Finance tạo invoice lỗi -> invoice cũ còn đó; event mới/late của invoice cũ có thể tác động chu kỳ mới. Hoặc callback authenticated có invoice không khớp/amount không đủ/không PAID vẫn vào lõi duyệt. Không gửi request thử này vào dữ liệu vận hành trong lượt audit.

Đề xuất: chốt contract Finance trước; bind event_id với enrollment/cycle/invoice bất biến; validation fail-closed về trạng thái/số tiền/định danh/thời điểm; ghi exception journal cho REJECTED/refund-pending/mismatch. Tách generation khi tái đăng ký, không tái dùng xác nhận/hoá đơn/cash-request/outbox sai chu kỳ. Kiểm thử stale invoice, mismatch event binding, PAID quá hạn và duplicate đồng thời. Đây là phần Admission còn thiếu, không thể chỉ ghi “chưa E2E Finance”.

### H02 — HIGH: sức chứa phòng có thể bị phá khi sửa phòng (Admission)

RoomServiceImpl.java:60–76 nhận capacity mới và save mà không kiểm tra các lớp đang gán; ClassServiceImpl chỉ kiểm tra max_students<=room.capacity lúc tạo/sửa lớp. Kịch bản cần test: phòng30 có lớp max25, PUT giảm capacity xuống10 vẫn thiếu guard quan hệ lớp. DTO chỉ Min1. Đề xuất kiểm tra/từ chối giảm dưới ràng buộc lớp và dùng khóa tài nguyên chung với lưu lớp để tránh stale check. Chưa chạy runtime repro ca này trong lượt audit.

### H03 — HIGH risk, chưa tái hiện concurrency: hai lệnh đặt cùng phòng/teacher

ConflictEngine đọc ứng viên rồi ClassServiceImpl.save; ClassRepository.java:113/138 không khóa resource,92 chỉ khóa class theo ID. Hai create có ID khác có thể cùng thấy lịch trống; chưa có DB exclusion/khóa chung trong code đã đọc để serialize phòng/teacher. Unit phát hiện lịch đã tồn tại không chứng minh concurrency-safe. Đề xuất PG hai transaction barrier trước save, rồi thống nhất resource locking/exclusion và thứ tự khóa; giữ test conflict hiện tại.

### H04 — MEDIUM: tự dựng mã Student khi export

ExportServiceImpl.java:82 dùng STU-{enrollments.student_id}; student_id ở Admission là users.id, không phải mã hồ sơ Student. Đây là dữ liệu sai nhãn do Admission tạo ra, không phải chỉ thiếu nghiệm thu liên hệ thống. Đề xuất xuất Admission user ID dưới nhãn đúng hoặc chỉ dùng mã Student khi có ACK/source mapping đáng tin cậy; không tự bịa STU code.

### H05 — MEDIUM/P1 workflow: countdown không dùng thời gian còn lại thực

test.html:332 lấy expires_at-started_at (toàn thời lượng),366 auto-submit sau countdown. Server PlacementTestServiceImpl từ chối now>expires_at. Network delay, tab throttling hoặc bắt đầu hiển thị muộn có thể khiến UI báo tự thu bài nhưng server từ chối. Đề xuất trả server_now/offset, render deadline tuyệt đối và chốt cách finalize bài hết giờ; không nới deadline để che lỗi. Chưa có browser mạng chậm/timeout auto-finalize proof.

### H06 — MEDIUM: gia hạn khôi phục PENDING đã quá hạn

EnrollmentServiceImpl.java:312–350 cho ownSlot=0 khi deadline qua và dùng base=now để cấp hạn mới nếu còn slot. FR-ENR-05 hiện nói còn hiệu lực. Đề xuất chốt policy rồi chặn deadline<=now hoặc đổi đặc tả có phê duyệt; test before-scheduler/EXPIRED/class canceled/concurrent extension.

### H07 — MEDIUM: tài khoản nhân sự và vòng đời session UI chưa đầy đủ

CreateUserRequest thiếu @NotNull/NotBlank cho phone/gender/DOB; UserServiceImpl tạo Staff/Teacher với must_change_password=true, nhưng AuthServiceImpl:122/221 chỉ cưỡng chế cờ này cho STUDENT, JwtAuthenticationFilter không có first-password gate cho nhân sự. account.js setSession chỉ dùng access_token/user; shared/js/auth.js logout chỉ xóa local storage, không gọi logout thu hồi. **Không sửa shared trong audit.** Đề xuất luồng Admission hoặc adapter riêng có validation/session refresh/logout/forced-change theo policy được chốt. GET auth/me có trong API catalog nhưng không thấy mapping trong AuthController: thành phần API này MISSING, cần đối chiếu/chốt contract.

## 6. Phân chia chính xác chủ thể công việc còn lại

### A. Admission tự xử lý / kiểm chứng tiếp

A1. Viết regression và xử lý H01 validation/cycle/event binding, không chờ E2E để làm các guard của Admission.
A2. Bảo vệ giảm capacity và concurrent phòng/teacher (H02/H03); thêm PG tests.
A3. Sửa mã export sai nhãn (H04) và kiểm chứng export HTTP/PG/chính chủ; CSV đủ định dạng FR, không bắt thêm XLSX nếu scope chưa đổi.
A4. Sửa countdown và hoàn thiện timeout UX theo quyết định nghiệp vụ (H05).
A5. Làm khớp gia hạn FR-ENR-05 (H06).
A6. Hoàn thiện validation/first-password/session/me API cần thiết (H07).
A7. UI còn MISSING: quản trị nhân sự/audit; sửa room/course/class, lifecycle/cancel class; ngân hàng bộ đề/câu hỏi/media và xuất kết quả; tự đăng ký không test/Student xem đơn-hạn. Chốt rõ V1 web đầy đủ hay API-only trước khi loại mục nào khỏi scope; hiện không có bằng chứng đã loại.
A8. Chứng minh cron expiry tác động DB thực, refresh concurrent, media playback, export authorization; thêm nghiệm thu rollback/restart process của outbox và quản trị REVIEW/EXHAUSTED có audit.
A9. Khi có Finance/Student contract, Admission vẫn phải làm phần transport/ACK/reconciliation còn thiếu cho cash/refund và bật dispatcher đúng điều kiện. Outbox/request local không tự đồng nghĩa đã chuyển liên service.

### B. Quyết định nghiệp vụ/kiến trúc của nhóm Admission

DEC01 tài khoản xác minh trước attempt và mời user tại quầy; DEC02 user_id vs Student profile ID; DEC03 paid_at/callback muộn/slot đầy; DEC04 Finance xác nhận CASH; DEC05 preset ca (không tự thêm class_shifts); DEC06 hold nhiều lớp/NULL deadline/reuse enrollment; DEC07 conversion14 ngày; DEC08 định nghĩa pipeline/thực thu và phân bổ; DEC09 hiệu lực thu hồi/khóa access JWT ngoài Admission; DEC10 DDL/migration và review ranh giới. Repo BA ghi đây là đề xuất, không tìm thấy bằng chứng phê duyệt tất cả trong tài liệu đã rà.

Cần chốt thêm: V1 snapshot hay historical as_of; mapping nhóm IELTS/TOEIC; bài trắng/auto-finalize timeout; inactive thay delete; có cho gia hạn PENDING đã quá hạn không; điều kiện first-password; ngưỡng login tổng5 vs hơn5 liên tiếp (Auth guard và BI signal đang khác định nghĩa). Không tự quyết thay nhóm trong lượt audit.

### C. Phụ thuộc Finance/Student và nghiệm thu liên service

- Finance phải triển khai endpoint CASH/refund đúng catalog; mapping invoice_code<->invoice_id, auth nội bộ, idempotency/ACK, PAID+amount+paid_at và contract doanh thu/dư nợ/hoàn/phân bổ. Admission còn phải nối những contract này sau khi chốt. Hiện cash/refund chỉ local review, callback cũ không đủ đối soát.
- Student backend chưa có endpoint internal idempotent theo user_id đúng contract Admission. Cần ACK student_profile_id/code và replay cùng user sau timeout không tạo hồ sơ thứ hai. Dispatcher mặc định tắt; không bật verified chỉ theo một mock test.
- Khóa/thu hồi JWT ở service khác, đối soát thanh toán thực, hoàn tiền thực và tạo hồ sơ Student thực là nghiệm thu liên service sau đó. **Việc chưa chạy E2E toàn hệ thống không phải lý do duy nhất của NOT COMPLETE**; A1–A8 đã đủ chỉ ra công việc riêng Admission.

## 7. Migration, cấu hình, quyền và dữ liệu nhạy cảm

- Đã đọc4 migration Admission: admission-account-audit.sql, admission-class-cancellation.sql, admission-cash-confirmation.sql, admission-student-sync.sql. DB QA đọc được4 bảng mới và CHECK audit có action email/refresh/security/create-user; PostgreSQL16.15. Không áp dụng/chạy migration lại trong audit; không chứng nhận fresh install/upgrade toàn môi trường chỉ từ DB đã có schema.
- Base scripts/init-db.sql tạo cả4 schema: không chạy bừa trên hệ thống đang có dữ liệu. ddl-auto=none, không có auto migration runner. Gate triển khai phải nạp đúng base rồi4 migration có kiểm soát; bổ sung runbook/upgrade smoke nếu chưa có.
- Biến môi trường bắt buộc cho môi trường thật: DB*, JWT_SECRET, INTERNAL_SECRET_KEY, service URLs; không dùng giá trị mặc định phát triển công khai. SMTP enable/host/port/auth/from/TLS/HTTPS account URL theo docs. Không sao chép mật khẩu vào báo cáo/log.
- Student sync enabled=false, idempotency-verified=false là mặc định an toàn, nhưng nghĩa là **chưa có đồng bộ tự động đang hoạt động** khi chưa bật/chốt endpoint. Cron expiry default5m; integration test dùng cron=-.
- ADMIN-only users/audit/security BI; STAFF/ADMIN cash/class writes; STUDENT chính chủ enrollment/attempt; export teacher lớp phụ trách. Có bằng chứng HTTP role/ownership đã test nhưng chưa đủ mọi endpoint/role tổ hợp. Internal webhook có constant-time secret filter; chỉ xác thực nguồn, không thay validation H01.
- Email token hash/purpose/single-use được PG/SMTP loopback chứng minh, không ghi token/mật khẩu vào log trong các case capture hiện có. UserResponse không có password_hash. Không tuyên bố quét mọi runtime log/PII tuyệt đối sạch: Finance client còn log response body khi lỗi; cần redaction/retention khi triển khai.
- Refresh token còn dạng raw trong DB; access JWT sau đổi/reset mật khẩu có thể còn hiệu lực tới TTL, trong khi khóa tài khoản bị chặn ngay. Tách hardening này khỏi phần core đã đạt, chốt tiêu chí V1 rõ ràng.
- Auth tin X-Forwarded-For; cần trusted proxy/Gateway sanitization. Shared auth có fallback demo nhưng Admission role helper chặn guest không token; không suy ra fallback UI đồng nghĩa vượt quyền backend. Chưa xác minh cấu hình triển khai production/trusted proxy.

## 8. Tái lập kiểm thử ngày audit

Kết quả thực tế mới: **161/161 Java tests pass**, trong đó **48 PostgreSQL integration tests**,0 failures/errors/skipped; JDK21.0.13/Maven3.9.9. **15/15 invariant**, **10/10 Node regressions** và Chrome7 trang + BI STAFF/ADMIN/guest pass. git diff --check pass.

Lần đầu java.log/db-readonly.log lỗi vì PostgreSQL QA chưa lắng nghe55432. Khởi động lại data directory QA hiện hữu, không init/reset DB; java-verified.log/db-verified.log là bằng chứng sau phục hồi. Không quy lỗi môi trường này thành bug source, cũng không lấy log fail làm kết quả cuối.

Phạm vi proof: PostgreSQL/JPA/transaction/HTTP Admission/SMTP loopback là thật; Finance/Student client mock; browser dùng API fixtures; invariant suite là model/static/SQLite, không phải48 PG tests. Scheduler expiry tắt trong PG fixture; restart outbox dùng instance worker mới/lease hết hạn, chưa restart JVM thật. Không chạy E2E toàn hệ thống. Các phát hiện H01–H07 chưa có runtime regression mới trong lượt này; không sửa/chạy attack payload để giả chứng minh.

Lệnh tái lập: JAVA_HOME trỏ JDK21 ở qa-v1, ADMISSION_PG_TEST=true, ADMISSION_QA_JDBC_URL=jdbc:postgresql://127.0.0.1:55432/admission_qa_v1?currentSchema=admission, password lấy cấu hình QA ngoài repo; Maven -B với local cache english-center-qa-tools/m2 và -f services/admission/backend/pom.xml test. Python unittest discover tests/invariants; Node frontend/tests/regression.cjs; browser.cjs dùng NODE_PATH=qa-admission-quality/browser/node_modules và Chrome. Bỏ ADMISSION_PG_TEST sẽ skip48 cases: không được gọi là PostgreSQL pass.

## 9. Điều kiện chuyển sang ADMISSION V1 FEATURE-COMPLETE

1. Chốt scope/DEC trong repo; không âm thầm loại UI,14 ngày hoặc historical khỏi scope để nâng nhãn DONE.
2. Đóng các guard/mismatch trực tiếp thuộc Admission (A1–A6) với test tái hiện trước sửa; nghiệm thu các workflow/UI V1 còn thiếu (A7).
3. Bổ sung bằng chứng A8, runbook migration/config/security và chuẩn bị dữ liệu hồi quy độc lập.
4. Chốt contract và hoàn tất phía Admission các handoff còn thiếu; phân biệt rõ nghiệm thu riêng phần đã sẵn sàng tích hợp với ACK thật của Finance/Student. Nếu tách integration khỏi scope V1 thì cần phê duyệt phạm vi và label điều kiện tương ứng, không tự đổi định nghĩa FR-ENR-07/08.
5. Chạy lại regression sau sửa và lập bảng FR mới. Chưa có cơ sở tuyên bố feature-complete ở snapshot hiện tại.

---
Báo cáo được tạo ngoài repository; không có file source cần review/merge trong lượt audit này.

## 10. Liên kết bằng chứng chính

- [BA/BI và30 FR](../services/admission/docs/admission-ba-bi-spec.md)
- [Schema](../services/admission/docs/admission-schema-spec.md), [API catalog](../services/admission/tasks/API_CATALOG.md), [UML](../services/admission/docs/admission-uml-spec.md)
- [Tiến độ nguồn](../services/admission/docs/admission-quality-progress.md), [QA nguồn](../docs/admission-bug-report.md)
- [Callback/reuse invoice](../services/admission/backend/src/main/java/com/englishcenter/admission/service/impl/EnrollmentServiceImpl.java:245)
- [Gia hạn](../services/admission/backend/src/main/java/com/englishcenter/admission/service/impl/EnrollmentServiceImpl.java:312)
- [Sửa capacity phòng](../services/admission/backend/src/main/java/com/englishcenter/admission/service/impl/RoomServiceImpl.java:60)
- [Mã Student trong CSV](../services/admission/backend/src/main/java/com/englishcenter/admission/service/impl/ExportServiceImpl.java:82)
- [Countdown](../services/admission/frontend/test.html:332)
- [Log Java lượt audit](C:/Users/Admin/admission-work/qa-admission-final-audit-20261110/java-verified.log)
- [Hash xác nhận bảo toàn](C:/Users/Admin/admission-work/qa-admission-final-audit-20261110/preservation.json)
