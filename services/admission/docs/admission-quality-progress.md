# Tiến độ chất lượng riêng Admission — 10/11/2026


Cập nhật sức chứa/booking 10/11/2026: đã sửa H02/H03 bằng capacity guard và PostgreSQL transaction advisory lock dùng chung cho create/update class, update/status room.10 PG tests mới: capacity/races room-teacher/update/status, giờ kề, ngày rời, rollback. **171/171 Java tests (58 PostgreSQL),15/15 invariants,10/10 Node và Chrome regression pass**. Giữ các luồng cũ; không có migration mới/không chỉnh frontend. Xem [room-booking-qa.md](room-booking-qa.md) về READ COMMITTED, writer phối hợp, contention và CUSTOM chưa có định nghĩa ngày. Không tự đánh dấu Admission V1 feature-complete hoặc xử lý nhóm lỗi khác.


Cập nhật BI 10/11/2026: **PARTIAL tổng thể**. Đã sửa cohort/distinct user/14 ngày, conversion/quá hạn theo cohort đơn, occupancy có trọng số, ranking volume, DAU/MAU lịch Việt Nam, rate/signal bảo mật và dashboard/bộ lọc/quyền. Tiền dư nợ/thực thu trả null + UNAVAILABLE vì phụ thuộc Finance. Chưa hỗ trợ historical as_of/nhóm IELTS-TOEIC thiếu nguồn. Matrix từng KPI: [bi-kpi-qa.md](bi-kpi-qa.md). Kết quả mới nhất **161/161 Java tests (48 PostgreSQL), 15/15 invariants, 10/10 Node regressions, Chrome 7 trang và BI STAFF/ADMIN/guest pass**. Không chạy E2E hoặc tổng nghiệm thu V1. Các mốc retry/cash bên dưới là lịch sử.


Cập nhật retry Student (10/11/2026): **PARTIAL**. Outbox nguyên tử với duyệt, worker lease/SKIP LOCKED/token fencing, backoff tối đa 5 lần, REVIEW/EXHAUSTED và reconcile cờ đã triển khai. Gửi mặc định tắt; idempotency Student thật chưa kiểm chứng. **153/153 Java tests pass (40 PostgreSQL), 15/15 invariants, 7/7 Node regressions**, không Java test skip. Helper trong PG fixture đã chạy thật; chỉ client Student được mock. Xem [student-sync-retry.md](student-sync-retry.md). Những mốc tiền mặt/cascade bên dưới là lịch sử; không làm BI hoặc E2E toàn hệ thống trong đợt retry.

Cập nhật tiền mặt: **PARTIAL**. STAFF/ADMIN ghi yêu cầu và audit nguyên tử, không duyệt ngay; Finance chưa có endpoint CASH/mapping/hợp đồng chuyển yêu cầu đầy đủ. Frontend hiển thị chờ đối soát. Đã kiểm chứng **143 Java tests, gồm 31 PostgreSQL tests; 15 invariants; 7 Node regressions; Chrome 7 trang với API fixtures và luồng gửi CASH**. Không chạy Finance thật hoặc E2E toàn hệ thống. Chi tiết: [cash-confirmation.md](cash-confirmation.md). Cascade, giữ chỗ và webhook cũ vẫn pass; retry Student và BI chưa xử lý trong đợt này.

Cập nhật cascade cancellation: đã thêm điều kiện hủy lớp và cascade transaction có khóa; yêu cầu đối soát hoàn phí lưu tại Admission, chưa gửi/tạo refund từ xa do hợp đồng Finance chưa được triển khai đầy đủ. Xem [class-cancellation.md](class-cancellation.md). Ba chức năng còn lại giữ nguyên phạm vi trước đó.

Phạm vi: `feature/admission`, baseline `84af6e9`, thư mục `C:\Users\Admin\admission-work\english_center`. Không nghiệm thu V1 toàn hệ thống. Đã giữ 64 file thay đổi tại thời điểm tiếp tục trong snapshot riêng ngoài repo; không reset, stash, commit, push hoặc merge. Không sửa Finance, Student, Learning hoặc shared assets.

## Kết quả đã kiểm chứng

- **143/143 Java tests pass, không failure/error/skip** sau tiền mặt, Microsoft OpenJDK 21, Maven 3.9.9. Trong đó **31 PostgreSQL integration tests** chạy trên PostgreSQL 16.15 tại `127.0.0.1:55432/admission_qa_v1`. Log cuối: `C:\Users\Admin\admission-work\qa-admission-cash-20261110\java-final.log`. Mốc sau cascade, trước tiền mặt: 131/19 tại `cancel-verified.log`.
- **15/15 invariant tests pass**; đây là mô hình SQLite/kiểm tra tĩnh, được báo riêng với test Java/DB thật.
- **7/7 Node frontend regression tests pass**: giữ 5 ca cũ và thêm thông báo yêu cầu CASH, trạng thái chờ Finance/CANCELLED_REFUND_PENDING.
- Chrome headless kiểm tra lại 7 trang: courses, classes, rooms, enrollments, bi-dashboard, test, account. Giữ các kiểm tra export JWT, role, guest, email fragment/setup token; thêm form CASH, POST có JWT, nhận 202 và hiển thị chờ Finance sau reload. Các API Chrome dùng fixtures, không phải E2E toàn hệ thống.
- Migration audit chạy hai lần liên tiếp thành công, không xóa dữ liệu. `git diff --check` pass.

