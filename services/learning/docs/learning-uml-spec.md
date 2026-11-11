# ĐẶC TẢ SƠ ĐỒ UML - PHÂN HỆ ĐÀO TẠO & KẾT QUẢ HỌC TẬP (`learning`)

> **Phiên bản:** Final 1.1 (bổ sung UC-LRN-02.3, sequence §3.3, activity §4.2 cho luồng PUT)

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
        UC_LRN_02_1("LRN-02.1: Nhập điểm trọng số (POST)")
        UC_LRN_02_3("LRN-02.3: Cập nhật điểm (PUT)")
        UC_LRN_02_2("LRN-02.2: Xem bảng điểm & Nhận xét cá nhân")
        UC_LRN_03_1("LRN-03.1: Xem báo cáo tổng kết & Tỷ lệ Pass/Fail lớp")
    end

    Student --> UC_LRN_01_2
    Student --> UC_LRN_02_2

    Teacher --> UC_LRN_01_1
    Teacher --> UC_LRN_01_2
    Teacher --> UC_LRN_02_1
    Teacher --> UC_LRN_02_3
    Teacher --> UC_LRN_03_1

    Staff --> UC_LRN_01_1
    Staff --> UC_LRN_01_2
    Staff --> UC_LRN_02_1
    Staff --> UC_LRN_02_3
    Staff --> UC_LRN_03_1

    Admin --> UC_LRN_01_1
    Admin --> UC_LRN_01_2
    Admin --> UC_LRN_02_1
    Admin --> UC_LRN_02_3
    Admin --> UC_LRN_03_1
```



## PHẦN 2: ĐẶC TẢ USE CASE CHI TIẾT

### UC-LRN-01.1: Đăng tải & Xóa học liệu số

* **Actor:** Teacher, Staff, Admin.
* **Luồng chính:** Chọn lớp (`class_id`), nhập tiêu đề, chọn loại (`DOCUMENT`, `VIDEO`, `EXERCISE`, `SLIDE`), nhập `file_url` → Hệ thống kiểm tra `file_url` khác rỗng (`INV-LRN-04`) → Xác thực lớp qua Admission Service → Lưu vào bảng `learning.materials`.
* **Ngoại lệ:** `file_url` rỗng → Trả lỗi `400 LRN_EMPTY_FILE_URL`; Giáo viên upload vào lớp không dạy → `403 LRN_FORBIDDEN_CLASS`.

### UC-LRN-02.1: Nhập điểm trọng số (POST)

* **Actor:** Teacher, Staff, Admin.
* **Luồng chính:** Chọn lớp, chọn học viên (`student_id`), nhập tên bài kiểm tra (`assessment_name`), `score`, `max_score`, `weight`, `feedback` → Kiểm tra `0 <= score <= max_score` (`INV-LRN-01`) → Gọi Admission kiểm tra `teacher_id` (`INV-LRN-03`) → Gọi Student kiểm tra học viên đang `STUDYING` → Lưu vào `learning.grades`.
* **Ngoại lệ:** Điểm âm hoặc vượt `max_score` → `400 LRN_INVALID_SCORE`; Trùng đầu điểm → `409 LRN_DUPLICATE_ASSESSMENT`; Sai giáo viên → `403 LRN_FORBIDDEN_CLASS`.

### UC-LRN-02.3: Cập nhật điểm đã chấm (PUT)

* **Actor:** Teacher (dạy lớp), Staff, Admin.
* **Luồng chính:** Chọn bản ghi điểm theo `grade_id` → Nhập giá trị mới (`score`, `max_score`, `weight`, `feedback`) → Kiểm tra `0 <= score <= max_score` (`INV-LRN-01`) → Nếu `role == TEACHER`, gọi Admission kiểm tra `teacher_id == jwt.sub` (`INV-LRN-03`) → Cập nhật `learning.grades`.
* **Ngoại lệ:** Điểm âm hoặc vượt max → `400 LRN_INVALID_SCORE`; `weight` ngoài khoảng → `400 LRN_INVALID_WEIGHT`; Sai giáo viên → `403 LRN_FORBIDDEN_CLASS`; Không tìm thấy `grade_id` → `404 LRN_GRADE_NOT_FOUND`.

### UC-LRN-02.2: Xem bảng điểm & Nhận xét cá nhân

* **Actor:** Student.
* **Luồng chính:** Học viên mở trang Bảng điểm → Hệ thống trích xuất `user_id` từ JWT → Ánh xạ sang `student.students.id` (`R-ID-1`) → Trả về danh sách điểm và kết quả tổng kết từ `v_student_grade_summary` của riêng học viên đó (`INV-LRN-02`).
* **Ngoại lệ:** Nếu không tìm thấy `student.students.id` tương ứng → trả danh sách rỗng (không lỗi 404 vì user có thể chưa được sync sang Student).

### UC-LRN-03.1: Xem báo cáo tổng kết & Tỷ lệ Pass/Fail lớp

* **Actor:** Teacher, Staff, Admin.
* **Luồng chính:** Chọn `class_id` → Hệ thống truy vấn `learning.v_class_pass_rate` và `learning.v_student_grade_summary` → Trả về sĩ số có điểm, số lượng Pass/Fail, tỷ lệ đỗ (%) và bảng xếp hạng điểm chuẩn hóa thang 100%.

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
        LrnSvc-->>UI: 400 Bad Request (LRN_INVALID_SCORE / LRN_INVALID_WEIGHT)
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

### 3.3. Luồng Cập nhật điểm (PUT) — Kiểm tra quyền & tương thích INV-LRN-01/03

```mermaid
sequenceDiagram
    autonumber
    actor User as Giảng viên / Staff / Admin
    participant UI as Frontend
    participant LrnSvc as Learning Service (:8083)
    participant AdmSvc as Admission Service (:8081)
    participant LrnDB as PostgreSQL (Schema: learning)

    User->>UI: Sửa điểm (grade_id, score, max_score, weight, feedback)
    UI->>LrnSvc: PUT /api/learning/grades/{grade_id} (Bearer JWT)
    LrnSvc->>LrnDB: SELECT * FROM learning.grades WHERE id = {grade_id}

    alt Không tìm thấy
        LrnDB-->>LrnSvc: empty
        LrnSvc-->>UI: 404 Not Found (LRN_GRADE_NOT_FOUND)
    else Tìm thấy
        LrnDB-->>LrnSvc: { class_id, student_id, ... }
        LrnSvc->>LrnSvc: Kiểm tra INV-LRN-01 (0 <= score <= max_score) & 0 < weight <= 1

        alt Điểm / weight không hợp lệ
            LrnSvc-->>UI: 400 (LRN_INVALID_SCORE / LRN_INVALID_WEIGHT)
        else Hợp lệ
            LrnSvc->>AdmSvc: GET /api/admission/internal/classes/{class_id}
            AdmSvc-->>LrnSvc: { teacher_id, status }

            alt role == TEACHER và teacher_id != jwt.sub (INV-LRN-03)
                LrnSvc-->>UI: 403 Forbidden (LRN_FORBIDDEN_CLASS)
            else Hợp lệ (đúng GV, hoặc STAFF/ADMIN)
                LrnSvc->>LrnDB: UPDATE learning.grades SET ... WHERE id = {grade_id}
                LrnDB-->>LrnSvc: OK
                LrnSvc-->>UI: 200 OK {success: true, data: grade}
            end
        end
    end
