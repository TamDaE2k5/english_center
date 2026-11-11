
Dưới đây là trọn bộ **3 tài liệu đặc tả chính thức của Phân hệ Learning (`learning-ba-bi-spec.md`, `learning-tech-spec.md`, `learning-uml-spec.md`)** được thiết kế đồng bộ hoàn toàn với cấu trúc của phân hệ Student, bám sát `learning-schema-spec.md` và `MASTER_DB_AND_API_RULES.md`, kèm theo **toàn bộ mã nguồn Full-Stack (Backend FastAPI + Frontend HTML/JS)** cho `services/learning/`.

---

# PHẦN I: 3 FILE ĐẶC TẢ PHÂN HỆ LEARNING (`services/learning/docs/`)

## 1. File 1: `services/learning/docs/learning-ba-bi-spec.md`

```markdown
# ĐẶC TẢ NGHIỆP VỤ & BÁO CÁO (BA & BI SPECIFICATION) - PHÂN HỆ ĐÀO TẠO & KẾT QUẢ HỌC TẬP (`learning`)

## PHẦN 1: BỐI CẢNH & ĐẶT VẤN ĐỀ (OVERVIEW & PROBLEM STATEMENT)
Tại các trung tâm ngoại ngữ, công tác quản lý học liệu và đánh giá kết quả học tập thường rời rạc: giảng viên gửi tài liệu qua nhóm Zalo hoặc phát bản in rời từng buổi, còn điểm số được nhập thủ công trên từng file Excel riêng lẻ của giáo viên[cite: 1].
**Hệ quả:** 65% học viên thất lạc tài liệu hoặc vào lớp muộn không tìm lại được bài giảng cũ; 40% học viên hiếm khi nhận được nhận xét chi tiết; việc nhập điểm thủ công dễ nhầm thang điểm (thang 10 vs thang 100) và trung tâm thiếu báo cáo tổng hợp tỷ lệ Đạt/Trượt định kỳ theo thời gian thực[cite: 1, 21].

## PHẦN 2: MINH CHỨNG KHẢO SÁT THỰC TẾ
*(Số liệu tổng hợp từ khảo sát Chương 1)*[cite: 1]
- **65%** học viên nhận tài liệu qua bản in rời hoặc link Zalo trôi nổi, dễ thất lạc[cite: 1].
- **40%** học viên hiếm khi nhận được nhận xét chi tiết đi kèm điểm số từng bài kiểm tra[cite: 1].
- **65%** học viên và phụ huynh mong muốn có báo cáo học lực định kỳ tự động nhưng trung tâm không đủ nhân lực tổng hợp thủ công[cite: 1].

## PHẦN 3: BẢNG ÁNH XẠ KHÓ KHĂN & TÍNH NĂNG GIẢI QUYẾT
| Khó khăn thực tế | Biểu hiện & Hậu quả | Tính năng hệ thống đảm nhiệm |
| :--- | :--- | :--- |
| Học liệu trôi nổi, phân tán trên Zalo/giấy | Dễ thất lạc, học viên vào sau không tìm lại được tài liệu các buổi trước[cite: 1]. | **LRN-01: Kho học liệu số tập trung theo lớp**<br>- Lưu trữ và phân loại chuẩn (`DOCUMENT`, `VIDEO`, `EXERCISE`, `SLIDE`) gắn với từng `class_id`, kiểm tra đường dẫn hợp lệ (`INV-LRN-04`)[cite: 1, 21]. |
| Nhập điểm Excel thủ công, thiếu nhận xét & lệch thang điểm | Dễ gõ âm hoặc vượt thang tối đa, giáo viên chấm nhầm lớp khác, học viên xem lẫn điểm của nhau[cite: 1, 17, 20]. | **LRN-02: Số hóa Sổ điểm & Đánh giá trọng số**<br>- Chấm điểm theo đầu điểm (`assessment_name`) có trọng số (`weight`) và lời phê (`feedback`); chặn `0 <= score <= max_score` (`INV-LRN-01`), kiểm tra đúng giáo viên dạy lớp (`INV-LRN-03`) và cô lập điểm học viên (`INV-LRN-02`)[cite: 1, 21]. |
| Không có báo cáo học lực & tỷ lệ Đạt/Trượt định kỳ | Tốn thời gian tổng hợp thủ công, sai lệch khi lớp có nhiều thang điểm (10 và 100)[cite: 1, 21]. | **LRN-03: Báo cáo học lực & BI tỷ lệ Đạt/Trượt tự động**<br>- Tự động chuẩn hóa về thang 100% qua 2 views `v_student_grade_summary` và `v_class_pass_rate`, xét ngưỡng Đạt `average_percentage >= 50%` (`INV-LRN-05`)[cite: 1, 21]. |

## PHẦN 4: MỤC TIÊU CỐT LÕI (PROJECT OBJECTIVES)
1. **Tập trung hóa kho học liệu số:** Quản lý tài liệu, slide, video, bài tập theo từng lớp học với kiểm soát quyền truy cập chặt chẽ[cite: 20, 21].
2. **Chuẩn hóa & Bảo mật sổ điểm:** Đảm bảo tính toàn vẹn điểm số (`INV-LRN-01`, `INV-LRN-06`), phân quyền giảng viên theo lớp phụ trách (`INV-LRN-03` / `INV-SEC-02`) và bảo mật điểm cá nhân học viên (`INV-LRN-02`)[cite: 17, 21].
3. **Báo cáo BI Đào tạo thời gian thực:** Cung cấp bảng tổng kết điểm chuẩn hóa thang 100% và thống kê tỷ lệ Pass/Fail tự động cho từng lớp học[cite: 21].

## PHẦN 5: DANH SÁCH TÍNH NĂNG CHI TIẾT (FUNCTIONAL & BI REQUIREMENTS)

### LRN-01: Quản lý Kho Học liệu Số (Digital Materials Management)
- **Tác nhân:** Quản trị viên (`ADMIN`), Giáo vụ (`STAFF`), Giảng viên (`TEACHER`), Học viên (`STUDENT`)[cite: 20, 21].
- **Mô tả:**
  - Giảng viên / Giáo vụ đăng tải học liệu mới cho lớp học: tiêu đề (`title`), mô tả (`description`), phân loại (`DOCUMENT`, `VIDEO`, `EXERCISE`, `SLIDE`), đường dẫn (`file_url`), tên file gốc (`original_filename`) và dung lượng (`file_size`)[cite: 21].
  - Học viên và Giảng viên tra cứu, lọc danh sách học liệu theo `class_id` và `material_type`[cite: 21].
  - Xóa học liệu khi cập nhật phiên bản mới (`ADMIN`, `STAFF`, `TEACHER`)[cite: 18, 21].
- **Ràng buộc:**
  - `file_url` bắt buộc khác rỗng sau khi cắt khoảng trắng (`INV-LRN-04`)[cite: 21].
  - `file_size >= 0` (nếu có)[cite: 21].
  - Trước khi thêm học liệu, hệ thống gọi sang Admission Service để xác minh lớp học tồn tại và nếu người tải lên là `TEACHER` thì phải đúng là giáo viên phụ trách lớp đó[cite: 18, 21].

### LRN-02: Quản lý Bảng điểm & Đánh giá Trọng số (Weighted Gradebook)
- **Tác nhân:** Giảng viên (`TEACHER`), Giáo vụ (`STAFF`), Quản trị viên (`ADMIN`), Học viên (`STUDENT`)[cite: 20, 21].
- **Mô tả:**
  - Giảng viên / Giáo vụ nhập hoặc cập nhật điểm cho học viên theo từng đầu điểm (`Quiz 1`, `Midterm`, `Final`...), kèm thang điểm tối đa (`max_score`, mặc định `100.0`), trọng số (`weight` trong `(0, 1]`, mặc định `1.0`) và nhận xét (`feedback`)[cite: 21].
  - Học viên xem bảng điểm cá nhân và nhận xét của giáo viên[cite: 20, 21].
- **Ràng buộc:**
  - **Giới hạn điểm (`INV-LRN-01`):** `0.0 <= score <= max_score` và `max_score > 0`[cite: 21].
  - **Đơn nhất đầu điểm (`INV-LRN-06`):** Mỗi bộ `(class_id, student_id, assessment_name)` chỉ có duy nhất 1 bản ghi điểm[cite: 21].
  - **Ranh giới giảng viên (`INV-LRN-03` / `INV-SEC-02`):** Nếu người chấm có role `TEACHER`, hệ thống bắt buộc gọi `GET /api/admission/internal/classes/{class_id}` để đối chiếu `teacher_id == current_user.user_id`. Nếu không khớp, trả lỗi `403 LRN_FORBIDDEN_CLASS`[cite: 18, 21].
  - **Cô lập điểm học viên (`INV-LRN-02`):** Học viên (`role = STUDENT`) chỉ xem được điểm của chính mình; hệ thống tự ánh xạ `user_id` trong JWT sang `student.students.id` (`R-ID-1`), bỏ qua mọi `student_id` truyền từ query param[cite: 14, 21].

### LRN-03: Báo cáo Kết quả Học tập & BI Tỷ lệ Đạt/Trượt (Academic BI & Reporting)
- **Tác nhân:** Quản trị viên (`ADMIN`), Giáo vụ (`STAFF`), Giảng viên (`TEACHER`), Học viên (`STUDENT` xem kết quả tổng kết của mình)[cite: 21].
- **Mô tả:**
  - Tự động tính điểm trung bình có trọng số (`weighted_avg_score`) và điểm phần trăm chuẩn hóa thang 100 (`average_percentage`) cho từng học viên trong lớp qua view `learning.v_student_grade_summary`[cite: 21].
  - Tự động phân loại kết quả `Pass` (khi `average_percentage >= 50.0`) hoặc `Fail` (`< 50.0`) theo `INV-LRN-05`[cite: 21].
  - Thống kê tổng số học viên đã có điểm (`total_students`), số lượng Đạt (`pass_count`), số lượng Trượt (`fail_count`) và tỷ lệ đỗ (`pass_rate_percentage`) của từng lớp qua view `learning.v_class_pass_rate`[cite: 21].

```

---

## 2. File 2: `services/learning/docs/learning-tech-spec.md`

