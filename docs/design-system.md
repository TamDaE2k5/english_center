# Design System - Trung tâm Anh ngữ

> Hướng dẫn thiết kế giao diện thống nhất. Mọi module phải tuân theo để hệ thống trông như **một sản phẩm duy nhất**.

## 1. Bảng màu (Color Palette)

### Màu chính (Primary)

| Tên | Hex | Dùng cho |
|---|---|---|
| Primary | `#2563EB` | Button chính, link, icon active, sidebar active |
| Primary Hover | `#1D4ED8` | Hover state |
| Primary Light | `#EFF6FF` | Background highlight, badge, selected row |

### Màu phụ (Secondary)

| Tên | Hex | Dùng cho |
|---|---|---|
| Success | `#16A34A` | Trạng thái thành công, badge "Đã thanh toán" |
| Warning | `#F59E0B` | Cảnh báo, badge "Chờ duyệt" |
| Danger | `#DC2626` | Lỗi, xóa, badge "Quá hạn" |
| Info | `#0EA5E9` | Thông tin, tooltip |

### Màu nền & Text

| Tên | Hex | Dùng cho |
|---|---|---|
| Background | `#F8FAFC` | Nền trang |
| Surface | `#FFFFFF` | Card, modal, bảng |
| Border | `#E2E8F0` | Đường viền |
| Text Primary | `#1E293B` | Tiêu đề, nội dung chính |
| Text Secondary | `#64748B` | Mô tả, label, text phụ |
| Text Muted | `#94A3B8` | Placeholder, text mờ |

### CSS Variables

```css
/* Đã định nghĩa sẵn trong shared/css/variables.css */
:root {
  --color-primary: #2563EB;
  --color-primary-hover: #1D4ED8;
  --color-primary-light: #EFF6FF;

  --color-success: #16A34A;
  --color-warning: #F59E0B;
  --color-danger: #DC2626;
  --color-info: #0EA5E9;

  --color-bg: #F8FAFC;
  --color-surface: #FFFFFF;
  --color-border: #E2E8F0;

  --color-text: #1E293B;
  --color-text-secondary: #64748B;
  --color-text-muted: #94A3B8;
}
```

> **Quy tắc:** KHÔNG hardcode màu trong CSS của module. Luôn dùng `var(--color-...)`.

---

## 2. Typography

### Font chữ

- **Font chính:** `Inter` (Google Fonts)
- **Fallback:** `-apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif`
- Inter hỗ trợ tốt tiếng Việt, dễ đọc, miễn phí.

```html
<!-- Thêm vào <head> - đã có sẵn trong base.css -->
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
```

### Kích thước

| Element | Size | Weight | CSS Class |
|---|---|---|---|
| Heading 1 | 24px | 700 (Bold) | `.heading-1` |
| Heading 2 | 20px | 600 (SemiBold) | `.heading-2` |
| Heading 3 | 16px | 600 (SemiBold) | `.heading-3` |
| Body | 14px | 400 (Regular) | (mặc định) |
| Body Small | 13px | 400 | `.text-sm` |
| Caption | 12px | 400 | `.text-xs` |
| Label | 13px | 500 (Medium) | `.label` |

---

## 3. Spacing & Layout

### Spacing Scale (bội số của 4px)

| Token | Giá trị | CSS Variable |
|---|---|---|
| xs | 4px | `--space-xs` |
| sm | 8px | `--space-sm` |
| md | 16px | `--space-md` |
| lg | 24px | `--space-lg` |
| xl | 32px | `--space-xl` |
| 2xl | 48px | `--space-2xl` |

### Bố cục trang (Page Layout)

