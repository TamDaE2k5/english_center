# QA Placement Test H05 — 11/11/2026

H05, FR-TST-03 và FR-TST-04 ở trạng thái **PARTIAL**. Đã sửa đồng hồ, khôi phục lượt và kiểm soát việc nộp. BA/team cần quyết định cách chốt và chấm bài khi hết giờ hoặc khi bài trắng.

## Tái hiện

- `test.html` lấy `expires_at - started_at` và giảm một giây trên mỗi callback. Khi API trả muộn hoặc tab bị tạm dừng, UI hiển thị nhiều thời gian hơn phần thực còn lại. Refresh làm mất lượt trên UI. Gửi bài lỗi dừng timer. Countdown cũ còn gọi submit khi về 0.
- `PlacementTestServiceImpl.submitAndGrade` kiểm tra deadline với giờ server nhưng nạp lượt mà chưa khóa. Hai request cùng có thể vào luồng chấm dù UNIQUE ngăn lưu hai bài. Transaction rollback khi ném lỗi khiến bản ghi đã được gán `EXPIRED` vẫn là `STARTED`. Trạng thái `EXPIRED` có deadline tương lai vẫn lọt qua kiểm tra cũ.
- Trước sửa, 5 ca Node mới thất bại và 12 ca cũ qua. Lượt Java tái hiện trên PostgreSQL 16.15 có 4 lỗi trên 9 ca gồm ba lỗi HTTP/DB (resume, expiry rollback, cạnh tranh khi chấm) và một lỗi unit (trạng thái EXPIRED). Ca thử lại sau lỗi chấm đã qua trước sửa.

## Hành vi sau sửa

- Start giữ các trường cũ và trả thêm `server_now`, `status`. UI gắn chênh lệch `expires_at - server_now` vào `performance.now()`, trừ nửa round-trip đo được để ước lượng độ trễ. UI tính lại khi timer chạy hoặc khi quay về tab. Mạng bất đối xứng và thời gian xử lý response có thể làm ước lượng lệch quanh deadline. Server quyết định kết quả nộp.
- `GET /api/admission/tests/public/attempts/{id}` đọc deadline cũ và trạng thái cho STUDENT chính chủ đã xác minh. Đề đã ẩn đáp án chỉ được trả cho lượt còn hạn. UI lưu ID và đáp án tạm trong `sessionStorage` theo tài khoản, để khôi phục trong cùng tab. Resume phụ thuộc việc trình duyệt cho phép lưu phiên.
- Khi về 0, UI khóa bài. Request nộp đã gửi tiếp tục chờ kết quả server. Khi submit lỗi, UI thử đọc lại trạng thái để phân biệt lượt còn làm, hết hạn và đã nộp.
- Backend khóa theo ID lượt, lấy giờ sau khi khóa và chấm một lần. Theo TASK-ADM-31, mốc đúng bằng `expires_at` được chấp nhận nếu quyền vẫn hợp lệ. Sau mốc này, request nộp hợp lệ về quyền bị từ chối bằng `ADM_ATTEMPT_EXPIRED` và lượt STARTED được ghi EXPIRED. Resume phản ánh hết hạn theo giờ server bằng thao tác đọc. Lượt không có yêu cầu nộp muộn có thể vẫn là STARTED trong DB, dù đã hết hạn và không nộp được.
- BA/BI FR-TST-03 yêu cầu từ chối bài quá hạn. Luồng BA/BI mục 3.4 và TASK-ADM-62 mô tả gửi tự động khi đồng hồ về 0, còn cơ chế server tự chốt chưa được thống nhất với TASK-ADM-31. Các ca nộp hợp lệ vẫn dùng cách chấm hiện tại. Câu bỏ trống không cộng điểm và API yêu cầu ít nhất một đáp án. Chưa có luồng lưu nháp server hoặc chấm tự động khi hết giờ.

## Phạm vi file

- Backend `PlacementTestController.java`, `TakeTestResponse.java`, `TestAttemptRepository.java`, `PlacementTestService.java`, `PlacementTestServiceImpl.java`, `PublicPlacementTestService.java` và `TestAttemptExpiredException.java`.
- Frontend `test.html`. Regression `PlacementTestServiceTest.java`, `PublicPlacementServiceTest.java`, `AdmissionRegressionTest.java`, `AdmissionPostgresTest.java`, `regression.cjs` và `browser.cjs`.
- Tài liệu API, tiến độ và báo cáo QA. Bản sửa sử dụng bảng và ràng buộc UNIQUE hiện có. Không có migration H05.

## Kiểm thử

| Bộ kiểm thử | Kết quả |
|---|---|
| Java unit và MockMvc trên JDK 21 | 115/115 PASS, gồm 110 unit/service và 5 MockMvc. Ca chấm ba kỹ năng chạy trước hạn và đúng deadline. |
| HTTP/Spring Boot với PostgreSQL 16.15 | 92/92 PASS. Bao gồm resume chính chủ, nộp muộn ghi EXPIRED, hai request chỉ chấm một lần, chờ khóa qua hạn, lỗi chấm rồi thử lại, quyền, dữ liệu submit giả và validation. |
| Tổng Maven Admission | 207/207 PASS, 0 fail, 0 skip. |
| Node | 18/18 PASS, gồm 6 ca H05. Có response chậm, giờ máy lệch, quay lại tab, refresh, API báo hết hạn và chờ kết quả qua mốc 0. |
| Chrome headless | 14 kiểm tra PASS gồm 7 trang, 2 ca Placement Test và các regression phân quyền, BI, CASH, gia hạn. API dùng fixtures. |
| Invariants | 15/15 PASS. |
| `git diff --check` | PASS. |

Log cục bộ tại `C:/Users/Admin/admission-work/qa-admission-h05-20261111/`. Các lượt tái hiện dùng `java-red-verified.log`, `node-red-verified.log`. Kết quả cuối dùng `java-full.log`, `node-verified.log`, `chrome-verified.log`, `invariants.log`. Lượt đầu bị sandbox chặn truy cập loopback và lượt thử Chrome có JWT fixture hết hạn theo giờ giả không được tính vào kết quả.

Nhánh `feature/admission` và HEAD `cbf5be7c2e956397cffe4e962a12c2acce197953` được giữ nguyên. Trước sửa có 21 file tracked đã sửa và 7 file untracked. Sau sửa có 30 file tracked đã sửa và 9 file untracked. Trong 28 file ban đầu, 21 file khớp SHA-256 và 7 file trùng phạm vi được đối chiếu với bản chụp trước sửa. Các thay đổi H01/H06 được bảo toàn.

## Quyết định cần chốt

BA/team cần chọn đóng lượt khi hết giờ hoặc chấm tự động từ các đáp án đã được server nhận trước hạn. Nếu chọn chấm tự động, cần thống nhất cách lưu đáp án, cách chấm bài trắng và thời điểm công bố kết quả trước khi triển khai.
