# H01 — Callback thanh toán và chu kỳ hóa đơn — 11/11/2026

Trạng thái hạng mục: **PARTIAL**. Các guard, transaction, idempotency và journal thuộc Admission đã sửa. Chưa chứng nhận số dư/thu đủ tiền hoặc chu kỳ hóa đơn mới từ Finance thật. Không tổng nghiệm thu Admission V1.

## Căn cứ và phạm vi hợp đồng

- Baseline sạch `cbf5be7c2e956397cffe4e962a12c2acce197953`, nhánh `feature/admission`; giữ nguyên H02/H03. Audit gốc: `C:/Users/Admin/admission-work/qa-admission-final-audit-20261110/admission-v1-final-audit.md`, H01.
- BA `FR-ENR-06`, schema `enrollments`, `processed_finance_events`, `INV-ENR-02/03`, `INV-ADM-01/03`; TASK-ADM-45 và TASK-ADM-03.
- Dùng endpoint **đã có** `POST /api/admission/webhooks/payment-success`, header `X-Internal-Secret`. TASK-ADM-45 mô tả đây là thông báo hóa đơn đã PAID nhưng ví dụ JSON không có trường status. Vì vậy status vắng mặt giữ semantics PAID ngầm; nếu gửi `invoice_status` hoặc alias `status` thì phải đúng `PAID`. Không coi sự có mặt của shared secret là đối soát số dư.
- TASK-ADM-45 dùng `invoice_id: "INV-2026-0120"` trong khi catalog #50 dùng số 55. Admission hiện chỉ lưu `enrollments.invoice_code`; DTO có alias `invoice_code`. Guard đối chiếu **mã hóa đơn chính xác**, không tự suy diễn numeric invoice ID thành invoice code. Mapping số/mã cần Finance chốt trước tích hợp thật.
- TASK-ADM-03 quy định auto-generate idempotent theo enrollment_id, trả invoice_id + invoice_code. Không có hợp đồng thế hệ hóa đơn, tái phát hành sau hủy, số dư authoritative hoặc endpoint truy vấn đối soát đủ rõ để Admission tự dùng.

## Lỗi xác nhận và sửa

| Phát hiện | Mức độ, vị trí | Tái hiện/nguyên nhân trước sửa | Sửa và bằng chứng |
|---|---|---|---|
| ADM-QA-25 / H01 evidence | High, EnrollmentServiceImpl + webhook DTO | Callback authenticated có invoice khác hoặc amount âm vẫn duyệt; chỉ kiểm tra nguồn, chưa kiểm tra evidence | Test PG `callbackWrongInvoiceMustNotApprove`, `callbackNegativeAmountMustNotApprove` đỏ trước sửa. Chặn invoice thiếu/sai, amount null/0/âm, paid_at thiếu/trước enrolled_at/tương lai, explicit status khác PAID. Không ghi đè invoice_code từ callback |
| ADM-QA-26 / H01 replay binding | High, service + ProcessedFinanceEventRepository | Event đã lưu dùng enrollment_id khác vẫn trả theo payload mới, không so binding/evidence | Test `callbackEventCannotMoveToAnotherEnrollment` đỏ trước sửa. Receipt mới có SHA-256 canonical fingerprint; cùng event phải cùng enrollment/invoice/amount/paid_at/transaction_ref/effective status. Khóa transaction PG trước lookup, kể cả khác lớp |
| ADM-QA-27 / H01 cycle | High, createEnrollment | Tái dùng row đóng + invoice cũ khi Finance lỗi có thể đưa thanh toán cũ vào chu kỳ mới | Bỏ tái kích hoạt row đã đóng/quá hạn. Trả ADM_INVALID_STATE_TRANSITION, giữ nguyên invoice, deadline, receipt, cash request, outbox và lịch sử. Test `callbackCycleCannotReuseClosedEnrollment`. Đây là chặn an toàn, **chưa triển khai đăng ký lại với hóa đơn mới** |
| ADM-QA-28 / H01 journal | High, processPaymentSuccess | Chỉ CANCELLED/EXPIRED có nhánh đối soát; REJECTED/refund-pending hoặc event mới sau APPROVED xử lý thiếu journal | Receipt + ghi chú đối soát một lần theo event cho trạng thái không còn PENDING/lớp CANCELLED; không hồi sinh đơn, không đổi approved_at/approval_method. Test terminal states và out-of-order |

Log tái hiện: `C:/Users/Admin/admission-work/qa-admission-callback-20261111/red.log`: **3 tests, 3 failures**, không lỗi fixture. Các test bổ sung kiểm tra những nhánh liên quan; không tuyên bố tất cả đã chạy đỏ trên baseline.

## Transaction, bảo mật và triển khai

