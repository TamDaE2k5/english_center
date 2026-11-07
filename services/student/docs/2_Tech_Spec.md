# ĐẶC TẢ KỸ THUẬT (TECHNICAL SPECIFICATION) - PHÂN HỆ HỌC VIÊN


## 1. Công nghệ & Kiến trúc sử dụng

- **Ngôn ngữ Backend:** Python 3.10+
- **Framework:** FastAPI (Xử lý API, tự động sinh Swagger UI).
- **ORM:** SQLAlchemy (Giao tiếp Database).
- **Database:** PostgreSQL (Sử dụng Schema `student`).
- **Giao tiếp:** Tuân thủ kiến trúc Microservices. Giao tiếp với phân hệ khác qua REST API nội bộ, tuyệt đối không query chéo Database.


## 2. Thiết kế Cơ sở dữ liệu (Database Schema)

### 2.1. Bảng `student.students` 
- `id` (INTEGER, PK)
- `user_id` (INTEGER) - *Liên kết logic tới admission.users*
- **`student_code` (VARCHAR(30), UNIQUE, NOT NULL)** - *Mới: Mã định danh dạng `HV-2026-xxxx`.*
- `full_name` (VARCHAR(255), NOT NULL)
- **`email` (VARCHAR(255), UNIQUE, NOT NULL)** - *Cập nhật: Thêm ràng buộc UNIQUE để tránh trùng lặp.*
- `phone` (VARCHAR(50))
- `date_of_birth` (DATE)
- `address` (TEXT)
- `status` (VARCHAR(50), CHECK IN ('STUDYING', 'RESERVED', 'DROPPED', 'GRADUATED'), DEFAULT 'STUDYING')
- `created_at`, **`updated_at`** (TIMESTAMPTZ) - *Mới: Thêm updated_at để theo dõi thời gian chỉnh sửa.*

### 2.2. Bảng `student.attendance` 
- `id` (INTEGER, PK)
- `class_id` (INTEGER, NOT NULL) - *Liên kết logic tới admission.classes*
- `student_id` (INTEGER, FK -> `student.students(id)`, NOT NULL)
- `session_date` (DATE, NOT NULL)
- `status` (VARCHAR(50), CHECK IN ('PRESENT', 'ABSENT', 'LATE', 'EXCUSED'), DEFAULT 'PRESENT')
- `notes` (TEXT)
- `created_at` (TIMESTAMPTZ)
- **RÀNG BUỘC MỚI:** `UNIQUE(class_id, student_id, session_date)` (Chặn tuyệt đối việc điểm danh 2 lần/ngày/lớp).

### 2.3. Bảng `student.status_requests` 
*Lưu trữ yêu cầu bảo lưu, chuyển lớp, rút hồ sơ của học viên.*
- `id` (INTEGER, PK)
- `student_id` (INTEGER, FK -> `student.students(id)`, NOT NULL)
- `request_type` (VARCHAR(20), CHECK IN ('RESERVE', 'TRANSFER', 'DROP'))
- `reason` (TEXT, NOT NULL)
- `status` (VARCHAR(20), CHECK IN ('PENDING', 'APPROVED', 'REJECTED'), DEFAULT 'PENDING')
- `created_at` (TIMESTAMPTZ)


## 3. Thiết kế API Endpoints (Dự kiến)
*(Frontend sẽ gọi các API này qua cổng Nginx Gateway ở địa chỉ `http://localhost:8080/api/v1/...`)*

### Nhóm API Hồ sơ (STU-01)
- `GET /api/v1/students` : Lấy danh sách học viên (có phân trang, tìm kiếm theo tên/mã).
- `POST /api/v1/students` : Tạo hồ sơ (Hệ thống tự sinh `student_code`).
- `PUT /api/v1/students/{id}` : Cập nhật thông tin.
- `GET /api/v1/students/{id}` : Lấy chi tiết hồ sơ.

### Nhóm API Điểm danh (STU-02)
- **`GET /api/v1/classes/{class_id}/members`** : Lấy danh sách học viên của lớp. 
  - *Lưu ý kiến trúc:* API này sẽ **gọi sang phân hệ Tuyển sinh (Admission)** để lấy dữ liệu từ bảng `admission.enrollments` (với điều kiện `status = 'APPROVED'`). Tuyệt đối không tạo bảng `class_members` riêng.
- `POST /api/v1/attendance` : Điểm danh học viên. 
  - *Logic:* Nếu gửi request trùng `class_id`, `student_id`, `session_date` (đã có trong DB), trả về lỗi `HTTP 400 Bad Request` kèm mã lỗi `ATTENDANCE_DUPLICATE`.

### Nhóm API Bảo lưu/Rút hồ sơ (STU-03)
- `POST /api/v1/status-requests` : Học viên gửi yêu cầu.
- `PUT /api/v1/status-requests/{id}/approve` : Staff duyệt yêu cầu.
  - *Logic:* Cập nhật trạng thái trong `status_requests` thành `APPROVED`, đồng thời UPDATE trạng thái bên bảng `students` thành `RESERVED` (nếu bảo lưu) hoặc `DROPPED` (nếu rút hồ sơ).