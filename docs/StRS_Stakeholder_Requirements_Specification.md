# StRS – Stakeholder Requirements Specification
# Đặc tả Yêu cầu Các Bên Liên Quan

**Dự án:** Hệ thống Quản lý Trung tâm Anh ngữ (English Center Management System)  
**Mã tài liệu:** `EC-StRS-2026-V1`  
**Phiên bản tài liệu:** 1.0  
**Ngày lập:** 11/11/2026  
**Tham chiếu chuẩn:** ISO/IEC/IEEE 29148:2018 (Systems and software engineering — Life cycle processes — Requirements engineering)  
**Kiến trúc hệ thống:** Polyglot Microservices (Nginx Gateway `:8080`, Admission `:8081`, Student `:8082`, Learning `:8083`, Finance `:8084`, PostgreSQL 16 — 4 Schemas độc lập)  
**Tác giả biên soạn:** Lead BA & User Representatives  
**Đối tượng Đọc giả mục tiêu (Target Audience):** Trưởng phòng, User, Business Analyst  
**Cấp độ trừu tượng:** Trung bình (Mức quy trình nghiệp vụ — User Scenarios, Workflows, Business Rules, Pain Points)  
**Câu hỏi cốt lõi giải quyết:** **"AI CẦN GÌ để làm việc?" (WHO NEEDS WHAT to do their work?)**  

---

> ### 📍 Tuyên bố Vị trí Tài liệu theo Chuẩn IEEE 29148:2018
> Tài liệu **StRS (Stakeholder Requirements Specification)** ghi nhận các **nhu cầu (User Scenarios)**, **kịch bản vận hành (Workflows)**, **quy tắc nghiệp vụ (Business Rules)** và **các điểm đau (Pain Points)** từ **góc nhìn Người dùng & Phòng ban**. StRS nằm ở **tầng giữa** trong tháp đặc tả yêu cầu:
>
> | Tầng | Tài liệu | Câu hỏi | Góc nhìn |
> |------|----------|---------|----------|
> | 🔺 Cao nhất | **BRS** (Business Requirements) | TẠI SAO? (WHY) | Kinh doanh & Chiến lược |
> | 🔷 **Giữa** | **StRS** (Stakeholder Requirements) | **AI CẦN GÌ? (WHO NEEDS WHAT)** | **Người dùng & Phòng ban** |
> | 🔻 Chi tiết | **SRS** (Software Requirements) | HỆ THỐNG PHẢI LÀM GÌ? (WHAT) | Kỹ thuật / Phần mềm |

---

## MỤC LỤC

