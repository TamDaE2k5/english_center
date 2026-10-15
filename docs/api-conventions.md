# Quy ước API - Trung tâm Anh ngữ

> Tất cả service phải tuân theo quy ước này để đảm bảo frontend và cross-service calls hoạt động nhất quán.

## 1. URL Pattern

```
/api/{service}/{resource}          # Danh sách
/api/{service}/{resource}/{id}     # Chi tiết
```

### Ví dụ

| Method | URL | Mô tả |
|---|---|---|
| GET | `/api/admission/courses` | Danh sách khóa học |
| GET | `/api/admission/courses/123` | Chi tiết khóa học 123 |
| POST | `/api/admission/courses` | Tạo khóa học mới |
| PUT | `/api/admission/courses/123` | Cập nhật khóa học 123 |
| DELETE | `/api/admission/courses/123` | Xóa khóa học 123 |
| GET | `/api/student/students?class_id=5` | Danh sách học viên của lớp 5 |

### Quy tắc đặt tên

- Resource name: **danh từ, số nhiều, tiếng Anh, snake_case**
  - ✅ `/api/admission/courses`
  - ✅ `/api/finance/payment_records`
  - ❌ `/api/admission/getCourse`
  - ❌ `/api/finance/paymentRecord`
- Không dùng động từ trong URL. Hành động thể hiện qua HTTP method.
- Nested resources tối đa 2 cấp: `/api/admission/courses/123/classes`

---

## 2. Response Format

### Thành công (Success)

```json
{
  "success": true,
  "data": { ... },
  "message": "Tạo khóa học thành công"
}
```

### Thành công - Danh sách có phân trang

```json
{
  "success": true,
  "data": [
    { "id": 1, "name": "IELTS Foundation" },
    { "id": 2, "name": "TOEIC 600+" }
  ],
  "pagination": {
    "page": 1,
    "size": 20,
    "total_items": 45,
    "total_pages": 3
  }
}
```

### Lỗi (Error)

```json
{
  "success": false,
  "error": {
    "code": "COURSE_NOT_FOUND",
    "message": "Không tìm thấy khóa học với ID này"
  }
}
```

---

## 3. HTTP Status Codes

| Code | Khi nào dùng |
|---|---|
| `200 OK` | GET, PUT thành công |
| `201 Created` | POST tạo mới thành công |
| `204 No Content` | DELETE thành công |
| `400 Bad Request` | Dữ liệu đầu vào không hợp lệ |
| `401 Unauthorized` | Chưa đăng nhập / token hết hạn |
| `403 Forbidden` | Không có quyền truy cập |
| `404 Not Found` | Resource không tồn tại |
| `409 Conflict` | Xung đột (ví dụ: email đã tồn tại) |
| `500 Internal Server Error` | Lỗi server |

---

## 4. Query Parameters

### Phân trang

```
GET /api/admission/courses?page=1&size=20
```

- `page`: Trang hiện tại (bắt đầu từ **1**)
- `size`: Số item mỗi trang (mặc định **20**, tối đa **100**)

### Sắp xếp

```
GET /api/admission/courses?sort=created_at&order=desc
```

- `sort`: Tên field cần sắp xếp
- `order`: `asc` hoặc `desc` (mặc định `asc`)

### Tìm kiếm / Lọc

```
GET /api/admission/courses?search=IELTS&status=active
```

- `search`: Tìm kiếm full-text (tùy service quyết định tìm ở field nào)
- Các field khác: filter theo giá trị cụ thể

---

## 5. Naming Convention cho JSON Fields

**Sử dụng `snake_case` cho tất cả JSON field names.**

```json
{
  "id": 1,
  "course_name": "IELTS Foundation",
  "start_date": "2024-03-01",
  "max_students": 30,
  "created_at": "2024-01-15T10:30:00Z",
  "is_active": true
}
```

Lý do:
- FastAPI (Python) mặc định dùng snake_case
- Spring Boot dễ dàng cấu hình snake_case qua `@JsonNaming(SnakeCaseStrategy.class)` hoặc property `spring.jackson.property-naming-strategy=SNAKE_CASE`
- Frontend JavaScript xử lý snake_case tốt

---

## 6. Date & Time Format

