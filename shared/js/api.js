/**
 * API Client & Toast Notification Helpers - Hệ thống Trung tâm Anh ngữ
 * shared/js/api.js
 */

// Universal Fetch Wrapper
async function apiRequest(endpoint, options = {}) {
  const url = endpoint.startsWith('http') ? endpoint : endpoint;
  
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  };

  const token = typeof getToken === 'function' ? getToken() : localStorage.getItem('ec_token');
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  try {
    const response = await fetch(url, {
      ...options,
      headers
    });

    // Handle Unauthorized
    if (response.status === 401) {
      showToast('error', 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
      if (typeof logout === 'function') {
        setTimeout(() => logout(), 1500);
      }
      return null;
    }

    // Handle 204 No Content
    if (response.status === 204) {
      return { success: true };
    }

    const data = await response.json();

    if (!response.ok) {
      const errorMsg = (data.error && data.error.message) || data.message || `Lỗi HTTP ${response.status}`;
      showToast('error', errorMsg);
      throw new Error(errorMsg);
    }

    return data;
  } catch (err) {
    console.error('API Request Error:', err);
    throw err;
  }
}

// Shortcut Methods
async function apiGet(endpoint) {
  return apiRequest(endpoint, { method: 'GET' });
}

async function apiPost(endpoint, body) {
  return apiRequest(endpoint, {
    method: 'POST',
    body: JSON.stringify(body)
  });
}

async function apiPut(endpoint, body) {
  return apiRequest(endpoint, {
    method: 'PUT',
    body: JSON.stringify(body)
  });
}

async function apiDelete(endpoint) {
  return apiRequest(endpoint, { method: 'DELETE' });
}

// Toast Notifications
function showToast(type = 'info', message = '', duration = 3000) {
  let container = document.querySelector('.toast-container');
  if (!container) {
    container = document.createElement('div');
    container.className = 'toast-container';
    document.body.appendChild(container);
  }

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  
  let icon = 'ℹ️';
  if (type === 'success') icon = '✅';
  if (type === 'error') icon = '❌';
  if (type === 'warning') icon = '⚠️';

  toast.innerHTML = `
    <div style="display: flex; align-items: center; gap: 8px;">
      <span>${icon}</span>
      <span>${message}</span>
    </div>
  `;

  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(10px)';
    toast.style.transition = 'all 0.3s ease';
    setTimeout(() => toast.remove(), 300);
  }, duration);
}

// Format Currency VND
function formatCurrency(amount) {
  if (amount == null) return '0 ₫';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
}

// Format Date YYYY-MM-DD to DD/MM/YYYY
function formatDate(dateStr) {
  if (!dateStr) return '-';
  const d = new Date(dateStr);
  if (isNaN(d.getTime())) return dateStr;
  return d.toLocaleDateString('vi-VN');
}