1. [Giới thiệu & Mục đích Tài liệu (Introduction)](#1-giới-thiệu--mục-đích-tài-liệu-introduction)
2. [Bối cảnh Nghiệp vụ & Đặt vấn đề (Business Context & Problem Statement)](#2-bối-cảnh-nghiệp-vụ--đặt-vấn-đề-business-context--problem-statement)
3. [Hồ sơ Các Bên Liên Quan (Stakeholder Profiles)](#3-hồ-sơ-các-bên-liên-quan-stakeholder-profiles)
4. [Nhu cầu & Yêu cầu Của Các Bên Liên Quan (Stakeholder Needs & Requirements)](#4-nhu-cầu--yêu-cầu-của-các-bên-liên-quan-stakeholder-needs--requirements)
5. [Kịch bản Vận hành Xuyên Phân hệ (Operational Scenarios / End-to-End Value Streams)](#5-kịch-bản-vận-hành-xuyên-phân-hệ-operational-scenarios--end-to-end-value-streams)
6. [Kiến trúc Tổng quan & Mô hình Dữ liệu Hệ thống (System Architecture & Data Model)](#6-kiến-trúc-tổng-quan--mô-hình-dữ-liệu-hệ-thống-system-architecture--data-model)
7. [Máy Trạng thái (State Machines)](#7-máy-trạng-thái-state-machines)
8. [Yêu cầu Chất lượng, Bất biến & Ràng buộc Hệ thống (Quality Attributes, Invariants & Constraints)](#8-yêu-cầu-chất-lượng-bất-biến--ràng-buộc-hệ-thống-quality-attributes-invariants--constraints)
9. [Ma trận Truy vết Yêu cầu Toàn Dự án (End-to-End Traceability Matrix)](#9-ma-trận-truy-vết-yêu-cầu-toàn-dự-án-end-to-end-traceability-matrix)
10. [Tiêu chí Nghiệm thu & Phạm vi Loại trừ (Acceptance Criteria & Explicit Non-Goals)](#10-tiêu-chí-nghiệm-thu--phạm-vi-loại-trừ-acceptance-criteria--explicit-non-goals)
11. [Phụ lục](#11-phụ-lục)

---

## 1. Giới thiệu & Mục đích Tài liệu (Introduction)

### 1.1. Mục đích (Purpose)

Tài liệu **Đặc tả Yêu cầu Các bên Liên quan (StRS - Stakeholder Requirements Specification)** xác định rõ **tại sao (Why)** hệ thống Quản lý Trung tâm Anh ngữ được xây dựng, **ai (Who)** là những bên có quyền lợi/trách nhiệm liên quan, và **những giá trị nghiệp vụ nào (What)** hệ thống bắt buộc phải mang lại cho từng nhóm đối tượng.

Tài liệu StRS mô tả hệ thống ở góc nhìn **"hộp đen" (Black Box)**: tập trung vào **CÁI GÌ** hệ thống phải làm (behavior), kiến trúc tổng thể, giao diện giữa các thành phần và yêu cầu phi chức năng — mà chưa đi sâu vào chi tiết hiện thực phần mềm.

Tài liệu này là tầng giữa trong tháp đặc tả của dự án, làm cơ sở tham chiếu trực tiếp cho:
- **BRS** — Đặc tả Yêu cầu Nghiệp vụ (tầng trên — WHY / Chiến lược).
- **SRS** — Đặc tả Yêu cầu Phần mềm (tầng dưới — HOW / Kỹ thuật chi tiết).
- Đặc tả Yêu cầu Nghiệp vụ & BI của 4 phân hệ.
- Thiết kế CSDL Master & Hợp đồng Giao tiếp Liên Service (`docs/MASTER_DB_AND_API_RULES.md`, `scripts/init-db.sql`).
- Bộ Kiểm thử Bất biến Tự động (`tests/invariants/test_*.py`).

Tài liệu phục vụ: System Architect, Software Architect, Tech Lead, QA Lead, Project Manager, Trưởng phòng các bộ phận và Đại diện người dùng.

### 1.2. Phạm vi Hệ thống (System Scope)

Hệ thống số hóa trọn vẹn vòng đời học viên tại Trung tâm Anh ngữ qua **4 phân hệ Microservices độc lập**:

1. **Phân hệ 1 — Tuyển sinh, Khảo thí & Định danh (`admission-service`, Port `8081`, Schema `admission`)**: Quản lý định danh tập trung (IAM), cơ sở vật chất (phòng học, ca học), danh mục khóa học/lớp học, chống xung đột lịch, khảo thí đầu vào trực tuyến (Placement Test) và giữ chỗ tuyển sinh 48 giờ.
2. **Phân hệ 2 — Quản lý Học viên & Điểm danh (`student-service`, Port `8082`, Schema `student`)**: Quản lý hồ sơ học viên 360 độ (`HV-YYYY-NNNN`), điểm danh điện tử theo buổi học và xử lý yêu cầu học vụ (Bảo lưu, Chuyển lớp, Bỏ học).
3. **Phân hệ 3 — Quản lý Học tập & Bảng điểm (`learning-service`, Port `8083`, Schema `learning`)**: Quản lý kho học liệu số theo lớp học, nhập điểm đánh giá có trọng số và tự động tổng hợp kết quả xếp loại Đạt/Trượt (`Pass`/`Fail`).
4. **Phân hệ 4 — Quản lý Tài chính & Học phí (`finance-service`, Port `8084`, Schema `finance`)**: Quản lý biểu phí, chiến dịch Voucher, tự động phát hành hóa đơn & chia kỳ trả góp, thu tiền mặt tại quầy, đối soát tự động qua Webhook VietQR, quản lý ví bảo lưu nội bộ và phê duyệt hoàn tiền 3 cấp.

Hệ thống triển khai trên nền tảng container hóa (Docker), chạy được trên máy cá nhân lẫn server.

### 1.3. Thuật ngữ & Từ viết tắt (Definitions & Acronyms)

| Thuật ngữ / Viết tắt | Giải nghĩa |
|---|---|
| **StRS** | Stakeholder Requirements Specification — Đặc tả Yêu cầu Các bên Liên quan. |
| **Microservices** | Kiến trúc phần mềm chia nhỏ thành các dịch vụ độc lập |
| **API Gateway** | Điểm truy cập duy nhất, chuyển tiếp request đến đúng service |
| **IAM / RBAC** | Identity and Access Management / Role-Based Access Control (Phân quyền theo 4 vai trò: `ADMIN`, `STAFF`, `TEACHER`, `STUDENT` + trạng thái `GUEST`). |
| **JWT** | JSON Web Token — cơ chế xác thực không trạng thái (stateless) |
| **48h Slot Hold** | Cơ chế giữ chỗ tạm thời 48 giờ cho đơn đăng ký ở trạng thái `PENDING` trước khi hoàn tất đóng học phí. |
| **Logical FK** | Khóa ngoại logic (`INTEGER`) tham chiếu liên schema qua REST API / Webhook, tuyệt đối không tạo Physical FK hay JOIN SQL chéo schema. |
| **Schema** | Không gian dữ liệu riêng biệt trong PostgreSQL |
| **R-ID-1 (Identity Bridge)** | Quy ước định danh học viên: `admission.enrollments` và `finance.*` lưu `student_id = admission.users.id`; `student.*` và `learning.*` lưu `student_id = student.students.id`. |
| **VietQR Webhook** | Cơ chế nhận thông báo giao dịch chuyển khoản sang hệ thống an toàn, chính xác, không bị nhân đôi phiếu thu khi gửi lặp (Retry). |
| **Cash-Basis Revenue** | Doanh thu thực thu tính trên dòng tiền thực tế đã vào quỹ (`CASH` + `VIETQR`) trừ đi tổng tiền đã hoàn trả (`COMPLETED`). |
| **Healthcheck** | Cơ chế tự động kiểm tra service còn hoạt động hay không |
| **Container** | Đơn vị đóng gói phần mềm chạy cô lập (Docker) |

### 1.4 Tài liệu tham chiếu

| Tài liệu | Mô tả |
|----------|-------|
| BRS (tài liệu đi kèm) | Đặc tả Yêu cầu Nghiệp vụ — WHY |
| SRS (tài liệu đi kèm) | Đặc tả Yêu cầu Phần mềm — HOW |
| IEEE 29148:2018 | Requirements Engineering |
| ISO/IEC 15288:2015 | System life cycle processes |

---

## 2. Bối cảnh Nghiệp vụ & Đặt vấn đề (Business Context & Problem Statement)

### 2.1. Hiện trạng & Nỗi đau Nghiệp vụ (Current Pain Points)

Trước khi triển khai hệ thống tập trung, quy trình vận hành tại các trung tâm Anh ngữ gặp 5 điểm nghẽn nghiêm trọng:

1. **Phễu tuyển sinh rời rạc & vượt sĩ số lớp (Over-enrollment)**: Tư vấn viên giữ chỗ thủ công trên Google Sheets dẫn đến tình trạng một lớp nhận vượt sức chứa phòng học, hoặc học viên giữ chỗ ảo làm mất cơ hội của người đóng tiền thật.
2. **Xung đột lịch phòng & giảng viên**: Việc xếp lớp thủ công thường xuyên gây trùng phòng học hoặc trùng ca dạy của giáo viên.
3. **70% dữ liệu học viên bị phân mảnh**: Thông tin học viên nằm rải rác giữa bộ phận Tư vấn (Excel), Học vụ (Sheets) và Giáo viên (sổ điểm danh/Zalo), gây trùng lặp hồ sơ và thất lạc lịch sử học tập.
4. **85% giáo vụ và giảng viên quá tải vì điểm danh & tính điểm thủ công**: Sai sót khi tính điểm trung bình có trọng số và chậm trễ trong việc cảnh báo học viên nghỉ học hoặc xét điều kiện lên lớp.
5. **Rủi ro thất thoát tài chính & đối soát chậm**: Kế toán phải kiểm tra sao kê ngân hàng bằng mắt, dễ áp dụng lố số lượng Voucher khuyến mãi khi nhiều người đăng ký cùng lúc, và thiếu quy trình kiểm soát chặt chẽ khi xét duyệt hoàn tiền hoặc bảo lưu học phí.

### 2.2. Mục tiêu Chiến lược Của Dự án (Strategic Business Objectives)

| Mã mục tiêu | Mục tiêu chiến lược | Thước đo thành công (Success Metrics / KPIs) |
|---|---|---|
| **BO-01** | **Tự động hóa Phễu Tuyển sinh & Khảo thí Đầu vào** | 100% thí sinh làm bài Placement Test trực tuyến được chấm điểm tự động ngay lập tức và nhận đề xuất khóa học khớp trình độ. |
| **BO-02** | **Bảo toàn Sĩ số & Lịch học Tuyệt đối (Zero Conflict / Zero Over-capacity)** | 0% lớp học bị duyệt vượt `max_students` hoặc vượt sức chứa phòng; 0% ca học bị trùng phòng hoặc trùng giảng viên. |
| **BO-03** | **Hồ sơ Học viên 360 Độ & Số hóa Học vụ** | 100% đơn đăng ký `APPROVED` được đồng bộ tự động sang hồ sơ học viên (`HV-YYYY-NNNN`); điểm danh 1-chạm không trùng lặp. |
| **BO-04** | **Minh bạch Học tập & Đánh giá Trọng số Tự động** | 100% đầu điểm được kiểm soát trong biên `0 <= score <= max_score`; tự động chuẩn hóa thang 100% và xếp loại `Pass`/`Fail`. |
| **BO-05** | **An toàn Dòng tiền, Gạch nợ Thời gian thực & Kiểm soát Hoàn phí 3 Cấp** | 100% giao dịch VietQR đối soát tự động (Idempotent); 0% Voucher vượt trần lượt dùng; 100% yêu cầu hoàn tiền phải qua kiểm tra số buổi học (`<= 3 buổi`) và duyệt đủ 3 cấp. |

---

## 3. Hồ sơ Các Bên Liên Quan (Stakeholder Profiles)

### 3.1 Sơ đồ Bên Liên Quan

```mermaid
flowchart TB
    subgraph External["Bên liên quan Bên ngoài & Học viên"]
        ST1["Khách vãng lai / Thí sinh (GUEST)"]
        ST2["Học viên chính thức (STUDENT)"]
        ST7["Cổng Thanh toán Ngân hàng (VietQR Webhook)"]
    end

    subgraph InternalOps["Đội ngũ Vận hành & Giảng dạy Nội bộ"]
        ST3["Giảng viên (TEACHER)"]
        ST4["Chuyên viên Tư vấn & Học vụ (STAFF - Academic)"]
        ST5["Chuyên viên Kế toán (STAFF - Accountant)"]
    end

    subgraph ExecutiveTech["Ban Giám đốc & Quản trị Kỹ thuật"]
        ST6["Ban Giám đốc / Quản trị viên (ADMIN)"]
        ST8["Đội ngũ Phát triển & Kiểm thử (DevOps / QA)"]
    end

    EC(("Hệ thống Quản lý\nTrung tâm Anh ngữ"))

    ST1 --> EC
    ST2 --> EC
    ST3 --> EC
    ST4 --> EC
    ST5 --> EC
    ST6 --> EC
    ST7 --> EC
    ST8 --> EC
```

### 3.2 Bảng Phân tích Chi tiết Các Bên Liên Quan

| Mã | Bên liên quan (Stakeholder) | Vai trò Hệ thống | Mối quan tâm chính (Key Concerns) | Phân hệ Tương tác Chính |
|---|---|---|---|---|
| **SH-01** | **Khách vãng lai / Học viên tiềm năng** | `GUEST` | Xem thông tin khóa học/lịch khai giảng minh bạch; đăng ký tài khoản nhanh; làm bài test xếp lớp trực tuyến và biết ngay kết quả. | `admission` |
| **SH-02** | **Học viên chính thức** | `STUDENT` | Giữ chỗ lớp học 48h; áp mã giảm giá Voucher; quét mã VietQR đóng học phí; xem tài liệu lớp học; xem điểm thi cá nhân; xem số dư ví bảo lưu và gửi đơn xin bảo lưu/chuyển lớp. | `admission`, `student`, `learning`, `finance` |
| **SH-03** | **Giảng viên** | `TEACHER` | Xem lịch giảng dạy không bị chồng chéo; tải học liệu/slide lên cho lớp; điểm danh học viên từng buổi; nhập điểm đánh giá và nhận xét cho lớp mình phụ trách. | `admission`, `student`, `learning` |
| **SH-04** | **Chuyên viên Tư vấn & Học vụ** | `STAFF` (Academic) | Mở lớp học không trùng phòng/giáo viên; hỗ trợ đăng ký tại quầy; gia hạn giữ chỗ; duyệt đơn bảo lưu/chuyển lớp; khởi tạo yêu cầu hoàn tiền cấp 1; xuất báo cáo CSV. | `admission`, `student`, `learning`, `finance` |
| **SH-05** | **Chuyên viên Kế toán** | `STAFF` (Accountant) | Thiết lập biểu phí & chiến dịch Voucher; lập hóa đơn & chia kỳ đóng phí; thu tiền mặt tại quầy; quản lý ví bảo lưu & cấn trừ học phí; xác minh hoàn tiền cấp 2 và giải ngân UNC. | `finance`, `admission` |
| **SH-06** | **Ban Giám đốc & Quản trị viên** | `ADMIN` | Quản trị nhân sự & phân quyền; giám sát an ninh đăng nhập (`auth_audit_logs`); phê duyệt hoàn tiền cấp 3; theo dõi Dashboard BI toàn diện (Phễu tuyển sinh, Lấp đầy lớp, Doanh thu thực thu Cash-basis, Hiệu quả Voucher). | Toàn bộ 4 phân hệ |
| **SH-07** | **Cổng Ngân hàng / VietQR** | `WEBHOOK` | Truyền thông báo giao dịch chuyển khoản sang hệ thống an toàn, chính xác, không bị nhân đôi phiếu thu khi gửi lặp (Retry). | `finance` -> `admission` |
| **SH-08** | **Đội ngũ Kỹ thuật & QA** | `INTERNAL` / DevOps | Cô lập dữ liệu giữa 4 microservice (`INV-SYS-02`), chuẩn hóa giao thức JSON `snake_case` (`INV-SYS-01`), dễ dàng triển khai qua Docker Compose và kiểm thử tự động. | Toàn bộ hệ thống |

### 3.3 Người dùng & Vai trò Hệ thống

| Vai trò | Mô tả | Tương tác với hệ thống |
|---------|-------|----------------------|
| **ADMIN** | Quản trị viên | Full access — CRUD mọi dữ liệu, quản lý user |
| **STAFF** | Nhân viên tuyển sinh / học vụ | Duyệt đơn, lập hóa đơn, thu tiền, quản lý hồ sơ |
| **TEACHER** | Giáo viên | Điểm danh, nhập điểm, upload tài liệu — chỉ lớp mình |
| **STUDENT** | Học viên | Xem hồ sơ, điểm, hóa đơn, tải học liệu — chỉ của mình |

### 3.4 Môi trường Vận hành (Operating Environment)

| Yếu tố | Mô tả |
|--------|-------|
| **Hệ điều hành** | Windows 10/11, macOS, Linux (bất kỳ OS hỗ trợ Docker) |
| **Container** | Docker Engine 24+ / Docker Desktop |
| **Trình duyệt** | Chrome, Firefox, Edge (phiên bản mới nhất, desktop ≥ 1024px) |
| **Mạng** | Localhost (phát triển), LAN hoặc Cloud (triển khai) |

---

## 4. Nhu cầu & Yêu cầu Của Các Bên Liên Quan (Stakeholder Needs & Requirements)

### 4.1. Nhóm Yêu cầu từ Khách vãng lai & Học viên (`SH-01`, `SH-02`)

| Mã StR | Mô tả Yêu cầu của Bên liên quan (Stakeholder Requirement) | Phân hệ Đáp ứng | Mức ưu tiên |
|---|---|---|---|
| **STR-STU-01** | **Tra cứu & Khảo thí Đầu vào Trực tuyến**: Khách vãng lai và học viên có thể tra cứu khóa học, lớp học đang mở, đăng ký xác thực tài khoản và thực hiện bài thi khảo thí 3 kỹ năng (Listening, Reading, Grammar) có đồng hồ đếm ngược, tự động chấm điểm và gợi ý khóa học phù hợp kèm danh sách lớp còn chỗ. | `admission` | P0 (Bắt buộc) |
| **STR-STU-02** | **Đăng ký Lớp học & Giữ chỗ 48 Giờ**: Học viên có thể chọn lớp học để đăng ký trực tuyến, được hệ thống giữ chỗ độc quyền trong 48 giờ (`PENDING`) và tự động phát hành hóa đơn học phí tương ứng. | `admission`, `finance` | P0 (Bắt buộc) |
| **STR-STU-03** | **Áp dụng Voucher & Thanh toán VietQR Tức thời**: Học viên có thể kiểm tra mã giảm giá Voucher, lấy mã QR động VietQR theo từng đợt đóng phí (`installments`) và được duyệt vào lớp tự động ngay khi chuyển khoản thành công. | `finance`, `admission` | P0 (Bắt buộc) |
| **STR-STU-04** | **Truy cập Kho Học liệu & Tra cứu Bảng điểm Cá nhân**: Học viên chỉ xem và tải được tài liệu/slide của lớp mình đang học, đồng thời theo dõi điểm các bài kiểm tra và kết quả xếp loại tổng kết (`Pass`/`Fail`) của riêng mình (bảo mật tuyệt đối giữa các học viên). | `learning` | P0 (Bắt buộc) |
| **STR-STU-05** | **Quản lý Hồ sơ, Yêu cầu Học vụ & Ví Bảo lưu**: Học viên có thể xem hồ sơ cá nhân (`HV-YYYY-NNNN`), gửi đơn xin Bảo lưu (`RESERVE`), Chuyển lớp (`TRANSFER`) hoặc Rút hồ sơ (`DROP`), và kiểm tra số dư Ví bảo lưu để cấn trừ cho khóa học tiếp theo. | `student`, `finance` | P1 (Quan trọng) |

---

### 4.2. Nhóm Yêu cầu từ Giảng viên (`SH-03`)

| Mã StR | Mô tả Yêu cầu của Bên liên quan (Stakeholder Requirement) | Phân hệ Đáp ứng | Mức ưu tiên |
|---|---|---|---|
| **STR-TCH-01** | **Điểm danh Điện tử Theo Buổi học**: Giảng viên có thể điểm danh học viên trong lớp theo từng ngày học (`PRESENT`, `ABSENT`, `LATE`, `EXCUSED`) kèm ghi chú, hệ thống tự động chặn ghi trùng điểm danh trong cùng một buổi. | `student` | P0 (Bắt buộc) |
| **STR-TCH-02** | **Đăng tải & Phân phối Học liệu**: Giảng viên có thể đăng tải tài liệu (`DOCUMENT`, `VIDEO`, `EXERCISE`, `SLIDE`) gắn với lớp học cụ thể cho học viên. | `learning` | P1 (Quan trọng) |
| **STR-TCH-03** | **Nhập Điểm Đánh giá Có Trọng số Trong Phạm vi Phân công**: Giảng viên chỉ được nhập/cập nhật điểm (`score`, `max_score`, `weight`, `feedback`) cho các lớp mình trực tiếp giảng dạy; hệ thống tự động chặn điểm âm hoặc điểm vượt quá `max_score`. | `learning`, `admission` | P0 (Bắt buộc) |

---

### 4.3. Nhóm Yêu cầu từ Chuyên viên Tư vấn & Học vụ (`SH-04`)

| Mã StR | Mô tả Yêu cầu của Bên liên quan (Stakeholder Requirement) | Phân hệ Đáp ứng | Mức ưu tiên |
|---|---|---|---|
| **STR-ACA-01** | **Thiết lập Khóa học, Mở Lớp & Chống Xung đột Lịch**: Học vụ có thể quản lý danh mục khóa học (kèm khóa tiên quyết), mở lớp học mới gắn với phòng học và giảng viên; hệ thống tự động kiểm tra trần sức chứa phòng và chặn mọi xung đột lịch phòng/lịch giảng viên. | `admission` | P0 (Bắt buộc) |
| **STR-ACA-02** | **Điều phối Bàn Tuyển sinh, Gia hạn Giữ chỗ & Thu hồi Slot Quá hạn**: Học vụ có thể tiếp nhận học viên đăng ký tại quầy, gia hạn thời gian giữ chỗ kèm lý do nghiệp vụ, hoặc để hệ thống tự động thu hồi slot khi quá hạn 48h (`EXPIRED`). | `admission` | P0 (Bắt buộc) |
| **STR-ACA-03** | **Hủy Lớp Thiếu Sĩ số & Bảo vệ Quyền lợi Học viên (Cascade Cancellation)**: Khi lớp chưa khai giảng không đạt sĩ số tối thiểu (`min_students`), Học vụ có thể hủy lớp: các đơn `PENDING` tự động hủy giải phóng chỗ, các đơn `APPROVED` chuyển sang `CANCELLED_REFUND_PENDING` và đồng bộ yêu cầu hoàn phí 100% sang Finance. | `admission`, `finance` | P0 (Bắt buộc) |
| **STR-ACA-04** | **Xét duyệt Yêu cầu Học vụ & Khởi tạo Hoàn tiền Cấp 1**: Học vụ xem xét duyệt/từ chối đơn xin Bảo lưu, Chuyển lớp, Bỏ học của học viên; khởi tạo yêu cầu hoàn tiền Cấp 1 khi học viên rút hồ sơ sớm (học không quá 3 buổi). | `student`, `finance` | P1 (Quan trọng) |
| **STR-ACA-05** | **Xuất Dữ liệu Vận hành (CSV/Excel)**: Học vụ có thể xuất danh sách học viên theo lớp và bảng điểm khảo thí đầu vào ra file CSV an toàn (chống CSV Injection). | `admission`, `learning` | P1 (Quan trọng) |

---

### 4.4. Nhóm Yêu cầu từ Chuyên viên Kế toán (`SH-05`)

| Mã StR | Mô tả Yêu cầu của Bên liên quan (Stakeholder Requirement) | Phân hệ Đáp ứng | Mức ưu tiên |
|---|---|---|---|
| **STR-FIN-01** | **Quản trị Biểu phí & Chiến dịch Voucher Chống Race Condition**: Kế toán thiết lập học phí chuẩn theo khóa học và phát hành mã giảm giá Voucher (`discount_percent` hoặc `discount_amount`, giới hạn `max_uses`, `min_order_value`, `expires_at`) với cơ chế khóa dòng chống dùng vượt hạn mức. | `finance` | P0 (Bắt buộc) |
| **STR-FIN-02** | **Quản lý Hóa đơn, Chia kỳ Trả góp & Thu tiền mặt Tại quầy**: Kế toán theo dõi hóa đơn (`COURSE_PACKAGE`, `STANDALONE_RETAKE`, `MIXED`), lịch đóng phí nhiều đợt (`installments`), ghi nhận phiếu thu tiền mặt (`CASH`) gắn mã chứng từ duy nhất và tự động cập nhật trạng thái `PARTIAL`/`PAID`. | `finance`, `admission` | P0 (Bắt buộc) |
| **STR-FIN-03** | **Quản lý Ví Bảo lưu & Cấn trừ Học phí Nội bộ**: Kế toán ghi nhận tiền bảo lưu vào ví học viên (`RESERVATION_CREDIT`) và thực hiện cấn trừ sang hóa đơn khóa mới (`TUITION_OFFSET`), đảm bảo ví bảo lưu không bao giờ bị rút thành tiền mặt (`is_withdrawable = FALSE`). | `finance` | P0 (Bắt buộc) |
| **STR-FIN-04** | **Xác minh Hoàn tiền Cấp 2 & Giải ngân Hoàn tất**: Kế toán kiểm tra công nợ và số buổi thực học để xác minh yêu cầu hoàn tiền Cấp 2 (`PENDING_DIRECTOR`), và ghi nhận mã chứng từ Ủy nhiệm chi (`COMPLETED`) sau khi Giám đốc phê duyệt, đồng thời khóa bất biến hóa đơn ở trạng thái `REFUNDED`. | `finance` | P0 (Bắt buộc) |

---

### 4.5. Nhóm Yêu cầu từ Ban Giám đốc & Quản trị Hệ thống (`SH-06`)

| Mã StR | Mô tả Yêu cầu của Bên liên quan (Stakeholder Requirement) | Phân hệ Đáp ứng | Mức ưu tiên |
|---|---|---|---|
| **STR-ADM-01** | **Quản trị Định danh Tập trung, Nhân sự & Kiểm toán Bảo mật**: Giám đốc/Admin tạo và quản lý tài khoản nhân sự (`STAFF`, `TEACHER`), khóa/mở khóa tài khoản, giám sát nhật ký kiểm toán đăng nhập (`auth_audit_logs`) và cảnh báo tấn công Brute-force. | `admission` | P0 (Bắt buộc) |
| **STR-ADM-02** | **Phê duyệt Hoàn tiền Cấp 3 (Chốt kiểm soát cuối)**: Giám đốc là cấp duy nhất có quyền phê duyệt (`APPROVED`) hoặc từ chối (`REJECTED`) số tiền hoàn trả thực tế trên các hồ sơ hoàn phí đã qua Kế toán xác minh. | `finance` | P0 (Bắt buộc) |
| **STR-ADM-03** | **Hệ thống Báo cáo Quản trị Thông minh (Executive BI Dashboards)**: Giám đốc theo dõi trực quan các chỉ số KPI thời gian thực: Phễu chuyển đổi tuyển sinh 4 tầng, Tỷ lệ lấp đầy lớp học (`Occupancy Rate`), Doanh thu thực thu thuần theo tháng (`Cash-Basis Net Revenue`), Dư nợ học phí chờ thu (`Pipeline Revenue`), Hiệu quả chiến dịch Voucher, và DAU/MAU hệ thống. | `admission`, `finance`, `learning` | P1 (Quan trọng) |

---

## 5. Kịch bản Vận hành Xuyên Phân hệ (Operational Scenarios / End-to-End Value Streams)

### 5.1. Luồng Giá trị 1: Từ Khảo thí Đầu vào đến Nhập học & Tạo Hồ sơ Học viên Tự động

```mermaid
sequenceDiagram
    autonumber
    actor HV as Học viên (GUEST/STUDENT)
    participant ADM as Admission Service (:8081)
    participant FIN as Finance Service (:8084)
    participant QR as Cổng Ngân hàng (VietQR)
    participant STU as Student Service (:8082)

    HV->>ADM: 1. Đăng ký tài khoản, xác thực email & làm bài Placement Test
    ADM-->>HV: 2. Chấm điểm 3 kỹ năng & Đề xuất khóa học + Lớp còn slot
    HV->>ADM: 3. Nộp đơn đăng ký lớp học (kèm mã Voucher nếu có)
    ADM->>ADM: 4. Khóa kiểm tra slot & Tạo Enrollment (PENDING, giữ chỗ 48h)
    ADM->>FIN: 5. POST /api/finance/invoices/auto-generate (X-Internal-Secret)
    FIN->>FIN: 6. Khóa Voucher (FOR UPDATE), tạo Invoice, Items & Installments
    FIN-->>ADM: 7. Trả về invoice_id, invoice_code, final_amount, payment_ref_code
    HV->>FIN: 8. GET /api/finance/invoices/{id}/vietqr
    FIN-->>HV: 9. Hiển thị mã QR động chứa payment_ref_code
    HV->>QR: 10. Quét mã QR chuyển khoản học phí
    QR->>FIN: 11. POST /api/finance/webhooks/vietqr (bank_transaction_id)
    FIN->>FIN: 12. Kiểm tra Idempotency, ghi Payment & cập nhật Invoice = PAID
    FIN->>ADM: 13. POST /api/admission/webhooks/payment-success (X-Internal-Secret)
    ADM->>ADM: 14. Kiểm tra Invariant sĩ số (INV-ADM-01) & Duyệt đơn = APPROVED
    ADM->>STU: 15. Đồng bộ tạo hồ sơ học viên chính thức (HV-YYYY-NNNN)
```

### 5.2. Luồng Giá trị 2: Giảng dạy, Điểm danh, Đánh giá & Xếp loại Học tập

1. **Giảng viên (`TEACHER`)** đăng nhập qua Admission Service nhận JWT Token.
2. Giảng viên mở lớp học được phân công trên **Learning Service (`:8083`)** để đăng tải giáo trình, slide và bài tập (`learning.materials`).
3. Trong mỗi buổi học, Giảng viên thực hiện điểm danh trên **Student Service (`:8082`)** (`student.attendance`), đảm bảo mỗi học viên có đúng 1 trạng thái chuyên cần/buổi.
4. Giữa kỳ và cuối kỳ, Giảng viên nhập điểm đánh giá trên **Learning Service** (`learning.grades`). Hệ thống tự động tính điểm trung bình chuẩn hóa thang 100% có trọng số qua View `learning.v_student_grade_summary` và thống kê tỷ lệ đỗ của cả lớp qua `learning.v_class_pass_rate`.

### 5.3. Luồng Giá trị 3: Bảo lưu Ví Nội bộ & Quy trình Hoàn tiền 3 Cấp

1. **Trường hợp Bảo lưu**: Học viên nộp đơn xin bảo lưu trên **Student Service** (`student.status_requests`). Sau khi Học vụ duyệt, **Kế toán** cộng số dư tương ứng vào Ví bảo lưu trên **Finance Service** (`POST /api/finance/wallets/{student_id}/reserve` với `tx_type = RESERVATION_CREDIT`). Khi học viên đăng ký khóa mới, số dư này được dùng để cấn trừ trực tiếp vào hóa đơn (`TUITION_OFFSET`).
2. **Trường hợp Hoàn tiền 3 cấp**:
   - **Cấp 1 (Tư vấn viên - `STAFF`)**: Khởi tạo yêu cầu hoàn tiền (`POST /api/finance/refunds`). Finance đối chiếu số buổi đã điểm danh (`attended_sessions <= max_allowed_sessions`, mặc định tối đa 3 buổi). Nếu vượt quá 3 buổi, hệ thống tự động từ chối (`INV-FIN-07`).
   - **Cấp 2 (Kế toán - `STAFF`)**: Kiểm tra thực thu và xác minh công nợ (`PUT /api/finance/refunds/{id}/verify`), chuyển trạng thái sang `PENDING_DIRECTOR`.
   - **Cấp 3 (Giám đốc - `ADMIN`)**: Phê duyệt mức hoàn tiền cuối cùng (`PUT /api/finance/refunds/{id}/approve`), chuyển trạng thái sang `APPROVED`.
   - **Giải ngân Hoàn tất (Kế toán - `STAFF`)**: Chuyển khoản hoàn phí, cập nhật mã chứng từ UNC (`PUT /api/finance/refunds/{id}/complete`), chuyển yêu cầu sang `COMPLETED` và khóa hóa đơn thành `REFUNDED`.

### 5.4 Use Case cấp Hệ thống (System-Level Use Cases)

#### UC-SYS-01: Xử lý Đơn Tuyển sinh

```
Tên:            Xử lý Đơn Tuyển sinh
Actors:         Staff, Admission Service, Student Service, Finance Service
Pre-condition:  Lớp học tồn tại, HV có tài khoản
Post-condition: Enrollment APPROVED, Hồ sơ HV được tạo, Hóa đơn được tạo

Luồng chính:
  1. Staff tạo đơn đăng ký → PENDING (hold 48h)
  2. Staff chọn duyệt đơn
  3. Admission kiểm tra sĩ số lớp < max_students
  4. Admission chuyển đơn → APPROVED
  5. Admission gọi Student Service → tạo hồ sơ HV (nếu chưa có)
  6. Admission gọi Finance Service → tạo hóa đơn PENDING

Luồng thay thế:
  3a. Lớp đầy → Trả lỗi ADM_CLASS_FULL, giữ PENDING
  5a. Student Service không phản hồi → Đơn vẫn APPROVED, retry sau
```

#### UC-SYS-02: Điểm danh Buổi học

```
Tên:            Điểm danh Buổi học
Actors:         Teacher, Student Service
Pre-condition:  GV phụ trách lớp, HV đã enrollment APPROVED

Luồng chính:
  1. GV chọn lớp + ngày + HV
  2. Hệ thống kiểm tra: GV phụ trách lớp này?
  3. Hệ thống kiểm tra: HV chưa điểm danh hôm nay?
  4. Lưu bản ghi điểm danh

Luồng thay thế:
  2a. GV không phụ trách → 403 Forbidden
  3a. Đã điểm danh → 409 Conflict
```

#### UC-SYS-03: Thu Học phí

```
Tên:            Thu Học phí
Actors:         Staff, Finance Service
Pre-condition:  Hóa đơn tồn tại, chưa CANCELLED

Luồng chính:
  1. Staff nhập: invoice_id, số tiền, hình thức, mã tham chiếu
  2. Hệ thống kiểm tra transaction_ref chưa tồn tại
  3. Ghi nhận payment, cập nhật tổng đã thu
  4. Nếu tổng thu ≥ giá trị hóa đơn → chuyển PAID

Luồng thay thế:
  2a. transaction_ref trùng → 409 Conflict (Idempotency)
```

---

## 6. Kiến trúc Tổng quan & Mô hình Dữ liệu Hệ thống (System Architecture & Data Model)

### 6.1 Kiến trúc Tổng quan

Hệ thống áp dụng kiến trúc **Microservices Polyglot Monorepo**: nhiều service viết bằng nhiều ngôn ngữ (Java, Python), cùng nằm trong 1 repository, giao tiếp qua REST API nội bộ.

```
                         INTERNET / TRÌNH DUYỆT
                                   │
                                   ▼
                    ┌──────────────────────────────┐
                    │      NGINX API GATEWAY        │  Port 8080
                    │  Reverse Proxy + Static Files │
                    └──┬──────────┬──────┬──────┬──┘
                       │          │      │      │
            ┌──────────▼──┐  ┌────▼──┐ ┌▼────┐ ┌▼──────────┐
            │  Admission  │  │Student│ │Learn│ │  Finance  │
            │  Service    │  │Service│ │ Svc │ │  Service  │
            │  :8081      │  │:8082  │ │:8083│ │  :8084    │
            │  Java 21    │  │Py 3.11│ │Py   │ │  Py 3.11  │
            │  Spring Boot│  │FastAPI│ │FA   │ │  FastAPI  │
            └──────┬───────┘  └───┬───┘ └──┬──┘ └────┬───────┘
                   │              │         │          │
                   └──────────────┴────┬────┘──────────┘
                                       │
                        ┌──────────────▼──────────────────┐
                        │       PostgreSQL 16 Alpine       │
                        │    1 Database — 4 Schemas         │
                        │  admission | student              │
                        │  learning  | finance              │
                        └──────────────────────────────────┘
```

### 6.2 Danh mục Thành phần

| ID | Thành phần | Cổng | Công nghệ | Nhiệm vụ chính |
|----|-----------|------|-----------|---------------|
| C-01 | API Gateway | 8080 | Nginx Alpine | Reverse proxy, phục vụ frontend tĩnh |
| C-02 | Admission Service | 8081 | Spring Boot 3.3 / Java 21 | Auth JWT, Khóa học, Lớp, Tuyển sinh, Bài thi, BI |
| C-03 | Student Service | 8082 | FastAPI / Python 3.11 | Hồ sơ HV, Điểm danh, Yêu cầu học vụ |
| C-04 | Learning Service | 8083 | FastAPI / Python 3.11 | Kho học liệu, Sổ điểm, Báo cáo |
| C-05 | Finance Service | 8084 | FastAPI / Python 3.11 | Biểu phí, Voucher, Hóa đơn, Thanh toán, Ví, Hoàn phí |
| C-06 | PostgreSQL Database | 5432 | PostgreSQL 16 Alpine | Lưu trữ dữ liệu với 4 schema cô lập |

### 6.3 Phân rã Hệ thống (System Decomposition)

```
Hệ thống Quản lý Trung tâm Anh ngữ
│
├── 1. Phân hệ Tuyển sinh (Admission)
│   ├── 1.1 Xác thực & Phân quyền (Auth)
│   ├── 1.2 Quản lý Khóa học
│   ├── 1.3 Quản lý Phòng học
│   ├── 1.4 Quản lý Lớp học
│   ├── 1.5 Xét duyệt Tuyển sinh
│   ├── 1.6 Bài thi Đầu vào
│   ├── 1.7 Quản lý Người dùng
│   ├── 1.8 BI & Phân tích
│   └── 1.9 Nhật ký Bảo mật (Audit Log)
│
├── 2. Phân hệ Học viên (Student)
│   ├── 2.1 Quản lý Hồ sơ Học viên
│   ├── 2.2 Điểm danh Buổi học
│   └── 2.3 Yêu cầu Học vụ (Bảo lưu / Chuyển / Rút)
│
├── 3. Phân hệ Đào tạo (Learning)
│   ├── 3.1 Quản lý Kho Học liệu
│   ├── 3.2 Sổ Điểm & Đánh giá
│   └── 3.3 Báo cáo Kết quả Học tập
│
├── 4. Phân hệ Tài chính (Finance)
│   ├── 4.1 Biểu phí & Voucher
│   ├── 4.2 Lập Hóa đơn & Theo dõi Nợ
│   ├── 4.3 Ghi nhận Thanh toán
│   ├── 4.4 Ví Học viên
│   ├── 4.5 Yêu cầu Hoàn phí
│   └── 4.6 BI Tài chính
│
└── 5. Hạ tầng
    ├── 5.1 API Gateway (Nginx)
    ├── 5.2 Cơ sở Dữ liệu (PostgreSQL 16)
    └── 5.3 Nền tảng Container (Docker Compose)
```

### 6.4 Giao diện Hệ thống (System Interfaces)

#### Giao diện Người dùng (User Interfaces)

| Module | URL | Các trang chính |
|--------|-----|----------------|
| Cổng chính | `/` | Dashboard tổng quan |
| Đăng nhập | `/shared/login.html` | Form login, chọn role test nhanh |
| Admission | `/admission/` | courses, classes, rooms, enrollments, test, bi-dashboard |
| Student | `/student/` | students, attendance, status_requests |
| Learning | `/learning/` | materials, grades, reports |
| Finance | `/finance/` | invoices, tuition, bi-dashboard |

#### Giao diện Phần mềm (API Contract)

**Cấu trúc Response chuẩn:**

```json
// Thành công
{ "success": true, "data": { ... }, "message": "Mô tả" }

// Thất bại
{ "success": false, "error": { "code": "MÃ_LỖI", "message": "Mô tả" } }
```

#### Giao tiếp liên Service (Internal API Matrix)

| Nguồn → Đích | Endpoint | Mục đích |
|--------------|---------|---------|
| Admission → Student | `POST /api/student/students` | Tạo hồ sơ HV khi duyệt đơn |
| Admission → Finance | `POST /api/finance/invoices` | Tạo hóa đơn khi duyệt đơn |
| Learning → Admission | `GET /api/admission/classes/{id}` | Lấy thông tin lớp |
| Learning → Student | `GET /api/student/students?class_id=X` | Danh sách HV của lớp |
| Finance → Admission | `GET /api/admission/courses/{id}` | Lấy mức phí chuẩn |

### 6.5 Nguyên tắc Cô lập Schema

```
admission-service  →  CHỈ query admission.*
student-service    →  CHỈ query student.*
learning-service   →  CHỈ query learning.*
finance-service    →  CHỈ query finance.*

⛔ CẤM:
  SELECT ... FROM student.students s
  JOIN admission.enrollments e ON ...;
  -- Phải dùng REST API nội bộ thay thế
```

---

## 7. Máy Trạng thái (State Machines)

### 7.1 Enrollment (Đơn Đăng ký)

```
                     tạo mới
                        │
                        ▼
                    [PENDING]
                   /    |     \
                  /     |      \
          duyệt   từ chối  quá 48h
            /       |        \
           ▼        ▼         ▼
      [APPROVED] [CANCELLED] [EXPIRED]
           │
       hủy sau duyệt
           ▼
      [CANCELLED]

Chuyển trạng thái hợp lệ:
  PENDING  → APPROVED  (khi sĩ số < max_students)
  PENDING  → CANCELLED (khi staff từ chối)
  PENDING  → EXPIRED   (khi quá 48 giờ hold)
  APPROVED → CANCELLED (khi hủy sau duyệt)

Chuyển trạng thái CẤM:
  CANCELLED ✘→ APPROVED
  EXPIRED   ✘→ APPROVED
```

### 7.2 Invoice (Hóa đơn)

```
                    tạo mới
                       │
                       ▼
                   [PENDING]
                  /    |    \
            nộp 1   quá hạn   hủy
            phần       |       \
             /         |        \
            ▼          ▼         ▼
      [PARTIAL]   [OVERDUE]  [CANCELLED]
           \         |
        nộp đủ    nộp đủ
             \     /
              ▼  ▼
             [PAID]

Chuyển trạng thái CẤM:
  CANCELLED ✘→ PAID
```

### 7.3 Student Status (Trạng thái Học viên)

```
         tạo mới
            │
            ▼
        [STUDYING]
        /        \
  bảo lưu    tốt nghiệp
      /            \
     ▼              ▼
 [RESERVED]    [GRADUATED]
     │
  hết hạn 6 tháng
     ▼
  [DROPPED]
```

### 7.4 Status Request (Yêu cầu Học vụ)

```
    tạo mới
       │
       ▼
   [PENDING]
    /      \
 duyệt  từ chối
  /          \
 ▼            ▼
[APPROVED] [REJECTED]
```

---

## 8. Yêu cầu Chất lượng, Bất biến & Ràng buộc Hệ thống (Quality Attributes, Invariants & Constraints)

### 8.1. Mười lăm Quy tắc Bất biến Cốt lõi (System & Domain Invariants)

Hệ thống bắt buộc duy trì **15 quy tắc bất biến** (được kiểm chứng tự động qua `tests/invariants/`):

- **Kiến trúc Hệ thống (`INV-SYS`)**:
  - `INV-SYS-01`: 100% trường dữ liệu JSON Request/Response và Query Params giữa mọi ngôn ngữ (Java & Python) phải sử dụng chuẩn `snake_case`.
  - `INV-SYS-02`: Cô lập tuyệt đối 4 Schemas (`admission`, `student`, `learning`, `finance`). Nghiêm cấm JOIN SQL hoặc tạo khóa ngoại vật lý chéo giữa các schema.
- **An ninh & Phân quyền (`INV-SEC`)**:
  - `INV-SEC-01`: Học viên (`STUDENT`) chỉ được truy cập hồ sơ, hóa đơn, ví bảo lưu và bảng điểm của chính mình.
  - `INV-SEC-02`: Giảng viên (`TEACHER`) chỉ được điểm danh và nhập/sửa điểm cho các lớp mình được phân công phụ trách.
  - `INV-SEC-03`: Tuyệt đối không lưu mật khẩu dạng rõ (phải băm BCrypt) và không đưa khóa bí mật sản xuất (Production Secrets) vào kho mã nguồn Git.
- **Nghiệp vụ Tuyển sinh (`INV-ADM`)**:
  - `INV-ADM-01`: Không bao giờ duyệt (`APPROVED`) vượt quá sĩ số tối đa của lớp (`max_students`) hoặc sức chứa phòng học (`capacity`).
  - `INV-ADM-02`: Mã khóa học (`courses.code`), mã lớp (`classes.code`) và mã phòng (`rooms.code`) là duy nhất toàn hệ thống.
  - `INV-ADM-03`: Tuân thủ chặt chẽ máy trạng thái đơn đăng ký; đơn đã `CANCELLED` hoặc `REJECTED` không được chuyển trực tiếp sang `APPROVED`.
- **Nghiệp vụ Học viên (`INV-STU`)**:
  - `INV-STU-01`: Mỗi học viên chỉ có tối đa 1 bản ghi điểm danh trong 1 buổi học của 1 lớp (`UNIQUE(class_id, student_id, session_date)`).
  - `INV-STU-02`: Trạng thái học vụ của học viên chỉ thuộc tập hợp hợp lệ (`STUDYING`, `RESERVED`, `DROPPED`, `GRADUATED`).
- **Nghiệp vụ Đào tạo (`INV-LRN`)**:
  - `INV-LRN-01`: Điểm số đánh giá luôn thỏa mãn `0 <= score <= max_score` và `max_score > 0`.
  - `INV-LRN-02`: Cô lập điểm thi cá nhân giữa các học viên.
- **Nghiệp vụ Tài chính (`INV-FIN`)**:
  - `INV-FIN-01`: Hóa đơn và giao dịch thanh toán phải có số tiền dương; hóa đơn đã `PAID` hoặc `REFUNDED` là bất biến (không được sửa số tiền gốc/chiết khấu).
  - `INV-FIN-02` / `INV-FIN-05`: Chống ghi nhận trùng lặp thanh toán (Idempotency) dựa trên ràng buộc `UNIQUE(bank_transaction_id)` và `UNIQUE(transaction_ref)`.
  - `INV-FIN-03`: Hóa đơn chỉ chuyển sang trạng thái `PAID` khi tổng tiền thực thu (`paid_amount`) đạt đủ `final_amount`.
  - `INV-FIN-04`: Áp dụng khóa bi quan `SELECT ... FOR UPDATE` khi trừ lượt dùng Voucher (`current_uses <= max_uses`).
  - `INV-FIN-06`: Ví bảo lưu học viên luôn khóa rút tiền mặt (`is_withdrawable = FALSE`).
  - `INV-FIN-07`: Chỉ cho phép hoàn tiền khi số buổi đã học `attended_sessions <= max_allowed_sessions` và đi qua đủ 3 cấp phê duyệt.

### 8.2 Yêu cầu Phi chức năng (Non-Functional Requirements - NFR)

#### 8.2.1 Hiệu năng (Performance)

| ID | Yêu cầu | Tiêu chí đo lường | Cách đo |
|----|---------|-------------------|---------
| NFR-PERF-01 | Thời gian phản hồi API | p95 ≤ 2000ms tại 100 concurrent users | Load test (JMeter / k6) |
| NFR-PERF-02 | Tải trang frontend | First Contentful Paint ≤ 1500ms | Chrome DevTools / Lighthouse |
| NFR-PERF-03 | Hiệu quả truy vấn DB | Mỗi API call ≤ 5 queries | Logging SQL queries |
| NFR-PERF-04 | File upload | Hỗ trợ ≤ 50MB/file | Kiểm thử thực tế |

#### 8.2.2 Độ sẵn sàng (Availability)

| ID | Yêu cầu | Tiêu chí |
|----|---------|---------|
| NFR-AVAIL-01 | Uptime | ≥ 99.5% (ngoại trừ bảo trì có lịch) |
| NFR-AVAIL-02 | Healthcheck DB | PostgreSQL healthcheck mỗi 5 giây, tự restart nếu fail 5 lần |
| NFR-AVAIL-03 | Thứ tự khởi động | Services phải chờ postgres healthy mới khởi động |
| NFR-AVAIL-04 | Graceful degradation | Nếu 1 service down, các service khác vẫn hoạt động bình thường |

#### 8.2.3 Bảo mật (Security)

| ID | Yêu cầu | Chi tiết |
|----|---------|---------|
| NFR-SEC-01 | Xác thực (Authentication) | JWT HS256; access_token TTL 15 phút; refresh_token TTL 7 ngày |
| NFR-SEC-02 | Mã hóa mật khẩu | Bcrypt cost factor ≥ 10, không lưu plaintext |
| NFR-SEC-03 | Phân quyền (Authorization) | RBAC 4 cấp: ADMIN > STAFF > TEACHER > STUDENT |
| NFR-SEC-04 | Cô lập dữ liệu | Mỗi service chỉ truy cập schema DB của mình |
| NFR-SEC-05 | Quản lý bí mật | Credentials đọc từ environment variables, không hardcode |
| NFR-SEC-06 | Chống tấn công | Input validation, parameterized queries (chống SQL Injection) |
| NFR-SEC-07 | Giới hạn tần suất | Rate limiting: max 10 lần login sai / 15 phút / IP |
| NFR-SEC-08 | Nhật ký bảo mật | Log toàn bộ hành động nhạy cảm (login, duyệt đơn, thanh toán) |
| NFR-SEC-09 | CORS | Chỉ cho phép origin cụ thể trong production |
| NFR-SEC-10 | Xử lý ngoại lệ | Không lộ stack trace, chi tiết lỗi hệ thống ra client |

#### 8.2.4 Khả năng Bảo trì (Maintainability)

| ID | Yêu cầu | Chi tiết |
|----|---------|---------|
| NFR-MAINT-01 | Quy chuẩn đặt tên | `snake_case` cho toàn bộ API, DB columns, JSON fields |
| NFR-MAINT-02 | Tài liệu code | Comment tiếng Việt, mọi function/endpoint có docstring |
| NFR-MAINT-03 | Cấu hình | 12-Factor App: 100% config qua environment variables |
| NFR-MAINT-04 | Hot-reload | Frontend: save + F5; Python: `--reload`; Java: rebuild container |
| NFR-MAINT-05 | Kiểm thử bất biến | 15 invariant tests phải luôn PASS sau mỗi thay đổi |
| NFR-MAINT-06 | Cô lập code | Mỗi nhóm chỉ sửa trong `services/{tên_phân_hệ}/` |

#### 8.2.5 Khả năng Tương tác (Interoperability)

| ID | Yêu cầu | Chi tiết |
|----|---------|---------|
| NFR-INTER-01 | Giao tiếp liên service | REST API nội bộ qua HTTP, format JSON, snake_case |
| NFR-INTER-02 | Retry with Backoff | Gọi API nội bộ thất bại → retry 3 lần (1s, 2s, 4s) |
| NFR-INTER-03 | Timeout | Mọi gọi nội bộ phải có timeout = 10 giây |
| NFR-INTER-04 | Eventual Consistency | Enrollment APPROVED trước; hóa đơn tạo sau nếu Finance lỗi |

### 8.3 Ràng buộc Thiết kế (Design Constraints)

| ID | Ràng buộc | Mô tả |
|----|----------|-------|
| DC-01 | **Container hóa** | Toàn bộ hệ thống chạy qua Docker Compose |
| DC-02 | **Polyglot** | Admission bắt buộc Java/Spring Boot; 3 phân hệ còn lại Python/FastAPI |
| DC-03 | **Monorepo** | Toàn bộ code nằm trong 1 repository Git duy nhất |
| DC-04 | **Database chung** | 1 PostgreSQL instance chia 4 schema, KHÔNG dùng 4 DB riêng |
| DC-05 | **Frontend tĩnh** | HTML/CSS/JS thuần — không dùng React/Vue/Angular |
| DC-06 | **Cô lập code** | Mỗi nhóm chỉ sửa `services/{tên}/`, không sửa service khác |
| DC-07 | **API snake_case** | Toàn bộ request/response JSON dùng `snake_case` |
| DC-08 | **Init script** | Schema + seed data khởi tạo qua `scripts/init-db.sql` |

---

## 9. Ma trận Truy vết Yêu cầu Toàn Dự án (End-to-End Traceability Matrix)

| Mã Yêu cầu StR | Bên liên quan | Tính năng Phân hệ (Module FR) | Bảng / View CSDL Chính (`scripts/init-db.sql`) | API Endpoint Tiêu biểu | Kiểm thử Bất biến / Regression |
|---|---|---|---|---|---|
| **STR-STU-01** | `GUEST`, `STUDENT` | `FR-TST-01..05` | `admission.test_sets`, `test_questions`, `test_attempts`, `test_submissions`, `test_recommendation_rules` | `POST /api/admission/tests/start`, `POST /api/admission/tests/submit` | `PlacementTestServiceTest` |
| **STR-STU-02** | `STUDENT`, `STAFF` | `FR-ENR-01..05`, `FIN-F03` | `admission.enrollments`, `finance.invoices`, `finance.invoice_items`, `finance.installments` | `POST /api/admission/enrollments`, `POST /api/finance/invoices/auto-generate` | `INV-ADM-01`, `INV-FIN-01` |
| **STR-STU-03** | `STUDENT`, `WEBHOOK` | `FIN-F02`, `FIN-F06`, `FR-ENR-06` | `finance.vouchers`, `finance.payments`, `admission.enrollments` | `POST /api/finance/vouchers/validate`, `GET .../vietqr`, `POST /api/finance/webhooks/vietqr` | `INV-FIN-02`, `INV-FIN-03` |
| **STR-STU-04** | `STUDENT` | `LRN-01`, `LRN-02`, `LRN-03` | `learning.materials`, `learning.grades`, `learning.v_student_grade_summary` | `GET /api/learning/materials`, `GET /api/learning/grades` | `INV-LRN-02`, `INV-SEC-01` |
| **STR-STU-05** | `STUDENT`, `STAFF` | `STU-01`, `STU-03`, `FIN-F07` | `student.students`, `student.status_requests`, `finance.student_wallets` | `GET /api/student/students`, `GET /api/finance/wallets/{student_id}` | `INV-STU-02`, `INV-FIN-06` |
| **STR-TCH-01** | `TEACHER` | `STU-02` | `student.attendance` | `GET/POST /api/student/attendance` | `INV-STU-01` |
| **STR-TCH-02** | `TEACHER` | `LRN-01` | `learning.materials` | `POST /api/learning/materials` | `INV-SYS-01` |
| **STR-TCH-03** | `TEACHER` | `LRN-02` | `learning.grades`, `learning.v_class_pass_rate` | `POST /api/learning/grades` | `INV-LRN-01`, `INV-SEC-02` |
| **STR-ACA-01** | `STAFF`, `ADMIN` | `FR-FAC-01..03`, `FR-ACA-01..05` | `admission.rooms`, `admission.courses`, `admission.classes` | `POST /api/admission/rooms`, `POST /api/admission/courses`, `POST /api/admission/classes` | `INV-ADM-02`, `ConflictEngineTest` |
| **STR-ACA-02** | `STAFF` | `FR-ENR-04`, `FR-ENR-05`, `FR-ENR-08` | `admission.enrollments`, `student.students` | `PUT /api/admission/enrollments/{id}/extend-hold` | `INV-ADM-03` |
| **STR-ACA-03** | `STAFF`, `ADMIN` | `FR-ACA-04`, `FIN-F08B` | `admission.classes`, `admission.enrollments`, `finance.refund_requests` | `PUT /api/admission/classes/{id}/cancel`, `POST /api/finance/internal/class-cancellations` | `ClassCancellationTest` |
| **STR-FIN-01** | `STAFF`, `ADMIN` | `FIN-F01`, `FIN-F02` | `finance.tuition_fees`, `finance.vouchers` | `GET/POST /api/finance/tuition-fees`, `GET/POST /api/finance/vouchers` | `INV-FIN-04` |
| **STR-FIN-02** | `STAFF` | `FIN-F04`, `FIN-F05` | `finance.invoices`, `finance.installments`, `finance.payments` | `POST /api/finance/payments/cash`, `GET /api/finance/invoices/{id}` | `INV-FIN-02`, `INV-FIN-03` |
| **STR-FIN-03** | `STAFF` | `FIN-F07` | `finance.student_wallets`, `finance.wallet_transactions` | `POST /api/finance/wallets/{student_id}/reserve` | `INV-FIN-06` |
| **STR-FIN-04** | `STAFF`, `ADMIN` | `FIN-F08` | `finance.refund_requests`, `finance.invoices` | `POST /api/finance/refunds`, `PUT .../verify`, `PUT .../approve`, `PUT .../complete` | `INV-FIN-07` |
| **STR-ADM-01** | `ADMIN` | `FR-IAM-01..07` | `admission.users`, `refresh_tokens`, `password_reset_tokens`, `auth_audit_logs` | `POST /auth/login`, `GET /api/admission/users`, `GET /api/admission/audit-logs` | `INV-SEC-03`, `AuthServiceTest` |
| **STR-ADM-03** | `ADMIN`, `STAFF` | `BI-01..03`, `FIN-F09` | `finance.vw_bi_cash_revenue_monthly`, `finance.vw_bi_voucher_campaigns`, `learning.v_class_pass_rate` | `GET /api/admission/bi/*`, `GET /api/finance/bi/*` | `BiAnalyticsServiceTest`, `test_finance_api.py` |

---

## 10. Tiêu chí Nghiệm thu & Phạm vi Loại trừ (Acceptance Criteria & Explicit Non-Goals)

### 10.1. Tiêu chí Nghiệm thu Cấp Hệ thống (System-Wide Acceptance Criteria)

Dự án được nghiệm thu đạt chuẩn khi thỏa mãn đồng thời các điều kiện sau:

1. **Khởi tạo Cơ sở Dữ liệu**: Kịch bản `scripts/init-db.sql` chạy thành công không lỗi trên PostgreSQL 16, tạo đủ 4 schemas (`admission`, `student`, `learning`, `finance`), 28 bảng, 4 views, 35 triggers và dữ liệu mẫu chuẩn.
2. **Vượt qua Bộ Kiểm thử Bất biến Tự động**: Lệnh `python -m unittest discover -s tests/invariants -p "test_*.py" -v` đạt kết quả **`15/15 OK`**.
3. **Vận hành Trọn vẹn trên Docker Compose**: Toàn bộ 6 containers (`ec-postgres`, `ec-admission`, `ec-student`, `ec-learning`, `ec-finance`, `ec-gateway`) khởi chạy ở trạng thái `Up / Healthy` và định tuyến chính xác qua cổng duy nhất `http://localhost:8080`.
4. **Đảm bảo Độc lập & Tích hợp Liên Phân hệ**: Mỗi phân hệ hoạt động đúng trên schema riêng của mình và giao tiếp thành công qua các hợp đồng REST API / Webhook nội bộ (`X-Internal-Secret`).

```
☐ Docker Compose up → 6 containers healthy trong 60 giây
☐ 15/15 Invariant Tests pass
☐ API Gateway định tuyến đúng 4 service + 4 frontend
☐ Login/Logout JWT hoạt động bình thường
☐ 5 tiêu chí nghiệm thu nghiệp vụ (xem BRS §10)
```

### 10.2. Phạm vi Cố ý Không Thực hiện (Explicit Non-Goals)

Để tập trung nguồn lực bảo đảm chất lượng cốt lõi, các hạng mục sau nằm ngoài phạm vi của phiên bản hiện tại:
1. Không tích hợp Chatbot AI tư vấn tự động hoặc AI chấm bài tự luận (Speaking/Writing).
2. Không sử dụng thuật toán tối ưu hóa xếp lịch tự động bằng AI/Heuristic Solver (chỉ kiểm tra và chặn xung đột lịch).
3. Không kết nối cổng thanh toán thương mại tính phí thật (VNPay/MoMo Production) hoặc cổng SMS/Zalo ZNS trả phí.
4. Không bao gồm phân hệ kế toán nội bộ doanh nghiệp (chi phí vận hành OpEx, tính lương giáo viên/nhân viên, khấu hao tài sản).
5. Không phát triển ứng dụng di động Native (iOS/Android); giao diện người dùng được cung cấp qua nền tảng Web Responsive.

---

## 11. Phụ lục

### 11.1 Lệnh Điều khiển Hệ thống

```bash
# Khởi chạy toàn bộ
docker compose up -d --build

# Dừng hệ thống
docker compose down

# Chạy kiểm thử bất biến
python -m unittest discover -s tests/invariants -p "test_*.py" -v

# Kiểm tra trạng thái containers
docker compose ps
```

### 11.2 Lịch sử Thay đổi Tài liệu

| Phiên bản | Ngày | Người sửa | Mô tả |
|-----------|------|-----------|-------|
| 1.0 | 11/11/2026 | Nhóm CNPM | Phiên bản ban đầu — đổi tên từ SysRS sang StRS theo chuẩn IEEE 29148:2018, merge nội dung StRS tham khảo |

---

*Chuẩn tham chiếu: ISO/IEC/IEEE 29148:2018 — Systems and software engineering — Requirements engineering*
