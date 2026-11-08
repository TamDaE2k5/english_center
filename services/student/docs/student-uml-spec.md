# ĐẶC TẢ SƠ ĐỒ UML - PHÂN HỆ QUẢN LÝ HỌC VIÊN (STUDENT)

Tài liệu này mô tả trực quan kiến trúc, luồng nghiệp vụ và cấu trúc dữ liệu của Phân hệ Học viên.

## PHẦN 1: SƠ ĐỒ USE CASE (USE CASE DIAGRAM)
*Mô tả sự tương tác giữa các tác nhân và các chức năng của hệ thống.*

```mermaid
flowchart LR
    %% Actors Definition
    Admin([Quản trị viên / Admin])
    Staff([Giáo vụ - Staff])
    Teacher([Giảng viên - Teacher])
    Student([Học viên - Student])

    subgraph Boundary_Student_Service ["Phân hệ Quản lý Học viên"]
        %% STU-01
        UC_STU_01_1("STU-01.1: Tạo mới hồ sơ")
        UC_STU_01_2("STU-01.2: Cập nhật & Tra cứu")
        UC_STU_01_3("STU-01.3: Chuyển trạng thái (Soft-delete)")
        
        %% STU-02
        UC_STU_02_1("STU-02.1: Lấy DS học viên lớp")
        UC_STU_02_2("STU-02.2: Điểm danh 1-chạm")
        UC_STU_02_3("STU-02.3: Xem lịch sử chuyên cần")
        
        %% STU-03
        UC_STU_03_1("STU-03.1: Gửi yêu cầu học vụ")
        UC_STU_03_2("STU-03.2: Duyệt/Từ chối yêu cầu")
    end

    %% Connections
    Student --> UC_STU_01_2
    Student --> UC_STU_02_3
    Student --> UC_STU_03_1

    Teacher --> UC_STU_02_1
    Teacher --> UC_STU_02_2
    Teacher --> UC_STU_02_3

    Staff --> UC_STU_01_1
    Staff --> UC_STU_01_2
    Staff --> UC_STU_01_3
    Staff --> UC_STU_02_1
    Staff --> UC_STU_02_2
    Staff --> UC_STU_03_2

    Admin --> UC_STU_01_1
    Admin --> UC_STU_01_2
    Admin --> UC_STU_01_3
    Admin --> UC_STU_03_2
```

## PHẦN 2: ĐẶC TẢ USE CASE CHI TIẾT 

### UC-STU-01.1: Tạo mới hồ sơ
* Actor: Staff, Admin.
* Luồng chính: Nhập thông tin -> Hệ thống tự sinh student_code (HV-2026-xxxx) -> Lưu DB với status STUDYING.
* Ngoại lệ: Trùng Email/SĐT -> Trả về lỗi HTTP 400.

### UC-STU-01.2: Cập nhật & Tra cứu
* Actor: Staff, Học viên.
* Luồng chính: Tìm kiếm theo Mã/Tên/SĐT -> Hiển thị kết quả -> Cập nhật thông tin -> Ghi nhận updated_at. Học viên chỉ được sửa hồ sơ của mình.

### UC-STU-02.1: Lấy DS học viên trong lớp
* Actor: Teacher, Staff.
* Luồng chính: Student Service gọi API nội bộ sang Admission Service (GET /api/v1/internal/enrollments?class_id={id}&status=APPROVED) để lấy danh sách chính thức.

### UC-STU-02.2: Điểm danh 1-chạm
* Actor: Teacher, Staff.
* Luồng chính: Chọn lớp -> Hệ thống load DS -> Chọn trạng thái (PRESENT, ABSENT, LATE, EXCUSED) -> Lưu.
* Ngoại lệ: Vi phạm UNIQUE(class_id, student_id, session_date) -> Báo lỗi HTTP 400 ATTENDANCE_DUPLICATE.

### UC-STU-03.1 & 03.2: Yêu cầu học vụ (Bảo lưu/Rút hồ sơ)
* Actor: Học viên (gửi), Staff (duyệt).
* Luồng chính: Học viên gửi đơn (RESERVE, DROP) -> PENDING. Staff duyệt -> APPROVED -> Tự động UPDATE trạng thái bên bảng students thành RESERVED hoặc DROPPED.

## PHẦN 3: SƠ ĐỒ TUẦN TỰ (SEQUENCE DIAGRAMS)
### 3.1. Luồng Lấy danh sách học viên (Giao tiếp Microservices)

