import os
import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch

plt.rcParams['font.family'] = 'Times New Roman'
plt.rcParams['figure.autolayout'] = False

# Styling colors matching reference
BOX_BG = '#EBF3FC'
BOX_BORDER = '#B9D5F3'
TEXT_COLOR = '#0F2942'
LIFELINE_COLOR = '#94A3B8'
ARROW_COLOR = '#334155'
SELF_COLOR = '#2563EB'
RETURN_COLOR = '#0284C7'

def draw_sequence_diagram(output_path):
    # Dimensions: 5 participants, 15 steps
    fig, ax = plt.subplots(figsize=(15, 14), dpi=300)
    ax.set_xlim(0, 150)
    ax.set_ylim(0, 160)
    ax.axis('off')

    # Title
    ax.text(75, 155, "SƠ ĐỒ TRÌNH TỰ LUỒNG GIÁ TRỊ VẬN HÀNH XUYÊN PHÂN HỆ (END-TO-END VALUE STREAM)",
            ha='center', va='center', fontsize=14, fontweight='bold', color=TEXT_COLOR)

    # 5 Participants
    participants = [
        {"id": "HV", "name": "Học viên\n(GUEST / STUDENT)", "x": 16},
        {"id": "ADM", "name": "Admission Service\n(:8081)", "x": 45},
        {"id": "FIN", "name": "Finance Service\n(:8084)", "x": 75},
        {"id": "QR", "name": "Cổng Ngân hàng\n(VietQR)", "x": 105},
        {"id": "STU", "name": "Student Service\n(:8082)", "x": 134}
    ]

    top_y = 142
    bottom_y = 8
    box_w = 24
    box_h = 7.5

    # Draw participant boxes and lifelines
    for p in participants:
        px = p["x"]
        # Top Box
        box = FancyBboxPatch((px - box_w/2, top_y - box_h/2), box_w, box_h,
                             boxstyle="round,pad=0.2,rounding_size=1.2",
                             facecolor=BOX_BG, edgecolor=BOX_BORDER, linewidth=1.5, zorder=3)
        ax.add_patch(box)
        ax.text(px, top_y, p["name"], ha='center', va='center',
                fontsize=10.5, fontweight='bold', color=TEXT_COLOR, zorder=4)

        # Lifeline
        ax.plot([px, px], [top_y - box_h/2, bottom_y],
                color=LIFELINE_COLOR, linestyle='--', linewidth=1.1, zorder=1)

    # 15 Sequence steps
    steps = [
        # (step_num, from_id, to_id, text, is_dashed, is_self)
        (1, "HV", "ADM", "1. Đăng ký tài khoản, xác thực email & làm bài Placement Test", False, False),
        (2, "ADM", "HV", "2. Chấm điểm 3 kỹ năng & đề xuất khóa học + lớp còn slot", True, False),
        (3, "HV", "ADM", "3. Nộp đơn đăng ký lớp học (kèm mã Voucher nếu có)", False, False),
        (4, "ADM", "ADM", "4. Khóa kiểm tra slot & tạo Enrollment (PENDING, giữ chỗ 48h)", False, True),
        (5, "ADM", "FIN", "5. POST /api/finance/invoices/auto-generate (X-Internal-Secret)", False, False),
        (6, "FIN", "FIN", "6. Khóa Voucher (FOR UPDATE), tạo Invoice, Items & Installments", False, True),
        (7, "FIN", "ADM", "7. Trả về invoice_id, invoice_code, final_amount, payment_ref_code", True, False),
        (8, "HV", "FIN", "8. GET /api/finance/invoices/{id}/vietqr", False, False),
        (9, "FIN", "HV", "9. Hiển thị mã QR động chứa payment_ref_code", True, False),
        (10, "HV", "QR", "10. Quét mã QR chuyển khoản học phí", False, False),
        (11, "QR", "FIN", "11. POST /api/finance/webhooks/vietqr (bank_transaction_id)", False, False),
        (12, "FIN", "FIN", "12. Kiểm tra Idempotency, ghi Payment & cập nhật Invoice = PAID", False, True),
        (13, "FIN", "ADM", "13. POST /api/admission/webhooks/payment-success (X-Internal-Secret)", False, False),
        (14, "ADM", "ADM", "14. Kiểm tra Invariant sĩ số (INV-ADM-01) & Duyệt đơn = APPROVED", False, True),
        (15, "ADM", "STU", "15. Đồng bộ tạo hồ sơ học viên chính thức (HV-YYYY-NNNN)", False, False)
    ]

    p_map = {p["id"]: p["x"] for p in participants}
    step_y = 132
    y_step_gap = 8.2

    for num, from_p, to_p, msg, is_dashed, is_self in steps:
        y = step_y - (num - 1) * y_step_gap

        if is_self:
            # Self call loop arrow
            sx = p_map[from_p]
            loop_w = 7
            loop_h = 3.5
            # Draw self-loop path
            ax.annotate('', xy=(sx, y - loop_h), xytext=(sx, y),
                        arrowprops=dict(arrowstyle="->,head_width=0.35,head_length=0.5",
                                        color=SELF_COLOR, lw=1.2,
                                        connectionstyle=f"arc,angleA=0,angleB=0,armA={loop_w},armB={loop_w},rad=3"))
            # Label
            ax.text(sx + loop_w + 1, y - loop_h/2, msg, ha='left', va='center',
                    fontsize=9.5, fontweight='normal', color=TEXT_COLOR,
                    bbox=dict(boxstyle="round,pad=0.2,rounding_size=0.8", facecolor='#F1F5F9', edgecolor='#CBD5E1', lw=0.8))
        else:
            x1 = p_map[from_p]
            x2 = p_map[to_p]
            arrow_ls = '--' if is_dashed else '-'
            color = RETURN_COLOR if is_dashed else ARROW_COLOR

            ax.annotate('', xy=(x2, y), xytext=(x1, y),
                        arrowprops=dict(arrowstyle="->,head_width=0.35,head_length=0.5",
                                        color=color, lw=1.3, linestyle=arrow_ls))
            mid_x = (x1 + x2) / 2
            ax.text(mid_x, y + 1.8, msg, ha='center', va='bottom',
                    fontsize=9.5, fontweight='normal', color=TEXT_COLOR,
                    bbox=dict(boxstyle="round,pad=0.2,rounding_size=0.8", facecolor='#F8FAFC', edgecolor='#E2E8F0', lw=0.7))

    plt.tight_layout()
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    plt.savefig(output_path, dpi=300, bbox_inches='tight')
    plt.close()
    print(f"Generated {output_path}")

if __name__ == '__main__':
    draw_sequence_diagram('docs/diagrams/diagram_value_stream_sequence.png')
