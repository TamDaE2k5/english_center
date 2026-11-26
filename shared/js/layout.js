/**
 * Layout Dynamic Renderer - Hệ thống Trung tâm Anh ngữ
 * shared/js/layout.js
 * Tự động render Sidebar phân quyền theo vai trò (ADMIN, STAFF, TEACHER, STUDENT, GUEST) và Header.
 */

document.addEventListener('DOMContentLoaded', () => {
  renderSidebar();
  renderHeader();
});

/**
 * Escape ký tự HTML đặc biệt để chống XSS khi render thông tin người dùng.
 * @param {string} value Chuỗi đầu vào
 * @returns {string} Chuỗi an toàn HTML
 */
function escapeLayoutHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, c => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
  }[c]));
}

/**
 * Lấy vai trò hiện tại của người dùng (GUEST nếu chưa đăng nhập hoặc token hết hạn).
 * @returns {string} ADMIN | STAFF | TEACHER | STUDENT | GUEST
 */
function resolveLayoutRole() {
  const authed = typeof isAuthenticated === 'function' ? isAuthenticated() : false;
  const user = typeof getCurrentUser === 'function' ? getCurrentUser() : null;
  if (!authed || !user || !user.role) return 'GUEST';
  return String(user.role).toUpperCase();
}

/**
 * Render Sidebar điều hướng bên trái, lọc các mục phù hợp với vai trò người dùng.
 */
function renderSidebar() {
  const sidebarContainer = document.getElementById('sidebar');
  if (!sidebarContainer) return;

  const currentPath = window.location.pathname;
  const role = resolveLayoutRole();
  const ALL = ['ADMIN', 'STAFF', 'TEACHER', 'STUDENT', 'GUEST'];
  const AUTHED = ['ADMIN', 'STAFF', 'TEACHER', 'STUDENT'];

  const navItems = [
    { type: 'link', href: '/', label: 'Tổng quan', icon: '📊', roles: ALL },

    { type: 'section', title: '1. Tuyển sinh & Lớp học', roles: ALL },
    { type: 'link', href: '/admission/courses.html', label: 'Quản lý khóa học', icon: '📖', roles: ALL },
    { type: 'link', href: '/admission/classes.html', label: 'Quản lý lớp học', icon: '🏫', roles: ALL },
    { type: 'link', href: '/admission/rooms.html', label: 'Phòng học & Lịch trống', icon: '🚪', roles: ['ADMIN', 'STAFF'] },
    { type: 'link', href: '/admission/test.html', label: 'Khảo thí đầu vào', icon: '✍️', roles: ALL },
    { type: 'link', href: '/admission/enrollments.html', label: 'Đăng ký & Tuyển sinh', icon: '📝', roles: ['ADMIN', 'STAFF', 'STUDENT'] },
    { type: 'link', href: '/admission/bi-dashboard.html', label: 'Báo cáo Tuyển sinh BI', icon: '📈', roles: ['ADMIN', 'STAFF'] },

    { type: 'section', title: '2. Quản lý học viên', roles: ['ADMIN', 'STAFF', 'TEACHER', 'STUDENT'] },
    { type: 'link', href: '/student/students.html', label: 'Hồ sơ học viên', icon: '👨‍🎓', roles: ['ADMIN', 'STAFF', 'STUDENT'] },
    { type: 'link', href: '/student/attendance.html', label: 'Điểm danh buổi học', icon: '✅', roles: ['ADMIN', 'STAFF', 'TEACHER'] },
    { type: 'link', href: '/student/status_requests.html', label: 'Yêu cầu học vụ', icon: '📄', roles: ['ADMIN', 'STAFF', 'STUDENT'] },

    { type: 'section', title: '3. Học tập & Kết quả', roles: AUTHED },
    { type: 'link', href: '/learning/materials.html', label: 'Kho học liệu & Slide', icon: '📁', roles: AUTHED },
    { type: 'link', href: '/learning/grades.html', label: 'Bảng điểm & Đánh giá', icon: '🎯', roles: AUTHED },
    { type: 'link', href: '/learning/reports.html', label: 'Báo cáo kết quả học tập', icon: '📑', roles: AUTHED },

    { type: 'section', title: '4. Tài chính & Học phí', roles: ['ADMIN', 'STAFF', 'STUDENT'] },
    { type: 'link', href: '/finance/tuition.html', label: 'Biểu phí & Voucher', icon: '🏷️', roles: ['ADMIN', 'STAFF', 'STUDENT'] },
    { type: 'link', href: '/finance/invoices.html', label: 'Hóa đơn & Thu học phí', icon: '💳', roles: ['ADMIN', 'STAFF', 'STUDENT'] },
    { type: 'link', href: '/finance/bi-dashboard.html', label: 'Báo cáo Tài chính BI', icon: '💹', roles: ['ADMIN', 'STAFF'] },

    { type: 'section', title: 'Tài khoản', roles: ALL },
    { type: 'link', href: '/admission/account.html', label: 'Tài khoản & Mật khẩu', icon: '🔐', roles: ALL }
  ];

  // Lọc các mục theo role và bỏ section rỗng nếu không có link nào bên dưới
  const visibleItems = navItems.filter(item => !item.roles || item.roles.includes(role));

  let navHtml = `
    <div class="sidebar-brand">
      <span>🎓 English Center</span>
      <span class="brand-badge">PRO</span>
    </div>
    <div class="sidebar-nav">
  `;

  visibleItems.forEach((item, idx) => {
    if (item.type === 'section') {
      const next = visibleItems[idx + 1];
      if (!next || next.type === 'section') return;
      navHtml += `<div class="nav-section-title">${escapeLayoutHtml(item.title)}</div>`;
    } else {
      const isActive = currentPath === item.href || (item.href !== '/' && currentPath.endsWith(item.href));
      navHtml += `
        <a href="${item.href}" class="nav-link ${isActive ? 'active' : ''}">
          <span class="nav-icon">${item.icon}</span>
          <span>${escapeLayoutHtml(item.label)}</span>
        </a>
      `;
    }
  });

  navHtml += `</div>`;
  sidebarContainer.innerHTML = navHtml;
}

