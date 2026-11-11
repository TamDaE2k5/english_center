# Admission BI: đối chiếu BA và QA — 10/11/2026

**Đánh giá BI: PARTIAL.** Các công thức từ dữ liệu Admission hiện có đã triển khai/kiểm thử; doanh thu Finance và các chiều dữ liệu chưa tồn tại không được giả lập. Chưa tổng nghiệm thu Admission V1, không chạy E2E toàn hệ thống.

## Căn cứ và quy tắc chung

Nguồn: admission-ba-bi-spec.md mục 6/7/9 (đặc biệt bảng 7.1 v3.6), admission-schema-spec.md, admission-tech-spec.md, admission-uml-spec.md, tasks/API_CATALOG.md và TASK-ADM-51/52/53. Bảng BA mới phân biệt cohort/attempt/user/đơn được ưu tiên hơn các phép chia cũ trong phase task. Công thức học phí niêm yết ở BA 7.3 cũ không được dùng thay dư nợ/tiền thực thu Finance theo bảng 7.1 và yêu cầu hiện tại.

- Kỳ là ngày lịch Asia/Ho_Chi_Minh: [from_date 00:00, to_date+1 00:00), bind timestamptz có offset, không dựa timezone session DB. Mặc định 30 ngày kể cả ngày cuối, tối đa 366 ngày. Cấm ngày tương lai, kỳ đảo và giá trị bộ lọc sai.
- `metadata.as_of` do server cấp, tất cả truy vấn trong mỗi báo cáo dùng cùng snapshot REPEATABLE_READ. API từ chối tham số as_of vì chưa có event history đầy đủ để dựng trạng thái quá khứ. Các request báo cáo khác nhau có as_of riêng và đều hiển thị metadata; không hứa snapshot chung giữa ba request HTTP.
- Cohort đơn dùng enrolled_at của **bản ghi đăng ký hiện hành**; APPROVED là trạng thái hiện tại, không phải tiền thực thu. Đơn tái đăng ký được code cũ tái sử dụng bản ghi/enrolled_at nên không thể tái lập cohort gốc hoặc số lần đăng ký lịch sử đã mất. Không thay enrollment lifecycle trong lượt BI.
- Mẫu số 0 -> null (UI N/A); tiền thiếu nguồn -> null + finance_status=UNAVAILABLE và lý do. Không thay bằng 0, tuition × số đơn hoặc tiền do nhân viên nhập.
- Các benchmark và cửa sổ 14 ngày được BA ghi **đề xuất**, không coi là chuẩn ngành/chính sách đã duyệt. API và UI công khai trạng thái đề xuất; cohort còn mới chưa đủ thời gian quan sát có thể đổi kết quả tại lần chốt sau.
- Course/channel lọc enrollment và chuyển đổi sang enrollment. Test attempt chưa có course_id: cohort test vẫn toàn trung tâm, metadata/label ghi rõ GLOBAL_TESTS_NO_COURSE_LINK. Không lọc theo khóa học được đề xuất rồi giả là khóa học nguồn của lượt thi.

## Bảng đối chiếu KPI

