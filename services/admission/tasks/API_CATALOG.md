# Danh Mục Chi Tiết Các API Endpoints Thống Nhất (Admission & Identity Service)

> **Căn cứ tài liệu kỹ thuật chuẩn sau thống nhất**:
> - [`docs/MASTER_DB_AND_API_RULES.md`](../../../docs/MASTER_DB_AND_API_RULES.md) (Phiên bản Approved 2.1)
> - [`scripts/init-db.sql`](../../../scripts/init-db.sql) (PostgreSQL 16 - 14 bảng schema `admission`)
> - [`services/admission/docs/admission-schema-spec.md`](../docs/admission-schema-spec.md)
> - [`services/admission/docs/admission-ba-bi-spec.md`](../docs/admission-ba-bi-spec.md)

---

## 1. Quy Ước Kỹ Thuật Bắt Buộc Toàn Bộ API
1. **Base URL**:
   - Qua Nginx API Gateway: `http://localhost:8080/api/admission/` (hoặc `/auth/` đối với login/refresh tương thích)
   - Trực tiếp Service nội bộ: `http://admission-service:8081/api/admission/`
2. **Quy ước định dạng**:
   - 100% Request body, Response JSON và Query params dùng **`snake_case`**.
   - Header bắt buộc: `Content-Type: application/json`, `Accept: application/json`.
   - Header xác thực: `Authorization: Bearer <access_token>` cho API cần đăng nhập.
   - Header liên service nội bộ: `X-Internal-Secret: <INTERNAL_SECRET_KEY>` cho các API `/internal/*` và `/webhooks/*`.
3. **Cấu trúc Response Chuẩn**:
   - Thành công: `{"success": true, "data": { ... }, "message": "Thông báo"}`
   - Danh sách phân trang: `{"success": true, "data": {"items": [ ... ], "total": 100, "page": 0, "size": 20, "total_pages": 5}}`
   - Thất bại: `{"success": false, "error": {"code": "MÃ_LỖI", "message": "Mô tả lỗi"}}`
4. **Quy tắc Mã Lỗi**:
   - Nhóm Auth: Tiền tố `AUTH_` (ví dụ `AUTH_INVALID_CREDENTIALS`, `AUTH_TOKEN_EXPIRED`, `AUTH_TOKEN_INVALID`, `AUTH_ACCOUNT_LOCKED`)
   - Nhóm Admission: Tiền tố `ADM_` (ví dụ `ADM_CLASS_FULL`, `ADM_COURSE_NOT_FOUND`, `ADM_ROOM_CONFLICT`, `ADM_TEACHER_CONFLICT`, `ADM_ROOM_CAPACITY_EXCEEDED`)

---

## 2. Phân Hệ Xác Thực, Quản Trị Định Danh & Phiên (IAM)

