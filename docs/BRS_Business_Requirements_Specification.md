# BRS – Business Requirements Specification
# Đặc tả Yêu cầu Nghiệp vụ

**Dự án:** Hệ thống Quản lý Trung tâm Anh ngữ (English Center Management System)  
**Phiên bản tài liệu:** 1.1  
**Ngày lập:** 11/11/2026  
**Cập nhật lần cuối:** 11/11/2026  
**Tham chiếu chuẩn:** IEEE 29148:2018 — Systems and software engineering — Life cycle processes — Requirements engineering  
**Tác giả biên soạn:** Business Sponsor & Senior Business Analyst (Senior BA)  
**Đối tượng độc giả mục tiêu (Target Audience):** Ban Giám đốc (Executive Board), Business Sponsor, Product Owner (PO), Project Manager  
**Tầng kiến trúc & Cấp độ trừu tượng:** Cấp Chiến lược (Strategic Level) — Trừu tượng mức rất cao (Very High Abstraction)  
**Câu hỏi cốt lõi giải quyết:** **"TẠI SAO làm dự án?" (WHY are we doing this project?)**  
**Định hướng giải pháp:** **Độc lập hoàn toàn với giải pháp công nghệ (Independent of technology solutions)**  

---

> ### 📌 Tuyên bố Vị trí Tài liệu theo Chuẩn IEEE 29148:2018
> Tài liệu BRS này thiết lập nền tảng chiến lược cấp cao nhất cho toàn bộ dự án, tập trung vào **Tầm nhìn Kinh doanh (Business Vision)**, **Mục tiêu Chiến lược (Business Objectives)**, **Chỉ số Đo lường Hiệu quả (KPIs)**, **Lợi tức Đầu tư (ROI)** và **Phạm vi Dự án (Project Scope)**. BRS hoàn toàn độc lập với công nghệ hiện thực, đóng vai trò là "kim chỉ nam" định hướng cho tài liệu Đặc tả Yêu cầu Các bên Liên quan (**StRS**) và Đặc tả Yêu cầu Phần mềm (**SRS**).

---

## Mục lục

