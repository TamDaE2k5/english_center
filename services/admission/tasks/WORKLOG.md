# Nhật Ký Hoạt Động & Phối Hợp Làm Việc (Admission Module Worklog)

> **Mục đích của file này**:  
> Đây là nơi **toàn bộ thành viên trong nhóm** (Developer, Tech Lead, AI Agent, Tester) bắt buộc ghi nhận lịch sử công việc để tránh xung đột mã nguồn (merge conflict), nắm bắt ai đang làm gì, sửa file nào, thêm mới/xóa bỏ thành phần nào và kết quả kiểm thử tương ứng.

---

## 1. Bảng Trạng Thái Công Việc Đang Diễn Ra (Active Work In-Progress)

*Bảng này dùng để đăng ký task trước khi code, giúp các thành viên không làm trùng hoặc đè code lên nhau.*

| Task ID | Thành viên phụ trách | Nhánh Git (Branch) | Thời điểm bắt đầu | Trạng thái | Ghi chú & Phạm vi can thiệp |
|---|---|---|---|---|---|
| *Ví dụ: TASK-ADM-00* | *Nguyễn Văn A* | *feature/adm-foundation* | *2026-11-02 08:30* | 🟡 `IN_PROGRESS` | *Đang tạo ApiResponse.java & GlobalExceptionHandler.java* |
| *(Trống)* | | | | | *(Chưa có ai nhận task tiếp theo)* |

> **Quy tắc phối hợp nhóm**:
> 1. **Trước khi bắt đầu code**: Thêm một dòng vào bảng trên với trạng thái `IN_PROGRESS`.
> 2. **Trong khi code**: Chỉ sửa đổi các file trong phạm vi cho phép được quy định tại file mô tả task tương ứng.
> 3. **Sau khi code xong**: Chạy `python3 -m unittest discover -s tests/invariants -p "test_*.py" -v`, sau đó chuyển task sang bảng Lịch sử Hoàn thành bên dưới và tick vào [`CHECKLIST.md`](CHECKLIST.md).

---

## 2. Quy Chuẩn Ghi Chép Nhật Ký (Log Entry Template)

Mỗi khi hoàn thành hoặc bàn giao một phần việc, thành viên bổ sung một bản ghi vào **Mục 3 (Lịch sử Chi tiết)** theo mẫu chuẩn sau:

```markdown
### [YYYY-MM-DD HH:mm] - [TÊN_THÀNH_VIÊN] - [MÃ_TASK]
- **Nhánh Git (Branch)**: `feature/ten-nhanh`
- **Mục tiêu công việc**: Tóm tắt 1 câu việc vừa làm.
- **Chi tiết thay đổi**:
  - 🟢 **Thêm mới (Added)**: Danh sách file tạo mới.
  - 🟡 **Cập nhật / Nâng cấp (Updated)**: Danh sách file đã sửa & lý do sửa.
  - 🔴 **Xóa bỏ (Deleted)**: Danh sách file/hàm đã xóa (nếu có).
- **Trạng thái Quality Gate**:
  - Invariant Tests: `15/15 Tests OK`
  - Compile / Build: `Build Success` (hoặc N/A nếu là docs/frontend)
- **Ghi chú bàn giao**: Lưu ý cho người tiếp theo hoặc các phân hệ khác (Student, Finance).
```

---

## 3. Lịch Sử Chi Tiết Các Phiên Làm Việc (Historical Log Entries)