| STT | HTTP Method | Endpoint URI | Phân Quyền (RBAC) | Tóm tắt Chức năng Nghiệp vụ | Request Body / Query Params | Mã Lỗi Chính |
|:---:|:---:|---|---|---|---|---|
| 1 | `POST` | `/api/admission/auth/login` | Public | Đăng nhập hệ thống, cấp cặp Access Token (15m) / Refresh Token (7d) | `{"email": "...", "password": "..."}` | `AUTH_INVALID_CREDENTIALS`, `AUTH_ACCOUNT_LOCKED` |
| 2 | `POST` | `/api/admission/auth/refresh` | Public | Làm mới phiên (Refresh Token Rotation; thu hồi token cũ, cấp cặp mới) | `{"refresh_token": "..."}` | `AUTH_TOKEN_INVALID`, `AUTH_TOKEN_EXPIRED` |
| 3 | `POST` | `/api/admission/auth/logout` | Authenticated | Đăng xuất, đánh dấu `is_revoked = TRUE` trên `refresh_tokens` | `{"refresh_token": "..."}` | `AUTH_TOKEN_INVALID` |
| 4 | `POST` | `/api/admission/auth/forgot-password` | Public | Yêu cầu mã token đặt lại mật khẩu qua email (hạn 15 phút) | `{"email": "..."}` | N/A (Bảo mật enum) |
| 5 | `POST` | `/api/admission/auth/reset-password` | Public | Đặt lại mật khẩu mới bằng token 15 phút hợp lệ | `{"token": "...", "new_password": "..."}` | `AUTH_TOKEN_EXPIRED`, `AUTH_TOKEN_INVALID` |
| 6 | `GET` | `/api/admission/auth/me` | Authenticated | Lấy thông tin tài khoản đang đăng nhập từ token JWT | Header `Authorization: Bearer <token>` | `AUTH_UNAUTHORIZED` |
| 7 | `POST` | `/api/admission/auth/change-password` | Authenticated | Đổi mật khẩu chủ động của người dùng | `{"old_password": "...", "new_password": "..."}` | `AUTH_INVALID_CREDENTIALS` |
| 8 | `GET` | `/api/admission/users` | ADMIN | Tìm kiếm & phân trang danh sách tài khoản người dùng | `?role=TEACHER&is_active=true&page=0&size=20` | `ADM_FORBIDDEN` |
| 9 | `POST` | `/api/admission/users` | ADMIN | Tạo tài khoản nhân sự mới (Staff / Teacher), băm BCrypt | `{"email": "...", "full_name": "...", "role": "...", "phone": "..."}` | `ADM_USER_EMAIL_EXISTS` |
| 10 | `GET` | `/api/admission/users/{id}` | ADMIN | Xem chi tiết hồ sơ tài khoản | Path param `id` | `ADM_USER_NOT_FOUND` |
| 11 | `PUT` | `/api/admission/users/{id}` | ADMIN | Cập nhật thông tin hồ sơ nhân sự | `{"full_name": "...", "phone": "...", "gender": "..."}` | `ADM_USER_NOT_FOUND` |
| 12 | `PATCH`| `/api/admission/users/{id}/status` | ADMIN | Khóa hoặc mở khóa tài khoản (`is_active = FALSE/TRUE`) | `{"is_active": false, "reason": "..."}` | `ADM_USER_NOT_FOUND` |
| 13 | `GET` | `/api/admission/audit-logs` | ADMIN | Truy vấn nhật ký kiểm toán bảo mật có phân trang | `?action=LOGIN_FAILED&from_date=...` | `ADM_FORBIDDEN` |

---

## 3. Phân Hệ Cơ Sở Vật Chất & Phòng Học (FAC)

| STT | HTTP Method | Endpoint URI | Phân Quyền (RBAC) | Tóm tắt Chức năng Nghiệp vụ | Request Body / Query Params | Mã Lỗi Chính |
|:---:|:---:|---|---|---|---|---|
| 14 | `GET` | `/api/admission/rooms` | STAFF, ADMIN | Lấy danh mục phòng học theo chi nhánh | `?branch_name=...&is_active=true` | N/A |
| 15 | `POST` | `/api/admission/rooms` | ADMIN | Tạo mới phòng học kèm sức chứa trần (`capacity > 0`) | `{"code": "ROOM-101", "name": "...", "capacity": 25, "branch_name": "..."}` | `ADM_ROOM_CODE_EXISTS` |
| 16 | `GET` | `/api/admission/rooms/{id}` | STAFF, ADMIN | Xem chi tiết thông tin phòng học | Path param `id` | `ADM_ROOM_NOT_FOUND` |
| 17 | `PUT` | `/api/admission/rooms/{id}` | ADMIN | Cập nhật thông tin và sức chứa phòng | `{"name": "...", "capacity": 30}` | `ADM_ROOM_NOT_FOUND` |
| 18 | `PATCH`| `/api/admission/rooms/{id}/status` | ADMIN | Tạm ngưng hoặc kích hoạt phòng học | `{"is_active": false}` | `ADM_ROOM_NOT_FOUND` |
| 19 | `GET` | `/api/admission/rooms/{id}/schedule`| STAFF, ADMIN | Lấy thời khóa biểu các ca học trong tuần của phòng | `?from_date=...&to_date=...` | `ADM_ROOM_NOT_FOUND` |

