# Kế hoạch & Danh mục Task Phân hệ Tài chính & Học phí (Finance Service)

> **Căn cứ tài liệu kỹ thuật chuẩn (SSOT - Finance Module 2.0)**:
> - Đặc tả Nghiệp vụ & BI: [`../docs/finance-ba-bi-spec.md`](../docs/finance-ba-bi-spec.md)
> - Đặc tả Kiến trúc Kỹ thuật: [`../docs/finance-tech-spec.md`](../docs/finance-tech-spec.md)
> - Đặc tả Cơ sở dữ liệu: [`../docs/finance-schema-spec.md`](../docs/finance-schema-spec.md)
> - Đặc tả Thiết kế UML: [`../docs/finance-uml-spec.md`](../docs/finance-uml-spec.md)
> - Tổng hợp Tính năng & DDL: [`../docs/tong_hop_finance_tinh_nang_va_schema.md`](../docs/tong_hop_finance_tinh_nang_va_schema.md)
> - **Bảng Theo dõi Tiến độ (Checklist)**: [`CHECKLIST.md`](CHECKLIST.md)
> - **Nhật ký Phối hợp Nhóm (Worklog)**: [`WORKLOG.md`](WORKLOG.md)
> - **Danh mục Toàn bộ API Endpoints Dự kiến**: [`API_CATALOG.md`](API_CATALOG.md)
> - Quy chuẩn Lập trình: [`../../../AGENTS.md`](../../../AGENTS.md) | [`../../../GEMINI.md`](../../../GEMINI.md)

---

## 1. Tổng quan Kiến trúc & Nguyên tắc Triển khai

Phân hệ **Finance Service** (Port 8084, Gateway `/api/finance/...`) được xây dựng trên nền tảng **Python 3.11 + FastAPI**, chịu trách nhiệm:
1. Quản lý chiến dịch Voucher, giảm giá phần trăm/cố định và kiểm soát hạn mức sử dụng (FIN-F01, FIN-F02).
2. Tự động sinh hóa đơn khi Admission chốt đăng ký, gộp/tách phụ phí học lại và thiết lập lịch trả góp `installments` (FIN-F03, FIN-F04).
3. Thu học phí đa kênh: Phiếu thu tiền mặt tại quầy (`CASH`) và thanh toán chuyển khoản qua mã VietQR động kèm Webhook tự động gạch nợ (FIN-F05, FIN-F06).
4. Quản lý ví bảo lưu học viên nội bộ, cộng tiền bảo lưu và cấn trừ học phí khóa học tiếp theo (FIN-F07).
5. Quy trình hoàn tiền 3 cấp nghiêm ngặt: Kiểm tra số buổi thực học từ Learning Service, tính số tiền hoàn sau khấu trừ, phê duyệt tuần tự Tư vấn viên $\rightarrow$ Kế toán $\rightarrow$ Giám đốc (FIN-F08).
6. Cung cấp hệ thống báo cáo quản trị thông minh (BI Cash-basis, hiệu quả Voucher, tỷ lệ hoàn phí và quỹ bảo lưu) qua 2 BI Views (FIN-F09).

### Nguyên tắc Bất biến Bắt buộc Tuân thủ (Finance Invariants):
- **INV-FIN-01 (Bất biến hóa đơn đã thanh toán)**: Hóa đơn ở trạng thái `PAID` hoặc `REFUNDED` tuyệt đối không được xóa hoặc sửa trực tiếp các cột tiền tệ (`amount`, `discount_applied`, `final_amount`).
- **INV-FIN-02 (Đẳng thức toán học hóa đơn và trả góp)**: `final_amount = amount - discount_applied >= 0`; `0 <= paid_amount <= final_amount`; tổng `amount_due` của các `installments` bằng đúng `final_amount`.
- **INV-FIN-03 (Cô lập Schema)**: Finance chỉ truy cập schema `finance.*`. Các cột `student_id`, `course_id`, `class_id` là Logical FK kiểu INTEGER, không JOIN/FK vật lý sang schema khác.
- **INV-FIN-04 (Chống Race Condition Voucher)**: Kiểm tra `is_active`, `expires_at`, `current_uses < max_uses` bằng khóa dòng `SELECT ... FOR UPDATE` trước khi tăng `current_uses`.
- **INV-FIN-05 (Idempotency Webhook VietQR)**: `bank_transaction_id` UNIQUE; giao dịch đã tồn tại trả `200 OK` nhưng không ghi nhận phiếu thu lần hai.
- **INV-FIN-06 (Khóa rút tiền ví bảo lưu)**: Bảng `finance.student_wallets` luôn giữ `is_withdrawable = FALSE`; số dư chỉ giảm qua `TUITION_OFFSET`.
- **INV-FIN-07 (Chặn hoàn tiền theo số buổi)**: Chỉ tạo yêu cầu hoàn tiền khi `attended_sessions <= max_allowed_sessions` và đi qua đủ 3 cấp duyệt.