Spring Boot HTTP server, security chain, JPA/transaction, PostgreSQL và SMTP loopback trong integration test là thật. Hai cổng Finance/Student được mock, scheduler hết hạn tắt để kiểm tra deadline độc lập. Không chạy E2E toàn hệ thống, không gửi thư đến hộp thư bên ngoài.

Log lần tiếp tục: `C:\Users\Admin\admission-work\qa-admission-resume-20261109\{java-verification.log,frontend-node.log,frontend-chrome.log,invariants.log}`. Các log fail trước sửa nằm tại `qa-admission-quality`: `audit-before.log`, `staff-audit-before.log`, `frontend-before.log`, `recommend-before.log`, `csv-before.log` và `final-java.log` của lần bị ngắt.

## Phần vừa hoàn tất

Audit đăng nhập sai đã dùng transaction không rollback khi ném `BusinessException`; bản ghi `LOGIN_FAILED` được commit để guard đếm được. Migration bổ sung đầy đủ action của email, tạo nhân sự và guard bảo mật. Lần bị ngắt có 124 test và một failure: sau nhiều test dùng chung IP loopback, guard chặn login đúng. Fixture hiện cấp IP riêng cho mỗi test bằng header proxy có kiểm soát, giữ nguyên audit cũ và không tắt guard. Test mới xác nhận 5 lần sai vẫn dẫn đến 403 và lưu audit chặn.

Đăng ký, xác minh email, setup/reset/đổi mật khẩu và resend đã chạy qua HTTP + SMTP + PostgreSQL; kiểm tra hash token, purpose, hết hạn, replay, hai request xác minh đồng thời, khóa tài khoản, role STUDENT cố định, validation BCrypt và không lộ secret trong log. Chưa kiểm tra SMTP của nhà cung cấp thật. Xem [cấu hình email/migration](account-email-setup.md).

Frontend đã sửa sai `question_text`, `upcoming_classes`, `class_id`, `tuition_fee`, `enrolled_students`; API lookup giáo viên chỉ trả ID/họ tên và cho Staff sử dụng, không mở danh sách người dùng Admin. Export dùng fetch có JWT. Các chuỗi dữ liệu được kiểm thử có escape HTML; không ghép tên lớp vào JavaScript của nút đăng ký. Trang BI đã ghi rõ các giá trị niêm yết chỉ là ước tính, không phải tiền thực thu.

## Đối chiếu 30 FR — không đồng nhất có code với đã nghiệm thu đầy đủ

Nguồn đối chiếu: BA/BI v3.6, technical spec, UML, schema spec và bản tổng hợp trong cùng thư mục. Một số quy tắc BA ghi là đề xuất/chờ chốt; các khác biệt dưới đây không được tự coi là đã được nhóm phê duyệt.

