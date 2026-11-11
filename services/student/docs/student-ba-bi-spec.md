# ĐẶC TẢ NGHIỆP VỤ (BUSINESS SPECIFICATION) - PHÂN HỆ QUẢN LÝ HỌC VIÊN

## PHẦN 1: BỐI CẢNH & ĐẶT VẤN ĐỀ (OVERVIEW & PROBLEM STATEMENT)

Tại các trung tâm ngoại ngữ, dữ liệu học viên thường bị phân mảnh nghiêm trọng giữa các bộ phận (Tư vấn dùng Excel, Giáo vụ dùng Google Sheets, Giảng viên điểm danh qua sổ tay/Zalo).
**Hệ quả:** 70% dữ liệu phân mảnh gây trùng lặp hồ sơ, thắt lạc lịch sử học tập; 85% giáo vụ quá tải vì điểm danh thủ công và báo vắng trễ; trung tâm khó theo sát lộ trình để giữ chân học viên.

## PHẦN 2: MINH CHỨNG KHẢO SÁT THỰC TẾ

*(Số liệu được tổng hợp từ khảo sát nhu cầu thực tế)*

- **70%** dữ liệu học viên bị phân mảnh trên sổ sách, file Excel, nhóm Zalo.
- **85%** giáo vụ/trợ giảng bị quá tải vì điểm danh thủ công.
- **75%** mong muốn có một hệ thống quản lý hồ sơ tập trung 360 độ.

## PHẦN 3: BẢNG ÁNH XẠ KHÓ KHĂN & TÍNH NĂNG GIẢI QUYẾT

| Khó khăn thực tế                                     | Biểu hiện & Hậu quả                                                                               | Tính năng hệ thống đảm nhiệm                                                                                                                                              |
| :------------------------------------------------------- | :---------------------------------------------------------------------------------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Dữ liệu học viên phân mảnh, trùng lặp            | Trùng hồ sơ, thắt lạc lịch sử học tập do lưu trữ rời rạc.                                | **STU-01: Quản lý hồ sơ định danh duy nhất**- Cấp mã học viên duy nhất (`student_code` dạng `HV-2026-xxxx`), lưu trữ thông tin cá nhân xuyên suốt. |
| Điểm danh thủ công, thông báo trễ                 | Giáo viên mất nhiều thời gian gọi tên, dễ nhầm lẫn, phụ huynh không biết con nghỉ học. | **STU-02: Điểm danh điện tử**- Ghi nhận trạng thái chuyên cần 1-chạm (`PRESENT`, `ABSENT`, `LATE`, `EXCUSED`), lưu lịch sử tức thời.               |
| Xử lý bảo lưu, chuyển lớp, rút hồ sơ thủ công | Quy trình giấy tờ rườm rà, dễ sai sót quỹ buổi học.                                        | **STU-03: Số hóa quy trình yêu cầu học vụ**- Tiếp nhận và phê duyệt yêu cầu bảo lưu/chuyển lớp/rút hồ sơ trực tuyến.                                |

## PHẦN 4: MỤC TIÊU CỐT LÕI (PROJECT OBJECTIVES)

1. **Tập trung hóa dữ liệu:** Đồng bộ và quản lý hồ sơ cá nhân của học viên bằng mã định danh duy nhất.
2. **Tự động hóa điểm danh:** Giúp giáo viên điểm danh nhanh chóng, chặn lỗi trùng lặp dữ liệu.
3. **Số hóa quy trình học vụ:** Tiếp nhận và phê duyệt các yêu cầu bảo lưu/chuyển lớp/rút hồ sơ minh bạch.

## PHẦN 5: DANH SÁCH TÍNH NĂNG CHI TIẾT (FUNCTIONAL REQUIREMENTS)

### STU-01: Quản lý Hồ sơ Học viên (Profile Management)

- **Tác nhân:** Admin, Giáo vụ (Staff), Học viên.
- **Mô tả:**
  - Thêm mới, cập nhật hồ sơ (Họ tên, Email, SĐT, Ngày sinh, Địa chỉ).
  - 
  - Tra cứu học viên theo `student_code`, Họ tên hoặc SĐT.
  - Xóa mềm bằng cách cập nhật trạng thái (`STUDYING`, `RESERVED`, `DROPPED`, `GRADUATED`).
- **Ràng buộc:** Mã `student_code` là duy nhất (tự động sinh). Email không được trùng. Học viên chỉ được xem/sửa hồ sơ của mình.

### STU-02: Điểm danh buổi học (Electronic Attendance) - [Core V2]

- **Tác nhân:** Giảng viên (Teacher), Giáo vụ (Staff).
- **Mô tả:** Giảng viên chọn Lớp học -> Hệ thống hiển thị danh sách học viên -> Cập nhật trạng thái (`PRESENT`, `ABSENT`, `LATE`, `EXCUSED`) kèm ghi chú.
- **Ràng buộc:**
  - Giảng viên chỉ được điểm danh lớp mình dạy.
  - **Quy tắc chặn trùng:** Mỗi học viên chỉ có tối đa **1 bản ghi điểm danh / 1 ngày / 1 lớp**.
  - **Lưu ý kiến trúc:** Danh sách học viên chính thức của lớp được lấy từ bảng `admission.enrollments` (với điều kiện `status = 'APPROVED'`). Tuyệt đối **KHÔNG tạo thêm bảng `class_members` riêng** trong service Student để tránh dư thừa và lệch dữ liệu.

### STU-03: Quản lý Yêu cầu Học vụ (Status & Academic Requests)

- **Tác nhân:** Học viên, Giáo vụ (Staff).
- **Mô tả:** Học viên gửi yêu cầu trực tuyến (chọn loại: `RESERVE` - Bảo lưu, `TRANSFER` - Chuyển lớp, `DROP` - Rút hồ sơ; kèm lý do). Giáo vụ duyệt (`APPROVED`) hoặc Từ chối (`REJECTED`).
- **Ràng buộc:** Hệ thống tự động cập nhật trạng thái học viên thành `RESERVED` hoặc `DROPPED` ngay khi duyệt. Thời hạn bảo lưu tối đa 06 tháng.
