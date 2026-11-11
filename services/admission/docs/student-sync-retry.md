# Retry hồ sơ Student — Admission, 10/11/2026

**PARTIAL**: outbox/dispatcher Admission đã triển khai; endpoint Student thật và bảo đảm idempotency phía nhận chưa được kiểm chứng. Không sửa Student/Finance/Learning, không làm BI, không chạy E2E.

## Hợp đồng

Dùng endpoint/payload hiện có tại TASK-ADM-03 mục Student Sync và API_CATALOG Admission API 60: POST `/api/student/internal/students`, X-Internal-Secret, user_id/full_name/email/phone/date_of_birth. user_id là ID Admission, student_id trong ACK là ID hồ sơ Student riêng. Hợp đồng mô tả trả 201 khi tạo, 200 với hồ sơ hiện hữu theo user_id. BA FR-ENR-08 cũng yêu cầu không tạo trùng. Không tự đổi sang endpoint public: Student backend hiện chỉ có public POST students, bỏ qua user_id và không triển khai endpoint internal/idempotency như tài liệu.

Không tự đặt header Idempotency-Key chưa được thống nhất. user_id cố định là khóa tự nhiên theo hợp đồng; outbox PRIMARY KEY(user_id) đảm bảo một yêu cầu hồ sơ cho cùng tài khoản dù có nhiều enrollment. Payload snapshot cố định, không đổi giữa các lần gửi. Worker chỉ báo SYNCED sau ACK success có student_id dương và student_code không rỗng. ACK chưa có trường echo user_id theo contract hiện có nên không xác minh độc lập được binding hồ sơ phía Student.

## Transaction và trạng thái

Helper cũ vẫn là cổng gọi từ approveEnrollment nhưng không gọi mạng: chỉ nhận APPROVED, bắt buộc transaction MANDATORY, flush và ghi outbox cùng transaction duyệt. Lỗi DB outbox rollback duyệt/callback; không commit APPROVED thiếu journal. Student down sau commit không rollback duyệt. Yêu cầu tiền mặt chưa được Finance xác nhận không enqueue.

Outbox PENDING -> IN_FLIGHT -> SUCCEEDED/RETRY/REVIEW/EXHAUSTED. Lease 2 phút, token fencing, SELECT FOR UPDATE SKIP LOCKED để nhiều worker không gửi cùng một claim. Gọi HTTP ngoài transaction. Timeout HTTP client hiện có: connect 3s/read 5s. Tối đa 5 lần; backoff sau lần lỗi 30/60/120/240 giây, không retry sau lần thứ 5. Không sleep trong worker.

- Connection refused trước kết nối: RETRY. HTTP 429/5xx hoặc timeout/mất phản hồi chỉ retry khi operator đã kiểm chứng idempotency phía Student. Nếu chưa, REVIEW để không tự gửi lại một POST có thể đã tạo hồ sơ.
- HTTP 4xx khác/ACK sai/rỗng: REVIEW. Hết lần: EXHAUSTED. Không suy từ 409 rằng hồ sơ đã tồn tại đúng người.
- Crash sau claim: lease hết hạn sẽ REVIEW nếu chưa xác minh idempotency, hoặc RETRY sau thời gian chờ nếu đã xác minh (hết 5 lần thành EXHAUSTED). Worker cũ không ghi đè claim mới/hết hạn.
- Không còn enrollment APPROVED trong lớp chưa CANCELLED: job chờ chuyển CANCELLED, không gửi. Nếu hủy khi HTTP đã bay đi, không thể thu hồi POST: không tự hồi sinh enrollment/khẳng định đã xóa hồ sơ; cần đối soát Student. Job CANCELLED chưa gửi lần nào có thể được kích hoạt lại bởi một phê duyệt mới cùng user; nếu đã gửi thì cần kiểm tra thủ công.
- Cờ enrollment được reconcile riêng theo journal, chỉ cập nhật các enrollment APPROVED; không ghi đè CANCELLED/CANCELLED_REFUND_PENDING. Điều này tránh thứ tự khóa đảo giữa transaction duyệt và worker. Journal là nguồn trạng thái, cờ hiển thị có thể trễ một tick sau crash.

## Migration và vận hành