```
┌──────────────────────────────────────────────────┐
│  Header (56px cao)                    [User ▼]   │
├────────┬─────────────────────────────────────────┤
│        │  Breadcrumb: Trang chủ > Khóa học       │
│  Side  │─────────────────────────────────────────│
│  bar   │                                         │
│  (240  │  Content Area                           │
│   px)  │  (padding: 24px)                        │
│        │                                         │
│  - Tổng│                                         │
│    quan│                                         │
│  - Khóa│                                         │
│    học │                                         │
│  - Lớp │                                         │
│    học │                                         │
│  - ... │                                         │
│        │                                         │
└────────┴─────────────────────────────────────────┘
```

- **Sidebar:** 240px, cố định bên trái, có thể thu gọn thành 64px (icon only)
- **Header:** 56px, cố định trên cùng
- **Content:** Phần còn lại, padding 24px
- **Breadcrumb:** Hiển thị ngay dưới header

---

## 4. Components chuẩn

### 4.1 Button

```html
<button class="btn btn-primary">Tạo mới</button>
<button class="btn btn-secondary">Hủy</button>
<button class="btn btn-danger">Xóa</button>
<button class="btn btn-ghost">Quay lại</button>
```

| Variant | Background | Text | Dùng khi |
|---|---|---|---|
| `btn-primary` | Primary blue | White | Hành động chính (Tạo, Lưu, Xác nhận) |
| `btn-secondary` | White + border | Text primary | Hành động phụ (Hủy, Đóng) |
| `btn-danger` | Danger red | White | Xóa, hủy bỏ |
| `btn-ghost` | Transparent | Text secondary | Quay lại, link-style |

Tất cả button: `border-radius: 6px`, `padding: 8px 16px`, `font-weight: 500`.

### 4.2 Card

```html
<div class="card">
  <div class="card-header">
    <h2 class="heading-2">Danh sách khóa học</h2>
    <button class="btn btn-primary">+ Tạo mới</button>
  </div>
  <div class="card-body">
    <!-- Nội dung -->
  </div>
</div>
```

Card: `background: white`, `border: 1px solid var(--color-border)`, `border-radius: 8px`, `box-shadow: 0 1px 3px rgba(0,0,0,0.05)`.

### 4.3 Table (Bảng dữ liệu)

```html
<table class="data-table">
  <thead>
    <tr>
      <th>ID</th>
      <th>Tên khóa học</th>
      <th>Trạng thái</th>
      <th>Thao tác</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>1</td>
      <td>IELTS Foundation</td>
      <td><span class="badge badge-success">Đang mở</span></td>
      <td>
        <button class="btn-icon" title="Sửa">✏️</button>
        <button class="btn-icon" title="Xóa">🗑️</button>
      </td>
    </tr>
  </tbody>
</table>
```

### 4.4 Badge (Trạng thái)

```html
<span class="badge badge-success">Đã thanh toán</span>
<span class="badge badge-warning">Chờ duyệt</span>
<span class="badge badge-danger">Quá hạn</span>
<span class="badge badge-info">Mới</span>
```

### 4.5 Form

```html
<div class="form-group">
  <label class="label" for="course-name">Tên khóa học <span class="required">*</span></label>
  <input type="text" id="course-name" class="form-input" placeholder="Nhập tên khóa học">
  <span class="form-error">Tên khóa học không được để trống</span>
</div>

<div class="form-group">
  <label class="label" for="status">Trạng thái</label>
  <select id="status" class="form-select">
    <option value="active">Đang hoạt động</option>
    <option value="inactive">Tạm dừng</option>
  </select>
</div>
```

### 4.6 Modal (Dialog)

```html
<div class="modal-overlay" id="deleteModal">
  <div class="modal">
    <div class="modal-header">
      <h3 class="heading-3">Xác nhận xóa</h3>
      <button class="modal-close">&times;</button>
    </div>
    <div class="modal-body">
      <p>Bạn có chắc muốn xóa khóa học này?</p>
    </div>
    <div class="modal-footer">
      <button class="btn btn-secondary" onclick="closeModal()">Hủy</button>
      <button class="btn btn-danger" onclick="confirmDelete()">Xóa</button>
    </div>
  </div>
</div>
```

