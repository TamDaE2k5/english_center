# QA H04 — ID tài khoản và CSV Admission, 11/11/2026

H04 **DONE** cho lỗi gắn nhãn mã Student trong roster. FR-OPS-01 và FR-OPS-02 **PARTIAL**.

## Nguyên nhân và phạm vi

- Roster trong `ExportServiceImpl.java` trước sửa dùng `String.format("STU-%d", e.getStudentId())` cho cột `Mã học viên`. `admission.enrollments.student_id` là FK đến `admission.users.id` theo R-ID-1 và DEC-02. Student Service quản lý ID hồ sơ và Student code.
- DTO ACK của Student có `student_id` và `student_code`. Outbox Admission lưu `student_profile_id`, chưa lưu Student code. ACK hiện thiếu `user_id` để kiểm chứng độc lập liên kết. Export chỉ sử dụng khóa tài khoản có sẵn trong Admission.
- Bảng điểm lấy dữ liệu `test_submissions` theo bộ đề và dùng `USR-{user_id}` cho cột `Mã thí sinh`. Đây là định danh thí sinh Admission. Lỗi nhãn Student chỉ xuất hiện ở roster.

## Hành vi và contract

- Roster dùng cột thứ ba `ID tài khoản Admission` với ID số lấy trực tiếp từ enrollment, kể cả khi thiếu hồ sơ Student. Nếu đối tượng không có ID, CSV ghi ô trống. Endpoint, thứ tự cột, BOM UTF-8, CRLF và tên file tải xuống tương thích. Consumer đọc theo tiêu đề cũ hoặc tiền tố `STU-` cần đổi parser.
- Controller kiểm tra ID dương, trả `400 ADM_VALIDATION_ERROR` cho ID không hợp lệ và 404 khi lớp hoặc bộ đề không tồn tại. Đường đọc chỉ truy vấn dữ liệu của đúng lớp hoặc bộ đề. Truy vấn roster tiếp tục lấy các đơn của lớp theo hành vi hiện có.
- Ma trận [BA/BI v3.6 §2.2](../../../services/admission/docs/admission-ba-bi-spec.md) cho STAFF/ADMIN xuất cả hai báo cáo. TEACHER, kể cả người được gán lớp, và STUDENT nhận 403 khi có phiên hợp lệ. Thiếu JWT, token sai/hết hạn, tài khoản không tồn tại hoặc bị vô hiệu hóa nhận 401. TASK-ADM-50 và catalog cũ có quyền TEACHER cho roster. H04 áp dụng quyền BA tại matcher HTTP và service.
- Cả hai file dùng cùng helper CSV. Helper giữ escaping dấu phẩy, nháy kép, xuống dòng và thêm dấu nháy đơn cho các tiền tố công thức, kể cả dạng full-width, whitespace Unicode và LF đầu ô. Kiểm thử xác nhận bytes xuất ra. Chưa kiểm tra thao tác lưu lại và mở lại bằng Excel. [OWASP CSV Injection](https://community.owasp.org/attacks/CSV_Injection)
- Trang lớp tiếp tục dùng Bearer JWT, tải Blob với tên `class-{id}.csv` và hiện lỗi nếu request thất bại. Chrome kiểm tra nút thật và nội dung file bằng API fixtures. Trình duyệt và backend HTTP/PostgreSQL được kiểm tra bằng các bộ riêng.

## Tệp sửa và tạo

- Backend `SecurityConfig.java`, `ExportController.java`, `ExportService.java`, `ExportServiceImpl.java`, `CsvExportHelper.java`.
- Test `ExportServiceTest.java`, `CsvExportSecurityTest.java`, `ExportHttpSecurityTest.java` (mới), `AdmissionPostgresTest.java`, `browser.cjs`.
- Tài liệu `API_CATALOG.md`, `admission-quality-progress.md`, `admission-bug-report.md` và báo cáo này. H04 không có migration.

## Kiểm thử

Lượt RED chạy 9 ca trên JDK 21, có 5 lỗi đúng mục tiêu. Hai ca roster xác nhận nhãn và ID sai khi có hoặc thiếu hồ sơ đính kèm. MockMvc xác nhận TEACHER nhận 200 và ID sai kiểu nhận 500. Test an toàn CSV xác nhận LF đầu ô chưa được xử lý. Bốn ca còn lại qua trước sửa, gồm định dạng bảng điểm và escaping cơ bản. Lượt GREEN chạy lại 9 ca, cả 9 qua.

PostgreSQL kiểm tra nội dung hai file, scope lớp/bộ đề, dữ liệu ngoài phạm vi và trường nội bộ không xuất, snapshot enrollment trước/sau, tên tệp và content type. Ma trận JWT gồm hai vai trò được phép, TEACHER được gán/không được gán lớp, STUDENT, token hết hạn/sai chữ ký/sai định dạng, tài khoản thiếu hoặc bị khóa và request không token. Lượt đầy đủ đầu tiên có một lỗi fixture vì STUDENT chưa xác minh email nhận 401 ở bước xác thực. Fixture được bổ sung xác minh để kiểm tra đúng 403 ở bước phân quyền.

| Bộ kiểm thử | Kết quả cuối |
|---|---|
| Java unit/service và MockMvc, JDK 21 | 119/119 PASS, gồm 112 unit/service và 7 MockMvc |
| HTTP/Spring Boot với PostgreSQL 16.15 | 96/96 PASS, gồm 4 ca export mới |
| Maven Admission | 215/215 PASS, 0 fail, 0 skip |
| Node | 18/18 PASS |
| Chrome headless với API fixtures | 16 kiểm tra PASS, gồm 7 trang và 2 ca tải roster/thất bại |
| Invariants và `git diff --check` | 15/15 invariants PASS, diff check PASS |

Log cục bộ trong `C:/Users/Admin/admission-work/qa-admission-h04-20261111/`. Kết quả RED ở `java-red-verified.log`, GREEN ở `java-green.log`. Lượt đầy đủ cuối ở `java-verified.log`, cùng `node.log`, `chrome.log`, `invariants.log` và Git status trước/sau.

Nhánh `feature/admission` và HEAD `cbf5be7c2e956397cffe4e962a12c2acce197953` được giữ nguyên. Trước sửa có 30 file tracked đã sửa và 9 file untracked. Sau sửa có 37 file tracked đã sửa và 11 file untracked. Trong 39 file ban đầu, 34 file khớp SHA-256 và diff của 5 file trùng phạm vi chỉ có thay đổi H04. H01/H05/H06 được bảo toàn.

## Giới hạn

- FR-OPS-01 cần nguồn mapping Student code đã xác nhận. BA và TASK-ADM-50 cần thống nhất quyền TEACHER, phạm vi trạng thái roster và nhu cầu mã Student thật trong báo cáo.
- FR-OPS-02 có API CSV theo bộ đề đã qua HTTP/PostgreSQL. UI tải kết quả và lọc thời gian nêu trong TASK-ADM-50 còn thiếu.
- Bước tiếp theo đề xuất là H07 về frontend session và account lifecycle.