```markdown
# ĐẶC TẢ KỸ THUẬT (TECHNICAL SPECIFICATION) - PHÂN HỆ ĐÀO TẠO & KẾT QUẢ HỌC TẬP (`learning`)

## 1. Công nghệ & Kiến trúc sử dụng
- **Ngôn ngữ Backend:** Python 3.10+[cite: 14, 21]
- **Framework:** FastAPI (RESTful API, Pydantic v2 validation, OpenAPI/Swagger)[cite: 13, 21]
- **ORM:** SQLAlchemy 2.0 (Khai báo schema `learning`)[cite: 13, 21]
- **Database:** PostgreSQL 16 (Sử dụng độc lập Schema `learning`, tuân thủ `INV-SYS-02`)[cite: 14, 21]
- **Xác thực:** Stateless JWT (`HS256`) dùng biến môi trường `JWT_SECRET` dùng chung với Admission Service (`ADR 003`, `INV-SEC-03`)[cite: 5, 17, 21]
- **Giao tiếp liên Service:** HTTP REST qua `httpx`/`urllib` kèm header `X-Internal-Secret: <INTERNAL_SECRET_KEY>`, timeout 5 giây, retry tối đa 3 lần (`R-INT-1`, `R-INT-2`)[cite: 18, 21].

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
- **`DELETE /api/learning/materials/{material_id}`**: Xóa học liệu (`ADMIN`, `STAFF`, `TEACHER` phụ trách lớp)[cite: 18, 21].

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
| Mã lỗi | HTTP Status | Mô tả |
| :--- | :---: | :--- |
| `LRN_INVALID_SCORE` | 400 | Điểm số `< 0` hoặc `> max_score`, hoặc `max_score <= 0` (`INV-LRN-01`)[cite: 18, 21]. |
| `LRN_INVALID_WEIGHT` | 400 | Trọng số nằm ngoài khoảng `(0, 1]`[cite: 21]. |
| `LRN_EMPTY_FILE_URL` | 400 | Đường dẫn tài liệu `file_url` bị rỗng (`INV-LRN-04`)[cite: 21]. |
| `LRN_FORBIDDEN_CLASS` | 403 | Giáo viên không được phân công phụ trách lớp học này (`INV-LRN-03` / `INV-SEC-02`)[cite: 18, 21]. |
| `LRN_FORBIDDEN_STUDENT_GRADES` | 403 | Học viên cố tình truy cập bảng điểm của học viên khác (`INV-LRN-02`)[cite: 17, 21]. |
| `LRN_MATERIAL_NOT_FOUND` | 404 | Không tìm thấy học liệu theo ID[cite: 12, 18]. |
| `LRN_GRADE_NOT_FOUND` | 404 | Không tìm thấy bản ghi điểm theo ID. |
| `LRN_CLASS_NOT_FOUND` | 404 | Lớp học không tồn tại bên Admission Service[cite: 18, 21]. |
| `LRN_STUDENT_NOT_FOUND` | 404 | Học viên không tồn tại hoặc không ở trạng thái hợp lệ bên Student Service[cite: 18, 21]. |
| `LRN_DUPLICATE_ASSESSMENT` | 409 | Đầu điểm `assessment_name` đã tồn tại cho học viên trong lớp này (`INV-LRN-06`)[cite: 21]. |

```

---

## 3. File 3: `services/learning/docs/learning-uml-spec.md`

```markdown
# ĐẶC TẢ SƠ ĐỒ UML - PHÂN HỆ ĐÀO TẠO & KẾT QUẢ HỌC TẬP (`learning`)

Tài liệu này mô tả trực quan các tác nhân, luồng tương tác liên service (Microservices), quy trình kiểm tra bất biến và cấu trúc dữ liệu của Phân hệ Learning[cite: 18, 21].

## PHẦN 1: SƠ ĐỒ USE CASE (USE CASE DIAGRAM)

```mermaid
flowchart LR
    Admin([Quản trị viên - ADMIN])
    Staff([Giáo vụ - STAFF])
    Teacher([Giảng viên - TEACHER])
    Student([Học viên - STUDENT])

    subgraph Boundary_Learning_Service ["Phân hệ Đào tạo & Kết quả Học tập (learning-service :8083)"]
        UC_LRN_01_1("LRN-01.1: Đăng tải & Xóa học liệu số")
        UC_LRN_01_2("LRN-01.2: Tra cứu & Tải học liệu theo lớp")
        UC_LRN_02_1("LRN-02.1: Nhập & Cập nhật điểm trọng số")
        UC_LRN_02_2("LRN-02.2: Xem bảng điểm & Nhận xét cá nhân")
        UC_LRN_03_1("LRN-03.1: Xem báo cáo tổng kết & Tỷ lệ Pass/Fail lớp")
    end

    Student --> UC_LRN_01_2
    Student --> UC_LRN_02_2

    Teacher --> UC_LRN_01_1
    Teacher --> UC_LRN_01_2
    Teacher --> UC_LRN_02_1
    Teacher --> UC_LRN_03_1

    Staff --> UC_LRN_01_1
    Staff --> UC_LRN_01_2
    Staff --> UC_LRN_02_1
    Staff --> UC_LRN_03_1

    Admin --> UC_LRN_01_1
    Admin --> UC_LRN_01_2
    Admin --> UC_LRN_02_1
    Admin --> UC_LRN_03_1

```

## PHẦN 2: ĐẶC TẢ USE CASE CHI TIẾT

### UC-LRN-01.1: Đăng tải & Xóa học liệu số

* **Actor:** Teacher, Staff, Admin.
* **Luồng chính:** Chọn lớp (`class_id`), nhập tiêu đề, chọn loại (`DOCUMENT`, `VIDEO`, `EXERCISE`, `SLIDE`), nhập `file_url` -> Hệ thống kiểm tra `file_url` khác rỗng (`INV-LRN-04`) -> Xác thực lớp qua Admission Service -> Lưu vào bảng `learning.materials`.
* **Ngoại lệ:** `file_url` rỗng -> Trả lỗi `400 LRN_EMPTY_FILE_URL`; Giáo viên upload vào lớp không dạy -> `403 LRN_FORBIDDEN_CLASS`.

### UC-LRN-02.1: Nhập & Cập nhật điểm trọng số

* **Actor:** Teacher, Staff, Admin.
* **Luồng chính:** Chọn lớp, chọn học viên (`student_id`), nhập tên bài kiểm tra (`assessment_name`), `score`, `max_score`, `weight`, `feedback` -> Kiểm tra `0 <= score <= max_score` (`INV-LRN-01`) -> Gọi Admission kiểm tra `teacher_id` (`INV-LRN-03`) -> Gọi Student kiểm tra học viên đang `STUDYING` -> Lưu vào `learning.grades`.
* **Ngoại lệ:** Điểm âm hoặc vượt `max_score` -> `400 LRN_INVALID_SCORE`; Trùng đầu điểm -> `409 LRN_DUPLICATE_ASSESSMENT`; Sai giáo viên -> `403 LRN_FORBIDDEN_CLASS`.

### UC-LRN-02.2: Xem bảng điểm & Nhận xét cá nhân

* **Actor:** Student.
* **Luồng chính:** Học viên mở trang Bảng điểm -> Hệ thống trích xuất `user_id` từ JWT -> Ánh xạ sang `student.students.id` (`R-ID-1`) -> Trả về danh sách điểm và kết quả tổng kết từ `v_student_grade_summary` của riêng học viên đó (`INV-LRN-02`).

### UC-LRN-03.1: Xem báo cáo tổng kết & Tỷ lệ Pass/Fail lớp

* **Actor:** Teacher, Staff, Admin.
* **Luồng chính:** Chọn `class_id` -> Hệ thống truy vấn `learning.v_class_pass_rate` và `learning.v_student_grade_summary` -> Trả về sĩ số có điểm, số lượng Pass/Fail, tỷ lệ đỗ (%) và bảng xếp hạng điểm chuẩn hóa thang 100%.

## PHẦN 3: SƠ ĐỒ TUẦN TỰ (SEQUENCE DIAGRAMS)

### 3.1. Luồng Giảng viên Nhập điểm & Kiểm tra Liên Service (`INV-LRN-01`, `INV-LRN-03`, `INV-LRN-06`)

```mermaid
sequenceDiagram
    autonumber
    actor Teacher as Giảng viên
    participant UI as Frontend
    participant LrnSvc as Learning Service (:8083)
    participant AdmSvc as Admission Service (:8081)
    participant StuSvc as Student Service (:8082)
    participant LrnDB as PostgreSQL (Schema: learning)

    Teacher->>UI: Nhập điểm (class_id, student_id, assessment_name, score, max_score, weight)
    UI->>LrnSvc: POST /api/learning/grades (Bearer JWT)
    LrnSvc->>LrnSvc: Kiểm tra INV-LRN-01 (0 <= score <= max_score) & 0 < weight <= 1

    alt Điểm không hợp lệ
        LrnSvc-->>UI: 400 Bad Request (LRN_INVALID_SCORE)
    else Điểm hợp lệ
        LrnSvc->>AdmSvc: GET /api/admission/internal/classes/{class_id} (X-Internal-Secret)
        AdmSvc-->>LrnSvc: 200 OK {id, teacher_id, status}

        alt role == TEACHER và teacher_id != jwt.user_id (Vi phạm INV-LRN-03 / INV-SEC-02)
            LrnSvc-->>UI: 403 Forbidden (LRN_FORBIDDEN_CLASS)
        else Đúng giáo viên phụ trách (hoặc STAFF/ADMIN)
            LrnSvc->>StuSvc: GET /api/student/internal/students/{student_id} (X-Internal-Secret)
            StuSvc-->>LrnSvc: 200 OK {id, student_code, status: "STUDYING"}
            LrnSvc->>LrnDB: INSERT INTO learning.grades (...)
            alt Vi phạm UNIQUE(class_id, student_id, assessment_name) (INV-LRN-06)
                LrnDB-->>LrnSvc: UniqueViolation
                LrnSvc-->>UI: 409 Conflict (LRN_DUPLICATE_ASSESSMENT)
            else Lưu thành công
                LrnDB-->>LrnSvc: Grade Record Created
                LrnSvc-->>UI: 201 Created {success: true, data: grade}
            end
        end
    end

