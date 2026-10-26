# ADR 001: Lựa chọn Kiến trúc Microservices Polyglot Monorepo

- **Trạng thái**: APPROVED
- **Ngày quyết định**: 2026-10-15
- **Người phê duyệt**: Project Lead & Team Architect

## Bối cảnh (Context)
Dự án được phát triển bởi nhóm 9 thành viên, trong đó một thành viên có kinh nghiệm sâu về Java Spring Boot, các thành viên khác thành thạo Python FastAPI. Hệ thống có 4 phân hệ độc lập: Admission, Student, Learning, Finance. Thời gian phát triển MVP là 2-3 tuần.

## Vấn đề cần giải quyết (Problem)
Cần kiến trúc cho phép các thành viên làm việc song song, tận dụng tối đa kỹ năng sẵn có mà không gây xung đột dependency hoặc cản trở tiến độ của nhau.

## Các phương án đã cân nhắc (Options Considered)
1. **Toàn bộ Java Spring Boot**: Thành viên khác mất thời gian học Spring Boot, nguy cơ trễ hạn MVP.
2. **Toàn bộ Python FastAPI**: Người biết Spring Boot không tận dụng được thế mạnh.
3. **Polyglot Monorepo (Spring Boot + FastAPI trong cùng 1 Git Repo)**: Mỗi service là 1 container độc lập, chung 1 repo dễ quản lý.

## Quyết định (Decision)
Lựa chọn phương án 3: **Microservices Polyglot Monorepo**.
- Admission Service: Java Spring Boot 3.3.4 (quản lý Auth, Course, Class).
- Student, Learning, Finance Services: Python FastAPI.
- Tất cả đóng gói container và chạy qua `docker-compose.yml`.

## Hệ quả (Consequences)
- Tích cực: Tốc độ phát triển tối đa, độc lập công nghệ, dễ dàng kiểm thử từng module.
- Tiêu cực: Cần thống nhất quy ước API (`snake_case`) và ranh giới schema dữ liệu để tránh không tương thích.
