/**
 * Authentication Helpers - Hệ thống Trung tâm Anh ngữ
 * shared/js/auth.js
 */

const TOKEN_KEY = 'ec_token';
const USER_KEY = 'ec_user';

// Decode JWT payload (without external libraries)
function parseJwt(token) {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch (e) {
    return null;
  }
}

function setSession(token, user) {
  localStorage.setItem(TOKEN_KEY, token);
  if (user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  } else {
    const payload = parseJwt(token);
    if (payload) {
      localStorage.setItem(USER_KEY, JSON.stringify({
        id: payload.sub,
        email: payload.email,
        name: payload.name || payload.email,
        role: payload.role || 'GUEST'
      }));
    }
  }
}

function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

function getCurrentUser() {
  const userStr = localStorage.getItem(USER_KEY);
  if (!userStr) {
    // Default demo user for development if not logged in
    return {
      id: 1,
      email: 'admin@english.edu.vn',
      name: 'Quản Trị Viên (Demo)',
      role: 'ADMIN'
    };
  }
  try {
    return JSON.parse(userStr);
  } catch (e) {
    return null;
  }
}

function isAuthenticated() {
  const token = getToken();
  if (!token) return false;
  const payload = parseJwt(token);
  if (!payload || !payload.exp) return true; // fallback
  return Date.now() < payload.exp * 1000;
}

function requireAuth() {
  if (!isAuthenticated() && !localStorage.getItem('ec_demo_mode')) {
    // Save current path to return after login
    sessionStorage.setItem('redirect_after_login', window.location.pathname);
    window.location.href = '/shared/login.html';
  }
}

function logout() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  window.location.href = '/shared/login.html';
}