*Mô tả luồng tương tác giữa Giảng viên, Frontend, Student Service và Admission Service khi lấy danh sách học viên thuộc một lớp để thực hiện điểm danh.*

```mermaid
sequenceDiagram
    autonumber
    actor Teacher as Giảng viên
    participant UI as Frontend
    participant StuSvc as Student Service
    participant AdmSvc as Admission Service
    participant AdmDB as Admission DB

    Teacher->>UI: Chọn lớp học (class_id)
    UI->>StuSvc: GET /api/v1/classes/{class_id}/members
    Note over StuSvc,AdmSvc: Gọi REST API nội bộ (Không query chéo DB)
    StuSvc->>AdmSvc: GET /api/v1/internal/enrollments?class_id={id}&status=APPROVED
    AdmSvc->>AdmDB: SELECT danh sách enrollments đã APPROVED
    AdmDB-->>AdmSvc: Trả về danh sách Enrollments
    AdmSvc-->>StuSvc: 200 OK (JSON danh sách học viên)
    StuSvc-->>UI: 200 OK (Danh sách để điểm danh)
    UI-->>Teacher: Hiển thị bảng điểm danh
```

### 3.2. Luồng Điểm danh 1-chạm (Xử lý Lưu & Chặn trùng)

*Mô tả luồng giảng viên lưu điểm danh cho cả lớp, hệ thống kiểm tra dữ liệu trùng lặp trước khi ghi nhận.*

```mermaid
sequenceDiagram
    autonumber
    actor Teacher as Giảng viên
    participant UI as Frontend
    participant StuSvc as Student Service
    participant StuDB as Student DB

    Teacher->>UI: Điều chỉnh trạng thái và nhấn "Lưu điểm danh"
    UI->>StuSvc: POST /api/v1/attendance (class_id, session_date, list_students)

    loop Với từng học viên trong danh sách
        StuSvc->>StuDB: INSERT attendance (class_id, student_id, session_date, status)

        alt Vi phạm UNIQUE(class_id, student_id, session_date)
            StuDB-->>StuSvc: PostgreSQL UNIQUE Constraint Violation
            StuSvc-->>UI: 400 Bad Request (ATTENDANCE_DUPLICATE)
            UI-->>Teacher: Hiển thị thông báo "Lớp này đã được điểm danh hôm nay!"
        else Thêm mới hợp lệ
            StuDB-->>StuSvc: Insert Success (ID bản ghi mới)
        end
    end

    StuSvc-->>UI: 201 Created (Điểm danh hoàn tất)
    UI-->>Teacher: Thông báo điểm danh thành công
```

### 3.3. Luồng Phê duyệt Yêu cầu Học vụ (Bảo lưu / Rút hồ sơ)

*Mô tả quá trình Giáo vụ phê duyệt yêu cầu học vụ của học viên. Hệ thống cập nhật trạng thái yêu cầu và trạng thái tương ứng của học viên trong cơ sở dữ liệu.*

```mermaid
sequenceDiagram
    autonumber
    actor Staff as Giáo vụ
    participant UI as Frontend
    participant StuSvc as Student Service
    participant StuDB as Student DB

    Staff->>UI: Chọn yêu cầu (request_id) và nhấn "Phê duyệt"
    UI->>StuSvc: PUT /api/v1/status-requests/{request_id}/approve

    StuSvc->>StuDB: UPDATE status_requests SET status = 'APPROVED' WHERE id = {request_id}

    alt Yêu cầu bảo lưu (RESERVE)
        StuSvc->>StuDB: UPDATE students SET status = 'RESERVED' WHERE id = {student_id}
    else Yêu cầu rút hồ sơ (DROP)
        StuSvc->>StuDB: UPDATE students SET status = 'DROPPED' WHERE id = {student_id}
    end

    StuDB-->>StuSvc: Transaction Committed Successfully
    StuSvc-->>UI: 200 OK (Phê duyệt thành công)
    UI-->>Staff: Hiển thị thông báo cập nhật trạng thái thành công
```

## PHẦN 4: SƠ ĐỒ HOẠT ĐỘNG (ACTIVITY DIAGRAMS)
### 4.1. Luồng Điểm danh Điện tử

*Mô tả quy trình giảng viên thực hiện điểm danh học viên trong một lớp học, từ lúc chọn lớp, tải danh sách học viên đến khi lưu kết quả điểm danh vào hệ thống.*

