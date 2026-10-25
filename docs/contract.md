# Hợp đồng Kỹ thuật & Ràng buộc Hệ thống (Technical Contract)

> **Trạng thái**: APPROVED (Nền tảng đã thống nhất) & PROPOSED (Các quy tắc mở rộng)

---

## 1. Domain Model (Dữ liệu nghiệp vụ)

Hệ thống phân tách dữ liệu thành 4 Schema độc lập trên PostgreSQL:

### A. Schema `admission` (Tuyển sinh & Quản trị tài khoản)
- **`users`**:
  - `id` (SERIAL PRIMARY KEY)
  - `email` (VARCHAR(255) UNIQUE NOT NULL)
  - `password_hash` (VARCHAR(255) NOT NULL)
  - `full_name` (VARCHAR(255) NOT NULL)
  - `role` (VARCHAR(50) NOT NULL: `ADMIN`, `STAFF`, `TEACHER`, `STUDENT`)
  - `phone` (VARCHAR(50))
  - `is_active` (BOOLEAN DEFAULT TRUE)
  - `created_at` (TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP)
- **`courses`**:
  - `id` (SERIAL PRIMARY KEY)
  - `code` (VARCHAR(50) UNIQUE NOT NULL)
  - `name` (VARCHAR(255) NOT NULL)
  - `description` (TEXT)
  - `level` (VARCHAR(50): `Beginner`, `Intermediate`, `Advanced`)
  - `duration_weeks` (INTEGER DEFAULT 12)
  - `tuition_amount` (NUMERIC(12,2) DEFAULT 0)
  - `is_active` (BOOLEAN DEFAULT TRUE)
  - `created_at` (TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP)
