
# ĐẶC TẢ NGHIỆP VỤ & BÁO CÁO (BA & BI SPECIFICATION) - PHÂN HỆ ĐÀO TẠO & KẾT QUẢ HỌC TẬP (`learning`)

## PHẦN 1: BỐI CẢNH & ĐẶT VẤN ĐỀ (OVERVIEW & PROBLEM STATEMENT)

Tại các trung tâm ngoại ngữ, công tác quản lý học liệu và đánh giá kết quả học tập thường rời rạc: giảng viên gửi tài liệu qua nhóm Zalo hoặc phát bản in rời từng buổi, còn điểm số được nhập thủ công trên từng file Excel riêng lẻ của giáo viên[cite: 1].
**Hệ quả:** 65% học viên thất lạc tài liệu hoặc vào lớp muộn không tìm lại được bài giảng cũ; 40% học viên hiếm khi nhận được nhận xét chi tiết; việc nhập điểm thủ công dễ nhầm thang điểm (thang 10 vs thang 100) và trung tâm thiếu báo cáo tổng hợp tỷ lệ Đạt/Trượt định kỳ theo thời gian thực[cite: 1, 21].

## PHẦN 2: MINH CHỨNG KHẢO SÁT THỰC TẾ

*(Số liệu tổng hợp từ khảo sát Chương 1)*[cite: 1]

- **65%** học viên nhận tài liệu qua bản in rời hoặc link Zalo trôi nổi, dễ thất lạc[cite: 1].
- **40%** học viên hiếm khi nhận được nhận xét chi tiết đi kèm điểm số từng bài kiểm tra[cite: 1].
- **65%** học viên và phụ huynh mong muốn có báo cáo học lực định kỳ tự động nhưng trung tâm không đủ nhân lực tổng hợp thủ công[cite: 1].

## PHẦN 3: BẢNG ÁNH XẠ KHÓ KHĂN & TÍNH NĂNG GIẢI QUYẾT

| Khó khăn thực tế                                                  | Biểu hiện & Hậu quả                                                                                                             | Tính năng hệ thống đảm nhiệm                                                                                                                                                                                                                                                                                                                      |
| :-------------------------------------------------------------------- | :---------------------------------------------------------------------------------------------------------------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Học liệu trôi nổi, phân tán trên Zalo/giấy                    | Dễ thất lạc, học viên vào sau không tìm lại được tài liệu các buổi trước[cite: 1].                                | **LRN-01: Kho học liệu số tập trung theo lớp**- Lưu trữ và phân loại chuẩn (`DOCUMENT`, `VIDEO`, `EXERCISE`, `SLIDE`) gắn với từng `class_id`, kiểm tra đường dẫn hợp lệ (`INV-LRN-04`)[cite: 1, 21].                                                                                                             |
| Nhập điểm Excel thủ công, thiếu nhận xét & lệch thang điểm | Dễ gõ âm hoặc vượt thang tối đa, giáo viên chấm nhầm lớp khác, học viên xem lẫn điểm của nhau[cite: 1, 17, 20]. | **LRN-02: Số hóa Sổ điểm & Đánh giá trọng số**- Chấm điểm theo đầu điểm (`assessment_name`) có trọng số (`weight`) và lời phê (`feedback`); chặn `0 <= score <= max_score` (`INV-LRN-01`), kiểm tra đúng giáo viên dạy lớp (`INV-LRN-03`) và cô lập điểm học viên (`INV-LRN-02`)[cite: 1, 21]. |
| Không có báo cáo học lực & tỷ lệ Đạt/Trượt định kỳ     | Tốn thời gian tổng hợp thủ công, sai lệch khi lớp có nhiều thang điểm (10 và 100)[cite: 1, 21].                        | **LRN-03: Báo cáo học lực & BI tỷ lệ Đạt/Trượt tự động**- Tự động chuẩn hóa về thang 100% qua 2 views `v_student_grade_summary` và `v_class_pass_rate`, xét ngưỡng Đạt `average_percentage >= 50%` (`INV-LRN-05`)[cite: 1, 21].                                                                                   |

## PHẦN 4: MỤC TIÊU CỐT LÕI (PROJECT OBJECTIVES)

1. **Tập trung hóa kho học liệu số:** Quản lý tài liệu, slide, video, bài tập theo từng lớp học với kiểm soát quyền truy cập chặt chẽ[cite: 20, 21].
2. **Chuẩn hóa & Bảo mật sổ điểm:** Đảm bảo tính toàn vẹn điểm số (`INV-LRN-01`, `INV-LRN-06`), phân quyền giảng viên theo lớp phụ trách (`INV-LRN-03` / `INV-SEC-02`) và bảo mật điểm cá nhân học viên (`INV-LRN-02`)[cite: 17, 21].
3. **Báo cáo BI Đào tạo thời gian thực:** Cung cấp bảng tổng kết điểm chuẩn hóa thang 100% và thống kê tỷ lệ Pass/Fail tự động cho từng lớp học[cite: 21].

