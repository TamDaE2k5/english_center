# Danh mục Agent Skills Dự án (Workspace Skills Catalog)

Thư mục này chứa 5 **Agent Skills** chuyên sâu được xây dựng riêng cho kiến trúc **Microservices Polyglot Monorepo** của Trung tâm Anh ngữ. 

Các AI Agent (Antigravity, Claude Code, Gemini CLI, Cursor, GitHub Copilot) khi làm việc trong workspace sẽ tự động nhận diện và kích hoạt các quy trình chuẩn này:

---

## 1. Bản đồ Kỹ năng (Skills Map)

| Skill Name | Phạm vi Công nghệ | Mô tả & Khi nào sử dụng |
|---|---|---|
| **[spring-boot-java-expert](./skills/spring-boot-java-expert/SKILL.md)** | Java 21, Spring Boot 3.3, JPA, Spring Security | Dành cho **Admission & Identity Service**. Quy chuẩn 3 tầng (Controller-Service-Repository), `@RestControllerAdvice`, DTO validation, Snake_case Jackson, và bảo đảm tính nguyên tử `@Transactional`. |
| **[fastapi-python-expert](./skills/fastapi-python-expert/SKILL.md)** | Python 3.11, FastAPI, Pydantic v2, SQLAlchemy | Dành cho **Student, Learning, Finance Services**. Cấu trúc `routers/`, `services/`, `models/`, `schemas/`, bắt lỗi toàn cục JSON chuẩn, và phòng chống SQL Injection. |
| **[vanilla-web-expert](./skills/vanilla-web-expert/SKILL.md)** | Vanilla HTML5, CSS3, ES6+ JS | Dành cho toàn bộ **Frontend** (`services/*/frontend/` và `shared/`). Quy chuẩn CSS variables token, gọi API qua `/shared/js/api.js`, xử lý token qua `/shared/js/auth.js`, và Nginx Hot-Reload. |
| **[postgres-schema-pro](./skills/postgres-schema-pro/SKILL.md)** | PostgreSQL 16 | Thiết kế và vận hành **4 Schema độc lập** (`admission`, `student`, `learning`, `finance`). Quy tắc bất biến: **ZERO Cross-Schema SQL JOINs**, thiết kế index và ACID transactions. |
| **[invariant-testing-pro](./skills/invariant-testing-pro/SKILL.md)** | Python unittest Quality Gate | Hướng dẫn chạy và bảo vệ bộ **15 Invariant Tests** tại `tests/invariants/`. Quy tắc vàng: **Không bao giờ sửa assertion của test** để lách lỗi. |

---

## 2. Cấu trúc Tiêu chuẩn của mỗi Skill

Tuân thủ đặc tả kỹ thuật của [Antigravity Customization System](https://antigravity.google):

```text
skills/<skill-name>/
└── SKILL.md       # Bắt buộc: Tệp chỉ dẫn cốt lõi chứa YAML frontmatter (name, description)
```

## 3. Cách Kích hoạt

- **Tự động**: Khi bạn yêu cầu Agent code backend Spring Boot, FastAPI hoặc sửa giao diện web, Agent sẽ tự động nạp (progressive disclosure) hướng dẫn từ skill tương ứng.
- **Thủ công**: Bạn có thể nhắc trực tiếp: *"Hãy dùng skill `spring-boot-java-expert` để viết Controller này nhé"*.
