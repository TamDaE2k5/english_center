# Danh mục Tài liệu Chính thức & Cây Cấu trúc Dự án — English Center Management System

> **Phiên bản**: Final 1.0 (Bản hợp nhất cuối cùng của Nhóm)
> **Kiến trúc**: Polyglot Microservices — 1 PostgreSQL 16 Instance với 4 Schemas độc lập (`admission`, `student`, `learning`, `finance`).

---

## 1. Hướng dẫn Tra cứu Nhanh cho 4 Nhóm

1. **Khởi tạo CSDL chuẩn (28 bảng, 4 views, 342 cột, 35 triggers + Seed Data)**: Chạy [`scripts/init-db.sql`](../scripts/init-db.sql) (tự động nạp khi chạy `docker compose up -d postgres`).
2. **Xem Thiết kế tổng thể 4 Schemas, Sơ đồ Mermaid & Quy tắc gọi API liên service**: Mở [**`docs/MASTER_DB_AND_API_RULES.md`**](MASTER_DB_AND_API_RULES.md).
3. **Xem Chỉ mục Từ điển Dữ liệu & Bản đồ 24 liên kết Logical FK toàn hệ thống**: Mở [**`docs/database/DATA_DICTIONARY_INDEX.md`**](database/DATA_DICTIONARY_INDEX.md).
4. **Mỗi nhóm tra cứu chi tiết từng bảng, từng cột, từng ràng buộc & API In/Out của phân hệ mình**:
   - 🏛️ Nhóm 1 (`admission`): [**`services/admission/docs/admission-schema-spec.md`**](../services/admission/docs/admission-schema-spec.md)
   - 🎓 Nhóm 2 (`student`): [**`services/student/docs/student-schema-spec.md`**](../services/student/docs/student-schema-spec.md)
   - 📚 Nhóm 3 (`learning`): [**`services/learning/docs/learning-schema-spec.md`**](../services/learning/docs/learning-schema-spec.md)
   - 💰 Nhóm 4 (`finance`): [**`services/finance/docs/finance-schema-spec.md`**](../services/finance/docs/finance-schema-spec.md)

---

## 2. Cây Thư mục Toàn Dự án & Chức năng Từng File (`english_center/`)

