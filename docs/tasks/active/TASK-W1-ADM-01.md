# TASK-W1-ADM-01: Kiểm tra Sĩ số Lớp khi Duyệt Đơn Đăng ký

- **Mục tiêu**: Khi gọi API duyệt đơn đăng ký (`PUT /api/admission/enrollments/{id}` hoặc qua giao diện), hệ thống phải đếm tổng số học viên đã được `APPROVED` của lớp đó. Nếu số lượng đã `>= max_students`, phải từ chối chuyển trạng thái và trả về mã lỗi `ADM_CLASS_FULL`.
- **Phạm vi file được phép sửa**:
  - `services/admission/backend/src/main/java/com/englishcenter/admission/`
- **File không được sửa**:
  - `tests/invariants/*`
  - Các service khác (`student`, `learning`, `finance`)
- **Invariant bảo vệ**: `INV-ADM-01`
- **Acceptance Criteria**:
  - Given: Lớp CLS-01 có `max_students = 20` và đã có 20 học viên trạng thái `APPROVED`.
  - When: Staff thực hiện duyệt đơn của học viên thứ 21.
  - Then: Hệ thống trả về lỗi HTTP 400 Bad Request kèm mã lỗi `ADM_CLASS_FULL`, trạng thái đơn giữ nguyên `PENDING`.
