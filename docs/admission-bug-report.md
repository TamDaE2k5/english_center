# Báo cáo QA Admission

## Danh mục phòng HTTP 500 — 13/11/2026

- `GET /api/admission/rooms` thiếu bộ lọc có thể trả 500 nếu là lần chạy truy vấn tìm phòng đầu tiên trong Spring context. PostgreSQL trả SQLSTATE 42883, `function lower(bytea) does not exist`. Ép kiểu tham số chi nhánh sang chuỗi trước khi ghép mẫu LIKE đã sửa ca tái hiện. [QA Rooms và bàn giao](tasks/completed/admission-rooms-http-500-2026-11-13.md)

## Tài khoản nhân sự và mật khẩu ban đầu H07-B — 13/11/2026

- API tạo STAFF/TEACHER từng chấp nhận thiếu dữ liệu BA. Phiên dùng mật khẩu ban đầu cũng gọi được API trực tiếp, reset email tự xóa cờ, và UI chuyển sang trang khảo thí. H07-B bổ sung validation, giới hạn quyền phiên và dẫn người dùng đến form đổi mật khẩu.
- H07-B **PARTIAL**. BA cần xác nhận reset email có hoàn tất yêu cầu đổi mật khẩu lần đầu hay không. Trong thời gian chờ, reset email giữ cờ đang bật. [QA H07-B](tasks/completed/admission-staff-account-lifecycle-2026-11-12.md)

## Vòng đời phiên Admission H07-A — 12/11/2026

- Login trước đây lưu access token và bỏ refresh token. Payload thiếu dữ liệu vẫn có thể redirect. Adapter Admission kiểm tra đủ cặp token và người dùng, xoay vòng một lần khi hết hạn hoặc nhận 401, và cập nhật UI khi mất phiên.
- Logout Admission gửi refresh token chính chủ để thu hồi, xóa local khi server lỗi và chỉ báo revoke khi server xác nhận. Hai tab có cùng bản sao phối hợp bằng Web Locks và marker không chứa token.
- Backend bổ sung `GET auth/me` theo BA, từ chối refresh JWT làm Bearer và bảo vệ logout chính chủ. **H07-A PARTIAL** vì cơ chế lưu refresh chống XSS và phiên dùng chung nhiều tab còn phụ thuộc kiến trúc. [QA H07-A](tasks/completed/admission-session-lifecycle-2026-11-11.md)

## ID tài khoản trong roster CSV H04 — 11/11/2026

- Roster từng dựng `STU-{id}` từ FK `enrollments.student_id` rồi ghi dưới cột `Mã học viên`. H04 thay bằng `ID tài khoản Admission` và giá trị số của FK, giữ vị trí cột và định dạng CSV. Bảng điểm dùng mã thí sinh Admission riêng.
- Hai endpoint CSV áp dụng quyền STAFF/ADMIN theo ma trận BA. Kiểm tra ID sai kiểu và tăng bảo vệ ô CSV bắt đầu bằng ký tự điều khiển hoặc tiền tố công thức.
- H04 **DONE** cho lỗi dữ liệu roster. FR-OPS-01 và FR-OPS-02 **PARTIAL**. Chi tiết contract, phân quyền và kết quả tại [QA H04](tasks/completed/admission-student-csv-export-2026-11-11.md).

## Countdown khảo thí H05 — 11/11/2026

- `test.html` hiển thị tổng thời lượng khi nhận đề và đếm theo nhịp timer. Response chậm, tab bị tạm dừng và refresh làm lệch thời gian. Giao diện dùng deadline và giờ server, mở lại đúng lượt trong cùng tab và khóa khi về 0.
- Hai request từng có thể cùng vào luồng chấm. Ghi `EXPIRED` khi trả lỗi cũng từng bị rollback. Backend khóa bản ghi trước khi kiểm tra giờ và trạng thái, giữ được lần chuyển `EXPIRED` khi từ chối nộp muộn.
- H05 **PARTIAL**. Cơ chế tự chốt hết giờ và chấm bài trắng cần BA/team quyết định. Kết quả tái hiện và kiểm thử nằm trong [QA H05](tasks/completed/admission-placement-timeout-2026-11-11.md).

## Gia hạn giữ chỗ quá hạn — H06, 11/11/2026

| Mã | Mức độ/vị trí, nguyên nhân và cách tái hiện | Sửa/giới hạn |
|---|---|---|
| ADM-QA-29 / H06 | Medium, extendHold: PENDING quá deadline/job chưa chạy hoặc deadline NULL vẫn được cấp hạn từ now | Chỉ gia hạn PENDING có deadline > now sau class lock + row refresh; <=/NULL bị từ chối. Expiry UPDATE thống nhất <=. Không tự chuyển status trong transaction gia hạn bị từ chối |
| ADM-QA-30 | Medium, API/service: gửi lại cùng body hoặc hai Staff cùng snapshot cộng giờ nhiều lần | API yêu cầu expected_hold_expires_at; compare-and-set dưới row lock, snapshot cũ409 ADM_HOLD_CONFLICT. Client cũ thiếu trường mới400; client phải cập nhật cùng bản sửa. Không tự đặt số lần tối đa |
| ADM-QA-31 | Medium, service: không có actor vẫn gia hạn; PENDING legacy của lớp CANCELLED vẫn được gia hạn | Guard STAFF/ADMIN tại service, chặn lớp CANCELLED; audit EXTENDED cùng transaction, REJECTED sau rollback. Lỗi audit rollback deadline/notes |
| ADM-QA-32 | Medium, frontend enrollments.html: nút vẫn hiện cho PENDING hết giờ/NULL; form không giữ precondition, API null vẫn có thể báo thành công | Guard nút/modal, gửi snapshot gốc qua retry, chống submit đang chờ, không báo thành công khi kết quả null/false |

Trước sửa **5/5 PG tests mới fail đúng lỗi**, 2 Node tests mới fail. Sau sửa **200/200 Java tests pass (JDK21,87 PostgreSQL16.15, không skip),15/15 invariants,12/12 Node,Chrome7 trang/BI roles/CASH/hold snapshot pass**. Matrix test, lịch sử các lần fail fixture và giới hạn được cập nhật tại [hold-extension-qa.md](../services/admission/docs/hold-extension-qa.md). Log `C:/Users/Admin/admission-work/qa-admission-hold-20261111/`. Giữ nguyên H01 cùng checkpoint cbf5be7/H02/H03. Chỉ xử lý H06, không Finance/Student/Learning/E2E/commit/push/merge.

