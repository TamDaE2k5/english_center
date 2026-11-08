# Hợp đồng Kỹ thuật & Ràng buộc Hệ thống (Technical Contract)

> **Trạng thái**: APPROVED (Bản chốt hợp nhất toàn nhóm — Final 1.0)
> **Tài liệu chi tiết liên kết**:
> - Bản đặc tả tổng thể CSDL & Quy tắc API liên service: [`docs/MASTER_DB_AND_API_RULES.md`](MASTER_DB_AND_API_RULES.md)
> - Chỉ mục Từ điển Dữ liệu toàn hệ thống (28 bảng, 4 views, 342 cột, 35 triggers): [`docs/database/DATA_DICTIONARY_INDEX.md`](database/DATA_DICTIONARY_INDEX.md)
> - DDL khởi tạo chuẩn trên PostgreSQL 16: [`scripts/init-db.sql`](../scripts/init-db.sql)

---

## 1. Domain Model (Dữ liệu nghiệp vụ — 4 Schemas độc lập)

Hệ thống phân tách dữ liệu thành **4 Schema độc lập** trên cùng 1 PostgreSQL 16 instance (`english_center`), tuân thủ tuyệt đối bất biến **`INV-SYS-02`** (không có Physical FK chéo schema, chỉ dùng Logical FK kiểu `INTEGER` hoặc mã định danh).

| Schema | Service | Port | Tech Stack | Số bảng | Số View | Đặc tả chi tiết từng cột |
|---|---|---:|---|---:|---:|---|
| `admission` | `admission-service` | `8081` | Java 21 / Spring Boot 3.3.4 | **14** | 0 | [`admission-schema-spec.md`](../services/admission/docs/admission-schema-spec.md) |
| `student` | `student-service` | `8082` | Python 3.10+ / FastAPI | **3** | 0 | [`student-schema-spec.md`](../services/student/docs/student-schema-spec.md) |
| `learning` | `learning-service` | `8083` | Python 3.10+ / FastAPI | **2** | **2** | [`learning-schema-spec.md`](../services/learning/docs/learning-schema-spec.md) |
| `finance` | `finance-service` | `8084` | Python 3.11 / FastAPI | **9** | **2** | [`finance-schema-spec.md`](../services/finance/docs/finance-schema-spec.md) |
| **Tổng cộng** | **4 Microservices** | `8080` (GW) | **Polyglot** | **28** | **4** | [`DATA_DICTIONARY_INDEX.md`](database/DATA_DICTIONARY_INDEX.md) |

> [!IMPORTANT]
> **Quy ước định danh người học (`R-ID-1`):**
> - Trong schema **`admission`** và **`finance`**: cột `student_id` lưu **`admission.users(id)`** (vì đơn tuyển sinh `enrollments` và hóa đơn `invoices` được sinh ở trạng thái `PENDING` trước khi hồ sơ học vụ chính thức được khởi tạo).
> - Trong schema **`student`** và **`learning`**: cột `student_id` lưu **`student.students(id)`**.
> - Cầu nối duy nhất giữa 2 định danh là cột **`student.students.user_id`** (`Logical FK -> admission.users.id`).

### A. Schema `admission` (Tuyển sinh, Khảo thí & Định danh — 14 bảng)
1. **`users`**: Tài khoản đăng nhập tập trung (`ADMIN`, `STAFF`, `TEACHER`, `STUDENT`), BCrypt password, `must_change_password`, `email_verified_at`.
2. **`refresh_tokens`**: Phiên đăng nhập stateful hỗ trợ Refresh Token Rotation (`token` & `token_hash` tự đồng bộ qua trigger).
3. **`password_reset_tokens`**: Token đặt lại mật khẩu hiệu lực 15 phút (`token` & `token_hash` tự đồng bộ).
4. **`auth_audit_logs`**: Nhật ký kiểm toán bảo mật (`LOGIN_SUCCESS`, `LOGIN_FAILED`, `REFRESH_TOKEN`, `LOGOUT`, `PASSWORD_CHANGED`, `ACCOUNT_LOCKED`, `ACCOUNT_UNLOCKED`).
5. **`rooms`**: Danh mục phòng học & sức chứa (`capacity > 0`).
6. **`courses`**: Danh mục khóa học (`code` UNIQUE, `level`, `duration_weeks`, `tuition_amount`, `prerequisite_course_id`).
7. **`classes`**: Lớp học & thời khóa biểu (`course_id`, `room_id`, `teacher_id`, `day_pattern`, `start_time`, `end_time`, `min_students`, `max_students`, `status`, kèm các cột tương thích `room`, `teacher_name`, `schedule`).
8. **`enrollments`**: Đơn đăng ký tuyển sinh & giữ chỗ 48h (`student_id`, `class_id`, `status`, `hold_expires_at`, `invoice_code`, `enrollment_channel`, `student_sync_status` / `is_synced_to_student`).
9. **`test_sets`**: Bộ đề khảo thí đầu vào (`code`, `title`, `duration_minutes`, `total_max_score`).
10. **`test_questions`**: Câu hỏi trắc nghiệm 3 kỹ năng (`LISTENING`, `READING`, `GRAMMAR`; `order_index` / `display_order`).
11. **`test_recommendation_rules`**: Ma trận quy đổi dải điểm sang trình độ & khóa học khuyến nghị (`advice_text` / `advice_note`).
12. **`test_attempts`**: Lượt làm bài thi theo giờ server (`STARTED`, `SUBMITTED`, `EXPIRED` — chống nộp trễ hạn & chấm lặp `INV-TST-02`).
13. **`test_submissions`**: Kết quả bài khảo thí đã chấm (`attempt_id` UNIQUE, điểm 3 kỹ năng, `answers_detail` JSONB).
14. **`processed_finance_events`**: Sổ đối soát callback thanh toán từ Finance (`finance_event_id` UNIQUE — đảm bảo Idempotency `INV-ENR-02` & xử lý thanh toán muộn `INV-ENR-03`).

