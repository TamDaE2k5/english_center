# TASK-FIN-70: Giao diện Tra cứu & Quản lý Hóa đơn, Lịch trả góp (invoices.html)

- **Mã nhiệm vụ**: `TASK-FIN-70`
- **Phân kỳ**: `Phase 7: Giao diện Người dùng Frontend (UI)`
- **Độ ưu tiên**: `P1`
- **Yêu cầu chức năng liên quan**: UI Phân hệ Finance (`invoices.html`)

---

## 1. Mục tiêu Nghiệp vụ & Kỹ thuật
Nâng cấp trang `services/finance/frontend/invoices.html`:
1. Sử dụng Design System chung của trung tâm (`shared/css/base.css`, `shared/css/components.css`).
2. Hiển thị danh sách hóa đơn với badge trạng thái chuẩn màu:
   - `PAID`: Xanh lá (`--color-success`)
   - `PARTIAL`: Vàng cam (`--color-warning`)
   - `PENDING`: Đỏ nhẹ (`--color-danger`)
   - `REFUNDED`: Tím / Xám
3. Bảng mở rộng hiển thị các khoản mục `invoice_items` và các kỳ trả góp `installments`.
4. Tìm kiếm theo mã hóa đơn, mã học viên, bộ lọc trạng thái.

---

## 2. Tiêu chí Nghiệm thu
- [ ] Giao diện responsive, tải dữ liệu mượt mà từ API.
- [ ] Hỗ trợ hot-reload qua Nginx Gateway.