### [2026-11-08 16:40] - AI Assistant (Pair with Team Lead) - TASK-ADM-13
- **Nhánh Git (Branch)**: `feature/admission`
- **Mục tiêu công việc**: Triển khai cơ chế Refresh Token Rotation (RTR) chống tấn công Replay Attack (`POST /api/admission/auth/refresh`), thu hồi lập tức token cũ khi cấp token mới, phòng vệ an ninh tự động hủy toàn bộ phiên của người dùng nếu phát hiện tái sử dụng token đã thu hồi, và endpoint đăng xuất thu hồi token (`POST /api/admission/auth/logout`).
- **Chi tiết thay đổi**:
  - 🟢 **Thêm mới (Added)**:
    - `dto/auth/RefreshTokenRequest.java`: DTO nhận chuỗi refresh token.
    - `dto/auth/RefreshTokenResponse.java`: DTO trả về cặp token mới (`access_token`, `refresh_token`, `expires_in`).
    - `dto/auth/LogoutRequest.java`: DTO yêu cầu thu hồi token đăng xuất.
  - 🟡 **Cập nhật / Nâng cấp (Updated)**:
    - `service/AuthService.java` & `AuthServiceImpl.java`: Triển khai `refreshToken` với phát hiện Replay Attack, kiểm tra hết hạn `expires_at`, thu hồi khẩn cấp toàn bộ phiên và hàm `logout`.
    - `controller/AuthController.java`: Khai báo endpoints `/api/admission/auth/refresh` và `/api/admission/auth/logout`.
    - `src/test/java/com/englishcenter/admission/service/AuthServiceTest.java`: Bổ sung 3 test case kiểm thử Rotation thành công, phát hiện Replay Attack phòng vệ hủy phiên, và đăng xuất thu hồi token (6 tests passed).
- **Trạng thái Quality Gate**:
  - Invariant Tests: `15/15 OK (Ran in 0.003s)`
  - Unit Tests: `19/19 Tests Run, 0 Failures, 0 Errors (Surefire)`
  - Compile / Build: `BUILD SUCCESS (57 source files compiled in 3.76s)`
- **Ghi chú bàn giao**: Sẵn sàng chuyển tiếp sang `TASK-ADM-14` (Quy trình Quên mật khẩu & Đặt lại mật khẩu qua Token 15 phút).

### [2026-11-08 16:35] - AI Assistant (Pair with Team Lead) - TASK-ADM-12
- **Nhánh Git (Branch)**: `feature/admission`
- **Mục tiêu công việc**: Triển khai API Đăng nhập chuẩn REST (`POST /api/admission/auth/login`), cấp phát bộ token kép (Access Token 15m, Refresh Token 7d lưu vào `admission.refresh_tokens`), ghi nhận nhật ký kiểm toán `LOGIN_SUCCESS`, `LOGIN_FAILED`, `LOGIN_BLOCKED` vào `admission.auth_audit_logs`.
- **Chi tiết thay đổi**:
  - 🟢 **Thêm mới (Added)**:
    - `dto/auth/LoginRequest.java`: DTO nhận thông tin đăng nhập với validation `@NotBlank` và `@Email`.
    - `dto/auth/LoginResponse.java`: DTO trả về cặp token (`access_token`, `refresh_token`, `expires_in`, `token_type`, `user`).
    - `dto/auth/UserSummary.java`: DTO tóm tắt người dùng (`user_id`, `email`, `full_name`, `role`).
    - `service/AuthService.java`: Interface định nghĩa nghiệp vụ xác thực đăng nhập.
    - `service/impl/AuthServiceImpl.java`: Triển khai xác thực mật khẩu BCrypt, kiểm tra trạng thái khóa tài khoản, cấp phát JWT kép, lưu phiên và ghi log kiểm toán.
    - `src/test/java/com/englishcenter/admission/service/AuthServiceTest.java`: Bộ kiểm thử đơn vị JUnit 5 & Mockito (3 tests passed).
  - 🟡 **Cập nhật / Nâng cấp (Updated)**:
    - `controller/AuthController.java`: Refactor sử dụng AuthService và chuẩn hóa response ApiResponse, hỗ trợ bóc tách IP qua `X-Forwarded-For`.
  - 🔴 **Xóa bỏ (Deleted)**:
    - `util/JwtUtil.java`: Xóa bỏ lớp util mẫu cũ do đã được thay thế hoàn toàn bởi `JwtTokenProvider` và `SecurityConfig`.