**Chính sách PARTIAL:** FR-ENR-05 hiện quy định còn hiệu lực; task cũ lại dùng max(deadline,now), BA nêu24/48 trong khi task/API1–72. Bản sửa giữ giới hạn giờ đã có và chặn quá hạn theo FR; override khôi phục, cap số lần/tổng thời gian và NULL legacy cần người phụ trách/Tâm chốt. Không diễn giải yêu cầu chủ ý với snapshot mới là chính sách gia hạn vô hạn đã được duyệt.

## Callback thanh toán/chu kỳ hóa đơn — 11/11/2026

**H01: PARTIAL**, đã sửa guard Admission; chưa chứng nhận thu đủ tiền hoặc tái phát hành hóa đơn Finance thật. Baseline sạch `cbf5be7`, giữ nguyên H02/H03.

| Mã/mức độ | Vị trí, tái hiện và nguyên nhân | Sửa/kiểm chứng, phần còn lại |
|---|---|---|
| ADM-QA-25 / High | EnrollmentServiceImpl + PaymentSuccessWebhookRequest: callback authenticated sai invoice/amount âm vẫn duyệt, thiếu đối chiếu evidence | Hai PG tests đỏ trước sửa; chặn thiếu/sai invoice, amount null/âm/0, thiếu/old/future paid_at, explicit status khác PAID. Không lấy invoiceCode từ callback. Thu thiếu so số phải thu chưa có dữ liệu authoritative để kiểm chứng |
| ADM-QA-26 / High | ProcessedFinanceEventRepository/service: event cũ dùng enrollment mới vẫn trả theo payload; chỉ exists-check | Một PG test đỏ trước sửa. Thêm fingerprint receipt và khóa transaction PG trước lookup; exact replay chỉ đọc, altered/binding/legacy thiếu hash bị từ chối + audit. Concurrent hai lớp chỉ một binding commit |
| ADM-QA-27 / High | createEnrollment: tái dùng đơn đóng và invoice cũ khi Finance lỗi; idempotency Finance theo cùng enrollment_id | Chặn reuse, giữ nguyên dữ liệu cũ; PG test xác nhận không thay invoice/status. **PARTIAL:** chưa hỗ trợ đăng ký lại cùng user/class bằng chu kỳ mới, cần Finance/BA chốt generation/idempotency/state history |
| ADM-QA-28 / High | processPaymentSuccess: REJECTED/refund-pending/event mới sau APPROVED thiếu journal đối soát | Ghi receipt/note, giữ status/approved_at/method; test terminal/out-of-order pass. Receipt/duyệt/outbox nguyên tử; lỗi receipt rollback và thử lại được. Business rejection audit commit sau rollback, 12 concurrent rejections vẫn đủ audit |

- **185/185 Java tests pass, JDK21;72 PostgreSQL 16.15 tests; không skip.15/15 invariants;10/10 Node;Chrome7 trang và BI roles với API fixtures pass.** Migration bổ sung Admission chạy lại thành công; `git diff --check` pass. Không thay assertion cũ để che lỗi; fixture callback được bổ sung evidence hợp lệ.
- Chưa xác minh Finance thật: tiền thu đủ/cumulative/currency/transaction_ref; mapping numeric ID↔mã hóa đơn; PAID/partial semantics, timezone/clock skew, hóa đơn thế hệ mới; xử lý hoàn/bảo lưu/slot đầy và receipt legacy. Giữ contract TASK-ADM-45: payment-success ngầm PAID khi không có status; amount dương không chứng minh thu đủ. Không dựng endpoint/bảng hóa đơn Finance.
- Log đỏ `qa-admission-callback-20261111/red.log` (3/3 fail), log cuối `java-final.log` (185 pass). Chi tiết contract, test matrix, migration và giới hạn tại [payment-callback-qa.md](../services/admission/docs/payment-callback-qa.md). Snapshot audit gốc giữ nguyên; không xử lý nhóm lỗi khác/không chạy E2E toàn hệ thống/không commit/push/merge.


## Sửa sức chứa phòng/concurrent booking — 10/11/2026

- **ADM-QA-23 / audit H02 (High):** giảm capacity dưới max_students của lớp gán phòng hoặc resize cạnh tranh create/update có thể phá invariant. Đã thêm guard MAX lớp và serialize check+save qua PostgreSQL transaction advisory lock. PATCH room status lấy cùng khóa để không phục hồi capacity từ entity cũ.
- **ADM-QA-24 / audit H03 (High):** concurrent create/update cùng phòng hoặc cùng teacher có thể cùng vượt conflict check. Đã đặt khóa chung trước read/class-row-lock; giữ nguyên công thức ngày/giờ và mã lỗi.
- Trước sửa:5 test PostgreSQL fail; thêm1 race update fail. Test PATCH từng lỗi fixture abstract method, đã sửa fixture bằng EntityManager thật, không nới assertion.
- Sau sửa: **171/171 Java tests pass JDK21, gồm58 PostgreSQL tests, không skip;15/15 invariants;10/10 Node và Chrome7 trang/BI role pass**. Không có migration hoặc thay đổi frontend.
- Bảo đảm/giới hạn: writer Admission cùng database/isolation READ COMMITTED; khóa chung làm các thao tác ghi lịch phải chờ nhau; không tự sửa dữ liệu lịch cũ hoặc diễn giải CUSTOM chưa có ngày cụ thể. Không chạy E2E, không sửa phân hệ khác/nhóm audit khác.

Chi tiết test/bằng chứng và giới hạn tại [room-booking-qa.md](../services/admission/docs/room-booking-qa.md). Log cuối `C:/Users/Admin/admission-work/qa-admission-room-booking-20261110/verified.log`. Báo cáo tổng audit trước là snapshot trước sửa; chỉ H02/H03 được cập nhật trạng thái trong đợt này.


## Đợt Dashboard BI — 10/11/2026

**PARTIAL tổng thể**, các KPI thuộc dữ liệu Admission đã kiểm thử; nguồn tiền Finance/historical as_of/nhóm sản phẩm chưa đủ. Không tổng nghiệm thu Admission V1.