### B. Schema `student` (Hồ sơ học viên, Điểm danh & Học vụ — 3 bảng)
1. **`students`**: Hồ sơ học viên (`id`, `user_id` Logical FK, `student_code` UNIQUE dạng `HV-YYYY-NNNN` tự sinh qua sequence `student.student_code_seq`, `full_name`, `email` UNIQUE, `status`: `STUDYING`, `RESERVED`, `DROPPED`, `GRADUATED`).
2. **`attendance`**: Điểm danh theo buổi (`class_id` Logical FK, `student_id`, `session_date`, `status`: `PRESENT`, `ABSENT`, `LATE`, `EXCUSED`; ràng buộc `UNIQUE(class_id, student_id, session_date)` theo `INV-STU-01`).
3. **`status_requests`**: Yêu cầu học vụ (`student_id`, `request_type`: `RESERVE`, `TRANSFER`, `DROP`, `class_id`, `target_class_id`, `status`: `PENDING`, `APPROVED`, `REJECTED`, `reviewed_by`).
- *Bất biến `INV-STU-03`*: Tuyệt đối không tạo bảng `class_members`; danh sách học viên của lớp lấy qua REST API từ `admission.enrollments` (`status = 'APPROVED'`).

### C. Schema `learning` (Học liệu, Bảng điểm & Báo cáo — 2 bảng + 2 views)
1. **`materials`**: Học liệu gắn với lớp (`class_id` Logical FK, `title`, `material_type`: `DOCUMENT`, `VIDEO`, `EXERCISE`, `SLIDE`, `file_url` không rỗng `INV-LRN-04`, `original_filename`, `file_size`, `uploaded_by`).
2. **`grades`**: Điểm đánh giá học viên (`class_id`, `student_id` Logical FK tới `student.students(id)`, `assessment_name`, `0 <= score <= max_score` theo `INV-LRN-01`, `weight` trong `(0, 1]` hoặc `NULL` coi là `1.0`, `graded_by`; ràng buộc `UNIQUE(class_id, student_id, assessment_name)` theo `INV-LRN-06`).
- **Views báo cáo**: `learning.v_student_grade_summary` (chuẩn hóa từng đầu điểm về thang 100% trước khi tính trung bình trọng số; `average_percentage >= 50` là `Pass`) và `learning.v_class_pass_rate`.

### D. Schema `finance` (Biểu phí, Hóa đơn, Thanh toán, Ví & Hoàn tiền — 9 bảng + 2 BI views)
1. **`tuition_fees`**: Biểu phí chuẩn theo khóa học (`course_id` UNIQUE Logical FK, `course_name`, `amount` / `base_amount` tự đồng bộ, `currency`, `discount_pct`, `note` / `description`).
2. **`vouchers`**: Chiến dịch mã giảm giá (`code` UNIQUE, `discount_percent`, `discount_amount`, `min_order_value`, `max_discount_amount`, `current_uses <= max_uses` khóa bi quan `INV-FIN-04`).
3. **`invoices`**: Hóa đơn học phí (`invoice_code` UNIQUE tự sinh, `student_id`, `course_id`, `enrollment_id`, `amount`, `discount_applied`, `final_amount = amount - discount_applied`, `paid_amount <= final_amount`, `remaining_amount` GENERATED, `status`: `PENDING`, `UNPAID`, `PARTIAL`, `PAID`, `OVERDUE`, `CANCELLED`, `REFUNDED`).
4. **`invoice_items`**: Chi tiết khoản mục hóa đơn (`TUITION_PACKAGE`, `RETAKE_FEE`, `line_total = quantity * unit_price`).
5. **`installments`**: Lịch đóng học phí nhiều đợt (`installment_no`, `payment_ref_code` UNIQUE, `amount_due`, `amount_paid`, `status`).
6. **`payments`**: Phiếu thu & gạch nợ (`payment_code` UNIQUE tự sinh, `invoice_id`, `installment_id`, `payment_method`: `CASH`, `VIETQR`, `WALLET_OFFSET`, `BANK_TRANSFER`, `CARD`, `MOMO`, `amount > 0`, `bank_transaction_id` / `transaction_ref` UNIQUE đảm bảo Idempotency `INV-FIN-05`).
7. **`student_wallets`**: Ví bảo lưu học viên (`student_id` UNIQUE, `balance >= 0`, `is_withdrawable = FALSE` theo `INV-FIN-06`).
8. **`wallet_transactions`**: Lịch sử giao dịch ví (`RESERVATION_CREDIT`, `TUITION_OFFSET`, `ADJUSTMENT`).
9. **`refund_requests`**: Yêu cầu hoàn tiền 3 cấp duyệt (`attended_sessions <= max_allowed_sessions` theo `INV-FIN-07`, trạng thái `PENDING_ACCOUNTANT -> PENDING_DIRECTOR -> APPROVED -> COMPLETED` theo `INV-FIN-08`).
- **BI Views**: `finance.vw_bi_cash_revenue_monthly` và `finance.vw_bi_voucher_campaigns`.

