# TASK-FIN-62: API Báo cáo Công nợ Phải thu, Nợ Quá hạn & Quỹ Bảo lưu Đang giữ

- **Mã nhiệm vụ**: `TASK-FIN-62`
- **Phân kỳ**: `Phase 6: Báo cáo Thông minh BI & Dashboard (BI)`
- **Độ ưu tiên**: `P1`
- **Yêu cầu chức năng liên quan**: `FIN-F09` (BI Tài chính - Công nợ & Quỹ bảo lưu)

---

## 1. Mục tiêu Nghiệp vụ & Kỹ thuật
Xây dựng API `GET /api/finance/bi/refund-summary`:
1. Thống kê công nợ phải thu: Tổng `remaining_amount` từ các hóa đơn `PENDING` và `PARTIAL`.
2. Phân loại công nợ quá hạn (Overdue AR) theo kỳ trả góp có `due_date < CURRENT_DATE`.
3. Đo lường quy mô Quỹ bảo lưu đang giữ: $\sum \text{balance}$ trong `finance.student_wallets`.
4. Tính toán tỷ lệ duy trì và cấn trừ học phí thành công từ ví.

---

## 2. Tiêu chí Nghiệm thu
- [ ] Phân nhóm nợ quá hạn chính xác theo các mốc thời gian (<30 ngày, 30-60 ngày, >60 ngày).