| Lỗi | Mức độ/vị trí | Tái hiện và nguyên nhân | Sửa/kiểm chứng |
|---|---|---|---|
| ADM-QA-19 | High, BiAnalyticsServiceImpl funnel | Chia submissions trong kỳ cho attempts trong kỳ khác cohort; duplicate-user làm lệch conversion, N/A trả0. Test empty fail trước sửa | Cùng attempt cohort, distinct user/EXISTS, cửa sổ14 ngày, cohort đơn riêng; PG66.67%/100%/25%/25%, ngày và ranh giới14 ngày |
| ADM-QA-20 | High, courses-revenue/dashboard | APPROVED×tuition bị gọi realized; PENDING quá hạn cũng góp pipeline; ranking theo số tiền giả | Bỏ mọi phép suy tiền; null+UNAVAILABLE, ranking theo volume, pending deadline/class hợp lệ; occupancy2/40=5%; PG/UI kiểm chứng |
| ADM-QA-21 | Medium, security-engagement | 24h/30d trượt gắn nhãn DAU/MAU; không có trend ngày/tháng hoặc lọcrole; denominator0 trả0% | Ngày/tháng lịch Asia/Ho_Chi_Minh, distinct login user và failed/(success+failed); PGqua31/1-1/2, refresh excluded, 6fail/success reset; ADMIN-only cả alias |
| ADM-QA-22 | Medium, bi-dashboard.html/controller | Bộ lọc/API lỗi có thể để số cũ, N/A thành0, thiếu course/channel/role; query ngày sai ra500 | Bộ lọc thực, N/A/loading/error/reset/generation guard, parse lỗi400, từ chối historicalas_of; Node+Chrome STAFF/ADMIN/guest |

Kết quả cuối: **161/161 Java tests pass JDK21, gồm48 PostgreSQL16.15 tests, không skip; 15/15 invariants; 10/10 Node; Chrome7 trang cùng ca BI role/filter/KPI pass**. Các assertion ngoài BI giữ nguyên, không sửa Finance/Student/Learning/shared, không chạy E2E. Chi tiết matrix, định nghĩa, trạng thái DONE/PARTIAL/BLOCKED và file/test tại [bi-kpi-qa.md](../services/admission/docs/bi-kpi-qa.md) và [báo cáo hoàn thành](tasks/completed/admission-bi-2026-11-10.md). Log `C:/Users/Admin/admission-work/qa-admission-bi-20261110/verified-final.log`.

Phụ thuộc còn mở: hợp đồng dư nợ/thu/hoàn Finance, phê duyệt cửa sổ14 ngày, mapping nhóm sản phẩm và lịch sử enrollment/cycle/role. Không hiển thị tiền thiếu nguồn thành số0 hoặc số ước tính.


## ADM-QA-18 — High: đồng bộ Student gọi mạng trong transaction, không có retry bền vững

- **Vị trí:** StudentServiceSyncHelper, StudentServiceClient, cổng gọi từ approveEnrollment.
- **Tái hiện:** helper cũ nhận PENDING vẫn gọi Student (StudentSyncGuardTest fail trước sửa). Khi service lỗi chỉ ghi FAILED và log hứa thử lại; không có worker/queue. HTTP thành công trước Admission rollback hoặc timeout có thể tạo hồ sơ không rõ trạng thái và gây gửi trùng.
- **Nguyên nhân:** ghi cờ thay journal bền vững, HTTP ngay trong transaction duyệt, mất phân loại lỗi do client bọc mọi ngoại lệ.
- **Sửa:** helper chỉ APPROVED và MANDATORY transaction; outbox snapshot unique user_id cùng commit duyệt. Worker lấy lease bằng SKIP LOCKED, token fencing, gọi HTTP ngoài transaction, backoff 30/60/120/240 giây tối đa 5 lần. Giữ ngoại lệ transport/HTTP có kiểu; ACK chưa đủ chuyển REVIEW, không gán SYNCED. Unknown timeout/crash chỉ retry khi idempotency đã được xác minh; mặc định REVIEW. Không sửa trạng thái hủy khi reconcile.
- **Kết quả:** 153/153 Java pass trên JDK 21, gồm 40 PostgreSQL tests (9 PG retry mới), 15/15 invariants và 7/7 Node regressions pass. Migration chỉ Admission đã áp dụng và chạy lặp trên QA. Giữ assertion cascade/tiền mặt/hold cũ. PG fixture dùng helper thật, Student client mock; contract client có HTTP mock server.
- **Còn mở / PARTIAL:** Student hiện chưa có endpoint internal và idempotency đúng tài liệu. Mặc định dispatcher tắt; không khẳng định hồ sơ Student thật đã tạo hoặc exactly-once từ xa. Restart mới được mô phỏng bằng instance worker mới + lease DB, chưa kill/restart JVM thật. Chưa có UI/API requeue có audit; cần đối soát trước xử lý REVIEW/EXHAUSTED/legacy. Xem [student-sync-retry.md](../services/admission/docs/student-sync-retry.md).

Log cuối: `C:/Users/Admin/admission-work/qa-admission-retry-20261110/verified-legacy.log`. Không sửa Student/Finance/Learning, không chạy E2E toàn hệ thống hay làm dashboard BI.

## Đợt yêu cầu xác nhận tiền mặt — 10/11/2026

**Đánh giá hạng mục: PARTIAL**, phần Admission đã kiểm chứng; chưa ghi nhận khoản CASH hoặc phiếu thu Finance thật. Không làm retry Student/BI hoặc E2E toàn hệ thống. Giữ 76 file đầu lượt trong bản sao ngoài repo, không mất file cũ; cascade giữ nguyên code/migration/unit test và các PostgreSQL tests trước vẫn pass.

### ADM-QA-16 — High: approve-manual duyệt trước xác nhận Finance

- **Vị trí:** `EnrollmentServiceImpl.approveManual`, DTO `ManualApproveRequest`, API `EnrollmentController`.
- **Tái hiện trước sửa:** STAFF gửi chứng từ và số tiền dương cho đơn PENDING còn hạn qua POST approve-manual, không gọi Finance. PostgreSQL cho thấy status đã APPROVED (test mong PENDING thất bại). Gửi cùng chứng từ cho đơn khác cũng được duyệt; thiếu kiểm soát phương thức/chứng từ và audit nghiệp vụ riêng.
- **Nguyên nhân:** API gọi approveEnrollment trực tiếp, ghi note và MANUAL_CASH dựa hoàn toàn dữ liệu nhân viên; chưa có bước xác minh theo FR-ENR-07/DEC-04 BA mới.
- **Sửa nguyên nhân:** chỉ ghi yêu cầu NEEDS_FINANCE_CONFIRMATION với actor/time server, số tiền/chứng từ/note; khóa lớp/refresh đơn, UNIQUE và replay/conflict; cấm quá hạn/trạng thái kết thúc/lớp đóng hoặc đầy/chưa có invoice. Yêu cầu cùng audit commit nguyên tử, audit REJECTED commit riêng sau rollback. Không tự gán PAID, APPROVED hoặc MANUAL_CASH.
- **Kiểm chứng:** 12 test PG bổ sung: quyền/validation, pending không tự duyệt, replay/conflict/audit, quá hạn/cancel, đồng thời/cascade/callback, lỗi audit rollback, PUT/alias, deadline null/lớp đầy. Unit cash cũ đã đổi tiêu chí theo BA; invariant và assertion cascade cũ giữ nguyên.
- **Còn mở:** Finance chưa có FIN-F05 thực thi/mapping invoice_id/transport có idempotency và xác nhận CASH. Không có gửi hoặc retry tự động. Callback đang giữ contract payment-success hiện có, chưa chứng minh phương thức CASH hay đối soát đủ số tiền/dư nợ. Các guard này phải được kiểm chứng cùng Finance khi hợp đồng được hoàn thiện.