---

## 4. Phân Hệ Khóa Học & Lộ Trình Đào Tạo (ACA - Course)

| STT | HTTP Method | Endpoint URI | Phân Quyền (RBAC) | Tóm tắt Chức năng Nghiệp vụ | Request Body / Query Params | Mã Lỗi Chính |
|:---:|:---:|---|---|---|---|---|
| 20 | `GET` | `/api/admission/courses` | Public | Lấy danh mục khóa học đang mở tuyển sinh | `?level=BEGINNER&is_active=true` | N/A |
| 21 | `POST` | `/api/admission/courses` | STAFF, ADMIN | Tạo mới khóa học (`code` UNIQUE - INV-ADM-02) | `{"code": "IELTS-6.5", "name": "...", "tuition_amount": 7500000, ...}` | `ADM_COURSE_CODE_EXISTS` |
| 22 | `GET` | `/api/admission/courses/{id}` | Public | Xem thông tin chi tiết và lộ trình khóa học | Path param `id` | `ADM_COURSE_NOT_FOUND` |
| 23 | `PUT` | `/api/admission/courses/{id}` | STAFF, ADMIN | Cập nhật tên, học phí niêm yết, thời lượng | `{"name": "...", "tuition_amount": 8000000, ...}` | `ADM_COURSE_NOT_FOUND` |
| 24 | `DELETE`| `/api/admission/courses/{id}`| ADMIN | Vô hiệu hóa hoặc ngừng tuyển sinh khóa học | Path param `id` | `ADM_COURSE_HAS_CLASSES` |

---

## 5. Phân Hệ Lớp Học & Điều Phối Sĩ Số (ACA - Class)

| STT | HTTP Method | Endpoint URI | Phân Quyền (RBAC) | Tóm tắt Chức năng Nghiệp vụ | Request Body / Query Params | Mã Lỗi Chính |
|:---:|:---:|---|---|---|---|---|
| 25 | `GET` | `/api/admission/classes` | Public | Danh sách lớp học kèm sĩ số và slot khả dụng | `?course_id=1&status=OPEN` | N/A |
| 26 | `POST` | `/api/admission/classes` | STAFF, ADMIN | Mở lớp học mới (Kiểm tra xung đột phòng & GV, `max_students <= room.capacity`) | `{"course_id": 1, "room_id": 2, "teacher_id": 3, "max_students": 20, ...}` | `ADM_ROOM_CAPACITY_EXCEEDED`, `ADM_ROOM_CONFLICT`, `ADM_TEACHER_CONFLICT` |
| 27 | `GET` | `/api/admission/classes/{id}` | Public | Xem chi tiết thông tin lớp học, phòng và giảng viên | Path param `id` | `ADM_CLASS_NOT_FOUND` |
| 28 | `PUT` | `/api/admission/classes/{id}` | STAFF, ADMIN | Cập nhật thông tin lớp học (chặn sửa nếu xung đột) | `{"name": "...", "room_id": 3, ...}` | `ADM_CLASS_NOT_FOUND`, `ADM_ROOM_CONFLICT` |
| 29 | `PATCH`| `/api/admission/classes/{id}/status`| STAFF, ADMIN | Chuyển trạng thái lớp (OPEN, IN_PROGRESS, COMPLETED, CANCELLED) | `{"status": "IN_PROGRESS"}` | `ADM_INVALID_STATE_TRANSITION` |
| 30 | `GET` | `/api/admission/classes/{id}/capacity`| Public | Lấy số chỗ khả dụng thời gian thực ($Available = max - approved - held$) | Path param `id` | `ADM_CLASS_NOT_FOUND` |
| 31 | `POST` | `/api/admission/classes/check-conflict`| STAFF, ADMIN | API kiểm tra xung đột lịch trước khi tạo | `{"room_id": 2, "teacher_id": 3, "day_pattern": "MON_WED_FRI", "start_time": "...", ...}` | Trả về `{"has_conflict": true/false, ...}` |