| KPI | Ý nghĩa/công thức và dữ liệu nguồn | Điều kiện/trùng/trống | Kiểm chứng | Đánh giá |
|---|---|---|---|---|
| BI-FUN-01 | SUBMITTED attempts / STARTED attempts trong cùng cohort test_attempts.started_at | submitted_at >= started_at và <= as_of; đếm attempt, không đếm lặp khi join submissions; mẫu 0 N/A | 3 lượt cùng cohort, 2 SUBMITTED của cùng user, kết quả 66.67%; nộp sau ngày kết thúc cohort vẫn tính outcome | DONE cho cohort hiện có |
| BI-FUN-02 | distinct user hoàn thành test có >=1 enrollment từ submitted_at đến submitted_at+14 ngày / distinct user hoàn thành test trong cohort | EXISTS tránh nhân đôi nhiều lượt/đơn; course/channel lọc đơn; đúng 14 ngày tính, +1 microsecond không tính | 2 lượt hoàn thành của 1 user vẫn mẫu số 1; 100%/0% ở ranh giới | PARTIAL: công thức triển khai, cửa sổ 14 ngày còn là đề xuất BA |
| BI-FUN-03 | APPROVED / tổng enrollment trong cohort enrolled_at | Tổng gồm mọi trạng thái, không lấy PENDING hiện tồn làm mẫu số; approved_at null legacy đếm theo trạng thái hiện tại, không suy ngày thanh toán | 2/8=25%, gồm cả CANCELLED và CANCELLED_REFUND_PENDING trong mẫu số | DONE cho snapshot hiện tại; không chứng nhận lịch sử |
| BI-FUN-04 | EXPIRED hiệu lực / tổng enrollment cohort | EXPIRED hoặc PENDING có deadline <= as_of; không phụ thuộc scheduler. CANCELLED/refund/rejected không thành expired. Deadline null không suy là quá hạn | 1 EXPIRED + 1 PENDING quá hạn / 8 =25%; 1 PENDING còn hạn giữ riêng | DONE cho snapshot hiện tại |
| BI-REV-01 | Tổng dư nợ hóa đơn Finance của PENDING hợp lệ | Admission không sở hữu dư nợ/giảm giá/thanh toán một phần; pending count chỉ là số đơn, không phải giá trị tiền | Assert null toàn báo cáo và từng course dù tuition=9 triệu, có APPROVED/PENDING | BLOCKED bởi hợp đồng dữ liệu Finance |
| BI-REV-02 | Finance tiền thu thật - hoàn thật trong kỳ | APPROVED, CASH request và callback marker không đủ số tiền/dư nợ/hoàn phí; không gọi endpoint tự đặt | null + UNAVAILABLE; UI “Chưa khả dụng” | BLOCKED bởi hợp đồng dữ liệu Finance |
| BI-OCC-01 | Tổng APPROVED slots / tổng max_students cùng lớp OPEN/IN_PROGRESS | Snapshot tất cả enrollment APPROVED ở cùng tập lớp; không hạn chế cohort ngày/kênh để tránh chia sai tập. Loại CANCELLED/COMPLETED; capacity 0 N/A | 2 slots / (10+30)=5%; lớp CANCELLED capacity100 không góp mẫu số; không lấy trung bình 10% và 3.33% | DONE |
| BI-USR-01 | distinct user_id LOGIN_SUCCESS theo ngày/tháng và vai trò | DAU ngày cuối kỳ; MAU tháng lịch chứa ngày cuối kỳ đến hết ngày đó. Trend theo các ô ngày/tháng trong kỳ; không cộng DAU thành MAU. Vai trò hiện tại từ users.role, không giả role lịch sử | Login lặp cùng user qua 31/1–1/2: DAU1, MAU1; 2 ngày/2 tháng; loại đúng 00:00 ngày sau | DONE cho login, không phải mọi hoạt động |
| BI-SEC-01 | LOGIN_FAILED / (LOGIN_SUCCESS+LOGIN_FAILED) trong kỳ | Loại refresh/password/blocked khỏi mẫu theo định nghĩa; role ALL giữ failure user_id null, chọn role chỉ gồm user xác định; mẫu 0 N/A | 1 fail/(3 success+1 fail)=25%; refresh không đếm | DONE |

## Các phần báo cáo bổ sung theo BA

| Chức năng | Nguồn/quy tắc | Đánh giá |
|---|---|---|
| Phễu 4 tầng user | distinct started users -> submitted users -> users có đơn trong 14 ngày -> users có đơn hiện APPROVED; thứ tự thời gian và cùng cohort test | DONE; đơn vị khác KPI attempt/đơn được ghi rõ |
| Nhánh trực tiếp/không test | Số đơn không có test_attempt bắt đầu trước enrolled_at; tổng theo enrollment_channel ONLINE/AT_COUNTER, cùng cohort đơn | DONE; không chia người không test cho số bài thi |
| Top Courses by Volume | COUNT đơn tạo trong kỳ theo course, gồm các trạng thái để giữ lượng đăng ký; trạng thái tách cột. ORDER BY enrolled_count DESC, code/id tie-break; limit1..100 | DONE; trả khóa học 0 đơn nếu catalog tồn tại, không có dòng minh họa |
| Occupancy theo level | Gộp tử/mẫu theo courses.level có thật (BEGINNER/INTERMEDIATE/ADVANCED) | DONE, không gọi đây là nhóm sản phẩm |
| Occupancy IELTS/TOEIC/Giao tiếp | Chưa có trường danh mục sản phẩm chuẩn trong schema; không đoán qua prefix tên/code | BLOCKED nguồn phân loại |
| Tín hiệu tài khoản đăng nhập sai | >5 LOGIN_FAILED liên tiếp trong 10 phút tại as_of, reset bởi LOGIN_SUCCESS; chỉ ADMIN, trả user_id và aggregate | DONE cho báo cáo; test 6 fail có signal, success reset. Không thay policy tự khóa Auth trong lượt BI |
| Query as_of quá khứ/original cohort bị tái dùng | Thiếu lịch sử trạng thái/tái đăng ký/role | PARTIAL: API từ chối as_of, không giả dựng dữ liệu |
| Phân quyền | STAFF/ADMIN funnel/course; chỉ ADMIN security, kể cả alias BA. Guest401, STUDENT/TEACHER không có báo cáo kinh doanh | DONE; HTTP/JWT PostgreSQL tests và Chrome STAFF/ADMIN/guest |

## API thực tế và thay đổi response

Giữ `/api/admission/bi/funnel`, `/courses-revenue`, `/security-engagement`. Thêm alias BA tại `/api/admission/analytics/funnel`, `/courses-ranking`, `/user-activity` (đường bi cũng nhận tên alias). Query: from_date/to_date, course_id, channel, limit; security dùng role/from_date/to_date hoặc days1..366. Không nhận as_of do client. Bộ lọc course/channel không áp dụng cho login; role không áp dụng cho đơn.

