
# TỔNG HỢP TÍNH NĂNG VÀ CƠ SỞ DỮ LIỆU PHÂN HỆ ĐÀO TẠO & KẾT QUẢ HỌC TẬP (LEARNING)

**Dự án:** Hệ thống Quản lý Trung tâm Anh ngữ
**Phân hệ:** Learning Service (`learning-service`, port 8083)
**Công nghệ:** Python FastAPI + Frontend HTML/JS + PostgreSQL
**Trạng thái:** Bản tổng hợp đặc tả nghiệp vụ và CSDL (Version 2.0) - Dùng để gửi duyệt và triển khai code.

---

## 1. Danh sách các tính năng (Feature List)

Dựa trên yêu cầu nghiệp vụ và các quy tắc bất biến (Invariant), phân hệ Learning bao gồm các tính năng cốt lõi sau:

### Nhóm 1: Quản lý Học liệu (LRN-01)

- **Mô tả:** Giáo viên đăng tải slide, tài liệu đọc, file audio; học viên xem và tải về.
- **Actor:** `TEACHER`, `STAFF`, `STUDENT`
- **Ràng buộc nghiệp vụ:**
  - Học viên chỉ được xem/tải học liệu của lớp mình đang tham gia.
  - Đường dẫn file (`file_url`) không được để trống.
  - Quản lý theo `class_id` và `material_type` (DOCUMENT, VIDEO, SLIDE, EXERCISE).

### Nhóm 2: Quản lý Điểm số & Đánh giá (LRN-02) - [Core V2]

- **Mô tả:** Ghi nhận điểm số bài kiểm tra giữa kỳ, cuối kỳ, chuyên cần và nhận xét.
- **Actor:** `TEACHER`, `STAFF`, `STUDENT`
- **Ràng buộc nghiệp vụ (Invariant):**
  - Thang điểm hợp lệ: `0 <= score <= max_score`.
  - Giáo viên không được nhập/sửa điểm cho học viên thuộc lớp ngoài thẩm quyền phụ trách.
  - Học viên không được xem điểm của học viên khác (chỉ xem điểm của chính mình).
  - (Đề xuất bổ sung) Thêm cột `weight` để phục vụ tính điểm trung bình môn.

### Nhóm 3: Báo cáo Đánh giá Định kỳ (LRN-03)

- **Mô tả:** Thống kê bảng xếp loại học lực, tỷ lệ đạt/trượt của lớp học.
- **Actor:** `STAFF`, `TEACHER`
- **Ràng buộc nghiệp vụ:** Điểm trung bình >= 50% (tùy thuộc vào cấu hình `max_score`) được tính là Đạt (Pass).

---

## 2. Thiết kế Cơ sở dữ liệu (Schema `learning`)

Dựa trên việc rà soát file `init-db.sql` và các đề xuất cải tiến, schema `learning` bao gồm 2 bảng chính với cấu trúc như sau:

| STT | Tên bảng             | Vai trò                                | Các cột (Bao gồm cập nhật mới)                                                                                                                         |
| --: | ---------------------- | --------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------ |
|   1 | `learning.materials` | Quản lý học liệu, slide bài giảng | `id`, `class_id`, `title`, `description`, `material_type`, `file_url`, `original_filename`*(mới)*, `file_size`*(mới)*, `uploaded_at` |
|   2 | `learning.grades`    | Quản lý điểm số và đánh giá    | `id`, `class_id`, `student_id`, `assessment_name`, `score`, `max_score`, `weight`*(mới)*, `feedback`, `graded_at`                       |

**Lưu ý quan hệ:**

- `class_id` liên kết logic (logical FK) với `admission.classes(id)`.
- `student_id` liên kết logic (logical FK) với `student.students(id)`.

---

## 3. Toàn văn SQL Script Khởi tạo (Full DDL)

> **Ghi chú cho Leader:** Đây là script full 1 lần chạy (bao gồm cả `DROP` và `CREATE`). Script này đã tích hợp sẵn các cột cải tiến để chạy độc lập, không cần file ALTER.

