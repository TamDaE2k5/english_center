# ADR 002: Lựa chọn Nginx Gateway & Static Web Mounting

- **Trạng thái**: APPROVED
- **Ngày quyết định**: 2026-10-15
- **Người phê duyệt**: Project Lead & Team Architect

## Bối cảnh (Context)
Frontend của 4 module được viết bằng HTML, JavaScript thuần và CSS, không sử dụng build tools (Vite, Webpack, React build). Cần cơ chế phục vụ web và proxy API đơn giản, hỗ trợ xem thay đổi ngay lập tức.

## Quyết định (Decision)
Sử dụng container **Nginx (Alpine)** đóng vai trò:
1. Reverse Proxy định tuyến `/api/admission/*`, `/api/student/*`, `/api/learning/*`, `/api/finance/*` và `/auth/*` tới container tương ứng.
2. Mount trực tiếp thư mục `services/{module}/frontend/` và `shared/` vào Nginx document root.

## Hệ quả (Consequences)
- Thành viên và AI Agent khi chỉnh sửa file HTML/JS/CSS chỉ cần bấm F5 trình duyệt là thấy ngay kết quả (Zero build time, instant reload).
- Nginx đóng vai trò Single Entry Point (Port 8080) duy nhất, tránh vấn đề CORS trong môi trường local.