```mermaid
flowchart TD
    Start([Bắt đầu]) --> SelectClass[Giảng viên chọn lớp học class_id]

    SelectClass --> FetchMembers[Hệ thống gọi API nội bộ sang Admission Service]

    FetchMembers --> CheckStatus{Có danh sách học viên APPROVED?}

    CheckStatus -- Không --> EmptyList[Hiển thị thông báo lớp chưa có học viên]
    EmptyList --> End([Kết thúc])

    CheckStatus -- Có --> RenderList[Hiển thị danh sách học viên với trạng thái mặc định PRESENT]

    RenderList --> MarkAttendance[Giảng viên điều chỉnh trạng thái ABSENT / LATE / EXCUSED]

    MarkAttendance --> Submit[Giảng viên nhấn Lưu điểm danh]

    Submit --> CheckConstraint{Kiểm tra ràng buộc UNIQUE}

    CheckConstraint -- Trùng lặp --> ErrorReturn[Trả về lỗi HTTP 400 ATTENDANCE_DUPLICATE]
    ErrorReturn --> End

    CheckConstraint -- Hợp lệ --> SaveDB[Lưu bản ghi vào DB student.attendance]

    SaveDB --> SuccessMsg[Thông báo điểm danh thành công]
    SuccessMsg --> End
```

### 4.2. Luồng Xử lý Yêu cầu Bảo lưu / Rút hồ sơ

*Mô tả quy trình học viên gửi yêu cầu bảo lưu hoặc rút hồ sơ, giáo vụ tiếp nhận và đưa ra quyết định phê duyệt hoặc từ chối. Nếu được phê duyệt, hệ thống cập nhật trạng thái tương ứng của học viên.*

```mermaid
flowchart TD
    Start([Bắt đầu]) --> StudentSubmit[Học viên gửi yêu cầu học vụ kèm lý do]

    StudentSubmit --> CreatePending[Tạo bản ghi trong status_requests với status = PENDING]

    CreatePending --> StaffView[Giáo vụ mở danh sách yêu cầu PENDING]

    StaffView --> StaffDecision{Giáo vụ ra quyết định}

    StaffDecision -- Từ chối --> RejectRequest[Cập nhật status = REJECTED]
    RejectRequest --> NotifyStudent[Thông báo từ chối tới học viên]
    NotifyStudent --> End([Kết thúc])

    StaffDecision -- Phê duyệt --> ApproveRequest[Cập nhật status = APPROVED]

    ApproveRequest --> CheckType{Loại yêu cầu là gì?}

    CheckType -- Bảo lưu RESERVE --> UpdateReserved[Cập nhật students status = RESERVED]

    CheckType -- Rút hồ sơ DROP --> UpdateDropped[Cập nhật students status = DROPPED]

    UpdateReserved --> SyncSuccess[Thông báo phê duyệt thành công]
    UpdateDropped --> SyncSuccess

    SyncSuccess --> End
```

## PHẦN 5: LƯỢC ĐỒ CƠ SỞ DỮ LIỆU (ERD)

*Sơ đồ ERD mô tả cấu trúc dữ liệu của Student Service, bao gồm ba bảng chính: STUDENTS, ATTENDANCE và STATUS_REQUESTS, cùng mối quan hệ giữa chúng.*

```mermaid
erDiagram
    STUDENTS ||--o{ ATTENDANCE : "có lịch sử chuyên cần"
    STUDENTS ||--o{ STATUS_REQUESTS : "khởi tạo yêu cầu"

    STUDENTS {
        int id PK
        int user_id "Liên kết admission.users"
        string student_code UK "Mã định danh HV-2026-xxxx"
        string full_name
        string email UK
        string phone
        date date_of_birth
        text address
        string status "STUDYING | RESERVED | DROPPED | GRADUATED"
        timestamptz created_at
        timestamptz updated_at
    }

    ATTENDANCE {
        int id PK
        int class_id "Liên kết admission.classes"
        int student_id FK
        date session_date
        string status "PRESENT | ABSENT | LATE | EXCUSED"
        text notes
        timestamptz created_at
    }

    STATUS_REQUESTS {
        int id PK
        int student_id FK
        string request_type "RESERVE | TRANSFER | DROP"
        text reason
        string status "PENDING | APPROVED | REJECTED"
        timestamptz created_at
    }
```