```

### 3.2. Luồng Học viên Xem Bảng điểm Cô lập (`INV-LRN-02` & Cầu nối `R-ID-1`)

```mermaid
sequenceDiagram
    autonumber
    actor Student as Học viên
    participant UI as Frontend
    participant LrnSvc as Learning Service (:8083)
    participant StuSvc as Student Service (:8082)
    participant LrnDB as PostgreSQL (Schema: learning)

    Student->>UI: Mở trang Bảng điểm & Kết quả học tập
    UI->>LrnSvc: GET /api/learning/grades?class_id=1 (Bearer JWT)
    LrnSvc->>LrnSvc: Giải mã JWT -> role = STUDENT, user_id = sub
    Note over LrnSvc,StuSvc: Cầu nối R-ID-1: đổi admission.users.id -> student.students.id
    LrnSvc->>StuSvc: GET /api/student/internal/students?user_id={jwt.user_id} (X-Internal-Secret)
    StuSvc-->>LrnSvc: 200 OK {student_id: 1, student_code: "HV-2026-0001"}
    LrnSvc->>LrnDB: SELECT * FROM learning.grades WHERE student_id = 1 AND class_id = 1
    LrnDB-->>LrnSvc: Danh sách điểm của riêng student_id = 1
    LrnSvc-->>UI: 200 OK (Chỉ trả về điểm của chính học viên đó - INV-LRN-02)

```

## PHẦN 4: SƠ ĐỒ HOẠT ĐỘNG (ACTIVITY DIAGRAMS)

### 4.1. Luồng Chấm điểm & Tính Kết quả Tổng kết Chuẩn hóa Thang 100%

```mermaid
flowchart TD
    Start([Bắt đầu]) --> InputGrade[Giảng viên gửi request chấm điểm]
    InputGrade --> ValidateScore{0 <= score <= max_score\n& 0 < weight <= 1?}
    ValidateScore -- Sai --> ErrScore[Trả lỗi 400 LRN_INVALID_SCORE] --> End([Kết thúc])
    ValidateScore -- Đúng --> CheckRole{User role == TEACHER?}
    CheckRole -- Có --> CallAdm[Gọi Admission lấy thông tin lớp]
    CallAdm --> MatchTeacher{class.teacher_id == jwt.sub?}
    MatchTeacher -- Không --> ErrForbidden[Trả lỗi 403 LRN_FORBIDDEN_CLASS] --> End
    MatchTeacher -- Có --> CallStu[Gọi Student xác minh student_id]
    CheckRole -- STAFF / ADMIN --> CallStu
    CallStu --> CheckStu{Học viên tồn tại & STUDYING?}
    CheckStu -- Không --> ErrStu[Trả lỗi 404 LRN_STUDENT_NOT_FOUND] --> End
    CheckStu -- Có --> InsertDB[Lưu vào bảng learning.grades]
    InsertDB --> UpdateView[View v_student_grade_summary tự động tính lại:\naverage_percentage = SUM((score/max*100)*weight)/SUM(weight)]
    UpdateView --> EvalPass{average_percentage >= 50?}
    EvalPass -- Có --> MarkPass[Kết quả: Pass] --> End
    EvalPass -- Không --> MarkFail[Kết quả: Fail] --> End

```

## PHẦN 5: LƯỢC ĐỒ CƠ SỞ DỮ LIỆU (ERD & VIEWS SCHEMA `learning`)

```mermaid
erDiagram
    MATERIALS {
        int id PK
        int class_id "Logical FK -> admission.classes.id"
        varchar title
        text description
        varchar material_type "DOCUMENT | VIDEO | EXERCISE | SLIDE"
        text file_url "CHECK btrim <> '' (INV-LRN-04)"
        varchar original_filename
        bigint file_size "CHECK >= 0"
        int uploaded_by "Logical FK -> admission.users.id"
        timestamptz uploaded_at
        timestamptz updated_at
    }

    GRADES {
        int id PK
        int class_id UK "Logical FK -> admission.classes.id"
        int student_id UK "Logical FK -> student.students.id (R-ID-1)"
        varchar assessment_name UK "UNIQUE(class_id, student_id, assessment_name)"
        numeric score "0 <= score <= max_score (INV-LRN-01)"
        numeric max_score "DEFAULT 100.0, > 0"
        numeric weight "DEFAULT 1.0, (0, 1]"
        text feedback
        int graded_by "Logical FK -> admission.users.id (INV-LRN-03)"
        timestamptz graded_at
        timestamptz updated_at
    }

    V_STUDENT_GRADE_SUMMARY {
        int class_id
        int student_id
        bigint total_assessments
        numeric weighted_avg_score
        numeric average_percentage
        text result "Pass (>=50) | Fail (<50)"
    }

    V_CLASS_PASS_RATE {
        int class_id
        bigint total_students
        bigint pass_count
        bigint fail_count
        numeric pass_rate_percentage
    }

    GRADES ||--o| V_STUDENT_GRADE_SUMMARY : "tổng hợp trọng số"
    V_STUDENT_GRADE_SUMMARY ||--o| V_CLASS_PASS_RATE : "thống kê theo lớp"

```

```

---

# PHẦN II: CODE FULL-STACK PHÂN HỆ LEARNING (`services/learning/`)

Cấu trúc thư mục triển khai tuân thủ đúng cây dự án trong `README_2.md`[cite: 19]:
```text
services/learning/
├── Dockerfile
├── backend/
│   ├── requirements.txt
│   └── app/
│       ├── __init__.py
│       ├── database.py
│       ├── models.py
│       ├── schemas.py
│       ├── security.py
│       ├── internal_client.py
│       ├── main.py
│       └── routers/
│           ├── __init__.py
│           ├── materials.py
│           └── grades.py
└── frontend/
    ├── materials.html
    ├── grades.html
    └── js/
        ├── materials.js
        └── grades.js

```

## 1. Backend (`services/learning/backend/`)

### 1.1. `services/learning/Dockerfile`

```dockerfile
FROM python:3.11-slim

WORKDIR /app

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

COPY backend/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY backend/ .

EXPOSE 8083

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8083"]

```

### 1.2. `services/learning/backend/requirements.txt`

```text
fastapi==0.115.0
uvicorn[standard]==0.30.6
sqlalchemy==2.0.35
psycopg2-binary==2.9.9
pydantic==2.9.2
PyJWT==2.9.0
httpx==0.27.2

```

### 1.3. `services/learning/backend/app/database.py`

```python
import os
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, declarative_base

DATABASE_URL = os.getenv(
    "DATABASE_URL",
    "postgresql://postgres:postgres@postgres:5432/english_center",
)

engine = create_engine(DATABASE_URL, pool_pre_ping=True)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

```

### 1.4. `services/learning/backend/app/models.py`

*(Tuân thủ 100% `learning-schema-spec.md` và `INV-SYS-02`: chỉ truy cập schema `learning`, không có FK vật lý chéo schema)*

```python
from sqlalchemy import (
    BigInteger,
    CheckConstraint,
    Column,
    DateTime,
    Integer,
    Numeric,
    String,
    Text,
    UniqueConstraint,
    func,
)
from .database import Base


class Material(Base):
    __tablename__ = "materials"
    __table_args__ = (
        CheckConstraint(
            "material_type IN ('DOCUMENT', 'VIDEO', 'EXERCISE', 'SLIDE')",
            name="chk_materials_type",
        ),
        CheckConstraint("btrim(file_url) <> ''", name="chk_materials_file_url_not_empty"),
        CheckConstraint("file_size IS NULL OR file_size >= 0", name="chk_materials_file_size"),
        {"schema": "learning"},
    )

    id = Column(Integer, primary_key=True, index=True)
    class_id = Column(Integer, nullable=False, index=True)
    title = Column(String(255), nullable=False)
    description = Column(Text, nullable=True)
    material_type = Column(String(50), nullable=False, default="DOCUMENT")
    file_url = Column(Text, nullable=False)
    original_filename = Column(String(255), nullable=True)
    file_size = Column(BigInteger, nullable=True)
    uploaded_by = Column(Integer, nullable=True)
    uploaded_at = Column(DateTime(timezone=True), nullable=False, server_default=func.now())
    updated_at = Column(
        DateTime(timezone=True),
        nullable=False,
        server_default=func.now(),
        onupdate=func.now(),
    )


class Grade(Base):
    __tablename__ = "grades"
    __table_args__ = (
        UniqueConstraint(
            "class_id",
            "student_id",
            "assessment_name",
            name="grades_class_id_student_id_assessment_name_key",
        ),
        CheckConstraint("score >= 0", name="chk_grades_score_non_negative"),
        CheckConstraint("max_score > 0", name="chk_grades_max_score_positive"),
        CheckConstraint("score <= max_score", name="chk_score_lte_max"),
        CheckConstraint(
            "weight IS NULL OR (weight > 0 AND weight <= 1)",
            name="chk_grades_weight_range",
        ),
        {"schema": "learning"},
    )

    id = Column(Integer, primary_key=True, index=True)
    class_id = Column(Integer, nullable=False, index=True)
    student_id = Column(Integer, nullable=False, index=True)
    assessment_name = Column(String(255), nullable=False)
    score = Column(Numeric(5, 2), nullable=False)
    max_score = Column(Numeric(5, 2), nullable=False, default=100.0)
    weight = Column(Numeric(3, 2), nullable=True, default=1.0)
    feedback = Column(Text, nullable=True)
    graded_by = Column(Integer, nullable=True)
    graded_at = Column(DateTime(timezone=True), nullable=False, server_default=func.now())
    updated_at = Column(
        DateTime(timezone=True),
        nullable=False,
        server_default=func.now(),
        onupdate=func.now(),
    )

```

### 1.5. `services/learning/backend/app/schemas.py`

*(Tuân thủ chuẩn JSON `snake_case` `INV-SYS-01` và kiểm tra `INV-LRN-01`, `INV-LRN-04`)*