- **Trạng thái Quality Gate**:
  - Invariant Tests: `15/15 OK (Ran in 0.003s)`
  - Unit Tests: `16/16 Tests Run, 0 Failures, 0 Errors (Surefire)`
  - Compile / Build: `BUILD SUCCESS (54 source files compiled in 3.63s)`
- **Ghi chú bàn giao**: Sẵn sàng chuyển tiếp sang `TASK-ADM-13` (Cơ chế Refresh Token Rotation & Thu hồi Token Đang hoạt động).

### [2026-11-08 16:30] - AI Assistant (Pair with Team Lead) - TASK-ADM-11
- **Nhánh Git (Branch)**: `feature/admission`
- **Mục tiêu công việc**: Xây dựng toàn bộ luồng quản lý nhân sự Staff/Teacher và cơ chế Khóa/Mở khóa tài khoản (FR-IAM-02, FR-IAM-05), thu hồi lập tức Refresh Token khi khóa tài khoản, ghi nhận nhật ký kiểm toán và bảo vệ ranh giới xem hồ sơ INV-SEC-01.
- **Chi tiết thay đổi**:
  - 🟢 **Thêm mới (Added)**:
    - `repository/RefreshTokenRepository.java`: Quản lý truy vấn và thu hồi token (`revokeAllByUserId`).
    - `repository/AuthAuditLogRepository.java`: Quản lý ghi và truy vấn nhật ký kiểm toán bảo mật.
    - `dto/user/CreateUserRequest.java`: DTO tạo nhân viên mới (validation email, role STAFF|TEACHER, password >= 8 ký tự).
    - `dto/user/UpdateUserRequest.java`: DTO cập nhật hồ sơ người dùng.
    - `dto/user/ToggleActiveRequest.java`: DTO khóa/mở khóa tài khoản kèm lý do.
    - `dto/user/UserResponse.java`: DTO trả về thông tin người dùng an toàn.
    - `controller/UserController.java`: Endpoints REST API `/api/admission/users` với `@PreAuthorize("hasRole('ADMIN')")` và phân quyền chi tiết.
  - 🟡 **Cập nhật / Nâng cấp (Updated)**:
    - `repository/UserRepository.java`: Bổ sung `@Query searchUsers` tìm kiếm theo role và từ khóa keyword.
    - `service/UserService.java` & `UserServiceImpl.java`: Triển khai các hàm `createUser`, `getUserById`, `listUsers`, `updateUser`, `toggleUserStatus`, kiểm tra `INV-SEC-01`.
    - `common/dto/PageResponse.java`: Bổ sung static factory method `PageResponse.of(...)`.
    - `src/test/java/com/englishcenter/admission/service/UserServiceTest.java`: Bổ sung các test case kiểm tra tạo nhân viên, kiểm tra trùng email, kiểm tra INV-SEC-01 cách ly học viên, kiểm tra khóa tài khoản thu hồi token (6 tests passed).
- **Trạng thái Quality Gate**:
  - Invariant Tests: `15/15 OK (Ran in 0.003s)`
  - Unit Tests: `13/13 Tests Run, 0 Failures, 0 Errors (Surefire)`
  - Compile / Build: `BUILD SUCCESS (50 source files compiled in 3.55s)`
- **Ghi chú bàn giao**: Sẵn sàng chuyển tiếp sang `TASK-ADM-12` (API Đăng nhập, cấp JWT kép & ghi nhận nhật ký kiểm toán bảo mật).

