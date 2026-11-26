/**
 * API Client & Toast Notification Helpers - Hệ thống Trung tâm Anh ngữ
 * shared/js/api.js
 * Chuẩn hóa gọi API, gắn JWT Bearer token, xử lý lỗi 401/snake_case và thông báo Toast.
 */

let _authRedirectScheduled = false;

/**
 * Trích xuất thông báo lỗi thân thiện từ cấu trúc JSON chuẩn của các microservice.
 * Hỗ trợ cả {error: {code, message}} và FastAPI detail.
 * @param {object|null} data Payload JSON phản hồi từ server
 * @param {number} status HTTP status code
 * @returns {string} Thông báo lỗi rõ ràng
 */
function extractApiErrorMessage(data, status) {
  if (data && typeof data === 'object') {
    if (data.error && typeof data.error === 'object' && data.error.message) {
      return data.error.message;
    }
    if (data.detail && typeof data.detail === 'object' && data.detail.message) {
      return data.detail.message;
    }
    if (typeof data.detail === 'string' && data.detail.trim()) {
      return data.detail;
    }
    if (typeof data.message === 'string' && data.message.trim()) {
      return data.message;
    }
  }
  return `Lỗi HTTP ${status}`;
}

/**
 * Thực thi fetch có gắn JWT và xử lý 401/lỗi chung.
 * @param {string} endpoint Đường dẫn API (vd: /api/student/students)
 * @param {object} options Tùy chọn fetch (method, body, headers, skipAuthRedirect, throwOnError)
 * @returns {Promise<object>} Kết quả JSON từ server
 */