```text
english_center/
├── docker-compose.yml                         # Khởi chạy toàn bộ hệ thống (Postgres 16, 4 Microservices, Nginx Gateway :8080)
├── README.md                                  # Hướng dẫn cài đặt, chạy dự án và danh sách tài khoản mẫu (Admin/Staff/Teacher/Student)
│
├── scripts/
│   ├── init-db.sql                            # ⭐ [FILE CỐT LÕI] Master DDL khởi tạo 4 Schemas (28 bảng, 4 views, 342 cột, 35 triggers, 3 sequences),
│   │                                          #    từ điển COMMENT ON 100% cột và Seed Data chuẩn BCrypt (đã test sống trên PostgreSQL 16)
│   └── verify.sh                              # Script chạy kiểm tra tự động hệ thống và bộ test bất biến (Invariants)
│
├── gateway/
│   └── nginx.conf                             # Cấu hình API Gateway (:8080) định tuyến /auth, /api/admission (8081),
│                                              # /api/student (8082), /api/learning (8083), /api/finance (8084)
│
├── docs/                                      # 📚 THƯ MỤC TÀI LIỆU CHUNG CỦA TOÀN DỰ ÁN (CẤP HỆ THỐNG)
│   ├── README.md                              # ⭐ Mục lục điều hướng trung tâm & cây cấu trúc chức năng từng file trong dự án
│   ├── StRS.md                                # ⭐ [ĐẶC TẢ CÁC BÊN LIÊN QUAN] Stakeholder Requirements Specification (ISO/IEC/IEEE 29148) cho cả dự án
│   ├── MASTER_DB_AND_API_RULES.md             # ⭐ [TÀI LIỆU MASTER CỦA LEADER] Thiết kế tổng thể 4 Schemas, sơ đồ Mermaid kiến trúc,

│   │                                          #    quy ước định danh R-ID-1 (student_id), State Machines, và Quy tắc gọi REST API/Webhook liên service
│   ├── contract.md                            # Hợp đồng kỹ thuật chung: tóm tắt 28 bảng, cấu trúc JSON snake_case, RBAC 4 vai trò
│   ├── invariants.md                          # Đặc tả 15 Bất biến Hệ thống (INV-SYS, INV-SEC, INV-ADM, INV-STU, INV-LRN, INV-FIN)
│   ├── architecture.md                        # Mô tả kiến trúc Polyglot Microservices và cơ chế cô lập Database theo Schema
│   ├── api-conventions.md                     # Quy chuẩn thiết kế RESTful API, phân trang, bọc dữ liệu trả về và mã lỗi chuẩn
│   ├── spec.md                                # Đặc tả yêu cầu nghiệp vụ tổng quan ban đầu của dự án
│   ├── design-system.md                       # Quy chuẩn giao diện người dùng (màu sắc, font chữ, layout, components)
│   ├── git-workflow.md                        # Quy trình làm việc với Git (nhánh main, dev, feature/<module>) và quy tắc Merge/PR
│   ├── agent-workflow.md                      # Hướng dẫn quy trình làm việc chuẩn khi phát triển từng task
│   ├── state.md                               # Trạng thái hiện tại của dự án qua từng giai đoạn
│   ├── worklog.md                             # Nhật ký công việc chung của toàn dự án
│   │
│   ├── database/
│   │   └── DATA_DICTIONARY_INDEX.md           # ⭐ Bảng chỉ mục Từ điển Dữ liệu toàn hệ thống: thống kê 28 bảng, 4 views, 342 cột,
│   │                                          #    35 triggers và Bản đồ 24 liên kết Logical FK xuyên giữa 4 schemas
│   │
│   ├── decisions/                             # Hồ sơ Quyết định Kiến trúc (ADR - Architecture Decision Records)
│   │   ├── 001-polyglot-microservices.md      # Quyết định chọn Java Spring Boot (Admission) + Python FastAPI (Student/Learning/Finance)
│   │   ├── 002-nginx-gateway-and-static-mount.md # Quyết định dùng Nginx làm Reverse Proxy & phục vụ giao diện Frontend
│   │   ├── 003-stateless-jwt-authentication.md   # Quyết định xác thực tập trung qua Stateless JWT (HS256) do Admission cấp
│   │   ├── 004-api-snake-case-convention.md      # Quyết định thống nhất chuẩn đặt tên JSON snake_case giữa Java và Python
│   │   └── 005-invariant-testing-strategy.md     # Quyết định kiểm thử tự động các bất biến kiến trúc bằng Python unittest
│   │
│   ├── tasks/                                 # Quản lý backlog cấp dự án
│   │   ├── README.md                          # Hướng dẫn quản lý task chung
│   │   ├── backlog.md                         # Danh sách tính năng tổng thể
│   │   └── active/TASK-W1-ADM-01.md           # Mẫu phiếu giao việc (Task Card)
│   │
│   └── archive/
│       └── dac_ta_4_phan_he_va_chot_csdl.md   # Bản nháp phân tích tổng hợp 4 phân hệ ban đầu (lưu trữ tham khảo)
│
├── services/                                  # 🧩 4 PHÂN HỆ MICROSERVICES ĐỘC LẬP
│   │
│   ├── admission/                             # 🏛️ PHÂN HỆ 1: TUYỂN SINH, KHẢO THÍ & ĐỊNH DANH (Java Spring Boot - Port 8081)
│   │   ├── docs/
│   │   │   ├── README.md                      # Mục lục tài liệu của riêng phân hệ Admission
│   │   │   ├── admission-schema-spec.md       # ⭐ Đặc tả CSDL chính thức Schema `admission` (14 bảng, 167 cột, 19 triggers, ERD, API In/Out)
│   │   │   ├── admission-ba-bi-spec.md        # Đặc tả Nghiệp vụ (BA) & Báo cáo Thông minh (BI) phân hệ Admission (bản chốt v3.6)
│   │   │   ├── admission-tech-spec.md         # Đặc tả Kỹ thuật, Bảo mật JWT, Khóa bi quan giữ chỗ 48h & Xử lý xung đột lịch
│   │   │   ├── admission-uml-spec.md          # Toàn bộ sơ đồ UML của Admission (Use Case, Activity, Sequence, State, Class, ERD)
│   │   │   ├── tong_hop_admission_tinh_nang_va_schema.md # Bản tóm tắt nhanh tính năng và các bảng của Admission
│   │   │   └── tasks/README.md                # Đường dẫn trỏ sang thư mục quản lý task `services/admission/tasks/`
│   │   ├── tasks/                             # Hệ thống 42 file chia việc chi tiết của nhóm Admission (Phase 0 -> Phase 6, API_CATALOG, CHECKLIST)
│   │   ├── backend/                           # Source code Java 21 Spring Boot (`pom.xml`, `src/...`)
│   │   └── frontend/                          # Giao diện Web của phân hệ Admission
│   │
│   ├── student/                               # 🎓 PHÂN HỆ 2: HỒ SƠ HỌC VIÊN, ĐIỂM DANH & HỌC VỤ (Python FastAPI - Port 8082)
│   │   ├── docs/
│   │   │   ├── README.md                      # Mục lục tài liệu của riêng phân hệ Student
│   │   │   ├── student-schema-spec.md         # ⭐ Đặc tả CSDL chính thức Schema `student` (3 bảng, 31 cột, sequence HV-YYYY-NNNN, ERD, API In/Out)
│   │   │   ├── student-ba-bi-spec.md          # Đặc tả Nghiệp vụ (BA): Quản lý hồ sơ, Điểm danh, Bảo lưu/Chuyển lớp/Bỏ học
│   │   │   ├── student-tech-spec.md           # Đặc tả Kỹ thuật & Thiết kế API (`/api/student/...` và alias `/api/v1/...`)
│   │   │   └── student-uml-spec.md            # Sơ đồ UML của phân hệ Student (Use Case, Sequence, Activity, State)
│   │   ├── backend/                           # Source code Python FastAPI (`main.py`, `models.py`, `database.py`)
│   │   └── frontend/                          # Giao diện Web của phân hệ Student
│   │
│   ├── learning/                              # 📚 PHÂN HỆ 3: HỌC LIỆU, BẢNG ĐIỂM & KẾT QUẢ HỌC TẬP (Python FastAPI - Port 8083)
│   │   ├── docs/
│   │   │   ├── README.md                      # Mục lục tài liệu của riêng phân hệ Learning
│   │   │   ├── learning-schema-spec.md        # ⭐ Đặc tả CSDL chính thức Schema `learning` (2 bảng, 2 views điểm tổng kết & tỷ lệ đỗ, ERD, API In/Out)
│   │   │   └── tong_hop_learning_tinh_nang_va_schema.md # Đặc tả chi tiết tính năng Học liệu, Bảng điểm trọng số và công thức chuẩn hóa thang 100%
│   │   ├── backend/                           # Source code Python FastAPI (`app/main.py`, `models.py`, `schemas.py`, `routers/grades.py`, `materials.py`)
│   │   └── frontend/                          # Giao diện Web của phân hệ Learning
│   │
│   └── finance/                               # 💰 PHÂN HỆ 4: BIỂU PHÍ, HÓA ĐƠN, VIETQR, VÍ BẢO LƯU & HOÀN TIỀN (Python FastAPI - Port 8084)
│       ├── docs/
│       │   ├── README.md                      # Mục lục tài liệu của riêng phân hệ Finance
│       │   ├── finance-schema-spec.md         # ⭐ Đặc tả CSDL chính thức Schema `finance` (9 bảng, 2 BI views, 126 cột, 12 triggers, ERD, API In/Out)
│       │   ├── finance-ba-bi-spec.md          # Đặc tả Nghiệp vụ (BA) & Báo cáo BI Doanh thu thực thu / Chiến dịch Voucher
│       │   ├── finance-tech-spec.md           # Đặc tả Kỹ thuật: Transaction, Khóa bi quan Voucher, Idempotency VietQR, Hoàn tiền 3 cấp (Port 8084)
│       │   ├── finance-uml-spec.md            # Toàn bộ sơ đồ UML của Finance (Use Case, Activity, Sequence, State, Class, ERD)
│       │   ├── tong_hop_finance_tinh_nang_va_schema.md # Bản tóm tắt nhanh tính năng và thiết kế 9 bảng của Finance
│       │   └── tasks/README.md                # Đường dẫn trỏ sang thư mục quản lý task `services/finance/tasks/`
│       ├── tasks/                             # Hệ thống 41 file chia việc chi tiết của nhóm Finance (Phase 0 -> Phase 7, API_CATALOG, CHECKLIST)
│       ├── backend/                           # Source code Python FastAPI (`main.py`, `models.py`, `database.py`)
│       └── frontend/                          # Giao diện Web của phân hệ Finance
│
└── tests/
    └── invariants/                            # 🛡️ BỘ KIỂM THỬ BẤT BIẾN TỰ ĐỘNG (15 bài test kiểm tra quy tắc kiến trúc & nghiệp vụ)
        ├── test_admission_invariants.py       # Kiểm tra INV-ADM-01..03 (sĩ số lớp, mã khóa học, state machine đơn đăng ký)
        ├── test_student_invariants.py         # Kiểm tra INV-STU-01..02 (chống điểm danh trùng, trạng thái học vụ)
        ├── test_learning_invariants.py        # Kiểm tra INV-LRN-01..02 (ràng buộc 0 <= score <= max_score, cô lập bảng điểm học viên)
        ├── test_finance_invariants.py         # Kiểm tra INV-FIN-01..03 (số tiền > 0, chống trùng giao dịch, điều kiện hóa đơn PAID)
        ├── test_security_invariants.py        # Kiểm tra INV-SEC-01..03 (quyền truy cập hồ sơ, ranh giới chấm điểm của GV, không lộ secret)
        └── test_system_invariants.py          # Kiểm tra INV-SYS-01..02 (chuẩn snake_case và bắt buộc 4 schemas độc lập trong init-db.sql)
```

