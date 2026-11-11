
# ĐẶC TẢ KỸ THUẬT (TECHNICAL SPECIFICATION) - PHÂN HỆ ĐÀO TẠO & KẾT QUẢ HỌC TẬP (`learning`)

## 1. Công nghệ & Kiến trúc sử dụng

- **Ngôn ngữ Backend:** Python 3.10+[cite: 14, 21]
- **Framework:** FastAPI (RESTful API, Pydantic v2 validation, OpenAPI/Swagger)[cite: 13, 21]
- **ORM:** SQLAlchemy 2.0 (Khai báo schema `learning`)[cite: 13, 21]
- **Database:** PostgreSQL 16 (Sử dụng độc lập Schema `learning`, tuân thủ `INV-SYS-02`)[cite: 14, 21]
- **Xác thực:** Stateless JWT (`HS256`) dùng biến môi trường `JWT_SECRET` dùng chung với Admission Service (`ADR 003`, `INV-SEC-03`)[cite: 5, 17, 21]
- **Giao tiếp liên Service:** HTTP REST qua `httpx`/`urllib` kèm header `X-Internal-Secret: <INTERNAL_SECRET_KEY>`, timeout 5 giây, retry tối đa 3 lần (`R-INT-1`, `R-INT-2`).

## 2. Thiết kế Cơ sở dữ liệu (Database Schema `learning`)

### 2.1. Bảng `learning.materials` (11 cột)[cite: 21]

- `id` (`SERIAL`, PK)[cite: 21]
- `class_id` (`INTEGER`, `NOT NULL`) — *Logical FK trỏ tới `admission.classes.id`*[cite: 21]
- `title` (`VARCHAR(255)`, `NOT NULL`)[cite: 21]
- `description` (`TEXT`, Nullable)[cite: 21]
- `material_type` (`VARCHAR(50)`, `NOT NULL`, `DEFAULT 'DOCUMENT'`, `CHECK IN ('DOCUMENT', 'VIDEO', 'EXERCISE', 'SLIDE')`)[cite: 21]
- `file_url` (`TEXT`, `NOT NULL`, `CHECK (btrim(file_url) <> '')` — `INV-LRN-04`)[cite: 21]
- `original_filename` (`VARCHAR(255)`, Nullable)[cite: 21]
- `file_size` (`BIGINT`, Nullable, `CHECK (file_size >= 0)`)[cite: 21]
- `uploaded_by` (`INTEGER`, Nullable) — *Logical FK trỏ tới `admission.users.id`*[cite: 21]
- `uploaded_at` (`TIMESTAMPTZ`, `NOT NULL`, `DEFAULT CURRENT_TIMESTAMP`)[cite: 21]
- `updated_at` (`TIMESTAMPTZ`, `NOT NULL`, `DEFAULT CURRENT_TIMESTAMP`, tự động cập nhật qua trigger `trg_materials_updated_at`)[cite: 21]

### 2.2. Bảng `learning.grades` (11 cột)[cite: 21]

- `id` (`SERIAL`, PK)[cite: 21]
- `class_id` (`INTEGER`, `NOT NULL`) — *Logical FK trỏ tới `admission.classes.id`*[cite: 21]
- `student_id` (`INTEGER`, `NOT NULL`) — *Logical FK trỏ tới `student.students.id` (Tuân thủ `R-ID-1`: KHÔNG lưu `users.id`)*[cite: 14, 21]
- `assessment_name` (`VARCHAR(255)`, `NOT NULL`)[cite: 21]
- `score` (`NUMERIC(5,2)`, `NOT NULL`, `CHECK (score >= 0 AND score <= max_score)` — `INV-LRN-01`)[cite: 21]
- `max_score` (`NUMERIC(5,2)`, `NOT NULL`, `DEFAULT 100.0`, `CHECK (max_score > 0)`)[cite: 21]
- `weight` (`NUMERIC(3,2)`, Nullable, `DEFAULT 1.0`, `CHECK (weight IS NULL OR (weight > 0 AND weight <= 1))`)[cite: 21]
- `feedback` (`TEXT`, Nullable)[cite: 21]
- `graded_by` (`INTEGER`, Nullable) — *Logical FK trỏ tới `admission.users.id`*[cite: 21]
- `graded_at` (`TIMESTAMPTZ`, `NOT NULL`, `DEFAULT CURRENT_TIMESTAMP`)[cite: 21]
- `updated_at` (`TIMESTAMPTZ`, `NOT NULL`, `DEFAULT CURRENT_TIMESTAMP`, tự động cập nhật qua trigger `trg_grades_updated_at`)[cite: 21]
- **Ràng buộc cấp bảng:** `UNIQUE (class_id, student_id, assessment_name)` (`INV-LRN-06`)[cite: 21].

### 2.3. Views Báo cáo Tích hợp[cite: 21]

1. **`learning.v_student_grade_summary`**: Tính `total_assessments`, `weighted_avg_score`, `average_percentage` (chuẩn hóa thang 100%) và `result` (`'Pass'` nếu `average_percentage >= 50`, ngược lại `'Fail'`) nhóm theo `(class_id, student_id)`[cite: 21].
2. **`learning.v_class_pass_rate`**: Tính `total_students`, `pass_count`, `fail_count`, `pass_rate_percentage` nhóm theo `class_id` từ `v_student_grade_summary`[cite: 21].

## 3. Thiết kế API Endpoints (`http://localhost:8080/api/learning/...`)

Tất cả API tuân thủ cấu trúc JSON `snake_case` (`INV-SYS-01`): `{ "success": true, "data": ..., "message": "..." }` hoặc `{ "success": false, "error": { "code": "LRN_...", "message": "..." } }`[cite: 12, 14, 21].