```python
from datetime import datetime
from decimal import Decimal
from typing import Optional, Literal
from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator

MaterialType = Literal["DOCUMENT", "VIDEO", "EXERCISE", "SLIDE"]


class MaterialCreate(BaseModel):
    class_id: int = Field(..., gt=0)
    title: str = Field(..., min_length=1, max_length=255)
    description: Optional[str] = None
    material_type: MaterialType = "DOCUMENT"
    file_url: str = Field(..., min_length=1)
    original_filename: Optional[str] = Field(None, max_length=255)
    file_size: Optional[int] = Field(None, ge=0)

    @field_validator("title", "file_url")
    @classmethod
    def must_not_be_blank(cls, v: str) -> str:
        cleaned = v.strip()
        if not cleaned:
            raise ValueError("Trường này không được để trống (INV-LRN-04)")
        return cleaned


class MaterialResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    class_id: int
    title: str
    description: Optional[str] = None
    material_type: str
    file_url: str
    original_filename: Optional[str] = None
    file_size: Optional[int] = None
    uploaded_by: Optional[int] = None
    uploaded_at: datetime
    updated_at: datetime


class GradeCreate(BaseModel):
    class_id: int = Field(..., gt=0)
    student_id: int = Field(..., gt=0)
    assessment_name: str = Field(..., min_length=1, max_length=255)
    score: Decimal = Field(...)
    max_score: Decimal = Field(default=Decimal("100.0"))
    weight: Optional[Decimal] = Field(default=Decimal("1.0"))
    feedback: Optional[str] = None

    @field_validator("assessment_name")
    @classmethod
    def clean_assessment_name(cls, v: str) -> str:
        cleaned = v.strip()
        if not cleaned:
            raise ValueError("Tên đầu điểm không được để trống")
        return cleaned

    @model_validator(mode="after")
    def validate_score_and_weight(self):
        if self.max_score <= 0:
            raise ValueError("max_score phải lớn hơn 0 (INV-LRN-01)")
        if self.score < 0 or self.score > self.max_score:
            raise ValueError("Điểm số phải thỏa mãn 0 <= score <= max_score (INV-LRN-01)")
        if self.weight is not None and (self.weight <= 0 or self.weight > 1):
            raise ValueError("Trọng số weight phải nằm trong khoảng (0, 1]")
        return self


class GradeUpdate(BaseModel):
    score: Decimal = Field(...)
    max_score: Optional[Decimal] = None
    weight: Optional[Decimal] = None
    feedback: Optional[str] = None


class GradeResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    class_id: int
    student_id: int
    assessment_name: str
    score: float
    max_score: float
    weight: Optional[float] = 1.0
    feedback: Optional[str] = None
    graded_by: Optional[int] = None
    graded_at: datetime
    updated_at: datetime


class StudentGradeSummaryItem(BaseModel):
    class_id: int
    student_id: int
    total_assessments: int
    weighted_avg_score: float
    average_percentage: float
    result: str


class ClassPassRateSummary(BaseModel):
    class_id: int
    total_students: int
    pass_count: int
    fail_count: int
    pass_rate_percentage: float
    student_summaries: list[StudentGradeSummaryItem] = []

```

### 1.6. `services/learning/backend/app/security.py`

*(Xác thực Stateless JWT từ biến môi trường `JWT_SECRET` — `ADR 003`, `INV-SEC-03`)*

```python
import os
from dataclasses import dataclass
from typing import Optional
import jwt
from fastapi import Depends, Header, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

JWT_SECRET = os.getenv("JWT_SECRET", "dev-shared-jwt-secret-change-in-env")
JWT_ALGORITHM = os.getenv("JWT_ALGORITHM", "HS256")
INTERNAL_SECRET_KEY = os.getenv("INTERNAL_SECRET_KEY", "dev-internal-secret-key")

bearer_scheme = HTTPBearer(auto_error=False)


class AppError(Exception):
    def __init__(self, status_code: int, code: str, message: str):
        self.status_code = status_code
        self.code = code
        self.message = message
        super().__init__(message)


@dataclass
class CurrentUser:
    user_id: int
    email: str
    role: str
    full_name: Optional[str] = None
    student_id: Optional[int] = None


def get_current_user(
    credentials: Optional[HTTPAuthorizationCredentials] = Depends(bearer_scheme),
) -> CurrentUser:
    if not credentials or credentials.scheme.lower() != "bearer":
        raise AppError(
            status.HTTP_401_UNAUTHORIZED,
            "AUTH_UNAUTHORIZED",
            "Thiếu hoặc sai định dạng Authorization Bearer token",
        )
    token = credentials.credentials
    try:
        payload = jwt.decode(token, JWT_SECRET, algorithms=[JWT_ALGORITHM])
    except jwt.ExpiredSignatureError:
        raise AppError(
            status.HTTP_401_UNAUTHORIZED,
            "AUTH_TOKEN_EXPIRED",
            "Phiên đăng nhập đã hết hạn",
        )
    except jwt.InvalidTokenError:
        raise AppError(
            status.HTTP_401_UNAUTHORIZED,
            "AUTH_INVALID_TOKEN",
            "Token xác thực không hợp lệ",
        )

    raw_user_id = payload.get("user_id") or payload.get("sub")
    try:
        user_id = int(raw_user_id)
    except (TypeError, ValueError):
        raise AppError(
            status.HTTP_401_UNAUTHORIZED,
            "AUTH_INVALID_TOKEN",
            "Định danh người dùng trong token không hợp lệ",
        )

    role = str(payload.get("role", "")).upper()
    raw_student_id = payload.get("student_id")
    student_id = int(raw_student_id) if raw_student_id is not None else None

    return CurrentUser(
        user_id=user_id,
        email=payload.get("email", ""),
        role=role,
        full_name=payload.get("name") or payload.get("full_name"),
        student_id=student_id,
    )


def require_roles(*allowed_roles: str):
    allowed = {r.upper() for r in allowed_roles}

    def dependency(user: CurrentUser = Depends(get_current_user)) -> CurrentUser:
        if user.role not in allowed:
            raise AppError(
                status.HTTP_403_FORBIDDEN,
                "LRN_FORBIDDEN",
                f"Vai trò {user.role} không có quyền thực hiện thao tác này",
            )
        return user

    return dependency


def verify_internal_secret(
    x_internal_secret: Optional[str] = Header(default=None, alias="X-Internal-Secret"),
):
    if not x_internal_secret or x_internal_secret != INTERNAL_SECRET_KEY:
        raise AppError(
            status.HTTP_403_FORBIDDEN,
            "AUTH_INVALID_INTERNAL_SECRET",
            "X-Internal-Secret không hợp lệ",
        )

```

### 1.7. `services/learning/backend/app/internal_client.py`

*(Gọi REST nội bộ sang `admission-service` và `student-service` kèm retry & timeout 5s theo `R-INT-1`, `R-INT-2`, `INV-LRN-02`, `INV-LRN-03`)*

```python
import os
import time
from typing import Optional
import httpx
from fastapi import status
from .security import AppError, CurrentUser, INTERNAL_SECRET_KEY

ADMISSION_SERVICE_URL = os.getenv("ADMISSION_SERVICE_URL", "http://admission-service:8081")
STUDENT_SERVICE_URL = os.getenv("STUDENT_SERVICE_URL", "http://student-service:8082")
STRICT_INTERNAL_VERIFY = os.getenv("STRICT_INTERNAL_VERIFY", "true").lower() == "true"


def _request_with_retry(url: str, params: Optional[dict] = None) -> Optional[httpx.Response]:
    headers = {"X-Internal-Secret": INTERNAL_SECRET_KEY, "Accept": "application/json"}
    last_exc = None
    for attempt in range(3):
        try:
            with httpx.Client(timeout=5.0) as client:
                return client.get(url, params=params, headers=headers)
        except httpx.RequestError as exc:
            last_exc = exc
            if attempt < 2:
                time.sleep(0.2 * (2 ** attempt))
    if STRICT_INTERNAL_VERIFY and last_exc is not None:
        return None
    return None


def verify_class_and_teacher(class_id: int, current_user: CurrentUser) -> Optional[dict]:
    """
    Gọi GET /api/admission/internal/classes/{class_id} để:
    1. Xác minh lớp tồn tại.
    2. Nếu current_user.role == 'TEACHER', đối chiếu teacher_id == current_user.user_id (INV-LRN-03 / INV-SEC-02).
    """
    url = f"{ADMISSION_SERVICE_URL}/api/admission/internal/classes/{class_id}"
    resp = _request_with_retry(url)

    if resp is None:
        # Nếu môi trường local chỉ chạy đơn lẻ container learning mà chưa bật admission
        if not STRICT_INTERNAL_VERIFY:
            return {"id": class_id, "teacher_id": current_user.user_id, "status": "IN_PROGRESS"}
        raise AppError(
            status.HTTP_503_SERVICE_UNAVAILABLE,
            "LRN_ADMISSION_UNAVAILABLE",
            "Không thể kết nối tới Admission Service để xác minh lớp học",
        )

    if resp.status_code == status.HTTP_404_NOT_FOUND:
        raise AppError(
            status.HTTP_404_NOT_FOUND,
            "LRN_CLASS_NOT_FOUND",
            f"Không tìm thấy lớp học với ID {class_id}",
        )

    payload = resp.json() if resp.content else {}
    class_data = payload.get("data") if isinstance(payload, dict) and "data" in payload else payload

    if current_user.role == "TEACHER":
        teacher_id = class_data.get("teacher_id") if isinstance(class_data, dict) else None
        if teacher_id is not None and int(teacher_id) != current_user.user_id:
            raise AppError(
                status.HTTP_403_FORBIDDEN,
                "LRN_FORBIDDEN_CLASS",
                "Giáo viên không được phép thao tác trên lớp mình không phụ trách (INV-LRN-03)",
            )

    return class_data


def verify_student_studying(student_id: int) -> Optional[dict]:
    """
    Gọi GET /api/student/internal/students/{student_id} xác minh học viên tồn tại và đang STUDYING.
    """
    url = f"{STUDENT_SERVICE_URL}/api/student/internal/students/{student_id}"
    resp = _request_with_retry(url)

    if resp is None:
        if not STRICT_INTERNAL_VERIFY:
            return {"id": student_id, "status": "STUDYING"}
        raise AppError(
            status.HTTP_503_SERVICE_UNAVAILABLE,
            "LRN_STUDENT_SERVICE_UNAVAILABLE",
            "Không thể kết nối tới Student Service để xác minh hồ sơ học viên",
        )

    if resp.status_code == status.HTTP_404_NOT_FOUND:
        raise AppError(
            status.HTTP_404_NOT_FOUND,
            "LRN_STUDENT_NOT_FOUND",
            f"Không tìm thấy hồ sơ học viên với ID {student_id}",
        )

    payload = resp.json() if resp.content else {}
    student_data = payload.get("data") if isinstance(payload, dict) and "data" in payload else payload
    if isinstance(student_data, dict):
        stu_status = student_data.get("status")
        if stu_status and stu_status != "STUDYING":
            raise AppError(
                status.HTTP_422_UNPROCESSABLE_ENTITY,
                "LRN_STUDENT_NOT_STUDYING",
                f"Học viên đang ở trạng thái {stu_status}, không thể nhập điểm mới",
            )
    return student_data


def resolve_student_id_for_user(current_user: CurrentUser) -> int:
    """
    Cầu nối R-ID-1 & INV-LRN-02:
    Ánh xạ admission.users.id (trong JWT sub) sang student.students.id.
    """
    if current_user.student_id is not None:
        return current_user.student_id

    url = f"{STUDENT_SERVICE_URL}/api/student/internal/students"
    resp = _request_with_retry(url, params={"user_id": current_user.user_id})
    if resp is not None and resp.status_code == status.HTTP_200_OK:
        payload = resp.json()
        data = payload.get("data") if isinstance(payload, dict) else None
        if isinstance(data, dict) and "id" in data:
            return int(data["id"])
        if isinstance(data, list) and len(data) > 0 and "id" in data[0]:
            return int(data[0]["id"])

    # Fallback an toàn nếu seed data có student.id đồng bộ hoặc chưa bật student-service
    return current_user.user_id
```

