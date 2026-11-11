# Hủy lớp và cascade Admission

## API và điều kiện

`PUT /api/admission/classes/{id}/cancel`, quyền ADMIN/STAFF, body `{"reason":"Không đủ sĩ số"}`. Đường cũ `PATCH /classes/{id}/status` với `status=CANCELLED` dùng cùng nghiệp vụ, không còn đường bỏ qua cascade.

Chỉ hủy lớp OPEN, ngày khai giảng còn ở tương lai, số đơn APPROVED nhỏ hơn min_students. Lớp IN_PROGRESS/COMPLETED hoặc đã đủ ngưỡng bị từ chối. Gọi lại lớp CANCELLED trả trạng thái hiện tại, không ghi lại ghi chú hoặc tạo yêu cầu mới.

Trong cùng transaction:

- Khóa lớp trước, rồi refresh/khóa các đơn theo ID tăng dần, cùng thứ tự với tạo/duyệt/hủy đơn.
- PENDING -> CANCELLED, kết thúc hold để giải phóng chỗ.
- APPROVED -> CANCELLED_REFUND_PENDING, lưu yêu cầu đối soát Finance duy nhất theo enrollment_id.
- Các trạng thái kết thúc khác giữ nguyên. Hủy đơn lại không làm mất CANCELLED_REFUND_PENDING.
- Lỗi ghi yêu cầu đối soát làm rollback cả trạng thái lớp và các đơn. Không gọi mạng bên trong transaction; không có tình trạng Finance đã nhận nhưng DB Admission rollback.
- Duyệt idempotent cũng phải khóa/refresh để tránh ghi đè cascade. Sửa lớp/chuyển trạng thái dùng khóa lớp để không ghi lại trạng thái cũ sau hủy.

## Migration

Áp dụng trước khi dùng API, chọn đúng database bằng tham số psql/biến môi trường:

```powershell
psql -v ON_ERROR_STOP=1 -f services/admission/backend/src/main/resources/db/admission-class-cancellation.sql
```

Bảng `admission.class_cancellation_requests` giữ invoice_code, enrollment_id, class_id, lý do và người yêu cầu. UNIQUE(enrollment_id) chống trùng. Chỉ thay schema Admission, không xóa dữ liệu. Trạng thái ban đầu `NEEDS_FINANCE_REVIEW` có nghĩa **chưa có xác nhận Finance**; không tự đặt ACKNOWLEDGED hoặc trạng thái đã hoàn tiền.

## Giới hạn hợp đồng Finance — chưa hoàn thành gửi refund từ xa

Tài liệu `services/finance/tasks/API_CATALOG.md` mô tả POST `/api/finance/refunds`, quyền STAFF, cần invoice_id và thông tin ngân hàng. Admission hiện chỉ lưu invoice_code; Finance backend hiện chưa có endpoint refunds. Không có hợp đồng internal/idempotency cho thông báo hủy lớp được triển khai để Admission gọi an toàn.

Vì vậy yêu cầu xử lý đã được lưu bền vững tại Admission, **chưa phải refund_request đã tạo trong Finance**. Không bịa invoice_id, tài khoản ngân hàng, số tiền hoặc tự chuyển sang đã hoàn tiền. Cần Finance cung cấp lookup invoice_code -> invoice_id, quy tắc khởi tạo khi hủy lớp, xác thực nội bộ và idempotency/acknowledgment; sau đó mới nối bộ chuyển yêu cầu sang endpoint thực. Không tự retry POST chưa có hợp đồng chống trùng vì timeout có thể tạo refund kép.

Chưa kiểm chứng thanh toán PARTIAL hoặc giao dịch PAID mà callback chưa về khi đơn vẫn PENDING. Việc phân loại hiện dựa trên trạng thái Admission đã nhận; các ca đó cần đối soát Finance. Hủy hóa đơn chưa thanh toán tại Finance và giải ngân thực tế chưa được triển khai/kiểm chứng trong đợt này. Không sửa Finance, Student, Learning hoặc ba chức năng khác.

## Bằng chứng kiểm thử

`ClassCancellationTest` fail trước sửa vì PENDING vẫn giữ nguyên sau khi hủy lớp. Test PostgreSQL bổ sung kiểm tra cascade/replay, rollback khi queue lỗi, hai lệnh hủy đồng thời, hủy tranh chấp với duyệt, và từ chối lớp đã khai giảng/đủ sĩ số. Finance không chạy thật; bảng yêu cầu nội bộ và transaction Admission được kiểm tra trên PostgreSQL 16.15.

Fixture idempotency của EnrollmentService đã đổi từ “không lấy khóa” sang kiểm tra có lấy khóa; vẫn giữ assertion trạng thái APPROVED. Đây là thay đổi có chủ đích để bảo vệ cascade khi cạnh tranh, không bỏ kiểm tra nghiệp vụ.

Kết quả cuối ngày 09/11/2026: JDK 21, **131 Java tests pass, 0 fail/error/skip**, gồm **19 PostgreSQL integration tests**. Đợt này thêm 1 unit test và 5 test PostgreSQL; 15 invariant tests và 5 frontend Node regression tests cũng pass. Log Java trước sửa: `C:\Users\Admin\admission-work\cancel-before.log`; log xác nhận cuối: `C:\Users\Admin\admission-work\cancel-verified.log`. Không chạy E2E toàn hệ thống, không gọi Finance thật, không kiểm chứng giải ngân.