### ADM-QA-17 — Medium: giao diện báo thu/duyệt thành công sai và nhận kênh ngoài CASH

- **Vị trí:** `services/admission/frontend/enrollments.html`, modal approve-manual và danh sách.
- **Tái hiện trước sửa:** mở modal thấy hứa chuyển APPROVED; submit báo “thu học phí thành công” ngay; có POS_CARD/DIRECT_TRANSFER; yêu cầu chờ Finance không hiển thị, CANCELLED_REFUND_PENDING bị hiển thị nhãn mặc định.
- **Nguyên nhân:** UI theo luồng duyệt trực tiếp cũ, thiếu trạng thái yêu cầu tài chính độc lập.
- **Sửa:** chỉ CASH, giới hạn độ dài/số tiền, gửi chuỗi tiền tránh mất chính xác; thông báo chờ Finance/chưa gửi tự động; hiển thị yêu cầu sau reload, chặn nút yêu cầu mới nếu đã có/quá hạn/thiếu invoice và giữ nhãn chờ hoàn phí; lỗi API hiển thị rõ.
- **Kiểm chứng:** hai test Node mới fail trước sửa rồi pass; Chrome với API fixtures kiểm tra thao tác form CASH/JWT/202, chờ Finance và không hiển thị đã nhập học.

Kết quả cuối: **143/143 Java tests pass trên JDK 21, gồm 31 PostgreSQL 16.15 integration tests, không skip; 15/15 invariants; 7/7 Node regression tests; Chrome 7 trang và role guest pass**. Migration mới chạy lặp thành công, chỉ schema Admission. Log tại `C:\Users\Admin\admission-work\qa-admission-cash-20261110`; xem [cash-confirmation.md](../services/admission/docs/cash-confirmation.md) và [báo cáo file/test](tasks/completed/admission-cash-confirmation-2026-11-10.md). Các kết quả bên dưới là lịch sử đợt trước.

## Đợt cascade cancellation (09/11/2026)

ADM-QA-15, High: hủy lớp trước đây chỉ đổi status lớp, không cập nhật các đơn. Test `ClassCancellationTest` fail trước sửa; đã bổ sung cascade nguyên tử, điều kiện trước khai giảng/thiếu sĩ số, khóa lớp và chống lặp yêu cầu. PENDING chuyển CANCELLED; APPROVED chuyển CANCELLED_REFUND_PENDING, kèm yêu cầu đối soát bền vững trong Admission. Không xác nhận đã hoàn tiền hoặc đã tạo refund tại Finance.

Hợp đồng Finance hiện chưa đủ để gửi tự động: tài liệu yêu cầu invoice_id/thông tin ngân hàng nhưng Admission giữ invoice_code; backend Finance chưa triển khai refunds. Yêu cầu giữ ở NEEDS_FINANCE_REVIEW, chưa có transport hoặc acknowledgment từ Finance. Các ca PARTIAL/PAID chưa về callback và hủy hóa đơn Finance chưa kiểm chứng. Chi tiết migration, điều kiện và test: [class-cancellation.md](../services/admission/docs/class-cancellation.md).

Kết quả đợt cascade: **131/131 Java tests pass trên JDK 21, gồm 19 PostgreSQL 16.15 integration tests; 15/15 invariants; 5/5 Node regression tests**. Không có test skip; không chạy E2E toàn hệ thống hoặc Finance thật. Các con số 125/14 dưới đây thuộc đợt QA trước. Log cuối: `C:\Users\Admin\admission-work\cancel-verified-legacy.log`. Danh sách file và nghiệm thu: [admission-class-cancellation-2026-11-09.md](tasks/completed/admission-class-cancellation-2026-11-09.md).

## Cập nhật mới nhất — QA riêng Admission, tiếp tục phiên bị ngắt

Ngày 09/11/2026, nhánh `feature/admission`, không commit/push/merge. Đã kiểm tra và giữ toàn bộ thay đổi trước đó, lưu snapshot 64 file ngoài repo. Không sửa code Finance/Student/Learning/shared, không chạy E2E toàn hệ thống.

**Kết quả:** 125/125 Java tests pass trên JDK 21 (gồm 14 test PostgreSQL 16.15 thật, không skip), 15/15 invariants, 5/5 Node frontend regressions. Chrome kiểm tra thành công 7 trang Admission, export có JWT, role guest và luồng giao diện thiết lập mật khẩu với API fixtures. Migration audit đã chạy lặp hai lần thành công, không xóa dữ liệu.

Luồng tài khoản mới đã được HTTP/SMTP loopback/PostgreSQL kiểm chứng: đăng ký, gửi lại xác minh, xác minh email, thiết lập mật khẩu, quên/reset/đổi mật khẩu; hash/purpose/token một lần, deadline, replay đồng thời và khóa tài khoản. Không gửi thư ra ngoài; cấu hình SMTP nhà cung cấp chưa được kiểm chứng. Cần áp dụng migration audit trước khi bật luồng email trên DB khác.

Đánh giá **riêng Admission: regression hiện đạt, toàn bộ BA/BI vẫn hoàn thiện một phần**. Còn thiếu hoàn thiện BI cohort/doanh thu, cascade hủy lớp, retry sync thực sự và policy/cổng xác nhận tiền mặt. Đây là thiếu chức năng/khác biệt BA, không phải E2E toàn hệ thống chưa chạy. Chi tiết đối chiếu 30 FR: [admission-quality-progress.md](../services/admission/docs/admission-quality-progress.md). Cấu hình/migration: [account-email-setup.md](../services/admission/docs/account-email-setup.md).