---

## 2. Ma trận Phân kỳ & Danh mục Toàn bộ Tasks (WBS)

Hệ thống được chia thành **8 Phases** với tổng cộng **24 Tasks** chi tiết:

```
Phase 0: Nền móng & Hạ tầng (INF)
   │
   ├── Phase 1: Biểu phí & Chiến dịch Voucher (VOU) ──┐
   │                                                  │
   ├── Phase 2: Hóa đơn & Lịch Đóng phí (INV/INS) ────┼── Phase 3: Thu tiền & Gạch nợ VietQR (PAY)
   │                                                  │         │
   └── Phase 4: Ví Bảo lưu Học viên (WAL) ────────────┘         ├── Phase 5: Quy trình Hoàn tiền 3 cấp (REF)
                                                                │
                                                                ├── Phase 6: Báo cáo Thông minh BI (BI)
                                                                │
                                                                └── Phase 7: Giao diện Người dùng (UI)
```

### Bảng Tổng hợp Toàn bộ Tasks

| Task ID | Tên Nhiệm vụ | FR / Invariant | Độ ưu tiên | File Task Chi tiết |
|---|---|---|---|---|
| **Phase 0: Nền móng & Hạ tầng (INF)** | | | | |
| `TASK-FIN-00` | Thiết lập Nền tảng FastAPI, Pydantic v2 snake_case, JWT Middleware & Exception Handler | N/A | **P0** (Bắt buộc) | [`TASK-FIN-00-PROJECT-FOUNDATION.md`](phase-0-foundation/TASK-FIN-00-PROJECT-FOUNDATION.md) |
| `TASK-FIN-01` | Khởi tạo Schema PostgreSQL 8 Bảng, 2 BI Views, Ràng buộc DDL & Seed Data | N/A | **P0** (Bắt buộc) | [`TASK-FIN-01-SCHEMA-MIGRATION.md`](phase-0-foundation/TASK-FIN-01-SCHEMA-MIGRATION.md) |
| `TASK-FIN-02` | Cấu hình HTTP Clients Tích hợp Liên Service (Admission & Learning Service) | N/A | **P1** | [`TASK-FIN-02-INTERSERVICE-CLIENTS.md`](phase-0-foundation/TASK-FIN-02-INTERSERVICE-CLIENTS.md) |
| **Phase 1: Biểu phí & Chiến dịch Voucher (VOU)** | | | | |
| `TASK-FIN-10` | Quản lý Chiến dịch Voucher (Tạo mới, Cập nhật, Giới hạn lượt dùng & Hạn dùng) | FIN-F01 | **P1** | [`TASK-FIN-10-VOUCHER-MANAGEMENT.md`](phase-1-vouchers/TASK-FIN-10-VOUCHER-MANAGEMENT.md) |
| `TASK-FIN-11` | Động cơ Kiểm tra Voucher & Áp dụng Khóa dòng `SELECT FOR UPDATE` Chống Race Condition | FIN-F02, INV-FIN-04 | **P0** (Bắt buộc) | [`TASK-FIN-11-VOUCHER-VALIDATION-LOCK.md`](phase-1-vouchers/TASK-FIN-11-VOUCHER-VALIDATION-LOCK.md) |
| **Phase 2: Hóa đơn & Lịch Đóng phí Nhiều đợt (INV/INS)** | | | | |
| `TASK-FIN-20` | Tự động Sinh Hóa đơn từ Admission (Gói học phí, Phụ phí học lại, Invoice Items) | FIN-F03 | **P0** (Bắt buộc) | [`TASK-FIN-20-INVOICE-AUTO-GENERATE.md`](phase-2-invoices/TASK-FIN-20-INVOICE-AUTO-GENERATE.md) |
| `TASK-FIN-21` | Cơ chế Chia đợt Đóng phí (Installments) & Sinh Mã Tham chiếu Duy nhất `payment_ref_code` | FIN-F03, INV-FIN-02 | **P0** (Bắt buộc) | [`TASK-FIN-21-INSTALLMENTS-ENGINE.md`](phase-2-invoices/TASK-FIN-21-INSTALLMENTS-ENGINE.md) |
| `TASK-FIN-22` | API Tra cứu Danh sách & Chi tiết Hóa đơn, Khoản mục và Lịch sử Đóng tiền | FIN-F04 | **P1** | [`TASK-FIN-22-INVOICE-QUERY.md`](phase-2-invoices/TASK-FIN-22-INVOICE-QUERY.md) |
| **Phase 3: Thu tiền & Thanh toán Đa kênh (PAY)** | | | | |
| `TASK-FIN-30` | Ghi nhận Phiếu thu Tiền mặt Tại quầy & Tự động Cập nhật Trạng thái `PARTIAL`/`PAID` | FIN-F05 | **P0** | [`TASK-FIN-30-CASH-PAYMENT.md`](phase-3-payments/TASK-FIN-30-CASH-PAYMENT.md) |
| `TASK-FIN-31` | API Sinh Mã VietQR Động Gắn với `payment_ref_code` Cho Học viên | FIN-F06 | **P1** | [`TASK-FIN-31-VIETQR-GENERATION.md`](phase-3-payments/TASK-FIN-31-VIETQR-GENERATION.md) |
| `TASK-FIN-32` | Webhook VietQR Đối soát Ngân hàng Thời gian Thực & Idempotency `bank_transaction_id` | FIN-F06, INV-FIN-05 | **P0** (Bắt buộc) | [`TASK-FIN-32-VIETQR-WEBHOOK-IDEMPOTENCY.md`](phase-3-payments/TASK-FIN-32-VIETQR-WEBHOOK-IDEMPOTENCY.md) |
| **Phase 4: Ví Bảo lưu Học viên (WAL)** | | | | |
| `TASK-FIN-40` | Quản lý Ví Bảo lưu Học viên & API Tra cứu Số dư Nội bộ | FIN-F07 | **P1** | [`TASK-FIN-40-WALLET-MANAGEMENT.md`](phase-4-wallets/TASK-FIN-40-WALLET-MANAGEMENT.md) |
| `TASK-FIN-41` | Giao dịch Cấn trừ Học phí Khóa học Tiếp theo (`TUITION_OFFSET`) & Khóa Rút Tiền Mặt | FIN-F07, INV-FIN-06 | **P0** | [`TASK-FIN-41-TUITION-OFFSET-LOCK.md`](phase-4-wallets/TASK-FIN-41-TUITION-OFFSET-LOCK.md) |
| **Phase 5: Quy trình Hoàn tiền 3 cấp (REF)** | | | | |
| `TASK-FIN-50` | Tạo Yêu cầu Hoàn tiền Cấp 1 (Tư vấn viên) & Kiểm tra Điều kiện Số buổi từ Learning | FIN-F08, INV-FIN-07 | **P0** (Bắt buộc) | [`TASK-FIN-50-REFUND-INITIATE.md`](phase-5-refunds/TASK-FIN-50-REFUND-INITIATE.md) |
| `TASK-FIN-51` | Xác minh Công nợ & Số buổi Cấp 2 (Kế toán - `PENDING_DIRECTOR`) | FIN-F08 | **P1** | [`TASK-FIN-51-REFUND-VERIFY.md`](phase-5-refunds/TASK-FIN-51-REFUND-VERIFY.md) |
| `TASK-FIN-52` | Phê duyệt Hoàn tiền Cấp 3 (Giám đốc - `APPROVED`/`REJECTED`) | FIN-F08 | **P0** | [`TASK-FIN-52-REFUND-APPROVE.md`](phase-5-refunds/TASK-FIN-52-REFUND-APPROVE.md) |
| `TASK-FIN-53` | Ghi nhận Giải ngân (Kế toán - `COMPLETED`) & Khóa Bất biến Hóa đơn `REFUNDED` | FIN-F08, INV-FIN-01 | **P0** (Bắt buộc) | [`TASK-FIN-53-REFUND-COMPLETE.md`](phase-5-refunds/TASK-FIN-53-REFUND-COMPLETE.md) |
| **Phase 6: Báo cáo Thông minh BI & Dashboard (BI)** | | | | |
| `TASK-FIN-60` | API Báo cáo Doanh thu Thực thu Theo Tháng Cash-basis (`vw_bi_cash_revenue_monthly`) | FIN-F09 | **P1** | [`TASK-FIN-60-BI-CASH-REVENUE.md`](phase-6-bi/TASK-FIN-60-BI-CASH-REVENUE.md) |
| `TASK-FIN-61` | API Báo cáo Hiệu quả Chiến dịch Voucher (`vw_bi_voucher_campaigns`) | FIN-F09 | **P1** | [`TASK-FIN-61-BI-VOUCHER-CAMPAIGNS.md`](phase-6-bi/TASK-FIN-61-BI-VOUCHER-CAMPAIGNS.md) |
| `TASK-FIN-62` | API Báo cáo Công nợ Phải thu, Nợ Quá hạn & Quỹ Bảo lưu Đang giữ | FIN-F09 | **P1** | [`TASK-FIN-62-BI-RETENTION-REFUND.md`](phase-6-bi/TASK-FIN-62-BI-RETENTION-REFUND.md) |
| **Phase 7: Giao diện Người dùng Frontend (UI)** | | | | |
| `TASK-FIN-70` | Giao diện Tra cứu & Quản lý Hóa đơn, Lịch trả góp (`invoices.html`) | UI-FIN-01 | **P1** | [`TASK-FIN-70-UI-INVOICE-DESK.md`](phase-7-frontend/TASK-FIN-70-UI-INVOICE-DESK.md) |
| `TASK-FIN-71` | Giao diện Thu tiền mặt & Bàn giao dịch Quét mã VietQR (`tuition.html`) | UI-FIN-02 | **P1** | [`TASK-FIN-71-UI-PAYMENT-VIETQR.md`](phase-7-frontend/TASK-FIN-71-UI-PAYMENT-VIETQR.md) |
| `TASK-FIN-72` | Giao diện Quản lý Ví bảo lưu & Bàn xử lý Yêu cầu Hoàn tiền 3 cấp | UI-FIN-03 | **P1** | [`TASK-FIN-72-UI-WALLET-REFUND.md`](phase-7-frontend/TASK-FIN-72-UI-WALLET-REFUND.md) |
| `TASK-FIN-73` | Dashboard Trực quan hóa Báo cáo BI Doanh thu & Voucher | UI-FIN-04 | **P2** | [`TASK-FIN-73-UI-BI-DASHBOARD.md`](phase-7-frontend/TASK-FIN-73-UI-BI-DASHBOARD.md) |

---

## 3. Quy chuẩn Nghiệm thu & Testing Quality Gate

Mỗi khi hoàn thành một task, lập trình viên bắt buộc thực hiện kiểm tra:
1. **Kiểm tra tính toàn vẹn Invariant Suite**:
   ```bash
   python3 -m unittest discover -s tests/invariants -p "test_*.py" -v
   ```
   Tất cả 15 bài kiểm thử phải đạt kết quả `OK`. Tuyệt đối không sửa đổi assertions của test.
2. **Quy chuẩn Code**:
   - Tuân thủ cấu trúc FastAPI phân tầng: `routers/` $\rightarrow$ `services/` $\rightarrow$ `models/`.
   - 100% Request Payload, Response JSON và Query params sử dụng **`snake_case`**.
   - Mọi thao tác tài chính, thanh toán, gạch nợ phải thực thi trong Database Transaction (`BEGIN ... COMMIT`) và có xử lý khóa bi quan chống Race Condition.