- **`classes`**:
  - `id` (SERIAL PRIMARY KEY)
  - `course_id` (INTEGER REFERENCES admission.courses(id) ON DELETE CASCADE)
  - `code` (VARCHAR(50) UNIQUE NOT NULL)
  - `name` (VARCHAR(255) NOT NULL)
  - `room` (VARCHAR(100))
  - `teacher_name` (VARCHAR(255))
  - `schedule` (VARCHAR(255))
  - `start_date` (DATE), `end_date` (DATE)
  - `max_students` (INTEGER DEFAULT 25 CHECK (max_students > 0))
  - `status` (VARCHAR(50) DEFAULT 'OPEN': `OPEN`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`)
  - `created_at` (TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP)
- **`enrollments`**:
  - `id` (SERIAL PRIMARY KEY)
  - `student_id` (INTEGER NOT NULL) -- Tham chiếu logic sang `student.students(id)`
  - `class_id` (INTEGER REFERENCES admission.classes(id) ON DELETE CASCADE)
  - `status` (VARCHAR(50) DEFAULT 'PENDING': `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`)
  - `enrolled_at` (TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP)
  - *Ràng buộc logic*: Không được vượt quá `max_students` của lớp khi chuyển sang `APPROVED`.

### B. Schema `student` (Hồ sơ học viên & Điểm danh)
- **`students`**:
  - `id` (SERIAL PRIMARY KEY)
  - `user_id` (INTEGER) -- Liên kết tùy chọn tới `admission.users(id)`
  - `full_name` (VARCHAR(255) NOT NULL)
  - `email` (VARCHAR(255) NOT NULL)
  - `phone` (VARCHAR(50))
  - `date_of_birth` (DATE)
  - `address` (TEXT)
  - `status` (VARCHAR(50) DEFAULT 'STUDYING': `STUDYING`, `RESERVED`, `DROPPED`, `GRADUATED`)
  - `created_at` (TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP)
- **`attendance`**:
  - `id` (SERIAL PRIMARY KEY)
  - `class_id` (INTEGER NOT NULL)
  - `student_id` (INTEGER REFERENCES student.students(id) ON DELETE CASCADE)
  - `session_date` (DATE NOT NULL)
  - `status` (VARCHAR(50) DEFAULT 'PRESENT': `PRESENT`, `ABSENT`, `LATE`, `EXCUSED`)
  - `notes` (TEXT)
  - `created_at` (TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP)
  - *Ràng buộc logic*: Mỗi học viên chỉ có 1 bản ghi điểm danh trên 1 buổi học của lớp (`UNIQUE(class_id, student_id, session_date)`).

### C. Schema `learning` (Học liệu & Đánh giá kết quả)
- **`materials`**:
  - `id` (SERIAL PRIMARY KEY)
  - `class_id` (INTEGER NOT NULL)
  - `title` (VARCHAR(255) NOT NULL)
  - `description` (TEXT)
  - `material_type` (VARCHAR(50) DEFAULT 'DOCUMENT': `DOCUMENT`, `VIDEO`, `EXERCISE`, `SLIDE`)
  - `file_url` (TEXT)
  - `uploaded_at` (TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP)
- **`grades`**:
  - `id` (SERIAL PRIMARY KEY)
  - `class_id` (INTEGER NOT NULL)
  - `student_id` (INTEGER NOT NULL)
  - `assessment_name` (VARCHAR(255) NOT NULL)
  - `score` (NUMERIC(5,2) NOT NULL CHECK (score >= 0))
  - `max_score` (NUMERIC(5,2) DEFAULT 100.0 CHECK (max_score > 0))
  - `feedback` (TEXT)
  - `graded_at` (TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP)
  - *Ràng buộc logic*: `score <= max_score`.

### D. Schema `finance` (Học phí, Hóa đơn & Thanh toán)
- **`tuition_fees`**:
  - `id` (SERIAL PRIMARY KEY)
  - `course_id` (INTEGER NOT NULL)
  - `amount` (NUMERIC(12,2) NOT NULL CHECK (amount >= 0))
  - `currency` (VARCHAR(10) DEFAULT 'VND')
  - `note` (TEXT)
  - `created_at` (TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP)
- **`invoices`**:
  - `id` (SERIAL PRIMARY KEY)
  - `invoice_code` (VARCHAR(50) UNIQUE NOT NULL)
  - `student_id` (INTEGER NOT NULL)
  - `enrollment_id` (INTEGER)
  - `amount` (NUMERIC(12,2) NOT NULL CHECK (amount >= 0))
  - `status` (VARCHAR(50) DEFAULT 'UNPAID': `UNPAID`, `PAID`, `CANCELLED`, `OVERDUE`)
  - `due_date` (DATE)
  - `created_at` (TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP)
- **`payments`**:
  - `id` (SERIAL PRIMARY KEY)
  - `invoice_id` (INTEGER REFERENCES finance.invoices(id) ON DELETE CASCADE)
  - `amount` (NUMERIC(12,2) NOT NULL CHECK (amount > 0))
  - `payment_method` (VARCHAR(50) DEFAULT 'BANK_TRANSFER': `CASH`, `BANK_TRANSFER`, `CARD`, `MOMO`)
  - `transaction_ref` (VARCHAR(100) UNIQUE)
  - `paid_at` (TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP)

---

## 2. State Machines (Vòng đời đối tượng)

### A. Vòng đời Đăng ký (`admission.enrollments`)
```
[PENDING] --(Duyệt hồ sơ & Lớp còn chỗ)--> [APPROVED]
[PENDING] --(Từ chối / Hết chỗ)---------> [REJECTED]
[PENDING / APPROVED] --(Học viên rút đơn)--> [CANCELLED]
```
- **Quy tắc**: Không được chuyển từ `PENDING` sang `APPROVED` nếu sĩ số lớp đã đạt `max_students`.

### B. Vòng đời Hóa đơn (`finance.invoices`)
```
[UNPAID] --(Thanh toán đủ tổng tiền)------> [PAID]
[UNPAID] --(Quá hạn thanh toán)----------> [OVERDUE]
[OVERDUE] --(Thanh toán hoàn tất)---------> [PAID]
[UNPAID / OVERDUE] --(Hủy khóa / Đổi đơn)-> [CANCELLED]
```
- **Quy tắc**: Chỉ chuyển sang `PAID` khi có giao dịch thanh toán hợp lệ với `sum(payments.amount) >= invoices.amount`.

---

## 3. API Contract & Conventions

- **Cấu trúc JSON chung**:
  - Thành công: `{ "success": true, "data": ..., "message": "..." }`
  - Lỗi: `{ "success": false, "error": { "code": "ERR_CODE", "message": "..." } }`
- **Naming style**: `snake_case` cho toàn bộ JSON fields và query parameters.
- **Header xác thực**: `Authorization: Bearer <jwt_token>` trên tất cả API yêu cầu bảo mật.
- **Tiền tố mã lỗi**:
  - Auth: `AUTH_`
  - Admission: `ADM_`
  - Student: `STU_`
  - Learning: `LRN_`
  - Finance: `FIN_`

---

## 4. Phân quyền & Ranh giới Bảo mật (Authorization)
- `ADMIN`: Toàn quyền hệ thống.
- `STAFF`: Quản lý danh mục khóa, lớp, tiếp nhận hồ sơ, lập hóa đơn.
- `TEACHER`: Chỉ được xem danh sách lớp mình dạy, điểm danh lớp mình dạy, nhập điểm bài học mình phụ trách. Không được sửa học phí, không được xem doanh thu tài chính.
- `STUDENT`: Chỉ được xem thông tin cá nhân của chính mình, điểm thi của mình, tài liệu lớp mình đang học, và hóa đơn thanh toán của bản thân. Tuyệt đối không xem được hồ sơ/điểm của học viên khác.