| Mã | Mức độ | Kết quả, nguyên nhân và kiểm chứng |
|---|---|---|
| ADM-QA-10 | High | **Đã sửa/kiểm chứng:** thiếu luồng gửi email/thiết lập tài khoản khách. HTTP đi qua `AccountLifecycleService` + SMTP; token mới chỉ lưu hash, purpose riêng và có khóa chống dùng lại. API đăng ký trước sửa không chạy thành công; hiện luồng SMTP/DB thật pass. |
| ADM-QA-11 | High | **Đã sửa/kiểm chứng:** LOGIN_FAILED bị rollback khi service ném BusinessException; test PG trước sửa đếm 0 thay vì 1. `login/refresh` giữ thay đổi bảo mật khi từ chối nghiệp vụ. Audit CHECK thiếu action email/CREATE_USER đã bổ sung bằng migration; test tạo nhân sự/audit và brute-force 5 lần sai pass. |
| ADM-QA-12 | High/Medium | **Đã sửa/kiểm chứng:** HTML từ tên khóa học, trường câu hỏi sai, lookup giáo viên gọi API Admin/sai shape, nút lớp đề xuất dùng sai ID, export thiếu JWT, 401 đăng nhập bị chuyển trang ngoài ý muốn. Các test Node fail trước sửa, hiện pass; Chrome kiểm tra các trang/validation với fixtures. Không coi smoke test là đã thử mọi nút/CRUD. |
| ADM-QA-13 | High | **Đã sửa/kiểm chứng:** CSV có thể thực thi công thức từ dữ liệu người dùng. Unit test trước sửa fail; `CsvExportHelper` thêm tiền tố literal cho trường nguy hiểm và giữ escaping CSV thông thường; 2 test security pass. |
| ADM-QA-14 | Test fixture | **Đã sửa:** lần bị ngắt có 124 tests, 1 failure đổi mật khẩu (200 mong đợi, 403 thực tế). Audit sai đã được commit đúng, nhưng các test dùng chung 127.0.0.1 nên tích lũy ngưỡng brute-force. Tách IP nguồn từng test, giữ audit cũ/guard; thêm test khẳng định 5 lần sai vẫn bị chặn. Lần mới 125 tests pass. |

Bằng chứng mới tại `C:\Users\Admin\admission-work\qa-admission-resume-20261109\`: `java-verification.log`, `frontend-node.log`, `frontend-chrome.log`, `invariants.log`. Kết quả phía dưới là lịch sử các đợt QA trước; nhận xét cũ “email chưa triển khai” không còn là trạng thái của luồng HTTP hiện tại.

## Đợt chuẩn bị V1 trước đây (lịch sử 09/11/2026)

**NOT READY FOR V1** cho phát hành toàn hệ thống. Ba vấn đề ADM-QA-06..08 đã được sửa và kiểm chứng; lỗi JSONB ADM-QA-09 phát hiện khi tích hợp cũng đã sửa. Tuy nhiên chưa nghiệm thu Finance/Student thật và giao diện qua Gateway; email khách mới chưa được xác minh, luồng gửi email khôi phục tài khoản chưa hoạt động. Không đánh đồng kiểm thử HTTP Admission với E2E đầy đủ.

- **110/110 test Java pass, 0 failure/error/skip**, chạy bằng Microsoft OpenJDK **21.0.13**, Maven 3.9.9. Trong đó có **3 test PostgreSQL thật**, dùng Spring Boot server trên cổng HTTP ngẫu nhiên và DB PostgreSQL **16.15** riêng. Không cần cờ Byte Buddy experimental của JDK 25.
- **15/15 invariant test pass**. Kiểm tra cú pháp JavaScript inline của `frontend/test.html` bằng Node pass; `git diff --check` pass.
- Giữ nguyên 8 test hồi quy đợt đầu: hash của `AdmissionRegressionTest`, `WebhookSecurityTest`, `AuthServiceTest` trùng bản snapshot trước phiên làm việc. Năm bản sửa cũ vẫn còn; không hoàn tác các thay đổi chưa commit. Chỉ cập nhật fixture/mock của `EnrollmentServiceTest` để dùng query khóa mới, không sửa assertion cũ.
- Trước sửa: 5 test mới tái hiện deadline/giữ chỗ/quyền thi đều fail; thêm test tranh chấp trên source snapshot cũ cũng fail với DB thật. HTTP test phát hiện lỗi JSONB trước khi sửa. Log nằm tại `C:\Users\Admin\admission-work\qa-v1\`: `before-tests.log`, `postgres-before.log`, `postgres-after.log`, `verified-v1.log`, `invariants.log`.
- Docker Compose config hợp lệ, nhưng Docker Linux engine vẫn không có named pipe. Đã dùng PostgreSQL portable theo [kênh binaries EDB](https://www.enterprisedb.com/download-postgresql-binaries), bind `127.0.0.1:55432`, SCRAM, database QA riêng `admission_qa_v1`; đã nạp nguyên `scripts/init-db.sql`. Không đụng DB vận hành, không sửa schema/phân hệ khác.
- Sau nghiệm thu đã dừng riêng PostgreSQL QA; giữ nguyên data directory, source snapshot và log trong `qa-v1`, không xóa dữ liệu. Spring Boot server của integration test cũng đã kết thúc.
- Trong integration test, **FinanceServiceClient và StudentServiceSyncHelper được mock**; ORM/repository/transaction, security chain, HTTP server, JSONB và PostgreSQL là thật. Scheduler bị tắt trong test để xác nhận deadline độc lập với job.
- Không commit, push, merge, fetch mới hoặc đổi nhánh main trong phiên này. Source vẫn ở `feature/admission`, baseline `84af6e9`.

### Điều kiện còn thiếu trước khi chốt V1

1. Chạy lại trên môi trường Docker/CI chuẩn và nghiệm thu luồng thực: đăng ký -> Finance tạo hóa đơn -> thanh toán -> callback có secret -> Admission duyệt -> Student đồng bộ; kiểm tra retry, timeout và giao dịch lỗi. Test hiện tại chưa xác nhận các hợp đồng này.
2. Nghiệm thu trình duyệt qua Gateway, đặc biệt payload `attempt_token`, khách mới, email đã có tài khoản, đăng nhập lại và thi lại. Kiểm tra cú pháp JavaScript không thay thế kiểm thử UI.
3. Hoàn thiện và nghiệm thu xác minh email/khôi phục tài khoản khách. Hiện khách được tạo tài khoản với mật khẩu ngẫu nhiên nhưng chưa có kênh email nhận link reset; chính sách yêu cầu đăng nhập cho email tồn tại bảo vệ danh tính nhưng còn vướng luồng quay lại thi. Không tự bỏ kiểm tra sở hữu để làm luồng này chạy.
4. Cấu hình secret riêng đủ mạnh và đồng nhất trên môi trường release (JWT/internal); các giá trị fallback có sẵn trong repository không phải bằng chứng cấu hình triển khai đã đạt yêu cầu. Không thay cấu hình dùng chung của phân hệ khác trong đợt này.

Hướng chạy lại và giới hạn của fixture PostgreSQL: [testing-v1.md](../services/admission/docs/testing-v1.md). Các phần “đợt QA đầu” bên dưới giữ lại làm lịch sử; kết quả mới nhất ở mục này thay thế trạng thái môi trường/test cũ.

- Ngày: 09/11/2026 (Asia/Saigon).
- Repository: `https://github.com/TamDaE2k5/english_center`.
- Nhánh: `feature/admission`, baseline `84af6e9` (đã fetch, đồng bộ fast-forward với origin).
- Bản làm việc hiện tại: `C:\Users\Admin\admission-work\english_center` (đã di chuyển từ thư mục product-classifier).
- Thư mục cha là dự án khác, có nhiều tệp chưa commit và không có origin. Đã clone riêng, không stash/reset/ghi đè công việc cũ. Bản clone sạch trước khi QA; không commit, push, merge hoặc thay đổi main.

