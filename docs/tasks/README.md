# Hướng dẫn Quản lý Task & Phối hợp

Mọi nhiệm vụ trong dự án được tổ chức theo quy chuẩn:
- `docs/tasks/backlog.md`: Danh sách toàn bộ task của Sprint/Wave.
- `docs/tasks/active/`: Các file mô tả chi tiết từng task đang được Agent triển khai.
- `docs/tasks/completed/`: Báo cáo hoàn thành và bằng chứng nghiệm thu từ Agent.

Quy tắc:
1. Mỗi Agent chỉ nhận 1 task tại một thời điểm.
2. Không bắt đầu task khi dependency chưa hoàn thành.
3. Chạy toàn bộ Invariant Tests trước khi tạo báo cáo hoàn thành.
