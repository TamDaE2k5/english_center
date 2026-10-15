# Kiến trúc hệ thống - Trung tâm Anh ngữ

> Tài liệu nền tảng — mọi thành viên cần đọc trước khi code.

## 1. Tổng quan kiến trúc

```
                    ┌──────────────────────────┐
                    │      Trình duyệt          │
                    │   (HTML + JS + CSS)        │
                    └────────────┬───────────────┘
                                 │
                    ┌────────────▼───────────────┐
                    │    Nginx API Gateway        │
                    │        Port 8080            │
                    │                             │
                    │  /shared/*  → static files  │
                    │  /auth/*    → auth service  │
                    │  /api/admission/* → :8081   │
                    │  /api/student/*   → :8082   │
                    │  /api/learning/*  → :8083   │
                    │  /api/finance/*   → :8084   │
                    │  /admission/*     → :8081   │
                    │  /student/*       → :8082   │
                    │  /learning/*      → :8083   │
                    │  /finance/*       → :8084   │
                    └──┬─────┬──────┬──────┬─────┘
                       │     │      │      │
              ┌────────▼┐ ┌─▼────┐┌▼─────┐┌▼──────┐
              │Admission│ │Stud- ││Learn-││Finance│
              │ :8081   │ │ent   ││ing   ││ :8084 │
              │         │ │:8082 ││:8083 ││       │
              └────┬────┘ └──┬───┘└──┬───┘└───┬───┘
                   │         │       │        │
              ┌────▼─────────▼───────▼────────▼───┐
              │         PostgreSQL :5432            │
              │                                    │
              │  schema: admission                 │
              │  schema: student                   │
              │  schema: learning                  │
              │  schema: finance                   │
              └────────────────────────────────────┘
```

**Nguyên tắc chính:**
- Mỗi team sở hữu toàn bộ module (backend + frontend) của mình.
- Backend polyglot: team tự chọn **Java Spring Boot** hoặc **Python FastAPI**.
- Frontend: **HTML + JavaScript + CSS thuần** — không framework.
- Tất cả service cùng dùng **PostgreSQL**, mỗi service sở hữu 1 schema riêng.
- **Nginx** làm API Gateway, phân phối request và phục vụ shared assets.
- Giao tiếp giữa các service: **REST API qua HTTP** (đơn giản, không message queue).

---

## 2. Tech Stack

| Thành phần | Công nghệ | Lý do |
|---|---|---|
| Backend | Java Spring Boot **hoặc** Python FastAPI (team tự chọn) | Tận dụng kinh nghiệm có sẵn, tạo demo nhanh |
| Frontend | HTML + JavaScript + CSS thuần | Đơn giản, không learning curve thêm |
| Database | PostgreSQL 16 | Mạnh, hỗ trợ tốt cả Spring Boot (JPA) và FastAPI (SQLAlchemy) |
| API Gateway | Nginx | Nhẹ, cấu hình đơn giản, phục vụ static files |
| Container | Docker + docker-compose | 1 lệnh chạy toàn bộ hệ thống |
| Auth | JWT (JSON Web Token) | Stateless, dễ chia sẻ giữa các service |

---

## 3. Port Convention

| Service | Port | Mô tả |
|---|---|---|
| Nginx Gateway | **8080** | Điểm vào duy nhất cho browser |
| Admission Service | **8081** | Khóa học, lớp học, tuyển sinh |
| Student Service | **8082** | Học viên, điểm danh, bảo lưu |
| Learning Service | **8083** | Học liệu, điểm số, kết quả |
| Finance Service | **8084** | Học phí, thanh toán, công nợ |
| PostgreSQL | **5432** | Cơ sở dữ liệu |

> **Quy tắc:** Không service nào được dùng port của service khác. Browser chỉ truy cập qua port 8080 (Nginx).

---

## 4. Cấu trúc Monorepo

