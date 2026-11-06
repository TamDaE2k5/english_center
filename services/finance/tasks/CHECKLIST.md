# BẢNG THEO DÕI TIẾN ĐỘ THỰC HIỆN TASKS (CHECKLIST)
## PHÂN HỆ TÀI CHÍNH (FINANCE SERVICE)

> **Căn cứ kế hoạch**: [`README.md`](README.md)  
> **Người thực hiện**: Chi & Phúc (Phân hệ 4)  
> **Trạng thái quy ước**:
> - `[ ]` Chưa bắt đầu (TODO)
> - `[-]` Đang thực hiện (IN_PROGRESS)
> - `[x]` Đã hoàn thành & Đạt kiểm thử (DONE)

---

## 📌 Phase 0: Nền móng Kỹ thuật & Hạ tầng (INF)
- [ ] **`TASK-FIN-00`**: Thiết lập Nền tảng FastAPI, Pydantic v2 snake_case, JWT Middleware & Exception Handler
- [ ] **`TASK-FIN-01`**: Khởi tạo Schema PostgreSQL 8 Bảng, 2 BI Views, Ràng buộc DDL & Seed Data
- [ ] **`TASK-FIN-02`**: Cấu hình HTTP Clients Tích hợp Liên Service (Admission & Learning Service)

## 📌 Phase 1: Biểu phí & Chiến dịch Voucher (VOU)
- [ ] **`TASK-FIN-10`**: Quản lý Chiến dịch Voucher (Tạo mới, Cập nhật, Giới hạn lượt dùng & Hạn dùng)
- [ ] **`TASK-FIN-11`**: Động cơ Kiểm tra Voucher & Áp dụng Khóa dòng `SELECT FOR UPDATE` Chống Race Condition

## 📌 Phase 2: Hóa đơn & Lịch Đóng phí Nhiều đợt (INV/INS)
- [ ] **`TASK-FIN-20`**: Tự động Sinh Hóa đơn từ Admission (Gói học phí, Phụ phí học lại, Invoice Items)
- [ ] **`TASK-FIN-21`**: Cơ chế Chia đợt Đóng phí (Installments) & Sinh Mã Tham chiếu Duy nhất `payment_ref_code`
- [ ] **`TASK-FIN-22`**: API Tra cứu Danh sách & Chi tiết Hóa đơn, Khoản mục và Lịch sử Đóng tiền

## 📌 Phase 3: Thu tiền & Thanh toán Đa kênh (PAY)
- [ ] **`TASK-FIN-30`**: Ghi nhận Phiếu thu Tiền mặt Tại quầy & Tự động Cập nhật Trạng thái `PARTIAL`/`PAID`
- [ ] **`TASK-FIN-31`**: API Sinh Mã VietQR Động Gắn với `payment_ref_code` Cho Học viên
- [ ] **`TASK-FIN-32`**: Webhook VietQR Đối soát Ngân hàng Thời gian Thực & Idempotency `bank_transaction_id`

## 📌 Phase 4: Ví Bảo lưu Học viên (WAL)
- [ ] **`TASK-FIN-40`**: Quản lý Ví Bảo lưu Học viên & API Tra cứu Số dư Nội bộ
- [ ] **`TASK-FIN-41`**: Giao dịch Cấn trừ Học phí Khóa học Tiếp theo (`TUITION_OFFSET`) & Khóa Rút Tiền Mặt

## 📌 Phase 5: Quy trình Hoàn tiền 3 cấp (REF)
- [ ] **`TASK-FIN-50`**: Tạo Yêu cầu Hoàn tiền Cấp 1 (Tư vấn viên) & Kiểm tra Điều kiện Số buổi từ Learning
- [ ] **`TASK-FIN-51`**: Xác minh Công nợ & Số buổi Cấp 2 (Kế toán - `PENDING_DIRECTOR`)
- [ ] **`TASK-FIN-52`**: Phê duyệt Hoàn tiền Cấp 3 (Giám đốc - `APPROVED`/`REJECTED`)
- [ ] **`TASK-FIN-53`**: Ghi nhận Giải ngân (Kế toán - `COMPLETED`) & Khóa Bất biến Hóa đơn `REFUNDED`

## 📌 Phase 6: Báo cáo Thông minh BI & Dashboard (BI)
- [ ] **`TASK-FIN-60`**: API Báo cáo Doanh thu Thực thu Theo Tháng Cash-basis (`vw_bi_cash_revenue_monthly`)
- [ ] **`TASK-FIN-61`**: API Báo cáo Hiệu quả Chiến dịch Voucher (`vw_bi_voucher_campaigns`)
- [ ] **`TASK-FIN-62`**: API Báo cáo Công nợ Phải thu, Nợ Quá hạn & Quỹ Bảo lưu Đang giữ

## 📌 Phase 7: Giao diện Người dùng Frontend (UI)
- [ ] **`TASK-FIN-70`**: Giao diện Tra cứu & Quản lý Hóa đơn, Lịch trả góp (`invoices.html`)
- [ ] **`TASK-FIN-71`**: Giao diện Thu tiền mặt & Bàn giao dịch Quét mã VietQR
- [ ] **`TASK-FIN-72`**: Giao diện Quản lý Ví bảo lưu & Bàn xử lý Yêu cầu Hoàn tiền 3 cấp
- [ ] **`TASK-FIN-73`**: Dashboard Trực quan hóa Báo cáo BI Doanh thu & Voucher
