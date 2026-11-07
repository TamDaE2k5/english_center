# TASK-FIN-02: Cấu hình HTTP Clients Tích hợp Liên Service (Admission & Learning Service)

- **Mã nhiệm vụ**: `TASK-FIN-02`
- **Phân kỳ**: `Phase 0: Nền móng & Hạ tầng (INF)`
- **Độ ưu tiên**: `P1`
- **Yêu cầu chức năng liên quan**: Tích hợp liên service (FIN-F03, FIN-F08)
- **Quy tắc bất biến bảo vệ**: `INV-FIN-03` (Cô lập Schema), `INV-FIN-07` (Chặn hoàn tiền theo số buổi)

---

## 1. Mục tiêu Nghiệp vụ & Kỹ thuật
Xây dựng module HTTP Client bằng `httpx` (Async):
1. **Admission Client**: Nhận và xử lý thông tin đăng ký khóa học, lớp học khi auto-generate hóa đơn.
2. **Learning Client**: Gọi endpoint `/api/learning/classes/{class_id}/students/{student_id}/attendance` để trích xuất `total_sessions` và `attended_sessions` nhằm phục vụ việc kiểm tra điều kiện hoàn tiền 3 cấp.
3. Thiết lập timeout, retry policy và fallback khi service bạn gặp sự cố.

---

## 2. Tiêu chí Nghiệm thu
- [ ] Khởi tạo client kết nối tới Learning Service với timeout chuẩn (5s).
- [ ] Bắt lỗi kết nối graceful và thông báo lỗi rõ ràng khi service đích không khả dụng.
