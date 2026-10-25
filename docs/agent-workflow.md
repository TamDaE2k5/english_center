# Quy trình Phối hợp Đa Agent (Multi-Agent Workflow)

> **Mục tiêu**: Hướng dẫn quy trình nhận việc, phân quyền sửa đổi file, kiểm tra chéo và bàn giao giữa các AI Agent trong dự án.

---

## 1. Mô hình Phân vai Agent

### Vai trò 1: Team Lead (Orchestrator)
- **Trách nhiệm**:
  - Duy trì kiến trúc tổng thể và tài liệu SSOT.
  - Phân chia task trong `docs/tasks/`.
  - Kiểm tra và review code trước khi merge vào nhánh chính.
  - Là **người duy nhất** được quyền cập nhật file `docs/state.md`.
  - Quyết định khi nào một Wave/Sprint được coi là hoàn thành.

### Vai trò 2: Backend Developer Agent (Service Owner)
- **Trách nhiệm**: Hiện thực hóa business logic, REST API, tương tác DB schema của service được giao.
- **Ranh giới file**: Chỉ được sửa file trong thư mục service của mình (ví dụ `services/admission/backend/`). Không sửa code của service khác.

### Vai trò 3: Frontend Developer Agent
- **Trách nhiệm**: Xây dựng giao diện web HTML/JS/CSS cho module, kết nối API backend qua `shared/js/api.js`.
- **Ranh giới file**: `services/{service}/frontend/` và tham chiếu assets từ `shared/`. Không sửa logic `shared/css/variables.css` nếu chưa được Team Lead duyệt.

### Vai trò 4: QA & Invariant Test Agent
- **Trách nhiệm**: Chạy test kiểm chứng, phát hiện vi phạm nghiệp vụ hoặc contract mâu thuẫn.
- **Ranh giới file**: Duy trì và bổ sung test cases trong `tests/invariants/`. **Không được sửa assertion để làm cho test pass**.

---

## 2. Quy trình Thực thi Task (Step-by-step)

Mỗi khi một Agent bắt đầu phiên làm việc:

```
[1. Đọc Hướng dẫn] ➔ CLAUDE.md & docs/state.md
       │
[2. Nhận Task] ────➔ Đọc mô tả task trong docs/tasks/active/
       │
[3. Kiểm tra Contract] ➔ docs/contract.md & docs/spec.md
       │
[4. Code & Thực thi] ─➔ Chỉ sửa các file trong phạm vi được giao
       │
[5. Chạy Invariant Tests] ➔ python3 -m unittest discover -s tests/invariants
       │
       ├─► (Nếu FAIL) ──➔ Sửa lại code nghiệp vụ, chạy lại test
       ▼
[6. Báo cáo Team Lead] ➔ Tạo báo cáo nghiệm thu tại docs/tasks/completed/
```

---

## 3. Ranh giới sửa đổi File (File Ownership Rules)

| Thư mục / File | Ai được phép sửa? | Ghi chú |
|---|---|---|
| `docs/state.md` | **Chỉ Team Lead** | Teammate không được tự ý sửa |
| `docs/contract.md`, `spec.md` | **Team Lead** | Cần thảo luận trước khi sửa |
| `tests/invariants/*` | **QA Agent / Team Lead** | Invariant Rules là bất biến |
| `services/admission/*` | **Admission Team Agent** | Trách nhiệm riêng của Service |
| `services/student/*` | **Student Team Agent** | Trách nhiệm riêng của Service |
| `services/learning/*` | **Learning Team Agent** | Trách nhiệm riêng của Service |
| `services/finance/*` | **Finance Team Agent** | Trách nhiệm riêng của Service |
| `shared/*` | **UI/UX Lead & Team Lead** | Thành phần dùng chung toàn hệ thống |
