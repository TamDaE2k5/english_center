# Task Backlog — Wave 1 (Sprint 1)

> Danh sách các nhiệm vụ kỹ thuật cho Wave 1 đã được phân tích và sẵn sàng triển khai.

| Task ID | Module | Tên nhiệm vụ | Mức ưu tiên | Trạng thái | Agent phụ trách |
|---|---|---|---|---|---|
| `TASK-W1-ADM-01` | Admission | Hiện thực hóa kiểm tra sĩ số lớp khi duyệt đơn đăng ký (`INV-ADM-01`) | P0 | READY | Backend Agent A |
| `TASK-W1-STU-01` | Student | Ngăn chặn trùng lặp điểm danh trong ngày (`INV-STU-01`) | P1 | READY | Backend Agent B |
| `TASK-W1-LRN-01` | Learning | Ràng buộc điểm số hợp lệ `0 <= score <= max_score` (`INV-LRN-01`) | P0 | READY | Backend Agent C |
| `TASK-W1-FIN-01` | Finance | Cơ chế Idempotency thanh toán chống lặp `transaction_ref` (`INV-FIN-02`) | P0 | READY | Backend Agent D |