### 1.8. `services/learning/backend/app/routers/materials.py`

```python
from typing import Optional
from fastapi import APIRouter, Depends, Query, status
from sqlalchemy.orm import Session
from ..database import get_db
from ..internal_client import verify_class_and_teacher
from ..models import Material
from ..schemas import MaterialCreate, MaterialResponse
from ..security import AppError, CurrentUser, get_current_user, require_roles

router = APIRouter(prefix="/api/learning/materials", tags=["Materials (LRN-01)"])


@router.get("")
def list_materials(
    class_id: Optional[int] = Query(default=None, gt=0),
    material_type: Optional[str] = Query(default=None),
    search: Optional[str] = Query(default=None),
    page: int = Query(default=1, ge=1),
    size: int = Query(default=20, ge=1, le=100),
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(get_current_user),
):
    query = db.query(Material)
    if class_id is not None:
        query = query.filter(Material.class_id == class_id)
    if material_type:
        query = query.filter(Material.material_type == material_type.upper())
    if search:
        keyword = f"%{search.strip()}%"
        query = query.filter(Material.title.ilike(keyword))

    total_items = query.count()
    items = (
        query.order_by(Material.uploaded_at.desc(), Material.id.desc())
        .offset((page - 1) * size)
        .limit(size)
        .all()
    )
    total_pages = (total_items + size - 1) // size if total_items > 0 else 1

    return {
        "success": True,
        "data": [MaterialResponse.model_validate(item).model_dump(mode="json") for item in items],
        "pagination": {
            "page": page,
            "size": size,
            "total_items": total_items,
            "total_pages": total_pages,
        },
        "message": "Lấy danh sách học liệu thành công",
    }


@router.get("/{material_id}")
def get_material_detail(
    material_id: int,
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(get_current_user),
):
    material = db.query(Material).filter(Material.id == material_id).first()
    if not material:
        raise AppError(
            status.HTTP_404_NOT_FOUND,
            "LRN_MATERIAL_NOT_FOUND",
            f"Không tìm thấy học liệu với ID {material_id}",
        )
    return {
        "success": True,
        "data": MaterialResponse.model_validate(material).model_dump(mode="json"),
        "message": "Lấy chi tiết học liệu thành công",
    }


@router.post("", status_code=status.HTTP_201_CREATED)
def create_material(
    payload: MaterialCreate,
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(require_roles("ADMIN", "STAFF", "TEACHER")),
):
    # Kiểm tra lớp tồn tại và quyền giáo viên phụ trách lớp (INV-LRN-03)
    verify_class_and_teacher(payload.class_id, current_user)

    new_material = Material(
        class_id=payload.class_id,
        title=payload.title,
        description=payload.description,
        material_type=payload.material_type,
        file_url=payload.file_url,
        original_filename=payload.original_filename,
        file_size=payload.file_size,
        uploaded_by=current_user.user_id,
    )
    db.add(new_material)
    db.commit()
    db.refresh(new_material)

    return {
        "success": True,
        "data": MaterialResponse.model_validate(new_material).model_dump(mode="json"),
        "message": "Đăng tải học liệu thành công",
    }


@router.delete("/{material_id}")
def delete_material(
    material_id: int,
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(require_roles("ADMIN", "STAFF", "TEACHER")),
):
    material = db.query(Material).filter(Material.id == material_id).first()
    if not material:
        raise AppError(
            status.HTTP_404_NOT_FOUND,
            "LRN_MATERIAL_NOT_FOUND",
            f"Không tìm thấy học liệu với ID {material_id}",
        )

    if current_user.role == "TEACHER":
        verify_class_and_teacher(material.class_id, current_user)

    db.delete(material)
    db.commit()

    return {
        "success": True,
        "data": {"deleted_id": material_id},
        "message": "Xóa học liệu thành công",
    }

```

### 1.9. `services/learning/backend/app/routers/grades.py`

```python
from decimal import Decimal
from typing import Optional
from fastapi import APIRouter, Depends, Query, status
from sqlalchemy import text
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session
from ..database import get_db
from ..internal_client import (
    resolve_student_id_for_user,
    verify_class_and_teacher,
    verify_student_studying,
)
from ..models import Grade
from ..schemas import GradeCreate, GradeResponse, GradeUpdate
from ..security import AppError, CurrentUser, get_current_user, require_roles

router = APIRouter(prefix="/api/learning", tags=["Grades & Academic Reports (LRN-02, LRN-03)"])


@router.get("/grades")
def list_grades(
    class_id: Optional[int] = Query(default=None, gt=0),
    student_id: Optional[int] = Query(default=None, gt=0),
    assessment_name: Optional[str] = Query(default=None),
    page: int = Query(default=1, ge=1),
    size: int = Query(default=50, ge=1, le=100),
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(get_current_user),
):
    query = db.query(Grade)

    # INV-LRN-02: Học viên chỉ được xem bảng điểm của chính mình
    if current_user.role == "STUDENT":
        own_student_id = resolve_student_id_for_user(current_user)
        if student_id is not None and student_id != own_student_id:
            raise AppError(
                status.HTTP_403_FORBIDDEN,
                "LRN_FORBIDDEN_STUDENT_GRADES",
                "Học viên không được phép xem bảng điểm của học viên khác (INV-LRN-02)",
            )
        query = query.filter(Grade.student_id == own_student_id)
    elif student_id is not None:
        query = query.filter(Grade.student_id == student_id)

    if class_id is not None:
        query = query.filter(Grade.class_id == class_id)
    if assessment_name:
        query = query.filter(Grade.assessment_name.ilike(f"%{assessment_name.strip()}%"))

    total_items = query.count()
    items = (
        query.order_by(Grade.class_id.asc(), Grade.student_id.asc(), Grade.graded_at.asc())
        .offset((page - 1) * size)
        .limit(size)
        .all()
    )
    total_pages = (total_items + size - 1) // size if total_items > 0 else 1

    return {
        "success": True,
        "data": [GradeResponse.model_validate(g).model_dump(mode="json") for g in items],
        "pagination": {
            "page": page,
            "size": size,
            "total_items": total_items,
            "total_pages": total_pages,
        },
        "message": "Lấy danh sách điểm thành công",
    }


@router.post("/grades", status_code=status.HTTP_201_CREATED)
def create_grade(
    payload: GradeCreate,
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(require_roles("ADMIN", "STAFF", "TEACHER")),
):
    # 1. Kiểm tra giáo viên có đúng dạy lớp này không (INV-LRN-03 / INV-SEC-02)
    verify_class_and_teacher(payload.class_id, current_user)

    # 2. Kiểm tra học viên tồn tại và đang STUDYING bên Student Service
    verify_student_studying(payload.student_id)

    # 3. Kiểm tra trùng đầu điểm (INV-LRN-06)
    existing = (
        db.query(Grade)
        .filter(
            Grade.class_id == payload.class_id,
            Grade.student_id == payload.student_id,
            Grade.assessment_name == payload.assessment_name,
        )
        .first()
    )
    if existing:
        raise AppError(
            status.HTTP_409_CONFLICT,
            "LRN_DUPLICATE_ASSESSMENT",
            f"Đầu điểm '{payload.assessment_name}' đã tồn tại cho học viên {payload.student_id} trong lớp {payload.class_id} (INV-LRN-06)",
        )

    grade = Grade(
        class_id=payload.class_id,
        student_id=payload.student_id,
        assessment_name=payload.assessment_name,
        score=payload.score,
        max_score=payload.max_score,
        weight=payload.weight if payload.weight is not None else Decimal("1.0"),
        feedback=payload.feedback,
        graded_by=current_user.user_id,
    )
    db.add(grade)
    try:
        db.commit()
    except IntegrityError:
        db.rollback()
        raise AppError(
            status.HTTP_409_CONFLICT,
            "LRN_DUPLICATE_ASSESSMENT",
            "Đầu điểm đã tồn tại hoặc vi phạm ràng buộc dữ liệu",
        )
    db.refresh(grade)

    return {
        "success": True,
        "data": GradeResponse.model_validate(grade).model_dump(mode="json"),
        "message": "Nhập điểm đánh giá thành công",
    }


@router.put("/grades/{grade_id}")
def update_grade(
    grade_id: int,
    payload: GradeUpdate,
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(require_roles("ADMIN", "STAFF", "TEACHER")),
):
    grade = db.query(Grade).filter(Grade.id == grade_id).first()
    if not grade:
        raise AppError(
            status.HTTP_404_NOT_FOUND,
            "LRN_GRADE_NOT_FOUND",
            f"Không tìm thấy bản ghi điểm với ID {grade_id}",
        )

    verify_class_and_teacher(grade.class_id, current_user)

    new_max = payload.max_score if payload.max_score is not None else Decimal(str(grade.max_score))
    if new_max <= 0 or payload.score < 0 or payload.score > new_max:
        raise AppError(
            status.HTTP_400_BAD_REQUEST,
            "LRN_INVALID_SCORE",
            "Điểm số phải thỏa mãn 0 <= score <= max_score (INV-LRN-01)",
        )
    if payload.weight is not None and (payload.weight <= 0 or payload.weight > 1):
        raise AppError(
            status.HTTP_400_BAD_REQUEST,
            "LRN_INVALID_WEIGHT",
            "Trọng số weight phải nằm trong khoảng (0, 1]",
        )

    grade.score = payload.score
    grade.max_score = new_max
    if payload.weight is not None:
        grade.weight = payload.weight
    if payload.feedback is not None:
        grade.feedback = payload.feedback
    grade.graded_by = current_user.user_id

    db.commit()
    db.refresh(grade)

    return {
        "success": True,
        "data": GradeResponse.model_validate(grade).model_dump(mode="json"),
        "message": "Cập nhật điểm đánh giá thành công",
    }


@router.get("/reports/class-summary")
def get_class_summary_report(
    class_id: int = Query(..., gt=0),
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(require_roles("ADMIN", "STAFF", "TEACHER")),
):
    if current_user.role == "TEACHER":
        verify_class_and_teacher(class_id, current_user)

    pass_rate_row = db.execute(
        text(
            """
            SELECT class_id, total_students, pass_count, fail_count, pass_rate_percentage
            FROM learning.v_class_pass_rate
            WHERE class_id = :class_id
            """
        ),
        {"class_id": class_id},
    ).mappings().first()

    student_rows = db.execute(
        text(
            """
            SELECT class_id, student_id, total_assessments,
                   weighted_avg_score, average_percentage, result
            FROM learning.v_student_grade_summary
            WHERE class_id = :class_id
            ORDER BY average_percentage DESC, student_id ASC
            """
        ),
        {"class_id": class_id},
    ).mappings().all()

    summaries = [
        {
            "class_id": int(r["class_id"]),
            "student_id": int(r["student_id"]),
            "total_assessments": int(r["total_assessments"]),
            "weighted_avg_score": float(r["weighted_avg_score"] or 0),
            "average_percentage": float(r["average_percentage"] or 0),
            "result": r["result"],
        }
        for r in student_rows
    ]

    report_data = {
        "class_id": class_id,
        "total_students": int(pass_rate_row["total_students"]) if pass_rate_row else 0,
        "pass_count": int(pass_rate_row["pass_count"]) if pass_rate_row else 0,
        "fail_count": int(pass_rate_row["fail_count"]) if pass_rate_row else 0,
        "pass_rate_percentage": float(pass_rate_row["pass_rate_percentage"]) if pass_rate_row else 0.0,
        "student_summaries": summaries,
    }

    return {
        "success": True,
        "data": report_data,
        "message": "Lấy báo cáo tổng kết lớp học thành công",
    }


@router.get("/reports/student-summary")
def get_student_summary_report(
    class_id: Optional[int] = Query(default=None, gt=0),
    student_id: Optional[int] = Query(default=None, gt=0),
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(get_current_user),
):
    if current_user.role == "STUDENT":
        target_student_id = resolve_student_id_for_user(current_user)
        if student_id is not None and student_id != target_student_id:
            raise AppError(
                status.HTTP_403_FORBIDDEN,
                "LRN_FORBIDDEN_STUDENT_GRADES",
                "Học viên chỉ được xem kết quả tổng kết của chính mình (INV-LRN-02)",
            )
    else:
        if student_id is None:
            raise AppError(
                status.HTTP_400_BAD_REQUEST,
                "LRN_MISSING_STUDENT_ID",
                "Vui lòng cung cấp student_id cần tra cứu",
            )
        target_student_id = student_id

    sql = """
        SELECT class_id, student_id, total_assessments,
               weighted_avg_score, average_percentage, result
        FROM learning.v_student_grade_summary
        WHERE student_id = :student_id
    """
    params = {"student_id": target_student_id}
    if class_id is not None:
        sql += " AND class_id = :class_id"
        params["class_id"] = class_id

    rows = db.execute(text(sql), params).mappings().all()
    data = [
        {
            "class_id": int(r["class_id"]),
            "student_id": int(r["student_id"]),
            "total_assessments": int(r["total_assessments"]),
            "weighted_avg_score": float(r["weighted_avg_score"] or 0),
            "average_percentage": float(r["average_percentage"] or 0),
            "result": r["result"],
        }
        for r in rows
    ]

    return {
        "success": True,
        "data": data,
        "message": "Lấy kết quả tổng kết học tập thành công",
    }

```