1. [Giới thiệu](#1-giới-thiệu)
   - 1.1 [Mục đích tài liệu](#11-mục-đích-tài-liệu)
   - 1.2 [Định vị Tài liệu trong Mô hình Kim tự tháp Yêu cầu (BRS – StRS – SRS)](#12-định-vị-tài-liệu-trong-mô-hình-kim-tự-tháp-yêu-cầu-brs--strs--srs)
   - 1.3 [Phạm vi dự án](#13-phạm-vi-dự-án)
   - 1.4 [Định nghĩa và Thuật ngữ](#14-định-nghĩa-và-thuật-ngữ)
   - 1.5 [Tài liệu tham chiếu](#15-tài-liệu-tham-chiếu)
   - 1.6 [Tổng quan tài liệu](#16-tổng-quan-tài-liệu)
2. [Bối cảnh Nghiệp vụ](#2-bối-cảnh-nghiệp-vụ)
   - 2.1 [Bối cảnh Tổ chức](#21-bối-cảnh-tổ-chức)
   - 2.2 [Phát biểu Vấn đề (Problem Statement)](#22-phát-biểu-vấn-đề-problem-statement)
   - 2.3 [Cơ hội Kinh doanh (Business Opportunity)](#23-cơ-hội-kinh-doanh-business-opportunity)
   - 2.4 [Phân tích Lợi tức Đầu tư (ROI) và Giá trị Chiến lược](#24-phân-tích-lợi-tức-đầu-tư-roi-và-giá-trị-chiến-lược)
3. [Các bên Liên quan (Stakeholder Analysis)](#3-các-bên-liên-quan-stakeholder-analysis)
   - 3.1 [Danh sách Stakeholder cấp Chiến lược](#31-danh-sách-stakeholder-cấp-chiến-lược)
   - 3.2 [Ma trận Quyền lực – Quan tâm (Power-Interest)](#32-ma-trận-quyền-lực--quan-tâm-power-interest)
   - 3.3 [Ma trận Phối hợp giữa các Phân hệ Nghiệp vụ](#33-ma-trận-phối-hợp-giữa-các-phân-hệ-nghiệp-vụ)
4. [Yêu cầu Nghiệp vụ (Business Requirements)](#4-yêu-cầu-nghiệp-vụ-business-requirements)
   - 4.1 [Mục tiêu Nghiệp vụ (Business Objectives theo chuẩn SMART)](#41-mục-tiêu-nghiệp-vụ-business-objectives-theo-chuẩn-smart)
   - 4.2 [Các yếu tố Thành công Quan trọng (Critical Success Factors - CSFs)](#42-các-yếu-tố-thành-công-quan-trọng-critical-success-factors)
5. [Quy tắc Nghiệp vụ (Business Rules)](#5-quy-tắc-nghiệp-vụ-business-rules)
   - 5.1 [Phân loại Quy tắc](#51-phân-loại-quy-tắc)
   - 5.2 [Quy tắc Phân hệ Tuyển sinh](#52-quy-tắc-phân-hệ-tuyển-sinh)
   - 5.3 [Quy tắc Phân hệ Học viên](#53-quy-tắc-phân-hệ-học-viên)
   - 5.4 [Quy tắc Phân hệ Đào tạo](#54-quy-tắc-phân-hệ-đào-tạo)
   - 5.5 [Quy tắc Phân hệ Tài chính](#55-quy-tắc-phân-hệ-tài-chính)
   - 5.6 [Quy tắc Bảo mật và Hệ thống](#56-quy-tắc-bảo-mật-và-hệ-thống)
6. [Mô hình Quy trình Nghiệp vụ (Business Process Models)](#6-mô-hình-quy-trình-nghiệp-vụ-business-process-models)
   - 6.1 [Quy trình Tuyển sinh — As-Is vs. To-Be](#61-quy-trình-tuyển-sinh--as-is-hiện-tại-vs-to-be-mong-muốn)
   - 6.2 [Quy trình Điểm danh](#62-quy-trình-điểm-danh)
   - 6.3 [Quy trình Thu học phí](#63-quy-trình-thu-học-phí)
   - 6.4 [Quy trình Bảo lưu / Rút hồ sơ](#64-quy-trình-bảo-lưu--rút-hồ-sơ)
7. [Phạm vi và Giới hạn](#7-phạm-vi-và-giới-hạn)
   - 7.1 [Trong phạm vi (In-scope)](#71-trong-phạm-vi-in-scope)
   - 7.2 [Ngoài phạm vi (Out-of-scope)](#72-ngoài-phạm-vi-out-of-scope)
   - 7.3 [Phân định Phạm vi Nghiệp vụ và Phạm vi Phần mềm](#73-phân-định-phạm-vi-nghiệp-vụ-và-phạm-vi-phần-mềm)
8. [Ràng buộc và Giả định](#8-ràng-buộc-và-giả-định)
   - 8.1 [Ràng buộc (Constraints)](#81-ràng-buộc-constraints)
   - 8.2 [Giả định (Assumptions)](#82-giả-định-assumptions)
9. [Rủi ro Nghiệp vụ](#9-rủi-ro-nghiệp-vụ)
10. [Tiêu chí Nghiệm thu (Acceptance Criteria)](#10-tiêu-chí-nghiệm-thu-acceptance-criteria)
11. [Phụ lục](#11-phụ-lục)
    - 11.1 [Phân công Nhóm Phát triển](#111-phân-công-nhóm-phát-triển)
    - 11.2 [Lịch sử Thay đổi Tài liệu](#112-lịch-sử-thay-đổi-tài-liệu)

---

## 1. Giới thiệu

### 1.1 Mục đích tài liệu

Tài liệu BRS (Business Requirements Specification) mô tả **các yêu cầu ở cấp nghiệp vụ và chiến lược** (business-strategic level) cho Hệ thống Quản lý Trung tâm Anh ngữ (English Center Management System) theo tiêu chuẩn kỹ nghệ yêu cầu **IEEE 29148:2018**.

Tài liệu tập trung trả lời câu hỏi cốt lõi:
- **"TẠI SAO cần đầu tư xây dựng hệ thống?" (WHY are we doing this project?)**
- **"Hệ thống mang lại GIÁ TRỊ KINH DOANH và ROI GÌ cho tổ chức?"**
- **"Những mục tiêu chiến lược và chỉ số KPI nào cần đạt được?"**

Tài liệu được xây dựng từ **góc nhìn Kinh doanh & Chiến lược**, hoàn toàn **độc lập với giải pháp kỹ thuật và công nghệ cụ thể**. Tài liệu đóng vai trò làm chuẩn mực đối soát để Ban Giám đốc, Business Sponsor, Product Owner, Project Manager và các bên liên quan thống nhất về tầm nhìn, mục tiêu và phạm vi trước khi tiến hành khảo sát nhu cầu chi tiết (StRS) và thiết kế giải pháp phần mềm (SRS).

### 1.2 Định vị Tài liệu trong Mô hình Kim tự tháp Yêu cầu (BRS – StRS – SRS)

Theo chuẩn kỹ nghệ yêu cầu IEEE 29148:2018, hệ thống tài liệu yêu cầu được phân cấp theo mô hình kim tự tháp 3 tầng với các vai trò, góc nhìn và câu hỏi cốt lõi phân định rõ ràng:

```
                      ▲
                     / \
                    /   \
                   / BRS \          ◄── TẦNG CHIẾN LƯỢC (STRATEGIC)
                  /-------\             Trả lời: TẠI SAO? (WHY)
                 /  StRS   \        ◄── TẦNG NGƯỜI DÙNG / STAKEHOLDER
                /-----------\           Trả lời: AI CẦN GÌ? (WHO & WHAT)
               /     SRS     \      ◄── TẦNG GIẢI PHÁP KỸ THUẬT (SOFTWARE/TECH)
              /---------------\         Trả lời: LÀM THẾ NÀO? (HOW & DETAILS)
```

| Tiêu chí | BRS (Business Requirements) | StRS (Stakeholder Requirements) | SRS (Software Requirements) |
|---|---|---|---|
| **Cấp độ trừu tượng** | Cấp Chiến lược (Strategic) | Cấp Người dùng / Tác nhân (Stakeholder) | Cấp Kỹ thuật / Giải pháp (Technical / Software) |
| **Câu hỏi cốt lõi** | **TẠI SAO (WHY)** làm dự án? | **AI CẦN GÌ (WHO & WHAT)**? | **LÀM THẾ NÀO (HOW)** để đáp ứng? |
| **Trọng tâm nội dung** | Tầm nhìn, Bối cảnh, Mục tiêu SMART, KPIs, ROI, Phạm vi dự án, Quy tắc nghiệp vụ cấp cao | Nhu cầu, Kỳ vọng, User Personas, Use Case nghiệp vụ, Kịch bản vận hành (Scenarios) | Yêu cầu chức năng chi tiết, API endpoints, NFRs, Mô hình DB schema, UI/UX contract |
| **Góc nhìn (Perspective)** | Kinh doanh & Chiến lược (Business & Strategy) | Người dùng & Tác nhân tương tác (User / Stakeholder) | Kỹ thuật & Hiện thực hóa (Engineering & Implementation) |
| **Sự phụ thuộc công nghệ** | **Độc lập hoàn toàn** với giải pháp công nghệ | Độc lập với giải pháp kỹ thuật cụ thể | Phụ thuộc vào công nghệ (FastAPI, Spring Boot, PostgreSQL, Docker) |
| **Đối tượng độc giả** | Ban Giám đốc, Sponsor, Product Owner | Business Analyst, Product Owner, Đại diện Stakeholders | Software Developers, Tech Lead, QA/QC Engineers, System Architects |
| **Tác giả chủ trì** | Business Sponsor & Senior BA | Senior BA & UX/Business Analyst | Tech Lead & Software Engineers |

### 1.3 Phạm vi dự án

Hệ thống phần mềm quản lý tích hợp cho Trung tâm Anh ngữ, bao gồm 4 phân hệ chính: **Tuyển sinh**, **Học viên**, **Đào tạo** và **Tài chính**. Hệ thống phục vụ toàn bộ vòng đời của học viên: từ đăng ký nhập học, quản lý quá trình học tập, đến thu — quản lý học phí.

### 1.4 Định nghĩa và Thuật ngữ

| Thuật ngữ | Định nghĩa |
|-----------|-----------|
| **BRS** | Business Requirements Specification — Tài liệu Đặc tả Yêu cầu Nghiệp vụ (Cấp chiến lược - WHY) |
| **StRS** | Stakeholder Requirements Specification — Tài liệu Đặc tả Yêu cầu Các bên Liên quan (Cấp người dùng - WHO & WHAT) |
| **SRS** | Software Requirements Specification — Tài liệu Đặc tả Yêu cầu Phần mềm (Cấp kỹ thuật - HOW) |
| **ROI** | Return on Investment — Tỷ suất lợi tức đầu tư, đo lường hiệu quả tài chính của dự án |
| **KPI** | Key Performance Indicator — Chỉ số định lượng đo lường mức độ hoàn thành mục tiêu nghiệp vụ |
| **CSF** | Critical Success Factor — Yếu tố thành công then chốt quyết định sự thành bại của dự án |
| **SMART** | Bộ tiêu chí mục tiêu: Specific (Cụ thể) – Measurable (Đo lường được) – Achievable (Khả thi) – Relevant (Thực tế/Liên quan) – Time-bound (Có thời hạn) |
| **HV** | Học viên — người đăng ký và tham gia khóa học tại Trung tâm |
| **GV** | Giáo viên — người trực tiếp giảng dạy |
| **Enrollment** | Đơn đăng ký nhập học — liên kết học viên với một lớp cụ thể |
| **Voucher** | Mã giảm giá — áp dụng khi lập hóa đơn học phí |
| **BI** | Business Intelligence — Báo cáo thống kê phân tích kinh doanh |
| **Phân hệ** | Module phần mềm đảm nhiệm một nhóm chức năng nghiệp vụ |
| **Bảo lưu** | Tạm ngưng học — giữ hồ sơ trong thời hạn cho phép |
| **Sĩ số** | Số lượng học viên đã được duyệt trong một lớp |

### 1.5 Tài liệu tham chiếu

| Tài liệu | Mô tả | Vai trò & Liên kết |
|----------|-------|--------------------|
| `Bieu_Mau_Chi_Tiet_Tinh_Nang_Va_CSDL.docx` | Đặc tả chi tiết tính năng và CSDL (do nhóm lập) | Tài liệu phân tích nghiệp vụ nội bộ nhóm |
| `yêu cầu btl.pdf` | Yêu cầu bài tập lớn từ giảng viên | Đề bài gốc và tiêu chuẩn đánh giá môn học |
| IEEE 29148:2018 | Systems and software engineering — Requirements engineering | Chuẩn quốc tế định hình quy trình và cấu trúc đặc tả yêu cầu |
| **StRS (tài liệu đi kèm)** | **Stakeholder Requirements Specification** — Đặc tả Yêu cầu Các bên Liên quan | Cầu nối cụ thể hóa mục tiêu BRS thành nhu cầu chi tiết của từng đối tượng người dùng |
| **SRS (tài liệu đi kèm)** | **Software Requirements Specification** — Đặc tả Yêu cầu Phần mềm | Hiện thực hóa kỹ thuật các mục tiêu BRS và nhu cầu StRS thành API, DB và UI |

### 1.6 Tổng quan tài liệu

- **Phần 2:** Bối cảnh tổ chức, phát biểu vấn đề, cơ hội kinh doanh và phân tích ROI chiến lược.
- **Phần 3:** Phân tích các bên liên quan cấp chiến lược (chi tiết nhu cầu người dùng được chuyển giao sang StRS).
- **Phần 4:** Mục tiêu nghiệp vụ (SMART KPIs) và các yếu tố thành công then chốt (CSFs).
- **Phần 5:** Bộ quy tắc nghiệp vụ bất biến (Business Rules) áp dụng cho 4 phân hệ.
- **Phần 6:** Mô hình quy trình nghiệp vụ tổng thể (As-Is → To-Be).
- **Phần 7–8:** Phạm vi nghiệp vụ (In-scope / Out-of-scope), ràng buộc và giả định chiến lược.
- **Phần 9–10:** Quản trị rủi ro nghiệp vụ và tiêu chí nghiệm thu cấp dự án.
- **Phần 11:** Phụ lục phân công trách nhiệm và lịch sử thay đổi tài liệu.

---

## 2. Bối cảnh Nghiệp vụ

### 2.1 Bối cảnh Tổ chức

Trung tâm Anh ngữ (English Center) là đơn vị đào tạo ngoại ngữ quy mô vừa, hoạt động với các nghiệp vụ chính: tuyển sinh học viên mới, tổ chức giảng dạy theo khóa/lớp, quản lý hồ sơ và chuyên cần học viên, thu và đối soát học phí.

Trung tâm phục vụ khoảng 100–500 học viên đồng thời, với đội ngũ 5–20 giáo viên và 3–5 nhân viên văn phòng.

### 2.2 Phát biểu Vấn đề (Problem Statement)

```
VẤN ĐỀ:       Trung tâm Anh ngữ đang quản lý tuyển sinh, học viên,
               giảng dạy và tài chính bằng sổ sách, Excel và các công
               cụ rời rạc, thiếu tích hợp, dễ sai sót.

ẢNH HƯỞNG:    Nhân viên tuyển sinh, giáo viên, học viên, phụ huynh,
               bộ phận kế toán, ban giám đốc.

HẬU QUẢ:      - Sĩ số lớp dễ bị vượt quá giới hạn do kiểm soát thủ công
               - Điểm danh trùng lặp, sai lệch báo cáo chuyên cần
               - Thanh toán học phí bị ghi trùng hoặc bỏ sót giao dịch
               - Điểm số nhập sai (âm, vượt thang) không bị phát hiện
               - Không có báo cáo tổng thể về doanh thu và tuyển sinh
               - Hồ sơ học viên phân tán, khó tra cứu

GIẢI PHÁP     Xây dựng hệ thống phần mềm tích hợp 4 phân hệ cùng
KỲ VỌNG:      bộ kiểm thử tự động để đảm bảo toàn vẹn dữ liệu,
               tự động hóa quy trình, và cung cấp báo cáo quản trị.
```

### 2.3 Cơ hội Kinh doanh (Business Opportunity)

| STT | Cơ hội | Lợi ích kỳ vọng |
|-----|--------|----------------|
| 1 | Số hóa toàn bộ quy trình tuyển sinh | Giảm thời gian xử lý đơn, tăng trải nghiệm HV |
| 2 | Tự động hóa kiểm soát sĩ số và điểm danh | Loại bỏ sai sót thủ công |
| 3 | Tích hợp tuyển sinh – tài chính | Duyệt đơn tự tạo hóa đơn, không nhập lại |
| 4 | Dashboard BI thời gian thực | Ra quyết định nhanh, dựa trên dữ liệu |
| 5 | Kiểm soát voucher giảm giá | Tránh lạm dụng, đo lường hiệu quả chiến dịch |

### 2.4 Phân tích Lợi tức Đầu tư (ROI) và Giá trị Chiến lược

Việc xây dựng hệ thống mang lại giá trị chiến lược và hoàn vốn đầu tư rõ rệt trên cả hai khía cạnh định lượng và định tính:

#### Lợi ích Định lượng (Quantifiable ROI Metrics):
1. **Tiết kiệm thời gian vận hành:** Giảm 75% thời gian xử lý một đơn tuyển sinh (từ trung bình 20 phút xuống dưới 5 phút thông qua quy trình duyệt tự động và liên thông phân hệ Học viên – Tài chính).
2. **Triệt tiêu thất thoát tài chính:** Giảm 100% tình trạng ghi trùng thanh toán (Idempotency với `transaction_ref` duy nhất) hoặc bỏ sót đối soát hóa đơn học phí.
3. **Tối ưu hóa doanh thu từ lớp học:** Chấm dứt 100% tình trạng overbooking (quá tải phòng gây hủy lớp hoặc bồi hoàn) và tối ưu công suất phòng học lên đến 95% sức chứa chuẩn.
4. **Kiểm soát ngân sách khuyến mãi:** Chặn hoàn toàn nguy cơ lạm dụng voucher quá hạn hoặc quá lượt phát hành thông qua cơ chế kiểm tra tự động trước khi lập hóa đơn.

#### Giá trị Định tính (Strategic & Qualitative Value):
- **Trải nghiệm khách hàng xuất sắc:** Học viên và phụ huynh được phục vụ tức thì, tra cứu hồ sơ và kết quả học tập minh bạch, gia tăng chỉ số hài lòng (NPS).
- **Tính toàn vẹn dữ liệu học thuật:** Loại bỏ lỗi điểm danh khống, bảo vệ sự chính xác tuyệt đối của bảng điểm và kết quả xếp loại.
- **Năng lực ra quyết định nhanh của Ban Giám đốc:** Dashboard BI cung cấp số liệu doanh thu thực tế và phễu tuyển sinh tức thời, thay thế việc chờ đợi tổng hợp báo cáo thủ công mất từ 3 đến 5 ngày cuối tháng.

---

## 3. Các bên Liên quan (Stakeholder Analysis)

> [!NOTE]
> **Phân định ranh giới tài liệu theo chuẩn IEEE 29148:2018:**  
> Mục này trong tài liệu BRS chỉ xác định danh mục tổng quan các bên liên quan và ma trận phân công trách nhiệm ở tầm chiến lược.  
> Toàn bộ các phân tích chi tiết về **User Personas**, **Khảo sát Nhu cầu chuyên sâu (Stakeholder Needs)**, **Điểm đau (Pain Points)** và các **Kịch bản vận hành chi tiết (Operational Scenarios)** được đặc tả toàn diện trong tài liệu **StRS (Stakeholder Requirements Specification)**.

### 3.1 Danh sách Stakeholder cấp Chiến lược

| ID | Vai trò | Mô tả | Nhu cầu cấp Chiến lược | Tài liệu đặc tả chi tiết |
|----|---------|-------|------------------------|---------------------------|
| SH-01 | **Ban Giám đốc** | Ra quyết định chiến lược, đầu tư dự án | Dashboard BI, báo cáo doanh thu, phễu tuyển sinh, giám sát ROI | StRS §3.1, §4.1; SRS §4.1 (FR-ADM-06) |
| SH-02 | **Admin** | Quản trị viên hệ thống | Full access, quản lý user, cấu hình hệ thống, bảo mật toàn diện | StRS §3.2, §4.2; SRS §4.1 (FR-ADM-01) |
| SH-03 | **Staff** | Nhân viên tuyển sinh / học vụ / kế toán | Duyệt đơn nhanh, lập hóa đơn, quản lý hồ sơ, thu tiền chính xác | StRS §3.3, §4.3; SRS §4.1, §4.2, §4.4 |
| SH-04 | **Teacher** | Giáo viên giảng dạy | Điểm danh tiện lợi, nhập điểm chuẩn xác, chia sẻ học liệu an toàn | StRS §3.4, §4.4; SRS §4.2, §4.3 |
| SH-05 | **Student** | Học viên | Xem hồ sơ, điểm, hóa đơn minh bạch, tải học liệu đúng lớp | StRS §3.5, §4.5; SRS §4.2, §4.3 |
| SH-06 | **Phụ huynh** | Giám sát kết quả học tập (giai đoạn sau) | Tra cứu kết quả, tình trạng học phí minh bạch | StRS §3.6; SRS (Phase 2) |

### 3.2 Ma trận Quyền lực – Quan tâm (Power-Interest)

```
        CAO
   Quyền  │  [Giữ hài lòng]         [Quản lý chặt chẽ]
   lực    │   Ban Giám đốc           Staff, Teacher
          │
          │  [Theo dõi tối thiểu]    [Thông báo thường xuyên]
          │   Cơ quan thuế            Student, Phụ huynh
        THẤP ──────────────────────────────────────────
              THẤP              Quan tâm              CAO
```

### 3.3 Ma trận Phối hợp giữa các Phân hệ Nghiệp vụ

| Phân hệ | ↔ Tuyển sinh | ↔ Học viên | ↔ Đào tạo | ↔ Tài chính |
|---------|-------------|-----------|----------|------------|
| **Tuyển sinh** | — | Gửi `student_id` khi duyệt đơn | Cung cấp `class_id`, thông tin lớp | Gửi `enrollment_id` để tạo hóa đơn |
| **Học viên** | Nhận `student_id` cập nhật trạng thái | — | Cung cấp DS HV theo lớp | Cung cấp trạng thái học vụ |
| **Đào tạo** | Lấy thông tin GV phụ trách lớp | Lấy sĩ số lớp | — | Báo kết quả đậu/rớt |
| **Tài chính** | Lấy mức phí chuẩn khóa học | Khóa quyền nếu nợ phí | Cảnh báo HV chưa thanh toán | — |

---

## 4. Yêu cầu Nghiệp vụ (Business Requirements)

### 4.1 Mục tiêu Nghiệp vụ (Business Objectives theo chuẩn SMART)

Mỗi mục tiêu tuân thủ nghiêm ngặt tiêu chí **SMART** (Specific – Measurable – Achievable – Relevant – Time-bound), đảm bảo định hướng trực tiếp cho các yêu cầu trong StRS và SRS:

| ID | Mục tiêu Nghiệp vụ | KPI / Tiêu chí đo lường định lượng | Mức ưu tiên | Truy vết StRS (Nhu cầu) | Truy vết SRS (Giải pháp) |
|----|--------------------|-----------------------------------|-------------|--------------------------|--------------------------|
| **BO-01** | Kiểm soát sĩ số lớp tự động | 0% trường hợp vượt sĩ số `max_students` | **MUST** | StRS §4.1 (Quản lý lớp) | SRS FR-ADM-04, BR-ADM-03 |
| **BO-02** | Loại bỏ điểm danh trùng lặp | 0% bản ghi trùng (1 HV / 1 lớp / 1 ngày) | **MUST** | StRS §4.4 (Điểm danh lớp) | SRS FR-STU-02, BR-STU-07 |
| **BO-03** | Đảm bảo toàn vẹn thang điểm | 100% điểm nằm trong đoạn [0, `max_score`] | **MUST** | StRS §4.4 (Nhập điểm) | SRS FR-LRN-02, BR-LRN-04 |
| **BO-04** | Chống ghi trùng thanh toán | Mỗi `transaction_ref` chỉ ghi nhận 1 lần duy nhất | **MUST** | StRS §4.3 (Thu học phí) | SRS FR-FIN-03, BR-FIN-03 |
| **BO-05** | Cô lập dữ liệu người dùng | HV chỉ truy cập dữ liệu của chính mình (100% cách ly) | **MUST** | StRS §4.5 (Bảo mật HV) | SRS NFR-SEC-04, BR-SEC-01 |
| **BO-06** | Báo cáo quản trị thời gian thực | Dashboard phễu tuyển sinh, doanh thu, voucher cập nhật tức thì | **SHOULD** | StRS §4.1 (Báo cáo điều hành) | SRS FR-ADM-06, FR-FIN-06 |
| **BO-07** | Tự động hóa luồng tuyển sinh → tài chính | Duyệt đơn → tự động tạo hồ sơ HV + hóa đơn học phí | **SHOULD** | StRS §4.3 (Liên thông đơn) | SRS FR-ADM-04, UC-SYS-01 |
| **BO-08** | Quản lý bảo lưu / chuyển lớp có kiểm soát | Bảo lưu tối đa 6 tháng, chuyển lớp phải còn chỗ, cần phê duyệt | **SHOULD** | StRS §4.3 (Học vụ linh hoạt) | SRS FR-STU-03, BR-STU-14 |
| **BO-09** | Kiểm soát chương trình khuyến mãi | Voucher có hạn dùng, giới hạn lượt, giới hạn tối đa 50% | **SHOULD** | StRS §4.3 (Khuyến mãi) | SRS FR-FIN-01, BR-FIN-10..13 |
| **BO-10** | Theo dõi và đốc thúc công nợ | Tự chuyển OVERDUE khi quá hạn, cảnh báo dashboard | **COULD** | StRS §4.3 (Quản lý nợ) | SRS FR-FIN-02, BR-FIN-08 |

### 4.2 Các yếu tố Thành công Quan trọng (Critical Success Factors)

| CSF | Mô tả Yếu tố Thành công | Vai trò Chiến lược |
|-----|-------------------------|---------------------|
| **CSF-01** | Bộ kiểm thử tự động (15 Invariant Tests) phải luôn PASS sau mỗi lần thay đổi | Đảm bảo hệ thống không bị hồi quy (regression-free) và tuân thủ tuyệt đối quy tắc nghiệp vụ |
| **CSF-02** | Mỗi nhóm chỉ sửa code trong phạm vi phân hệ của mình (`services/{tên_phân_hệ}/`) | Tránh xung đột code, đảm bảo tính tự chủ và khả năng mở rộng dịch vụ độc lập |
| **CSF-03** | Toàn bộ API, database, JSON tuân thủ quy ước chuẩn hóa `snake_case` | Đồng nhất giao thức giao tiếp giữa các phân hệ đa ngôn ngữ (Polyglot Monorepo) |
| **CSF-04** | Không hardcode credential/secret vào source code (tuân thủ 12-Factor App) | Bảo vệ an toàn dữ liệu khách hàng và tuân thủ các quy định bảo mật thông tin |
| **CSF-05** | Dữ liệu giữa các phân hệ chỉ trao đổi qua API, không truy vấn chéo schema DB | Đảm bảo tính đóng gói (encapsulation), cô lập dữ liệu và khả năng tách service trong tương lai |

---

## 5. Quy tắc Nghiệp vụ (Business Rules)

> [!NOTE]
> Các quy tắc nghiệp vụ dưới đây là các **Bất biến Nghiệp vụ (Business Invariants)** bắt buộc phải được bảo đảm ở mọi tầng của hệ thống. Trong tài liệu **SRS (§4, §6.1)**, các quy tắc này được cụ thể hóa thành các validation logic, ràng buộc CSDL và mã lỗi HTTP phản hồi tương ứng.

### 5.1 Phân loại Quy tắc

| Loại | Mô tả | Ví dụ minh họa |
|------|-------|----------------|
| **Structural** | Quy tắc về cấu trúc, tính toàn vẹn dữ liệu, mối quan hệ | "Mã khóa học phải duy nhất trong toàn hệ thống" |
| **Action Enabler** | Điều kiện tiền đề bắt buộc để một hành động được phép thực hiện | "Chỉ duyệt đơn đăng ký khi lớp học còn chỗ trống" |
| **Computation** | Công thức tính toán và logic định lượng | "Học phí thực thu = Học phí gốc − Số tiền giảm giá" |
| **Timing** | Ràng buộc về mặt thời gian và hạn định xử lý | "Đơn đăng ký PENDING quá 48 giờ tự động EXPIRED" |

### 5.2 Quy tắc Phân hệ Tuyển sinh

| ID | Quy tắc | Loại | Hiện thực hóa tại SRS |
|----|---------|------|------------------------|
| **BR-ADM-01** | Mã khóa học (`code`) phải duy nhất trong toàn hệ thống | Structural | SRS FR-ADM-02, HTTP 409 |
| **BR-ADM-02** | Học phí (`tuition_amount`) ≥ 0 | Computation | SRS FR-ADM-02, HTTP 400 |
| **BR-ADM-03** | Sĩ số đã duyệt của lớp < `max_students` mới cho duyệt thêm | Action Enabler | SRS FR-ADM-04, HTTP 400 |
| **BR-ADM-04** | `max_students` tối thiểu 1, mặc định 25 | Structural | SRS FR-ADM-03, Check DB |
| **BR-ADM-05** | Không trùng phòng học cùng khung giờ, cùng ngày | Timing | SRS FR-ADM-03, HTTP 400 |
| **BR-ADM-06** | GV không dạy 2 lớp trùng khung giờ cùng ngày | Timing | SRS FR-ADM-03, HTTP 400 |
| **BR-ADM-07** | Sức chứa phòng (`capacity`) ≥ `max_students` | Structural | SRS FR-ADM-03, HTTP 400 |
| **BR-ADM-08** | Đơn CANCELLED không được chuyển sang APPROVED | Action Enabler | SRS FR-ADM-04, HTTP 400 |
| **BR-ADM-09** | Đơn PENDING quá 48 giờ → tự chuyển EXPIRED | Timing | SRS FR-ADM-04, Scheduler |
| **BR-ADM-10** | Mỗi HV chỉ có tối đa 1 đơn PENDING/APPROVED cho 1 lớp | Structural | SRS FR-ADM-04, HTTP 400 |
| **BR-ADM-11** | Không tạo lớp mới cho khóa học bị tạm ngưng (`is_active = false`) | Action Enabler | SRS FR-ADM-03, HTTP 400 |
| **BR-ADM-12** | Không xóa khóa học nếu đang có lớp OPEN | Action Enabler | SRS FR-ADM-02, HTTP 400 |
| **BR-ADM-13** | Lớp COMPLETED không nhận enrollment mới | Action Enabler | SRS FR-ADM-04, HTTP 400 |

### 5.3 Quy tắc Phân hệ Học viên

| ID | Quy tắc | Loại | Hiện thực hóa tại SRS |
|----|---------|------|------------------------|
| **BR-STU-01** | Email hợp lệ theo chuẩn RFC 5322 | Structural | SRS FR-STU-01, Pydantic |
| **BR-STU-02** | Email phải duy nhất trong toàn hệ thống | Structural | SRS FR-STU-01, HTTP 409 |
| **BR-STU-03** | SĐT phải đúng 10 chữ số (nếu cung cấp) | Structural | SRS FR-STU-01, Pydantic |
| **BR-STU-04** | Ngày sinh phải trước ngày hiện tại ít nhất 10 năm | Structural | SRS FR-STU-01, Pydantic |
| **BR-STU-05** | Mã HV tự sinh, duy nhất, format `HV-YYYY-XXXX` | Structural | SRS FR-STU-01, Logic gen |
| **BR-STU-06** | Trạng thái HV ∈ {STUDYING, RESERVED, DROPPED, GRADUATED} | Structural | SRS FR-STU-01, DB Check |
| **BR-STU-07** | Mỗi HV chỉ có tối đa 1 điểm danh / ngày / lớp | Action Enabler | SRS FR-STU-02, HTTP 409 |
| **BR-STU-08** | Trạng thái điểm danh ∈ {PRESENT, ABSENT, LATE, EXCUSED} | Structural | SRS FR-STU-02, Enum DB |
| **BR-STU-09** | Ngày điểm danh ≤ ngày hiện tại (không cho tương lai) | Timing | SRS FR-STU-02, HTTP 400 |
| **BR-STU-10** | Ngày điểm danh nằm trong khoảng thời gian lớp diễn ra | Timing | SRS FR-STU-02, HTTP 400 |
| **BR-STU-11** | Chỉ điểm danh HV đã được duyệt (APPROVED) trong lớp đó | Action Enabler | SRS FR-STU-02, HTTP 400 |
| **BR-STU-12** | GV chỉ điểm danh lớp mình phụ trách | Action Enabler | SRS FR-STU-02, HTTP 403 |
| **BR-STU-13** | HV chỉ xem hồ sơ của chính mình | Action Enabler | SRS FR-STU-01, HTTP 403 |
| **BR-STU-14** | Bảo lưu tối đa 6 tháng, quá hạn → tự chuyển DROPPED | Timing | SRS FR-STU-03, Background job |
| **BR-STU-15** | HV trạng thái DROPPED không được đăng ký lớp mới | Action Enabler | SRS FR-STU-01, HTTP 400 |
| **BR-STU-16** | Chuyển lớp chỉ được phép nếu lớp đích còn chỗ | Action Enabler | SRS FR-STU-03, HTTP 400 |
| **BR-STU-17** | Không tạo yêu cầu mới khi đang có PENDING chưa xử lý | Action Enabler | SRS FR-STU-03, HTTP 400 |
| **BR-STU-18** | Không xóa HV nếu còn đơn đăng ký APPROVED | Action Enabler | SRS FR-STU-01, HTTP 400 |

### 5.4 Quy tắc Phân hệ Đào tạo

| ID | Quy tắc | Loại | Hiện thực hóa tại SRS |
|----|---------|------|------------------------|
| **BR-LRN-01** | Đường dẫn tài liệu (`file_url`) không được để trống | Structural | SRS FR-LRN-01, HTTP 400 |
| **BR-LRN-02** | File upload tối đa 50MB | Structural | SRS FR-LRN-01, Gateway check |
| **BR-LRN-03** | Loại tài liệu ∈ {DOCUMENT, VIDEO, SLIDE, EXERCISE} | Structural | SRS FR-LRN-01, Enum DB |
| **BR-LRN-04** | 0 ≤ `score` ≤ `max_score` (chặn điểm âm và vượt thang) | Computation | SRS FR-LRN-02, HTTP 400 |
| **BR-LRN-05** | `max_score` > 0 | Computation | SRS FR-LRN-02, HTTP 400 |
| **BR-LRN-06** | Hệ số điểm `weight` ∈ [0.0, 1.0] | Computation | SRS FR-LRN-02, HTTP 400 |
| **BR-LRN-07** | HV chỉ xem điểm của chính mình | Action Enabler | SRS FR-LRN-02, HTTP 403 |
| **BR-LRN-08** | GV chỉ nhập điểm và upload tài liệu cho lớp mình phụ trách | Action Enabler | SRS FR-LRN-01/02, HTTP 403 |
| **BR-LRN-09** | HV chỉ xem học liệu của lớp mình đang học | Action Enabler | SRS FR-LRN-01, HTTP 403 |
| **BR-LRN-10** | Không sửa điểm đã nhập quá 7 ngày (trừ ADMIN) | Timing | SRS FR-LRN-02, HTTP 403 |
| **BR-LRN-11** | Chỉ tính kết quả đậu/rớt khi HV có ≥ 2 bài đánh giá | Computation | SRS FR-LRN-03, Logic báo cáo |
| **BR-LRN-12** | Điểm trung bình ≥ 50% → Đậu (Pass) | Computation | SRS FR-LRN-03, Logic báo cáo |
| **BR-LRN-13** | Trung bình tính theo trọng số: Σ(score × weight) / Σ(weight) | Computation | SRS FR-LRN-03, Formula |

### 5.5 Quy tắc Phân hệ Tài chính

| ID | Quy tắc | Loại | Hiện thực hóa tại SRS |
|----|---------|------|------------------------|
| **BR-FIN-01** | Số tiền thanh toán > 0 | Computation | SRS FR-FIN-03, HTTP 400 |
| **BR-FIN-02** | Mã hóa đơn duy nhất, format `INV-YYYY-XXX` | Structural | SRS FR-FIN-02, HTTP 409 |
| **BR-FIN-03** | `transaction_ref` duy nhất — chống ghi trùng (Idempotency) | Structural | SRS FR-FIN-03, HTTP 409 |
| **BR-FIN-04** | Hóa đơn chỉ chuyển PAID khi tổng thanh toán ≥ `final_amount` | Computation | SRS FR-FIN-03, State transition |
| **BR-FIN-05** | Hình thức thanh toán ∈ {CASH, BANK_TRANSFER, VIETQR} | Structural | SRS FR-FIN-03, Enum DB |
| **BR-FIN-06** | Tiền tệ mặc định VND | Structural | SRS FR-FIN-01, Config |
| **BR-FIN-07** | `due_date` ≥ ngày tạo hóa đơn | Timing | SRS FR-FIN-02, HTTP 400 |
| **BR-FIN-08** | Quá `due_date` mà chưa PAID → tự chuyển OVERDUE | Timing | SRS FR-FIN-02, Background job |
| **BR-FIN-09** | Hóa đơn quá hạn > 30 ngày → cảnh báo trên dashboard | Timing | SRS FR-FIN-06, Dashboard alert |
| **BR-FIN-10** | Voucher code duy nhất | Structural | SRS FR-FIN-01, HTTP 409 |
| **BR-FIN-11** | `used_count` ≤ `max_uses` — hết lượt thì từ chối | Action Enabler | SRS FR-FIN-01, HTTP 400 |
| **BR-FIN-12** | Voucher quá `expires_at` → không áp dụng | Timing | SRS FR-FIN-01, HTTP 400 |
| **BR-FIN-13** | Giảm giá không vượt 50% giá trị nếu loại PERCENTAGE | Computation | SRS FR-FIN-01, HTTP 400 |
| **BR-FIN-14** | `final_amount` = `amount` − `discount_applied` (luôn ≥ 0) | Computation | SRS FR-FIN-02, Calculation |
| **BR-FIN-15** | Không thanh toán cho hóa đơn CANCELLED | Action Enabler | SRS FR-FIN-03, HTTP 400 |
| **BR-FIN-16** | Không tạo 2 hóa đơn cùng enrollment chưa CANCELLED | Action Enabler | SRS FR-FIN-02, HTTP 400 |
| **BR-FIN-17** | Thanh toán dư → ghi nhận vào ví HV | Computation | SRS FR-FIN-04, Wallet topup |
| **BR-FIN-18** | Số dư ví ≥ 0 | Computation | SRS FR-FIN-04, DB Check |
| **BR-FIN-19** | Hoàn phí cần 2 cấp duyệt | Action Enabler | SRS FR-FIN-05, Workflow |
| **BR-FIN-20** | Số tiền hoàn ≤ số tiền đã thu thực tế | Computation | SRS FR-FIN-05, HTTP 400 |

### 5.6 Quy tắc Bảo mật và Hệ thống

| ID | Quy tắc | Loại | Hiện thực hóa tại SRS |
|----|---------|------|------------------------|
| **BR-SEC-01** | HV chỉ truy cập dữ liệu của chính mình (hồ sơ, điểm, hóa đơn) | Action Enabler | SRS NFR-SEC-04, RBAC/JWT |
| **BR-SEC-02** | GV chỉ thao tác trên lớp mình phụ trách | Action Enabler | SRS NFR-SEC-04, RBAC check |
| **BR-SEC-03** | Không hardcode credential/secret vào source code | Structural | SRS NFR-SEC-07, Env variables |
| **BR-SYS-01** | Toàn bộ API, DB, JSON phải dùng `snake_case` | Structural | SRS NFR-MAINT-01, Code convention |
| **BR-SYS-02** | Mỗi phân hệ chỉ truy vấn schema DB của chính mình — cấm cross-schema | Structural | SRS NFR-SEC-05, Service isolation |

---

## 6. Mô hình Quy trình Nghiệp vụ (Business Process Models)

> [!NOTE]
> Các quy trình dưới đây mô tả luồng cộng tác ở mức vĩ mô giữa các phân hệ nghiệp vụ. Chi tiết hành vi người dùng, biểu mẫu tương tác và kịch bản người dùng (Operational Scenarios) được mô tả trong **StRS (§4)**; chi tiết chuỗi gọi API nội bộ (Sequence Diagrams) được đặc tả trong **SRS (§4)**.

### 6.1 Quy trình Tuyển sinh — As-Is (Hiện tại) vs. To-Be (Mong muốn)

**As-Is (Thủ công hiện tại):**
```
HV nộp hồ sơ giấy → NV nhập Excel → Trưởng BP duyệt thủ công
→ Gọi điện thông báo → Thu tiền mặt tại quầy → Ghi sổ kế toán
```

**To-Be (Hệ thống phần mềm tích hợp):**
```
HV/Staff tạo đơn trên hệ thống
         │
         ▼
Tạo enrollment PENDING (giữ chỗ 48 giờ)
         │
    ┌────┴────┐
  Lớp đầy?  CÓ → Trả lỗi, giữ PENDING, thông báo
   KHÔNG
         ↓
Staff duyệt → APPROVED
         │
    ┌────┴────────────────────┐
    ▼                         ▼
Tự tạo hồ sơ HV          Tự tạo hóa đơn
(Phân hệ Học viên)        (Phân hệ Tài chính)
```

### 6.2 Quy trình Điểm danh

```
GV chọn lớp + ngày
         │
    GV phụ trách lớp?
    KHÔNG → Từ chối (403 Forbidden)
    CÓ ↓
         │
    Đã điểm danh HV này hôm nay?
    RỒI → Từ chối (409 Conflict — trùng)
    CHƯA ↓
         │
    Lưu: PRESENT / ABSENT / LATE / EXCUSED
```

### 6.3 Quy trình Thu học phí

```
Staff nhập: mã hóa đơn, số tiền, hình thức, mã tham chiếu
         │
    transaction_ref đã tồn tại?
    CÓ → Từ chối (409 Conflict — Idempotency)
    KHÔNG ↓
         │
    Ghi nhận payment → Cập nhật tổng đã thu
         │
    Tổng thu ≥ giá trị hóa đơn?
    CÓ → Chuyển hóa đơn → PAID
    CHƯA → Giữ trạng thái hiện tại
```

### 6.4 Quy trình Bảo lưu / Rút hồ sơ

```
HV tạo yêu cầu (RESERVE / TRANSFER / DROP) + lý do
         │
    Hệ thống lưu: PENDING
         │
    Staff duyệt → APPROVED / REJECTED
         │
    ┌────┴────┐
  RESERVE:      DROP:
  HV → RESERVED  HV → DROPPED
  (Hết 6 tháng    + Thông báo Tài chính
   → tự DROPPED)    xử lý hoàn phí
```

---

## 7. Phạm vi và Giới hạn

### 7.1 Trong phạm vi (In-scope)

| Lĩnh vực Nghiệp vụ | Chức năng bao gồm | Mục tiêu chiến lược hỗ trợ |
|---------------------|-------------------|----------------------------|
| **Tuyển sinh** | Quản lý Khóa học, Phòng học, Lớp học, Xét duyệt Đơn đăng ký, Bài thi đầu vào phân lớp, Báo cáo BI tuyển sinh | Tối ưu hóa chuyển đổi tuyển sinh, kiểm soát sĩ số (BO-01, BO-06, BO-07) |
| **Học viên** | Quản lý Hồ sơ học viên cá nhân, Điểm danh chuyên cần theo lớp, Quy trình xét duyệt Bảo lưu / Chuyển lớp / Rút hồ sơ | Tự động hóa quản lý quá trình học vụ, chống sai sót chuyên cần (BO-02, BO-05, BO-08) |
| **Đào tạo** | Quản lý Kho học liệu & Slide bài giảng theo lớp, Sổ điểm đánh giá đa thành phần, Báo cáo xếp loại và tỷ lệ đậu/rớt | Đảm bảo chất lượng giảng dạy, toàn vẹn dữ liệu đánh giá học thuật (BO-03) |
| **Tài chính** | Thiết lập Biểu phí chuẩn, Quản lý Voucher chiến dịch, Lập Hóa đơn học phí, Ghi nhận Thanh toán đối soát, Quản lý Ví học viên, Quy trình Hoàn phí, Báo cáo BI doanh thu | Triệt tiêu thất thoát tài chính, chống trùng thanh toán, kiểm soát công nợ (BO-04, BO-09, BO-10) |
| **Hạ tầng & Nền tảng** | Xác thực JWT tập trung, Phân quyền RBAC 4 cấp, Cổng API Gateway định tuyến, Bộ kiểm thử tự động Invariant Tests | Đảm bảo tính bảo mật, toàn vẹn dữ liệu và độ tin cậy vận hành (CSF-01..05) |

### 7.2 Ngoài phạm vi (Out-of-scope)

Hệ thống trong giai đoạn này chủ động loại trừ các phạm vi sau nhằm tập trung nguồn lực vào việc hoàn thiện các nghiệp vụ cốt lõi:
- Ứng dụng di động dành riêng (Native Mobile App trên iOS / Android).
- Tích hợp trực tiếp với các hệ thống kế toán doanh nghiệp bên ngoài (như SAP, MISA, FAST).
- Quản lý nhân sự hành chính, chấm công và tính lương chi tiết cho giáo viên.
- Hệ thống phòng học ảo và hội nghị truyền hình trực tuyến (Live e-learning / Video call / WebRTC).
- Cổng dịch vụ gửi tin nhắn SMS Brandname hoặc Email Marketing tự động (dự kiến xem xét tại Giai đoạn 2).
- Tích hợp trực tiếp cổng thanh toán trực tuyến của bên thứ ba (VNPay, Momo, ZaloPay – dự kiến triển khai tại Giai đoạn 2).

### 7.3 Phân định Phạm vi Nghiệp vụ và Phạm vi Phần mềm

| Đặc trưng | Phạm vi Nghiệp vụ (Business Scope - BRS) | Phạm vi Các bên Liên quan (Stakeholder Scope - StRS) | Phạm vi Phần mềm (Software Scope - SRS) |
|---|---|---|---|
| **Mục tiêu xác định** | Xác định các lĩnh vực kinh doanh trọng yếu trung tâm cần số hóa | Xác định quyền hạn và kịch bản tương tác của từng nhóm người dùng | Xác định các ranh giới dịch vụ, API endpoints và cấu trúc dữ liệu cụ thể |
| **Cách tiếp cận** | Định hướng theo kết quả kinh doanh và chỉ số KPI | Định hướng theo trải nghiệm người dùng và nhu cầu tác vụ | Định hướng theo kiến trúc module, hiệu năng và chuẩn công nghệ |
| **Ranh giới nghiệm thu** | Đạt mục tiêu không overbook, không trùng tiền, báo cáo kịp thời | Đáp ứng trọn vẹn Use Case của Giám đốc, Staff, GV và HV | Đạt 15/15 Invariant Tests, response time < 2s, 100% snake_case |

---

## 8. Ràng buộc và Giả định

### 8.1 Ràng buộc (Constraints)

| ID | Nhóm Ràng buộc | Nội dung Ràng buộc Cụ thể |
|----|----------------|---------------------------|
| **CON-01** | **Thời gian triển khai** | Toàn bộ dự án phải hoàn thành và nghiệm thu trong phạm vi 1 học kỳ (khoảng 15 tuần làm việc) |
| **CON-02** | **Nguồn lực nhân sự** | Đội ngũ triển khai gồm 9 sinh viên kỹ thuật, phân chia thành 4 nhóm phụ trách 4 phân hệ độc lập |
| **CON-03** | **Công nghệ nền tảng** | Bắt buộc chạy trên nền tảng Docker Compose; CSDL PostgreSQL 16 Alpine; Service Tuyển sinh dùng Java/Spring Boot; 3 service còn lại dùng Python/FastAPI |
| **CON-04** | **Môi trường vận hành** | Hệ thống phải triển khai mượt mà trên môi trường máy tính cá nhân của nhà phát triển (Localhost), không phụ thuộc vào hạ tầng cloud trả phí |
| **CON-05** | **Cô lập mã nguồn** | Mỗi nhóm phát triển chỉ được phép chỉnh sửa mã nguồn trong thư mục riêng của mình (`services/{tên_phân_hệ}/`) |

### 8.2 Giả định (Assumptions)

| ID | Giả định Chiến lược |
|----|---------------------|
| **ASM-01** | Mỗi học viên tham gia học tập chỉ sử dụng duy nhất một địa chỉ email định danh trên toàn hệ thống |
| **ASM-02** | Trung tâm hiện tại hoạt động tập trung tại 1 cơ sở duy nhất (chưa mở rộng chi nhánh đa địa điểm) |
| **ASM-03** | Việc ghi nhận thanh toán học phí do nhân viên văn phòng (Staff) thực hiện đối soát thủ công từ sao kê ngân hàng hoặc tiền mặt tại quầy |
| **ASM-04** | Tất cả thành viên trong nhóm phát triển đã được trang bị kiến thức nền tảng về Git, Docker, RESTful API và ngôn ngữ lập trình được giao |
| **ASM-05** | Dữ liệu mẫu (seed data) ban đầu phục vụ kiểm thử và demo được nạp sẵn thông qua script `scripts/init-db.sql` |

---

## 9. Rủi ro Nghiệp vụ

| ID | Rủi ro Nghiệp vụ | Xác suất | Mức độ Tác động | Biện pháp Phòng ngừa và Giảm thiểu Chiến lược |
|----|------------------|----------|-----------------|-----------------------------------------------|
| **RISK-01** | Lớp học bị overbook (sĩ số thực tế vượt quá sức chứa lớp/phòng) | Trung bình | Cao | Bắt buộc áp dụng quy tắc **BR-ADM-03**: kiểm tra sĩ số nguyên tử (atomic check) ngay tại thời điểm duyệt đơn |
| **RISK-02** | Thanh toán bị ghi nhận trùng lặp, gây sai lệch báo cáo tài chính | Cao | Rất cao | Bắt buộc áp dụng quy tắc **BR-FIN-03**: tạo chỉ mục duy nhất (UNIQUE constraint) trên trường `transaction_ref` |
| **RISK-03** | Điểm danh bị ghi nhận trùng nhiều lần trong ngày cho cùng 1 học viên | Trung bình | Trung bình | Bắt buộc áp dụng quy tắc **BR-STU-07**: UNIQUE constraint trên bộ khóa `(class_id, student_id, session_date)` |
| **RISK-04** | Điểm số nhập sai thang đo (điểm âm, điểm vượt quá thang tối đa) | Thấp | Cao | Bắt buộc áp dụng quy tắc **BR-LRN-04**: kiểm tra chặn cứng `0 ≤ score ≤ max_score` tại cả backend API và form frontend |
| **RISK-05** | Rò rỉ dữ liệu chéo giữa các học viên hoặc giáo viên khác lớp | Thấp | Rất cao | Bắt buộc thực thi quy tắc **BR-SEC-01** & **BR-SEC-02**: xác thực quyền sở hữu tài nguyên qua token JWT trước khi trả kết quả |
| **RISK-06** | Các service truy vấn chéo schema CSDL, phá vỡ kiến trúc microservices | Trung bình | Cao | Bắt buộc thực thi quy tắc **BR-SYS-02**: cấu hình quyền tài khoản CSDL riêng biệt kết hợp quy trình Code Review nghiêm ngặt |
| **RISK-07** | Mã voucher bị lạm dụng quá lượt hoặc sử dụng khi đã hết hạn | Trung bình | Trung bình | Bắt buộc áp dụng quy tắc **BR-FIN-11** & **BR-FIN-12**: tự động khóa voucher khi `used_count ≥ max_uses` hoặc quá `expires_at` |
| **RISK-08** | Học viên duy trì trạng thái bảo lưu vô thời hạn gây đọng dữ liệu | Thấp | Trung bình | Bắt buộc thực thi quy tắc **BR-STU-14**: tác vụ định kỳ tự động chuyển học viên sang `DROPPED` sau 6 tháng bảo lưu |
| **RISK-09** | Xung đột mã nguồn giữa 4 nhóm phát triển trong quá trình tích hợp | Trung bình | Cao | Thực thi nghiêm ngặt ràng buộc **CON-05**: phân vùng thư mục độc lập, tích hợp qua hợp đồng REST API chuẩn hóa |

---

## 10. Tiêu chí Nghiệm thu (Acceptance Criteria)

Các tiêu chí nghiệm thu cấp dự án là căn cứ tối hậu để Ban Giám đốc và Giảng viên đánh giá sự thành công của hệ thống:

| STT | Hạng mục Kiểm tra Bắt buộc | Phân hệ Phụ trách | Căn cứ BRS Mục tiêu | Căn cứ Kỹ thuật SRS |
|:---:|----------------------------|:-----------------:|:-------------------:|:-------------------:|
| **1** | Khởi chạy lệnh `python -m unittest discover tests/invariants` phải đạt kết quả tuyệt đối **15/15 OK** | Cả 4 phân hệ | CSF-01, CSF-02 | Invariant Test Suite |
| **2** | API duyệt đơn đăng ký **chặn thành công** khi lớp đã đạt tối đa `max_students` (trả về mã HTTP 400 Bad Request) | Tuyển sinh (Admission) | BO-01, BR-ADM-03 | SRS FR-ADM-04 (`ADM_CLASS_FULL`) |
| **3** | API điểm danh **ghi nhận thành công** lần đầu và **chặn tuyệt đối** khi điểm danh trùng 2 lần/ngày (trả về mã HTTP 409 Conflict) | Học viên (Student) | BO-02, BR-STU-07 | SRS FR-STU-02 (Unique violation) |
| **4** | API nhập điểm kiểm tra chặt chẽ điều kiện `0 ≤ score ≤ max_score`, **chặn đứng điểm âm và điểm vượt trần** (HTTP 400) | Đào tạo (Learning) | BO-03, BR-LRN-04 | SRS FR-LRN-02 (`LRN_INVALID_SCORE`) |
| **5** | API thanh toán chỉ đổi trạng thái sang `PAID` khi nộp đủ tiền; **chặn tuyệt đối trùng `transaction_ref`** (HTTP 409 Conflict) | Tài chính (Finance) | BO-04, BR-FIN-03 | SRS FR-FIN-03 (`FIN_DUPLICATE_TRANSACTION`) |

---

## 11. Phụ lục

### 11.1 Phân công Nhóm Phát triển

| Phân hệ | Thành viên Đảm nhiệm | Thư mục Mã nguồn |
|---------|---------------------|------------------|
| **Tuyển sinh (Admission)** | Nguyễn Mạnh Hưng, Nghiêm Đình Tâm | `services/admission/` |
| **Học viên (Student)** | Lê Hữu Phong, Đào Trọng Phương | `services/student/` |
| **Đào tạo (Learning)** | Phạm Anh Nam, Lê Hữu Phong | `services/learning/` |
| **Tài chính (Finance)** | Lê Thị Thủy Chi, Vũ Hoàng Phúc | `services/finance/` |

### 11.2 Lịch sử Thay đổi Tài liệu

| Phiên bản | Ngày | Người sửa | Nội dung Thay đổi Chi tiết |
|-----------|------|-----------|----------------------------|
| **1.0** | 11/11/2026 | Nhóm CNPM | Khởi tạo tài liệu BRS phiên bản ban đầu. |
| **1.1** | 11/11/2026 | Nhóm CNPM | Chuẩn hóa tài liệu toàn diện theo chuẩn quốc tế **IEEE 29148:2018**:<br>- Bổ sung header phân định vai trò chiến lược (WHY, ROI, KPI, độc lập giải pháp công nghệ).<br>- Bổ sung Mục 1.2 định vị mô hình Kim tự tháp Yêu cầu 3 tầng (**BRS – StRS – SRS**).<br>- Phân tích giá trị Lợi tức Đầu tư (**ROI**) định lượng và định tính tại Mục 2.4.<br>- Làm rõ phân định ranh giới giữa BRS và **StRS** tại Mục 3 (chuyển giao chi tiết nhu cầu người dùng sang StRS).<br>- Bổ sung cột ma trận truy vết từ Mục tiêu Nghiệp vụ (BO) và Quy tắc Nghiệp vụ (BR) sang StRS và SRS.<br>- Cập nhật bảng tài liệu tham chiếu hướng tới tài liệu `StRS` thay thế `SysRS`. |

---

*Chuẩn tham chiếu: IEEE 29148:2018 — Systems and software engineering — Life cycle processes — Requirements engineering*