---

## 3. Danh mục Tài liệu Nền tảng & Hợp đồng Chung (`docs/`)

| Tài liệu | Mô tả |
|---|---|
| [**`StRS.md`**](StRS.md) | **Đặc tả Yêu cầu Các bên Liên quan (StRS)**: Phân tích bài toán, 8 nhóm Stakeholder, 17 yêu cầu cấp cao xuyên 4 phân hệ và Ma trận Truy vết toàn dự án theo chuẩn ISO/IEC/IEEE 29148. |
| [**`MASTER_DB_AND_API_RULES.md`**](MASTER_DB_AND_API_RULES.md) | **Tài liệu Master**: Thiết kế chi tiết 4 Schemas (28 bảng, 4 views), Sơ đồ Mermaid & Quy tắc gọi API liên service (Inter-Service REST & Webhook Rules). |

| [**`database/DATA_DICTIONARY_INDEX.md`**](database/DATA_DICTIONARY_INDEX.md) | **Chỉ mục Từ điển Dữ liệu**: Bảng tổng hợp 28 bảng, 4 views, 342 cột, 35 triggers và bản đồ 24 liên kết Logical FK toàn hệ thống. |
| [**`contract.md`**](contract.md) | Hợp đồng kỹ thuật, quy ước định danh `R-ID-1`, vòng đời đối tượng (State Machines) và phân quyền RBAC. |
| [**`invariants.md`**](invariants.md) | 15 Bất biến Hệ thống (System Invariants) và cơ chế kiểm thử tự động trong `tests/invariants/`. |
| [**`architecture.md`**](architecture.md) | Kiến trúc tổng thể Polyglot Microservices và định tuyến Nginx API Gateway (`:8080`). |
| [**`api-conventions.md`**](api-conventions.md) | Quy chuẩn RESTful API, JSON `snake_case`, phân trang và mã lỗi chuẩn. |
| [**`spec.md`**](spec.md) | Đặc tả yêu cầu nghiệp vụ tổng quan của dự án. |
| [**`design-system.md`**](design-system.md) | Quy chuẩn giao diện người dùng (UI/UX Design System). |
| [**`git-workflow.md`**](git-workflow.md) | Quy trình phân nhánh Git và gộp code của nhóm. |
| [**`../scripts/init-db.sql`**](../scripts/init-db.sql) | **Master DDL & Seed Script**: File SQL khởi tạo toàn bộ 4 schemas, constraints, triggers, views, comments và dữ liệu mẫu trên PostgreSQL 16. |

