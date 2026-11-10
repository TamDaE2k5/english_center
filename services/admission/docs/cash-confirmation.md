# Yêu cầu xác nhận tiền mặt — Admission, 10/11/2026

**Đánh giá: PARTIAL.** Hoàn thành phần Admission ghi yêu cầu và audit; chưa gửi yêu cầu/tạo khoản CASH tại Finance thật, chưa xác nhận thu tiền cuối cùng hoặc in phiếu thu. Không nghiệm thu V1 toàn hệ thống.

## Căn cứ và ranh giới

- BA hiện hành `admission-ba-bi-spec.md`: phạm vi ở mục 1, ma trận quyền, FR-ENR-07, API approve-manual và DEC-04 đều yêu cầu STAFF/ADMIN gửi yêu cầu; Finance xác nhận CASH trước khi Admission duyệt. Các mô tả cũ trong UML/bản tổng hợp/task cho phép duyệt trực tiếp đã khác BA mới. Đợt này tuân theo yêu cầu Finance là nguồn xác thực, không tự gán PAID.
- `docs/contract.md` quy định STAFF được thu tiền; `services/finance/tasks/API_CATALOG.md` FIN-F05 quy định ADMIN/STAFF gọi POST `/api/finance/payments/cash`, body invoice_id, installment_id tùy chọn, amount, notes. TASK-FIN-30 yêu cầu Finance khóa hóa đơn, cấm thu vượt dư nợ, cập nhật PARTIAL/PAID theo số tiền thật.
- Backend Finance hiện chỉ có POST `/api/finance/payments`, chưa có endpoint CASH theo catalog. Không dùng endpoint generic thay thế vì nó đánh dấu hóa đơn PAID trực tiếp. Admission chỉ lưu invoice_code, chưa có mapping invoice_id, hợp đồng chuyển yêu cầu nội bộ/chống trùng/acknowledgment hoặc phân biệt phương thức trong callback đã triển khai.

Vì vậy đợt này không tạo endpoint Finance giả, không suy ra số tiền phải thu từ giá khóa học và không coi dữ liệu nhân viên nhập là bằng chứng Finance đã xác nhận. Không sửa Finance, Student, Learning; không làm retry Student hoặc BI.

## API Admission

POST hoặc PUT `/api/admission/enrollments/{id}/approve-manual`, JWT ADMIN/STAFF. POST giữ tương thích API cũ, PUT theo BA mới. Ví dụ:

```json
{"receipt_number":"PT-2026-0089","payment_method":"CASH","amount_received":"6500000.00","notes":"Thông tin đối soát tại quầy"}
```

Hỗ trợ alias cash_amount cho amount_received và note cho notes. Chứng từ bắt buộc, tối đa 80 ký tự (trim trước khi lưu); số tiền dương, tối đa 12 chữ số nguyên và 2 chữ số thập phân; notes tối đa 2000 ký tự. Chỉ nhận CASH; POS_CARD/DIRECT_TRANSFER không thuộc luồng này. Mã chứng từ phải duy nhất, không tự sinh số phiếu thu Finance hoặc hợp thức hóa chứng từ do client nhập.

HTTP **202 Accepted** trả EnrollmentResponse còn PENDING và cash_confirmation gồm id, status=NEEDS_FINANCE_CONFIRMATION, invoice_code, receipt_number, amount, requested_by, requested_at. Đây là yêu cầu **đã lưu ở Admission, chưa chuyển tự động sang Finance**. requested_by lấy từ JWT và requested_at từ server DB, không nhận từ client. Khi xem chi tiết/danh sách Học vụ, yêu cầu vẫn hiển thị sau reload.

Guard: lớp OPEN; đơn PENDING còn deadline tương lai; có invoice_code; chưa đủ số đơn APPROVED theo max_students. Cấm đơn APPROVED, CANCELLED, CANCELLED_REFUND_PENDING, EXPIRED, REJECTED hoặc PENDING đã quá hạn dù scheduler chưa chạy. Deadline null chưa đủ cơ sở tiếp nhận nên bị từ chối. Không tự gia hạn, giải phóng/chiếm thêm slot hoặc đổi trạng thái đơn khi gửi yêu cầu.

## Nhất quán, chống trùng và audit

- Khóa lớp rồi khóa/refresh đơn, cùng thứ tự với giữ chỗ, duyệt và cascade. Transaction riêng ghi yêu cầu cùng audit REQUESTED; nếu audit/storage lỗi, rollback toàn bộ, không báo 202.
- UNIQUE(enrollment_id, invoice_code) và UNIQUE(receipt_number) trong PostgreSQL. Replay cùng đơn/hóa đơn/chứng từ/số tiền trả 202, ghi audit REPLAYED, giữ nguyên người/thời điểm/note gốc. Đổi chứng từ hoặc số tiền, hay dùng chứng từ cho đơn khác, trả 409 ADM_CASH_CONFLICT. Không sửa yêu cầu cũ qua replay.
- Guard trạng thái/deadline vẫn chạy trước replay: yêu cầu đã lưu không cho phép gọi lại thành công sau hủy/hết hạn. Không có ràng buộc idempotency được giả định ở Finance vì chưa gọi Finance.
- Từ chối nghiệp vụ rollback trước rồi ghi audit REJECTED bằng REQUIRES_NEW; không nuốt lỗi audit. Bảng `admission.cash_confirmation_audit` lưu enrollment_id, actor_id, occurred_at, outcome, reason_code, receipt_number và amount_text. Không FK enrollment_id để audit được ID không tồn tại. Không ghi token, mật khẩu hoặc notes vào log/audit.
- Audit này dành riêng nghiệp vụ tiền mặt, không sửa constraint hoặc migration auth_audit_logs cũ. Request bị SecurityFilter/Bean Validation chặn trước service (401/403/JSON sai) không có bản ghi audit tiền mặt; không tuyên bố đã audit mọi HTTP request.