### 1.10. `services/learning/backend/app/main.py`

```python
from fastapi import FastAPI, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from .routers import grades, materials
from .security import AppError

app = FastAPI(
    title="English Center - Learning & Assessment Service",
    description="Phân hệ Quản lý Học liệu, Bảng điểm Trọng số & Báo cáo Học lực (Port 8083)",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.exception_handler(AppError)
async def app_error_handler(request: Request, exc: AppError):
    return JSONResponse(
        status_code=exc.status_code,
        content={
            "success": False,
            "error": {
                "code": exc.code,
                "message": exc.message,
            },
        },
    )


@app.exception_handler(RequestValidationError)
async def validation_error_handler(request: Request, exc: RequestValidationError):
    errors = exc.errors()
    first_msg = errors[0].get("msg", "Dữ liệu đầu vào không hợp lệ") if errors else "Dữ liệu không hợp lệ"
    err_code = "LRN_VALIDATION_ERROR"
    if "score" in str(errors) or "max_score" in str(errors):
        err_code = "LRN_INVALID_SCORE"
    elif "file_url" in str(errors):
        err_code = "LRN_EMPTY_FILE_URL"
    elif "weight" in str(errors):
        err_code = "LRN_INVALID_WEIGHT"

    return JSONResponse(
        status_code=status.HTTP_400_BAD_REQUEST,
        content={
            "success": False,
            "error": {
                "code": err_code,
                "message": first_msg,
            },
        },
    )


@app.get("/health")
def health_check():
    return {
        "success": True,
        "data": {"service": "learning-service", "port": 8083, "status": "UP"},
        "message": "Learning service is healthy",
    }


app.include_router(materials.router)
app.include_router(grades.router)

```

---

## 2. Frontend (`services/learning/frontend/`)

(Tuân thủ `design-system.md` và `ADR 002`: dùng CSS variables `/shared/css/*.css`, `/shared/js/api.js`, zero build step).

### 2.1. `services/learning/frontend/materials.html`

```html
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Kho Học liệu Số - Trung tâm Anh ngữ</title>
  <link rel="stylesheet" href="/shared/css/variables.css">
  <link rel="stylesheet" href="/shared/css/base.css">
  <link rel="stylesheet" href="/shared/css/components.css">
  <link rel="stylesheet" href="/shared/css/layout.css">
</head>
<body>
  <div id="app">
    <div id="sidebar"></div>
    <div class="main-content">
      <div id="header"></div>
      <div class="content">
        <nav class="breadcrumb">
          <a href="/">Trang chủ</a> / <span>Đào tạo & Học tập</span> / <span>Kho học liệu số</span>
        </nav>

        <div class="card" style="margin-bottom: var(--space-lg);">
          <div class="card-header" style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:12px;">
            <div>
              <h1 class="heading-1">Kho Học liệu Số theo Lớp (LRN-01)</h1>
              <p class="text-sm" style="color: var(--color-text-secondary);">
                Tập trung hóa Slide, Tài liệu đọc, Video bài giảng và Bài tập theo từng lớp học
              </p>
            </div>
            <button id="btnOpenMaterialModal" class="btn btn-primary" onclick="openMaterialModal()">
              + Đăng tải học liệu
            </button>
          </div>

          <div class="card-body">
            <div style="display:flex; gap:12px; flex-wrap:wrap; margin-bottom: var(--space-md);">
              <div class="form-group" style="min-width: 160px;">
                <label class="label" for="filterClassId">Mã Lớp (class_id)</label>
                <input type="number" id="filterClassId" class="form-input" value="1" min="1">
              </div>
              <div class="form-group" style="min-width: 180px;">
                <label class="label" for="filterType">Phân loại</label>
                <select id="filterType" class="form-select">
                  <option value="">Tất cả loại</option>
                  <option value="DOCUMENT">DOCUMENT (Tài liệu)</option>
                  <option value="SLIDE">SLIDE (Bài giảng)</option>
                  <option value="VIDEO">VIDEO (Video)</option>
                  <option value="EXERCISE">EXERCISE (Bài tập)</option>
                </select>
              </div>
              <div class="form-group" style="flex:1; min-width: 220px;">
                <label class="label" for="filterSearch">Tìm tiêu đề</label>
                <input type="text" id="filterSearch" class="form-input" placeholder="Nhập từ khóa học liệu...">
              </div>
              <div class="form-group" style="display:flex; align-items:flex-end;">
                <button class="btn btn-secondary" onclick="loadMaterials()">Lọc dữ liệu</button>
              </div>
            </div>

            <div style="overflow-x:auto;">
              <table class="data-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Lớp</th>
                    <th>Tiêu đề học liệu</th>
                    <th>Loại</th>
                    <th>Tên file / Link</th>
                    <th>Dung lượng</th>
                    <th>Ngày đăng</th>
                    <th>Thao tác</th>
                  </tr>
                </thead>
                <tbody id="materialsTableBody">
                  <tr>
                    <td colspan="8" style="text-align:center;">Đang tải dữ liệu...</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- Modal Đăng tải học liệu -->
  <div class="modal-overlay" id="materialModal" style="display:none;">
    <div class="modal">
      <div class="modal-header">
        <h3 class="heading-3">Đăng tải Học liệu Mới</h3>
        <button class="modal-close" onclick="closeMaterialModal()">×</button>
      </div>
      <div class="modal-body">
        <div class="form-group">
          <label class="label" for="matClassId">ID Lớp học (`class_id`) *</label>
          <input type="number" id="matClassId" class="form-input" value="1" min="1" required>
        </div>
        <div class="form-group">
          <label class="label" for="matTitle">Tiêu đề học liệu *</label>
          <input type="text" id="matTitle" class="form-input" placeholder="VD: IELTS Reading Unit 1 - Skimming & Scanning" required>
        </div>
        <div class="form-group">
          <label class="label" for="matType">Loại học liệu *</label>
          <select id="matType" class="form-select">
            <option value="DOCUMENT">DOCUMENT</option>
            <option value="SLIDE">SLIDE</option>
            <option value="VIDEO">VIDEO</option>
            <option value="EXERCISE">EXERCISE</option>
          </select>
        </div>
        <div class="form-group">
          <label class="label" for="matFileUrl">Đường dẫn File (`file_url` - INV-LRN-04) *</label>
          <input type="text" id="matFileUrl" class="form-input" placeholder="https://cdn.englishcenter.edu.vn/materials/unit1.pdf" required>
        </div>
        <div class="form-group">
          <label class="label" for="matFilename">Tên file gốc (`original_filename`)</label>
          <input type="text" id="matFilename" class="form-input" placeholder="unit1_reading.pdf">
        </div>
        <div class="form-group">
          <label class="label" for="matFileSize">Dung lượng (Bytes)</label>
          <input type="number" id="matFileSize" class="form-input" min="0" placeholder="1048576">
        </div>
        <div class="form-group">
          <label class="label" for="matDesc">Mô tả hướng dẫn học tập</label>
          <textarea id="matDesc" class="form-input" rows="3" placeholder="Đọc trước trang 1-12 trước buổi học..."></textarea>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn btn-secondary" onclick="closeMaterialModal()">Hủy</button>
        <button class="btn btn-primary" onclick="submitCreateMaterial()">Lưu học liệu</button>
      </div>
    </div>
  </div>

  <script src="/shared/js/auth.js"></script>
  <script src="/shared/js/api.js"></script>
  <script src="/shared/js/layout.js"></script>
  <script src="/learning/js/materials.js"></script>
</body>
</html>

```

