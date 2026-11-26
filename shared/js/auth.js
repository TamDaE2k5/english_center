/**
 * Authentication Helpers - Hệ thống Trung tâm Anh ngữ
 * shared/js/auth.js
 * Quản lý phiên JWT dùng chung (localStorage) giữa các phân hệ.
 */

const TOKEN_KEY = 'ec_token';
const REFRESH_TOKEN_KEY = 'ec_refresh_token';
const USER_KEY = 'ec_user';

/**
 * Giải mã phần payload của JWT (Base64URL -> JSON) không cần thư viện ngoài.
 * @param {string} token Chuỗi JWT cần giải mã
 * @returns {object|null} Đối tượng claims hoặc null nếu token không hợp lệ
 */
function parseJwt(token) {
  try {
    if (!token || typeof token !== 'string') return null;
    const parts = token.split('.');
    if (parts.length !== 3 || !parts[1]) return null;
    const base64Url = parts[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4);
    const jsonPayload = decodeURIComponent(
      atob(padded)
        .split('')
        .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    const parsed = JSON.parse(jsonPayload);
    return parsed && typeof parsed === 'object' ? parsed : null;
  } catch (e) {
    return null;
  }
}

/**
 * Chuẩn hóa đối tượng user để tương thích cả snake_case (user_id, full_name)
 * từ Admission Auth API và camelCase (id, name) ở các trang frontend.
 * @param {object|null} user Thông tin user từ response hoặc localStorage
 * @param {object|null} payload Claims giải mã từ JWT
 * @returns {object|null} Đối tượng user đã chuẩn hóa
 */
function normalizeSessionUser(user, payload) {
  const source = user || {};
  const claims = payload || {};
  const rawId = source.id ?? source.user_id ?? claims.user_id ?? claims.sub;
  const numericId = rawId !== undefined && rawId !== null && /^\d+$/.test(String(rawId))
    ? Number(rawId)
    : rawId;
  const email = source.email || claims.email || '';
  const fullName = source.full_name || source.name || claims.full_name || claims.name || email || 'Người dùng';
  const role = String(source.role || claims.role || 'GUEST').toUpperCase();

  if (!email && !numericId && role === 'GUEST') return null;

  return {
    ...source,
    id: numericId ?? null,
    user_id: source.user_id ?? numericId ?? null,
    email,
    name: source.name || fullName,
    full_name: source.full_name || fullName,
    role,
    must_change_password: Boolean(source.must_change_password ?? source.mustChangePassword ?? claims.must_change_password),
    mustChangePassword: Boolean(source.must_change_password ?? source.mustChangePassword ?? claims.must_change_password)
  };
}

/**
 * Lưu phiên đăng nhập (Access Token, thông tin User, và Refresh Token nếu có).
 * @param {string} token JWT access token
 * @param {object|null} user Thông tin người dùng trả về từ /auth/login
 * @param {string|null} refreshToken JWT refresh token (tùy chọn)
 */
function setSession(token, user, refreshToken) {
  if (!token || typeof token !== 'string' || token === 'undefined' || token === 'null') {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    return;
  }
  localStorage.setItem(TOKEN_KEY, token);
  if (refreshToken && typeof refreshToken === 'string') {
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  }
  const payload = parseJwt(token);
  const normalized = normalizeSessionUser(user, payload);
  if (normalized) {
    localStorage.setItem(USER_KEY, JSON.stringify(normalized));
  } else {
    localStorage.removeItem(USER_KEY);
  }
}

/**
 * Lấy Access Token hiện tại từ localStorage.
 * @returns {string|null} Access token hợp lệ hoặc null
 */
function getToken() {
  const token = localStorage.getItem(TOKEN_KEY);
  if (!token || token === 'undefined' || token === 'null') return null;
  return token;
}

/**
 * Lấy thông tin người dùng hiện tại đang đăng nhập.
 * Không tự giả mạo tài khoản ADMIN nếu người dùng chưa đăng nhập.
 * @returns {object|null} Thông tin user chuẩn hóa hoặc null nếu chưa đăng nhập
 */
function getCurrentUser() {
  const token = getToken();
  const payload = token ? parseJwt(token) : null;
  const userStr = localStorage.getItem(USER_KEY);

  if (userStr) {
    try {
      const parsed = JSON.parse(userStr);
      return normalizeSessionUser(parsed, payload);
    } catch (e) {
      localStorage.removeItem(USER_KEY);
    }
  }

  if (payload) {
    return normalizeSessionUser(null, payload);
  }

  return null;
}

/**
 * Kiểm tra người dùng có access token hợp lệ và còn hạn sử dụng hay không.
 * @returns {boolean} true nếu đã xác thực và token chưa hết hạn
 */
function isAuthenticated() {
  const token = getToken();
  if (!token) return false;
  const payload = parseJwt(token);
  if (!payload) return false;
  if (typeof payload.exp === 'number') {
    return Date.now() < payload.exp * 1000;
  }
  return true;
}

/**
 * Kiểm tra vai trò hiện tại của người dùng.
 * @param {string} role Vai trò cần kiểm tra (ADMIN, STAFF, TEACHER, STUDENT)
 * @returns {boolean} true nếu khớp vai trò và đang đăng nhập hợp lệ
 */
function hasRole(role) {
  if (!isAuthenticated()) return false;
  const user = getCurrentUser();
  return Boolean(user && user.role === String(role || '').toUpperCase());
}

/**
 * Yêu cầu đăng nhập cho các trang cần bảo vệ, tự động chuyển về trang login nếu chưa đăng nhập.
 */
function requireAuth() {
  if (!isAuthenticated()) {
    sessionStorage.setItem('redirect_after_login', window.location.pathname + window.location.search);
    window.location.href = '/shared/login.html';
  }
}

/**
 * Đăng xuất: xóa token, thông tin người dùng khỏi localStorage và chuyển về trang đăng nhập.
 */
function logout() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  window.location.href = '/shared/login.html';
}

// Hỗ trợ namespace window.auth theo tài liệu vanilla-web-expert
const auth = {
  parseJwt,
  setSession,
  getToken,
  getCurrentUser,
  isAuthenticated,
  hasRole,
  requireAuth,
  logout
};
if (typeof window !== 'undefined') {
  window.auth = auth;
}
