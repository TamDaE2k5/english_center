# Hướng dẫn Vận hành Agent & Quy tắc Dự án (SSOT)

> **Lưu ý**: Tài liệu điều phối chi tiết và thực dụng nhất cho dự án được đặt tại **`GEMINI.md`**.
> File này cung cấp hướng dẫn tương thích chuẩn cho Claude Code. Mọi Agent đều đọc chung Single Source of Truth (SSOT).

---

## 1. Bản chất & Triết lý Dự án (Core Philosophy)
Tham khảo chi tiết tại [`GEMINI.md`](GEMINI.md):
- **Hệ thống**: Quản lý trung tâm Anh ngữ theo kiến trúc Microservices Polyglot Monorepo (Spring Boot + FastAPI).
- **Thực dụng & Tối giản**: Làm từng bước nhỏ, test độc lập trước khi mở rộng. Không over-engineering. Không viết code phòng thủ cho vấn đề chưa tồn tại.
- **Ranh giới bất biến**: Không tự bịa thông tin học phí/điểm số; không duyệt đơn quá sĩ số lớp; cô lập dữ liệu giữa các học viên và giáo viên.

---

## 2. Các lệnh Thường dùng (Commands)
```bash
# Khởi chạy hệ thống toàn diện qua Docker
docker compose up -d --build

# Dừng hệ thống
docker compose down

# Chạy toàn bộ Invariant Tests (Trọng tài chất lượng - 15 tests)
python3 -m unittest discover -s tests/invariants -p "test_*.py" -v

# Kiểm tra cú pháp Compose
docker compose config
```

---

## 3. Quy ước Bắt buộc (Conventions)
1. **API & Data**: Toàn bộ Request payload, Response JSON và query parameters đều tuân theo chuẩn **`snake_case`**.
2. **Invariant Tests là Bất biến**: Tuyệt đối không sửa assertion trong `tests/invariants/` để làm test pass. Nếu test fail, phải sửa code nghiệp vụ của service tương ứng.
3. **Frontend Hot-Reload**: Chỉnh sửa HTML/JS/CSS chỉ cần lưu file và Refresh trình duyệt (`http://localhost:8080`), không cần build lại container.
4. **Không đưa secret lên Git**: Dùng `.env.example` làm mẫu; giữ nguyên tắc bí mật cấu hình.
5. **Cập nhật Tiến độ**: Teammate Agent bàn giao task qua `docs/tasks/completed/`. Chỉ Team Lead mới có quyền cập nhật `docs/state.md`.

---

## 4. Tài liệu Tham khảo Chuyên sâu
- **Đặc tả nghiệp vụ (PRD) & Non-Goals**: [`docs/spec.md`](docs/spec.md)
- **Hợp đồng kỹ thuật (API, Domain, State Machine)**: [`docs/contract.md`](docs/contract.md)
- **Danh mục 15 Quy tắc Bất biến**: [`docs/invariants.md`](docs/invariants.md)
- **Nhật ký Trạng thái Dự án**: [`docs/state.md`](docs/state.md)
- **Quy trình Phối hợp Đa Agent**: [`docs/agent-workflow.md`](docs/agent-workflow.md)
- **Kiến trúc Tổng quan**: [`docs/architecture.md`](docs/architecture.md)
