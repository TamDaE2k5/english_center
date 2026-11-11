# TÀI LIỆU ĐẶC TẢ YÊU CẦU NGHIỆP VỤ (BRS) - HỆ THỐNG QUẢN LÝ TRUNG TÂM ANH NGỮ

## 1. GIỚI THIỆU (INTRODUCTION)
### 1.1. Mục đích tài liệu
Tài liệu này xác định các yêu cầu nghiệp vụ tổng thể (Business Requirements) cho toàn bộ Hệ thống Quản lý Trung tâm Anh ngữ. Tài liệu là cơ sở để thống nhất giữa Ban Giám đốc (Khách hàng) và Đội ngũ phát triển (4 nhóm Microservices) về các mục tiêu kinh doanh, quy trình vận hành và các ràng buộc nghiệp vụ cốt lõi trước khi đi vào thiết kế kỹ thuật.

### 1.2. Phạm vi hệ thống
Hệ thống bao gồm 4 phân hệ (Phân hệ) tương tác chặt chẽ với nhau để số hóa toàn diện hoạt động của trung tâm:
1. **Phân hệ Tuyển sinh (Admission):** Quản lý khóa học, lớp học, khảo thí đầu vào và đơn đăng ký.
2. **Phân hệ Học viên (Student):** Quản lý hồ sơ định danh, điểm danh và yêu cầu học vụ.
3. **Phân hệ Đào tạo (Learning):** Quản lý học liệu, bài giảng và kết quả học tập (điểm số).
4. **Phân hệ Tài chính (Finance):** Quản lý biểu phí, hóa đơn, thanh toán và hoàn phí.

---

## 2. BỐI CẢNH & ĐẶT VẤN ĐỀ (PROBLEM STATEMENT)
Hiện tại, các trung tâm ngoại ngữ đang đối mặt với nhiều "nỗi đau" trong vận hành do thiếu một nền tảng quản lý tập trung:
- **Dữ liệu phân mảnh:** Thông tin tuyển sinh (Excel), hồ sơ học viên (Sổ sách), điểm số (Giấy tờ) và hóa đơn (Phần mềm riêng lẻ) không đồng bộ, dẫn đến sai sót và thất lạc dữ liệu.
- **Quy trình thủ công gây quá tải:** Việc xếp lớp, kiểm tra sĩ số, điểm danh và thu học phí đều làm bằng tay khiến nhân viên giáo vụ quá tải, dễ nhầm lẫn và chậm trễ (đặc biệt là khâu báo vắng và đốc thúc học phí).
- **Thiếu minh bạch tài chính:** Dòng tiền thu chi, công nợ và các khoản hoàn phí/bảo lưu không được theo dõi chặt chẽ, tiềm ẩn rủi ro thất thoát.
- **Trải nghiệm học viên kém:** Học viên gặp khó khăn khi muốn tra cứu lịch học, kết quả thi, hay thực hiện các thủ tục bảo lưu/rút hồ sơ.

---

## 3. CÁC BÊN LIÊN QUAN (STAKEHOLDERS) & MỤC TIÊU
| Bên liên quan (Role) | Nhu cầu / Mục tiêu chính |
| :--- | :--- |
| **Ban Giám đốc (Admin)** | Cần Dashboard báo cáo doanh thu, tỷ lệ lấp đầy lớp, tỷ lệ duy trì học viên. Quản lý toàn bộ hệ thống và phân quyền. |
| **Giáo vụ (Staff)** | Cần công cụ để duyệt đơn tuyển sinh, xếp lớp, quản lý hồ sơ, duyệt các yêu cầu xin nghỉ/bảo lưu của học viên. |
| **Giáo viên (Teacher)** | Cần công cụ để quản lý lớp học, điểm danh nhanh chóng, upload tài liệu và nhập điểm thi. |
| **Học viên (Student)** | Cần một cổng thông tin (Portal) để tự tra cứu lịch học, xem điểm, tải tài liệu và gửi yêu cầu bảo lưu trực tuyến. |
| **Kế toán (Accountant)** | Cần hệ thống tự động sinh hóa đơn, ghi nhận thanh toán (tiền mặt/chuyển khoản) và quản lý quỹ hoàn tiền minh bạch. |

---

## 4. YÊU CẦU NGHIỆP VỤ CẤP HỆ THỐNG (BUSINESS REQUIREMENTS)

