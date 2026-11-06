# NHẬT KÝ PHỐI HỢP & TIẾN ĐỘ THỰC HIỆN (WORKLOG)
## PHÂN HỆ TÀI CHÍNH & HỌC PHÍ (FINANCE SERVICE)

> **Nhóm thực hiện**: Phân hệ 4 (Chi & Phúc)  
> **Quy ước**: Cập nhật nhật ký công việc sau mỗi buổi làm việc hoặc sau khi hoàn thành task.

---

### [Ngày 06/11/2026] - Khởi tạo Khung Đặc tả & Danh mục Task Phân hệ Finance
- **Người thực hiện**: Chi
- **Nội dung công việc**:
  - Hoàn thiện toàn bộ 5 tài liệu đặc tả chuẩn:
    1. `tong_hop_finance_tinh_nang_va_schema_ver2.md`: Danh mục 9 tính năng (FIN-F01 đến FIN-F09), 8 bảng CSDL, 2 BI Views và toàn văn DDL SQL.
    2. `finance-ba-bi-spec.md` & `finance-ba-bi-spec_ver2.md`: Đặc tả chi tiết phân tích nghiệp vụ, tác nhân, quy tắc bất biến INV-FIN-01 đến INV-FIN-07 và KPI quản trị.
    3. `finance-tech-spec.md`: Đặc tả kiến trúc kỹ thuật FastAPI, JWT, tích hợp Admission/Learning, xử lý khóa dòng `SELECT FOR UPDATE` và Idempotency VietQR.
    4. `finance-schema-spec.md`: Từ điển dữ liệu 8 bảng, constraints, State Machine hóa đơn/hoàn tiền và DDL.
    5. `finance-uml-spec.md`: Sơ đồ Use Case, Sequence Diagram sinh hóa đơn VietQR, Sequence Diagram hoàn tiền 3 cấp và ERD.
  - Xây dựng cấu trúc điều phối công việc `services/finance/docs/tasks/`:
    - `README.md`: Kế hoạch và danh mục 24 tasks chia theo 8 Phases.
    - `CHECKLIST.md`: Bảng theo dõi trạng thái công việc.
    - `API_CATALOG.md`: Danh mục đầy đủ các REST API endpoints của Finance.
- **Trạng thái**: Đã đồng bộ lên nhánh `feature/finance`.