| FR | Đã triển khai/kiểm thử | Phần chưa hoàn thiện hoặc chưa kiểm chứng |
|---|---|---|
| IAM-01 | Tài khoản STUDENT, xác minh/setup qua email; HTTP/SMTP/PG | SMTP ngoài môi trường QA; mời tài khoản tạo tại quầy |
| IAM-02 | API nhân sự, unit; tạo user + audit trên PG | Giao diện quản trị nhân sự chuyên biệt |
| IAM-03 | Login/refresh/logout, unit; HTTP login sau setup/reset | Refresh token hiện chưa chuyển toàn bộ sang lưu hash |
| IAM-04 | Rotation/replay, khóa repository, unit và test thu hồi refresh | Chưa có PG stress test nhiều request refresh; access-token versioning |
| IAM-05 | Khóa user chặn access JWT cũ, PG | Kiểm tra vận hành nhiều instance chưa thực hiện |
| IAM-06 | Email VERIFY/SETUP/RESET, quên/đổi mật khẩu, PG/SMTP | Provider SMTP và chuyển thư thực tế |
| IAM-07 | Audit thành công/thất bại, brute-force, CHECK migration, PG | UI audit chuyên biệt; chính sách trusted proxy |
| FAC-01 | API tạo/sửa/trạng thái phòng, unit, Chrome trang phòng | UI chỉnh sửa đầy đủ và nghiệm thu CRUD với DB thật |
| FAC-02 | Enum/preset ca, validation giờ | CRUD ca chuẩn theo đề xuất V2 chưa triển khai |
| FAC-03 | Ràng buộc room.capacity, unit ClassService | Nghiệm thu DB tranh chấp cập nhật sức chứa phòng |
| ACA-01 | Course API, prerequisite, unit, Chrome | Form sửa khóa đầy đủ; kiểm tra chuỗi tiên quyết nâng cao |
| ACA-02 | Mở lớp/API sửa, validation, teacher lookup Staff trên PG | UI sửa lớp; tranh chấp mở lớp đồng thời |
| ACA-03 | ConflictEngine và ClassService unit tests | Chưa PG test hai transaction đặt cùng phòng/giáo viên |
| ACA-04 | State transitions lớp, unit | Hủy lớp chưa cascade toàn bộ trạng thái đơn/chờ hoàn phí |
| ACA-05 | Tính approved + active hold, unit và PG cạnh tranh slot | Quy ước deadline null legacy cần thống nhất |
| TST-01 | API test sets, unit | Trang quản lý ngân hàng đề |
| TST-02 | API câu hỏi, unit; Chrome hiển thị nội dung | Trang biên soạn đề; phát media ngoài môi trường fixture |
| TST-03 | Deadline server, token sở hữu, frontend countdown | Chưa nghiệm thu đồng hồ qua nhiều múi giờ/mạng chậm |
| TST-04 | Chấm ba kỹ năng, chống lặp; unit/PG/HTTP JSONB | Timeout tự chốt bài và bài trắng cần chốt chính sách |
| TST-05 | Recommendation API/unit; sửa nút class_id, Node | Nghiệm thu toàn bộ các dải điểm và UI tư vấn |
| ENR-01 | Staff-assisted API; Student tự đăng ký chính chủ | Invite email tự động sau khi Staff tiếp nhận |
| ENR-02 | Khóa slot theo lớp, PG hai transaction | Chưa load test nhiều instance |
| ENR-03 | Hold 48h và trùng người-lớp; unit/PG | Nghiệm thu toàn bộ trải nghiệm tra cứu của Student |
| ENR-04 | Expiration service/unit; deadline độc lập job trên PG | Chưa kiểm tra cron vận hành; hủy hóa đơn chưa đầy đủ |
| ENR-05 | Gia hạn có khóa và tính slot, unit | BA mới đề xuất cấm gia hạn PENDING quá hạn; code legacy có thể khôi phục nếu còn slot |
| ENR-06 | Webhook secret/idempotency, late reconciliation; unit/PG | Đối chiếu đủ tình trạng/số tiền invoice và chính sách paid_at |
| ENR-07 | Ghi yêu cầu CASH/audit; quyền, validation, replay, đồng thời/rollback trên PG; frontend Node/Chrome | PARTIAL: chưa gửi/tạo CASH tại Finance; thiếu mapping invoice_id, acknowledgment và bằng chứng phương thức CASH trong callback |
| ENR-08 | Outbox + worker bền vững, giới hạn/backoff/lease/fencing, PG retry/rollback/concurrency | PARTIAL: endpoint/idempotency Student thật; restart process thật; UI/requeue quản trị có audit |
| OPS-01 | Export CSV/unit, Chrome JWT; chống CSV formula | Export roster chưa kiểm chứng cùng DB/HTTP thật; XLSX chưa có |
| OPS-02 | Export kết quả CSV/unit | UI quản lý đề và nút export kết quả; XLSX chưa có |

## BI và lỗi/thiếu chức năng còn mở của Admission

- BI-FUN-01/03/04, BI-OCC-01, BI-USR-01 và BI-SEC-01 đã sửa/kiểm chứng cho snapshot hiện tại. FUN-02 đã có công thức 14 ngày và test ranh giới, nhưng chính sách này còn là đề xuất BA. Cohort test chưa có liên kết course; metadata hiển thị scope toàn trung tâm.
- BI-REV-01/02 chưa có dữ liệu/hợp đồng Finance để tính đúng. API trả null/UNAVAILABLE và UI “Chưa khả dụng”, đã bỏ ước tính niêm yết khỏi KPI tiền.
- Ranking theo volume và occupancy theo level đã triển khai. Nhóm sản phẩm IELTS/TOEIC/Giao tiếp chưa có nguồn phân loại chuẩn; không đoán theo tên/code.
- Không có lịch sử đầy đủ để dựng as_of quá khứ hoặc cohort enrollment trước khi tái sử dụng bản ghi. API từ chối historical as_of; role trên audit là vai trò hiện tại.
- Cascade/cash/retry giữ nguyên công việc trước; vẫn còn phụ thuộc Finance/Student thật như các báo cáo chuyên đề. Không dùng đợt BI để chứng nhận các tích hợp này hoặc Admission V1.
- Auth controller vẫn cần chính sách trusted proxy và chống lạm dụng toàn cục/outbox email khi triển khai; không chỉnh các luồng này trong đợt BI.

**Đánh giá riêng Admission:** các luồng tài khoản, audit và nghiệp vụ đã nằm trong bộ regression hiện đạt kiểm thử. Toàn phân hệ vẫn **hoàn thiện một phần**, chưa đủ cơ sở tuyên bố đã đáp ứng trọn BA/BI. Không đưa thiếu E2E toàn hệ thống thành kết luận của đợt QA này.
