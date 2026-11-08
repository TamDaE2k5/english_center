# Chỉ mục từ điển dữ liệu toàn hệ thống

Tổng hợp tự động từ `scripts/init-db.sql`. Đặc tả chi tiết của từng schema nằm cùng module.

| Schema | Bảng | View | Cột | Trigger | Đặc tả chi tiết |
|---|--:|--:|--:|--:|---|
| `admission` | 14 | 0 | 167 | 19 | [`services/admission/docs/admission-schema-spec.md`](../../services/admission/docs/admission-schema-spec.md) |
| `student` | 3 | 0 | 31 | 3 | [`services/student/docs/student-schema-spec.md`](../../services/student/docs/student-schema-spec.md) |
| `learning` | 2 | 2 | 22 | 2 | [`services/learning/docs/learning-schema-spec.md`](../../services/learning/docs/learning-schema-spec.md) |
| `finance` | 9 | 2 | 125 | 12 | [`services/finance/docs/finance-schema-spec.md`](../../services/finance/docs/finance-schema-spec.md) |
| **Tổng** | **28** | **4** | **345** | **36** | |

## Toàn bộ bảng

| Schema | Bảng | Số cột | Chức năng |
|---|---|--:|---|
| `admission` | `users` | 14 | Tài khoản đăng nhập tập trung của cả hệ thống (ADMIN, STAFF, TEACHER, STUDENT). Là nguồn định danh duy nhất (IAM). |
| `admission` | `refresh_tokens` | 8 | Phiên đăng nhập: Refresh Token lưu stateful để xoay vòng (rotation) và thu hồi khi đăng xuất. |
| `admission` | `password_reset_tokens` | 8 | Mã đặt lại mật khẩu dùng một lần, gửi qua email, hiệu lực 15 phút. |
| `admission` | `auth_audit_logs` | 9 | Nhật ký kiểm toán bảo mật: đăng nhập, làm mới token, đổi mật khẩu, khóa/mở khóa tài khoản. Phục vụ cảnh báo brute-force. |
| `admission` | `rooms` | 8 | Danh mục phòng học và sức chứa; dùng để kiểm tra xung đột lịch và sĩ số khi mở lớp. |
| `admission` | `courses` | 12 | Danh mục khóa học, học phí, thời lượng và lộ trình tiên quyết. |
| `admission` | `classes` | 19 | Lớp học cụ thể: khóa, phòng, giáo viên, lịch học, sĩ số và trạng thái. |
| `admission` | `enrollments` | 20 | Đơn đăng ký tuyển sinh: giữ chỗ 48h, duyệt tự động qua Finance hoặc duyệt tay, đồng bộ sang Student. |
| `admission` | `test_sets` | 9 | Bộ đề khảo thí trình độ đầu vào. |
| `admission` | `test_questions` | 16 | Câu hỏi trắc nghiệm 4 đáp án thuộc 3 kỹ năng. Đáp án đúng không bao giờ trả về API public. |
| `admission` | `test_recommendation_rules` | 10 | Ma trận quy đổi điểm test sang trình độ và khóa học khuyến nghị. |
| `admission` | `test_attempts` | 9 | Lượt làm bài theo giờ server (v3.6): chống nộp quá hạn và chấm lại cùng một lượt. |
| `admission` | `test_submissions` | 18 | Kết quả bài khảo thí đã chấm, kèm bài làm chi tiết. |
| `admission` | `processed_finance_events` | 7 | Sổ đối soát callback từ Finance (v3.6): đảm bảo idempotency và ghi nhận thanh toán đến muộn. |
| `student` | `students` | 11 | Hồ sơ học vụ của học viên; tạo tự động khi enrollment được APPROVED. |
| `student` | `attendance` | 8 | Điểm danh theo buổi học. Mỗi học viên chỉ có một bản ghi cho mỗi buổi của mỗi lớp (INV-STU-01). |
| `student` | `status_requests` | 12 | Yêu cầu học vụ của học viên: bảo lưu, chuyển lớp, bỏ học; do STAFF duyệt. |
| `learning` | `materials` | 11 | Học liệu (tài liệu, video, bài tập, slide) gắn với từng lớp. |
| `learning` | `grades` | 11 | Điểm đánh giá của học viên theo từng đầu điểm trong lớp. |
| `finance` | `tuition_fees` | 12 | Biểu phí chuẩn theo khóa học (FIN-01 / contract.md); hỗ trợ tra cứu học phí niêm yết và mức ưu đãi mặc định. |
| `finance` | `vouchers` | 14 | Chiến dịch mã giảm giá học phí. |
| `finance` | `invoices` | 18 | Hóa đơn học phí. Sau khi PAID/REFUNDED không được sửa amount, discount_applied, final_amount (INV-FIN-01). |
| `finance` | `invoice_items` | 10 | Khoản mục cấu thành hóa đơn (gói học phí, phí học lại). |
| `finance` | `installments` | 11 | Lịch đóng học phí nhiều đợt hoặc đặt cọc. Tổng amount_due các kỳ bằng final_amount hóa đơn (INV-FIN-02). |
| `finance` | `payments` | 15 | Phiếu thu: tiền mặt, VietQR hoặc cấn trừ ví. bank_transaction_id / transaction_ref UNIQUE đảm bảo idempotency (INV-FIN-05). |
| `finance` | `student_wallets` | 7 | Ví bảo lưu học phí, mỗi học viên một ví. Không cho rút tiền (INV-FIN-06). |
| `finance` | `wallet_transactions` | 9 | Sổ cái ví bảo lưu: cộng tiền bảo lưu, cấn trừ học phí, điều chỉnh. |
| `finance` | `refund_requests` | 29 | Yêu cầu hoàn học phí, duyệt 3 cấp: tư vấn tạo, kế toán xác minh, giám đốc duyệt, kế toán giải ngân (INV-FIN-07/08). |

