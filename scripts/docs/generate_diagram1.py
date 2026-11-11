import os
import matplotlib.pyplot as plt
import matplotlib.patches as patches
from matplotlib.patches import FancyBboxPatch, ConnectionPatch

plt.rcParams['font.family'] = 'Times New Roman'
plt.rcParams['figure.autolayout'] = False

# Colors matching user reference
BOX_BG = '#EBF3FC'
BOX_BORDER = '#B9D5F3'
TEXT_COLOR = '#0F2942'
GROUP_BG = '#F8FAFC'
GROUP_BORDER = '#CBD5E1'
GROUP_TITLE = '#334155'
ARROW_COLOR = '#475569'
CENTER_BG = '#DCEBFA'
CENTER_BORDER = '#7BAAEB'

def draw_diagram_stakeholders(output_path):
    fig, ax = plt.subplots(figsize=(14, 10), dpi=300)
    ax.set_xlim(0, 140)
    ax.set_ylim(0, 100)
    ax.axis('off')

    # Draw Title
    ax.text(70, 96, "SƠ ĐỒ HỆ SINH THÁI CÁC BÊN LIÊN QUAN (STAKEHOLDER ECOSYSTEM)", 
            ha='center', va='center', fontsize=15, fontweight='bold', color=TEXT_COLOR)

    # Helper function for rounded box
    def draw_box(x, y, w, h, text, is_center=False, fontsize=11):
        bg = CENTER_BG if is_center else BOX_BG
        border = CENTER_BORDER if is_center else BOX_BORDER
        box = FancyBboxPatch((x, y), w, h,
                             boxstyle="round,pad=0.2,rounding_size=1.5",
                             facecolor=bg, edgecolor=border, linewidth=1.5, zorder=3)
        ax.add_patch(box)
        weight = 'bold' if is_center else 'normal'
        ax.text(x + w/2, y + h/2, text, ha='center', va='center',
                fontsize=fontsize, fontweight=weight, color=TEXT_COLOR, zorder=4)
        return (x, y, w, h)

    def draw_group(x, y, w, h, title):
        group_box = FancyBboxPatch((x, y), w, h,
                                   boxstyle="round,pad=0.5,rounding_size=1.0",
                                   facecolor=GROUP_BG, edgecolor=GROUP_BORDER,
                                   linewidth=1.2, linestyle='--', zorder=1)
        ax.add_patch(group_box)
        ax.text(x + w/2, y + h - 2.5, title, ha='center', va='center',
                fontsize=11.5, fontweight='bold', color=GROUP_TITLE, zorder=2)

    # 1. Group Left: External Stakeholders
    draw_group(5, 10, 38, 78, "Bên liên quan Bên ngoài & Học viên")
    b_st1 = draw_box(7, 65, 34, 13, "Khách vãng lai / Thí sinh\n(GUEST)")
    b_st2 = draw_box(7, 43, 34, 13, "Học viên chính thức\n(STUDENT)")
    b_st7 = draw_box(7, 21, 34, 13, "Cổng Thanh toán Ngân hàng\n(VietQR Webhook)")

    # 2. Central Entity: English Center System
    b_ec = draw_box(52, 38, 36, 20, "HỆ THỐNG QUẢN LÝ\nTRUNG TÂM ANH NGỮ\n(English Center Management)", is_center=True, fontsize=12)

    # 3. Group Right Top: Internal Operations
    draw_group(97, 46, 38, 42, "Đội ngũ Vận hành & Giảng dạy Nội bộ")
    b_st3 = draw_box(99, 73, 34, 10, "Giảng viên (TEACHER)")
    b_st4 = draw_box(99, 61, 34, 10, "Chuyên viên Tư vấn & Học vụ\n(STAFF - Academic)")
    b_st5 = draw_box(99, 49, 34, 10, "Chuyên viên Kế toán\n(STAFF - Accountant)")

    # 4. Group Right Bottom: Executive & Tech
    draw_group(97, 10, 38, 32, "Ban Giám đốc & Quản trị Kỹ thuật")
    b_st6 = draw_box(99, 27, 34, 10, "Ban Giám đốc / Quản trị viên\n(ADMIN)")
    b_st8 = draw_box(99, 15, 34, 10, "Đội ngũ Kỹ thuật & QA\n(DevOps / QA)")

    # Arrows helper with elbow or straight lines
    def connect(from_pt, to_pt, rad=0.0):
        ax.annotate('', xy=to_pt, xytext=from_pt,
                    arrowprops=dict(arrowstyle="->,head_width=0.4,head_length=0.6",
                                    color=ARROW_COLOR, lw=1.3,
                                    connectionstyle=f"arc3,rad={rad}"))

    # Connect Left to Center
    connect((41, 71.5), (52, 53), rad=0.1)
    connect((41, 49.5), (52, 48), rad=0.0)
    connect((41, 27.5), (52, 43), rad=-0.1)

    # Connect Right to Center
    connect((99, 78), (88, 53), rad=-0.1)
    connect((99, 66), (88, 50), rad=-0.05)
    connect((99, 54), (88, 47), rad=0.0)
    connect((99, 32), (88, 44), rad=0.05)
    connect((99, 20), (88, 41), rad=0.1)

    plt.tight_layout()
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    plt.savefig(output_path, dpi=300, bbox_inches='tight')
    plt.close()
    print(f"Generated {output_path}")

if __name__ == '__main__':
    draw_diagram_stakeholders('docs/diagrams/diagram_stakeholder_ecosystem.png')
