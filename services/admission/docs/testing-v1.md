# Kiểm thử Admission độc lập


Mốc sức chứa/booking mới nhất: **171 Java tests pass,58 PostgreSQL tests, không skip;15 invariants;10 Node;Chrome7 trang và BI role pass**. Không cần migration mới. Xem [room-booking-qa.md](room-booking-qa.md). Log `C:/Users/Admin/admission-work/qa-admission-room-booking-20261110/verified.log`. Giữ dữ liệu/migration cũ, không E2E hệ thống.


Mốc BI mới nhất 10/11/2026: **161 Java tests pass, 48 PostgreSQL tests, không skip; 15 invariants; 10 Node regressions; Chrome7 trang và BI STAFF/ADMIN/guest pass với API fixtures**. Không có migration BI mới; giữ các migration QA trước. Expected dataset và matrix tại [bi-kpi-qa.md](bi-kpi-qa.md); log `C:/Users/Admin/admission-work/qa-admission-bi-20261110/verified-final.log`. Không chạy E2E hoặc tổng nghiệm thu V1.


Mốc retry Student 10/11/2026: **153 Java tests pass, 40 PostgreSQL tests, không skip; 15 invariants, 7 Node regressions**. Áp dụng migration `services/admission/backend/src/main/resources/db/admission-student-sync.sql` sau các migration Admission trước. Cổng Student được mock; helper/outbox/worker/transaction và PostgreSQL là thật trong các test retry. Gửi job mặc định tắt ngoài test. Xem [student-sync-retry.md](student-sync-retry.md). Log cuối `C:/Users/Admin/admission-work/qa-admission-retry-20261110/verified-legacy.log`. Không chạy lại Chrome hoặc E2E toàn hệ thống trong lượt retry.

Mốc tiền mặt 10/11/2026 mới nhất: **143 Java tests pass, gồm 31 PostgreSQL tests, không skip; 15 invariants; 7 Node regressions**. Chrome kiểm tra lại 7 trang với API fixtures, gồm gửi CASH/chờ Finance. Log `C:\Users\Admin\admission-work\qa-admission-cash-20261110\java-final.log`. Áp dụng thêm migration `services/admission/backend/src/main/resources/db/admission-cash-confirmation.sql` trước khi chạy; xem [cash-confirmation.md](cash-confirmation.md). Không chứng nhận Finance đã thu tiền. Các mốc cascade/QA cũ bên dưới được giữ làm lịch sử.

Kết quả sau cascade cancellation ngày 09/11/2026: **131 Java tests pass, gồm 19 PostgreSQL integration tests, không skip; 15 invariant tests; 5 Node frontend regression tests**. Chrome kiểm tra 7 trang thuộc đợt QA trước, không chạy lại trong đợt cascade. Chi tiết phạm vi và tồn đọng: [admission-quality-progress.md](admission-quality-progress.md). Không chạy E2E toàn hệ thống.

Đợt cascade cần áp dụng thêm `services/admission/backend/src/main/resources/db/admission-class-cancellation.sql` vào đúng database trước khi chạy. Migration tạo bảng yêu cầu đối soát nội bộ, không tạo refund tại Finance. Kết quả cuối tại `C:\Users\Admin\admission-work\cancel-verified-legacy.log`; xem [class-cancellation.md](class-cancellation.md).

Yêu cầu Java 21, Maven 3.9.x, PostgreSQL 16. Không cần sửa Java/POM để chạy trên JDK 25.

## Unit, HTTP security và invariant

Từ gốc repository:

```powershell
mvn -B -f services/admission/backend/pom.xml test
python -m unittest discover -s tests/invariants -p "test_*.py" -v
```

Nếu không bật PostgreSQL theo phần dưới, 58 test trong `AdmissionPostgresTest` sẽ **skip**. Không dùng kết quả đó làm bằng chứng tích hợp đã pass.

## PostgreSQL thật

Chuẩn bị một PostgreSQL cục bộ dành riêng cho QA, tạo database **admission_qa_v1** và nạp `scripts/init-db.sql` bằng `psql -v ON_ERROR_STOP=1`. Không chạy fixture này trên DB có dữ liệu vận hành. Test thêm bản ghi với tiền tố ngẫu nhiên `qa-`; không xóa/reset dữ liệu có sẵn.

Sau khi có schema, áp dụng migration audit trước khi chạy test:

```powershell
psql -v ON_ERROR_STOP=1 -f services/admission/backend/src/main/resources/db/admission-account-audit.sql
```

Chọn host/port/user/database đúng DB QA bằng các tham số `psql` hoặc biến môi trường. Migration chỉ thay CHECK constraint trong schema Admission, không tự chạy khi app khởi động. Hướng dẫn SMTP: [account-email-setup.md](account-email-setup.md).

