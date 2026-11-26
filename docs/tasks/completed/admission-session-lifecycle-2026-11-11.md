# QA vòng đời phiên Admission H07-A — 12/11/2026

H07-A **PARTIAL**. Vòng đời phiên trong tab đã được nối với backend. Lưu refresh token chống XSS và chia sẻ phiên giữa nhiều tab còn phụ thuộc kiến trúc xác thực. H07 tổng **PARTIAL** do H07-B về validation nhân sự và first-password còn thiếu.

## Tái hiện và phạm vi

- `account.js` từng gọi `setSession(access_token, user)`, bỏ refresh token và giả định response đủ dữ liệu. UI dùng `user_id` từ backend mà thiếu bước chuẩn hóa sang `id` cho các trang Admission. Wrapper dùng chung xử lý 401 bằng xóa local và chuyển trang. Nút logout dùng chung chưa gọi revoke.
- `AuthController` đã có login/refresh/logout nhưng thiếu `GET /auth/me` nêu trong BA và API Catalog. Filter JWT chấp nhận refresh JWT làm Bearer. Logout thiếu đối chiếu chủ refresh token, alias `/auth/logout` nằm trong matcher công khai.
- PostgreSQL RED xác nhận rotation/replay dưới cạnh tranh và luồng refresh khi access hết hạn đã hoạt động trước sửa. Hai ca này qua ngay ở baseline. Ba ca khác tái hiện thiếu `me`, refresh Bearer được chấp nhận, và logout token của tài khoản khác trả 200.
- Node RED có 4/6 ca thất bại đúng mục tiêu gồm lưu cặp token và chuẩn hóa user, payload thiếu vẫn redirect, 401 đồng thời không rotation, logout không gửi revoke. Sai mật khẩu/tài khoản khóa/lỗi server ở form và 403 từ API là các ca đã qua baseline.

## Thay đổi