### [2026-11-08 16:25] - AI Assistant (Pair with Team Lead) - TASK-ADM-10
- **Nhánh Git (Branch)**: `feature/admission`
- **Mục tiêu công việc**: Triển khai cơ chế tự động tìm kiếm hoặc khởi tạo tài khoản học viên (role STUDENT) với mật khẩu ngẫu nhiên băm BCrypt, cờ must_change_password=true khi học viên nộp đơn online hoặc làm bài kiểm tra đầu vào (FR-IAM-01).
- **Chi tiết thay đổi**:
  - 🟢 **Thêm mới (Added)**:
    - `dto/user/CreateStudentAccountRequest.java`: DTO xác thực email, fullName, phone.
    - `service/UserService.java`: Interface định nghĩa `getOrCreateStudentUser(email, fullName, phone)`.
    - `service/impl/UserServiceImpl.java`: Triển khai logic chuẩn hóa email, tái sử dụng tài khoản cũ nếu có hoặc tạo mới tài khoản STUDENT kèm mật khẩu ngẫu nhiên băm BCrypt.
    - `src/test/java/com/englishcenter/admission/service/UserServiceTest.java`: Bộ kiểm thử đơn vị JUnit 5 & Mockito (2 tests passed).
  - 🟡 **Cập nhật / Nâng cấp (Updated)**:
    - `repository/UserRepository.java`: Bổ sung `existsByEmail(email)` và `findByRole(role, pageable)`.
- **Trạng thái Quality Gate**:
  - Invariant Tests: `15/15 OK (Ran in 0.003s)`
  - Unit Tests: `9/9 Tests Run, 0 Failures, 0 Errors (Surefire)`
  - Compile / Build: `BUILD SUCCESS (43 source files compiled in 3.33s)`
- **Ghi chú bàn giao**: Sẵn sàng chuyển tiếp sang `TASK-ADM-11` (Quản lý tài khoản nhân sự Staff/Teacher và cơ chế Khóa/Mở khóa tài khoản).

### [2026-11-08 16:20] - AI Assistant (Pair with Team Lead) - TASK-ADM-03
- **Nhánh Git (Branch)**: `feature/admission`
- **Mục tiêu công việc**: Cấu hình Spring Boot 3 RestClient với timeout kết nối (3s) & đọc dữ liệu (5s), triển khai FinanceServiceClient & StudentServiceClient giao tiếp trực tiếp REST API liên service (East-West traffic) sử dụng header X-Internal-Secret và viết kiểm thử MockRestServiceServer.
- **Chi tiết thay đổi**:
  - 🟢 **Thêm mới (Added)**:
    - `config/RestClientConfig.java`: Cấu hình Bean RestClient với SimpleClientHttpRequestFactory (connectTimeout 3s, readTimeout 5s).
    - `client/dto/CreateInvoiceRequest.java`: DTO gửi yêu cầu tạo hóa đơn học phí tự động sang Finance Service.
    - `client/dto/InvoiceData.java`: DTO phản hồi dữ liệu hóa đơn từ Finance Service (`invoice_id`, `invoice_code`, `amount`).
    - `client/dto/CreateStudentProfileRequest.java`: DTO gửi yêu cầu tạo hồ sơ học viên chính thức sang Student Service.
    - `client/dto/StudentProfileData.java`: DTO phản hồi dữ liệu hồ sơ học viên từ Student Service (`student_id`, `student_code`).
    - `client/FinanceServiceClient.java`: Client gọi `POST /api/finance/invoices/auto-generate` kèm `X-Internal-Secret`.
    - `client/StudentServiceClient.java`: Client gọi `POST /api/student/internal/students` kèm `X-Internal-Secret` và cơ chế fallback an toàn `trySyncStudentProfile`.
    - `src/test/java/com/englishcenter/admission/client/InterServiceClientTest.java`: Bộ kiểm thử đơn vị JUnit 5 với MockRestServiceServer (3 tests passed).
  - 🟡 **Cập nhật / Nâng cấp (Updated)**:
    - `common/exception/ErrorCode.java`: Thêm mã lỗi `ADM_SERVICE_UNAVAILABLE` (HTTP 503) khi dịch vụ liên kết lân cận gặp sự cố.