## Quan hệ với callback và hủy lớp

Không thay đường xác thực webhook: POST `/api/admission/webhooks/payment-success` phải qua X-Internal-Secret và idempotency event_id hiện có. Chỉ event Finance đáng tin cậy mới đi vào lõi duyệt có guard deadline/sĩ số. Request tiền mặt tự nó không gọi approveEnrollment hoặc Student sync.

Callback hiện tại không chứng minh phương thức CASH/chứng từ tương ứng: vẫn dùng giá trị legacy ONLINE_PAYMENT cho đường callback, không suy từ yêu cầu tiền mặt rằng giao dịch đó là CASH và không gán MANUAL_CASH. NEEDS_FINANCE_CONFIRMATION mô tả yêu cầu CASH chưa được đối soát, không phải trạng thái thanh toán hóa đơn. Sau callback khác hoặc cascade, giữ nguyên chứng từ yêu cầu để đối soát, không xóa dữ liệu và không tự đánh dấu đã xác nhận CASH.

Cascade vẫn chuyển PENDING sang CANCELLED và APPROVED sang CANCELLED_REFUND_PENDING. Tiền đã thu ngoài thực tế nhưng chưa có callback, thanh toán một phần, paid_at đến muộn, đối soát số tiền/dư nợ và hoàn tiền đều cần Finance. Những dữ liệu này không được suy diễn từ yêu cầu nội bộ.

## Migration và kiểm thử

Áp dụng các migration Admission hiện có rồi chạy migration mới trên đúng DB trước khi triển khai API/giao diện:

```powershell
psql -v ON_ERROR_STOP=1 -f services/admission/backend/src/main/resources/db/admission-cash-confirmation.sql
```

Migration chỉ thêm 2 bảng và 1 index trong schema Admission; không tự chạy khi app khởi động. Đã chạy lần đầu và chạy lặp thành công trên PostgreSQL QA 16.15 (DB admission_qa_v1, cổng 55432), giữ dữ liệu. Bảng request chỉ là phần bổ sung Admission, không thay bảng payments Finance.

Test trước sửa chứng minh đơn PENDING bị approve-manual đổi sang APPROVED; test frontend phát hiện thông báo đã thu/duyệt sai và thiếu hiển thị yêu cầu Finance. Đã thêm **12 PostgreSQL tests** cho tiền mặt: không tự duyệt, replay/conflict và audit, quyền/validation, trạng thái kết thúc/quá hạn, đồng thời cùng đơn, trùng chứng từ giữa hai lớp, tranh chấp cascade, callback có/thiếu secret, audit lỗi rollback, deadline null/lớp đầy, PUT/alias, tranh chấp callback.

Kết quả cuối: **143 Java tests pass trên JDK 21, gồm 31 PostgreSQL integration tests, 0 failures/errors/skipped; 15 invariant tests và 7 Node regression tests pass**. Chrome kiểm tra 7 trang Admission với API fixtures, thêm thao tác gửi CASH/JWT/HTTP 202 và trạng thái chờ sau reload; guest bị chặn trang Học vụ. Không chạy E2E toàn hệ thống, Finance/Student ở integration fixture vẫn được mock.

Test unit manual approval trước đây mong APPROVED ngay đã được thay tiêu chí theo BA mới: phải ủy quyền ghi yêu cầu, còn PENDING và không gọi sync. Các assertion invariant/cascade/giữ chỗ cũ không bị nới lỏng; hai test tạo service thủ công chỉ bổ sung dependency mock mới. Log/bản sao đầu lượt tại `C:\Users\Admin\admission-work\qa-admission-cash-20261110` (red.log, java-final.log, invariants.log, frontend-final.log, browser.log, migration-replay.log).

## Điều kiện để DONE

Finance cần triển khai FIN-F05 đúng contract; cung cấp mapping invoice_code sang invoice_id và cơ chế chuyển yêu cầu có xác thực/idempotency/chứng từ; xác nhận kết quả CASH và trạng thái PARTIAL/PAID đáng tin cậy. Sau đó mới nối gửi/acknowledgment, đối soát người thu/thời điểm thu thật/số tiền/công nợ và kiểm chứng luồng Finance thực tế. Hiện không có transport hoặc worker gửi tự động; không có xác nhận hay phiếu thu CASH Finance thật.