/**
 * Render Header phía trên cùng với thông tin người dùng hiện tại và nút Đăng nhập / Đăng xuất.
 */
function renderHeader() {
  const headerContainer = document.getElementById('header');
  if (!headerContainer) return;

  const authed = typeof isAuthenticated === 'function' ? isAuthenticated() : false;
  const user = authed && typeof getCurrentUser === 'function' ? getCurrentUser() : null;

  if (!user) {
    headerContainer.innerHTML = `
      <div class="header-title">Hệ Thống Quản Lý Trung Tâm Anh Ngữ</div>
      <div class="header-user-section">
        <span class="badge badge-info">Khách (Chưa đăng nhập)</span>
        <a class="btn btn-primary btn-sm" href="/shared/login.html">🔐 Đăng nhập</a>
      </div>
    `;
    return;
  }

  const displayName = user.full_name || user.name || user.email || 'Người dùng';
  const initial = displayName.charAt(0).toUpperCase();

  headerContainer.innerHTML = `
    <div class="header-title">Hệ Thống Quản Lý Trung Tâm Anh Ngữ</div>
    <div class="header-user-section">
      <div class="user-badge">
        <div class="user-avatar">${escapeLayoutHtml(initial)}</div>
        <div class="user-details">
          <div class="user-name">${escapeLayoutHtml(displayName)}</div>
          <div class="user-role">${escapeLayoutHtml(user.role || 'USER')}</div>
        </div>
      </div>
      <button class="btn btn-ghost btn-sm" onclick="handleLogout()" title="Đăng xuất">
        🚪 Đăng xuất
      </button>
    </div>
  `;
}

/**
 * Xử lý sự kiện đăng xuất sau khi xác nhận với người dùng.
 */
function handleLogout() {
  if (confirm('Bạn có chắc chắn muốn đăng xuất?')) {
    if (typeof logout === 'function') {
      logout();
    } else {
      localStorage.clear();
      window.location.href = '/shared/login.html';
    }
  }
}
