/** Tiện ích cục bộ Admission, không sửa auth/API dùng chung của các phân hệ. */
function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
}
function admissionRole() {
  if (typeof getToken !== 'function' || !getToken() || !isAuthenticated()) return 'GUEST';
  const claims = typeof parseJwt === 'function' ? parseJwt(getToken()) : null;
  return claims?.role || getCurrentUser()?.role || 'GUEST';
}
function admissionCanManage() { return ['ADMIN', 'STAFF'].includes(admissionRole()); }
function admissionRequireStaff() {
  if (!admissionCanManage()) {
    document.body.innerHTML = '<main class="container"><p>Trang này dành cho Học vụ hoặc Quản trị viên.</p><a href="/admission/account.html">Đăng nhập</a></main>';
    throw new Error('Admission role required');
  }
}
document.documentElement.dataset.admissionRole = admissionRole();
// Wrapper chung trả null khi 401: đổi thành lỗi để các form Admission không báo thành công giả.
if (typeof apiRequest === 'function') {
  const originalAdmissionRequest = apiRequest;
  apiRequest = async function(endpoint, options) {
    // 401 của form đăng nhập/link email là lỗi nhập liệu, không phải phiên hết hạn.
    // Xử lý cục bộ vì shared wrapper sẽ logout và chuyển sang trang khác sau mọi 401.
    if (/^\/api\/admission\/auth\/(login|register|forgot-password|reset-password|verify-email|setup-password)$/.test(endpoint)) {
      const result = await fetch(endpoint, {...options, headers: {'Content-Type':'application/json', ...(options?.headers || {})}});
      const data = await result.json();
      if (!result.ok || data.success === false) throw new Error(data.error?.message || 'Không thể xử lý yêu cầu tài khoản.');
      return data;
    }
    const response = await originalAdmissionRequest(endpoint, options);
    if (!response || response.success === false) throw new Error('Yêu cầu chưa hoàn tất. Vui lòng đăng nhập hoặc thử lại.');
    return response;
  };
}
/** Download qua fetch có JWT; window.open không gửi được Authorization. */
async function admissionDownload(url, filename) {
  const response = await fetch(url, {headers: {Authorization: `Bearer ${getToken()}`}});
  if (!response.ok) throw new Error('Không thể xuất dữ liệu. Vui lòng kiểm tra quyền truy cập.');
  const blob = await response.blob(); const href = URL.createObjectURL(blob);
  const anchor = document.createElement('a'); anchor.href = href; anchor.download = filename;
  anchor.click(); URL.revokeObjectURL(href);
}
