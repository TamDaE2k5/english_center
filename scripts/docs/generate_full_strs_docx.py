import os
import docx
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml import parse_xml
from docx.oxml.ns import nsdecls

from docx_helpers import (
    set_cell_background, set_cell_margins, set_table_borders,
    format_run, add_p, add_heading_1, add_heading_2, add_heading_3,
    add_bullet, add_callout, create_table, add_diagram_image
)

def build_document(output_path):
    doc = docx.Document()

    # Page setup: A4, 1 inch (72 pt * 1 = 1440 dxa) margins
    for section in doc.sections:
        section.top_margin = Inches(1.0)
        section.bottom_margin = Inches(1.0)
        section.left_margin = Inches(1.0)
        section.right_margin = Inches(1.0)
        section.page_width = Inches(8.5)
        section.page_height = Inches(11.0)

        # Header & Footer setup
        header = section.header
        hp = header.paragraphs[0]
        hp.alignment = WD_ALIGN_PARAGRAPH.RIGHT
        hrun = hp.add_run("Hệ thống Quản lý Trung tâm Anh ngữ | Đặc tả Yêu cầu Các bên Liên quan (StRS)")
        format_run(hrun, "Times New Roman", 8.5, italic=True, color_rgb=(148, 163, 184))

        footer = section.footer
        fp = footer.paragraphs[0]
        fp.alignment = WD_ALIGN_PARAGRAPH.CENTER
        frun = fp.add_run("Mã tài liệu: EC-StRS-2026-V1 — ISO/IEC/IEEE 29148:2018")
        format_run(frun, "Times New Roman", 8.5, italic=True, color_rgb=(148, 163, 184))

    # --- TITLE & COVER HEADER ---
    add_p(doc, "ĐẶC TẢ YÊU CẦU CÁC BÊN LIÊN QUAN TOÀN HỆ THỐNG",
          size_pt=20, bold=True, color_rgb=(26, 54, 93),
          align=WD_ALIGN_PARAGRAPH.CENTER, space_before=12, space_after=4)

    add_p(doc, "(STAKEHOLDER REQUIREMENTS SPECIFICATION - StRS)",
          size_pt=14, bold=True, color_rgb=(37, 99, 235),
          align=WD_ALIGN_PARAGRAPH.CENTER, space_before=0, space_after=6)

    add_p(doc, "HỆ THỐNG QUẢN LÝ TRUNG TÂM ANH NGỮ (ENGLISH CENTER MANAGEMENT SYSTEM)",
          size_pt=12, bold=True, color_rgb=(51, 65, 85),
          align=WD_ALIGN_PARAGRAPH.CENTER, space_before=0, space_after=14)

    # Document Metadata Callout Box
    meta_lines = [
        "• Mã tài liệu: EC-StRS-2026-V1",
        "• Tiêu chuẩn áp dụng: ISO/IEC/IEEE 29148:2018 (Systems and software engineering — Life cycle processes — Requirements engineering)",
        "• Kiến trúc hệ thống: Polyglot Microservices (Nginx Gateway :8080, Admission :8081, Student :8082, Learning :8083, Finance :8084, PostgreSQL 16 — 4 Schemas độc lập)",
        "• Phiên bản: 1.0 — Bản hợp nhất toàn dự án",
        "• Ngày ban hành: 2026-11-11"
    ]
    add_callout(doc, meta_lines, title="THÔNG TIN HỒ SƠ TÀI LIỆU (DOCUMENT METADATA)")
    add_p(doc, "", space_after=6)

    # --- MỤC LỤC ---
    add_heading_1(doc, "MỤC LỤC")
    toc_items = [
        "1. Giới thiệu & Mục đích Tài liệu (Introduction)",
        "2. Bối cảnh Nghiệp vụ & Đặt vấn đề (Business Context & Problem Statement)",
        "3. Hồ sơ Các Bên Liên Quan (Stakeholder Profiles)",
        "4. Nhu cầu & Yêu cầu Của Các Bên Liên Quan (Stakeholder Needs & Requirements)",
        "5. Kịch bản Vận hành Xuyên Phân hệ (Operational Scenarios / End-to-End Value Streams)",
        "6. Yêu cầu Chất lượng, Bất biến & Ràng buộc Hệ thống (Quality Attributes, Invariants & Constraints)",
        "7. Ma trận Truy vết Yêu cầu Toàn Dự án (End-to-End Traceability Matrix)",
        "8. Tiêu chí Nghiệm thu & Phạm vi Loại trừ (Acceptance Criteria & Explicit Non-Goals)"
    ]
    for item in toc_items:
        add_p(doc, item, size_pt=11, bold=True, color_rgb=(30, 41, 59), space_before=2, space_after=3)

    add_p(doc, "", space_after=10)

    # --- 1. GIỚI THIỆU & MỤC ĐÍCH TÀI LIỆU ---
    add_heading_1(doc, "1. Giới thiệu & Mục đích Tài liệu (Introduction)")
    
    add_heading_2(doc, "1.1. Mục đích (Purpose)")
    add_p(doc, "Tài liệu Đặc tả Yêu cầu Các bên Liên quan (StRS - Stakeholder Requirements Specification) xác định rõ tại sao (Why) hệ thống Quản lý Trung tâm Anh ngữ được xây dựng, ai (Who) là những bên có quyền lợi/trách nhiệm liên quan, và những giá trị nghiệp vụ nào (What) hệ thống bắt buộc phải mang lại cho từng nhóm đối tượng.")
    add_p(doc, "Tài liệu này là tầng cao nhất trong tháp đặc tả của dự án, làm cơ sở tham chiếu trực tiếp cho:")
    add_bullet(doc, "• Đặc tả Yêu cầu Nghiệp vụ & BI của 4 phân hệ: ", "admission-ba-bi-spec.md, student-ba-bi-spec.md, tong_hop_learning_tinh_nang_va_schema.md, finance-ba-bi-spec.md.")
    add_bullet(doc, "• Thiết kế CSDL Master & Hợp đồng Giao tiếp Liên Service: ", "docs/MASTER_DB_AND_API_RULES.md, scripts/init-db.sql.")
    add_bullet(doc, "• Bộ Kiểm thử Bất biến Tự động: ", "tests/invariants/test_*.py.")

    add_heading_2(doc, "1.2. Phạm vi Hệ thống (System Scope)")
    add_p(doc, "Hệ thống số hóa trọn vẹn vòng đời học viên tại Trung tâm Anh ngữ qua 4 phân hệ Microservices độc lập:")
    add_bullet(doc, "1. Phân hệ 1 — Tuyển sinh, Khảo thí & Định danh (admission-service, Port 8081, Schema admission): ",
               "Quản lý định danh tập trung (IAM), cơ sở vật chất (phòng học, ca học), danh mục khóa học/lớp học, chống xung đột lịch, khảo thí đầu vào trực tuyến (Placement Test) và giữ chỗ tuyển sinh 48 giờ.")
    add_bullet(doc, "2. Phân hệ 2 — Quản lý Học viên & Điểm danh (student-service, Port 8082, Schema student): ",
               "Quản lý hồ sơ học viên 360 độ (HV-YYYY-NNNN), điểm danh điện tử theo buổi học và xử lý yêu cầu học vụ (Bảo lưu, Chuyển lớp, Bỏ học).")
    add_bullet(doc, "3. Phân hệ 3 — Quản lý Học tập & Bảng điểm (learning-service, Port 8083, Schema learning): ",
               "Quản lý kho học liệu số theo lớp học, nhập điểm đánh giá có trọng số và tự động tổng hợp kết quả xếp loại Đạt/Trượt (Pass/Fail).")
    add_bullet(doc, "4. Phân hệ 4 — Quản lý Tài chính & Học phí (finance-service, Port 8084, Schema finance): ",
               "Quản lý biểu phí, chiến dịch Voucher, tự động phát hành hóa đơn & chia kỳ trả góp, thu tiền mặt tại quầy, đối soát tự động qua Webhook VietQR, quản lý ví bảo lưu nội bộ và phê duyệt hoàn tiền 3 cấp.")

    add_heading_2(doc, "1.3. Thuật ngữ & Từ viết tắt (Definitions & Acronyms)")
    def_headers = ["Thuật ngữ / Viết tắt", "Giải nghĩa"]
    def_data = [
        ["StRS", "Stakeholder Requirements Specification — Đặc tả Yêu cầu Các bên Liên quan."],
        ["IAM / RBAC", "Identity and Access Management / Role-Based Access Control (Phân quyền theo 4 vai trò: ADMIN, STAFF, TEACHER, STUDENT + trạng thái GUEST)."],
        ["48h Slot Hold", "Cơ chế giữ chỗ tạm thời 48 giờ cho đơn đăng ký ở trạng thái PENDING trước khi hoàn tất đóng học phí."],
        ["Logical FK", "Khóa ngoại logic (INTEGER) tham chiếu liên schema qua REST API / Webhook, tuyệt đối không tạo Physical FK hay JOIN SQL chéo schema."],
        ["R-ID-1 (Identity Bridge)", "Quy ước định danh học viên: admission.enrollments và finance.* lưu student_id = admission.users.id; student.* và learning.* lưu student_id = student.students.id."],
        ["VietQR Webhook", "Cơ chế nhận thông báo biến động số dư thời gian thực từ cổng ngân hàng để gạch nợ hóa đơn tự động (Idempotent theo bank_transaction_id)."],
        ["Cash-Basis Revenue", "Doanh thu thực thu tính trên dòng tiền thực tế đã vào quỹ (CASH + VIETQR) trừ đi tổng tiền đã hoàn trả (COMPLETED)."]
    ]
    create_table(doc, def_headers, def_data, [2.2, 4.3])

    # --- 2. BỐI CẢNH NGHIỆP VỤ & ĐẶT VẤN ĐỀ ---
    add_heading_1(doc, "2. Bối cảnh Nghiệp vụ & Đặt vấn đề (Business Context & Problem Statement)")
    
    add_heading_2(doc, "2.1. Hiện trạng & Nỗi đau Nghiệp vụ (Current Pain Points)")
    add_p(doc, "Trước khi triển khai hệ thống tập trung, quy trình vận hành tại các trung tâm Anh ngữ gặp 5 điểm nghẽn nghiêm trọng:")
    add_bullet(doc, "1. Phễu tuyển sinh rời rạc & vượt sĩ số lớp (Over-enrollment): ", "Tư vấn viên giữ chỗ thủ công trên Google Sheets dẫn đến tình trạng một lớp nhận vượt sức chứa phòng học, hoặc học viên giữ chỗ ảo làm mất cơ hội của người đóng tiền thật.")
    add_bullet(doc, "2. Xung đột lịch phòng & giảng viên: ", "Việc xếp lớp thủ công thường xuyên gây trùng phòng học hoặc trùng ca dạy của giáo viên.")
    add_bullet(doc, "3. 70% dữ liệu học viên bị phân mảnh: ", "Thông tin học viên nằm rải rác giữa bộ phận Tư vấn (Excel), Học vụ (Sheets) và Giáo viên (sổ điểm danh/Zalo), gây trùng lặp hồ sơ và thất lạc lịch sử học tập.")
    add_bullet(doc, "4. 85% giáo vụ và giảng viên quá tải vì điểm danh & tính điểm thủ công: ", "Sai sót khi tính điểm trung bình có trọng số và chậm trễ trong việc cảnh báo học viên nghỉ học hoặc xét điều kiện lên lớp.")
    add_bullet(doc, "5. Rủi ro thất thoát tài chính & đối soát chậm: ", "Kế toán phải kiểm tra sao kê ngân hàng bằng mắt, dễ áp dụng lố số lượng Voucher khuyến mãi khi nhiều người đăng ký cùng lúc, và thiếu quy trình kiểm soát chặt chẽ khi xét duyệt hoàn tiền hoặc bảo lưu học phí.")

    add_heading_2(doc, "2.2. Mục tiêu Chiến lược Của Dự án (Strategic Business Objectives)")
    bo_headers = ["Mã mục tiêu", "Mục tiêu chiến lược", "Thước đo thành công (Success Metrics / KPIs)"]
    bo_data = [
        ["BO-01", "Tự động hóa Phễu Tuyển sinh & Khảo thí Đầu vào", "100% thí sinh làm bài Placement Test trực tuyến được chấm điểm tự động ngay lập tức và nhận đề xuất khóa học khớp trình độ."],
        ["BO-02", "Bảo toàn Sĩ số & Lịch học Tuyệt đối (Zero Conflict / Zero Over-capacity)", "0% lớp học bị duyệt vượt max_students hoặc vượt sức chứa phòng; 0% ca học bị trùng phòng hoặc trùng giảng viên."],
        ["BO-03", "Hồ sơ Học viên 360 Độ & Số hóa Học vụ", "100% đơn đăng ký APPROVED được đồng bộ tự động sang hồ sơ học viên (HV-YYYY-NNNN); điểm danh 1-chạm không trùng lặp."],
        ["BO-04", "Minh bạch Học tập & Đánh giá Trọng số Tự động", "100% đầu điểm được kiểm soát trong biên 0 <= score <= max_score; tự động chuẩn hóa thang 100% và xếp loại Pass/Fail."],
        ["BO-05", "An toàn Dòng tiền, Gạch nợ Thời gian thực & Kiểm soát Hoàn phí 3 Cấp", "100% giao dịch VietQR đối soát tự động (Idempotent); 0% Voucher vượt trần lượt dùng; 100% yêu cầu hoàn tiền phải qua kiểm tra số buổi học (<= 3 buổi) và duyệt đủ 3 cấp."]
    ]
    create_table(doc, bo_headers, bo_data, [1.0, 2.3, 3.2])

    # --- 3. HỒ SƠ CÁC BÊN LIÊN QUAN ---
    add_heading_1(doc, "3. Hồ sơ Các Bên Liên Quan (Stakeholder Profiles)")
    add_p(doc, "Dưới đây là sơ đồ tổng quan hệ sinh thái các bên liên quan trực tiếp và gián tiếp tương tác với Hệ thống Quản lý Trung tâm Anh ngữ:")
    
    # Insert Diagram 1
    add_diagram_image(doc, "docs/diagrams/diagram_stakeholder_ecosystem.png",
                      "Hình 1: Sơ đồ Hệ sinh thái Các bên liên quan (Stakeholder Ecosystem)", width_inches=6.2)

    add_heading_2(doc, "Bảng Phân tích Chi tiết Các Bên Liên Quan")
    sh_headers = ["Mã", "Bên liên quan (Stakeholder)", "Vai trò Hệ thống", "Mối quan tâm chính (Key Concerns)", "Phân hệ Tương tác"]
    sh_data = [
        ["SH-01", "Khách vãng lai / Học viên tiềm năng", "GUEST", "Xem thông tin khóa học/lịch khai giảng minh bạch; đăng ký tài khoản nhanh; làm bài test xếp lớp trực tuyến và biết ngay kết quả.", "admission"],
        ["SH-02", "Học viên chính thức", "STUDENT", "Giữ chỗ lớp học 48h; áp mã giảm giá Voucher; quét mã VietQR đóng học phí; xem tài liệu lớp học; xem điểm thi cá nhân; xem số dư ví bảo lưu và gửi đơn xin bảo lưu/chuyển lớp.", "admission, student, learning, finance"],
        ["SH-03", "Giảng viên", "TEACHER", "Xem lịch giảng dạy không bị chồng chéo; tải học liệu/slide lên cho lớp; điểm danh học viên từng buổi; nhập điểm đánh giá và nhận xét cho lớp mình phụ trách.", "admission, student, learning"],
        ["SH-04", "Chuyên viên Tư vấn & Học vụ", "STAFF (Academic)", "Mở lớp học không trùng phòng/giáo viên; hỗ trợ đăng ký tại quầy; gia hạn giữ chỗ; duyệt đơn bảo lưu/chuyển lớp; khởi tạo yêu cầu hoàn tiền cấp 1; xuất báo cáo CSV.", "admission, student, learning, finance"],
        ["SH-05", "Chuyên viên Kế toán", "STAFF (Accountant)", "Thiết lập biểu phí & chiến dịch Voucher; lập hóa đơn & chia kỳ đóng phí; thu tiền mặt tại quầy; quản lý ví bảo lưu & cấn trừ học phí; xác minh hoàn tiền cấp 2 và giải ngân UNC.", "finance, admission"],
        ["SH-06", "Ban Giám đốc & Quản trị viên", "ADMIN", "Quản trị nhân sự & phân quyền; giám sát an ninh đăng nhập (auth_audit_logs); phê duyệt hoàn tiền cấp 3; theo dõi Dashboard BI toàn diện (Phễu tuyển sinh, Lấp đầy lớp, Doanh thu thực thu Cash-basis, Hiệu quả Voucher).", "Toàn bộ 4 phân hệ"],
        ["SH-07", "Cổng Ngân hàng / VietQR", "WEBHOOK", "Truyền thông báo giao dịch chuyển khoản sang hệ thống an toàn, chính xác, không bị nhân đôi phiếu thu khi gửi lặp (Retry).", "finance -> admission"],
        ["SH-08", "Đội ngũ Kỹ thuật & QA", "INTERNAL / DevOps", "Cô lập dữ liệu giữa 4 microservice (INV-SYS-02), chuẩn hóa giao thức JSON snake_case (INV-SYS-01), dễ dàng triển khai qua Docker Compose và kiểm thử tự động.", "Toàn bộ hệ thống"]
    ]
    create_table(doc, sh_headers, sh_data, [0.7, 1.4, 0.9, 2.5, 1.0])

    # --- 4. NHU CẦU & YÊU CẦU CỦA CÁC BÊN LIÊN QUAN ---
    add_heading_1(doc, "4. Nhu cầu & Yêu cầu Của Các Bên Liên Quan (Stakeholder Needs & Requirements)")

    add_heading_2(doc, "4.1. Nhóm Yêu cầu từ Khách vãng lai & Học viên (SH-01, SH-02)")
    str_stu_headers = ["Mã StR", "Mô tả Yêu cầu của Bên liên quan (Stakeholder Requirement)", "Phân hệ Đáp ứng", "Mức ưu tiên"]
    str_stu_data = [
        ["STR-STU-01", "Tra cứu & Khảo thí Đầu vào Trực tuyến: Khách vãng lai và học viên có thể tra cứu khóa học, lớp học đang mở, đăng ký xác thực tài khoản và thực hiện bài thi khảo thí 3 kỹ năng (Listening, Reading, Grammar) có đồng hồ đếm ngược, tự động chấm điểm và gợi ý khóa học phù hợp kèm danh sách lớp còn chỗ.", "admission", "P0 (Bắt buộc)"],
        ["STR-STU-02", "Đăng ký Lớp học & Giữ chỗ 48 Giờ: Học viên có thể chọn lớp học để đăng ký trực tuyến, được hệ thống giữ chỗ độc quyền trong 48 giờ (PENDING) và tự động phát hành hóa đơn học phí tương ứng.", "admission, finance", "P0 (Bắt buộc)"],
        ["STR-STU-03", "Áp dụng Voucher & Thanh toán VietQR Tức thời: Học viên có thể kiểm tra mã giảm giá Voucher, lấy mã QR động VietQR theo từng đợt đóng phí (installments) và được duyệt vào lớp tự động ngay khi chuyển khoản thành công.", "finance, admission", "P0 (Bắt buộc)"],
        ["STR-STU-04", "Truy cập Kho Học liệu & Tra cứu Bảng điểm Cá nhân: Học viên chỉ xem và tải được tài liệu/slide của lớp mình đang học, đồng thời theo dõi điểm các bài kiểm tra và kết quả xếp loại tổng kết (Pass/Fail) của riêng mình (bảo mật tuyệt đối giữa các học viên).", "learning", "P0 (Bắt buộc)"],
        ["STR-STU-05", "Quản lý Hồ sơ, Yêu cầu Học vụ & Ví Bảo lưu: Học viên có thể xem hồ sơ cá nhân (HV-YYYY-NNNN), gửi đơn xin Bảo lưu (RESERVE), Chuyển lớp (TRANSFER) hoặc Rút hồ sơ (DROP), và kiểm tra số dư Ví bảo lưu để cấn trừ cho khóa học tiếp theo.", "student, finance", "P1 (Quan trọng)"]
    ]
    create_table(doc, str_stu_headers, str_stu_data, [1.1, 3.7, 1.0, 0.7])

    add_heading_2(doc, "4.2. Nhóm Yêu cầu từ Giảng viên (SH-03)")
    str_tch_headers = ["Mã StR", "Mô tả Yêu cầu của Bên liên quan (Stakeholder Requirement)", "Phân hệ Đáp ứng", "Mức ưu tiên"]
    str_tch_data = [
        ["STR-TCH-01", "Điểm danh Điện tử Theo Buổi học: Giảng viên có thể điểm danh học viên trong lớp theo từng ngày học (PRESENT, ABSENT, LATE, EXCUSED) kèm ghi chú, hệ thống tự động chặn ghi trùng điểm danh trong cùng một buổi.", "student", "P0 (Bắt buộc)"],
        ["STR-TCH-02", "Đăng tải & Phân phối Học liệu: Giảng viên có thể đăng tải tài liệu (DOCUMENT, VIDEO, EXERCISE, SLIDE) gắn với lớp học cụ thể cho học viên.", "learning", "P1 (Quan trọng)"],
        ["STR-TCH-03", "Nhập Điểm Đánh giá Có Trọng số Trong Phạm vi Phân công: Giảng viên chỉ được nhập/cập nhật điểm (score, max_score, weight, feedback) cho các lớp mình trực tiếp giảng dạy; hệ thống tự động chặn điểm âm hoặc điểm vượt quá max_score.", "learning, admission", "P0 (Bắt buộc)"]
    ]
    create_table(doc, str_tch_headers, str_tch_data, [1.1, 3.7, 1.0, 0.7])

    add_heading_2(doc, "4.3. Nhóm Yêu cầu từ Chuyên viên Tư vấn & Học vụ (SH-04)")
    str_aca_headers = ["Mã StR", "Mô tả Yêu cầu của Bên liên quan (Stakeholder Requirement)", "Phân hệ Đáp ứng", "Mức ưu tiên"]
    str_aca_data = [
        ["STR-ACA-01", "Thiết lập Khóa học, Mở Lớp & Chống Xung đột Lịch: Học vụ có thể quản lý danh mục khóa học (kèm khóa tiên quyết), mở lớp học mới gắn với phòng học và giảng viên; hệ thống tự động kiểm tra trần sức chứa phòng và chặn mọi xung đột lịch phòng/lịch giảng viên.", "admission", "P0 (Bắt buộc)"],
        ["STR-ACA-02", "Điều phối Bàn Tuyển sinh, Gia hạn Giữ chỗ & Thu hồi Slot Quá hạn: Học vụ có thể tiếp nhận học viên đăng ký tại quầy, gia hạn thời gian giữ chỗ kèm lý do nghiệp vụ, hoặc để hệ thống tự động thu hồi slot khi quá hạn 48h (EXPIRED).", "admission", "P0 (Bắt buộc)"],
        ["STR-ACA-03", "Hủy Lớp Thiếu Sĩ số & Bảo vệ Quyền lợi Học viên (Cascade Cancellation): Khi lớp chưa khai giảng không đạt sĩ số tối thiểu (min_students), Học vụ có thể hủy lớp: các đơn PENDING tự động hủy giải phóng chỗ, các đơn APPROVED chuyển sang CANCELLED_REFUND_PENDING và đồng bộ yêu cầu hoàn phí 100% sang Finance.", "admission, finance", "P0 (Bắt buộc)"],
        ["STR-ACA-04", "Xét duyệt Yêu cầu Học vụ & Khởi tạo Hoàn tiền Cấp 1: Học vụ xem xét duyệt/từ chối đơn xin Bảo lưu, Chuyển lớp, Bỏ học của học viên; khởi tạo yêu cầu hoàn tiền Cấp 1 khi học viên rút hồ sơ sớm (học không quá 3 buổi).", "student, finance", "P1 (Quan trọng)"],
        ["STR-ACA-05", "Xuất Dữ liệu Vận hành (CSV/Excel): Học vụ có thể xuất danh sách học viên theo lớp và bảng điểm khảo thí đầu vào ra file CSV an toàn (chống CSV Injection).", "admission, learning", "P1 (Quan trọng)"]
    ]
    create_table(doc, str_aca_headers, str_aca_data, [1.1, 3.7, 1.0, 0.7])

    add_heading_2(doc, "4.4. Nhóm Yêu cầu từ Chuyên viên Kế toán (SH-05)")
    str_fin_headers = ["Mã StR", "Mô tả Yêu cầu của Bên liên quan (Stakeholder Requirement)", "Phân hệ Đáp ứng", "Mức ưu tiên"]
    str_fin_data = [
        ["STR-FIN-01", "Quản trị Biểu phí & Chiến dịch Voucher Chống Race Condition: Kế toán thiết lập học phí chuẩn theo khóa học và phát hành mã giảm giá Voucher (discount_percent hoặc discount_amount, giới hạn max_uses, min_order_value, expires_at) với cơ chế khóa dòng chống dùng vượt hạn mức.", "finance", "P0 (Bắt buộc)"],
        ["STR-FIN-02", "Quản lý Hóa đơn, Chia kỳ Trả góp & Thu tiền mặt Tại quầy: Kế toán theo dõi hóa đơn (COURSE_PACKAGE, STANDALONE_RETAKE, MIXED), lịch đóng phí nhiều đợt (installments), ghi nhận phiếu thu tiền mặt (CASH) gắn mã chứng từ duy nhất và tự động cập nhật trạng thái PARTIAL/PAID.", "finance, admission", "P0 (Bắt buộc)"],
        ["STR-FIN-03", "Quản lý Ví Bảo lưu & Cấn trừ Học phí Nội bộ: Kế toán ghi nhận tiền bảo lưu vào ví học viên (RESERVATION_CREDIT) và thực hiện cấn trừ sang hóa đơn khóa mới (TUITION_OFFSET), đảm bảo ví bảo lưu không bao giờ bị rút thành tiền mặt (is_withdrawable = FALSE).", "finance", "P0 (Bắt buộc)"],
        ["STR-FIN-04", "Xác minh Hoàn tiền Cấp 2 & Giải ngân Hoàn tất: Kế toán kiểm tra công nợ và số buổi thực học để xác minh yêu cầu hoàn tiền Cấp 2 (PENDING_DIRECTOR), và ghi nhận mã chứng từ Ủy nhiệm chi (COMPLETED) sau khi Giám đốc phê duyệt, đồng thời khóa bất biến hóa đơn ở trạng thái REFUNDED.", "finance", "P0 (Bắt buộc)"]
    ]
    create_table(doc, str_fin_headers, str_fin_data, [1.1, 3.7, 1.0, 0.7])

    add_heading_2(doc, "4.5. Nhóm Yêu cầu từ Ban Giám đốc & Quản trị Hệ thống (SH-06)")
    str_adm_headers = ["Mã StR", "Mô tả Yêu cầu của Bên liên quan (Stakeholder Requirement)", "Phân hệ Đáp ứng", "Mức ưu tiên"]
    str_adm_data = [
        ["STR-ADM-01", "Quản trị Định danh Tập trung, Nhân sự & Kiểm toán Bảo mật: Giám đốc/Admin tạo và quản lý tài khoản nhân sự (STAFF, TEACHER), khóa/mở khóa tài khoản, giám sát nhật ký kiểm toán đăng nhập (auth_audit_logs) và cảnh báo tấn công Brute-force.", "admission", "P0 (Bắt buộc)"],
        ["STR-ADM-02", "Phê duyệt Hoàn tiền Cấp 3 (Chốt kiểm soát cuối): Giám đốc là cấp duy nhất có quyền phê duyệt (APPROVED) hoặc từ chối (REJECTED) số tiền hoàn trả thực tế trên các hồ sơ hoàn phí đã qua Kế toán xác minh.", "finance", "P0 (Bắt buộc)"],
        ["STR-ADM-03", "Hệ thống Báo cáo Quản trị Thông minh (Executive BI Dashboards): Giám đốc theo dõi trực quan các chỉ số KPI thời gian thực: Phễu chuyển đổi tuyển sinh 4 tầng, Tỷ lệ lấp đầy lớp học (Occupancy Rate), Doanh thu thực thu thuần theo tháng (Cash-Basis Net Revenue), Dư nợ học phí chờ thu (Pipeline Revenue), Hiệu quả chiến dịch Voucher, và DAU/MAU hệ thống.", "admission, finance, learning", "P1 (Quan trọng)"]
    ]
    create_table(doc, str_adm_headers, str_adm_data, [1.1, 3.7, 1.0, 0.7])

    # --- 5. KỊCH BẢN VẬN HÀNH XUYÊN PHÂN HỆ ---
    add_heading_1(doc, "5. Kịch bản Vận hành Xuyên Phân hệ (Operational Scenarios / End-to-End Value Streams)")

    add_heading_2(doc, "5.1. Luồng Giá trị 1: Từ Khảo thí Đầu vào đến Nhập học & Tạo Hồ sơ Học viên Tự động")
    add_p(doc, "Đây là luồng nghiệp vụ xương sống của toàn hệ thống, liên kết chặt chẽ cả 4 phân hệ Microservices:")

    # Insert Diagram Flowchart
    add_diagram_image(doc, "docs/diagrams/diagram_value_stream_flowchart.png",
                      "Hình 2: Sơ đồ Tiến trình Luồng giá trị Tuyển sinh - Nhập học - Thanh toán", width_inches=6.0)

    # Insert Diagram Sequence
    add_diagram_image(doc, "docs/diagrams/diagram_value_stream_sequence.png",
                      "Hình 3: Sơ đồ Trình tự Chi tiết Giao tiếp Liên Phân hệ (15 bước E2E Sequence Diagram)", width_inches=6.2)

    add_heading_3(doc, "Chi tiết 15 bước thực thi liên service:")
    seq_steps = [
        "1. Học viên đăng ký tài khoản, xác thực email & làm bài Placement Test trên Admission Service (:8081).",
        "2. Admission Service tự động chấm điểm 3 kỹ năng & đề xuất khóa học phù hợp kèm danh sách lớp còn slot.",
        "3. Học viên nộp đơn đăng ký lớp học (kèm mã Voucher nếu có).",
        "4. Admission Service khóa kiểm tra slot (INV-ADM-01) & tạo bản ghi Enrollment ở trạng thái PENDING, giữ chỗ độc quyền 48h.",
        "5. Admission gọi REST API sang Finance: POST /api/finance/invoices/auto-generate (kèm header bảo mật X-Internal-Secret).",
        "6. Finance Service khóa Voucher bằng câu lệnh SELECT ... FOR UPDATE, tạo Invoice, Invoice Items & Installments.",
        "7. Finance trả về invoice_id, invoice_code, final_amount, payment_ref_code cho Admission Service.",
        "8. Học viên yêu cầu lấy mã QR thanh toán: GET /api/finance/invoices/{id}/vietqr.",
        "9. Finance Service tạo và trả về chuỗi mã QR động chứa số tiền chính xác và payment_ref_code.",
        "10. Học viên dùng ứng dụng ngân hàng quét mã QR chuyển khoản học phí.",
        "11. Cổng ngân hàng bắn Webhook sang Finance: POST /api/finance/webhooks/vietqr kèm mã giao dịch bank_transaction_id.",
        "12. Finance Service kiểm tra Idempotency chống ghi trùng, ghi nhận Payment, gạch nợ và cập nhật trạng thái Invoice = PAID.",
        "13. Finance Service bắn Webhook nội bộ sang Admission: POST /api/admission/webhooks/payment-success (X-Internal-Secret).",
        "14. Admission Service kiểm tra lại Invariant trần sĩ số (INV-ADM-01) & chuyển trạng thái đơn đăng ký sang APPROVED.",
        "15. Admission Service tự động đồng bộ sang Student Service (:8082) để khởi tạo hồ sơ học viên chính thức (mã HV-YYYY-NNNN)."
    ]
    for s in seq_steps:
        add_p(doc, s, size_pt=10.5, color_rgb=(30, 41, 59), space_before=1, space_after=3)

    add_heading_2(doc, "5.2. Luồng Giá trị 2: Giảng dạy, Điểm danh, Đánh giá & Xếp loại Học tập")
    add_bullet(doc, "1. Đăng nhập & Cấp quyền: ", "Giảng viên (TEACHER) đăng nhập qua Admission Service nhận JWT Token chứa vai trò hợp lệ.")
    add_bullet(doc, "2. Phân phối Học liệu: ", "Giảng viên mở lớp học được phân công trên Learning Service (:8083) để đăng tải giáo trình, slide và bài tập (learning.materials).")
    add_bullet(doc, "3. Điểm danh Điện tử: ", "Trong mỗi buổi học, Giảng viên thực hiện điểm danh trên Student Service (:8082) (student.attendance), đảm bảo mỗi học viên có đúng 1 trạng thái chuyên cần/buổi.")
    add_bullet(doc, "4. Nhập Điểm & Xếp loại Tự động: ", "Giữa kỳ và cuối kỳ, Giảng viên nhập điểm đánh giá trên Learning Service (learning.grades). Hệ thống tự động tính điểm trung bình chuẩn hóa thang 100% có trọng số qua View learning.v_student_grade_summary và thống kê tỷ lệ đỗ của cả lớp qua learning.v_class_pass_rate.")

    add_heading_2(doc, "5.3. Luồng Giá trị 3: Bảo lưu Ví Nội bộ & Quy trình Hoàn tiền 3 Cấp")
    add_p(doc, "Quy trình xử lý các biến động học vụ đặc biệt được quản lý nghiêm ngặt qua 2 trường hợp:")
    add_bullet(doc, "• Trường hợp 1 — Bảo lưu Ví Nội bộ: ",
               "Học viên nộp đơn xin bảo lưu trên Student Service (student.status_requests). Sau khi Học vụ duyệt, Kế toán cộng số dư tương ứng vào Ví bảo lưu trên Finance Service (POST /api/finance/wallets/{student_id}/reserve với tx_type = RESERVATION_CREDIT). Khi học viên đăng ký khóa mới, số dư này được dùng để cấn trừ trực tiếp vào hóa đơn (TUITION_OFFSET). Tuyệt đối không cho phép rút thành tiền mặt (is_withdrawable = FALSE).")
    add_bullet(doc, "• Trường hợp 2 — Hoàn tiền 3 Cấp Chặt chẽ:", "")
    
    refund_steps = [
        "  - Cấp 1 (Tư vấn viên - STAFF): Khởi tạo yêu cầu hoàn tiền (POST /api/finance/refunds). Finance đối chiếu số buổi đã điểm danh (attended_sessions <= max_allowed_sessions, mặc định tối đa 3 buổi). Nếu vượt quá 3 buổi, hệ thống tự động từ chối (INV-FIN-07).",
        "  - Cấp 2 (Kế toán - STAFF): Kiểm tra thực thu và xác minh công nợ (PUT /api/finance/refunds/{id}/verify), chuyển trạng thái sang PENDING_DIRECTOR.",
        "  - Cấp 3 (Giám đốc - ADMIN): Phê duyệt mức hoàn tiền cuối cùng (PUT /api/finance/refunds/{id}/approve), chuyển trạng thái sang APPROVED.",
        "  - Giải ngân Hoàn tất (Kế toán - STAFF): Chuyển khoản hoàn phí, cập nhật mã chứng từ UNC (PUT /api/finance/refunds/{id}/complete), chuyển yêu cầu sang COMPLETED và khóa hóa đơn thành REFUNDED."
    ]
    for r in refund_steps:
        add_p(doc, r, size_pt=10.5, color_rgb=(30, 41, 59), space_before=1, space_after=3)

    # --- 6. YÊU CẦU CHẤT LƯỢNG, BẤT BIẾN & RÀNG BUỘC HỆ THỐNG ---
    add_heading_1(doc, "6. Yêu cầu Chất lượng, Bất biến & Ràng buộc Hệ thống (Quality Attributes, Invariants & Constraints)")

    add_heading_2(doc, "6.1. Mười lăm Quy tắc Bất biến Cốt lõi (System & Domain Invariants)")
    add_p(doc, "Hệ thống bắt buộc duy trì 15 quy tắc bất biến (được kiểm chứng tự động 100% qua bộ test tests/invariants/):")

    invariants = [
        ("Kiến trúc Hệ thống (INV-SYS)", [
            "• INV-SYS-01: 100% trường dữ liệu JSON Request/Response và Query Params giữa mọi ngôn ngữ (Java & Python) phải sử dụng chuẩn snake_case.",
            "• INV-SYS-02: Cô lập tuyệt đối 4 Schemas (admission, student, learning, finance). Nghiêm cấm JOIN SQL hoặc tạo khóa ngoại vật lý chéo giữa các schema."
        ]),
        ("An ninh & Phân quyền (INV-SEC)", [
            "• INV-SEC-01: Học viên (STUDENT) chỉ được truy cập hồ sơ, hóa đơn, ví bảo lưu và bảng điểm của chính mình.",
            "• INV-SEC-02: Giảng viên (TEACHER) chỉ được điểm danh và nhập/sửa điểm cho các lớp mình được phân công phụ trách.",
            "• INV-SEC-03: Tuyệt đối không lưu mật khẩu dạng rõ (phải băm BCrypt) và không đưa khóa bí mật sản xuất (Production Secrets) vào kho mã nguồn Git."
        ]),
        ("Nghiệp vụ Tuyển sinh (INV-ADM)", [
            "• INV-ADM-01: Không bao giờ duyệt (APPROVED) vượt quá sĩ số tối đa của lớp (max_students) hoặc sức chứa phòng học (capacity).",
            "• INV-ADM-02: Mã khóa học (courses.code), mã lớp (classes.code) và mã phòng (rooms.code) là duy nhất toàn hệ thống.",
            "• INV-ADM-03: Tuân thủ chặt chẽ máy trạng thái đơn đăng ký; đơn đã CANCELLED hoặc REJECTED không được chuyển trực tiếp sang APPROVED."
        ]),
        ("Nghiệp vụ Học viên (INV-STU)", [
            "• INV-STU-01: Mỗi học viên chỉ có tối đa 1 bản ghi điểm danh trong 1 buổi học của 1 lớp (UNIQUE(class_id, student_id, session_date)).",
            "• INV-STU-02: Trạng thái học vụ của học viên chỉ thuộc tập hợp hợp lệ (STUDYING, RESERVED, DROPPED, GRADUATED)."
        ]),
        ("Nghiệp vụ Đào tạo (INV-LRN)", [
            "• INV-LRN-01: Điểm số đánh giá luôn thỏa mãn 0 <= score <= max_score và max_score > 0.",
            "• INV-LRN-02: Cô lập điểm thi cá nhân giữa các học viên (học viên không xem được điểm của nhau)."
        ]),
        ("Nghiệp vụ Tài chính (INV-FIN)", [
            "• INV-FIN-01: Hóa đơn và giao dịch thanh toán phải có số tiền dương; hóa đơn đã PAID hoặc REFUNDED là bất biến (không được sửa số tiền gốc/chiết khấu).",
            "• INV-FIN-02 / INV-FIN-05: Chống ghi nhận trùng lặp thanh toán (Idempotency) dựa trên ràng buộc UNIQUE(bank_transaction_id) và UNIQUE(transaction_ref).",
            "• INV-FIN-03: Hóa đơn chỉ chuyển sang trạng thái PAID khi tổng tiền thực thu (paid_amount) đạt đủ final_amount.",
            "• INV-FIN-04: Áp dụng khóa bi quan SELECT ... FOR UPDATE khi trừ lượt dùng Voucher (current_uses <= max_uses).",
            "• INV-FIN-06: Ví bảo lưu học viên luôn khóa rút tiền mặt (is_withdrawable = FALSE).",
            "• INV-FIN-07: Chỉ cho phép hoàn tiền khi số buổi đã học attended_sessions <= max_allowed_sessions và đi qua đủ 3 cấp phê duyệt."
        ])
    ]

    for category, rules in invariants:
        add_callout(doc, rules, title=category)
        add_p(doc, "", space_after=4)

    add_heading_2(doc, "6.2. Yêu cầu Phi chức năng (Non-Functional Requirements - NFR)")
    add_bullet(doc, "1. Tính toàn vẹn giao dịch & Đồng thời (Concurrency & ACID): ",
               "Sử dụng khóa bi quan (SELECT ... FOR UPDATE) hoặc PostgreSQL Advisory Lock tại các điểm nóng tranh chấp tài nguyên: đăng ký slot cuối cùng của lớp, xếp lịch phòng học/giáo viên, áp dụng Voucher số lượng có hạn, và gạch nợ hóa đơn.")
    add_bullet(doc, "2. Khả năng chịu lỗi tích hợp (Fault Tolerance & Idempotency): ",
               "Giao tiếp nội bộ giữa các service sử dụng header bảo mật X-Internal-Secret kèm cơ chế chống trùng lặp (Idempotency) khi gọi lại (auto-generate hóa đơn theo enrollment_id, callback thanh toán theo event_id/bank_transaction_id, đồng bộ học viên qua Outbox/Retry Worker).")
    add_bullet(doc, "3. Bảo mật Giao diện Người dùng (Frontend Security): ",
               "Toàn bộ dữ liệu động hiển thị lên DOM đều phải được escape HTML (escapeHtml) nhằm triệt tiêu lỗ hổng Cross-Site Scripting (XSS).")

    # --- 7. MA TRẬN TRUY VẾT YÊU CẦU TOÀN DỰ ÁN ---
    add_heading_1(doc, "7. Ma trận Truy vết Yêu cầu Toàn Dự án (End-to-End Traceability Matrix)")
    add_p(doc, "Ma trận dưới đây bảo đảm mọi yêu cầu của Stakeholder (StR) đều được ánh xạ trực tiếp đến tính năng phân hệ, cấu trúc bảng/view CSDL, API endpoint và ca kiểm thử tự động:")

    trace_headers = ["Mã StR", "Bên liên quan", "Tính năng (FR)", "Bảng / View CSDL Chính", "API Endpoint Tiêu biểu", "Kiểm thử Bất biến"]
    trace_data = [
        ["STR-STU-01", "GUEST, STUDENT", "FR-TST-01..05", "admission.test_sets, test_questions, test_attempts, test_submissions, test_recommendation_rules", "POST /api/admission/tests/start, POST /api/admission/tests/submit", "PlacementTestServiceTest"],
        ["STR-STU-02", "STUDENT, STAFF", "FR-ENR-01..05, FIN-F03", "admission.enrollments, finance.invoices, finance.invoice_items, finance.installments", "POST /api/admission/enrollments, POST /api/finance/invoices/auto-generate", "INV-ADM-01, INV-FIN-01"],
        ["STR-STU-03", "STUDENT, WEBHOOK", "FIN-F02, FIN-F06, FR-ENR-06", "finance.vouchers, finance.payments, admission.enrollments", "POST /api/finance/vouchers/validate, GET .../vietqr, POST /api/finance/webhooks/vietqr", "INV-FIN-02, INV-FIN-03"],
        ["STR-STU-04", "STUDENT", "LRN-01, LRN-02, LRN-03", "learning.materials, learning.grades, learning.v_student_grade_summary", "GET /api/learning/materials, GET /api/learning/grades", "INV-LRN-02, INV-SEC-01"],
        ["STR-STU-05", "STUDENT, STAFF", "STU-01, STU-03, FIN-F07", "student.students, student.status_requests, finance.student_wallets", "GET /api/student/students, GET /api/finance/wallets/{student_id}", "INV-STU-02, INV-FIN-06"],
        ["STR-TCH-01", "TEACHER", "STU-02", "student.attendance", "GET/POST /api/student/attendance", "INV-STU-01"],
        ["STR-TCH-02", "TEACHER", "LRN-01", "learning.materials", "POST /api/learning/materials", "INV-SYS-01"],
        ["STR-TCH-03", "TEACHER", "LRN-02", "learning.grades, learning.v_class_pass_rate", "POST /api/learning/grades", "INV-LRN-01, INV-SEC-02"],
        ["STR-ACA-01", "STAFF, ADMIN", "FR-FAC-01..03, FR-ACA-01..05", "admission.rooms, admission.courses, admission.classes", "POST /api/admission/rooms, POST /api/admission/courses, POST /api/admission/classes", "INV-ADM-02, ConflictEngineTest"],
        ["STR-ACA-02", "STAFF", "FR-ENR-04, FR-ENR-05, FR-ENR-08", "admission.enrollments, student.students", "PUT /api/admission/enrollments/{id}/extend-hold", "INV-ADM-03"],
        ["STR-ACA-03", "STAFF, ADMIN", "FR-ACA-04, FIN-F08B", "admission.classes, admission.enrollments, finance.refund_requests", "PUT /api/admission/classes/{id}/cancel, POST /api/finance/internal/class-cancellations", "ClassCancellationTest"],
        ["STR-FIN-01", "STAFF, ADMIN", "FIN-F01, FIN-F02", "finance.tuition_fees, finance.vouchers", "GET/POST /api/finance/tuition-fees, GET/POST /api/finance/vouchers", "INV-FIN-04"],
        ["STR-FIN-02", "STAFF", "FIN-F04, FIN-F05", "finance.invoices, finance.installments, finance.payments", "POST /api/finance/payments/cash, GET /api/finance/invoices/{id}", "INV-FIN-02, INV-FIN-03"],
        ["STR-FIN-03", "STAFF", "FIN-F07", "finance.student_wallets, finance.wallet_transactions", "POST /api/finance/wallets/{student_id}/reserve", "INV-FIN-06"],
        ["STR-FIN-04", "STAFF, ADMIN", "FIN-F08", "finance.refund_requests, finance.invoices", "POST /api/finance/refunds, PUT .../verify, PUT .../approve, PUT .../complete", "INV-FIN-07"],
        ["STR-ADM-01", "ADMIN", "FR-IAM-01..07", "admission.users, refresh_tokens, password_reset_tokens, auth_audit_logs", "POST /auth/login, GET /api/admission/users, GET /api/admission/audit-logs", "INV-SEC-03, AuthServiceTest"],
        ["STR-ADM-03", "ADMIN, STAFF", "BI-01..03, FIN-F09", "finance.vw_bi_cash_revenue_monthly, finance.vw_bi_voucher_campaigns, learning.v_class_pass_rate", "GET /api/admission/bi/*, GET /api/finance/bi/*", "BiAnalyticsServiceTest, test_finance_api.py"]
    ]
    create_table(doc, trace_headers, trace_data, [0.8, 0.9, 0.9, 1.6, 1.3, 1.0])

    # --- 8. TIÊU CHÍ NGHIỆM THU & PHẠM VI LOẠI TRỪ ---
    add_heading_1(doc, "8. Tiêu chí Nghiệm thu & Phạm vi Loại trừ (Acceptance Criteria & Explicit Non-Goals)")

    add_heading_2(doc, "8.1. Tiêu chí Nghiệm thu Cấp Hệ thống (System-Wide Acceptance Criteria)")
    add_p(doc, "Dự án được nghiệm thu đạt chuẩn khi thỏa mãn đồng thời 4 điều kiện sau:")
    add_bullet(doc, "1. Khởi tạo Cơ sở Dữ liệu: ", "Kịch bản scripts/init-db.sql chạy thành công không lỗi trên PostgreSQL 16, tạo đủ 4 schemas (admission, student, learning, finance), 28 bảng, 4 views, 35 triggers và dữ liệu mẫu chuẩn.")
    add_bullet(doc, "2. Vượt qua Bộ Kiểm thử Bất biến Tự động: ", "Lệnh python -m unittest discover -s tests/invariants -p \"test_*.py\" -v đạt kết quả tuyệt đối 15/15 OK.")
    add_bullet(doc, "3. Vận hành Trọn vẹn trên Docker Compose: ", "Toàn bộ 6 containers (ec-postgres, ec-admission, ec-student, ec-learning, ec-finance, ec-gateway) khởi chạy ở trạng thái Up / Healthy và định tuyến chính xác qua cổng duy nhất http://localhost:8080.")
    add_bullet(doc, "4. Đảm bảo Độc lập & Tích hợp Liên Phân hệ: ", "Mỗi phân hệ hoạt động đúng trên schema riêng của mình và giao tiếp thành công qua các hợp đồng REST API / Webhook nội bộ (X-Internal-Secret).")

    add_heading_2(doc, "8.2. Phạm vi Cố ý Không Thực hiện (Explicit Non-Goals)")
    add_p(doc, "Để tập trung nguồn lực bảo đảm chất lượng cốt lõi, các hạng mục sau nằm ngoài phạm vi của phiên bản hiện tại:")
    add_bullet(doc, "1. ", "Không tích hợp Chatbot AI tư vấn tự động hoặc AI chấm bài tự luận (Speaking/Writing).")
    add_bullet(doc, "2. ", "Không sử dụng thuật toán tối ưu hóa xếp lịch tự động bằng AI/Heuristic Solver (chỉ kiểm tra và chặn xung đột lịch).")
    add_bullet(doc, "3. ", "Không kết nối cổng thanh toán thương mại tính phí thật (VNPay/MoMo Production) hoặc cổng SMS/Zalo ZNS trả phí.")
    add_bullet(doc, "4. ", "Không bao gồm phân hệ kế toán nội bộ doanh nghiệp (chi phí vận hành OpEx, tính lương giáo viên/nhân viên, khấu hao tài sản).")
    add_bullet(doc, "5. ", "Không phát triển ứng dụng di động Native (iOS/Android); giao diện người dùng được cung cấp qua nền tảng Web Responsive.")

    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    doc.save(output_path)
    print(f"Successfully created: {output_path}")

if __name__ == '__main__':
    build_document('docs/StRS_Stakeholder_Requirements_Specification.docx')