## Phạm vi và phương pháp

Đọc `AGENTS.md`, README, `docs/{contract,agent-workflow,state}.md`, tài liệu kỹ thuật Admission, task khảo thí và task kiểm soát sĩ số. Kiểm tra các tầng Security, Controller, Service, Repository, DTO và cấu hình chạy ứng dụng; đối chiếu frontend thi trực tuyến đang gửi `attempt_id`.

Chạy invariant Python và toàn bộ test Java Admission. Test hồi quy gọi service thật với repository mock; test webhook chạy HTTP MockMvc qua Spring Security thật. Những test này không thay thế kiểm thử PostgreSQL hoặc E2E trình duyệt.

Mức độ: **Critical** = có thể thay đổi nghiệp vụ thanh toán không cần xác thực; **High** = sai trạng thái/điểm/danh tính hoặc lộ thông tin xác thực; **Medium** = tác động hạn chế. Mục “cần xác minh tích hợp” là phát hiện từ code, chưa khẳng định đã tái hiện trên hệ thống triển khai.

## ADM-QA-01 — Webhook thanh toán không xác thực nguồn gửi

- **Mức độ:** Critical. **Trạng thái:** Đã sửa, xác nhận bằng test HTTP trước/sau.
- **Vị trí:** `services/admission/backend/src/main/java/com/englishcenter/admission/config/SecurityConfig.java#securityFilterChain`, `controller/WebhookController.java#handlePaymentSuccess`.
- **Tái hiện:** Gửi POST `/api/admission/webhooks/payment-success`, JSON `{"event_id":"qa-event","enrollment_id":1}`, không JWT, không `X-Internal-Secret` hoặc secret sai. Trên mã gốc, MockMvc trả 200 và gọi service; nếu đơn hợp lệ, đường nghiệp vụ có thể duyệt đơn mà không có nguồn Finance đáng tin cậy.
- **Kỳ vọng:** 403 trước khi nghiệp vụ được gọi; Finance có secret đúng vẫn truy cập được mà không cần JWT người dùng.
- **Nguyên nhân:** Security `permitAll` toàn bộ webhook/internal, Controller và Service không xác minh secret.
- **Sửa:** Thêm `InternalSecretFilter` trong security chain, kiểm tra header theo `internal.secret-key`, so sánh bằng `MessageDigest.isEqual`, từ chối secret cấu hình rỗng. Bảo vệ cả hai nhóm webhook/internal; không thay đổi các service khác.
- **Kiểm chứng:** `WebhookSecurityTest`: thiếu secret, secret sai, secret đúng. Hai test từ chối thất bại trên mã gốc (200 thay vì 403).
- **Lưu ý vận hành:** Finance phải gửi `X-Internal-Secret` trùng cấu hình Admission. Cơ chế này vẫn dựa vào việc triển khai secret đủ mạnh; không thay đổi cấu hình secret dùng chung của các phân hệ trong đợt QA này.

## ADM-QA-02 — Đơn EXPIRED vẫn được duyệt thủ công

- **Mức độ:** High. **Trạng thái:** Đã sửa, xác nhận bằng unit test trước/sau.
- **Vị trí:** `service/impl/EnrollmentServiceImpl.java#approveEnrollment` (đường dẫn Java đều dưới package Admission).
- **Tái hiện:** Đơn có status `EXPIRED`, lớp còn chỗ; gọi lõi duyệt hoặc API POST `/api/admission/enrollments/{id}/approve-manual` bằng Staff. Mã gốc chuyển sang `APPROVED`.
- **Kỳ vọng:** `ADM_INVALID_STATE_TRANSITION`, không lưu hoặc đồng bộ học viên.
- **Nguyên nhân:** Danh sách trạng thái bị cấm bỏ sót `EXPIRED`.
- **Sửa:** Chỉ cho `PENDING` đi tiếp; giữ hành vi idempotent cho đơn đã `APPROVED`.
- **Kiểm chứng:** `AdmissionRegressionTest.cannotApproveExpiredEnrollment`; trước sửa không ném lỗi, sau sửa từ chối và không gọi save.

## ADM-QA-03 — Lặp câu trả lời làm tăng điểm thi

- **Mức độ:** High. **Trạng thái:** Đã sửa, xác nhận bằng unit test trước/sau.
- **Vị trí:** `service/impl/PlacementTestServiceImpl.java#submitAndGrade`.
- **Tái hiện:** Với lượt thi còn hạn và câu 7 có đáp án A, trọng số 1, nộp `answers=[{"question_id":7,"selected_option":"A"},{"question_id":7,"selected_option":"A"}]`. Mã gốc cộng 2 điểm cho một câu.
- **Kỳ vọng:** Từ chối payload sai bằng `ADM_VALIDATION_ERROR`, không lưu kết quả.
- **Nguyên nhân:** Cộng điểm theo từng phần tử trong danh sách đầu vào, không kiểm tra `question_id` trùng.
- **Sửa:** Theo dõi ID đã trả lời bằng Set, từ chối khi gặp trùng.
- **Kiểm chứng:** `AdmissionRegressionTest.rejectsDuplicateQuestionAnswers`; test `gradesValidAttempt` bảo đảm bài hợp lệ vẫn nhận đúng 1 điểm.

## ADM-QA-04 — Bỏ attempt_id để vượt qua countdown

