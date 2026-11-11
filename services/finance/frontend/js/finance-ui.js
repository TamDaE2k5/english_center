/** Tiện ích cục bộ Finance, không sửa auth/API dùng chung của các phân hệ. */
function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
}

function financeRole() {
  if (typeof getToken !== 'function' || !getToken() || !isAuthenticated()) return 'GUEST';
  const claims = typeof parseJwt === 'function' ? parseJwt(getToken()) : null;
  return claims?.role || getCurrentUser()?.role || 'GUEST';
}

function financeCanManage() {
  return ['ADMIN', 'STAFF'].includes(financeRole());
}

document.documentElement.dataset.financeRole = financeRole();
