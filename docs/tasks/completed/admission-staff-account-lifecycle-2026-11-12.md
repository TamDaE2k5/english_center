# QA tài khoản nhân sự H07-B — xác minh ngày 13/11/2026

H07-B **PARTIAL**. Validation và yêu cầu đổi mật khẩu lần đầu đã triển khai trong Admission. Quy tắc reset email cho STAFF/TEACHER còn chờ BA.

## Tái hiện

- `CreateUserRequest.java` thiếu ràng buộc bắt buộc cho phone, gender và DOB. RED xác nhận payload thiếu phone trả 201. Backend đã có kiểm tra ADMIN, vai trò STAFF/TEACHER và email trùng.
- `JwtAuthenticationFilter.java` từng cấp đầy đủ quyền khi JWT hợp lệ và người dùng còn hoạt động, kể cả khi cờ `must_change_password` của nhân sự đang bật. `AccountLifecycleService.java` từng xóa cờ sau reset email. `account.js` từng đưa nhân sự mới vào trang khảo thí.
- PostgreSQL RED có 4/6 ca thất bại, gồm tạo thiếu trường, hai đường gọi API trực tiếp và reset làm mất cờ. Hai ca quyền tạo/email trùng và cạnh tranh đổi mật khẩu đã qua baseline. Node RED có 1/19 ca thất bại do chuyển trang trước khi đổi mật khẩu.

## Hành vi đã xác minh

- Tạo nhân sự qua API yêu cầu ADMIN và đủ họ tên, email, phone, gender, DOB, vai trò và mật khẩu ban đầu. Payload sai trả 400, email trùng trả 409, vai trò gọi API không đủ quyền trả 403. Giữ enum giới tính và mức tối thiểu 8 ký tự hiện có cho mật khẩu ban đầu. Giới hạn 72 byte UTF-8 theo BCrypt.
- Backend đọc trạng thái hiện tại từ DB và giới hạn phiên STAFF/TEACHER đang bật cờ. Các đường dẫn cần thiết để đổi mật khẩu, xem `me`, login, refresh, logout và khôi phục tài khoản vẫn hoạt động theo quyền tương ứng. API ngoài danh sách trả `403 AUTH_PASSWORD_CHANGE_REQUIRED`.
- Đổi mật khẩu yêu cầu mật khẩu hiện tại và mật khẩu mới khác khi đang thực hiện first-password. Transaction dùng khóa bản ghi user, xóa cờ, thu hồi refresh cũ và ghi `PASSWORD_CHANGED`. Hai request cùng dùng mật khẩu cũ cho kết quả một thành công và một bị từ chối, với một audit. Phiên giới hạn cũ không được nâng quyền sau khi đổi. Access JWT nhân sự đời cũ thiếu claim phạm vi cần làm mới hoặc đăng nhập lại khi không còn cờ first-password.
- Frontend hiển thị form đổi mật khẩu ngay khi đăng nhập với cờ đang bật. Gõ URL trang được bảo vệ cũng quay lại form. Sai mật khẩu giữ phiên giới hạn để thử lại. Thành công xóa phiên local và yêu cầu đăng nhập lại.
- Entity `User` chỉ thêm phương thức đọc vai trò và cờ. Bản sửa dùng cột `admission.users.must_change_password` đã có, không cần migration.

## Kết quả kiểm thử

| Bộ kiểm thử | Kết quả |
|---|---|
| H07-B focused trên JDK 21 | 27/27 PASS, gồm 6 ca HTTP/PostgreSQL. Các ca này cũng nằm trong regression đầy đủ |
| Maven Admission trên JDK 21 | 226/226 PASS, 0 lỗi, 0 skip. Gồm 119 ca Java ngoài PostgreSQL và 107 ca trên PostgreSQL 16.15 thật |
| Node | 37/37 PASS, gồm session và frontend regression |
| Chrome | 22 kiểm tra PASS với API fixtures, gồm đổi mật khẩu và đăng nhập lại |
| Invariants và `git diff --check` | 15/15 invariants PASS, diff check exit 0 |

Khi tiếp tục, `java-focused.log` và `chrome.log` chưa có. Lượt Java cũ dừng lúc biên dịch vì thay đổi constructor `UserPrincipal`. Cây làm việc lúc tiếp tục đã có constructor tương thích và qua Maven. Auto-review từng từ chối lệnh PostgreSQL. Lệnh được duyệt ở lượt xác minh đã kết nối DB QA hiện có, không reset dữ liệu.

Log nằm tại `C:/Users/Admin/admission-work/qa-admission-h07b-20261112/`. Kết quả cuối gồm `java-focused-resume.log`, `java-full-resume.log`, `node-resume.log`, `chrome-resume.log` và `invariants-resume.log`. Rà năm log xác minh theo mẫu JWT và mật khẩu fixture không phát hiện giá trị khớp.

Nhánh `feature/admission`, HEAD `cbf5be7c2e956397cffe4e962a12c2acce197953`. Từ 55 modified, 14 untracked sang 55 modified, 15 untracked. 66 trong 69 file có sẵn khớp SHA-256. Ba file còn lại chỉ được cập nhật tài liệu QA và contract. Báo cáo này là file mới. Staging trống ở cả hai mốc.

## Quyết định còn mở

- **H07-B PARTIAL.** Reset email đặt mật khẩu khôi phục và giữ cờ đang bật của nhân sự. BA cần xác nhận reset email có thay thế bước đổi mật khẩu lần đầu hay không.
- **H07 tổng PARTIAL.** H07-A vẫn còn quyết định về lưu refresh token chống XSS và chia sẻ phiên giữa các tab.
- **FR-IAM-02 PARTIAL.** API tạo nhân sự đã kiểm chứng, UI quản trị nhân sự riêng chưa có.
- **FR-IAM-03 PARTIAL.** Login, rotation và logout vẫn hoạt động với phiên giới hạn. Cơ chế lưu phiên H07-A còn giới hạn.
- **FR-IAM-06 PARTIAL.** Luồng email và reset đã kiểm chứng với SMTP loopback. Còn quyết định reset nhân sự và nghiệm thu SMTP vận hành.
- **FR-IAM-07 PARTIAL.** Audit tạo tài khoản và đổi mật khẩu đã kiểm chứng qua PostgreSQL. UI audit và trusted proxy còn mở.
- Chrome dùng API fixtures. Các ca HTTP và JWT thật chạy riêng trên PostgreSQL QA. Lỗi `GET /api/admission/rooms` trả 500 với `lower(bytea)` là quan sát từ H07-A, nằm ngoài phạm vi lượt này.

Bước tiếp theo đề xuất là audit các workflow và màn hình Admission V1 còn thiếu.