- **Mức độ:** High. **Trạng thái:** Đã sửa, xác nhận bằng unit test trước/sau.
- **Vị trí:** `service/impl/PlacementTestServiceImpl.java#submitAndGrade`.
- **Tái hiện:** POST `/api/admission/tests/public/submit` với `test_set_id`, email, họ tên và answers, bỏ `attempt_id`. Mã gốc đi vào nhánh nộp trực tiếp và chấm điểm, không có thời hạn/lượt thi để kiểm tra.
- **Kỳ vọng:** Bắt buộc lượt thi do server cấp, kiểm tra hết hạn và chống nộp lại theo `INV-TST-02`.
- **Nguyên nhân:** Nhánh tương thích nộp trực tiếp bỏ toàn bộ kiểm tra attempt.
- **Sửa:** Từ chối thiếu attempt bằng `ADM_VALIDATION_ERROR`, bỏ đường chấm điểm trực tiếp.
- **Kiểm chứng:** `AdmissionRegressionTest.requiresServerAttempt`; không tải câu hỏi và không lưu bài.
- **Tương thích:** Frontend `services/admission/frontend/test.html` đã gửi `attempt_id`. Client cũ và ví dụ JSON thiếu attempt trong task ADM-31 phải gọi start trước rồi gửi ID được trả về.

## ADM-QA-05 — Reset token xuất hiện trong log

- **Mức độ:** High. **Trạng thái:** Đã sửa, xác nhận bằng test ghi nhận output trước/sau.
- **Vị trí:** `service/impl/AuthServiceImpl.java#forgotPassword`.
- **Tái hiện:** Gọi forgot-password cho email tồn tại, xem log INFO. Mã gốc ghi nguyên token có thể dùng đặt lại mật khẩu trong 15 phút.
- **Kỳ vọng:** Log không chứa token xác thực.
- **Nguyên nhân:** Lệnh log dùng cả `normalizedEmail` và `token` làm tham số.
- **Sửa:** Chỉ ghi user ID và việc tạo yêu cầu, bỏ token khỏi log.
- **Kiểm chứng:** Test mới `AuthServiceTest.forgotPasswordDoesNotLogResetToken` bắt token được lưu và kiểm tra token đó không xuất hiện trong output; fail trên mã gốc, pass sau sửa. Giữ nguyên assertion của các test cũ.
- **Giới hạn:** Đây không phải triển khai gửi email. Luồng gửi link reset cho người dùng vẫn cần hoàn thiện riêng.

## ADM-QA-06 — Vượt số chỗ giữ khi đăng ký đồng thời

- **Mức độ:** High. **Trạng thái:** Đã sửa; đã tái hiện và kiểm chứng PostgreSQL thật.
- **Vị trí:** `EnrollmentServiceImpl#createEnrollment`, `#extendHold`, `#approveEnrollment`, `#processPaymentSuccess`, `#cancelEnrollment`; repository refresh trạng thái.
- **Tái hiện trước sửa:** Lớp max=1, transaction A dừng sau khi đọc capacity; transaction B đăng ký trong lúc A chưa commit. Test trên bản sao QA của source trước sửa cho thấy B hoàn tất trước khi A được giải phóng; cả hai tạo đơn PENDING. Log `postgres-before.log`: 1 test fail, không lỗi hạ tầng.
- **Nguyên nhân:** Đọc số slot và tạo/khôi phục đơn không được serialize theo lớp; gia hạn chỉ tính APPROVED, bỏ qua PENDING của người khác.
- **Sửa:** `PESSIMISTIC_WRITE` trên lớp trước khi đếm và tạo/gia hạn; gia hạn tính toàn bộ occupied và trừ slot đang có của chính đơn. Các thao tác có đơn tồn tại khóa theo thứ tự lớp -> đơn, refresh entity từ DB sau khi chờ khóa để không sử dụng trạng thái cũ của persistence context. Callback kiểm tra lại idempotency sau khóa.
- **Kiểm chứng:** `AdmissionPostgresTest.concurrentRegistrationCannotOversellLastSlot`: B phải chờ, A thành công, B nhận `ADM_CLASS_FULL`, DB chỉ có 1 đơn PENDING. `EnrollmentV1Test.cannotRestoreExpiredHoldIntoFullClass`: không gia hạn lấn slot khác, test fail trước sửa/pass sau sửa.
- **Giới hạn:** Kiểm chứng hai transaction cạnh tranh cùng lớp; chưa có stress test nhiều instance hoặc đo thời gian giữ khóa khi Finance/Student chậm.

## ADM-QA-07 — API thi công khai không xác minh quyền sở hữu lượt thi

- **Mức độ:** High. **Trạng thái:** Đã sửa; xác nhận test HTTP, token thật và PostgreSQL.
- **Vị trí:** `PublicPlacementTestService`, `PlacementAttemptToken`, `UserServiceImpl#createGuestStudentUser`, DTO và `frontend/test.html`.
- **Tái hiện trước sửa:** Khách gửi start với `user_id` người khác hoặc submit chỉ với attempt ID đoán được; hai test MockMvc trước sửa đều nhận 200 thay vì 403 (`before-tests.log`).
- **Nguyên nhân:** Tin user ID/email tự khai và attempt ID tăng dần, không có bằng chứng chính chủ.
- **Sửa:** API công khai đi qua service xác thực riêng. Người đăng nhập lấy ID từ principal; không chấp nhận user ID khác. Khách chỉ được tạo tài khoản mới; email đã tồn tại phải đăng nhập, không được tái sử dụng tài khoản bằng email tự khai. INSERT có UNIQUE(email) là chốt chặn race, không fallback lấy tài khoản tồn tại. Server cấp `attempt_token` ký bằng khóa dẫn xuất riêng, gắn ID và deadline, chỉ có quyền nộp lượt thi; không dùng được như access JWT. Submit cần principal chính chủ hoặc token hợp lệ của đúng attempt. Frontend giữ token trong bộ nhớ và gửi cùng payload; không ghi log/localStorage.
- **Kiểm chứng:** 2 test HTTP từ chối giả danh; 10 test service/token cho token đúng, token giả, token hết hạn, sai attempt, owner/non-owner, start theo principal, khách mới và race email. HTTP thật trên PostgreSQL: start -> thiếu token 403 -> token đúng 200 -> lưu đúng kết quả -> nộp lại 400 `ADM_TEST_ALREADY_SUBMITTED`; email đã có tài khoản không được khách mạo danh.
- **Giới hạn danh tính:** Token chứng minh quyền sở hữu lượt thi, không chứng minh quyền sở hữu địa chỉ email mới. Email khách vẫn chưa được xác minh. Không phát hành access JWT cho khách từ token thi.
- **Ảnh hưởng API:** Client khách phải gửi `attempt_token` từ response start. Client cũ chỉ gửi ID bị từ chối. Tài khoản khách đã tạo không được thi lại bằng cách khai email; cần đăng nhập hoặc hoàn thiện luồng xác minh/khôi phục tài khoản. Việc gửi email reset hiện chưa được triển khai (xem điều kiện V1).