---

## 6. Phân Hệ Khảo Thí Đầu Vào & Lượt Thi (TST)

| STT | HTTP Method | Endpoint URI | Phân Quyền (RBAC) | Tóm tắt Chức năng Nghiệp vụ | Request Body / Query Params | Mã Lỗi Chính |
|:---:|:---:|---|---|---|---|---|
| 32 | `GET` | `/api/admission/tests/sets` | STAFF, ADMIN | Lấy danh sách các bộ đề thi khảo thí | `?is_active=true` | N/A |
| 33 | `POST` | `/api/admission/tests/sets` | STAFF, ADMIN | Tạo mới bộ đề thi khảo thí đầu vào | `{"code": "TEST-2026-A1", "title": "...", "duration_minutes": 45}` | `ADM_DUPLICATE_CODE` |
| 34 | `GET` | `/api/admission/tests/sets/{id}` | STAFF, ADMIN | Xem chi tiết bộ đề và danh sách câu hỏi gốc | Path param `id` | `ADM_TEST_NOT_FOUND` |
| 35 | `PUT` | `/api/admission/tests/sets/{id}` | STAFF, ADMIN | Cập nhật tiêu đề, thời lượng bộ đề | `{"title": "...", "duration_minutes": 60}` | `ADM_TEST_NOT_FOUND` |
| 36 | `POST` | `/api/admission/tests/sets/{id}/questions` | STAFF, ADMIN | Thêm câu hỏi 3 kỹ năng vào bộ đề | `{"skill_type": "LISTENING", "audio_url": "...", "correct_option": "B", ...}` | `ADM_INVALID_PAYLOAD` |
| 37 | `PUT` | `/api/admission/tests/questions/{id}`| STAFF, ADMIN | Chỉnh sửa nội dung câu hỏi, đáp án | `{"question_text": "...", "correct_option": "C"}` | `ADM_NOT_FOUND` |
| 38 | `DELETE`| `/api/admission/tests/questions/{id}`| STAFF, ADMIN | Xóa câu hỏi khỏi bộ đề | Path param `id` | `ADM_NOT_FOUND` |
| 39 | `POST`| `/api/admission/tests/public/{id}/start` | Public, STUDENT | Bắt đầu lượt thi (tạo `test_attempts`, trả đề thi ĐÃ GIẤU ĐÁP ÁN ĐÚNG và `attempt_id`) | Path param `id`, body: `{"user_id": 42}` (hoặc email) | `ADM_TEST_NOT_FOUND` |
| 40 | `POST` | `/api/admission/tests/public/submit` | Public, STUDENT | Nộp bài thi, đối chiếu `attempt_id`, server-side chấm điểm & trả khóa học khuyến nghị | `{"attempt_id": 105, "answers": [{"question_id": 1, "selected": "A"}]}` | `ADM_TEST_ALREADY_SUBMITTED`, `ADM_ATTEMPT_EXPIRED` |
| 41 | `GET` | `/api/admission/tests/submissions/{id}` | Authenticated, STAFF | Xem kết quả bài thi chi tiết và điểm bóc tách 3 kỹ năng | Path param `id` | `ADM_SUBMISSION_NOT_FOUND` |
| 42 | `GET` | `/api/admission/tests/rules` | STAFF, ADMIN | Lấy danh sách quy tắc đề xuất khóa học | `?test_set_id=1` | N/A |
| 43 | `POST` | `/api/admission/tests/rules` | STAFF, ADMIN | Cấu hình quy tắc đề xuất theo dải điểm | `{"test_set_id": 1, "min_score": 60.0, "max_score": 79.9, "recommended_course_id": 2}` | `ADM_RULE_OVERLAP` |

---

## 7. Phân Hệ Tuyển Sinh, Giữ Chỗ 48h & Webhook Xét Duyệt (ENR)