- **Trạng thái Quality Gate**:
  - Invariant Tests: `15/15 OK (Ran in 0.003s)`
  - Unit Tests: `7/7 Tests Run, 0 Failures, 0 Errors (Surefire)`
  - Compile / Build: `BUILD SUCCESS (40 source files compiled in 2.71s)`
- **Ghi chú bàn giao**: Đã hoàn thành 100% Phase 0 (Nền móng & Hạ tầng kỹ thuật). Sẵn sàng chuyển tiếp sang Phase 1: IAM (`TASK-ADM-10` đến `TASK-ADM-15`).

### [2026-11-08 16:15] - AI Assistant (Pair with Team Lead) - TASK-ADM-02
- **Nhánh Git (Branch)**: `feature/admission`
- **Mục tiêu công việc**: Thiết lập Spring Security 6, Bộ cấp phát Token kép (Access Token 15m / Refresh Token 7d), CustomUserDetailsService, UserPrincipal, JwtAuthenticationFilter, SecurityUtils và kiểm thử đơn vị.
- **Chi tiết thay đổi**:
  - 🟢 **Thêm mới (Added)**:
    - `security/UserPrincipal.java`: Triển khai UserDetails từ thực thể User, quản lý vai trò và quyền hạn tiền tố `ROLE_`.
    - `security/CustomUserDetailsService.java`: Nạp người dùng từ UserRepository theo email hoặc ID.
    - `security/JwtTokenProvider.java`: Ký và giải mã JWT HS256, sinh Access Token (15m) chứa `sub`, `email`, `role`, `full_name` và Refresh Token (7d) chứa `jti`.
    - `security/JwtAuthenticationFilter.java`: Filter chặn request, bóc tách Bearer token, xác thực và nạp Authentication vào SecurityContext.
    - `security/SecurityUtils.java`: Lớp tiện ích lấy thông tin người dùng đang đăng nhập (`getCurrentUserId`, `getCurrentUserRole`, `hasRole`, ...).
    - `config/SecurityConfig.java`: Cấu hình SecurityFilterChain Stateless, CSRF disable, CORS linh hoạt, phân quyền endpoint chuẩn REST và bắt lỗi 401/403 trả về ApiResponse JSON.
    - `src/test/java/com/englishcenter/admission/security/JwtTokenProviderTest.java`: Bộ kiểm thử đơn vị JUnit 5 cho JwtTokenProvider và UserPrincipal (4 tests passed).
  - 🟡 **Cập nhật / Nâng cấp (Updated)**:
    - `common/dto/ApiResponse.java`: Bổ sung các phương thức tiện ích `error(ErrorCode)`, `error(ErrorCode, String)`, `error(String, String)`.
- **Trạng thái Quality Gate**:
  - Invariant Tests: `15/15 OK (Ran in 0.003s)`
  - Unit Tests: `4/4 Tests Run, 0 Failures, 0 Errors (Surefire)`
  - Compile / Build: `BUILD SUCCESS (33 source files compiled in 1.51s)`
- **Ghi chú bàn giao**: Sẵn sàng chuyển tiếp sang `TASK-ADM-03` (Cấu hình RestClient tích hợp liên service Finance & Student với header X-Internal-Secret).

