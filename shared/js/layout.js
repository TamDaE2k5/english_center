/**
 * Layout Dynamic Renderer - Hệ thống Trung tâm Anh ngữ
 * shared/js/layout.js
 * Injects sidebar and header into HTML templates automatically
 */

document.addEventListener('DOMContentLoaded', () => {
  renderSidebar();
  renderHeader();
});

function renderSidebar() {
  const sidebarContainer = document.getElementById('sidebar');
  if (!sidebarContainer) return;

  const currentPath = window.location.pathname;

  const navItems = [
    { type: 'link', href: '/', label: 'Tổng quan', icon: '📊' },
    
    { type: 'section', title: '1. Tuyển sinh & Lớp học' },
    { type: 'link', href: '/admission/courses.html', label: 'Quản lý khóa học', icon: '📖' },
    { type: 'link', href: '/admission/classes.html', label: 'Quản lý lớp học', icon: '🏫' },
    { type: 'link', href: '/admission/enrollments.html', label: 'Đăng ký & Tuyển sinh', icon: '📝' },

    { type: 'section', title: '2. Quản lý học viên' },
    { type: 'link', href: '/student/students.html', label: 'Hồ sơ học viên', icon: '👨‍🎓' },
    { type: 'link', href: '/student/attendance.html', label: 'Điểm danh buổi học', icon: '✅' },

    { type: 'section', title: '3. Học tập & Kết quả' },
    { type: 'link', href: '/learning/materials.html', label: 'Kho học liệu & Slide', icon: '📁' },
    { type: 'link', href: '/learning/grades.html', label: 'Bảng điểm & Đánh giá', icon: '🎯' },

    { type: 'section', title: '4. Tài chính & Học phí' },
    { type: 'link', href: '/finance/tuition.html', label: 'Cấu hình biểu phí', icon: '🏷️' },
    { type: 'link', href: '/finance/invoices.html', label: 'Hóa đơn & Thu học phí', icon: '💳' },
  ];

  let navHtml = `
    <div class="sidebar-brand">
      <span>🎓 English Center</span>
      <span class="brand-badge">PRO</span>
    </div>
    <div class="sidebar-nav">
  `;

  navItems.forEach(item => {
    if (item.type === 'section') {
      navHtml += `<div class="nav-section-title">${item.title}</div>`;
    } else {
      const isActive = currentPath === item.href || (item.href !== '/' && currentPath.includes(item.href.replace('.html', '')));
      navHtml += `
        <a href="${item.href}" class="nav-link ${isActive ? 'active' : ''}">
          <span class="nav-icon">${item.icon}</span>
          <span>${item.label}</span>
        </a>
      `;
    }
  });

  navHtml += `</div>`;
  sidebarContainer.innerHTML = navHtml;
}

function renderHeader() {
  const headerContainer = document.getElementById('header');
  if (!headerContainer) return;

  const user = typeof getCurrentUser === 'function' ? getCurrentUser() : {
    name: 'Quản Trị Viên',
    role: 'ADMIN',
    email: 'admin@english.edu.vn'
  };

  const initial = user.name ? user.name.charAt(0).toUpperCase() : 'U';

  headerContainer.innerHTML = `
    <div class="header-title">Hệ Thống Quản Lý Trung Tâm Anh Ngữ</div>
    <div class="header-user-section">
      <div class="user-badge">
        <div class="user-avatar">${initial}</div>
        <div class="user-details">
          <div class="user-name">${user.name || user.email}</div>
          <div class="user-role">${user.role || 'USER'}</div>
        </div>
      </div>
      <button class="btn btn-ghost btn-sm" onclick="handleLogout()" title="Đăng xuất">
        🚪 Đăng xuất
      </button>
    </div>
  `;
}

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