### 2.2. `services/learning/frontend/js/materials.js`

```javascript
async function requestLearningApi(path, options = {}) {
  if (window.api && typeof window.api.request === 'function') {
    return window.api.request(path, options);
  }
  const token = localStorage.getItem('access_token') || localStorage.getItem('token') || '';
  const headers = Object.assign(
    { 'Content-Type': 'application/json' },
    token ? { Authorization: `Bearer ${token}` } : {},
    options.headers || {}
  );
  const response = await fetch(path, Object.assign({}, options, { headers }));
  return response.json();
}

function formatBytes(bytes) {
  if (bytes === null || bytes === undefined) return '—';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
}

function badgeForType(type) {
  const map = {
    DOCUMENT: 'badge-info',
    SLIDE: 'badge-warning',
    VIDEO: 'badge-success',
    EXERCISE: 'badge-danger',
  };
  return `<span class="badge ${map[type] || 'badge-info'}">${type}</span>`;
}

async function loadMaterials() {
  const classId = document.getElementById('filterClassId').value;
  const type = document.getElementById('filterType').value;
  const search = document.getElementById('filterSearch').value.trim();

  const params = new URLSearchParams();
  if (classId) params.set('class_id', classId);
  if (type) params.set('material_type', type);
  if (search) params.set('search', search);

  const tbody = document.getElementById('materialsTableBody');
  try {
    const res = await requestLearningApi(`/api/learning/materials?${params.toString()}`);
    if (!res.success) {
      tbody.innerHTML = `<tr><td colspan="8" style="color:var(--color-danger); text-align:center;">${res.error?.message || 'Lỗi tải dữ liệu'}</td></tr>`;
      return;
    }
    const items = res.data || [];
    if (items.length === 0) {
      tbody.innerHTML = `<tr><td colspan="8" style="text-align:center;">Chưa có học liệu nào cho bộ lọc này.</td></tr>`;
      return;
    }

    tbody.innerHTML = items
      .map(
        (m) => `
      <tr>
        <td>#${m.id}</td>
        <td>Lớp ${m.class_id}</td>
        <td>
          <strong>${m.title}</strong>
          ${m.description ? `<div class="text-xs" style="color:var(--color-text-secondary);">${m.description}</div>` : ''}
        </td>
        <td>${badgeForType(m.material_type)}</td>
        <td><a href="${m.file_url}" target="_blank" rel="noopener noreferrer">${m.original_filename || m.file_url}</a></td>
        <td>${formatBytes(m.file_size)}</td>
        <td>${new Date(m.uploaded_at).toLocaleDateString('vi-VN')}</td>
        <td>
          <button class="btn btn-danger" style="padding:4px 10px;" onclick="handleDeleteMaterial(${m.id})">🗑️ Xóa</button>
        </td>
      </tr>
    `
      )
      .join('');
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="8" style="color:var(--color-danger); text-align:center;">Không thể kết nối tới Learning Service</td></tr>`;
  }
}

function openMaterialModal() {
  document.getElementById('materialModal').style.display = 'flex';
}

function closeMaterialModal() {
  document.getElementById('materialModal').style.display = 'none';
}

async function submitCreateMaterial() {
  const payload = {
    class_id: Number(document.getElementById('matClassId').value),
    title: document.getElementById('matTitle').value.trim(),
    material_type: document.getElementById('matType').value,
    file_url: document.getElementById('matFileUrl').value.trim(),
    original_filename: document.getElementById('matFilename').value.trim() || null,
    file_size: document.getElementById('matFileSize').value
      ? Number(document.getElementById('matFileSize').value)
      : null,
    description: document.getElementById('matDesc').value.trim() || null,
  };

  const res = await requestLearningApi('/api/learning/materials', {
    method: 'POST',
    body: JSON.stringify(payload),
  });

  if (!res.success) {
    alert(res.error?.message || 'Không thể tạo học liệu');
    return;
  }
  closeMaterialModal();
  loadMaterials();
}

async function handleDeleteMaterial(materialId) {
  if (!confirm(`Bạn có chắc chắn muốn xóa học liệu #${materialId}?`)) return;
  const res = await requestLearningApi(`/api/learning/materials/${materialId}`, {
    method: 'DELETE',
  });
  if (!res.success) {
    alert(res.error?.message || 'Không thể xóa học liệu');
    return;
  }
  loadMaterials();
}

document.addEventListener('DOMContentLoaded', loadMaterials);

```

### 2.3. `services/learning/frontend/grades.html`

```html
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Sổ Điểm & Báo cáo Học lực - Trung tâm Anh ngữ</title>
  <link rel="stylesheet" href="/shared/css/variables.css">
  <link rel="stylesheet" href="/shared/css/base.css">
  <link rel="stylesheet" href="/shared/css/components.css">
  <link rel="stylesheet" href="/shared/css/layout.css">
</head>
<body>
  <div id="app">
    <div id="sidebar"></div>
    <div class="main-content">
      <div id="header"></div>
      <div class="content">
        <nav class="breadcrumb">
          <a href="/">Trang chủ</a> / <span>Đào tạo & Học tập</span> / <span>Sổ điểm & Kết quả học tập</span>
        </nav>

        <!-- Thống kê BI v_class_pass_rate (LRN-03) -->
        <div id="classReportSection" style="display:grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; margin-bottom: var(--space-lg);">
          <div class="card">
            <div class="card-body">
              <div class="text-xs" style="color:var(--color-text-secondary);">SĨ SỐ ĐÃ CÓ ĐIỂM</div>
              <div id="statTotalStudents" class="heading-1">0</div>
            </div>
          </div>
          <div class="card">
            <div class="card-body">
              <div class="text-xs" style="color:var(--color-text-secondary);">SỐ HỌC VIÊN ĐẠT (PASS ≥ 50%)</div>
              <div id="statPassCount" class="heading-1" style="color:var(--color-success);">0</div>
            </div>
          </div>
          <div class="card">
            <div class="card-body">
              <div class="text-xs" style="color:var(--color-text-secondary);">SỐ HỌC VIÊN CHƯA ĐẠT (FAIL)</div>
              <div id="statFailCount" class="heading-1" style="color:var(--color-danger);">0</div>
            </div>
          </div>
          <div class="card">
            <div class="card-body">
              <div class="text-xs" style="color:var(--color-text-secondary);">TỶ LỆ ĐẠT TOÀN LỚP</div>
              <div id="statPassRate" class="heading-1" style="color:var(--color-primary);">0%</div>
            </div>
          </div>
        </div>

        <!-- Bảng chi tiết đầu điểm (LRN-02) -->
        <div class="card" style="margin-bottom: var(--space-lg);">
          <div class="card-header" style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:12px;">
            <div>
              <h1 class="heading-1">Sổ Điểm Trọng Số & Nhận Xét (LRN-02)</h1>
              <p class="text-sm" style="color:var(--color-text-secondary);">
                Kiểm soát chặt chẽ 0 ≤ score ≤ max_score (INV-LRN-01) và phân quyền giảng viên phụ trách lớp (INV-LRN-03)
              </p>
            </div>
            <button class="btn btn-primary" onclick="openGradeModal()">+ Nhập điểm mới</button>
          </div>

          <div class="card-body">
            <div style="display:flex; gap:12px; flex-wrap:wrap; margin-bottom: var(--space-md);">
              <div class="form-group">
                <label class="label" for="gradeFilterClassId">Mã Lớp (`class_id`)</label>
                <input type="number" id="gradeFilterClassId" class="form-input" value="1" min="1">
              </div>
              <div class="form-group">
                <label class="label" for="gradeFilterStudentId">Mã Hồ sơ Học viên (`student_id`)</label>
                <input type="number" id="gradeFilterStudentId" class="form-input" placeholder="Tất cả (hoặc của tôi)">
              </div>
              <div class="form-group" style="display:flex; align-items:flex-end;">
                <button class="btn btn-secondary" onclick="refreshAllGradeData()">Tải sổ điểm & Báo cáo</button>
              </div>
            </div>

            <div style="overflow-x:auto;">
              <table class="data-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Lớp</th>
                    <th>Mã Học viên (`student_id`)</th>
                    <th>Bài đánh giá (`assessment_name`)</th>
                    <th>Điểm / Thang tối đa</th>
                    <th>Trọng số (`weight`)</th>
                    <th>Quy đổi (%)</th>
                    <th>Nhận xét của Giáo viên</th>
                  </tr>
                </thead>
                <tbody id="gradesTableBody">
                  <tr>
                    <td colspan="8" style="text-align:center;">Đang tải bảng điểm...</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- Bảng tổng kết chuẩn hóa thang 100% từ View v_student_grade_summary (LRN-03) -->
        <div class="card">
          <div class="card-header">
            <h2 class="heading-2">Bảng Tổng Kết Điểm Chuẩn Hóa Thang 100% (`v_student_grade_summary`)</h2>
          </div>
          <div class="card-body" style="overflow-x:auto;">
            <table class="data-table">
              <thead>
                <tr>
                  <th>Lớp (`class_id`)</th>
                  <th>Học viên (`student_id`)</th>
                  <th>Số đầu điểm</th>
                  <th>Điểm TB Trọng số (`weighted_avg_score`)</th>
                  <th>Tỷ lệ Phần trăm Chuẩn hóa (`average_percentage`)</th>
                  <th>Xếp loại (`INV-LRN-05`)</th>
                </tr>
              </thead>
              <tbody id="summaryTableBody">
                <tr>
                  <td colspan="6" style="text-align:center;">Chưa có dữ liệu tổng kết.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

      </div>
    </div>
  </div>

  <!-- Modal Nhập điểm -->
  <div class="modal-overlay" id="gradeModal" style="display:none;">
    <div class="modal">
      <div class="modal-header">
        <h3 class="heading-3">Nhập Điểm Đánh Giá Học Viên</h3>
        <button class="modal-close" onclick="closeGradeModal()">×</button>
      </div>
      <div class="modal-body">
        <div class="form-group">
          <label class="label" for="inpGradeClassId">Mã Lớp (`class_id`) *</label>
          <input type="number" id="inpGradeClassId" class="form-input" value="1" min="1" required>
        </div>
        <div class="form-group">
          <label class="label" for="inpGradeStudentId">ID Học viên (`student.students.id` - R-ID-1) *</label>
          <input type="number" id="inpGradeStudentId" class="form-input" value="1" min="1" required>
        </div>
        <div class="form-group">
          <label class="label" for="inpAssessmentName">Tên đầu điểm (`assessment_name` - INV-LRN-06) *</label>
          <input type="text" id="inpAssessmentName" class="form-input" placeholder="VD: Quiz 1, Midterm, Final Exam" required>
        </div>
        <div style="display:grid; grid-template-columns: 1fr 1fr 1fr; gap: 12px;">
          <div class="form-group">
            <label class="label" for="inpScore">Điểm đạt (`score`) *</label>
            <input type="number" step="0.01" id="inpScore" class="form-input" min="0" value="85" required>
          </div>
          <div class="form-group">
            <label class="label" for="inpMaxScore">Thang điểm (`max_score`) *</label>
            <input type="number" step="0.01" id="inpMaxScore" class="form-input" min="1" value="100" required>
          </div>
          <div class="form-group">
            <label class="label" for="inpWeight">Trọng số (`0 < w <= 1`) *</label>
            <input type="number" step="0.05" id="inpWeight" class="form-input" min="0.05" max="1.0" value="0.4" required>
          </div>
        </div>
        <div class="form-group">
          <label class="label" for="inpFeedback">Nhận xét chi tiết của Giảng viên</label>
          <textarea id="inpFeedback" class="form-input" rows="3" placeholder="Nắm vững cấu trúc ngữ pháp, cần cải thiện phát âm..."></textarea>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn btn-secondary" onclick="closeGradeModal()">Hủy</button>
        <button class="btn btn-primary" onclick="submitCreateGrade()">Lưu điểm</button>
      </div>
    </div>
  </div>

  <script src="/shared/js/auth.js"></script>
  <script src="/shared/js/api.js"></script>
  <script src="/shared/js/layout.js"></script>
  <script src="/learning/js/grades.js"></script>
