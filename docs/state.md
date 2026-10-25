# Nhật ký Trạng thái Dự án (Project State Log)

> **LƯU Ý QUAN TRỌNG**: Chỉ **Team Lead** mới có quyền cập nhật file này. Teammate Agents chỉ gửi báo cáo nghiệm thu và chờ Team Lead xác nhận.

---

## 1. Thông tin tổng thể
- **Phase hiện tại**: `Phase 0 — Project Bootstrap & Technical Foundation`
- **Wave hiện tại**: `Wave 0 (Hoàn tất)` ➔ Chuẩn bị bước vào `Wave 1 (Sprint 1 Feature Implementation)`
- **Trạng thái sẵn sàng**: **`READY FOR WAVE 1`**
- **Cập nhật lần cuối**: 2026-10-25

---

## 2. Tiến độ công việc

### Các hạng mục đã hoàn thành (COMPLETED)
- [x] **Repo Survey & Architecture Baseline**: Thiết lập kiến trúc 4 Microservices Polyglot (Spring Boot + FastAPI) và Nginx Gateway.
- [x] **Project Scaffolding**: Đã tạo đầy đủ `docker-compose.yml`, `gateway/nginx.conf`, script PostgreSQL 4 schemas `scripts/init-db.sql`.
- [x] **Shared Design System**: Hoàn thiện CSS variables, components, typography, layout, auth helper, login portal.
- [x] **SSOT Documentation**:
  - `CLAUDE.md` / `AGENTS.md`: Hướng dẫn vận hành Agent.
  - `docs/spec.md`: PRD đặc tả nghiệp vụ chi tiết & Explicit Non-Goals.
  - `docs/contract.md`: Hợp đồng kỹ thuật, Domain Model, State Machine, Authorization boundaries.
  - `docs/invariants.md`: Danh mục 15 quy tắc bất biến.
  - `docs/agent-workflow.md`: Quy trình phối hợp đa Agent.
  - `docs/decisions/`: Đầy đủ 5 ADR quan trọng.
- [x] **Invariant Automated Test Suite**: Bộ kiểm thử tự động tại `tests/invariants/` độc lập với môi trường ngoài, kiểm tra 100% các quy tắc P0/P1.

### Các hạng mục tiếp theo (NEXT UP - Wave 1)
- [ ] Triển khai `TASK-W1-ADM-01`: Hoàn thiện logic kiểm tra sĩ số lớp khi duyệt Enrollment trong Admission Service.
- [ ] Triển khai `TASK-W1-STU-01`: Hoàn thiện validation ngày sinh và ràng buộc điểm danh không trùng lặp trong Student Service.
- [ ] Triển khai `TASK-W1-LRN-01`: Hoàn thiện validation thang điểm `0 <= score <= max_score` trong Learning Service.
- [ ] Triển khai `TASK-W1-FIN-01`: Hiện thực hóa cơ chế idempotency thanh toán với `transaction_ref` trong Finance Service.

---

## 3. Quyết định kỹ thuật đã duyệt (APPROVED DECISIONS)
1. **ADR-001**: Kiến trúc Microservices Polyglot Monorepo.
2. **ADR-002**: Cơ chế Nginx Gateway & Static Web Mounting (Hot-reload UI).
3. **ADR-003**: Chiến lược xác thực JWT Stateless dùng Shared Secret Key.
4. **ADR-004**: Chuẩn hóa API Naming & Data Format dạng `snake_case`.
5. **ADR-005**: Cơ chế kiểm thử Invariant Test độc lập bằng Python unittest.

---

## 4. Blockers & Rủi ro
- *Không có blocker kỹ thuật nghiêm trọng nào ngăn cản việc triển khai Wave 1.*