---

## 2. State Machines (Vòng đời đối tượng cốt lõi)

### A. Vòng đời Đăng ký (`admission.enrollments`)
```
[PENDING (giữ chỗ 48h)] --(Finance PAID webhook / Staff duyệt)--> [APPROVED]
[PENDING] --------------(Quá hạn hold_expires_at)---------------> [EXPIRED]
[PENDING] --------------(Từ chối)-------------------------------> [REJECTED]
[PENDING] --------------(Học viên / Staff hủy)------------------> [CANCELLED]
[APPROVED] -------------(Hủy lớp thiếu sĩ số)-------------------> [CANCELLED_REFUND_PENDING] --> [CANCELLED]
```
- **Quy tắc (`INV-ADM-01`, `INV-ADM-03`)**: Không được chuyển sang `APPROVED` nếu `APPROVED + PENDING (còn hạn) >= max_students`. Các đơn đã `CANCELLED`, `REJECTED`, `EXPIRED` không được chuyển ngược thành `APPROVED`.

### B. Vòng đời Hóa đơn (`finance.invoices`)
```
[PENDING / UNPAID] --(Thu một phần)-------------> [PARTIAL]
[PENDING / PARTIAL] -(Thu đủ final_amount)------> [PAID]
[PENDING / PARTIAL] -(Hủy đơn)------------------> [CANCELLED]
[PAID] --------------(Hoàn tiền cấp 3 hoàn tất)-> [REFUNDED]
```
- **Quy tắc (`INV-FIN-01`, `INV-FIN-03`)**: Chỉ chuyển sang `PAID` khi tổng giao dịch `SUCCESS` đạt `final_amount`. Khi đã `PAID` hoặc `REFUNDED`, cấm sửa `amount`, `discount_applied`, `final_amount`.

---

## 3. API Contract & Conventions

- **Định tuyến qua Nginx Gateway (`:8080`)**:
  - `/auth/*` và `/api/admission/*` → `admission-service:8081`
  - `/api/student/*` (và alias `/api/v1/*`) → `student-service:8082`
  - `/api/learning/*` → `learning-service:8083`
  - `/api/finance/*` → `finance-service:8084`
- **Cấu trúc JSON chung (`INV-SYS-01`)**:
  - Thành công: `{ "success": true, "data": ..., "message": "..." }`
  - Lỗi: `{ "success": false, "error": { "code": "ERR_CODE", "message": "..." } }`
- **Naming style**: `snake_case` cho toàn bộ JSON fields và query parameters.
- **Header xác thực**:
  - Người dùng: `Authorization: Bearer <jwt_token>`
  - Giao tiếp nội bộ liên service (`/internal/*`, webhooks): `X-Internal-Secret: <shared_secret>`
- **Tiền tố mã lỗi**: `AUTH_`, `ADM_`, `STU_`, `LRN_`, `FIN_`.

---

## 4. Phân quyền & Ranh giới Bảo mật (Authorization)
- `ADMIN`: Toàn quyền hệ thống, phê duyệt hoàn tiền cấp 3 (Giám đốc), xem BI Dashboard.
- `STAFF`: Quản lý danh mục khóa/lớp/phòng, tiếp nhận & duyệt hồ sơ tuyển sinh, duyệt yêu cầu học vụ, lập hóa đơn, thu tiền mặt, xác minh hoàn tiền cấp 1 & 2.
- `TEACHER`: Chỉ xem danh sách lớp mình dạy, điểm danh lớp mình dạy, upload học liệu và nhập điểm lớp mình phụ trách (`INV-SEC-02` / `INV-LRN-03`). Không được sửa học phí hoặc xem báo cáo tài chính.
- `STUDENT`: Chỉ xem và cập nhật thông tin cá nhân của chính mình (`INV-SEC-01`), làm bài test đầu vào, đăng ký lớp, xem điểm/học liệu của lớp mình học (`INV-LRN-02`), và tra cứu hóa đơn/ví bảo lưu của bản thân.
