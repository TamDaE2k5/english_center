# TASK-FIN-60: API Báo cáo Doanh thu Thực thu Theo Tháng Cash-basis (vw_bi_cash_revenue_monthly)

- **Mã nhiệm vụ**: `TASK-FIN-60`
- **Phân kỳ**: `Phase 6: Báo cáo Thông minh BI & Dashboard (BI)`
- **Độ ưu tiên**: `P1`
- **Yêu cầu chức năng liên quan**: `FIN-F09` (BI Tài chính - Thực thu)

---

## 1. Mục tiêu Nghiệp vụ & Kỹ thuật
Xây dựng API `GET /api/finance/bi/cash-revenue`:
1. Truy vấn từ View `finance.vw_bi_cash_revenue_monthly`.
2. Tính toán tổng dòng tiền thực thu (Gross Cash-in) từ `CASH` và `VIETQR`. Tuyệt đối không tính `WALLET_OFFSET` vào dòng tiền mới.
3. Khấu trừ tổng số tiền hoàn phí thực tế trong tháng để tính Doanh thu thuần thực thu (Net Cash Revenue).
4. Tính tỷ lệ hoàn phí trên tổng thu (`refund_rate_pct`).

---

## 2. Tiêu chí Nghiệm thu
- [ ] Dữ liệu trả về đúng theo từng tháng, có so sánh tăng trưởng.
- [ ] Giao dịch cấn trừ ví không bị tính trùng vào dòng tiền thực thu.