---

## 4. Danh mục Tài liệu Chuyên sâu theo từng Phân hệ (`services/<module>/docs/`)

### 🏛️ Phân hệ 1: Tuyển sinh, Khảo thí & Định danh (`admission` — Port `8081`)
- [Danh mục tài liệu Admission (`services/admission/docs/README.md`)](../services/admission/docs/README.md)
- [**Đặc tả CSDL chính thức (`admission-schema-spec.md`)**](../services/admission/docs/admission-schema-spec.md) — 14 bảng, 167 cột, 19 triggers
- [Đặc tả Nghiệp vụ BA & BI (`admission-ba-bi-spec.md`)](../services/admission/docs/admission-ba-bi-spec.md)
- [Đặc tả Kỹ thuật (`admission-tech-spec.md`)](../services/admission/docs/admission-tech-spec.md)
- [Đặc tả Sơ đồ UML (`admission-uml-spec.md`)](../services/admission/docs/admission-uml-spec.md)
- [Tổng hợp Tính năng & Schema (`tong_hop_admission_tinh_nang_va_schema.md`)](../services/admission/docs/tong_hop_admission_tinh_nang_va_schema.md)
- [Danh mục Tasks & 55 API Endpoints (`services/admission/tasks/README.md`)](../services/admission/tasks/README.md)