### [2026-11-08 16:10] - AI Assistant (Pair with Team Lead) - TASK-ADM-01
- **Nhánh Git (Branch)**: `feature/admission`
- **Mục tiêu công việc**: Hoàn tất khởi tạo BaseEntity và toàn bộ 14 JPA Entities cho Schema `admission` theo đúng DDL trong `scripts/init-db.sql` và `docs/MASTER_DB_AND_API_RULES.md`.
- **Chi tiết thay đổi**:
  - 🟢 **Thêm mới (Added)**:
    - `BaseEntity.java`: Lớp kiểm toán thời gian tự động gán `created_at` và `updated_at`.
    - `RefreshToken.java`: Quản lý phiên token kép và thu hồi.
    - `PasswordResetToken.java`: Quản lý token đổi mật khẩu 15 phút.
    - `AuthAuditLog.java`: Quản lý nhật ký kiểm toán an toàn thông tin.
    - `Room.java`: Quản lý phòng học, sức chứa trần và chi nhánh.
    - `ClassEntity.java`: Quản lý lớp học, ca học, phòng học, giáo viên và sĩ số tối đa.
    - `Enrollment.java`: Quản lý đơn tuyển sinh, hạn giữ chỗ 48h và cờ đồng bộ sang Student Service.
    - `TestSet.java`: Quản lý bộ đề khảo thí và thời lượng làm bài đếm ngược.
    - `TestQuestion.java`: Quản lý câu hỏi trắc nghiệm 3 kỹ năng (Listening, Reading, Grammar).
    - `TestRecommendationRule.java`: Quản lý quy tắc đề xuất khóa học theo dải điểm.
    - `TestAttempt.java`: Quản lý lượt thi đếm ngược server-side (INV-TST-02).
    - `TestSubmission.java`: Quản lý bài nộp khảo thí, điểm bóc tách và khóa học đề xuất.
    - `ProcessedFinanceEvent.java`: Quản lý đối soát thanh toán Finance callback idempotency (INV-ENR-02 / INV-ENR-03).
  - 🟡 **Cập nhật / Nâng cấp (Updated)**:
    - `User.java`: Kế thừa BaseEntity, bổ sung `gender`, `date_of_birth`, `avatar_url`, `must_change_password`, `email_verified_at`.
    - `Course.java`: Kế thừa BaseEntity, bổ sung quan hệ `prerequisiteCourse`, `target_output`, `tuition_amount`.
- **Trạng thái Quality Gate**:
  - Invariant Tests: `15/15 OK (Ran in 0.003s)`
  - Compile / Build: `BUILD SUCCESS (Compiling 27 source files via Docker in 1.41s)`
- **Ghi chú bàn giao**: Sẵn sàng chuyển tiếp sang `TASK-ADM-02` (Spring Security 6, BCrypt, JwtTokenProvider, JwtAuthenticationFilter).

---

### [2026-11-08 16:05] - AI Assistant (Pair with Team Lead) - TASK-ADM-00
- **Nhánh Git (Branch)**: `feature/admission`
- **Mục tiêu công việc**: Thiết lập nền tảng đa tầng, cấu hình Jackson snake_case, chuẩn hóa ApiResponse, ErrorDetail, PageResponse, ErrorCode (tiền tố AUTH_ và ADM_) và GlobalExceptionHandler theo Master Rules.
- **Chi tiết thay đổi**:
  - 🟢 **Thêm mới (Added)**:
    - `services/admission/backend/src/main/java/com/englishcenter/admission/common/dto/ApiResponse.java`
    - `services/admission/backend/src/main/java/com/englishcenter/admission/common/dto/ErrorDetail.java`
    - `services/admission/backend/src/main/java/com/englishcenter/admission/common/dto/PageResponse.java`
    - `services/admission/backend/src/main/java/com/englishcenter/admission/common/exception/ErrorCode.java`
    - `services/admission/backend/src/main/java/com/englishcenter/admission/common/exception/BusinessException.java`
    - `services/admission/backend/src/main/java/com/englishcenter/admission/common/exception/GlobalExceptionHandler.java`
  - 🟡 **Cập nhật / Nâng cấp (Updated)**:
    - `services/admission/backend/pom.xml`: Thêm dependency `spring-boot-starter-validation` và `spring-boot-starter-security`.
    - `services/admission/backend/src/main/resources/application.properties`: Bổ sung cấu hình Jackson snake_case, JWT expiration, internal secret key và inter-service URLs.
    - `services/admission/tasks/API_CATALOG.md`: Đồng bộ 60 API Endpoints sau khi thống nhất liên service với dev.
