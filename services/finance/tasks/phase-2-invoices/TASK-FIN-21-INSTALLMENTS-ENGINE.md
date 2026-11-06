# TASK-FIN-21: Cơ chế Chia đợt Đóng phí (Installments) & Sinh Mã Tham chiếu payment_ref_code

- **Mã nhiệm vụ**: `TASK-FIN-21`
- **Phân kỳ**: `Phase 2: Hóa đơn & Lịch Đóng phí Nhiều đợt (INV/INS)`
- **Độ ưu tiên**: `P0 (Bắt buộc)`
- **Yêu cầu chức năng liên quan**: `FIN-F03` (Lịch đóng phí nhiều đợt)
- **Quy tắc bất biến bảo vệ**: `INV-FIN-02` (Tổng amount_due các kỳ bằng đúng final_amount)

---

## 1. Mục tiêu Nghiệp vụ & Kỹ thuật
Xây dựng động cơ tự động chia nhỏ hóa đơn thành các kỳ thanh toán:
1. Tạo các bản ghi trong `finance.installments` với số kỳ (`installment_no` 1, 2, ...).
2. Sinh mã tham chiếu thanh toán duy nhất (`payment_ref_code UNIQUE`) cho từng kỳ (ví dụ: `INS-202611-0001`).
3. Phân bổ `amount_due` theo tỷ lệ hoặc số tiền cố định, cam kết $\sum \text{amount\_due} = \text{final\_amount}$.
4. Thiết lập hạn nộp `due_date` tương ứng từng kỳ.

---

## 2. Tiêu chí Nghiệm thu
- [ ] Không tạo kỳ trả góp nếu tổng tiền không khớp với `final_amount`.
- [ ] Mã `payment_ref_code` không bao giờ trùng lặp trong toàn hệ thống.