- `admission-session.js` chỉ tải trên các trang Admission. Login đòi response thành công với access token, refresh token và user hợp lệ. Cặp token được lưu trong một bản ghi `sessionStorage` để giữ phiên qua điều hướng hoặc reload cùng tab. UI khôi phục phiên mới bằng `GET auth/me`, hiển thị tên qua `textContent` và ẩn dữ liệu bảo vệ khi mất phiên.
- Request đi qua adapter Admission. Access hết hạn được làm mới trước khi gửi. 401 do access bị từ chối chỉ retry một lần, các request chờ dùng cặp mới, 401 đến muộn dùng lại access đã xoay vòng. 403 giữ phiên. 401 do sai mật khẩu hiện tại trong change-password được hiển thị ngay. Các form email/login vẫn hiển thị lỗi tại chỗ.
- Web Locks tuần tự hóa rotation/logout theo ID phiên ngẫu nhiên. `localStorage` chỉ nhận marker ID, phiên bản hoặc trạng thái đang rotation. Nếu một tab giữ bản sao cũ thì tab đó xóa local sau khi thấy marker đổi, không phát lại refresh token cũ. Marker pending sau gián đoạn cũng chặn replay. Trình duyệt thiếu Web Locks kết thúc phiên nếu cần refresh. Các lần đăng nhập độc lập vẫn có phiên riêng. [W3C Web Locks](https://www.w3.org/TR/web-locks/) · [HTML Web Storage](https://html.spec.whatwg.org/multipage/webstorage.html)
- Logout bỏ ngay cặp token khỏi storage của tab và ngừng dùng access. Nếu access hết hạn, adapter lấy cặp mới một lần để thu hồi. Khi rotation đã gửi trong cùng tab, logout dùng kết quả mới nhất của rotation. UI báo rõ khi chỉ thoát local. Tab còn bản sao nhận thay đổi marker và cập nhật UI.
- Backend chỉ cho access JWT làm Bearer. `GET /auth/me` trả `id`, `email`, `role`, `full_name` từ principal hiện tại. Logout yêu cầu access chính chủ, ghi audit khi thu hồi lần đầu và cho phép gửi lại cùng token đã thu hồi. Cả route chuẩn và alias dùng cùng quyền. Hợp đồng payload login/refresh được giữ. Access JWT cũ hợp lệ vẫn được Admission chấp nhận đến hạn, còn refresh JWT được dùng đúng mục đích.

## File sửa và tạo

- Backend `AuthController.java`, `AuthServiceImpl.java`, `SecurityConfig.java`, `JwtAuthenticationFilter.java`, `JwtTokenProvider.java`.
- Frontend `admission-session.js` (mới), `admission-ui.js`, `account.js`, `access.css` và bảy trang `account.html`, `bi-dashboard.html`, `classes.html`, `courses.html`, `enrollments.html`, `rooms.html`, `test.html`. Thay đổi ở các trang H04/H05/H06 chỉ nối vào vòng đời phiên và kiểm tra hiển thị.
- Test `AuthServiceTest.java`, `AdmissionPostgresTest.java`, `regression.cjs`, `browser.cjs` và `session.cjs` (mới). Tài liệu gồm API Catalog, QA progress, bug report và báo cáo này.

## Kiểm thử

- RED PostgreSQL dùng roster làm endpoint bảo vệ để cô lập xác thực. Lượt thử trước đó phát hiện `GET /api/admission/rooms` trên fixture trả 500 do SQL `lower(bytea)`, nằm ngoài phạm vi H07-A.
- PostgreSQL dùng HTTP thật và JWT ký thật. Hai request chờ cùng khóa row, một response 200 và một 401. Replay thu hồi cặp vừa cấp và ghi audit. Kiểm tra logout khác chủ, gửi lặp, alias không token, access hết hạn và revoke thật. Kiểm tra output kiểm thử cạnh tranh không chứa token đã cấp.
- Chrome chạy API fixtures và Web Locks của trình duyệt thật. Bao gồm hai tab cùng bản sao, một lần rotation, tab cũ về đăng nhập, logout một lần và tab còn bản sao mất phiên. Regression quyền ADMIN/STAFF/STUDENT/GUEST/TEACHER được chạy cùng luồng khảo thí và roster.

| Bộ kiểm thử | Kết quả cuối |
|---|---|
| Java unit/service và MockMvc trên JDK 21 | 119/119 PASS |
| HTTP/PostgreSQL 16.15 thật | 101/101 PASS, gồm 5 ca session mới |
| Tổng Maven Admission | 220/220 PASS, 0 lỗi, 0 skip |
| Node | 36/36 PASS, gồm 18 ca session và 18 regression |
| Chrome headless với API fixtures | 20 kiểm tra PASS, gồm 4 kiểm tra bổ sung về phiên và phân quyền |
| Invariants và `git diff --check` | 15/15 invariants PASS, diff check exit 0 |

Log ở `C:/Users/Admin/admission-work/qa-admission-h07a-20261112/`. RED gồm `node-red.log`, `java-red-verified.log`. Kết quả cuối gồm `java-full.log`, `node-final-verified.log`, `chrome-final.log`, `invariants-final.log`. Kiểm tra output cạnh tranh đối chiếu token đã cấp và rà soát log test theo mẫu JWT đều không phát hiện token.

Nhánh `feature/admission`, HEAD `cbf5be7c2e956397cffe4e962a12c2acce197953`. Ban đầu có 37 modified, 11 untracked. Cuối cùng có 50 modified, 14 untracked. Trong 48 file có sẵn, 39 file khớp SHA-256 và 9 file chồng lấp đã được đối chiếu từng diff. H01/H04/H05/H06 được bảo toàn, staging trống ở cả hai mốc.

## Phụ thuộc và trạng thái

- **FR-IAM-03 PARTIAL.** Cặp JWT đã qua unit, HTTP/PG và Chrome fixtures. Refresh trong `sessionStorage` vẫn đọc được từ JavaScript cùng origin. Chưa có contract cookie HttpOnly. Client legacy giữ access tới hạn rồi đăng nhập qua Admission để có refresh.
- **FR-IAM-04 DONE cho rotation/replay.** DB vẫn lưu refresh token gốc, chuyển sang hash là hardening còn mở. Access JWT stateless đã cấp có thể dùng đến hạn sau khi refresh token bị thu hồi.
- **FR-IAM-07 PARTIAL.** Logout/replay có audit được kiểm chứng. UI audit và trusted proxy chưa nghiệm thu.
- Browser chưa chạy E2E với backend thật. Nếu mất kết nối đúng lúc rotate hoặc revoke, client xóa local và yêu cầu đăng nhập lại. Kết quả revoke chỉ được báo khi server xác nhận.
- Bước tiếp theo đề xuất là H07-B gồm validation tài khoản nhân sự, phone/gender/DOB, `must_change_password` và first-password enforcement.
