/** Tiện ích cục bộ Admission, không sửa auth/API dùng chung của các phân hệ. */
function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
}
function admissionRole() {
  if (typeof getToken !== 'function' || !getToken() || !isAuthenticated()) return 'GUEST';
  const user = getCurrentUser();
  return user?.mustChangePassword ? 'GUEST' : user?.role || 'GUEST';
}
function admissionCanManage() { return ['ADMIN', 'STAFF'].includes(admissionRole()); }
function admissionRequireStaff() {
  if (!admissionCanManage()) {
    document.body.innerHTML = '<main class="container"><p>Trang này dành cho Học vụ hoặc Quản trị viên.</p><a href="/admission/account.html">Đăng nhập</a></main>';
    return false;
  }
  return true;
}

// Dữ liệu hồ sơ dùng textContent để tránh chèn HTML vào header dùng chung.
if (typeof renderHeader === 'function') {
  renderHeader = function () {
    const header = document.getElementById('header');
    if (!header) return;
    const user = getCurrentUser();
    header.innerHTML = '<div class="header-title">Hệ Thống Quản Lý Trung Tâm Anh Ngữ</div>' +
      '<div class="header-user-section"></div>';
    const section = header.querySelector('.header-user-section');
    if (!user) {
      section.innerHTML = '<a class="btn btn-ghost btn-sm" href="/admission/account.html">Đăng nhập</a>';
      return;
    }
    section.innerHTML = '<div class="user-badge"><div class="user-avatar"></div>' +
      '<div class="user-details"><div class="user-name"></div><div class="user-role"></div></div></div>' +
      '<button class="btn btn-ghost btn-sm" type="button" onclick="handleLogout()">Đăng xuất</button>';
    section.querySelector('.user-avatar').textContent = user.name.charAt(0).toUpperCase();
    section.querySelector('.user-name').textContent = user.name;
    section.querySelector('.user-role').textContent = user.role;
  };
}

function admissionSessionChanged() {
  document.documentElement.dataset.admissionRole = admissionRole();
  if (typeof renderHeader === 'function') renderHeader();
  if (typeof updateAccountSession === 'function') updateAccountSession();
}
admissionSessionChanged();

// Yêu cầu Admission dùng adapter phiên cục bộ, giữ wrapper chung cho các endpoint khác.
if (typeof apiRequest === 'function') {
  const originalAdmissionRequest = apiRequest;
  apiRequest = async function(endpoint, options = {}) {
    if (!endpoint.startsWith('/api/admission/')) return originalAdmissionRequest(endpoint, options);
    const publicAuth = /^\/api\/admission\/auth\/(login|register|forgot-password|reset-password|verify-email|setup-password)$/.test(endpoint);
    const request = {...options, headers: {'Content-Type':'application/json', ...options.headers}};
    try {
      const result = await (publicAuth ? fetch(endpoint, request) : admissionFetch(endpoint, request));
      if (result.status === 204) return {success: true};
      const data = await result.json().catch(() => null);
      if (!result.ok || data?.success !== true) {
        const fallback = result.status === 403 ? 'Bạn không có quyền thực hiện thao tác này.' : 'Không thể xử lý yêu cầu.';
        throw new Error(data?.error?.message || fallback);
      }
      return data;
    } catch (error) {
      if (!publicAuth) showToast('error', escapeHtml(error.message));
      throw error;
    }
  };
}
/** Download qua fetch có JWT; window.open không gửi được Authorization. */
async function admissionDownload(url, filename) {
  const response = await admissionFetch(url);
  if (!response.ok) throw new Error('Không thể xuất dữ liệu. Vui lòng kiểm tra quyền truy cập.');
  const blob = await response.blob(); const href = URL.createObjectURL(blob);
  const anchor = document.createElement('a'); anchor.href = href; anchor.download = filename;
  anchor.click(); URL.revokeObjectURL(href);
}
