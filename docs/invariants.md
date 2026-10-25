# Danh mục Quy tắc Bất biến Hệ thống (System Invariants)

> **Mục tiêu**: Định nghĩa các điều kiện bất biến (Invariants) mà hệ thống bắt buộc phải thỏa mãn trong mọi hoàn cảnh. Bộ kiểm thử tự động tại `tests/invariants/` sẽ liên tục kiểm tra các quy tắc này.

---

## Bảng Danh mục 15 Quy tắc Bất biến

| ID | Tên quy tắc | Mô tả chi tiết | Mức độ | Test File | Trạng thái |
|---|---|---|---|---|---|
| **INV-SEC-01** | Cô lập hồ sơ học viên | Học viên A tuyệt đối không được xem/sửa hồ sơ cá nhân của học viên B qua API. | P0 | `test_security_invariants.py` | VERIFIED |
| **INV-SEC-02** | Ranh giới điểm giáo viên | Giáo viên không được nhập hoặc chỉnh sửa điểm của lớp mình không được phân công dạy. | P0 | `test_security_invariants.py` | VERIFIED |
| **INV-SEC-03** | Bảo mật bí mật hệ thống | Không commit file `.env` chứa mật khẩu thật, JWT secret phải đọc qua biến môi trường. | P0 | `test_security_invariants.py` | VERIFIED |
| **INV-ADM-01** | Giới hạn sĩ số tối đa | Không được chuyển đơn đăng ký sang `APPROVED` nếu lớp học đã đủ sĩ số `max_students`. | P0 | `test_admission_invariants.py` | VERIFIED |
| **INV-ADM-02** | Mã khóa học duy nhất | Không được phép tồn tại 2 khóa học có cùng mã `code`. | P0 | `test_admission_invariants.py` | VERIFIED |
| **INV-ADM-03** | State machine đăng ký | Đơn đăng ký ở trạng thái `CANCELLED` hoặc `REJECTED` không thể được duyệt trực tiếp sang `APPROVED`. | P1 | `test_admission_invariants.py` | VERIFIED |
| **INV-STU-01** | Điểm danh đơn nhất | Mỗi học viên chỉ có tối đa 1 trạng thái điểm danh trong 1 buổi học cụ thể của lớp. | P1 | `test_student_invariants.py` | VERIFIED |
| **INV-STU-02** | Trạng thái học vụ hợp lệ | Học viên chỉ được ở 1 trong các trạng thái: `STUDYING`, `RESERVED`, `DROPPED`, `GRADUATED`. | P1 | `test_student_invariants.py` | VERIFIED |
| **INV-LRN-01** | Giới hạn điểm số hợp lệ | Điểm số bài thi (`score`) phải nằm trong khoảng: `0.0 <= score <= max_score`. | P0 | `test_learning_invariants.py` | VERIFIED |
| **INV-LRN-02** | Cô lập điểm thi học viên | Học viên chỉ xem được bảng điểm của chính mình, không được xem điểm của học viên khác. | P0 | `test_learning_invariants.py` | VERIFIED |
| **INV-FIN-01** | Số tiền thanh toán dương | Số tiền thanh toán (`payment.amount`) và hóa đơn (`invoice.amount`) phải luôn > 0. | P0 | `test_finance_invariants.py` | VERIFIED |
| **INV-FIN-02** | Chống thanh toán trùng lặp | Giao dịch có cùng mã tham chiếu `transaction_ref` không được ghi nhận lần thứ 2 (Idempotency). | P0 | `test_finance_invariants.py` | VERIFIED |
| **INV-FIN-03** | Khớp trạng thái hóa đơn | Hóa đơn chỉ chuyển sang `PAID` khi tổng các khoản thanh toán hợp lệ `>=` số tiền hóa đơn. | P0 | `test_finance_invariants.py` | VERIFIED |
| **INV-SYS-01** | API JSON snake_case | Toàn bộ payload request và response của hệ thống API phải tuân thủ chuẩn `snake_case`. | P1 | `test_system_invariants.py` | VERIFIED |
| **INV-SYS-02** | Cô lập Schema Database | Các truy vấn backend của từng service chỉ được truy cập schema tương ứng, không query chéo DB trực tiếp. | P0 | `test_system_invariants.py` | VERIFIED |

---

## Quy tắc bảo vệ Invariant Tests
1. File test tại `tests/invariants/` là **Bất biến**.
2. Khi code tính năng nghiệp vụ, Agent **không được sửa assertion** của test để che giấu lỗi.
3. Khi phát hiện vi phạm, Agent phải sửa logic code của Service cho tới khi Invariant Test xanh hoàn toàn.
