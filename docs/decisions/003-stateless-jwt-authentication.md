# ADR 003: Chiến lược Xác thực JWT Stateless dùng Shared Secret

- **Trạng thái**: APPROVED
- **Ngày quyết định**: 2026-10-15
- **Người phê duyệt**: Project Lead & Team Architect

## Bối cảnh (Context)
Hệ thống microservices cần cơ chế xác thực người dùng mà không cần mỗi request đều phải gọi chéo sang Auth service (tránh nghẽn mạng và phụ thuộc runtime).

## Quyết định (Decision)
- **Admission Service** chịu trách nhiệm cấp phát JWT token tại `/auth/login` và `/auth/register`.
- Tất cả các service cùng chia sẻ một biến môi trường bí mật `JWT_SECRET` (định nghĩa trong `.env`).
- Mỗi service tự giải mã và kiểm tra chữ ký token JWT độc lập (Stateless Token Verification).

## Hệ quả (Consequences)
- Không có single point of failure khi verify token ở các service.
- Tốc độ xử lý nhanh. Mọi service đều nhận diện được `user_id`, `email`, và `role` từ claims của JWT.
