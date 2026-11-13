# Admission H01 — callback/chu kỳ hóa đơn, 11/11/2026

Kết quả: **PARTIAL**. Hoàn thành guard Admission với bằng chứng PostgreSQL; đối soát số dư/thu đủ tiền, mapping invoice và chu kỳ mới còn phụ thuộc hợp đồng Finance. Không tuyên bố Admission V1 feature-complete.

Repository `C:/Users/Admin/admission-work/english_center`, nhánh `feature/admission`. Đầu lượt working tree sạch, HEAD `cbf5be7c2e956397cffe4e962a12c2acce197953`; không reset/clean/discard. Giữ nguyên H02/H03 và checkpoint QA cũ. Không sửa Finance/Student/Learning hoặc frontend; không commit/push/merge, không E2E toàn hệ thống.

## Thay đổi

- Callback đối chiếu invoice_code, identity, amount dương, paid_at và explicit PAID; giữ PAID ngầm theo ví dụ TASK-ADM-45 khi không có status. Không lấy học phí niêm yết làm số phải thu.
- Receipt fingerprint ràng buộc event/evidence; khóa transaction PG trước lookup và class lock; replay changed/binding khác/legacy thiếu hash phải đối soát.
- Receipt + duyệt + Student outbox cùng transaction; audit business rejection lưu sau rollback. Journal callback muộn/terminal, không hồi sinh hoặc thay quyết định đã duyệt.
- Chặn reactivation đơn đã đóng/quá hạn: không tái sử dụng ID/invoice/cash request/outbox của chu kỳ cũ. **Đăng ký lại chưa hoàn thiện** cho đến khi BA/Finance chốt thế hệ hóa đơn và migration phù hợp.
- Migration thêm hash và journal trong Admission, không tạo bảng/endpoint hóa đơn Finance. Phải áp dụng trước deploy (`ddl-auto=none`). Không sửa dữ liệu thanh toán/receipt cũ tự động.

## Danh sách file (17)

Đường dẫn dưới đây tính từ repository nêu trên:

1. `services/admission/backend/src/main/java/com/englishcenter/admission/dto/webhook/PaymentSuccessWebhookRequest.java`
2. `services/admission/backend/src/main/java/com/englishcenter/admission/entity/ProcessedFinanceEvent.java`
3. `services/admission/backend/src/main/java/com/englishcenter/admission/repository/ProcessedFinanceEventRepository.java`
4. `services/admission/backend/src/main/java/com/englishcenter/admission/service/impl/EnrollmentServiceImpl.java`
5. **Mới:** `services/admission/backend/src/main/resources/db/admission-payment-callback.sql`
6. `services/admission/backend/src/test/java/com/englishcenter/admission/service/AdmissionPostgresTest.java`
7. `services/admission/backend/src/test/java/com/englishcenter/admission/service/AdmissionRegressionTest.java`
8. `services/admission/backend/src/test/java/com/englishcenter/admission/service/EnrollmentServiceTest.java`
9. `services/admission/backend/src/test/java/com/englishcenter/admission/service/EnrollmentV1Test.java`
10. **Mới:** `services/admission/docs/payment-callback-qa.md`
11. `services/admission/docs/admission-quality-progress.md`
12. `services/admission/docs/admission-schema-spec.md`
13. `services/admission/docs/testing-v1.md`
14. `services/admission/docs/README.md`
15. `services/admission/tasks/API_CATALOG.md`
16. `docs/admission-bug-report.md`
17. **Mới:** `docs/tasks/completed/admission-payment-callback-2026-11-11.md`

## Bằng chứng kiểm thử

Log tại `C:/Users/Admin/admission-work/qa-admission-callback-20261111/`:

| Kiểm tra | Kết quả | Log |
|---|---|---|
| Tái hiện trước sửa: invoice sai, amount âm, event đổi enrollment | 3/3 fail đúng vì không ném BusinessException; không lỗi fixture | red.log |
| Java đầy đủ JDK21.0.13/Maven3.9.9 | **185/185 pass, 0 skip**, gồm **72 PG tests** | java-final.log |
| PostgreSQL portable 16.15 thật, QA DB admission_qa_v1 port55432 | Callback races, rollback/outbox, auth/evidence, journal/cycle/legacy và regression cũ pass; 14 tests mới | java-final.log |
| Migration chạy lại | COMMIT, IF NOT EXISTS giữ dữ liệu cũ | migration-replay.log |
| Invariants | 15/15 pass | invariants.log |
| Node frontend regression | 10/10 pass | frontend-node.log |
| Chrome headless | 7 trang, CASH chờ Finance, BI STAFF/ADMIN/guest pass với API fixtures | frontend-chrome-verified.log |
| git diff --check | Pass | kiểm tra trực tiếp cuối lượt |

Lần đầu sau guard: 180 Java/67 PG pass (`java-first.log`). Lần Java sandbox bị chặn JVM attach/pipe; Chrome sandbox bị ERR_NETWORK_ACCESS_DENIED localhost. Không tính các lần đó là pass; suite cuối chạy ngoài sandbox thành công. H02/H03, giữ chỗ, hủy lớp, cash, retry Student và BI đều nằm trong suite regression cuối. Không đổi assertion cũ để che lỗi; chỉ cập nhật fixture callback có evidence hợp lệ và cổng transaction manager mock của unit tests.

Finance/Student clients được mock. Spring Boot HTTP, security chain, JPA/transaction, PostgreSQL và SMTP loopback là thật; Chrome dùng API fixtures; invariants là model/static/SQLite. Không suy luận Finance đã ghi nhận thu thật từ các test này.

## Còn lại

- Finance chốt numeric invoice_id↔invoice_code, PAID/partial/cumulative amount, tiền tệ/scale/transaction_ref và nguồn số dư authoritative. Amount dương hiện chỉ là structural guard, không phải proof thu đủ.
- Finance/BA quyết định tái đăng ký và thế hệ hóa đơn/idempotency mới; hiện fail-closed, không khẳng định có luồng mới hoạt động.
- Chốt timezone/clock skew của paid_at, chính sách tiền thu trước hạn nhưng callback tới muộn/slot đầy, workflow Finance xử lý journal/cash/refund và replay legacy. Không tự sửa receipt/approval lịch sử; cần đối soát dữ liệu trước deploy.
- Áp dụng migration trên môi trường đích và kiểm chứng Finance thật theo contract đã thống nhất ở đợt sau; không chạy trong lượt này.

Chi tiết root cause/test matrix/biên bảo đảm: [payment-callback-qa.md](../../../services/admission/docs/payment-callback-qa.md). Dừng sau kiểm thử để người phụ trách review.