| STT | HTTP Method | Endpoint URI | Phân Quyền (RBAC) | Tóm tắt Chức năng Nghiệp vụ | Request Body / Query Params | Mã Lỗi Chính |
|:---:|:---:|---|---|---|---|---|
| 44 | `POST` | `/api/admission/enrollments` | Public, STUDENT, STAFF | Nộp đơn tuyển sinh (Cấp slot giữ chỗ 48h `hold_expires_at`, gọi Finance sinh hóa đơn) | `{"class_id": 10, "student_id": 42, "source": "ONLINE", "enrollment_channel": "ONLINE"}` | `ADM_CLASS_FULL`, `ADM_CLASS_NOT_OPEN` |
| 45 | `GET` | `/api/admission/enrollments` | STAFF, ADMIN | Danh sách đơn đăng ký tuyển sinh có phân trang | `?class_id=10&status=PENDING&page=0&size=20` | `ADM_FORBIDDEN` |
| 46 | `GET` | `/api/admission/enrollments/{id}` | Authenticated, STAFF | Xem chi tiết thông tin đơn, hạn giữ chỗ 48h và `invoice_code` | Path param `id` | `ADM_ENROLLMENT_NOT_FOUND` |
| 47 | `POST` | `/api/admission/enrollments/{id}/extend-hold` | STAFF, ADMIN | Học vụ gia hạn thời gian giữ chỗ kèm lý do | `{"extended_hours": 24, "reason": "Chờ lương đầu tháng"}` | `ADM_INVALID_STATE_TRANSITION`, `ADM_CLASS_FULL` |
| 48 | `POST` | `/api/admission/enrollments/{id}/approve-manual`| STAFF, ADMIN | Duyệt đơn trực tiếp khi học viên nộp tiền mặt (kiểm tra sĩ số trần INV-ADM-01) | `{"receipt_number": "PT-01", "payment_method": "CASH", "amount_received": 4500000}` | `ADM_CLASS_FULL`, `ADM_INVALID_STATE_TRANSITION` |
| 49 | `PATCH`| `/api/admission/enrollments/{id}/cancel` | STAFF, Owner | Hủy đơn đăng ký tuyển sinh, giải phóng slot giữ chỗ | `{"reason": "Học viên hủy lịch học"}` | `ADM_INVALID_STATE_TRANSITION` |
| 50 | `POST` | `/api/admission/webhooks/payment-success` | Internal (Finance Service) | Webhook nhận thanh toán PAID từ Finance $\rightarrow$ Idempotent check qua `processed_finance_events` $\rightarrow$ Duyệt đơn & sync Student | `{"event_id": "EVT-...", "enrollment_id": 100, "invoice_id": 55, "amount_paid": 6500000, "paid_at": "..."}` | `ADM_CLASS_FULL` (HTTP 422), `ADM_ENROLLMENT_NOT_FOUND` |

---

## 8. Phân Hệ Xuất Dữ Liệu & Báo Cáo Vận Hành (OPS)

| STT | HTTP Method | Endpoint URI | Phân Quyền (RBAC) | Tóm tắt Chức năng Nghiệp vụ | Request Body / Query Params | Định Dạng File Trả Về |
|:---:|:---:|---|---|---|---|---|
| 51 | `GET` | `/api/admission/exports/classes/{id}/roster` | STAFF, TEACHER, ADMIN | Xuất danh sách học viên lớp học ra file | `?format=csv` (hoặc `xlsx`) | `text/csv; charset=UTF-8` |
| 52 | `GET` | `/api/admission/exports/tests/{id}/results` | STAFF, ADMIN | Xuất bảng điểm khảo thí đầu vào | `?format=csv` (hoặc `xlsx`) | `text/csv; charset=UTF-8` |

---

## 9. Phân Hệ Báo Cáo Thông Minh BI Tuyển Sinh (BI & KPIs)

Cập nhật 10/11/2026: contract BI và matrix KPI thực tế tại [bi-kpi-qa.md](../docs/bi-kpi-qa.md). Snapshot hiện tại; alias BA `/api/admission/analytics/*`; query as_of bị từ chối do chưa có history.

