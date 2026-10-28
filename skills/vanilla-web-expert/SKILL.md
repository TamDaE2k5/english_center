---
name: vanilla-web-expert
description: >-
  Use this skill whenever creating, modifying, or styling frontend user interfaces across all services
  (services/*/frontend/ and shared/). Covers Vanilla HTML5, CSS3 with Design Tokens, ES6+ JavaScript,
  API calling wrapper, authentication token handling, dynamic layout, and zero-build hot reload.
---

# Vanilla Web & Design System Engineering Skill

## 1. Architectural Philosophy (Triết lý Thiết kế)

Dự án áp dụng **Kiến trúc Web Không Cần Build (Zero-Build Frontend Architecture)**:
- Nginx Gateway (:8080) ánh xạ trực tiếp thư mục `services/{module}/frontend/` và `shared/`.
- Khi sửa đổi HTML, CSS, JavaScript, lập trình viên chỉ cần **lưu file và ấn F5 (hoặc Ctrl+F5)** trên trình duyệt để thấy kết quả ngay lập tức (Hot-Reload).
- Không sử dụng các framework nặng (React/Angular/Vue/Webpack/Vite) trong giai đoạn này nhằm tối ưu tốc độ khởi động và tính độc lập của từng module.

---

## 2. Chuẩn mực Nhúng Thư viện Dùng chung (Shared Assets)

Mọi file HTML của tất cả module bắt buộc phải nhúng bộ Design System dùng chung ở đầu trang:

```html
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Tiêu đề Trang - Hệ thống Quản lý Anh ngữ</title>

    <!-- 1. CSS Design Tokens & Layout Hệ thống -->
    <link rel="stylesheet" href="/shared/css/variables.css">
    <link rel="stylesheet" href="/shared/css/base.css">
    <link rel="stylesheet" href="/shared/css/components.css">
    <link rel="stylesheet" href="/shared/css/layout.css">
    
    <!-- 2. CSS riêng của module (nếu có) -->
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
    <!-- Khung layout chính do shared/js/layout.js tự động render Sidebar & Header -->
    <div id="app-layout"></div>

    <!-- 3. Scripts dùng chung -->
    <script src="/shared/js/auth.js"></script>
    <script src="/shared/js/api.js"></script>
    <script src="/shared/js/layout.js"></script>
    
    <!-- 4. Script xử lý nghiệp vụ của trang -->
    <script src="js/main.js"></script>
</body>
</html>
```

---

## 3. Quy chuẩn Design Tokens (Màu sắc & Giao diện)

Tuyệt đối **không hardcode** mã màu hex (như `#2563eb`, `#16a34a`) trong CSS riêng của module. Luôn sử dụng biến CSS từ `/shared/css/variables.css`:

| Token | Ý nghĩa nghiệp vụ | Ví dụ sử dụng CSS |
|---|---|---|
| `var(--color-primary)` | Hành động chính, nút bấm chính, sidebar active | `background-color: var(--color-primary);` |
| `var(--color-success)` | Trạng thái Đã thanh toán (`PAID`), Đã duyệt (`APPROVED`), Có mặt (`PRESENT`) | `color: var(--color-success);` |
| `var(--color-warning)` | Trạng thái Chờ duyệt (`PENDING`), Đi muộn (`LATE`), Cảnh báo hạn nộp | `background-color: var(--color-warning);` |
| `var(--color-danger)` | Trạng thái Chưa thanh toán (`UNPAID`), Bị từ chối (`REJECTED`), Vắng mặt (`ABSENT`) | `border-color: var(--color-danger);` |
| `var(--color-info)` | Thông tin khóa học, badge bổ trợ | `color: var(--color-info);` |
| `var(--color-bg)` | Màu nền ứng dụng | `background-color: var(--color-bg);` |
| `var(--color-surface)` | Màu nền thẻ Card, Modal, Bảng | `background-color: var(--color-surface);` |

---

## 4. Gọi API Chuẩn mực qua `shared/js/api.js`

Wrapper `api.js` tự động gắn `Authorization: Bearer <token>` từ `localStorage` và xử lý tiền tố URL:

```javascript
// Ví dụ: Lấy danh sách lớp học
async function loadClasses() {
    try {
        // Tự động gọi GET http://gateway:8080/api/admission/classes?status=OPEN
        const response = await api.get('/admission/classes?status=OPEN');
        if (response.success) {
            renderClassesTable(response.data);
        } else {
            showToast(response.error.message, 'danger');
        }
    } catch (error) {
        console.error('Lỗi khi tải dữ liệu lớp học:', error);
        showToast('Không thể kết nối tới máy chủ', 'danger');
    }
}

// Ví dụ: Nộp đơn đăng ký lớp học
async function submitEnrollment(classId) {
    const submitBtn = document.getElementById('btn-enroll');
    
    // 1. Trạng thái Loading: Disable nút bấm và hiển thị spinner
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span class="spinner"></span> Đang xử lý...';

    try {
        const payload = { class_id: classId };
        const response = await api.post('/admission/enrollments', payload);

        if (response.success) {
            showToast('Đăng ký lớp thành công! Vui lòng thanh toán trong 48h.', 'success');
            setTimeout(() => {
                window.location.href = '/admission/my-enrollments.html';
            }, 1500);
        } else {
            showToast(response.error.message || 'Đăng ký thất bại', 'danger');
        }
    } catch (err) {
        showToast(err.message || 'Lỗi mạng khi gửi yêu cầu', 'danger');
    } finally {
        // 2. Phục hồi trạng thái nút bấm
        submitBtn.disabled = false;
        submitBtn.textContent = 'Đăng ký học ngay';
    }
}
```

---

## 5. Xử lý Trạng thái Xác thực & Điều hướng

File `/shared/js/auth.js` cung cấp các tiện ích quản lý phiên:
- `auth.isAuthenticated()`: Kiểm tra đã có access token chưa.
- `auth.getUser()`: Lấy thông tin người dùng hiện tại (`id`, `email`, `role`, `full_name`).
- `auth.hasRole('ADMIN')`: Kiểm tra quyền truy cập.
- `auth.logout()`: Xóa token và điều hướng về trang đăng nhập `/shared/login.html`.

### Trang yêu cầu bảo vệ quyền (Guarded Page):
```javascript
document.addEventListener('DOMContentLoaded', () => {
    // Nếu chưa đăng nhập -> chuyển về trang login
    if (!auth.isAuthenticated()) {
        window.location.href = '/shared/login.html';
        return;
    }

    // Nếu không phải Staff hoặc Admin -> chặn truy cập
    if (!auth.hasRole('STAFF') && !auth.hasRole('ADMIN')) {
        alert('Bạn không có quyền truy cập trang quản lý này!');
        window.location.href = '/';
        return;
    }
});
```

---

## 6. Checklist Kiểm tra Frontend

- [ ] Nhúng đủ 4 file CSS/JS dùng chung từ `/shared/`.
- [ ] 100% màu sắc sử dụng biến `var(--color-*)`.
- [ ] Mọi nút bấm gửi dữ liệu đều có trạng thái Disable / Loading khi đang chờ API phản hồi.
- [ ] Bắt lỗi và hiển thị thông báo thân thiện từ `error.message`.
- [ ] Không có lỗi cú pháp JavaScript trong Developer Tools Console (F12).