## Bản đồ liên kết logic liên service

| Cột nguồn | Trỏ tới | Ý nghĩa |
|---|---|---|
| `student.students.user_id` | `admission.users.id` | Cầu nối hồ sơ học viên và tài khoản đăng nhập |
| `student.attendance.class_id` | `admission.classes.id` | Lớp được điểm danh |
| `student.status_requests.class_id` | `admission.classes.id` | Lớp đang học |
| `student.status_requests.target_class_id` | `admission.classes.id` | Lớp muốn chuyển đến |
| `student.status_requests.reviewed_by` | `admission.users.id` | Nhân viên duyệt |
| `learning.materials.class_id` | `admission.classes.id` | Lớp sở hữu học liệu |
| `learning.materials.uploaded_by` | `admission.users.id` | Giáo viên upload |
| `learning.grades.class_id` | `admission.classes.id` | Lớp chấm điểm |
| `learning.grades.student_id` | `student.students.id` | Học viên được chấm (KHÔNG phải users.id) |
| `learning.grades.graded_by` | `admission.users.id` | Giáo viên chấm |
| `finance.tuition_fees.course_id` | `admission.courses.id` | Khóa học áp dụng biểu phí chuẩn (FIN-01) |
| `finance.invoices.student_id` | `admission.users.id` | Người học (R-ID-1: dùng users.id) |
| `finance.invoices.course_id` | `admission.courses.id` | Khóa học thu phí |
| `finance.invoices.enrollment_id` | `admission.enrollments.id` | Đơn đăng ký phát sinh hóa đơn |
| `finance.invoice_items.course_id` | `admission.courses.id` | Khóa học của khoản mục |
| `finance.payments.received_by_staff_id` | `admission.users.id` | Nhân viên thu tiền |
| `finance.student_wallets.student_id` | `admission.users.id` | Chủ ví (R-ID-1) |
| `finance.wallet_transactions.created_by_staff_id` | `admission.users.id` | Nhân viên thao tác ví |
| `finance.refund_requests.student_id` | `admission.users.id` | Học viên được hoàn (R-ID-1) |
| `finance.refund_requests.class_id` | `admission.classes.id` | Lớp liên quan |
| `finance.refund_requests.requested_by_staff_id` | `admission.users.id` | Nhân viên tạo yêu cầu (cấp 1) |
| `finance.refund_requests.verified_by_accountant_id` | `admission.users.id` | Kế toán xác minh (cấp 2) |
| `finance.refund_requests.approved_by_director_id` | `admission.users.id` | Giám đốc duyệt (cấp 3) |
| `admission.enrollments.invoice_code` | `finance.invoices.invoice_code` | Mã hóa đơn Finance cấp (tham chiếu theo mã) |
