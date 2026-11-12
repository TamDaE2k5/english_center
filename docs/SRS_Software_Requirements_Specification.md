# SRS – Software Requirements Specification
# Đặc tả Yêu cầu Phần mềm

**Dự án:** Hệ thống Quản lý Trung tâm Anh ngữ (English Center Management System)  
**Phiên bản tài liệu:** 2.0  
**Ngày lập:** 11/11/2026  
**Tham chiếu chuẩn:** ISO/IEC/IEEE 29148:2018 §9.6, IEEE 830-1998  

---

## Mục lục

1. [Phần 1: Mở đầu và Mô tả Tổng quan Phần mềm (SRS Overview & Context)](#phần-1-mở-đầu-và-mô-tả-tổng-quan-phần-mềm-srs-overview--context)
   - 1.1 [Mục đích tài liệu & Định vị SRS](#11-mục-đích-tài-liệu-purpose--định-vị-srs-trong-hệ-thống-tài-liệu)
   - 1.2 [Phạm vi sản phẩm](#12-phạm-vi-sản-phẩm-scope)
   - 1.3 [Góc nhìn sản phẩm](#13-góc-nhìn-sản-phẩm-product-perspective)
   - 1.4 [Chức năng sản phẩm mức tổng quát](#14-chức-năng-sản-phẩm-mức-tổng-quát-product-functions)
   - 1.5 [Đặc điểm và Vai trò Người dùng](#15-đặc-điểm-và-vai-trò-người-dùng-user-characteristics)
   - 1.6 [Giả định, Phụ thuộc và Ràng buộc Chung](#16-giả-định-phụ-thuộc-và-ràng-buộc-chung-limitations-assumptions--dependencies)
   - 1.7 [Định nghĩa và Thuật ngữ](#17-định-nghĩa-và-thuật-ngữ-definitions--acronyms)
   - 1.8 [Tài liệu tham chiếu](#18-tài-liệu-tham-chiếu-references)
   - 1.9 [Tổng quan Cấu trúc Tài liệu](#19-tổng-quan-cấu-trúc-tài-liệu-document-overview)
2. [Phần 2: Yêu cầu Chức năng Chi tiết (Specific Requirements — Functional)](#phần-2-yêu-cầu-chức-năng-chi-tiết-specific-requirements--functional)
   - 2.1 [Yêu cầu Giao diện Ngoài](#21-yêu-cầu-giao-diện-ngoài-external-interface-requirements)
     - 2.1.1 [Giao diện Người dùng (UI)](#211-giao-diện-người-dùng-user-interface)
     - 2.1.2 [Giao diện Phần mềm (API Contract)](#212-giao-diện-phần-mềm-software-interface--api-contract)
     - 2.1.3 [Giao diện Truyền thông](#213-giao-diện-truyền-thông-communication-interface)
   - 2.2 [Phân hệ Tuyển sinh (Admission Service)](#22-phân-hệ-tuyển-sinh-admission-service)
   - 2.3 [Phân hệ Học viên (Student Service)](#23-phân-hệ-học-viên-student-service)
   - 2.4 [Phân hệ Đào tạo (Learning Service)](#24-phân-hệ-đào-tạo-learning-service)
   - 2.5 [Phân hệ Tài chính (Finance Service)](#25-phân-hệ-tài-chính-finance-service)
3. [Phần 3: Yêu cầu Phi chức năng (Non-functional Requirements)](#phần-3-yêu-cầu-phi-chức-năng-non-functional-requirements)
   - 3.1 [Bảo mật (Security)](#31-bảo-mật-security)
   - 3.2 [Hiệu năng (Performance)](#32-hiệu-năng-performance)
   - 3.3 [Khả năng Bảo trì (Maintainability)](#33-khả-năng-bảo-trì-maintainability)
4. [Phần 4: Mô hình Dữ liệu và Mã lỗi Nghiệp vụ (Data Models & Business Errors)](#phần-4-mô-hình-dữ-liệu-và-mã-lỗi-nghiệp-vụ-data-models--business-errors)
   - 4.1 [Tổng hợp Mã lỗi Nghiệp vụ](#41-tổng-hợp-mã-lỗi-nghiệp-vụ)
   - 4.2 [Thống kê Business Rules](#42-thống-kê-business-rules)
5. [Phần 5: Ma trận Truy vết Yêu cầu (Requirements Traceability Matrix)](#phần-5-ma-trận-truy-vết-yêu-cầu-requirements-traceability-matrix)
6. [Phần 6: Phụ lục (Appendices)](#phần-6-phụ-lục-appendices)
   - 6.1 [Tài khoản Seed mẫu (Dev/Test)](#61-tài-khoản-seed-mẫu-devtest)
   - 6.2 [Phân công Nhóm Phát triển](#62-phân-công-nhóm-phát-triển)
   - 6.3 [Lịch sử Thay đổi Tài liệu](#63-lịch-sử-thay-đổi-tài-liệu)

---

## Phần 1: Mở đầu và Mô tả Tổng quan Phần mềm (SRS Overview & Context)

### 1.1 Mục đích tài liệu (Purpose) & Định vị SRS trong Hệ thống Tài liệu

Tài liệu SRS (Software Requirements Specification) mô tả chi tiết yêu cầu phần mềm ở **cấp độ kỹ thuật (Technical Level)**. Tài liệu tập trung giải quyết câu hỏi: **"HỆ THỐNG PHẢI LÀM GÌ?" (WHAT must the software system do?)** ở mức chi tiết hiện thực kỹ thuật, bao gồm: giao diện người dùng (UI specs), API endpoints, luồng xử lý chi tiết (Use Cases với pre/post-conditions), mô hình dữ liệu (Data models), các ràng buộc validation và quy tắc nghiệp vụ (Business Rules).

Tài liệu đạt mức độ chi tiết kỹ thuật cần thiết để:
- **Developer / Software Architect:** Hiện thực hóa trực tiếp thành mã nguồn (source code) và kiến trúc dịch vụ chuẩn xác, không bị mơ hồ.
- **QA / Tester:** Thiết kế test cases, test scenarios chi tiết và xây dựng bộ kiểm thử bất biến tự động (Invariant Tests).

**Thông tin định vị theo chuẩn kỹ thuật phần mềm:**
- **Mức độ trừu tượng (Abstraction level):** Chi tiết kỹ thuật (Detailed — Mức chức năng, API & CSDL).
- **Đối tượng sử dụng (Target audience):** Software Architect, Backend/Frontend Developer, QA/Tester, Tech Lead, PM.
- **Tác giả (Authors):** Requirements Engineer & System Analyst.

#### Định vị Quan hệ giữa BRS, StRS và SRS

Hệ thống tài liệu yêu cầu của dự án được xây dựng theo mô hình phân tầng tiêu chuẩn ISO/IEC/IEEE 29148:2018:

| Tiêu chí | BRS (Business Requirements) | StRS (Stakeholder Requirements) | SRS (Software Requirements) |
|---|---|---|---|
| **Tầng phân tích** | Nghiệp vụ (Business Level) | Các bên liên quan (Stakeholder Level) | Kỹ thuật phần mềm (Technical / Software Level) |
| **Câu hỏi cốt lõi** | **TẠI SAO? (WHY)** Cần xây dựng hệ thống? | **CẦN CÁI GÌ? (WHAT stakeholders need)** từ góc nhìn người dùng? | **HỆ THỐNG PHẢI LÀM GÌ? (WHAT the system must do)** ở mức kỹ thuật? |
| **Góc nhìn** | Mục tiêu kinh doanh, bài toán vận hành, giá trị tổ chức | Hộp đen (Black Box), ranh giới hệ thống, quy trình tương tác | Hộp xám / Hộp trắng (Grey/White Box), cấu trúc module, API, logic code |
| **Nội dung chính** | Business Goals, Business Rules (69 rules), As-Is / To-Be | System Context, User Needs, High-level Use Cases, Quality attributes | Functional Specs, Endpoints, Input/Output, Error Codes, UI, Invariants |
| **Đối tượng đọc** | Ban Giám đốc, Product Owner, BA | System Architect, Stakeholders, PO, Lead QA | Developer, Tester, System/Software Engineer, Tech Lead |

*Mối liên kết dòng chảy yêu cầu (Traceability Flow):*  
Mục tiêu kinh doanh & Quy tắc nghiệp vụ (**BRS**) $\longrightarrow$ Nhu cầu tác nhân & Use Case mức hệ thống (**StRS**) $\longrightarrow$ Đặc tả kỹ thuật chức năng, API Contract, UI Specs & Test Cases (**SRS**).

---

### 1.2 Phạm vi sản phẩm (Scope)

Phần mềm bao gồm 4 backend services (RESTful API), 4 frontend modules (HTML/CSS/JS), 1 API Gateway (Nginx) và 1 cơ sở dữ liệu PostgreSQL. Phần mềm quản lý trọn vòng đời học viên: tuyển sinh → học tập → đánh giá → tài chính.

---

### 1.3 Góc nhìn sản phẩm (Product Perspective)

Phần mềm là hệ thống mới, tự xây dựng từ đầu (greenfield), không thay thế phần mềm sẵn có. Hệ thống chạy trên nền Docker Compose, truy cập qua trình duyệt web.

---

### 1.4 Chức năng sản phẩm mức tổng quát (Product Functions)

| ID | Nhóm chức năng | Phân hệ |
|----|---------------|---------|
| F-01 | Xác thực & Phân quyền (JWT + RBAC) | Admission |
| F-02 | Quản lý Khóa học / Phòng / Lớp | Admission |
| F-03 | Xét duyệt Tuyển sinh | Admission |
| F-04 | Bài thi Đầu vào | Admission |
| F-05 | Quản lý Hồ sơ Học viên | Student |
| F-06 | Điểm danh Buổi học | Student |
| F-07 | Yêu cầu Bảo lưu / Chuyển / Rút | Student |
| F-08 | Kho Học liệu & Slide | Learning |
| F-09 | Sổ Điểm & Đánh giá | Learning |
| F-10 | Báo cáo Kết quả Học tập | Learning |
| F-11 | Biểu phí & Voucher | Finance |
| F-12 | Hóa đơn & Theo dõi Nợ | Finance |
| F-13 | Ghi nhận Thanh toán | Finance |
| F-14 | Ví Học viên | Finance |
| F-15 | Yêu cầu Hoàn phí | Finance |
| F-16 | Dashboard BI | Admission + Finance |

---

### 1.5 Đặc điểm và Vai trò Người dùng (User Characteristics)

| Vai trò | Tần suất sử dụng | Trình độ kỹ thuật | Chức năng truy cập |
|---------|------------------|------------------|-------------------|
| **ADMIN** | Hàng ngày | Cao | Toàn bộ hệ thống |
| **STAFF** | Hàng ngày | Trung bình | Tuyển sinh, Hồ sơ, Hóa đơn, Thanh toán |
| **TEACHER** | Theo lịch giảng dạy | Cơ bản | Điểm danh, Nhập điểm, Upload tài liệu (chỉ lớp mình) |
| **STUDENT** | Không thường xuyên | Cơ bản | Xem hồ sơ/điểm/hóa đơn/tài liệu (chỉ của mình) |

---

### 1.6 Giả định, Phụ thuộc và Ràng buộc Chung (Limitations, Assumptions & Dependencies)

#### 1.6.1 Ràng buộc Kỹ thuật và Giới hạn (Constraints & Limitations)

| ID | Ràng buộc |
|----|----------|
| C-01 | Container hóa: chạy qua Docker Compose |
| C-02 | Admission bắt buộc Java 21 / Spring Boot 3.3 |
| C-03 | Student, Learning, Finance bắt buộc Python 3.11 / FastAPI |
| C-04 | Frontend: HTML/CSS/JS thuần, không framework SPA |
| C-05 | Database: 1 PostgreSQL chia 4 schema, không dùng 4 DB riêng |
| C-06 | Quy ước: 100% snake_case (API, DB, JSON) |
| C-07 | Mỗi nhóm chỉ sửa code trong `services/{tên}/` |

#### 1.6.2 Giả định và Phụ thuộc (Assumptions & Dependencies)

| ID | Giả định |
|----|---------|
| A-01 | Người dùng truy cập qua trình duyệt desktop (≥ 1024px) |
| A-02 | Docker Engine đã được cài đặt trên máy phát triển |
| A-03 | Thanh toán ghi nhận thủ công, chưa tích hợp cổng tự động |
| A-04 | Dữ liệu mẫu (seed) cung cấp sẵn qua `scripts/init-db.sql` |

---

### 1.7 Định nghĩa và Thuật ngữ (Definitions & Acronyms)

| Thuật ngữ | Định nghĩa |
|-----------|-----------|
| **CRUD** | Create, Read, Update, Delete — 4 thao tác dữ liệu cơ bản |
| **Endpoint** | Một URL API cụ thể mà client gọi đến |
| **Pydantic** | Thư viện Python validate dữ liệu đầu vào/ra |
| **SQLAlchemy** | ORM (Object-Relational Mapping) cho Python |
| **Spring Data JPA** | ORM cho Java/Spring Boot |
| **Idempotency** | Cùng request gửi nhiều lần → cùng kết quả, không sinh trùng giao dịch |
| **Toast** | Thông báo nhỏ hiển thị tạm thời trên giao diện |

---

### 1.8 Tài liệu tham chiếu (References)

| Tài liệu | Mô tả |
|----------|-------|
| BRS (tài liệu đi kèm) | Đặc tả Yêu cầu Nghiệp vụ — WHY (Mục tiêu kinh doanh & Quy tắc nghiệp vụ) |
| StRS (tài liệu đi kèm) | Đặc tả Yêu cầu Các bên liên quan (Stakeholder Requirements Specification) — WHAT |
| ISO/IEC/IEEE 29148:2018 | Systems and software engineering — Requirements engineering (§9.6 Software requirements specification) |
| IEEE 830-1998 | Recommended Practice for Software Requirements Specifications |

---

### 1.9 Tổng quan Cấu trúc Tài liệu (Document Overview)

Tài liệu được cấu trúc theo 3 phần trọng tâm của chuẩn ISO/IEC/IEEE 29148:2018 §9.6 cùng các phần mở rộng kỹ thuật:
- **Phần 1: Mở đầu và Mô tả Tổng quan Phần mềm:** Bối cảnh, mục đích, vị trí tài liệu, phạm vi, tổng quan chức năng, vai trò người dùng, giả định và ràng buộc.
- **Phần 2: Yêu cầu Chức năng Chi tiết:** Đặc tả giao diện ngoài (UI, API Contract, mạng) và 16 yêu cầu chức năng (FR) chi tiết theo 4 phân hệ (endpoints, I/O, business rules, error codes, luồng xử lý).
- **Phần 3: Yêu cầu Phi chức năng:** Tiêu chuẩn chất lượng phần mềm về Bảo mật, Hiệu năng và Khả năng Bảo trì.
- **Phần 4: Mô hình Dữ liệu và Mã lỗi Nghiệp vụ:** Tổng hợp mã lỗi chuẩn và thống kê 69 Business Rules.
- **Phần 5: Ma trận Truy vết Yêu cầu:** Ánh xạ liên kết giữa SRS, BRS, StRS và Invariant Tests.
- **Phần 6: Phụ lục:** Dữ liệu seed mẫu, danh sách phân công nhóm và lịch sử thay đổi tài liệu.

---

## Phần 2: Yêu cầu Chức năng Chi tiết (Specific Requirements — Functional)

### 2.1 Yêu cầu Giao diện Ngoài (External Interface Requirements)

#### 2.1.1 Giao diện Người dùng (User Interface)

| ID | Yêu cầu | Mô tả |
|----|---------|-------|
| UI-01 | Responsive layout | Desktop ≥ 1024px |
| UI-02 | Loading indicator | Hiển thị spinner trong khi chờ API phản hồi |
| UI-03 | Toast notification | Thông báo nổi khi thành công (xanh) hoặc lỗi (đỏ) |
| UI-04 | Confirm dialog | Bắt buộc trước mọi hành động DELETE |
| UI-05 | Ngôn ngữ | Toàn bộ giao diện bằng Tiếng Việt |
| UI-06 | Phân trang | Mặc định 20 bản ghi/trang, có nút Previous/Next |
| UI-07 | Sidebar động | Menu thay đổi theo role đang đăng nhập |
| UI-08 | Phân quyền UI | Ẩn nút/chức năng nếu user không đủ quyền |
| UI-09 | Breadcrumb | Hiển thị đường dẫn điều hướng |
| UI-10 | Color coding | Trạng thái mã màu: success → xanh, warning → vàng, danger → đỏ |
| UI-11 | Form validation | Validate phía client trước khi gửi server |
| UI-12 | Empty state | Thông báo thân thiện khi bảng dữ liệu rỗng |

#### 2.1.2 Giao diện Phần mềm (Software Interface — API Contract)

**Response thành công:**
```json
{
  "success": true,
  "data": { ... },
  "message": "Thao tác thành công"
}
```

**Response thất bại:**
```json
{
  "success": false,
  "error": {
    "code": "MÃ_LỖI",
    "message": "Mô tả lỗi bằng tiếng Việt"
  }
}
```

**Response phân trang:**
```json
{
  "success": true,
  "data": {
    "items": [ ... ],
    "total": 150,
    "page": 1,
    "size": 20,
    "total_pages": 8
  }
}
```

**Bảng HTTP Status Codes:**

| Code | Ý nghĩa | Ví dụ |
|------|---------|-------|
| 200 | OK | GET, PUT thành công |
| 201 | Created | POST tạo mới thành công |
| 400 | Bad Request | Lớp đầy, điểm âm, dữ liệu không hợp lệ |
| 401 | Unauthorized | Token hết hạn, chưa đăng nhập |
| 403 | Forbidden | HV xem hồ sơ người khác, GV thao tác lớp khác |
| 404 | Not Found | ID không tồn tại |
| 409 | Conflict | Trùng transaction_ref, trùng mã khóa, trùng điểm danh |
| 422 | Unprocessable | Lỗi validation Pydantic |
| 500 | Server Error | KHÔNG lộ stack trace |

#### 2.1.3 Giao diện Truyền thông (Communication Interface)

| Kết nối | Giao thức | Port |
|---------|----------|------|
| Browser ↔ Gateway | HTTP/HTTPS | 8080 |
| Gateway ↔ Service | HTTP | 8081–8084 |
| Service ↔ Database | TCP (PostgreSQL) | 5432 |
| Service ↔ Service | HTTP REST (JSON) | Internal |

---

### 2.2 Phân hệ TUYỂN SINH (Admission Service)

#### FR-ADM-01: Xác thực & Phân quyền

| Thuộc tính | Chi tiết |
|-----------|---------|
| **ID** | FR-ADM-01 |
| **Tên** | Xác thực và Phân quyền Người dùng |
| **Actor** | Mọi người dùng |
| **Trigger** | Đăng nhập hệ thống |
| **Pre-condition** | Tài khoản đã tồn tại trong `admission.users` |
| **Post-condition** | access_token + refresh_token trả về cho client |

**Endpoints:**

| Method | URL | Mô tả | Auth |
|--------|-----|-------|------|
| POST | `/auth/login` | Đăng nhập | Không |
| POST | `/auth/refresh` | Làm mới token | refresh_token |
| POST | `/auth/change-password` | Đổi mật khẩu | access_token |
| POST | `/auth/logout` | Đăng xuất (revoke refresh_token) | access_token |

**Business Rules:**
- Mật khẩu bcrypt cost ≥ 10
- access_token TTL = 15 phút
- refresh_token TTL = 7 ngày, revocable
- Rate limiting: max 10 lần sai / 15 phút / IP
- Audit log: ghi mỗi lần login/logout/đổi mật khẩu

---

#### FR-ADM-02: Quản lý Khóa học

| Thuộc tính | Chi tiết |
|-----------|---------|
| **ID** | FR-ADM-02 |
| **Actor** | ADMIN, STAFF |
| **UI** | `courses.html` |

**Endpoints:**

| Method | URL | Mô tả |
|--------|-----|-------|
| GET | `/api/admission/courses` | Danh sách (phân trang) |
| GET | `/api/admission/courses/{id}` | Chi tiết |
| POST | `/api/admission/courses` | Tạo mới |
| PUT | `/api/admission/courses/{id}` | Cập nhật |
| DELETE | `/api/admission/courses/{id}` | Xóa |

**Input:** `code`, `name`, `description`, `level`, `duration_weeks`, `tuition_amount`  
**Output:** Course object + `id`, `is_active`, `created_at`

**Business Rules:** BR-ADM-01 (code UNIQUE), BR-ADM-02 (tuition ≥ 0), BR-ADM-12 (không xóa nếu có lớp OPEN)

---

#### FR-ADM-03: Quản lý Lớp học

| Thuộc tính | Chi tiết |
|-----------|---------|
| **ID** | FR-ADM-03 |
| **Actor** | STAFF, ADMIN |
| **UI** | `classes.html` |

**Endpoints:**

| Method | URL | Mô tả |
|--------|-----|-------|
| GET | `/api/admission/classes` | Danh sách |
| POST | `/api/admission/classes` | Tạo mới |
| PUT | `/api/admission/classes/{id}` | Cập nhật |
| DELETE | `/api/admission/classes/{id}` | Xóa |

**Input:** `course_id`, `code`, `room_id`, `teacher_id`, `day_pattern`, `schedule`, `start_date`, `end_date`, `max_students`

**Business Rules:** BR-ADM-04 (max_students ≥ 1), BR-ADM-05 (không trùng phòng+giờ), BR-ADM-06 (không trùng GV+giờ), BR-ADM-07 (capacity ≥ max_students), BR-ADM-11 (khóa tạm ngưng), BR-ADM-13 (COMPLETED)

---

#### FR-ADM-04: Xét duyệt Tuyển sinh ★

| Thuộc tính | Chi tiết |
|-----------|---------|
| **ID** | FR-ADM-04 |
| **Tên** | Xét duyệt Đơn Đăng ký Nhập học |
| **Actor** | STAFF |
| **UI** | `enrollments.html` |
| **Ưu tiên** | **P0 — Tiêu chí nghiệm thu #2** |
| **Pre-condition** | Lớp học tồn tại, HV có tài khoản |
| **Post-condition** | Enrollment APPROVED, hồ sơ HV tạo, hóa đơn tạo |

**Endpoints:**

| Method | URL | Mô tả |
|--------|-----|-------|
| GET | `/api/admission/enrollments` | Danh sách đơn |
| POST | `/api/admission/enrollments` | Tạo đơn (PENDING) |
| PATCH | `/api/admission/enrollments/{id}/approve` | Duyệt |
| PATCH | `/api/admission/enrollments/{id}/cancel` | Từ chối / Hủy |

**Luồng chính (Duyệt đơn):**

```
1. Staff bấm "Duyệt" → gọi PATCH .../approve
2. Server kiểm tra: COUNT(APPROVED) < max_students ?
   → KHÔNG: Trả 400 { code: "ADM_CLASS_FULL" }
   → CÓ: Tiếp tục
3. Chuyển enrollment → APPROVED
4. Gọi POST /api/student/students → tạo hồ sơ HV
5. Gọi POST /api/finance/invoices → tạo hóa đơn PENDING
6. Trả 200 { success: true }
```

**Business Rules:** BR-ADM-03 (sĩ số), BR-ADM-08 (CANCELLED ✘→ APPROVED), BR-ADM-09 (48h EXPIRED), BR-ADM-10 (max 1 đơn/lớp/HV)

**Mã lỗi:**

| Code | HTTP | Mô tả |
|------|------|-------|
| `ADM_CLASS_FULL` | 400 | Lớp đã đủ sĩ số |
| `ADM_INVALID_TRANSITION` | 400 | Chuyển trạng thái không hợp lệ |

---

#### FR-ADM-05: Bài thi Đầu vào

| Thuộc tính | Chi tiết |
|-----------|---------|
| **ID** | FR-ADM-05 |
| **Actor** | STUDENT (làm bài), ADMIN/TEACHER (quản lý đề) |
| **UI** | `test.html` |

**Endpoints:**

| Method | URL | Mô tả |
|--------|-----|-------|
| GET | `/api/admission/test-sets` | Danh sách bộ đề |
| POST | `/api/admission/test-sets` | Tạo đề (ADMIN) |
| POST | `/api/admission/test/start` | Bắt đầu làm bài |
| POST | `/api/admission/test/submit` | Nộp bài |

**Business Rules:** Thời gian ≤ `time_limit_minutes`, chấm tự động, đề xuất khóa phù hợp.

---

#### FR-ADM-06: BI Dashboard Tuyển sinh

| Method | URL | Mô tả |
|--------|-----|-------|
| GET | `/api/admission/bi/funnel` | Phễu cohort tuyển sinh |
| GET | `/api/admission/bi/courses-revenue` | Xếp hạng khóa học |
| GET | `/api/admission/bi/security-engagement` | Thống kê login (ADMIN) |

---

### 2.3 Phân hệ HỌC VIÊN (Student Service)

#### FR-STU-01: Quản lý Hồ sơ Học viên

| Thuộc tính | Chi tiết |
|-----------|---------|
| **ID** | FR-STU-01 |
| **Actor** | ADMIN/STAFF (full CRUD), STUDENT (chỉ xem mình) |
| **UI** | `students.html` |

**Endpoints:**

| Method | URL | Mô tả |
|--------|-----|-------|
| GET | `/api/student/students` | Danh sách (Admin/Staff) |
| GET | `/api/student/students/me` | Hồ sơ chính mình (Student) |
| GET | `/api/student/students/{id}` | Chi tiết |
| POST | `/api/student/students` | Tạo mới |
| PUT | `/api/student/students/{id}` | Cập nhật |

**Input:** `full_name`, `email`, `phone`, `date_of_birth`, `address`, `user_id`  
**Output:** Student object + `student_code` tự sinh (HV-YYYY-XXXX)

**Business Rules:** BR-STU-01→06, BR-STU-13 (cô lập), BR-STU-15 (DROPPED), BR-STU-18 (không xóa)

---

#### FR-STU-02: Điểm danh Buổi học ★

| Thuộc tính | Chi tiết |
|-----------|---------|
| **ID** | FR-STU-02 |
| **Tên** | Điểm danh Buổi học — Chống trùng lặp |
| **Actor** | TEACHER (lớp mình), STAFF |
| **UI** | `attendance.html` |
| **Ưu tiên** | **P0 — Tiêu chí nghiệm thu #3** |

**Endpoints:**

| Method | URL | Mô tả |
|--------|-----|-------|
| GET | `/api/student/attendance?class_id=X` | Danh sách theo lớp |
| POST | `/api/student/attendance` | Tạo bản ghi |
| PUT | `/api/student/attendance/{id}` | Sửa trạng thái |

**Input:** `class_id`, `student_id`, `session_date`, `status`, `notes`

**Luồng chính:**

```
1. GV chọn lớp + ngày + HV → chọn trạng thái
2. Server kiểm tra: GV phụ trách lớp? → KHÔNG: 403
3. Server kiểm tra: UNIQUE(class_id, student_id, session_date)
   → Đã có: 409 Conflict
   → Chưa có: Lưu thành công → 201
```

**Business Rules:** BR-STU-07→12

**Mã lỗi:**

| Code | HTTP | Mô tả |
|------|------|-------|
| (UNIQUE violation) | 409 | Đã điểm danh HV này hôm nay |
| (Authorization) | 403 | GV không phụ trách lớp |

---

#### FR-STU-03: Yêu cầu Học vụ

| Thuộc tính | Chi tiết |
|-----------|---------|
| **ID** | FR-STU-03 |
| **Actor** | STUDENT (tạo), STAFF/ADMIN (duyệt) |
| **UI** | `status_requests.html` |

**Endpoints:**

| Method | URL | Mô tả |
|--------|-----|-------|
| GET | `/api/student/status-requests` | Danh sách |
| POST | `/api/student/status-requests` | Tạo yêu cầu |
| PATCH | `/api/student/status-requests/{id}/review` | Duyệt/Từ chối |

**Input:** `student_id`, `request_type` (RESERVE/TRANSFER/DROP), `reason`, `class_id`, `target_class_id`

**Business Rules:** BR-STU-14→18

---

### 2.4 Phân hệ ĐÀO TẠO (Learning Service)

#### FR-LRN-01: Quản lý Kho Học liệu

| Thuộc tính | Chi tiết |
|-----------|---------|
| **ID** | FR-LRN-01 |
| **Actor** | TEACHER (upload/xóa), STUDENT (xem lớp mình) |
| **UI** | `materials.html` |

**Endpoints:**

| Method | URL | Mô tả |
|--------|-----|-------|
| GET | `/api/learning/materials?class_id=X` | Tài liệu theo lớp |
| POST | `/api/learning/materials` | Upload tài liệu |
| DELETE | `/api/learning/materials/{id}` | Xóa tài liệu |

**Business Rules:** BR-LRN-01→03, BR-LRN-08, BR-LRN-09

---

#### FR-LRN-02: Sổ Điểm & Đánh giá ★

| Thuộc tính | Chi tiết |
|-----------|---------|
| **ID** | FR-LRN-02 |
| **Tên** | Nhập Điểm — Toàn vẹn thang điểm |
| **Actor** | TEACHER |
| **UI** | `grades.html` |
| **Ưu tiên** | **P0 — Tiêu chí nghiệm thu #4** |

**Endpoints:**

| Method | URL | Mô tả |
|--------|-----|-------|
| GET | `/api/learning/grades?class_id=X` | Điểm theo lớp |
| GET | `/api/learning/grades?class_id=X&student_id=Y` | Điểm theo HV |
| POST | `/api/learning/grades` | Nhập điểm |
| PUT | `/api/learning/grades/{id}` | Sửa điểm |

**Input:** `class_id`, `student_id`, `assessment_name`, `score`, `max_score`, `weight`, `feedback`

**Luồng chính:**

```
1. GV chọn lớp → chọn HV → nhập điểm
2. Server kiểm tra: 0 ≤ score ≤ max_score
   → Vi phạm: 400 { code: "LRN_INVALID_SCORE" }
   → Hợp lệ: Lưu → 201
```

**Business Rules:** BR-LRN-04→08, BR-LRN-10

**Mã lỗi:**

| Code | HTTP | Mô tả |
|------|------|-------|
| `LRN_INVALID_SCORE` | 400 | Điểm âm |
| `LRN_SCORE_EXCEEDS_MAX` | 400 | Điểm vượt max_score |

---

#### FR-LRN-03: Báo cáo Kết quả Học tập

| Method | URL | Mô tả |
|--------|-----|-------|
| GET | `/api/learning/reports/class-summary?class_id=X` | Bảng xếp loại + tỷ lệ đậu/rớt |

**Output:** `total_students`, `pass_count`, `fail_count`, `pass_rate_percentage` + danh sách HV với `weighted_avg_score`, `result` (Pass/Fail)

**Business Rules:** BR-LRN-11 (≥ 2 bài), BR-LRN-12 (≥ 50% = Pass), BR-LRN-13 (công thức trọng số)

---

### 2.5 Phân hệ TÀI CHÍNH (Finance Service)

#### FR-FIN-01: Biểu phí & Voucher

**Endpoints (Biểu phí):**

| Method | URL | Mô tả |
|--------|-----|-------|
| GET | `/api/finance/tuition-fees` | Danh sách |
| POST | `/api/finance/tuition-fees` | Thêm biểu phí |

**Endpoints (Voucher):**

| Method | URL | Mô tả |
|--------|-----|-------|
| GET | `/api/finance/vouchers` | Danh sách |
| POST | `/api/finance/vouchers` | Tạo voucher |
| PUT | `/api/finance/vouchers/{id}` | Cập nhật |

**Business Rules:** BR-FIN-01, BR-FIN-06, BR-FIN-10→13

---

#### FR-FIN-02: Hóa đơn & Theo dõi Nợ

| Thuộc tính | Chi tiết |
|-----------|---------|
| **ID** | FR-FIN-02 |
| **Actor** | STAFF, ADMIN |
| **UI** | `invoices.html` |

**Endpoints:**

| Method | URL | Mô tả |
|--------|-----|-------|
| GET | `/api/finance/invoices` | Danh sách (filter theo status, student_id) |
| GET | `/api/finance/invoices/{id}` | Chi tiết + danh sách payments |
| POST | `/api/finance/invoices` | Tạo hóa đơn |
| PATCH | `/api/finance/invoices/{id}/cancel` | Hủy hóa đơn |

**Input:** `student_id`, `enrollment_id`, `course_id`, `amount`, `voucher_code` (optional), `due_date`

**Business Rules:** BR-FIN-02 (code UNIQUE), BR-FIN-07→09, BR-FIN-14→16

---

#### FR-FIN-03: Ghi nhận Thanh toán ★

| Thuộc tính | Chi tiết |
|-----------|---------|
| **ID** | FR-FIN-03 |
| **Tên** | Ghi nhận Thanh toán — Idempotency |
| **Actor** | STAFF |
| **Ưu tiên** | **P0 — Tiêu chí nghiệm thu #5** |

**Endpoint:**

| Method | URL | Mô tả |
|--------|-----|-------|
| POST | `/api/finance/payments` | Ghi nhận thanh toán |

**Input:** `invoice_id`, `amount`, `payment_method`, `transaction_ref`

**Luồng chính:**

```
1. Staff nhập thông tin thanh toán
2. Server kiểm tra: amount > 0 ?
   → KHÔNG: 400 { code: "FIN_INVALID_AMOUNT" }
3. Server kiểm tra: transaction_ref chưa tồn tại ?
   → ĐÃ CÓ: 409 { code: "FIN_DUPLICATE_TRANSACTION" }
4. Ghi nhận payment → cập nhật invoice.paid_amount
5. Σpayments ≥ final_amount ?
   → CÓ: Chuyển invoice → PAID
   → CHƯA: Giữ trạng thái
```

**Business Rules:** BR-FIN-01, BR-FIN-03→05, BR-FIN-15, BR-FIN-17

**Mã lỗi:**

| Code | HTTP | Mô tả |
|------|------|-------|
| `FIN_INVALID_AMOUNT` | 400 | Số tiền ≤ 0 |
| `FIN_DUPLICATE_TRANSACTION` | 409 | Trùng transaction_ref |

---

#### FR-FIN-04: Ví Học viên

| Method | URL | Mô tả |
|--------|-----|-------|
| GET | `/api/finance/wallets/{student_id}` | Thông tin ví |
| POST | `/api/finance/wallets/{student_id}/topup` | Nạp tiền |
| GET | `/api/finance/wallets/{student_id}/transactions` | Lịch sử |

**Business Rules:** BR-FIN-18 (balance ≥ 0)

---

#### FR-FIN-05: Yêu cầu Hoàn phí

| Method | URL | Mô tả |
|--------|-----|-------|
| POST | `/api/finance/refunds` | Tạo yêu cầu |
| PATCH | `/api/finance/refunds/{id}/review` | Duyệt/Từ chối |

**Business Rules:** BR-FIN-19 (2 cấp duyệt), BR-FIN-20 (hoàn ≤ đã thu)

---

#### FR-FIN-06: BI Tài chính

| Method | URL | Mô tả |
|--------|-----|-------|
| GET | `/api/finance/bi/cash-revenue` | Doanh thu theo tháng |
| GET | `/api/finance/bi/voucher-campaigns` | Hiệu quả voucher |
| GET | `/api/finance/bi/refund-summary` | Tổng hợp hoàn phí |

---

## Phần 3: Yêu cầu Phi chức năng (Non-functional Requirements)

### 3.1 Bảo mật (Security)

| ID | Yêu cầu | Chi tiết |
|----|---------|---------|
| NFR-SEC-01 | JWT access_token | HS256, TTL 15 phút |
| NFR-SEC-02 | JWT refresh_token | TTL 7 ngày, revocable |
| NFR-SEC-03 | Mật khẩu | Bcrypt cost ≥ 10 |
| NFR-SEC-04 | Phân quyền RBAC | 4 cấp: ADMIN > STAFF > TEACHER > STUDENT |
| NFR-SEC-05 | Cô lập schema DB | Mỗi service chỉ query schema mình |
| NFR-SEC-06 | Input validation | Chống SQL Injection |
| NFR-SEC-07 | Secrets management | Env vars, không hardcode |
| NFR-SEC-08 | Rate limiting | 10 sai / 15 phút / IP |
| NFR-SEC-09 | Audit log | Login, duyệt đơn, thanh toán |
| NFR-SEC-10 | Error handling | Không lộ stack trace ra client |

---

### 3.2 Hiệu năng (Performance)

| ID | Yêu cầu | Tiêu chí |
|----|---------|---------|
| NFR-PERF-01 | API response | p95 ≤ 2000ms @100 CCU |
| NFR-PERF-02 | FCP | ≤ 1500ms |
| NFR-PERF-03 | DB query | ≤ 5 queries/API call |
| NFR-PERF-04 | Upload | ≤ 50MB/file |

---

### 3.3 Khả năng Bảo trì (Maintainability)

| ID | Yêu cầu | Chi tiết |
|----|---------|---------|
| NFR-MAINT-01 | Naming convention | 100% snake_case |
| NFR-MAINT-02 | Documentation | Comment tiếng Việt, docstring |
| NFR-MAINT-03 | Config | 12-Factor (env vars) |
| NFR-MAINT-04 | Hot-reload | Dev mode |
| NFR-MAINT-05 | Testing | 15 invariant tests luôn PASS |
| NFR-MAINT-06 | Code isolation | Mỗi nhóm riêng thư mục |

---

## Phần 4: Mô hình Dữ liệu và Mã lỗi Nghiệp vụ (Data Models & Business Errors)

*(Chi tiết schema database — xem StRS §7 để tránh trùng lặp)*

### 4.1 Tổng hợp Mã lỗi Nghiệp vụ

| Code | Module | HTTP | Mô tả |
|------|--------|------|-------|
| `ADM_CLASS_FULL` | Admission | 400 | Lớp đã đủ sĩ số |
| `ADM_INVALID_TRANSITION` | Admission | 400 | Chuyển trạng thái không hợp lệ |
| `ADM_DUPLICATE_CODE` | Admission | 409 | Mã khóa học trùng |
| `ADM_VALIDATION_ERROR` | Admission | 400 | Dữ liệu không hợp lệ |
| `LRN_INVALID_SCORE` | Learning | 400 | Điểm âm |
| `LRN_SCORE_EXCEEDS_MAX` | Learning | 400 | Điểm vượt max_score |
| `FIN_DUPLICATE_TRANSACTION` | Finance | 409 | Trùng transaction_ref |
| `FIN_INVALID_AMOUNT` | Finance | 400 | Số tiền ≤ 0 |
| `FIN_VOUCHER_EXPIRED` | Finance | 400 | Voucher hết hạn |
| `FIN_VOUCHER_EXHAUSTED` | Finance | 400 | Voucher hết lượt |
| `FIN_VALIDATION_ERROR` | Finance | 422 | Dữ liệu không hợp lệ |

---

### 4.2 Thống kê Business Rules

| Module | Số lượng |
|--------|---------|
| Admission (ADM) | 13 rules |
| Student (STU) | 18 rules |
| Learning (LRN) | 13 rules |
| Finance (FIN) | 20 rules |
| Bảo mật (SEC) | 3 rules |
| Hệ thống (SYS) | 2 rules |
| **Tổng** | **69 rules** |

---

## Phần 5: Ma trận Truy vết Yêu cầu (Requirements Traceability Matrix)

| SRS ID | Tên chức năng | BRS Rule | StRS Use Case | Invariant Test |
|--------|-------------|---------|----------------|---------------|
| FR-ADM-04 | Duyệt đơn tuyển sinh | BR-ADM-03 | UC-SYS-01 | test_admission_invariants |
| FR-STU-02 | Điểm danh buổi học | BR-STU-07 | UC-SYS-02 | test_student_invariants |
| FR-LRN-02 | Nhập điểm đánh giá | BR-LRN-04 | — | test_learning_invariants |
| FR-FIN-03 | Thanh toán Idempotency | BR-FIN-03, BR-FIN-04 | UC-SYS-03 | test_finance_invariants |
| — | Cô lập hồ sơ HV | BR-SEC-01 | — | test_security_invariants |
| — | GV chỉ lớp mình | BR-SEC-02 | — | test_security_invariants |
| — | snake_case + schema | BR-SYS-01, BR-SYS-02 | — | test_system_invariants |

---

## Phần 6: Phụ lục (Appendices)

### 6.1 Tài khoản Seed mẫu (Dev/Test)

| Email | Mật khẩu | Role |
|-------|---------|------|
| `admin@english.edu.vn` | `admin123` | ADMIN |
| `staff@english.edu.vn` | `staff123` | STAFF |
| `teacher@english.edu.vn` | `teacher123` | TEACHER |
| `student@english.edu.vn` | `student123` | STUDENT |

---

### 6.2 Phân công Nhóm Phát triển

| Phân hệ | Thành viên | Thư mục |
|---------|-----------|---------|
| Tuyển sinh (Admission) | Nguyễn Mạnh Hưng, Nghiêm Đình Tâm | `services/admission/` |
| Học viên (Student) | Lê Hữu Phong, Đào Trọng Phương | `services/student/` |
| Đào tạo (Learning) | Phạm Anh Nam, Lê Hữu Phong | `services/learning/` |
| Tài chính (Finance) | Lê Thị Thủy Chi, Vũ Hoàng Phúc | `services/finance/` |

---

### 6.3 Lịch sử Thay đổi Tài liệu

| Phiên bản | Ngày | Người sửa | Mô tả |
|-----------|------|-----------|-------|
| 1.0 | 11/11/2026 | Nhóm CNPM | Phiên bản ban đầu |
| 2.0 | 11/11/2026 | Nhóm CNPM | Tái cấu trúc theo chuẩn ISO/IEC/IEEE 29148:2018 §9.6 (3 phần chuẩn: Overview & Context, Specific Requirements - Functional & Interfaces, Non-functional Requirements; bổ sung định vị BRS - StRS - SRS, chuẩn hóa tham chiếu StRS) |

---

*Chuẩn tham chiếu: ISO/IEC/IEEE 29148:2018 §9.6, IEEE 830-1998 — Software Requirements Specifications*
