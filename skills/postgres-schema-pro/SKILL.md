---
name: postgres-schema-pro
description: >-
  Use this skill whenever designing, modifying, migrating, or optimizing PostgreSQL 16 database schemas,
  tables, indexes, constraints, and seed scripts across the 4 microservices schemas
  (admission, student, learning, finance).
---

# PostgreSQL 16 Multi-Schema Microservices Engineering Skill

## 1. Nguyên tắc Ranh giới Schema (Database Isolation)

Dự án áp dụng mô hình **Shared Database, Multi-Schema Isolation**:
1 cơ sở dữ liệu vật lý chia thành **4 Schema độc lập logic**, mỗi schema hoàn toàn thuộc quyền sở hữu của một microservice duy nhất:

```
                  PostgreSQL 16 Database
 ┌─────────────────┬─────────────────┬─────────────────┬─────────────────┐
 │    admission    │     student     │    learning     │     finance     │
 ├─────────────────┼─────────────────┼─────────────────┼─────────────────┤
 │ users           │ students        │ materials       │ tuition_fees    │
 │ refresh_tokens  │ attendance      │ grades          │ invoices        │
 │ courses         │ ...             │ ...             │ payments        │
 │ classes         │                 │                 │ ...             │
 │ enrollments     │                 │                 │                 │
 │ test_sets       │                 │                 │                 │
 └─────────────────┴─────────────────┴─────────────────┴─────────────────┘
```

### Quy tắc Bất biến:
1. **Tuyệt đối không Cross-Schema Foreign Keys (Ràng buộc Khóa ngoại Chéo Schema)**:
   - Trong DDL, không bao giờ viết: `FOREIGN KEY (student_id) REFERENCES student.students(id)` từ bên trong schema `admission` (hoặc ngược lại).
   - **Lý do**: Microservices cần có khả năng tách database vật lý độc lập trong tương lai mà không bị gãy khóa ngoại.
   - **Giải pháp**: Chỉ lưu `student_id INTEGER` dưới dạng **Khóa logic (Logical Key)**. Việc toàn vẹn dữ liệu được bảo đảm qua tầng ứng dụng (Service Layer) và REST API.
2. **Tuyệt đối không Cross-Schema SQL JOINs**:
   - Backend của `admission-service` chỉ được kết nối và truy vấn các bảng có tiền tố `admission.*`.
   - Cấm viết: `SELECT * FROM admission.classes c JOIN student.students s ...`.

---

## 2. Quy chuẩn Thiết kế Bảng & Kiểu Dữ liệu

1. **Quy ước đặt tên**: 100% tên bảng và tên cột dùng **`snake_case`**, số nhiều cho tên bảng (ví dụ: `courses`, `classes`, `invoices`).
2. **Khóa chính**: `id SERIAL PRIMARY KEY` (hoặc `BIGSERIAL` cho bảng log lớn).
3. **Thời gian**:
   - Luôn sử dụng `TIMESTAMPTZ` (hoặc `TIMESTAMP WITH TIME ZONE`).
   - Mặc định: `created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP`.
4. **Tiền tệ & Điểm số**:
   - Tuyệt đối không dùng `FLOAT` hay `DOUBLE` để lưu tiền.
   - Bắt buộc dùng `NUMERIC(12, 2)` cho tiền tệ và `NUMERIC(5, 2)` cho điểm số thi cử.
5. **Ràng buộc Tính đúng đắn (CHECK Constraints)**:
   - Khóa học: `CHECK (tuition_amount >= 0)`
   - Sĩ số: `CHECK (min_students > 0 AND max_students >= min_students)`
   - Trạng thái: `CHECK (status IN ('OPEN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'))`

---

## 3. Chiến lược Đánh Chỉ mục Hiệu năng (Indexing)

Tạo chỉ mục có chủ đích để hỗ trợ các câu truy vấn điều kiện thường xuyên:

```sql
-- 1. Index hỗ trợ thuật toán quét xung đột lịch lớp học
CREATE INDEX idx_classes_schedule 
ON admission.classes (room_id, teacher_id, day_pattern, start_date, end_date);

-- 2. Index hỗ trợ Job định kỳ quét đơn giữ chỗ 48h quá hạn
CREATE INDEX idx_enrollments_status_hold 
ON admission.enrollments (class_id, status, hold_expires_at);

-- 3. Index hỗ trợ truy vấn nhanh audit log bảo mật
CREATE INDEX idx_audit_user_created 
ON admission.auth_audit_logs (user_id, created_at);

-- 4. Index chống trùng lặp điểm danh của học viên trong 1 ngày (INV-STU-01)
CREATE UNIQUE INDEX idx_unique_daily_attendance 
ON student.attendance (class_id, student_id, session_date);

-- 5. Index chống ghi nhận trùng mã giao dịch thanh toán (INV-FIN-02)
CREATE UNIQUE INDEX idx_unique_transaction_ref 
ON finance.payments (transaction_ref) 
WHERE transaction_ref IS NOT NULL;
```

---

## 4. Giao dịch ACID & Bảo vệ Invariant

Khi thực hiện thao tác duyệt đơn nhập học (`APPROVED`), bắt buộc thực hiện trong Transaction có kiểm tra khóa dòng:

```sql
BEGIN;

-- Khóa dòng lớp học để tránh race-condition vượt sĩ số (INV-ADM-01)
SELECT id, max_students 
FROM admission.classes 
WHERE id = :class_id 
FOR UPDATE;

-- Đếm số lượng học viên đã APPROVED
SELECT COUNT(*) 
FROM admission.enrollments 
WHERE class_id = :class_id AND status = 'APPROVED';

-- Nếu approved_count < max_students -> Cho phép cập nhật
UPDATE admission.enrollments 
SET status = 'APPROVED', approved_at = CURRENT_TIMESTAMP 
WHERE id = :enrollment_id;

COMMIT;
```

---

## 5. Quy chuẩn Viết Script Khởi tạo (`scripts/init-db.sql`)

- Luôn dùng `CREATE TABLE IF NOT EXISTS` để script có tính **Idempotent** (chạy lại nhiều lần không lỗi).
- Dữ liệu seed mẫu luôn kèm `ON CONFLICT (code) DO NOTHING` hoặc `ON CONFLICT (email) DO NOTHING`.
