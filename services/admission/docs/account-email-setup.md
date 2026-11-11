# Tài khoản, email và migration Admission

## Luồng đang được HTTP API sử dụng

Trang `/admission/account.html` hỗ trợ đăng ký/gửi lại xác minh, đăng nhập, quên mật khẩu và đổi mật khẩu. Các endpoint dưới đây có hai tiền tố `/api/admission/auth` và `/auth`:

| Endpoint POST | Input chính | Hành vi |
|---|---|---|
| `/register` | `email`, `full_name`, `phone` tùy chọn | Tạo STUDENT chưa xác minh bằng INSERT chống trùng email; gửi VERIFY qua SMTP. Không nhận role từ client. |
| `/verify-email` | `token` | Tiêu thụ VERIFY một lần, ghi `email_verified_at`, trả `data.setup_token` cho client đang giữ link. |
| `/setup-password` | `token`, `new_password` | Dùng SETUP một lần, thiết lập mật khẩu và bỏ cờ `must_change_password`. |
| `/forgot-password` | `email` | Phản hồi chung; tài khoản chưa xác minh nhận VERIFY, đã xác minh nhận RESET. |
| `/reset-password` | `token`, `new_password` | Tiêu thụ RESET một lần, đổi mật khẩu và thu hồi refresh token cũ. |
| `/change-password` | `current_password`, `new_password` + JWT | Kiểm tra chính chủ và mật khẩu hiện tại, đổi mật khẩu, thu hồi refresh token cũ. |

Luồng HTTP gọi `AccountLifecycleService`. Hai hàm forgot/reset cũ của `AuthServiceImpl` còn tồn tại để tương thích mã/test trước đây; controller không gọi các hàm legacy đó. Không dùng chúng để tích hợp luồng email mới.

VERIFY/SETUP/RESET có hiệu lực 15 phút, dùng entropy 256 bit và chỉ lưu hash có tiền tố mục đích trong hai cột tương thích `password_reset_tokens.token`/`token_hash`. Không lưu raw token mới vào DB. Có khóa user rồi token và refresh entity để ngăn replay đồng thời. Resend trong 60 giây không gửi thêm email và vẫn trả phản hồi chung. Link không mở khóa user do Admin khóa.

Token email nằm trong fragment URL, được xóa khỏi thanh địa chỉ khi trang đọc link; SETUP chỉ giữ trong bộ nhớ trình duyệt. Không ghi token, mật khẩu hoặc body SMTP vào log. Mật khẩu cần tối thiểu 10 ký tự và tối đa 72 byte UTF-8 theo giới hạn BCrypt.

STUDENT phải xác minh email và thiết lập mật khẩu trước khi đăng nhập/tạo bài thi chính thức. Start lấy danh tính từ principal; nộp bài cần JWT chính chủ hoặc token của đúng lượt thi, đồng thời kiểm tra tài khoản chủ lượt thi đã xác minh và đang hoạt động. Token lượt thi không thay thế token đăng nhập.

## Migration bắt buộc trước khi dùng luồng email

Sau khi DB đã có schema từ `scripts/init-db.sql`, chạy file:

```powershell
psql -v ON_ERROR_STOP=1 -f services/admission/backend/src/main/resources/db/admission-account-audit.sql
```

Chọn host, port, database và user đúng môi trường bằng tham số `psql`/biến môi trường. Không đặt mật khẩu thật vào câu lệnh được lưu trong Git. Migration chỉ thay CHECK constraint của `admission.auth_audit_logs`, giữ các action cũ và bổ sung email, brute-force, lỗi refresh, tạo nhân sự. BEGIN/COMMIT giữ thao tác nguyên tử; đã kiểm tra chạy lặp hai lần trên PostgreSQL 16.15 QA. Không xóa bản ghi hoặc thay schema của phân hệ khác.

Migration chưa tự chạy khi Spring Boot khởi động. Cần đưa bước này vào quy trình triển khai; thiếu migration có thể làm transaction thất bại khi ghi audit. Các data directory QA đã dùng trước đây cũng cần migration, không cần khởi tạo/xóa lại DB.

## SMTP qua biến môi trường

SMTP bị tắt mặc định. Khi chưa bật hoặc gửi thất bại, API trả `AUTH_EMAIL_UNAVAILABLE` thay vì giả báo đã gửi.

| Biến | Cách dùng |
|---|---|
| `ADMISSION_MAIL_ENABLED` | `true` để bật gửi email |
| `SMTP_HOST`, `SMTP_PORT` | SMTP của môi trường triển khai, thường dùng cổng STARTTLS do nhà cung cấp chỉ định |
| `SMTP_USERNAME`, `SMTP_PASSWORD` | Thông tin xác thực lấy từ cấu hình bí mật, không commit |
| `SMTP_AUTH` | `true` nếu nhà cung cấp yêu cầu đăng nhập |
| `SMTP_FROM` | Địa chỉ gửi đã được nhà cung cấp cho phép |
| `SMTP_STARTTLS`, `SMTP_STARTTLS_REQUIRED` | Mặc định `true`; chỉ tắt cho SMTP loopback trong QA |
| `ADMISSION_ACCOUNT_URL` | URL công khai của `/admission/account.html`; dùng HTTPS trên môi trường có người dùng thật |

Link được dựng từ cấu hình URL, không lấy Host header từ request. SMTP có connection/read/write timeout 5 giây. Kiểm thử hiện tại dùng JavaMailSender gửi vào SMTP loopback thật, không gửi email ra ngoài. Chưa xác nhận cấu hình/khả năng chuyển thư của một nhà cung cấp SMTP thực tế.

Reset/đổi mật khẩu thu hồi refresh token. Access JWT đã cấp có thể còn hạn tối đa 15 phút; khóa tài khoản thì bị chặn ngay ở bộ lọc JWT. Việc lưu refresh token chỉ bằng hash và thu hồi access token theo phiên bản là phần hardening còn lại, không phải khả năng đã triển khai trong đợt này.