Response funnel giữ stages/kpis/summary, thêm counts và metadata. Các tầng là user còn test_completion_rate là attempt; paid_conversion_rate là tỷ lệ đơn APPROVED (tên key legacy), không chứng nhận tiền đã thu. Response course giữ các key tiền nhưng giá trị null, thêm finance_status/reason, enrolled_count và occupancy_by_level. Response security trả engagement {dau,mau,failed_login_rate,login_attempts,failed_logins_count}, daily_by_role, monthly_by_role, suspicious_ips, consecutive_failed_accounts; không còn wrapper security/các tên 24h sai ngữ nghĩa cũ. Frontend/backend BI phải cập nhật cùng nhau. Các DTO BI cũ vẫn được giữ trên disk để không xóa công việc; endpoint dùng response map mới đã kiểm thử.

Security API không trả token/password/nội dung audit/email; IP/user_id chỉ trong response ADMIN. Báo cáo không tự thay đổi dữ liệu, không gọi Finance/Student/Learning. Không có migration BI mới vì chỉ đọc bảng/cột Admission hiện có.

## Frontend

Dashboard dùng API thực, không demo chart/data. Bộ lọc ngày/tuần/tháng/quý, khóa học, kênh, top limit; ADMIN có role. Ngày preset theo Asia/Ho_Chi_Minh, không lấy ngày UTC lúc nửa đêm. Phễu, tỷ lệ, bảng top course, occupancy theo level, login trends và security signals được render từ response. Tiền luôn hiển thị “Chưa khả dụng”; tỷ lệ null là N/A. Có loading/disable, hiển thị lỗi và xóa số cũ, generation guard chặn response bộ lọc cũ đến muộn. Dữ liệu text được escape HTML. Không phụ thuộc Chart CDN.

## Bằng chứng QA

- Test trước sửa emptyCohortMustBeNA fail: expected null nhưng nhận 0.0 (red.log). Code cũ còn lấy submissions ngoài cohort và nhân giá khóa học thành doanh thu.
- 4 unit BI theo công thức cũ đã thay bằng 4 kiểm tra contract/validation/N/A/Finance unavailable; không giữ assertion sai rằng APPROVED × tuition là tiền thu. Các invariant và assertion nghiệp vụ ngoài BI không bị nới lỏng.
- Thêm 8 PostgreSQL tests với expected tính độc lập: cohort/duplicate-user/mọi trạng thái; occupancy/Finance-null; ngày/channel; lịch login và failure rate; 14 ngày; 6 fail/success-reset; rỗng; API/quyền/query sai/alias. Fixture BI mới được rollback trong transaction test riêng, không xóa/reset dữ liệu cũ. Lượt đầu fixture thiếu lịch lớp bắt buộc đã sửa đúng schema, không đổi expected KPI.
- **161/161 Java tests pass bằng JDK21, gồm 48 PostgreSQL16.15 integration tests, 0 failure/error/skip**. Giữ regression cascade/tiền mặt/retry Student/email/SMTP/audit/hold/webhook.
- **15/15 invariants, 10/10 Node frontend regression tests pass** (7 cũ +3 BI mới). **Chrome 7 trang và các ca BI STAFF/ADMIN/guest pass**, xác nhận KPI/Finance-N/A/filter/login-role từ API fixtures. Browser fixture ban đầu chờ networkidle cũ nên assert role request chạy quá sớm; đã chờ đúng request rồi pass, không nới assert.
- Git diff --check pass. Không chạy E2E toàn hệ thống, không gọi Finance thật, không tổng nghiệm thu Admission V1.

Log và snapshot đầu lượt: `C:/Users/Admin/admission-work/qa-admission-bi-20261110`: red.log, java-first.log (fixture lỗi), java-second.log (160 pass), verified-final.log (**161 pass**), invariants.log, frontend.log (**10 pass**), browser-verified.log. JDK21 tại qa-v1; PostgreSQL QA database admission_qa_v1/cổng55432. Không đưa password QA vào báo cáo.

## Điều kiện còn thiếu

Finance cần contract dữ liệu dư nợ/thu/hoàn phí và quy tắc phân bổ theo enrollment/course/channel/time; không sửa Finance hoặc tạo endpoint giả trong lượt này. Nhóm dự án cần chốt đề xuất cửa sổ14 ngày, mapping nhóm sản phẩm và nguồn lịch sử trạng thái/cycle/role nếu cần truy vấn as_of quá khứ. Cho đến khi có nguồn này, BI tổng thể PARTIAL; các chỉ số chưa tính được được công khai là không khả dụng.

Cảnh báo login dùng thứ tự (created_at,id) để xử lý đúng success/failure cùng timestamp; PostgreSQL test đã kiểm tra 6 failure sau success có cùng thời điểm.