```sql
-- ========================================================
-- DDL SCRIPT: LEARNING SERVICE (PostgreSQL)
-- Chạy độc lập, bao gồm xóa bảng cũ (nếu có) và tạo mới
-- ========================================================

-- 1. Khởi tạo Schema
CREATE SCHEMA IF NOT EXISTS learning;

-- 2. Xóa bảng cũ (Cẩn thận khi chạy trên Production, dùng cho Dev/Test)
DROP TABLE IF EXISTS learning.grades CASCADE;
DROP TABLE IF EXISTS learning.materials CASCADE;

-- 3. Bảng Quản lý Học liệu (Materials)
CREATE TABLE learning.materials (
    id SERIAL PRIMARY KEY,
    class_id INTEGER NOT NULL, -- Logical FK -> admission.classes(id)
    title VARCHAR(255) NOT NULL,
    description TEXT,
    material_type VARCHAR(50) DEFAULT 'DOCUMENT' CHECK (material_type IN ('DOCUMENT', 'VIDEO', 'EXERCISE', 'SLIDE')),
    file_url TEXT NOT NULL,
    original_filename VARCHAR(255), -- Đề xuất thêm: Lưu tên file gốc
    file_size INTEGER, -- Đề xuất thêm: Dung lượng file (Bytes)
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. Bảng Quản lý Điểm số (Grades)
CREATE TABLE learning.grades (
    id SERIAL PRIMARY KEY,
    class_id INTEGER NOT NULL, -- Logical FK -> admission.classes(id)
    student_id INTEGER NOT NULL, -- Logical FK -> student.students(id)
    assessment_name VARCHAR(255) NOT NULL, -- Midterm, Final, Speaking, Quiz...
    score NUMERIC(5, 2) NOT NULL CHECK (score >= 0),
    max_score NUMERIC(5, 2) DEFAULT 100.0 CHECK (max_score > 0),
    weight NUMERIC(3, 2) DEFAULT 1.0 CHECK (weight > 0 AND weight <= 1), -- Đề xuất thêm: Hệ số điểm (VD: 0.3, 0.7)
    feedback TEXT,
    graded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  
    -- Ràng buộc toàn vẹn: Điểm không được vượt quá điểm tối đa
    CONSTRAINT chk_score_max_score CHECK (score <= max_score),
  
    -- Ràng buộc chống trùng lặp: 1 học viên chỉ có 1 điểm cho 1 bài test trong 1 lớp
    CONSTRAINT uq_student_assessment UNIQUE (class_id, student_id, assessment_name)
);

-- 5. Tạo Indexes tối ưu hiệu năng
CREATE INDEX idx_materials_class_id ON learning.materials (class_id);
CREATE INDEX idx_grades_class_student ON learning.grades (class_id, student_id);

-- ========================================================
-- SEED DATA (Dữ liệu mẫu để test Frontend/Backend)
-- ========================================================

-- Thêm Học liệu mẫu
INSERT INTO learning.materials (class_id, title, description, material_type, file_url, original_filename, file_size)
VALUES 
    (1, 'Giáo trình IELTS Foundation Unit 1-4', 'Slide và bài tập từ vựng chủ đề Daily Routine', 'SLIDE', 'https://example.com/materials/ielts-u1-4.pdf', 'ielts-u1-4.pdf', 2048000),
    (1, 'Audio luyện nghe Cambridge Practice Test 1', 'File nghe định dạng MP3 cho bài tập về nhà', 'DOCUMENT', 'https://example.com/materials/listening-test1.mp3', 'listening-test1.mp3', 5120000),
    (2, 'Tài liệu Reading Intensive', 'Reading Passages for Advanced', 'DOCUMENT', 'https://example.com/materials/reading-adv.pdf', 'reading-adv.pdf', 3145728);

-- Thêm Điểm số mẫu (Lưu ý: class_id=1, student_id=1,2 tương ứng với dữ liệu mẫu của nhóm Admission/Student)
INSERT INTO learning.grades (class_id, student_id, assessment_name, score, max_score, weight, feedback)
VALUES 
    (1, 1, 'Midterm Test (Reading & Listening)', 82.5, 100.0, 0.4, 'Kỹ năng nghe tốt, cần chú ý phần Matching Headings.'),
    (1, 1, 'Final Test', 78.0, 100.0, 0.6, 'Cần cải thiện từ vựng học thuật.'),
    (1, 2, 'Midterm Test (Reading & Listening)', 90.0, 100.0, 0.4, 'Xuất sắc, phát âm chuẩn.');
```