### 4.7 Toast Notification

```javascript
// Gọi từ JavaScript
showToast('success', 'Tạo khóa học thành công');
showToast('error', 'Không thể kết nối server');
showToast('warning', 'Học viên này đã đăng ký rồi');
```

---

## 5. Icon

Sử dụng emoji hoặc [Lucide Icons](https://lucide.dev/) (SVG icons, nhẹ, miễn phí).

```html
<!-- Dùng emoji cho đơn giản -->
<button>📝 Sửa</button>
<button>🗑️ Xóa</button>

<!-- Hoặc Lucide Icons (CDN) -->
<script src="https://unpkg.com/lucide@latest"></script>
<i data-lucide="edit"></i>
<i data-lucide="trash-2"></i>
```

---

## 6. Responsive

Hệ thống là **admin dashboard** nên ưu tiên **desktop-first** (min-width: 1280px).

Nhưng cần responsive cơ bản:
- Sidebar thu gọn thành icon ở `< 1024px`
- Bảng có horizontal scroll ở `< 768px`
- Không cần mobile-perfect cho MVP

---

## 7. Trạng thái (Status) chuẩn hóa

Để giao diện thống nhất, các trạng thái dùng chung:

| Trạng thái | Badge class | Ý nghĩa |
|---|---|---|
| `active` / `Đang hoạt động` | `badge-success` | Entity đang hoạt động |
| `inactive` / `Tạm dừng` | `badge-warning` | Tạm dừng, chờ xử lý |
| `pending` / `Chờ duyệt` | `badge-warning` | Đang chờ phê duyệt |
| `approved` / `Đã duyệt` | `badge-success` | Đã được duyệt |
| `rejected` / `Từ chối` | `badge-danger` | Bị từ chối |
| `paid` / `Đã thanh toán` | `badge-success` | Đã thanh toán |
| `unpaid` / `Chưa thanh toán` | `badge-danger` | Chưa thanh toán |
| `overdue` / `Quá hạn` | `badge-danger` | Quá hạn |

---

## 8. Mẫu trang chuẩn

Mỗi module nên có các trang theo pattern:

1. **List page** — Bảng dữ liệu có tìm kiếm, phân trang, nút Tạo mới
2. **Create/Edit page** — Form nhập liệu
3. **Detail page** — Xem chi tiết (nếu cần)

### Template HTML chuẩn

```html
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Tên trang - Trung tâm Anh ngữ</title>

  <!-- Shared CSS -->
  <link rel="stylesheet" href="/shared/css/variables.css">
  <link rel="stylesheet" href="/shared/css/base.css">
  <link rel="stylesheet" href="/shared/css/components.css">
  <link rel="stylesheet" href="/shared/css/layout.css">

  <!-- Module CSS (nếu cần) -->
  <link rel="stylesheet" href="/admission/css/custom.css">
</head>
<body>
  <!-- Sidebar + Header sẽ được inject bởi layout.js -->
  <div id="app">
    <div id="sidebar"></div>
    <div class="main-content">
      <div id="header"></div>
      <div class="content">
        <!-- Breadcrumb -->
        <nav class="breadcrumb">
          <a href="/">Trang chủ</a> / <span>Khóa học</span>
        </nav>

        <!-- ===== NỘI DUNG MODULE TẠI ĐÂY ===== -->
        <div class="card">
          <div class="card-header">
            <h1 class="heading-1">Danh sách khóa học</h1>
            <button class="btn btn-primary">+ Tạo mới</button>
          </div>
          <div class="card-body">
            <!-- Table, Form, etc. -->
          </div>
        </div>

      </div>
    </div>
  </div>

  <!-- Shared JS -->
  <script src="/shared/js/auth.js"></script>
  <script src="/shared/js/api.js"></script>
  <script src="/shared/js/layout.js"></script>

  <!-- Module JS -->
  <script src="/admission/js/courses.js"></script>
</body>
</html>
```