```
english-center/
├── docker-compose.yml          # Chạy toàn bộ hệ thống
├── .env.example                # Biến môi trường mẫu
├── .gitignore
├── README.md
│
├── docs/                       # Tài liệu dùng chung
│   ├── architecture.md         # (file này)
│   ├── api-conventions.md      # Quy ước API
│   ├── design-system.md        # Thiết kế giao diện
│   └── git-workflow.md         # Quy trình Git
│
├── shared/                     # Assets dùng chung cho tất cả frontend
│   ├── css/
│   │   ├── variables.css       # Design tokens (màu, font, spacing)
│   │   ├── base.css            # Reset, typography
│   │   ├── components.css      # Button, card, table, form, modal
│   │   └── layout.css          # Sidebar, header, footer
│   ├── js/
│   │   ├── auth.js             # JWT login/logout, token management
│   │   ├── api.js              # HTTP client wrapper (tự gắn Authorization header)
│   │   └── layout.js           # Render sidebar + header động
│   └── assets/
│       └── logo.svg
│
├── services/
│   ├── admission/              # Team Admission sở hữu
│   │   ├── Dockerfile
│   │   ├── frontend/           # HTML pages
│   │   │   ├── courses.html
│   │   │   ├── classes.html
│   │   │   └── enrollment.html
│   │   └── backend/            # Spring Boot hoặc FastAPI
│   │       └── ...
│   │
│   ├── student/                # Team Student sở hữu
│   │   ├── Dockerfile
│   │   ├── frontend/
│   │   └── backend/
│   │
│   ├── learning/               # Team Learning sở hữu
│   │   ├── Dockerfile
│   │   ├── frontend/
│   │   └── backend/
│   │
│   └── finance/                # Team Finance sở hữu
│       ├── Dockerfile
│       ├── frontend/
│       └── backend/
│
├── gateway/
│   └── nginx.conf              # Cấu hình routing
│
└── scripts/
    ├── init-db.sql             # Tạo schemas + users
    └── seed-data.sql           # Dữ liệu mẫu
```

### Cách shared assets hoạt động

Nginx mount thư mục `shared/` và phục vụ tại đường dẫn `/shared/*`. Mỗi trang HTML chỉ cần include:

```html
<!-- Trong mọi trang HTML của mọi module -->
<link rel="stylesheet" href="/shared/css/variables.css">
<link rel="stylesheet" href="/shared/css/base.css">
<link rel="stylesheet" href="/shared/css/components.css">
<link rel="stylesheet" href="/shared/css/layout.css">

<!-- JS cuối body -->
<script src="/shared/js/auth.js"></script>
<script src="/shared/js/api.js"></script>
<script src="/shared/js/layout.js"></script>
```

Khi `layout.js` chạy, nó sẽ tự render sidebar và header. Team chỉ cần code phần **content** bên trong.

---

## 5. Quyền sở hữu dữ liệu (Data Ownership)

Mỗi service **chỉ đọc/ghi dữ liệu trong schema của mình**. Khi cần dữ liệu từ service khác → gọi REST API.

| Dữ liệu | Service sở hữu | Ai cần đọc |
|---|---|---|
| Course, Class, Schedule | **Admission** | Student (xem lớp), Learning (biết lớp nào) |
| Enrollment (đăng ký học) | **Admission** | Finance (kiểm tra để tạo hóa đơn) |
| Student Profile | **Student** | Admission (xem thông tin khi xét tuyển) |
| Attendance (điểm danh) | **Student** | Learning (tham khảo khi chấm điểm) |
| Grade, Result | **Learning** | Student (xem kết quả) |
| Study Material | **Learning** | — |
| Payment, Invoice | **Finance** | Admission (kiểm tra đã thanh toán chưa) |
| Tuition Fee Config | **Finance** | Admission (hiển thị học phí khi đăng ký) |

### Quy tắc cross-service

1. **Không truy cập trực tiếp DB của service khác.** Luôn gọi qua API.
2. **Lưu ID tham chiếu tối thiểu.** Ví dụ: Finance lưu `student_id` và `enrollment_id`, nhưng không lưu tên/email học viên.
3. **Chấp nhận eventual consistency** cho demo MVP. Ví dụ: nếu Student Service down, Finance vẫn hiển thị `student_id` nhưng không có tên.

---

## 6. Xác thực & Phân quyền (Auth)

### Vai trò người dùng

| Vai trò | Mã role | Quyền chính |
|---|---|---|
| Quản trị viên | `ADMIN` | Toàn quyền, quản lý user |
| Nhân viên học vụ | `STAFF` | Quản lý khóa học, đăng ký, học viên |
| Giáo viên | `TEACHER` | Điểm danh, chấm điểm, quản lý học liệu |
| Học viên | `STUDENT` | Xem khóa học, đăng ký, xem điểm, thanh toán |

### Luồng xác thực

```
1. User POST /auth/login → { email, password }
2. Gateway forward đến Auth endpoint → validate → trả JWT token
3. Browser lưu token vào localStorage
4. Mọi request sau đều gửi: Authorization: Bearer <token>
5. Mỗi service tự verify JWT bằng shared secret (KHÔNG gọi lại auth service)
```

### JWT Payload

