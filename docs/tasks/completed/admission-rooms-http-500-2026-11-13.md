# QA và bàn giao Admission Rooms — 13/11/2026

`GET /api/admission/rooms` đã sửa lỗi 500 khi bộ lọc chi nhánh bị bỏ trống ở lần chạy truy vấn đầu tiên. Endpoint, tham số, JSON, quyền, giao diện và schema giữ nguyên.

## Bằng chứng và thay đổi

Lượt đầu chạy năm ca HTTP trên PostgreSQL 16.15 và đều qua. Báo cáo Surefire cho thấy ca có `branch_name` chạy trước ca không lọc. Chạy ca không lọc riêng trong JVM mới tái hiện HTTP 500, SQLSTATE 42883. SQL Hibernate sinh ra có `lower(('%'||?||'%'))`. Khi `branch_name` được bind NULL ở lần chạy đầu, biểu thức ghép bị PostgreSQL phân giải thành kiểu `bytea` và `lower(bytea)` thất bại. Cột `branch_name` trong schema vẫn là VARCHAR. Nhánh `OR` kiểm tra NULL trong WHERE không ngăn lỗi phân giải kiểu của biểu thức còn lại.

- `RoomRepository.java` chỉ định kiểu `String` cho tham số chi nhánh trong `CONCAT` để Hibernate sinh phép ép kiểu chuỗi trước khi ghép mẫu LIKE. Giữ truy vấn có bind parameter, thứ tự theo mã phòng và các điều kiện lọc hiện có.
- `AdmissionPostgresTest.java` thêm năm ca HTTP với JWT thật. Ca không lọc buộc Spring context mới để lỗi cũ không bị che bởi thứ tự chạy. Các ca còn lại kiểm tra lọc theo chuỗi con không phân biệt hoa thường, kết hợp trạng thái/sức chứa, phòng có chi nhánh NULL, tham số rỗng, kết quả rỗng, JSON và quyền. STAFF/ADMIN nhận 200, không có token nhận 401, TEACHER/STUDENT nhận 403. Lọc `branch_name=` giữ hành vi LIKE chuỗi rỗng và loại các bản ghi có chi nhánh NULL.

## Kết quả kiểm thử

| Bộ kiểm thử | Kết quả |
|---|---|
| RED, JVM mới | 1/1 ca tái hiện 500 và SQLSTATE 42883 |
| Focused, JDK 21 | 21/21 PASS, gồm 6 RoomServiceTest, 5 Rooms HTTP và 10 Room/Class capacity/booking trên PostgreSQL |
| Maven Admission, JDK 21 | 231/231 PASS, gồm 112 ca trên PostgreSQL 16.15 thật. 0 lỗi, 0 skip |
| Node | 37/37 PASS |
| Chrome | 22 kiểm tra PASS với API fixtures |
| Invariants và `git diff --check` | 15/15 invariants PASS, diff check exit 0 |

Log chính gồm `java-cold-red.log`, `java-green.log`, `java-full.log`, `node.log`, `chrome.log` và `invariants.log`. Database QA hiện có được dùng qua loopback. Fixture chỉ chèn bản ghi QA có mã riêng, không reset dữ liệu.

Nhánh `feature/admission`, HEAD `cbf5be7c2e956397cffe4e962a12c2acce197953`. Từ 55 modified, 15 untracked sang 56 modified, 16 untracked. 67/70 file ban đầu khớp SHA-256. Ba file dùng chung chỉ thêm test Rooms hoặc cập nhật đúng mục tài liệu liên quan. Staging trống. Rà phần thêm và file untracked theo mẫu JWT/key không có kết quả khớp. Credential và cấu hình cục bộ dùng để chạy test nằm ngoài source.

## Bàn giao cho Tâm

- Sau khi được duyệt, commit Rooms gồm `RoomRepository.java`, phần năm ca và hai helper Rooms trong `AdmissionPostgresTest.java`, mục Rooms trong `docs/admission-bug-report.md`, dòng FAC-01 trong `admission-quality-progress.md` và báo cáo này. Ba file dùng chung với H01–H07 cần chọn đúng hunk nếu tách commit.
- Đợt bàn giao H01–H07 riêng gồm các source, test, SQL bổ sung H01/H06 và tài liệu đã review trong danh sách `git-initial.txt`. Các file Admission untracked có chủ đích được liệt kê ở đó, gồm SQL, source, test và QA Markdown. Review từng nhóm trước khi đưa vào Git.
- Giữ bên ngoài Git các thư mục `qa-admission-*`, `qa-v1`, cache Maven, `target/`, `node_modules/`, file `.env`, mật khẩu QA, log, bản chụp diff và dữ liệu PostgreSQL. Các log lượt này nằm ngoài repo tại `C:/Users/Admin/admission-work/qa-admission-rooms-20261113/`.
- Frontend tiếp tục dùng `GET /api/admission/rooms` khi tải trang phòng và khi mở lớp. Smoke test Chrome dùng API fixtures. HTTP/JPA/PostgreSQL thật được xác minh ở bộ Maven riêng.

## Các điểm còn mở trước nghiệm thu V1

| Hạng mục | Phần cần review hoặc chốt |
|---|---|
| H01 | PARTIAL. Đối chiếu đủ tiền, mapping hóa đơn, chu kỳ phát hành lại và hợp đồng Finance thật. |
| H02/H03 | Guard sức chứa và khóa lịch PostgreSQL đã có regression. Chính sách lịch CUSTOM còn cần định nghĩa. |
| H04 | Lỗi mã tự dựng đã sửa. FR-OPS vẫn PARTIAL vì mapping Student code và các phần UI/báo cáo chưa nghiệm thu. |
| H05 | PARTIAL. Chốt tự thu bài hết giờ, lưu đáp án phía server và chấm bài trắng. |
| H06 | PARTIAL. Chốt quyền phục hồi giữ chỗ quá hạn, giới hạn tổng và số lần gia hạn, khung 24/48 so với 1–72 giờ. |
| H07 | PARTIAL. H07-A còn kiến trúc lưu/chia sẻ phiên. H07-B còn quyết định reset email, UI quản trị nhân sự và nghiệm thu SMTP/audit. |
