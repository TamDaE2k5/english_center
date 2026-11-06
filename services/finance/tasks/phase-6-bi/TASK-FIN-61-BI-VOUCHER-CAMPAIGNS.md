# TASK-FIN-61: API Báo cáo Hiệu quả Chiến dịch Voucher (vw_bi_voucher_campaigns)

- **Mã nhiệm vụ**: `TASK-FIN-61`
- **Phân kỳ**: `Phase 6: Báo cáo Thông minh BI & Dashboard (BI)`
- **Độ ưu tiên**: `P1`
- **Yêu cầu chức năng liên quan**: `FIN-F09` (BI Tài chính - Voucher ROI)

---

## 1. Mục tiêu Nghiệp vụ & Kỹ thuật
Xây dựng API `GET /api/finance/bi/voucher-campaigns`:
1. Truy vấn từ View `finance.vw_bi_voucher_campaigns`.
2. Đo lường tỷ lệ sử dụng Voucher: `(current_uses / max_uses) * 100%`.
3. Đo lường tỷ suất ROI: Tổng số tiền giảm giá đã cấp (`total_discount_given`) so với tổng doanh thu thực thu mang về từ các hóa đơn có áp dụng Voucher (`actual_cash_collected`).

---

## 2. Tiêu chí Nghiệm thu
- [ ] Tính toán chính xác hiệu quả từng mã voucher theo chiến dịch.