1. Validation xong mới mở transaction callback riêng qua TransactionTemplate REQUIRES_NEW. Khóa advisory `(8081,2)` trước lookup event rồi khóa class/refresh enrollment theo quy trình hiện có. Khóa booking `(8081,1)` của H02/H03 giữ nguyên.
2. Approval, Student outbox và receipt cùng commit/rollback. Lỗi kỹ thuật lưu receipt rollback mọi thay đổi; callback có thể thử lại. Khóa được PostgreSQL nhả khi transaction kết thúc.
3. BusinessException ghi `finance_callback_audit` trong transaction độc lập **sau khi** transaction nghiệp vụ đã rollback/nhả connection. Audit lưu event_id, enrollment_id, mã lỗi và thời điểm; không ghi body, amount, transaction_ref, secret hoặc token vào log/audit mới. Audit là journal tối thiểu, không phải ledger tiền.
4. Payload bị security filter, Jackson hoặc Bean Validation chặn trước service không được tuyên bố có audit nghiệp vụ mới. Lỗi database/audit không trả thành công giả; yêu cầu phải thử lại/điều tra.
5. Replay exact cùng evidence trả trạng thái hiện tại, không duyệt/lưu receipt lại. BigDecimal khác scale được canonical hóa. Receipt cũ `payload_hash IS NULL` giữ nguyên dữ liệu nhưng trả lỗi cần đối soát vì không còn đủ evidence để xác minh replay. Không tự backfill hash giả.
6. Chỉ PENDING còn hạn, invoice khớp, callback PAID theo hợp đồng và guard sĩ số/class đạt mới tự APPROVED. Callback muộn dù paid_at trước deadline vẫn không tự hồi sinh đơn; giữ chính sách hiện có, cần quyết định nghiệp vụ cho việc ưu tiên thời điểm trả tiền hay nhận callback.
7. Migration bắt buộc: `backend/src/main/resources/db/admission-payment-callback.sql` sau các migration Admission hiện có. Thêm nullable `processed_finance_events.payload_hash` và bảng journal **Admission**, không tạo hóa đơn Finance. `ddl-auto=none`, phải áp dụng trước deploy bản code này. Chạy lại migration trên DB QA đã có dữ liệu thành công; không xóa dữ liệu.
8. Bảo đảm concurrency áp dụng cho writer Admission cùng database ở READ COMMITTED, dùng cùng khóa. Khóa chung serialize callback toàn service; chưa benchmark throughput. Không bảo vệ SQL/script ngoài service bỏ qua protocol khóa.

## Kiểm thử

Kết quả cuối: **185/185 Java tests pass với JDK 21.0.13, gồm 72 PostgreSQL 16.15 tests, không failure/error/skip**. Migration replay thành công; **15/15 invariants; 10/10 Node; Chrome 7 trang và BI STAFF/ADMIN/guest với API fixtures pass**. `git diff --check` pass. Chrome ban đầu bị sandbox chặn localhost, chạy lại ngoài sandbox thành công. Lần Java trong sandbox bị lỗi pipe JVM attach, không dùng làm bằng chứng pass; lần cuối ngoài sandbox BUILD SUCCESS.

Test PG mới (14): wrong invoice; negative amount; event đổi enrollment; changed replay/audit; explicit PARTIAL + old paid_at; terminal journal; duplicate concurrent; strict identity; closed cycle; receipt failure rollback + outbox; cross-class concurrent binding; out-of-order/legacy receipt; HTTP auth/missing/zero/future/status; 12 concurrent rejections không cạn pool audit. Các fixture callback cũ chỉ bổ sung invoice/amount/paid_at hợp lệ; giữ nguyên assertion nghiệp vụ trước đó.

Phạm vi proof: Spring Boot HTTP/security/JPA/transaction/PostgreSQL/SMTP loopback là thật. Finance và Student clients được mock; browser dùng API fixtures; invariant suite là model/static/SQLite, không thay PG. Không chạy E2E toàn hệ thống.

Log tại `C:/Users/Admin/admission-work/qa-admission-callback-20261111/`: `red.log`, `java-first.log` (180/67, trước bổ sung stress/rollback), `java-final.log`, `migration-replay.log`, `invariants.log`, `frontend-node.log`, `frontend-chrome-verified.log`.

## Phần còn phải chốt/kiểm chứng

| Việc | Trạng thái | Trách nhiệm/điều kiện |
|---|---|---|
| Chặn mismatch, malformed/âm/0, altered replay, trạng thái muộn; atomic receipt/approval/outbox | DONE trong phạm vi guard Admission | 14 regression tests mới, toàn bộ 185 Java tests pass; không có sửa Finance/Student/Learning |
| Xác minh tiền đã thu đủ, tiền tệ/scale, tổng thanh toán so với số phải thu, transaction_ref duy nhất toàn Finance | BLOCKED | Finance là nguồn tiền; `amount` optional trong InvoiceData chưa phải hợp đồng số dư authoritative. Amount dương **không chứng minh thu đủ**. Không so amount với tuition, không dựng endpoint truy vấn mới |
| numeric invoice_id ↔ invoice_code; explicit PAID/partial/cumulative semantics; paid_at timezone/clock skew | BLOCKED tích hợp | Finance/Admission phải thống nhất version contract. Guard hiện so LocalDateTime với đồng hồ Admission, không tự bù skew hoặc diễn giải timezone chưa chốt |
| Đăng ký lại user/class đã đóng/quá hạn, thế hệ hóa đơn và idempotency key mới | PARTIAL/BLOCKED | Guard Admission đã chặn reuse; UNIQUE(user,class) + idempotency Finance theo enrollment_id chưa cho phép tạo chu kỳ mới an toàn. Cần quyết định nghiệp vụ và hợp đồng, rồi mới thiết kế migration/state history phù hợp |
| Thu trước hạn nhưng callback muộn/slot đầy; cash/refund journal được Finance xử lý thực | PARTIAL | Giữ chính sách không tự APPROVED sau deadline; slot đầy rollback/ADM_CLASS_FULL có audit, không ghi receipt thành công. Chưa có workflow xử lý/ACK Finance thật. Không đánh dấu cash request đã được Finance xác nhận hoặc tạo refund giả |
| Receipt legacy thiếu fingerprint | PARTIAL | Nhận diện và audit lỗi; cần đối soát thủ công với Finance để xử lý replay cũ, chưa có công cụ reconciliation tự động |

Không thay trạng thái tổng nghiệm thu V1. Không commit/push/merge trong đợt này.