| STT | HTTP Method | Endpoint URI | Phân Quyền (RBAC) | Tóm tắt Chức năng Nghiệp vụ | Query Parameters | Response Key Metrics |
|:---:|:---:|---|---|---|---|---|
| 53 | `GET` | `/api/admission/bi/funnel` | STAFF, ADMIN | Dữ liệu phễu chuyển đổi 4 giai đoạn & 4 KPIs | `?from_date=2026-10-01&to_date=2026-10-31` | `stages[]`, `kpis: {test_completion_rate, test_to_enrollment_rate, paid_conversion_rate, expired_hold_rate}` |
| 54 | `GET` | `/api/admission/bi/courses-revenue` | STAFF, ADMIN | Xếp hạng số đơn & lấp đầy; dư nợ/thực thu Finance UNAVAILABLE | `?from_date=2026-10-01&to_date=2026-10-31` | `pipeline_potential_revenue=null`, `realized_admission_revenue=null`, `finance_status`, `average_occupancy_rate`, `top_courses[]` |
| 55 | `GET` | `/api/admission/bi/security-engagement` | ADMIN | Thống kê DAU/MAU và cảnh báo Brute-force | `?days=30` | `engagement: {dau,mau,failed_login_rate,login_attempts,failed_logins_count}`, `daily_by_role[]`, `monthly_by_role[]`, `suspicious_ips[]`, `consecutive_failed_accounts[]` |

---

## 10. Phân Hệ Giao Tiếp Liên Service Nội Bộ (Internal Service-to-Service API)

> **Bảo mật**: Các API này **KHÔNG ĐƯỢC PHÉP EXPOSE QUA NGINX GATEWAY**, chỉ dành cho các service backend giao tiếp nội bộ trong mạng Docker, bắt buộc đính kèm Header:  
> `X-Internal-Secret: <INTERNAL_SECRET_KEY>`

### 10.1. API mà Admission CUNG CẤP cho các Service khác gọi vào
| STT | HTTP Method | Endpoint URI | Caller Service | Tóm tắt Mục đích | Response Format |
|:---:|:---:|---|---|---|---|
| 56 | `GET` | `/api/admission/internal/classes/{id}` | **Learning**, **Student** | Xác minh lớp học tồn tại, lấy `teacher_id` và trạng thái lớp | `{"success": true, "data": {"id": 5, "code": "CLS-01", "teacher_id": 3, "status": "IN_PROGRESS"}}` |
| 57 | `GET` | `/api/admission/internal/enrollments` | **Student** | Lấy danh sách học viên `APPROVED` của lớp để giáo viên điểm danh | `?class_id=5&status=APPROVED` $\rightarrow$ `{"success": true, "data": [{"student_id": 42, "enrollment_id": 100}]}` |
| 58 | `GET` | `/api/admission/internal/courses/{id}` | **Finance** | Lấy học phí niêm yết `tuition_amount` để Finance sinh hóa đơn | `{"success": true, "data": {"id": 2, "code": "ENG-01", "tuition_amount": 6500000}}` |

### 10.2. API mà Admission GỌI SANG các Service khác (Callee)
| STT | HTTP Method | Endpoint URI | Callee Service | Trigger & Mục đích | Header & Payload |
|:---:|:---:|---|---|---|---|
| 59 | `POST` | `http://finance-svc:8084/api/finance/invoices/auto-generate` | **Finance Service** | Khi đơn đăng ký được tạo (PENDING) $\rightarrow$ Yêu cầu Finance sinh hóa đơn học phí | `X-Internal-Secret`<br>`{"enrollment_id": 100, "student_id": 42, "course_id": 2, "voucher_code": null}` |
| 60 | `POST` | `http://student-svc:8082/api/student/internal/students` | **Student Service** | Khi đơn chuyển sang `APPROVED` $\rightarrow$ Tự động tạo hồ sơ học viên chính thức | `X-Internal-Secret`<br>`{"user_id": 42, "full_name": "...", "email": "...", "phone": "..."}` |