Chạy migration trên đúng DB trước khi dùng code mới:

```powershell
psql -v ON_ERROR_STOP=1 -f services/admission/backend/src/main/resources/db/admission-student-sync.sql
```

Chỉ thêm bảng/index Admission. Backfill APPROVED chưa synced thành REVIEW/LEGACY_UNCONFIRMED: helper cũ có thể đã gửi trước khi rollback/timeout, không tự replay dữ liệu cũ. Migration chạy lặp ON CONFLICT giữ job đã có; không xóa dữ liệu. Với legacy đã có cờ SYNCED nhưng thiếu journal/ID, lần enqueue mới cũng chuyển REVIEW để không tự gửi hồ sơ có thể đã tồn tại; cần đối soát trước triển khai.

`ADMISSION_STUDENT_SYNC_ENABLED=false` mặc định. Bật chỉ sau khi endpoint internal/auth/payload được triển khai đúng. `ADMISSION_STUDENT_SYNC_IDEMPOTENCY_VERIFIED=false` mặc định; chỉ bật sau kiểm thử Student trả cùng hồ sơ cho cùng user_id ngay cả retry sau timeout. Tick mặc định 10 giây (`admission.student-sync.poll-ms`), mỗi worker xử lý một job/tick. Cờ tắt không cản việc ghi outbox khi duyệt.

Nhận diện yêu cầu cần xử lý bằng truy vấn chỉ schema Admission:

```sql
SELECT user_id,enrollment_id,state,attempts,next_attempt_at,lease_until,last_error,student_profile_id
FROM admission.student_sync_outbox
WHERE state IN ('REVIEW','EXHAUSTED') ORDER BY updated_at;
```

Không có tự retry vô hạn hoặc reset số lần tự động. Operator phải đối soát Student theo user_id, lưu bằng chứng xử lý trong quy trình vận hành; nếu đã có hồ sơ đúng thì xác nhận ID/code từ Student trước cập nhật journal/reconcile; nếu chắc chắn chưa nhận và đã sửa nguyên nhân thì mới requeue với attempts reset có kiểm soát. Không cung cấp nút bỏ qua REVIEW hoặc endpoint giả nhận ACK. Hiện chưa có UI/API quản trị/requeue có audit chuyên biệt; thao tác này cần quy trình DB được kiểm soát.

## Kiểm thử và giới hạn

Test guard fail trước sửa: helper cũ gửi HTTP cho PENDING. Thêm 9 test PostgreSQL: mất kết nối/backoff/instance worker mới/thành công/replay; timeout mơ hồ; HTTP 503 đến giới hạn; concurrent claim và lease crash; cascade trước dispatch; rollback khi outbox lỗi; ACK sai/404; enqueue trùng và default disabled. Giữ các assertion cũ cho tiền mặt, cascade, giữ chỗ, webhook, email/SMTP/audit. Integration fixture thay mock helper bằng helper thật, chỉ mock cổng Student HTTP client; client contract test hiện có vẫn kiểm tra endpoint/header/ACK bằng HTTP mock server.

Restart được kiểm tra bằng instance worker mới đọc journal PostgreSQL và mô phỏng lease hết hạn; chưa kill/restart tiến trình Spring Boot thật hoặc gửi Student thật. Chưa kiểm chứng nhiều máy/network partition phía Student; không tuyên bố exactly-once toàn hệ thống. Bảo đảm chống tạo trùng từ xa vẫn phụ thuộc Student thực thi user_id idempotency đúng hợp đồng.

Log/bản sao đầu lượt: C:/Users/Admin/admission-work/qa-admission-retry-20261110. Xem báo cáo QA/completed cho kết quả kiểm thử cuối.

Kết quả cuối: **153/153 Java tests, gồm 40 PostgreSQL integration tests, 0 failures/errors/skipped; 15/15 invariants; 7/7 Node regression tests pass**. Migration chạy lặp thành công; không chạy lại Chrome trong đợt này. Log verified-legacy.log, invariants.log, frontend.log, migration-replay.log ở thư mục QA nêu trên.

Test PostgreSQL bổ sung xác nhận cờ legacy synced thiếu journal không tự tạo lại hồ sơ.