async function _performApiFetch(endpoint, options = {}) {
  const { skipAuthRedirect = false, throwOnError = true, ...fetchOptions } = options;
  const url = endpoint;

  const headers = {
    'Content-Type': 'application/json',
    ...(fetchOptions.headers || {})
  };

  const token = typeof getToken === 'function' ? getToken() : localStorage.getItem('ec_token');
  if (token && token !== 'undefined' && token !== 'null') {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(url, {
    ...fetchOptions,
    headers
  });

  // Xử lý hết hạn phiên (401 Unauthorized)
  if (response.status === 401) {
    const errPayload = await response.json().catch(() => null);
    const msg = extractApiErrorMessage(errPayload, 401) || 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.';
    if (!skipAuthRedirect) {
      showToast('error', 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
      if (!_authRedirectScheduled && typeof logout === 'function') {
        _authRedirectScheduled = true;
        setTimeout(() => logout(), 1200);
      }
    }
    if (throwOnError) {
      throw new Error(msg);
    }
    return errPayload && typeof errPayload === 'object'
      ? errPayload
      : { success: false, error: { code: 'AUTH_UNAUTHORIZED', message: msg } };
  }

  // Xử lý 204 No Content
  if (response.status === 204) {
    return { success: true, data: null };
  }

  const data = await response.json().catch(() => null);

  if (!response.ok || (data && data.success === false)) {
    const errorMsg = extractApiErrorMessage(data, response.status);
    if (throwOnError) {
      showToast('error', errorMsg);
      throw new Error(errorMsg);
    }
    if (data && typeof data === 'object' && 'success' in data) {
      return data;
    }
    return {
      success: false,
      error: {
        code: (data && data.error && data.error.code) || `HTTP_${response.status}`,
        message: errorMsg
      }
    };
  }

  return data ?? { success: true, data: null };
}

/**
 * Wrapper gọi API chuẩn (ném Error và hiển thị Toast khi HTTP lỗi hoặc success === false).
 * @param {string} endpoint Đường dẫn API
 * @param {object} options Tùy chọn fetch
 * @returns {Promise<object>} Dữ liệu JSON thành công
 */
async function apiRequest(endpoint, options = {}) {
  try {
    return await _performApiFetch(endpoint, { ...options, throwOnError: true });
  } catch (err) {
    console.error('API Request Error:', err);
    throw err;
  }
}

/**
 * Gọi API GET.
 * @param {string} endpoint Đường dẫn API
 * @returns {Promise<object>} Dữ liệu JSON trả về
 */
async function apiGet(endpoint) {
  return apiRequest(endpoint, { method: 'GET' });
}

/**
 * Gọi API POST với body JSON.
 * @param {string} endpoint Đường dẫn API
 * @param {object} body Payload JSON (snake_case)
 * @returns {Promise<object>} Dữ liệu JSON trả về
 */
async function apiPost(endpoint, body) {
  return apiRequest(endpoint, {
    method: 'POST',
    body: JSON.stringify(body)
  });
}

/**
 * Gọi API PUT với body JSON.
 * @param {string} endpoint Đường dẫn API
 * @param {object} body Payload JSON (snake_case)
 * @returns {Promise<object>} Dữ liệu JSON trả về
 */
async function apiPut(endpoint, body) {
  return apiRequest(endpoint, {
    method: 'PUT',
    body: JSON.stringify(body)
  });
}

/**
 * Gọi API DELETE.
 * @param {string} endpoint Đường dẫn API
 * @returns {Promise<object>} Dữ liệu JSON trả về
 */
async function apiDelete(endpoint) {
  return apiRequest(endpoint, { method: 'DELETE' });
}

/**
 * Namespace window.api phục vụ các phân hệ dùng pattern `window.api.request(...)` (như Learning)
 * trả về object { success, data, error } nhất quán mà không làm sập luồng xử lý res.success === false.
 */
const api = {
  request: async (endpoint, options = {}) => {
    try {
      return await _performApiFetch(endpoint, { ...options, throwOnError: false });
    } catch (err) {
      return {
        success: false,
        error: {
          code: 'NETWORK_ERROR',
          message: err?.message || 'Không thể kết nối tới máy chủ'
        }
      };
    }
  },
  get: apiGet,
  post: apiPost,
  put: apiPut,
  delete: apiDelete
};
if (typeof window !== 'undefined') {
  window.api = api;
}

/**
 * Hiển thị thông báo Toast góc phải dưới màn hình (tự escape HTML tránh XSS).
 * @param {'info'|'success'|'error'|'warning'|'danger'} type Loại thông báo
 * @param {string} message Nội dung thông báo
 * @param {number} duration Thời gian hiển thị (ms)
 */
function showToast(type = 'info', message = '', duration = 3000) {
  if (typeof document === 'undefined') return;
  let container = document.querySelector('.toast-container');
  if (!container) {
    container = document.createElement('div');
    container.className = 'toast-container';
    document.body.appendChild(container);
  }

  const normalizedType = type === 'danger' ? 'error' : type;
  const toast = document.createElement('div');
  toast.className = `toast toast-${normalizedType}`;

  let icon = 'ℹ️';
  if (normalizedType === 'success') icon = '✅';
  if (normalizedType === 'error') icon = '❌';
  if (normalizedType === 'warning') icon = '⚠️';

  const row = document.createElement('div');
  row.style.cssText = 'display: flex; align-items: center; gap: 8px;';
  const iconSpan = document.createElement('span');
  iconSpan.textContent = icon;
  const msgSpan = document.createElement('span');
  msgSpan.textContent = String(message ?? '');
  row.appendChild(iconSpan);
  row.appendChild(msgSpan);
  toast.appendChild(row);

  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(10px)';
    toast.style.transition = 'all 0.3s ease';
    setTimeout(() => toast.remove(), 300);
  }, duration);
}

/**
 * Định dạng tiền tệ VND.
 * @param {number|string|null} amount Số tiền cần định dạng
 * @returns {string} Chuỗi tiền tệ VND
 */
function formatCurrency(amount) {
  if (amount == null || isNaN(Number(amount))) return '0 ₫';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(Number(amount));
}

/**
 * Định dạng ngày ISO (YYYY-MM-DD) sang chuẩn Việt Nam (DD/MM/YYYY).
 * @param {string|null} dateStr Chuỗi ngày cần định dạng
 * @returns {string} Chuỗi ngày DD/MM/YYYY hoặc '-'
 */
function formatDate(dateStr) {
  if (!dateStr) return '-';
  const d = new Date(dateStr);
  if (isNaN(d.getTime())) return dateStr;
  return d.toLocaleDateString('vi-VN');
}