```json
{
  "sub": "user-uuid-here",
  "email": "user@example.com",
  "role": "STAFF",
  "name": "Nguyễn Văn A",
  "iat": 1697000000,
  "exp": 1697086400
}
```

### Auth service nằm ở đâu?

Đề xuất: **Tích hợp vào Admission Service** (endpoint `/auth/login`, `/auth/register`).
Lý do: Tránh tạo thêm 1 service riêng chỉ cho auth, giảm phức tạp.
Admission quản lý user/account, nên auth nằm ở đây hợp lý nhất.

### Shared JWT Secret

Tất cả service cùng đọc `JWT_SECRET` từ biến môi trường (trong `.env`). Middleware verify token giống nhau cho mọi service.

---

## 7. Cách giao tiếp giữa các service

### Trong MVP: HTTP REST đơn giản

```
Ví dụ: Finance cần biết tên học viên

Finance Service → GET http://student-service:8082/api/student/students/{id}
                ← { "id": "...", "name": "Nguyễn Văn B", "email": "..." }
```

Trong docker-compose, các service gọi nhau bằng tên container (e.g., `admission-service:8081`).

### Không dùng trong MVP
- ❌ Message queue (RabbitMQ, Kafka)
- ❌ Service discovery (Eureka, Consul)
- ❌ API Gateway phức tạp (Spring Cloud Gateway, Kong)
- ❌ gRPC

Lý do: 2-3 tuần không đủ thời gian, và demo MVP không cần scale.

---

## 8. Phạm vi MVP (2-3 tuần)

### Luồng nghiệp vụ MVP

```
┌─────────┐     ┌──────────┐     ┌──────────┐     ┌─────────┐
│ Admin    │     │ Học viên  │     │ Giáo viên│     │ Kế toán │
│ tạo     │────▶│ đăng ký  │────▶│ điểm danh│────▶│ ghi nhận│
│ khóa học │     │ vào lớp  │     │ chấm điểm│     │ thanh   │
│ & lớp   │     │          │     │          │     │ toán    │
└─────────┘     └──────────┘     └──────────┘     └─────────┘
```

### Tuần 1: Nền tảng + CRUD cơ bản

| Ai | Làm gì |
|---|---|
| Infra (1 người) | Setup monorepo, Docker, Nginx, DB schemas, shared CSS/JS, auth |
| Admission (2 người) | CRUD khóa học, CRUD lớp học |
| Student (2 người) | CRUD hồ sơ học viên |
| Learning (2 người) | CRUD học liệu (tài liệu, bài giảng) |
| Finance (2 người) | CRUD cấu hình học phí |

### Tuần 2: Luồng nghiệp vụ chính

| Ai | Làm gì |
|---|---|
| Admission | Luồng đăng ký vào lớp (enrollment) |
| Student | Điểm danh theo buổi học |
| Learning | Nhập điểm + xem kết quả |
| Finance | Tạo hóa đơn + ghi nhận thanh toán |

### Tuần 3: Tích hợp + Demo

| Ai | Làm gì |
|---|---|
| Tất cả | Kết nối cross-service (ví dụ: kiểm tra thanh toán khi duyệt đăng ký) |
| Tất cả | Dashboard tổng quan cho Admin |
| Tất cả | Sửa bug, chỉnh UI, chuẩn bị demo |

### Tính năng **KHÔNG** trong MVP
- Bảo lưu, chuyển lớp
- Thông báo email/SMS
- Báo cáo thống kê nâng cao
- Xuất PDF
- Tìm kiếm nâng cao
- Quản lý lịch dạy giáo viên

---

## 9. Phân công đề xuất (9 người)

| Nhóm | Số người | Trách nhiệm |
|---|---|---|
| Infrastructure & Auth | 1 | Docker, Nginx, DB setup, shared assets, JWT auth |
| Admission | 2 | Backend + Frontend module Admission |
| Student | 2 | Backend + Frontend module Student |
| Learning | 2 | Backend + Frontend module Learning |
| Finance | 2 | Backend + Frontend module Finance |

> Người làm Infra nên là người hiểu Docker và có thể hỗ trợ setup cho các team khác.

---

## 10. Checklist trước khi code

- [ ] Đọc tài liệu: `architecture.md` (file này)
- [ ] Đọc tài liệu: `api-conventions.md`
- [ ] Đọc tài liệu: `design-system.md`
- [ ] Đọc tài liệu: `git-workflow.md`
- [ ] Clone repo và chạy `docker-compose up` thành công
- [ ] Hiểu module mình sở hữu những entity/API nào
- [ ] Tạo branch theo đúng convention