- **Trạng thái Quality Gate**:
  - Invariant Tests: `15/15 OK (Ran in 0.003s)`
  - Compile / Build: `BUILD SUCCESS (Maven Java 21 via Docker in 1.44s)`
- **Ghi chú bàn giao**: Sẵn sàng chuyển tiếp sang `TASK-ADM-01` (khởi tạo 14 JPA Entities theo `scripts/init-db.sql`).

---

### [2026-11-01 21:45] - Team Lead & AI Assistant - TASK-ADM-PLANNING
- **Nhánh Git (Branch)**: `feature/admission`
- **Mục tiêu công việc**: Hoàn tất tài liệu kiến trúc Ver 1 (v3.5), thiết lập 32 task chi tiết, checklist tiến độ và danh mục 55 API Endpoints của phân hệ Admission.
- **Chi tiết thay đổi**:
  - 🟢 **Thêm mới (Added)**:
    - `services/admission/tasks/README.md`: Kế hoạch WBS 7 phân kỳ và kiến trúc triển khai.
    - `services/admission/tasks/CHECKLIST.md`: Bảng theo dõi tiến độ sprint với checkbox từng task.
    - `services/admission/tasks/API_CATALOG.md`: Danh mục chuẩn 55 API Endpoints với HTTP method, quyền RBAC và payload.
    - `services/admission/tasks/WORKLOG.md`: File nhật ký phối hợp làm việc nhóm.
    - `services/admission/tasks/phase-0-foundation/`: 4 tasks nền móng hạ tầng (TASK-ADM-00 đến TASK-ADM-03).
    - `services/admission/tasks/phase-1-iam/`: 6 tasks định danh & phiên JWT (TASK-ADM-10 đến TASK-ADM-15).
    - `services/admission/tasks/phase-2-academic-facility/`: 5 tasks phòng học & lớp học (TASK-ADM-20 đến TASK-ADM-24).
    - `services/admission/tasks/phase-3-testing/`: 3 tasks khảo thí & đề xuất khóa học (TASK-ADM-30 đến TASK-ADM-32).
    - `services/admission/tasks/phase-4-enrollment/`: 8 tasks tuyển sinh & giữ chỗ 48h (TASK-ADM-40 đến TASK-ADM-47).
    - `services/admission/tasks/phase-5-operations-bi/`: 4 tasks xuất dữ liệu & BI (TASK-ADM-50 đến TASK-ADM-53).
    - `services/admission/tasks/phase-6-frontend/`: 5 tasks giao diện người dùng (TASK-ADM-60 đến TASK-ADM-64).
  - 🟡 **Cập nhật / Nâng cấp (Updated)**:
    - Thống nhất nguyên tắc giao tiếp liên service: Gọi **TRỰC TIẾP** qua private network Docker (`http://student-service:8082`, `http://finance-service:8084`), không đi vòng qua Nginx Gateway.
- **Trạng thái Quality Gate**:
  - Invariant Tests: `15/15 OK (Ran in 0.002s)`
- **Ghi chú bàn giao**: Toàn bộ tài liệu kỹ thuật và task backlog đã sẵn sàng để các lập trình viên bắt đầu nhận việc từ Phase 0 (`TASK-ADM-00` và `TASK-ADM-01`).

---

### [2026-10-25 22:45] - Project Initialization - WAVE-0
- **Nhánh Git (Branch)**: `main` / `dev`
- **Mục tiêu công việc**: Khởi tạo kiến trúc Microservices Monorepo, thiết lập Docker Compose 6 container, cấu hình Nginx Gateway và khởi tạo PostgreSQL 16 với 4 schema (`admission`, `student`, `learning`, `finance`).
- **Trạng thái Quality Gate**:
  - Bộ kiểm thử 15 bài Invariant Tests ban đầu: `15/15 OK`.