## PHẦN 5: DANH SÁCH TÍNH NĂNG CHI TIẾT (FUNCTIONAL & BI REQUIREMENTS)

### LRN-01: Quản lý Kho Học liệu Số (Digital Materials Management)

- **Tác nhân:** Quản trị viên (`ADMIN`), Giáo vụ (`STAFF`), Giảng viên (`TEACHER`), Học viên (`STUDENT`)[cite: 20, 21].
- **Mô tả:**
  - Giảng viên / Giáo vụ đăng tải học liệu mới cho lớp học: tiêu đề (`title`), mô tả (`description`), phân loại (`DOCUMENT`, `VIDEO`, `EXERCISE`, `SLIDE`), đường dẫn (`file_url`), tên file gốc (`original_filename`) và dung lượng (`file_size`)[cite: 21].
  - Học viên và Giảng viên tra cứu, lọc danh sách học liệu theo `class_id` và `material_type`[cite: 21].
  - Xóa học liệu khi cập nhật phiên bản mới (`ADMIN`, `STAFF`, `TEACHER`)[cite: 18, 21].
- **Ràng buộc:**
  - `file_url` bắt buộc khác rỗng sau khi cắt khoảng trắng (`INV-LRN-04`)[cite: 21].
  - `file_size >= 0` (nếu có)[cite: 21].
  - Trước khi thêm học liệu, hệ thống gọi sang Admission Service để xác minh lớp học tồn tại và nếu người tải lên là `TEACHER` thì phải đúng là giáo viên phụ trách lớp đó[cite: 18, 21].

### LRN-02: Quản lý Bảng điểm & Đánh giá Trọng số (Weighted Gradebook)

- **Tác nhân:** Giảng viên (`TEACHER`), Giáo vụ (`STAFF`), Quản trị viên (`ADMIN`), Học viên (`STUDENT`)[cite: 20, 21].
- **Mô tả:**
  - Giảng viên / Giáo vụ nhập hoặc cập nhật điểm cho học viên theo từng đầu điểm (`Quiz 1`, `Midterm`, `Final`...), kèm thang điểm tối đa (`max_score`, mặc định `100.0`), trọng số (`weight` trong `(0, 1]`, mặc định `1.0`) và nhận xét (`feedback`)[cite: 21].
  - Học viên xem bảng điểm cá nhân và nhận xét của giáo viên[cite: 20, 21].
- **Ràng buộc:**
  - **Giới hạn điểm (`INV-LRN-01`):** `0.0 <= score <= max_score` và `max_score > 0`[cite: 21].
  - **Đơn nhất đầu điểm (`INV-LRN-06`):** Mỗi bộ `(class_id, student_id, assessment_name)` chỉ có duy nhất 1 bản ghi điểm[cite: 21].
  - **Ranh giới giảng viên (`INV-LRN-03` / `INV-SEC-02`):** Nếu người chấm có role `TEACHER`, hệ thống bắt buộc gọi `GET /api/admission/internal/classes/{class_id}` để đối chiếu `teacher_id == current_user.user_id`. Nếu không khớp, trả lỗi `403 LRN_FORBIDDEN_CLASS`[cite: 18, 21].
  - **Cô lập điểm học viên (`INV-LRN-02`):** Học viên (`role = STUDENT`) chỉ xem được điểm của chính mình; hệ thống tự ánh xạ `user_id` trong JWT sang `student.students.id` (`R-ID-1`), bỏ qua mọi `student_id` truyền từ query param[cite: 14, 21].

### LRN-03: Báo cáo Kết quả Học tập & BI Tỷ lệ Đạt/Trượt (Academic BI & Reporting)

- **Tác nhân:** Quản trị viên (`ADMIN`), Giáo vụ (`STAFF`), Giảng viên (`TEACHER`), Học viên (`STUDENT` xem kết quả tổng kết của mình)[cite: 21].
- **Mô tả:**
  - Tự động tính điểm trung bình có trọng số (`weighted_avg_score`) và điểm phần trăm chuẩn hóa thang 100 (`average_percentage`) cho từng học viên trong lớp qua view `learning.v_student_grade_summary`[cite: 21].
  - Tự động phân loại kết quả `Pass` (khi `average_percentage >= 50.0`) hoặc `Fail` (`< 50.0`) theo `INV-LRN-05`[cite: 21].
  - Thống kê tổng số học viên đã có điểm (`total_students`), số lượng Đạt (`pass_count`), số lượng Trượt (`fail_count`) và tỷ lệ đỗ (`pass_rate_percentage`) của từng lớp qua view `learning.v_class_pass_rate`[cite: 21].