- **ISO 8601**: `2024-03-15T14:30:00Z` (UTC) hoặc `2024-03-15T21:30:00+07:00` (Vietnam timezone)
- **Date only**: `2024-03-15`
- **Time only**: `14:30:00`

---

## 7. ID Format

Sử dụng **auto-increment integer** cho đơn giản.

```json
{ "id": 1, "student_id": 42, "course_id": 7 }
```

> UUID cũng chấp nhận nếu team nào thích, nhưng integer đơn giản hơn cho demo.

---

## 8. Authentication Header

Mọi request (trừ `/auth/login` và `/auth/register`) phải gửi:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

Nếu thiếu hoặc token không hợp lệ → trả `401 Unauthorized`.

---

## 9. Cross-Service API Calls

Khi service A cần gọi service B:

```
# Trong docker-compose network
GET http://student-service:8082/api/student/students/42

# Header: vẫn cần Authorization (forward token từ request gốc)
```

**Quy tắc:**
- Forward JWT token từ request gốc để service B biết ai đang gọi
- Timeout: 5 giây cho cross-service calls
- Nếu service B không phản hồi → trả lỗi rõ ràng cho frontend, KHÔNG treo request

---

## 10. Error Codes theo Service

Mỗi service định nghĩa error codes với prefix riêng:

| Service | Prefix | Ví dụ |
|---|---|---|
| Admission | `ADM_` | `ADM_COURSE_NOT_FOUND`, `ADM_CLASS_FULL` |
| Student | `STU_` | `STU_STUDENT_NOT_FOUND`, `STU_ALREADY_ENROLLED` |
| Learning | `LRN_` | `LRN_GRADE_INVALID`, `LRN_MATERIAL_NOT_FOUND` |
| Finance | `FIN_` | `FIN_PAYMENT_FAILED`, `FIN_INVOICE_NOT_FOUND` |
| Auth | `AUTH_` | `AUTH_INVALID_CREDENTIALS`, `AUTH_TOKEN_EXPIRED` |

---

## 11. Template API cho mỗi Service

### Admission Service (`/api/admission/`)

```
POST   /auth/login                    # Đăng nhập
POST   /auth/register                 # Đăng ký tài khoản

GET    /api/admission/courses         # Danh sách khóa học
POST   /api/admission/courses         # Tạo khóa học
GET    /api/admission/courses/:id     # Chi tiết khóa học
PUT    /api/admission/courses/:id     # Cập nhật khóa học
DELETE /api/admission/courses/:id     # Xóa khóa học

GET    /api/admission/classes         # Danh sách lớp học
POST   /api/admission/classes         # Tạo lớp học
GET    /api/admission/classes/:id     # Chi tiết lớp học
PUT    /api/admission/classes/:id     # Cập nhật lớp học

POST   /api/admission/enrollments     # Đăng ký vào lớp
GET    /api/admission/enrollments     # Danh sách đăng ký
PUT    /api/admission/enrollments/:id # Duyệt/từ chối đăng ký
```

### Student Service (`/api/student/`)

```
GET    /api/student/students          # Danh sách học viên
GET    /api/student/students/:id      # Chi tiết học viên
PUT    /api/student/students/:id      # Cập nhật thông tin

GET    /api/student/attendances       # Danh sách điểm danh
POST   /api/student/attendances       # Ghi nhận điểm danh
GET    /api/student/attendances?class_id=5&date=2024-03-15
```

### Learning Service (`/api/learning/`)

```
GET    /api/learning/materials        # Danh sách học liệu
POST   /api/learning/materials        # Tạo học liệu
GET    /api/learning/materials/:id    # Chi tiết

GET    /api/learning/grades           # Danh sách điểm
POST   /api/learning/grades           # Nhập điểm
GET    /api/learning/grades?student_id=42&class_id=5
```

### Finance Service (`/api/finance/`)

```
GET    /api/finance/tuition_fees      # Cấu hình học phí
POST   /api/finance/tuition_fees      # Tạo cấu hình học phí

GET    /api/finance/invoices          # Danh sách hóa đơn
POST   /api/finance/invoices          # Tạo hóa đơn
GET    /api/finance/invoices/:id      # Chi tiết hóa đơn

POST   /api/finance/payments          # Ghi nhận thanh toán
GET    /api/finance/payments          # Danh sách thanh toán
GET    /api/finance/payments?student_id=42
```
