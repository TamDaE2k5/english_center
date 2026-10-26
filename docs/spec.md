# Đặc tả Nghiệp vụ Hệ thống (Product Requirements Document - PRD)

> **Mục tiêu**: Cung cấp tài liệu nghiệp vụ chi tiết cho 4 phân hệ để các AI Agent và lập trình viên hiện thực hóa chính xác tính năng.

---

## 1. Phân hệ 1: Tuyển sinh & Khóa học (Admission)

### Feature ADM-01: Quản lý Khóa học
- **Actor**: `ADMIN`, `STAFF`
- **Mô tả**: Tạo mới, chỉnh sửa thông tin, tạm dừng và xóa các khóa học trong trung tâm.
- **Rules**: Mã khóa học (`code`) phải là duy nhất. Học phí không được âm.
- **Priority**: P0

### Feature ADM-02: Quản lý Lớp học & Sĩ số
- **Actor**: `ADMIN`, `STAFF`
- **Mô tả**: Mở lớp học theo khóa học, xếp giáo viên, phòng học, lịch học trong tuần và giới hạn sĩ số tối đa (`max_students`).
- **Rules**: `max_students` tối thiểu là 1, mặc định 25.
- **Priority**: P0

### Feature ADM-03: Tiếp nhận & Duyệt Đăng ký (Enrollment)
- **Actor**: `STAFF`, `STUDENT`
- **Mô tả**: Học viên nộp đơn đăng ký học; nhân viên học vụ kiểm tra và phê duyệt.
- **Rules**:
  - Khi duyệt (`APPROVED`), hệ thống phải kiểm tra số lượng học viên đã duyệt của lớp.
  - Nếu sĩ số đã đạt `max_students`, bắt buộc từ chối hoặc báo lỗi, không được phê duyệt vượt chỉ tiêu (Invariant P0).
- **Priority**: P0

---

## 2. Phân hệ 2: Học viên & Vận hành lớp (Student)

### Feature STU-01: Hồ sơ Học viên
- **Actor**: `ADMIN`, `STAFF`, `STUDENT`
- **Mô tả**: Lưu trữ thông tin cá nhân học viên (họ tên, email, SĐT, ngày sinh, địa chỉ, trạng thái học vụ).
- **Rules**: Học viên chỉ có quyền xem và cập nhật thông tin của chính mình.
- **Priority**: P0

### Feature STU-02: Điểm danh buổi học (Attendance)
- **Actor**: `TEACHER`, `STAFF`
- **Mô tả**: Điểm danh học viên theo từng buổi học của lớp (`PRESENT`, `ABSENT`, `LATE`, `EXCUSED`).
- **Rules**:
  - Giáo viên chỉ được điểm danh cho lớp mình được phân công.
  - Không được trùng lặp bản ghi điểm danh cho cùng 1 học viên trong 1 ngày học.
- **Priority**: P1

---

## 3. Phân hệ 3: Đào tạo & Kết quả học tập (Learning)

### Feature LRN-01: Quản lý Học liệu & Slide bài giảng
- **Actor**: `TEACHER`, `STAFF`, `STUDENT`
- **Mô tả**: Giáo viên đăng tải slide, tài liệu đọc, file audio; học viên xem và tải về.
- **Rules**: Học viên chỉ xem được học liệu của lớp mình tham gia.
- **Priority**: P1

### Feature LRN-02: Sổ điểm & Đánh giá định kỳ
- **Actor**: `TEACHER`, `STAFF`, `STUDENT`
- **Mô tả**: Ghi nhận điểm số bài kiểm tra giữa kỳ, cuối kỳ, chuyên cần.
- **Rules**:
  - `0 <= score <= max_score`.
  - Giáo viên không được nhập/sửa điểm cho học viên thuộc lớp ngoài thẩm quyền phụ trách.
  - Học viên không được xem điểm của học viên khác.
- **Priority**: P0

---

## 4. Phân hệ 4: Tài chính & Học phí (Finance)

### Feature FIN-01: Quản lý Biểu phí Đào tạo
- **Actor**: `ADMIN`, `STAFF`
- **Mô tả**: Cấu hình mức học phí tiêu chuẩn theo từng khóa học.
- **Priority**: P1

### Feature FIN-02: Lập Hóa đơn & Theo dõi Công nợ
- **Actor**: `STAFF`, `STUDENT`
- **Mô tả**: Tự động hoặc thủ công lập hóa đơn cho hồ sơ đăng ký lớp học.
- **Rules**: Mã hóa đơn phải duy nhất. Hóa đơn mới khởi tạo ở trạng thái `UNPAID`.
- **Priority**: P0

### Feature FIN-03: Ghi nhận Thanh toán Học phí
- **Actor**: `STAFF`, `STUDENT`
- **Mô tả**: Ghi nhận giao dịch đóng tiền (Chuyển khoản, tiền mặt, thẻ).
- **Rules**:
  - Số tiền thanh toán phải > 0.
  - Không được ghi nhận trùng lặp giao dịch (Idempotency dựa trên mã giao dịch `transaction_ref`).
  - Khi tổng số tiền thanh toán đủ giá trị hóa đơn, chuyển hóa đơn sang `PAID`.
- **Priority**: P0

---

## 5. Explicit Non-Goals (Danh sách CỐ Ý KHÔNG LÀM)

Các tính năng sau đây **tuyệt đối không được triển khai** trong giai đoạn hiện tại (Wave 0 & Wave 1):
1. **Chatbot AI tư vấn tự động**: Không tích hợp LLM chatbot vào trang landing.
2. **AI tự động chấm thi hoặc tạo đề**: Không triển khai mô hình chấm tự động.
3. **Thuật toán tối ưu lịch học nâng cao**: Không làm solver xếp lịch thông minh.
4. **Tích hợp cổng thanh toán trực tiếp (VNPay, Momo API thật)**: Chỉ mock giao dịch và chuyển khoản nội bộ.
5. **Dịch vụ gửi tin nhắn SMS / Zalo ZNS có phí**: Không kết nối cổng viễn thông trả phí.
6. **Hệ thống kế toán tổng hợp chuyên sâu**: Không quản lý chi phí vận hành, lương giáo viên.
7. **Native Mobile App (Flutter / React Native)**: Chỉ tập trung web responsive.
