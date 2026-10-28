---
name: invariant-testing-pro
description: >-
  Use this skill whenever running, maintaining, debugging, or authoring Invariant Tests in Python
  under tests/invariants/. Enforces the non-negotiable quality gate, invariant rules preservation,
  and contract compliance across microservices.
---

# Invariant Testing & Quality Gate Engineering Skill

## 1. Bản chất của Invariant Tests trong Dự án

Trong kiến trúc Polyglot Monorepo, **Invariant Tests** (`tests/invariants/`) là hàng rào phòng thủ tối cao (Quality Gate).
Các bài test này kiểm chứng các **Quy tắc Nghiệp vụ Bất biến (Core Invariants)** mà bất kể công nghệ nào (Java, Python, JS) hay thành viên nào code cũng **không được phép vi phạm**.

```
[Mã nguồn Backend/Frontend] ──> [Commit/Review] ──> [INVARIANT QUALITY GATE] ──> [Pass / Merge]
                                                              │
                                                              ▼
                                                   15/15 Tests must pass (OK)
```

### Ranh giới Thép (The Golden Rule):
> **TUYỆT ĐỐI KHÔNG SỬA ĐỔI ASSERTION CỦA TEST KHI TEST BỊ FAIL.**  
> Khi test fail, điều đó chứng minh mã nguồn nghiệp vụ đang có bug hoặc vi phạm đặc tả.  
> Lập trình viên/Agent bắt buộc phải tìm nguyên nhân gốc rễ (root cause) trong mã nguồn và sửa code nghiệp vụ cho đến khi test pass.

---

## 2. Lệnh Điều khiển Kiểm thử

Chạy toàn bộ 15 tests với output chi tiết (verbose):
```bash
python3 -m unittest discover -s tests/invariants -p "test_*.py" -v
```

Chạy riêng kiểm thử của một module cụ thể:
```bash
# Kiểm thử ranh giới Tuyển sinh (Admission)
python3 -m unittest tests/invariants/test_admission_invariants.py -v

# Kiểm thử tính toàn vẹn Tài chính (Finance)
python3 -m unittest tests/invariants/test_finance_invariants.py -v

# Kiểm thử bảo mật (Security & Isolation)
python3 -m unittest tests/invariants/test_security_invariants.py -v
```

---

## 3. Bản đồ 15 Quy tắc Bất biến Cốt lõi

| Test File | Mã Invariant | Nội dung Quy tắc Bất biến |
|---|---|---|
| `test_admission_invariants.py` | `INV-ADM-01` | Không bao giờ duyệt đơn đăng ký nếu lớp đã đủ sĩ số tối đa (`max_students`). |
| | `INV-ADM-02` | Mã khóa học (`code`) phải là duy nhất trên toàn hệ thống. |
| | `INV-ADM-03` | Không cho phép chuyển trực tiếp đơn từ trạng thái `CANCELLED` sang `APPROVED`. |
| `test_finance_invariants.py` | `INV-FIN-01` | Số tiền thanh toán và giá trị hóa đơn bắt buộc phải `> 0`. |
| | `INV-FIN-02` | Cơ chế Idempotency: Không ghi nhận trùng lặp cùng một `transaction_ref`. |
| | `INV-FIN-03` | Hóa đơn chỉ chuyển sang `PAID` khi tổng số tiền thanh toán thực tế `>=` số tiền hóa đơn. |
| `test_learning_invariants.py` | `INV-LRN-01` | Điểm thi bắt buộc nằm trong khoảng: `0.0 <= score <= max_score`. |
| | `INV-LRN-02` | Ranh giới dữ liệu điểm: Học viên chỉ xem được điểm thi của chính mình. |
| `test_security_invariants.py` | `INV-SEC-01` | Hồ sơ học viên: Học viên chỉ được xem hồ sơ của bản thân. |
| | `INV-SEC-02` | Ranh giới giáo viên: Giáo viên chỉ được xem và sửa điểm lớp mình phụ trách. |
| | `INV-SEC-03` | An toàn git: Không commit password hoặc JWT secret thật vào repository. |
| `test_student_invariants.py` | `INV-STU-01` | Mỗi học viên chỉ có tối đa 1 lượt điểm danh trong 1 buổi học (Chống trùng lặp). |
| | `INV-STU-02` | Trạng thái học vụ của học viên phải nằm trong danh mục chuẩn (`STUDYING`, `RESERVED`, `DROPPED`, `GRADUATED`). |
| `test_system_invariants.py` | `INV-SYS-01` | Toàn bộ payload request/response và API parameter phải tuân thủ chuẩn `snake_case`. |
| | `INV-SYS-02` | Script `scripts/init-db.sql` phải khởi tạo 4 schema riêng biệt (`admission`, `student`, `learning`, `finance`). |

---

## 4. Hướng dẫn Viết Test Mới Đúng Chuẩn

1. **Tốc độ thực thi cực nhanh**: Toàn bộ bộ test phải chạy xong trong dưới **0.1 giây** (hiện tại là ~0.003s). Không phụ thuộc mạng ngoài.
2. **Deterministic (Tất định)**: Chạy 100 lần phải cùng ra 1 kết quả, không phụ thuộc vào thứ tự chạy.
3. **Cấu trúc mẫu cho một Invariant Test**:
   ```python
   import unittest

   class InvariantExampleTest(unittest.TestCase):
       def test_inv_custom_rule(self):
           """INV-XXX: Mô tả ngắn gọn quy tắc bất biến."""
           # Arrange: Chuẩn bị dữ liệu kiểm thử
           max_allowed = 25
           current_enrolled = 25
           
           # Act: Gọi hàm nghiệp vụ hoặc kiểm tra logic
           def enroll_new_student(current, max_cap):
               if current >= max_cap:
                   raise ValueError("CLASS_FULL")
               return current + 1
           
           # Assert: Xác minh bắt buộc ném lỗi khi vi phạm
           with self.assertRaises(ValueError) as ctx:
               enroll_new_student(current_enrolled, max_allowed)
           
           self.assertIn("CLASS_FULL", str(ctx.exception))
   ```

---

## 5. Quy trình Xử lý khi Test Bị Fail (Debugging Checklist)

1. Đọc tên test bị fail và thông báo lỗi assertion.
2. Mở file mã nguồn nghiệp vụ tương ứng (Spring Boot hoặc FastAPI).
3. Tìm điều kiện kiểm tra (validation condition) bị thiếu hoặc sai sót.
4. Sửa lại code nghiệp vụ, bổ sung comment tiếng Việt giải thích lý do sửa.
5. Chạy lại `python3 -m unittest discover -s tests/invariants -p "test_*.py" -v`.
6. Chỉ khi thấy chữ **`OK`** màu xanh thì mới được bàn giao task.