## ADM-QA-08 — PENDING quá hạn vẫn được duyệt trước khi scheduler chạy

- **Mức độ:** High. **Trạng thái:** Đã sửa; unit test trước/sau và PostgreSQL thật.
- **Vị trí:** `EnrollmentServiceImpl#isHoldOverdue`, `#approveEnrollment`, `#processPaymentSuccess`.
- **Tái hiện:** Đơn PENDING có deadline đã qua, lớp còn chỗ; gọi duyệt hoặc gửi callback thanh toán trước job. Trước sửa: duyệt được, callback trả APPROVED; cả hai test mới fail.
- **Nguyên nhân:** Dùng status làm nguồn quyết định, bỏ qua deadline server; kết quả phụ thuộc scheduler.
- **Sửa:** Sau khóa và refresh, duyệt phải kiểm tra deadline (`<= now` là quá hạn). Duyệt tay trả `ADM_INVALID_STATE_TRANSITION` và rollback. Webhook chuyển đơn quá hạn sang EXPIRED trong transaction, ghi event và ghi chú đối soát; không gọi Student sync, không tự duyệt.
- **Kiểm chứng:** `EnrollmentV1Test.rejectsOverduePendingApproval`, `latePaymentIsRecordedWithoutApproval`; `AdmissionPostgresTest.overduePendingCannotBeApprovedAndLateWebhookIsReconciled` tắt scheduler, xác nhận DB không APPROVED, callback ghi EXPIRED/event, replay không tạo event thứ hai.
- **Legacy:** Deadline null vẫn theo quy ước giữ chỗ hiện hành của repository, chưa đổi dữ liệu/schema legacy.

## ADM-QA-09 — Nộp bài gây 500 khi ghi answers_detail vào PostgreSQL JSONB

- **Mức độ:** High. **Trạng thái:** Đã sửa, xác nhận trên PostgreSQL thật.
- **Vị trí:** `entity/TestSubmission.java#answersDetail`.
- **Tái hiện:** Start khách hợp lệ, nộp bài có token hợp lệ. HTTP test nhận 500; log PostgreSQL SQLState `42804`: cột JSONB nhận expression `character varying` (`postgres-after.log`).
- **Nguyên nhân:** `columnDefinition=JSONB` chỉ mô tả DDL; field Java String vẫn được bind như VARCHAR.
- **Sửa:** Thêm `@JdbcTypeCode(SqlTypes.JSON)` để Hibernate bind JSON đúng kiểu, không thay schema hoặc driver cấu hình dùng chung.
- **Kiểm chứng:** HTTP test giữ nguyên kỳ vọng 200 và truy vấn DB `jsonb_array_length(answers_detail)=1`; pass sau sửa, điểm lưu 1, không thêm bài nộp khi replay.

## Kết quả kiểm thử và giới hạn đợt QA đầu (lịch sử)

- Invariant: `python -m unittest discover -s tests/invariants -p "test_*.py" -v`: **15/15 pass**, cả trước và sau sửa. Đây là mô hình SQLite và kiểm tra tĩnh, không chứng minh mã Java đúng.
- Hồi quy trên mã gốc: 7 test ở hai lớp mới, **5 fail / 2 pass**, không lỗi môi trường; xác nhận ADM-QA-01..04. Test log bổ sung **1 fail** trước khi sửa ADM-QA-05.
- Toàn bộ Java sau sửa: xem kết quả cuối trong phần nghiệm thu bên dưới.
- Môi trường: Maven 3.9.9 tải cục bộ; máy có Java 17 và 25, không có Java 21. Dùng JDK 25 với compiler `release 21` và `-Dnet.bytebuddy.experimental=true` để Mockito/Byte Buddy cũ chạy được. Cần chạy lại trên Java 21 trong CI trước phát hành; không sửa POM chỉ để thích nghi máy QA.
- `docker compose config --quiet`: pass. `docker compose up -d postgres admission-service`: fail vì không có pipe `dockerDesktopLinuxEngine`, dù `docker desktop start` báo already running.
- Đã thử chạy Spring Boot trên cổng QA 18081, DB_HOST=127.0.0.1, Hikari timeout 3000 ms: **không khởi động hoàn chỉnh**, PostgreSQL `127.0.0.1:5432` connection refused. Không có dịch vụ QA đang phục vụ thành công để kiểm tra trình duyệt.
- Chưa kiểm thử được E2E UI, DDL/trigger PostgreSQL, inter-service Finance/Student, khả năng ghi JSONB và kiểm thử đồng thời. Không kết luận các luồng này pass.
- Log QA nằm ngoài bản clone tại `../english-center-qa-tools/`; phần cache/công cụ còn lại nằm tại `../english-center-qa-cache/`. Log có thể chứa dữ liệu test nhạy cảm từ phiên trước sửa, chỉ dùng cục bộ, không commit.

## Nghiệm thu đợt QA đầu (lịch sử)

- Java Admission: **92 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**. Có 8 test mới (4 service, 3 HTTP security, 1 bảo vệ log). 84 test sẵn có vẫn pass.
- Lần baseline ban đầu với JDK 25 chưa bật Byte Buddy experimental có 29 lỗi hạ tầng Mockito trong 84 test; đã khắc phục bằng tham số chạy nêu trên, không sửa assertion hoặc dependency để che lỗi nghiệp vụ.
- Python invariants: **15/15 pass**; `git diff --check`: pass.
- Lệnh chuẩn trên máy có Maven và Java 21: `mvn -B -f services/admission/backend/pom.xml test`.
- Lệnh QA đã chạy từ gốc repo bằng PowerShell: đặt `JAVA_HOME` tới `C:\Program Files\Java\jdk-25`, gọi Maven 3.9.9 với `-B -Dnet.bytebuddy.experimental=true -Dmaven.repo.local=.qa-tools/m2 -f services/admission/backend/pom.xml test`. Sau kiểm thử, toàn bộ `.qa-tools` đã được chuyển ra ngoài repo để không lẫn vào diff bàn giao.
- Log cuối: `../english-center-qa-tools/verified-maven.log`; log tái hiện: `confirmed-before.log`, `reset-token-before.log`; log khởi động thất bại: `application-start.log` trong cùng thư mục công cụ.
- Đã sửa 5 lỗi xác nhận; 3 phát hiện cần kiểm thử tích hợp còn mở. Chỉ sửa mã/test Admission và thêm tài liệu báo cáo; không đổi phân hệ khác, shared assets, contract hoặc main.
