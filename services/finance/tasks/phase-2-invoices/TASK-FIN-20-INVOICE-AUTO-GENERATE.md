# TASK-FIN-20: Tự động Sinh Hóa đơn từ Admission (Gói học phí, Phụ phí học lại, Invoice Items)

- **Mã nhiệm vụ**: `TASK-FIN-20`
- **Phân kỳ**: `Phase 2: Hóa đơn & Lịch Đóng phí Nhiều đợt (INV/INS)`
- **Độ ưu tiên**: `P0 (Bắt buộc)`
- **Yêu cầu chức năng liên quan**: `FIN-F03` (Tự động sinh hóa đơn)
- **Quy tắc bất biến bảo vệ**: `INV-FIN-02` (Đẳng thức toán học hóa đơn), `INV-FIN-04` (Khóa Voucher)

---

## 1. Mục tiêu Nghiệp vụ & Kỹ thuật
Xây dựng API `POST /api/finance/invoices/auto-generate` tiếp nhận yêu cầu chốt đăng ký từ Admission Service:
1. Sinh bản ghi trong `finance.invoices` ở trạng thái `PENDING`.
2. Hỗ trợ 3 loại hóa đơn: `COURSE_PACKAGE`, `STANDALONE_RETAKE`, `MIXED`.
3. Sinh các khoản mục chi tiết trong `finance.invoice_items` với `item_type IN ('TUITION_PACKAGE', 'RETAKE_FEE')`.
4. Bảo đảm ràng buộc toán học: `final_amount = amount - discount_applied >= 0`.

---

## 2. Tiêu chí Nghiệm thu
- [ ] Tạo hóa đơn kèm items trong 1 transaction duy nhất.
- [ ] Trả về `invoice_id`, `invoice_code`, `final_amount` cho Admission Service.
- [ ] Nếu có Voucher, kiểm tra và khấu trừ chính xác.
