import os
import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch

plt.rcParams['font.family'] = 'Times New Roman'
plt.rcParams['figure.autolayout'] = False

# Colors matching user reference image
BOX_BG = '#EBF3FC'
BOX_BORDER = '#B9D5F3'
TEXT_COLOR = '#0F2942'
SUBTEXT_COLOR = '#475569'
ARROW_COLOR = '#64748B'

def draw_value_stream_flowchart(output_path):
    fig, ax = plt.subplots(figsize=(13, 11), dpi=300)
    ax.set_xlim(0, 130)
    ax.set_ylim(0, 110)
    ax.axis('off')

    # Title
    ax.text(65, 106, "SƠ ĐỒ TIẾN TRÌNH LUỒNG GIÁ TRỊ TUYỂN SINH - NHẬP HỌC - THANH TOÁN",
            ha='center', va='center', fontsize=14, fontweight='bold', color=TEXT_COLOR)

    def draw_card(x, y, w, h, title, subtitle="", service_badge=""):
        box = FancyBboxPatch((x, y), w, h,
                             boxstyle="round,pad=0.3,rounding_size=1.2",
                             facecolor=BOX_BG, edgecolor=BOX_BORDER, linewidth=1.5, zorder=3)
        ax.add_patch(box)

        # Center Y calculation
        if subtitle and service_badge:
            ax.text(x + w/2, y + h - 1.8, title, ha='center', va='center',
                    fontsize=10.5, fontweight='bold', color=TEXT_COLOR, zorder=4)
            ax.text(x + w/2, y + h/2, subtitle, ha='center', va='center',
                    fontsize=9.5, fontweight='normal', color=SUBTEXT_COLOR, zorder=4)
            ax.text(x + w/2, y + 1.8, service_badge, ha='center', va='center',
                    fontsize=8.5, fontweight='bold', color='#2563EB', zorder=4)
        elif subtitle:
            ax.text(x + w/2, y + h*0.65, title, ha='center', va='center',
                    fontsize=11, fontweight='bold', color=TEXT_COLOR, zorder=4)
            ax.text(x + w/2, y + h*0.32, subtitle, ha='center', va='center',
                    fontsize=9.5, fontweight='normal', color=SUBTEXT_COLOR, zorder=4)
        else:
            ax.text(x + w/2, y + h/2, title, ha='center', va='center',
                    fontsize=11, fontweight='bold', color=TEXT_COLOR, zorder=4)
        return (x, y, w, h)

    def connect_straight(from_box, to_box):
        fx = from_box[0] + from_box[2]/2
        fy = from_box[1]
        tx = to_box[0] + to_box[2]/2
        ty = to_box[1] + to_box[3]
        ax.annotate('', xy=(tx, ty), xytext=(fx, fy),
                    arrowprops=dict(arrowstyle="->,head_width=0.4,head_length=0.6",
                                    color=ARROW_COLOR, lw=1.3))

    def connect_elbow(from_pt, to_pt, via_x=None, via_y=None):
        fx, fy = from_pt
        tx, ty = to_pt
        if via_y is not None:
            # down to via_y, then horizontal, then down to ty
            ax.plot([fx, fx], [fy, via_y], color=ARROW_COLOR, lw=1.3)
            ax.plot([fx, tx], [via_y, via_y], color=ARROW_COLOR, lw=1.3)
            ax.annotate('', xy=(tx, ty), xytext=(tx, via_y),
                        arrowprops=dict(arrowstyle="->,head_width=0.4,head_length=0.6",
                                        color=ARROW_COLOR, lw=1.3))
        else:
            ax.annotate('', xy=(tx, ty), xytext=(fx, fy),
                        arrowprops=dict(arrowstyle="->,head_width=0.4,head_length=0.6",
                                        color=ARROW_COLOR, lw=1.3))

    card_w = 70
    card_h = 7.5
    start_x = 30

    # Step 1
    c1 = draw_card(start_x, 92, card_w, card_h,
                   "Bước 1: Khảo thí Đầu vào & Đề xuất Khóa học",
                   "Làm bài test 3 kỹ năng trực tuyến (Listening, Reading, Grammar), chấm điểm tự động",
                   "[ Phân hệ: Admission Service (:8081) ]")

    # Step 2
    c2 = draw_card(start_x, 78, card_w, card_h,
                   "Bước 2: Nộp Đơn Đăng ký Lớp & Giữ Chỗ Tạm thời 48h",
                   "Khóa kiểm tra sĩ số (INV-ADM-01), tạo đơn PENDING và giữ chỗ độc quyền 48 giờ",
                   "[ Phân hệ: Admission Service (:8081) ]")
    connect_straight(c1, c2)

    # Step 3
    c3 = draw_card(start_x, 64, card_w, card_h,
                   "Bước 3: Tự động Phát hành Hóa đơn & Khóa Lượt dùng Voucher",
                   "Khóa Voucher chống race condition (FOR UPDATE), chia kỳ installments",
                   "[ Phân hệ: Finance Service (:8084) — X-Internal-Secret ]")
    connect_straight(c2, c3)

    # Step 4: Branching or payment step
    # Left: VietQR online, Right: Cash at counter
    b4_w = 46
    c4_left = draw_card(15, 48, b4_w, 9.5,
                        "Bước 4A: Thanh toán VietQR Động",
                        "Quét mã QR động chuyển khoản ngân hàng\nIdempotent webhook theo bank_transaction_id",
                        "[ Cổng VietQR & Finance :8084 ]")

    c4_right = draw_card(69, 48, b4_w, 9.5,
                         "Bước 4B: Thu tiền mặt Tại quầy",
                         "Thu tiền trực tiếp, lập phiếu thu CASH\nghi nhận mã chứng từ kế toán duy nhất",
                         "[ Finance Service :8084 ]")

    # Connect c3 to c4_left and c4_right
    connect_elbow((c3[0] + c3[2]/2, c3[1]), (c4_left[0] + c4_left[2]/2, c4_left[1] + c4_left[3]), via_y=59)
    connect_elbow((c3[0] + c3[2]/2, c3[1]), (c4_right[0] + c4_right[2]/2, c4_right[1] + c4_right[3]), via_y=59)

    # Step 5: Payment Confirmed & Invoice Paid
    c5 = draw_card(start_x, 32, card_w, card_h,
                   "Bước 5: Gạch nợ Hóa đơn & Thông báo Thanh toán Thành công",
                   "Hóa đơn chuyển trạng thái PAID khi paid_amount >= final_amount (INV-FIN-03)",
                   "[ Phân hệ: Finance Service (:8084) ]")
    connect_elbow((c4_left[0] + c4_left[2]/2, c4_left[1]), (start_x + card_w/2, c5[1] + c5[3]), via_y=43)
    connect_elbow((c4_right[0] + c4_right[2]/2, c4_right[1]), (start_x + card_w/2, c5[1] + c5[3]), via_y=43)

    # Step 6: Admission approval
    c6 = draw_card(start_x, 19, card_w, card_h,
                   "Bước 6: Duyệt Đơn Nhập học Chính thức (APPROVED)",
                   "Kiểm tra lại trần sĩ số lớp, chuyển trạng thái đơn sang APPROVED",
                   "[ Phân hệ: Admission Service (:8081) ]")
    connect_straight(c5, c6)

    # Step 7: Sync Student Record
    c7 = draw_card(start_x, 6, card_w, card_h,
                   "Bước 7: Đồng bộ Hồ sơ Học viên 360 Độ & Cấp Mã Học viên",
                   "Khởi tạo bản ghi hồ sơ học viên chính thức với định dạng chuẩn HV-YYYY-NNNN",
                   "[ Phân hệ: Student Service (:8082) ]")
    connect_straight(c6, c7)

    plt.tight_layout()
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    plt.savefig(output_path, dpi=300, bbox_inches='tight')
    plt.close()
    print(f"Generated {output_path}")

if __name__ == '__main__':
    draw_value_stream_flowchart('docs/diagrams/diagram_value_stream_flowchart.png')