</body>
</html>

```

### 2.4. `services/learning/frontend/js/grades.js`

```javascript
async function requestGradeApi(path, options = {}) {
  if (window.api && typeof window.api.request === 'function') {
    return window.api.request(path, options);
  }
  const token = localStorage.getItem('access_token') || localStorage.getItem('token') || '';
  const headers = Object.assign(
    { 'Content-Type': 'application/json' },
    token ? { Authorization: `Bearer ${token}` } : {},
    options.headers || {}
  );
  const response = await fetch(path, Object.assign({}, options, { headers }));
  return response.json();
}

async function loadGradesList() {
  const classId = document.getElementById('gradeFilterClassId').value;
  const studentId = document.getElementById('gradeFilterStudentId').value;

  const params = new URLSearchParams();
  if (classId) params.set('class_id', classId);
  if (studentId) params.set('student_id', studentId);

  const tbody = document.getElementById('gradesTableBody');
  const res = await requestGradeApi(`/api/learning/grades?${params.toString()}`);

  if (!res.success) {
    tbody.innerHTML = `<tr><td colspan="8" style="color:var(--color-danger); text-align:center;">${res.error?.message || 'Không thể tải bảng điểm'}</td></tr>`;
    return;
  }

  const items = res.data || [];
  if (items.length === 0) {
    tbody.innerHTML = `<tr><td colspan="8" style="text-align:center;">Chưa có đầu điểm nào được ghi nhận.</td></tr>`;
    return;
  }

  tbody.innerHTML = items
    .map((g) => {
      const pct = g.max_score > 0 ? ((g.score / g.max_score) * 100).toFixed(1) : '0.0';
      return `
        <tr>
          <td>#${g.id}</td>
          <td>Lớp ${g.class_id}</td>
          <td>HV #${g.student_id}</td>
          <td><strong>${g.assessment_name}</strong></td>
          <td><strong>${g.score}</strong> / ${g.max_score}</td>
          <td>${g.weight ?? 1.0}</td>
          <td>${pct}%</td>
          <td>${g.feedback || '<span style="color:var(--color-text-muted);">Chưa có nhận xét</span>'}</td>
        </tr>
      `;
    })
    .join('');
}

async function loadClassSummaryReport() {
  const classId = document.getElementById('gradeFilterClassId').value || 1;
  const summaryBody = document.getElementById('summaryTableBody');

  const res = await requestGradeApi(`/api/learning/reports/class-summary?class_id=${classId}`);
  if (!res.success || !res.data) {
    summaryBody.innerHTML = `<tr><td colspan="6" style="text-align:center;">${res.error?.message || 'Chỉ Giảng viên/Giáo vụ/Admin mới xem được thống kê toàn lớp.'}</td></tr>`;
    return;
  }

  const report = res.data;
  document.getElementById('statTotalStudents').textContent = report.total_students;
  document.getElementById('statPassCount').textContent = report.pass_count;
  document.getElementById('statFailCount').textContent = report.fail_count;
  document.getElementById('statPassRate').textContent = `${report.pass_rate_percentage}%`;

  const list = report.student_summaries || [];
  if (list.length === 0) {
    summaryBody.innerHTML = `<tr><td colspan="6" style="text-align:center;">Chưa có dữ liệu tổng kết cho lớp ${classId}.</td></tr>`;
    return;
  }

  summaryBody.innerHTML = list
    .map(
      (s) => `
      <tr>
        <td>Lớp ${s.class_id}</td>
        <td>HV #${s.student_id}</td>
        <td>${s.total_assessments} bài</td>
        <td>${s.weighted_avg_score}</td>
        <td><strong>${s.average_percentage}%</strong></td>
        <td>
          <span class="badge ${s.result === 'Pass' ? 'badge-success' : 'badge-danger'}">
            ${s.result === 'Pass' ? 'ĐẠT (Pass)' : 'CHƯA ĐẠT (Fail)'}
          </span>
        </td>
      </tr>
    `
    )
    .join('');
}

function openGradeModal() {
  document.getElementById('gradeModal').style.display = 'flex';
}

function closeGradeModal() {
  document.getElementById('gradeModal').style.display = 'none';
}

async function submitCreateGrade() {
  const score = Number(document.getElementById('inpScore').value);
  const maxScore = Number(document.getElementById('inpMaxScore').value);
  const weight = Number(document.getElementById('inpWeight').value);

  if (score < 0 || score > maxScore || maxScore <= 0) {
    alert('Vi phạm INV-LRN-01: Điểm số phải nằm trong khoảng 0 <= score <= max_score!');
    return;
  }

  const payload = {
    class_id: Number(document.getElementById('inpGradeClassId').value),
    student_id: Number(document.getElementById('inpGradeStudentId').value),
    assessment_name: document.getElementById('inpAssessmentName').value.trim(),
    score: score,
    max_score: maxScore,
    weight: weight,
    feedback: document.getElementById('inpFeedback').value.trim() || null,
  };

  const res = await requestGradeApi('/api/learning/grades', {
    method: 'POST',
    body: JSON.stringify(payload),
  });

  if (!res.success) {
    alert(`[${res.error?.code || 'ERROR'}] ${res.error?.message || 'Lỗi khi lưu điểm'}`);
    return;
  }

  closeGradeModal();
  refreshAllGradeData();
}

function refreshAllGradeData() {
  loadGradesList();
  loadClassSummaryReport();
}

document.addEventListener('DOMContentLoaded', refreshAllGradeData);

```

---

## 3. Cập nhật mục lục `services/learning/docs/README.md`

Để đồng bộ với 3 file đặc tả mới vừa tạo, bạn cập nhật `services/learning/docs/README.md` như sau:

```markdown
# Tài liệu Phân hệ Quản lý Học tập & Bảng điểm (`learning-service`)

- **Port nội bộ**: `8083` (Gateway route: `/api/learning/*`)
- **Database Schema**: `learning` (2 bảng, 2 views báo cáo, 22 cột, 2 triggers)

## Danh mục Tài liệu Chính thức
1. [**`learning-schema-spec.md`**](learning-schema-spec.md) — Đặc tả CSDL, View báo cáo & Quy tắc API liên service (sinh tự động từ PostgreSQL 16 `scripts/init-db.sql`).
2. [**`learning-ba-bi-spec.md`**](learning-ba-bi-spec.md) — Đặc tả Nghiệp vụ (BA) & Báo cáo Học lực (BI): Kho học liệu số, Sổ điểm trọng số và Tỷ lệ Pass/Fail.
3. [**`learning-tech-spec.md`**](learning-tech-spec.md) — Đặc tả Kỹ thuật & Thiết kế REST API (`/api/learning/...`), mã lỗi chuẩn và quy tắc gọi liên service.
4. [**`learning-uml-spec.md`**](learning-uml-spec.md) — Toàn bộ sơ đồ UML của phân hệ Learning (Use Case, Sequence, Activity, ERD & Views).
5. [**`tong_hop_learning_tinh_nang_va_schema.md`**](tong_hop_learning_tinh_nang_va_schema.md) — Bản tổng hợp tính năng nghiệp vụ, công thức điểm trọng số và thiết kế schema.
```

Bạn có muốn tôi viết thêm file kiểm thử `tests/invariants/test_learning_invariants.py` (hoặc kiểm tra khớp với bộ test bất biến hiện tại của repo) để đảm bảo chạy `python3 -m unittest discover -s tests/invariants` xanh 100% không?
