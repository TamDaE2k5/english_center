/** Tiện ích cục bộ Finance, không sửa auth/API dùng chung của các phân hệ. */
function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
}

function financeRole() {
  if (typeof getToken !== 'function' || !getToken() || !isAuthenticated()) return 'GUEST';
  const claims = typeof parseJwt === 'function' ? parseJwt(getToken()) : null;
  return String(claims?.role || getCurrentUser()?.role || 'GUEST').toUpperCase();
}

function financeUserId() {
  if (typeof parseJwt === 'function' && typeof getToken === 'function' && getToken()) {
    const claims = parseJwt(getToken());
    const idVal = claims?.sub || claims?.user_id;
    if (idVal && !isNaN(Number(idVal))) return Number(idVal);
  }
  const user = typeof getCurrentUser === 'function' ? getCurrentUser() : null;
  return user?.id ? Number(user.id) : null;
}

function financeCanManage() {
  return ['ADMIN', 'STAFF'].includes(financeRole());
}

function financeIsAdmin() {
  return financeRole() === 'ADMIN';
}

function financeGuardAuth() {
  if (typeof requireAuth === 'function' && typeof isAuthenticated === 'function' && !isAuthenticated()) {
    requireAuth();
    return false;
  }
  return true;
}

function setButtonLoading(btn, isLoading, defaultText) {
  if (!btn) return;
  if (isLoading) {
    btn._defaultText = defaultText || btn.textContent;
    btn.disabled = true;
    btn.textContent = 'Đang xử lý...';
  } else {
    btn.disabled = false;
    btn.textContent = btn._defaultText || defaultText || btn.textContent;
  }
}

document.documentElement.dataset.financeRole = financeRole();