### 3.1. Nhóm API Học liệu (LRN-01)

- **`GET /api/learning/materials`**: Lấy danh sách học liệu (lọc theo `class_id`, `material_type`, `search`, hỗ trợ phân trang `page`, `size`)[cite: 12, 21].
- **`GET /api/learning/materials/{material_id}`**: Xem chi tiết 1 học liệu[cite: 12].
- **`POST /api/learning/materials`**: Tạo mới học liệu (`ADMIN`, `STAFF`, `TEACHER`). Kiểm tra `btrim(file_url) <> ''` (`INV-LRN-04`) và gọi Admission xác thực lớp/giáo viên[cite: 21].
- **`DELETE /api/learning/materials/{material_id}`**: Xóa học liệu (`ADMIN`, `STAFF`, `TEACHER` phụ trách lớp).

### 3.2. Nhóm API Bảng điểm (LRN-02)

- **`GET /api/learning/grades`**: Lấy danh sách điểm[cite: 18, 21].
  - *Logic bảo mật (`INV-LRN-02`):* Nếu JWT `role == 'STUDENT'`, hệ thống gọi `student-service` (`GET /api/student/internal/students?user_id={jwt.sub}`) để lấy `student.students.id` của chính học viên đó và chỉ trả về bảng điểm của `student_id` này. Mọi tham số `student_id` của người khác truyền vào đều bị chặn (`403 LRN_FORBIDDEN_STUDENT_GRADES`) hoặc ghi đè về chính mình[cite: 18, 21].
- **`POST /api/learning/grades`**: Nhập điểm mới cho học viên (`TEACHER`, `STAFF`, `ADMIN`)[cite: 18, 21].
  - *Logic kiểm tra:*
    1. Kiểm tra `0 <= score <= max_score` (`INV-LRN-01`), vi phạm trả `400 LRN_INVALID_SCORE`[cite: 18, 21].
    2. Nếu `role == 'TEACHER'`, gọi `GET http://admission-service:8081/api/admission/internal/classes/{class_id}` kiểm tra `teacher_id == jwt.sub` (`INV-LRN-03` / `INV-SEC-02`), nếu khác trả `403 LRN_FORBIDDEN_CLASS`[cite: 18, 21].
    3. Gọi `GET http://student-service:8082/api/student/internal/students/{student_id}` xác minh học viên tồn tại và đang `STUDYING`[cite: 18, 21].
    4. Nếu trùng `(class_id, student_id, assessment_name)` (`INV-LRN-06`), trả `409 LRN_DUPLICATE_ASSESSMENT`[cite: 18, 21].
- **`PUT /api/learning/grades/{grade_id}`**: Cập nhật điểm hoặc nhận xét của bài kiểm tra đã chấm (`TEACHER` dạy lớp, `STAFF`, `ADMIN`)[cite: 21].

### 3.3. Nhóm API Báo cáo Kết quả Học tập (LRN-03)

- **`GET /api/learning/reports/class-summary?class_id={id}`**: Trả về thống kê tỷ lệ Đạt/Trượt của lớp từ `v_class_pass_rate` kèm danh sách điểm tổng kết từng học viên từ `v_student_grade_summary` (`ADMIN`, `STAFF`, `TEACHER`)[cite: 21].
- **`GET /api/learning/reports/student-summary?class_id={id}&student_id={sid}`**: Trả về kết quả tổng kết chuẩn hóa thang 100% (`weighted_avg_score`, `average_percentage`, `result`: `Pass`/`Fail`) của học viên[cite: 21].

## 4. Danh mục Mã lỗi Chuẩn (`LRN_*`)

| Mã lỗi                         | HTTP Status | Mô tả                                                                                                            |
| :------------------------------- | :---------: | :----------------------------------------------------------------------------------------------------------------- |
| `LRN_INVALID_SCORE`            |     400     | Điểm số `< 0` hoặc `> max_score`, hoặc `max_score <= 0` (`INV-LRN-01`)[cite: 18, 21].                 |
| `LRN_INVALID_WEIGHT`           |     400     | Trọng số nằm ngoài khoảng `(0, 1]`[cite: 21].                                                               |
| `LRN_EMPTY_FILE_URL`           |     400     | Đường dẫn tài liệu `file_url` bị rỗng (`INV-LRN-04`)[cite: 21].                                        |
| `LRN_FORBIDDEN_CLASS`          |     403     | Giáo viên không được phân công phụ trách lớp học này (`INV-LRN-03` / `INV-SEC-02`)[cite: 18, 21]. |
| `LRN_FORBIDDEN_STUDENT_GRADES` |     403     | Học viên cố tình truy cập bảng điểm của học viên khác (`INV-LRN-02`)[cite: 17, 21].                  |
| `LRN_MATERIAL_NOT_FOUND`       |     404     | Không tìm thấy học liệu theo ID[cite: 12, 18].                                                                |
| `LRN_GRADE_NOT_FOUND`          |     404     | Không tìm thấy bản ghi điểm theo ID.                                                                         |
| `LRN_CLASS_NOT_FOUND`          |     404     | Lớp học không tồn tại bên Admission Service[cite: 18, 21].                                                   |
| `LRN_STUDENT_NOT_FOUND`        |     404     | Học viên không tồn tại hoặc không ở trạng thái hợp lệ bên Student Service[cite: 18, 21].              |
| `LRN_DUPLICATE_ASSESSMENT`     |     409     | Đầu điểm `assessment_name` đã tồn tại cho học viên trong lớp này (`INV-LRN-06`)[cite: 21].         |