Ví dụ PowerShell (mật khẩu lấy từ cấu hình QA cục bộ, không ghi vào Git):

```powershell
$env:ADMISSION_PG_TEST = 'true'
$env:ADMISSION_QA_JDBC_URL = 'jdbc:postgresql://127.0.0.1:55432/admission_qa_v1?currentSchema=admission'
$env:ADMISSION_QA_DB_PASSWORD = $env:QA_POSTGRES_PASSWORD
mvn -B -f services/admission/backend/pom.xml test
```

User DB của fixture là `postgres`; URL phải có đúng tên DB `admission_qa_v1`. Test tắt expiration scheduler, dùng server Spring Boot cổng ngẫu nhiên và mock hai cổng liên service Finance/Student. Không có container/Testcontainers hay H2 thay thế PostgreSQL trong fixture này. SMTP loopback dùng cổng ngẫu nhiên và giữ email trong bộ nhớ; không gửi ra hộp thư bên ngoài. Mỗi test có IP nguồn QA riêng để audit của test khác không làm kích hoạt nhầm guard chống brute-force; không xóa audit hoặc tắt guard.

Các test DB xác nhận:

- Hai transaction đăng ký slot cuối: transaction thứ hai chờ khóa, chỉ một PENDING được commit.
- Duyệt PENDING quá hạn bị từ chối dù scheduler tắt; webhook muộn được ghi sổ, chuyển EXPIRED và xử lý replay idempotent.
- HTTP start/submit với token lượt thi: thiếu token 403, token đúng 200, JSONB lưu đúng, nộp lại bị chặn, không mạo danh tài khoản bằng email hiện hữu.
- Đăng ký -> SMTP -> xác minh -> setup -> login -> quên/reset/đổi mật khẩu; token hash, hết hạn, purpose và replay/concurrency.
- Audit LOGIN_FAILED được commit; 5 lần sai kích hoạt 403. Kiểm tra audit tạo nhân sự, lookup giáo viên Staff và khóa access JWT cũ.

Đợt trước ghi nhận 110 Java tests. Kết quả hiện tại 125 tests được lưu tại `C:\Users\Admin\admission-work\qa-admission-resume-20261109\java-verification.log`; log invariant/Node/Chrome ở cùng thư mục. Công cụ Java/PostgreSQL tại `qa-v1`; DB riêng dùng cổng 55432. Không yêu cầu Docker để chạy fixture PostgreSQL portable này.

PostgreSQL QA có thể được giữ chạy giữa các phiên. Kiểm tra bằng `pg_ctl status` trước khi khởi động, tránh chạy trùng. Trên máy này lệnh khởi động là:

```powershell
& C:\Users\Admin\admission-work\qa-v1\postgres\pgsql\bin\pg_ctl.exe -D C:\Users\Admin\admission-work\qa-v1\pgdata -l C:\Users\Admin\admission-work\qa-v1\postgres.log -o '-h 127.0.0.1 -p 55432' -w start
```

Không cần nạp lại schema vào data directory đã có. Mật khẩu QA lưu cục bộ ngoài repository tại `qa-v1\pg-password.txt`, không đưa vào báo cáo hoặc commit.

## Thay đổi contract khảo thí công khai

Response start có `data.attempt_token`. Lượt thi chính thức chỉ được tạo bởi STUDENT đã xác minh email, thiết lập mật khẩu và đăng nhập. Khi submit, dùng JWT chính chủ hoặc `attempt_token` cùng `attempt_id` và `answers`; server vẫn kiểm tra chủ lượt thi đang hoạt động và đã xác minh. Token chỉ cho phép nộp đúng lượt thi đến deadline; không dùng làm token đăng nhập. Không gửi token lượt thi vào URL hoặc log.

Khách không được dùng `user_id` tùy ý hoặc email tự khai để nhận danh tính tài khoản có sẵn. Khách mới đi qua trang `/admission/account.html`; link email VERIFY/RESET và SETUP token có mục đích riêng với token bài thi.

## Frontend regression

```powershell
node services/admission/frontend/tests/regression.cjs
node services/admission/frontend/tests/browser.cjs
```

Node regression không cần package ngoài. Browser test cần `playwright` và Google Chrome; có thể cài package trong thư mục QA riêng rồi đặt `NODE_PATH` tới `node_modules` đó. Trên máy QA hiện tại dùng `C:\Users\Admin\admission-work\qa-admission-quality\browser\node_modules`. Browser test tự dựng static server loopback, dùng API fixtures và tự đóng Chrome; không chạy các phân hệ khác.
