# ADR 005: Chiến lược Kiểm thử Invariant Tests bằng Python unittest độc lập

- **Trạng thái**: APPROVED
- **Ngày quyết định**: 2026-10-25
- **Người phê duyệt**: Project Lead & Team Architect

## Bối cảnh (Context)
Dự án được phát triển bởi nhiều AI Agent độc lập trong môi trường hạn chế (không cài sẵn pytest, maven, npm trên host runtime). Cần một cơ chế kiểm thử quy tắc bất biến (Invariants) có thể chạy mọi lúc, không phụ thuộc kết nối mạng hoặc database ngoài.

## Quyết định (Decision)
- Xây dựng bộ Invariant Tests đặt tại thư mục `tests/invariants/` sử dụng thư viện chuẩn **`python3 -m unittest`**.
- Các test case kiểm chứng logic ràng buộc bằng database in-memory (`sqlite3`) giả lập chính xác các ràng buộc schema và state machines của hệ thống, cùng kiểm tra bảo mật tĩnh (static config checks).
- Đảm bảo lệnh `python3 -m unittest discover -s tests/invariants` luôn chạy được trực tiếp trên môi trường máy chủ.

## Hệ quả (Consequences)
- Mọi Agent có thể chạy test nhanh chóng trong < 1 giây để xác minh các quy tắc bất biến P0/P1.
- Không yêu cầu cài đặt thêm bất kỳ thư viện bên ngoài nào.