```

## PHẦN 4: SƠ ĐỒ HOẠT ĐỘNG (ACTIVITY DIAGRAMS)

### 4.1. Luồng Chấm điểm & Tính Kết quả Tổng kết Chuẩn hóa Thang 100%

```mermaid
flowchart TD
    Start([Bắt đầu]) --> InputGrade[Giảng viên gửi request chấm điểm]
    InputGrade --> ValidateScore{0 <= score <= max_score\n& 0 < weight <= 1?}
    ValidateScore -- Sai --> ErrScore[Trả lỗi 400 LRN_INVALID_SCORE / LRN_INVALID_WEIGHT] --> End([Kết thúc])
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

### 4.2. Luồng Cập nhật điểm (PUT)

```mermaid
flowchart TD
    Start([Bắt đầu]) --> FindGrade[SELECT grade theo grade_id]
    FindGrade --> Exists{Tồn tại?}
    Exists -- Không --> ErrNotFound[404 LRN_GRADE_NOT_FOUND] --> End([Kết thúc])
    Exists -- Có --> ValidateScore{0 <= score <= max_score\n& 0 < weight <= 1?}
    ValidateScore -- Sai --> ErrScore[400 LRN_INVALID_SCORE / LRN_INVALID_WEIGHT] --> End
    ValidateScore -- Đúng --> CheckRole{role == TEACHER?}
    CheckRole -- Có --> CallAdm[GET admission /internal/classes/{class_id}]
    CallAdm --> MatchTeacher{teacher_id == jwt.sub?}
    MatchTeacher -- Không --> ErrForbidden[403 LRN_FORBIDDEN_CLASS] --> End
    MatchTeacher -- Có --> UpdateDB[UPDATE learning.grades]
    CheckRole -- STAFF/ADMIN --> UpdateDB
    UpdateDB --> UpdateView[View tự tính lại average_percentage] --> End
```

### 4.3. Luồng Upload học liệu

```mermaid
flowchart TD
    Start([Bắt đầu]) --> InputMat[Nhận request tạo học liệu]
    InputMat --> ValidateURL{file_url trim != '' ?}
    ValidateURL -- Không --> ErrURL[400 LRN_EMPTY_FILE_URL] --> End([Kết thúc])
    ValidateURL -- Có --> CheckRole{role == TEACHER?}
    CheckRole -- Có --> CallAdm[GET admission /internal/classes/{class_id}]
    CallAdm --> MatchTeacher{teacher_id == jwt.sub?}
    MatchTeacher -- Không --> ErrForbidden[403 LRN_FORBIDDEN_CLASS] --> End
    MatchTeacher -- Có --> InsertMat[INSERT learning.materials]
    CheckRole -- STAFF/ADMIN --> InsertMat
    InsertMat --> Response[201 Created] --> End
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
        numeric weight "DEFAULT 1.0, (0, 1] | NULL"
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