### BR-01: Quản lý Tuyển sinh & Lớp học (Phân hệ Admission)
- **Mô tả:** Hệ thống phải cung cấp danh mục khóa học, lịch khai giảng và cho phép học viên đăng ký trực tuyến. Hệ thống tự động quản lý sĩ số và phòng học.
- **Quy tắc Nghiệp vụ:** 
  - Chặn không cho đăng ký khi lớp đã đạt sĩ số tối đa (`max_students`).
  - Áp dụng cơ chế **giữ chỗ 48h**: Đơn đăng ký ở trạng thái `PENDING` sẽ tự động bị hủy (chuyển sang `EXPIRED`) nếu học viên không hoàn tất thanh toán trong 48 giờ.

### BR-02: Quản lý Hồ sơ & Chuyên cần (Phân hệ Student)
- **Mô tả:** Hệ thống tiếp nhận thông tin từ Tuyển sinh để tạo hồ sơ định danh duy nhất cho học viên. Đồng thời cung cấp công cụ điểm danh cho giáo viên.
- **Quy tắc Nghiệp vụ:**
  - Cấp mã định danh duy nhất (`student_code`) không trùng lặp cho mỗi học viên.
  - **Chặn điểm danh trùng lặp:** Một học viên tuyệt đối không thể bị điểm danh 2 lần trong cùng 1 ngày của 1 lớp.
  - Quy trình xin bảo lưu/rút hồ sơ phải được thực hiện trực tuyến và tự động đồng bộ trạng thái hồ sơ khi được duyệt.

### BR-03: Quản lý Đào tạo & Kết quả (Phân hệ Learning)
- **Mô tả:** Cung cấp kho học liệu điện tử (Slide, tài liệu, bài tập) cho từng lớp. Cho phép giáo viên nhập điểm và đánh giá học viên.
- **Quy tắc Nghiệp vụ:**
  - Chỉ giáo viên phụ trách lớp mới có quyền nhập điểm và upload tài liệu cho lớp đó.
  - Điểm số phải được kiểm soát chặt chẽ (điểm không được âm và không vượt quá thang điểm tối đa). Hệ thống tự động tính điểm trung bình để xét Đạt/Trượt.

### BR-04: Quản lý Tài chính & Học phí (Phân hệ Finance)
- **Mô tả:** Hệ thống tự động tính học phí dựa trên khóa học và các chương trình khuyến mãi (voucher). Quản lý hóa đơn và các đợt thanh toán.
- **Quy tắc Nghiệp vụ:**
  - Hóa đơn tự động chuyển sang trạng thái `PAID` (Đã thanh toán) khi tổng tiền nhận được bằng hoặc lớn hơn số tiền cần thu.
  - **Chống gian lận:** Mã giao dịch ngân hàng (`transaction_ref`) phải là duy nhất, chặn việc dùng 1 giao dịch để thanh toán cho nhiều hóa đơn.
  - Quy trình hoàn phí (khi học viên rút hồ sơ) phải trải qua quy trình phê duyệt nhiều cấp (Giáo vụ -> Kế toán -> Giám đốc) để đảm bảo an toàn dòng tiền.

---

## 5. YÊU CẦU PHI CHỨC NĂNG (NON-FUNCTIONAL REQUIREMENTS)
- **Kiến trúc Hệ thống:** Hệ thống được thiết kế theo kiến trúc **Microservices** nhằm đảm bảo tính mở rộng và độc lập. Các phân hệ giao tiếp với nhau qua REST API nội bộ, tuyệt đối không truy vấn chéo cơ sở dữ liệu của nhau.
- **Bảo mật & Phân quyền:** Áp dụng cơ chế xác thực tập trung (JWT). Phân quyền chặt chẽ theo vai trò (RBAC). Học viên chỉ được xem dữ liệu của chính mình; Giáo viên chỉ thao tác trên lớp mình dạy.
- **Hiệu năng:** Các thao tác tra cứu thông thường (xem danh sách, tìm kiếm) phải phản hồi trong vòng dưới 2 giây. Hệ thống phải chịu được tải lượng truy cập đồng thời của hàng trăm học viên trong giờ cao điểm đăng ký.
- **Tính khả dụng:** Giao diện Web phải tương thích trên nhiều thiết bị (Responsive), dễ sử dụng cho cả người không rành công nghệ (Giáo viên lớn tuổi, phụ huynh).

---