### 🎓 Phân hệ 2: Quản lý Học viên & Điểm danh (`student` — Port `8082`)
- [Danh mục tài liệu Student (`services/student/docs/README.md`)](../services/student/docs/README.md)
- [**Đặc tả CSDL chính thức (`student-schema-spec.md`)**](../services/student/docs/student-schema-spec.md) — 3 bảng, 31 cột, 3 triggers
- [Đặc tả Nghiệp vụ BA (`student-ba-bi-spec.md`)](../services/student/docs/student-ba-bi-spec.md)
- [Đặc tả Kỹ thuật (`student-tech-spec.md`)](../services/student/docs/student-tech-spec.md)
- [Đặc tả Sơ đồ UML (`student-uml-spec.md`)](../services/student/docs/student-uml-spec.md)

### 📚 Phân hệ 3: Quản lý Học tập & Bảng điểm (`learning` — Port `8083`)
- [Danh mục tài liệu Learning (`services/learning/docs/README.md`)](../services/learning/docs/README.md)
- [**Đặc tả CSDL chính thức (`learning-schema-spec.md`)**](../services/learning/docs/learning-schema-spec.md) — 2 bảng, 2 views, 22 cột, 2 triggers
- [Tổng hợp Tính năng & Schema (`tong_hop_learning_tinh_nang_va_schema.md`)](../services/learning/docs/tong_hop_learning_tinh_nang_va_schema.md)

### 💰 Phân hệ 4: Quản lý Tài chính & Học phí (`finance` — Port `8084`)
- [Danh mục tài liệu Finance (`services/finance/docs/README.md`)](../services/finance/docs/README.md)
- [**Đặc tả CSDL chính thức (`finance-schema-spec.md`)**](../services/finance/docs/finance-schema-spec.md) — 9 bảng, 2 BI views, 126 cột, 12 triggers
- [Đặc tả Nghiệp vụ BA & BI (`finance-ba-bi-spec.md`)](../services/finance/docs/finance-ba-bi-spec.md)
- [Đặc tả Kỹ thuật (`finance-tech-spec.md`)](../services/finance/docs/finance-tech-spec.md)
- [Đặc tả Sơ đồ UML (`finance-uml-spec.md`)](../services/finance/docs/finance-uml-spec.md)
- [Tổng hợp Tính năng & Schema (`tong_hop_finance_tinh_nang_va_schema.md`)](../services/finance/docs/tong_hop_finance_tinh_nang_va_schema.md)
- [Danh mục Tasks & API Endpoints (`services/finance/tasks/README.md`)](../services/finance/tasks/README.md)
