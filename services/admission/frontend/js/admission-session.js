/** Phiên Admission theo tab. Storage liên tab chỉ chứa ID điều phối ngẫu nhiên và số phiên bản. */
(() => {
  const key = 'admission_session';
  const noticeKey = 'admission_session_notice';
  const auth = '/api/admission/auth/';
  const loginPage = '/admission/account.html';
  const expired = 'Phiên Admission đã hết hạn. Vui lòng đăng nhập lại.';
  const roles = ['ADMIN', 'STAFF', 'STUDENT', 'TEACHER'];
  let refreshFlight;
  let refreshOwner;
  let logoutFlight;
  let ending = false;
  let timer;

  function claims(token) {
    if (typeof token !== 'string' || !/^[\w-]+\.[\w-]+\.[\w-]+$/.test(token)) return null;
    const parsed = parseJwt(token);
    return Number.isFinite(parsed?.exp) ? parsed : null;
  }

  function usable(token) {
    const parsed = claims(token);
    return parsed && parsed.token_type !== 'REFRESH' && parsed.exp * 1000 > Date.now();
  }

  function profile(data) {
    const id = Number(data?.id ?? data?.user_id);
    const name = data?.full_name ?? data?.name;
    if (!Number.isInteger(id) || id < 1 || typeof name !== 'string' || !name.trim()
        || typeof data?.email !== 'string' || !data.email.trim() || !roles.includes(data.role)) return null;
    return {id, name, email: data.email, role: data.role, mustChangePassword: data.must_change_password === true};
  }

  function tokenPair(data, userId) {
    const access = claims(data?.access_token);
    const refresh = claims(data?.refresh_token);
    if (!usable(data?.access_token) || !roles.includes(access.role) || Number(access.sub) !== userId
        || refresh?.token_type !== 'REFRESH' || Number(refresh.sub) !== userId
        || refresh.exp * 1000 <= Date.now()) throw new Error('Dữ liệu phiên đăng nhập không hợp lệ.');
    return {access: data.access_token, refresh: data.refresh_token, role: access.role,
      mustChangePassword: access.must_change_password};
  }

  function readSession() {
    try {
      const saved = sessionStorage.getItem(key);
      if (saved !== null) return JSON.parse(saved);
      const access = localStorage.getItem('ec_token');
      const user = profile(JSON.parse(localStorage.getItem('ec_user')));
      const token = claims(access);
      // Hỗ trợ access token cũ của trang dùng chung cho đến khi hết hạn.
      if (user && token && token.token_type !== 'REFRESH' && Number(token.sub) === user.id) {
        return {access, user: {...user, role: roles.includes(token.role) ? token.role : user.role,
          mustChangePassword: token.must_change_password === true || user.mustChangePassword}};
      }
    } catch {
      // Storage bị chặn hoặc dữ liệu phiên không đọc được.
    }
    return null;
  }

  let session = readSession();
  let ready = !session?.id;
  const marker = record => 'admission_session_revision:' + record.id;
  const revision = record => String(record.revision);
  const pending = record => 'pending:' + record.revision;

  function sameSession(left, right) {
    return Boolean(left && right && (left.id ? left.id === right.id : !right.id && left.access === right.access));
  }

  function current(record) {
    if (!record) return false;
    try {
      if (!record.id) return localStorage.getItem('ec_token') === record.access;
      const value = localStorage.getItem(marker(record));
      return value === revision(record) || value === pending(record);
    } catch {
      return false;
    }
  }

  function publish() {
    globalThis.admissionSessionChanged?.();
    redirectForPasswordChange();
  }

  let redirectingForPasswordChange = false;
  function redirectForPasswordChange() {
    if (redirectingForPasswordChange) return true;
    if (!visibleSession()?.user.mustChangePassword || window.location.pathname === loginPage) return false;
    redirectingForPasswordChange = true;
    if (document.body) document.body.hidden = true;
    window.location.replace(loginPage);
    return true;
  }

  function showNotice(message, navigate) {
    if (navigate || window.location.pathname !== loginPage) {
      try { sessionStorage.setItem(noticeKey, message); } catch { /* Vẫn chuyển về đăng nhập. */ }
      if (document.body) document.body.hidden = true;
      window.location.replace(loginPage);
    } else {
      const target = document.getElementById('accountMessage');
      if (target) target.textContent = message;
    }
  }

  function clearTab() {
    try { sessionStorage.setItem(key, 'null'); } catch { /* Phiên trong bộ nhớ vẫn bị vô hiệu hóa. */ }
  }

  // Xóa token theo tab và chỉ bỏ marker nếu còn thuộc đúng phiên bản đã biết.
  function clearSession(record = session, unlink = true) {
    if (record && !sameSession(record, session)) return;
    if (unlink && current(record)) {
      if (record.id) localStorage.removeItem(marker(record));
      else {
        localStorage.removeItem('ec_token');
        localStorage.removeItem('ec_user');
      }
    }
    session = null;
    clearTab();
    clearTimeout(timer);
    publish();
  }

  function forgetSession(record, message = expired, unlink = true) {
    if (!sameSession(record, session) || ending) return;
    clearSession(record, unlink);
    showNotice(message, false);
  }

  async function authPost(action, refresh, access) {
    const headers = {'Content-Type': 'application/json'};
    if (access) headers.Authorization = 'Bearer ' + access;
    const response = await fetch(auth + action, {method: 'POST', headers,
      body: JSON.stringify({refresh_token: refresh}), signal: AbortSignal.timeout(8000)});
    const payload = await response.json();
    if (!response.ok || payload?.success !== true) throw new Error(expired);
    return payload.data;
  }

  function withSessionLock(record, operation) {
    return navigator.locks.request(marker(record), {signal: AbortSignal.timeout(10000)}, operation);
  }

  // Web Locks phối hợp các tab sao chép phiên. Marker pending ngăn phát lại sau lỗi mạng hoặc tab bị đóng.
  function rotate(record) {
    if (refreshFlight && sameSession(refreshOwner, record)) return refreshFlight;
    const operation = async () => {
      if (!record?.id || !navigator.locks) throw new Error(expired);
      return withSessionLock(record, async () => {
        if (!sameSession(record, session) || ending || localStorage.getItem(marker(record)) !== revision(record)) {
          throw new Error(expired);
        }
        localStorage.setItem(marker(record), pending(record));
        const {role, mustChangePassword, ...pair} = tokenPair(await authPost('refresh', record.refresh), record.user.id);
        if (pair.refresh === record.refresh || !sameSession(record, session)
            || localStorage.getItem(marker(record)) !== pending(record)) throw new Error(expired);
        const user = {...record.user, role, mustChangePassword: mustChangePassword ?? record.user.mustChangePassword};
        const updated = {...record, ...pair, user, revision: record.revision + 1};
        if (!ending) sessionStorage.setItem(key, JSON.stringify(updated));
        session = updated;
        localStorage.setItem(marker(record), revision(updated));
        scheduleExpiry();
        return updated;
      });
    };
    const flight = operation().catch(() => {
      // Nếu marker đã được tab khác cập nhật, chỉ xóa bản sao local.
      forgetSession(record);
      throw new Error(expired);
    }).finally(() => {
      if (refreshFlight === flight) {
        refreshFlight = null;
        refreshOwner = null;
        publish();
      }
    });
    refreshOwner = record;
    refreshFlight = flight;
    publish();
    return flight;
  }

  async function available() {
    const record = session;
    if (ending || !record) return null;
    if (!current(record)) {
      forgetSession(record, 'Phiên trên tab này đã thay đổi. Vui lòng đăng nhập lại.', false);
      throw new Error(expired);
    }
    const waiting = sameSession(refreshOwner, record) ? refreshFlight : null;
    if (!waiting && usable(record.access)) return record;
    const refreshed = await (waiting || rotate(record));
    if (ending || !sameSession(refreshed, session) || !current(refreshed)) throw new Error(expired);
    return refreshed;
  }

  async function rejectedAccess(response, endpoint) {
    if (response.status !== 401) return false;
    if (endpoint !== auth + 'change-password') return true;
    const payload = await response.clone().json().catch(() => null);
    return payload?.error?.code !== 'AUTH_INVALID_CREDENTIALS';
  }

  // Mỗi yêu cầu chỉ retry một lần khi 401. 403 đi thẳng về caller.
  async function admissionFetch(endpoint, options = {}) {
    const original = session;
    const record = await available();
    const send = token => {
      const headers = new Headers(options.headers);
      if (token) headers.set('Authorization', 'Bearer ' + token);
      else headers.delete('Authorization');
      return fetch(endpoint, {...options, headers});
    };
    const check = () => {
      if (ending || (record ? !sameSession(record, session) || !current(session) : session !== null)) {
        throw new Error(expired);
      }
    };
    check();
    let response = await send(record?.access);
    check();
    if (record && await rejectedAccess(response, endpoint)) {
      check();
      if (original?.revision !== record.revision) {
        forgetSession(record);
        return response;
      }
      const replacement = session.revision !== record.revision ? session : await rotate(record);
      check();
      response = await send(replacement.access);
      check();
      if (await rejectedAccess(response, endpoint)) forgetSession(replacement);
    }
    if (record && response.status === 403) {
      const data = await response.clone().json().catch(() => null);
      check();
      if (data?.error?.code === 'AUTH_PASSWORD_CHANGE_REQUIRED') {
        if (session.id) {
          session = {...session, user: {...session.user, mustChangePassword: true}};
          sessionStorage.setItem(key, JSON.stringify(session));
          publish();
        } else {
          forgetSession(record, 'Đăng nhập lại để đổi mật khẩu ban đầu.');
        }
      }
    }
    return response;
  }

  function scheduleExpiry() {
    clearTimeout(timer);
    if (!session || ending) return;
    const delay = (claims(session.access)?.exp ?? 0) * 1000 - Date.now();
    timer = setTimeout(checkSession, Math.max(0, Math.min(delay, 2147483647)));
  }

  function checkSession() {
    if (session && !ending && (!current(session) || !usable(session.access))) {
      publish();
      available().catch(() => {});
    } else scheduleExpiry();
  }

  function saveLogin(data) {
    const user = profile(data?.user);
    if (!user) throw new Error('Dữ liệu phiên đăng nhập không hợp lệ.');
    const {role, mustChangePassword, ...pair} = tokenPair(data, user.id);
    if (role !== user.role || ending) throw new Error('Dữ liệu phiên đăng nhập không hợp lệ.');
    const next = {...pair, user: {...user, mustChangePassword: user.mustChangePassword || mustChangePassword === true},
      revision: 0, id: crypto.getRandomValues(new Uint32Array(4)).join('-')};
    localStorage.setItem(marker(next), revision(next));
    sessionStorage.setItem(key, JSON.stringify(next));
    session = next;
    ready = true;
    scheduleExpiry();
    publish();
  }

  function logout() {
    if (logoutFlight) return logoutFlight;
    const original = session;
    ending = true;
    // Ngừng cấp access và xóa token khỏi storage ngay, kể cả nếu backend mất kết nối.
    clearTab();
    publish();
    logoutFlight = (async () => {
      let message = 'Đã đăng xuất trên tab này. Chưa xác nhận thu hồi phiên trên máy chủ.';
      try {
        if (sameSession(refreshOwner, original)) await refreshFlight;
        const record = session;
        if (record?.id && sameSession(original, record)) {
          const revoke = async () => {
            if (localStorage.getItem(marker(record)) !== revision(record)) return;
            localStorage.removeItem(marker(record));
            const pair = usable(record.access) ? record :
              tokenPair(await authPost('refresh', record.refresh), record.user.id);
            await authPost('logout', pair.refresh, pair.access);
            message = 'Đã đăng xuất và thu hồi phiên làm mới trên máy chủ.';
          };
          if (navigator.locks) await withSessionLock(record, revoke);
          else if (usable(record.access)) await revoke();
        }
      } catch {
        // Chỉ thông báo thu hồi khi backend đã xác nhận.
      } finally {
        clearSession(original);
        showNotice(message, true);
      }
    })();
    return logoutFlight;
  }

  function visibleSession() {
    return ready && !ending && !sameSession(refreshOwner, session) && current(session) && usable(session.access) ? session : null;
  }

  // Phiên Admission khôi phục trong tab được đối chiếu với principal hiện tại trước khi render.
  const restored = session;
  const boot = restored?.id ? (async () => {
    try {
      const response = await admissionFetch(auth + 'me', {signal: AbortSignal.timeout(8000)});
      const data = await response.json();
      const user = profile(data?.data);
      if (!response.ok || data.success !== true || !user || user.id !== restored.user.id) throw new Error(expired);
      if (sameSession(restored, session) && !ending) {
        session = {...session, user};
        sessionStorage.setItem(key, JSON.stringify(session));
      }
    } catch {
      forgetSession(restored);
    } finally {
      ready = true;
      publish();
    }
  })() : null;

  Object.assign(globalThis, {
    getToken: () => visibleSession()?.access ?? null,
    getCurrentUser: () => visibleSession()?.user ?? null,
    isAuthenticated: () => Boolean(visibleSession()),
    logout,
    admissionFetch,
    admissionSaveLogin: saveLogin,
    admissionClearSession: () => clearSession(),
    admissionHasSession: async () => {
      await boot;
      return Boolean(await available());
    },
    admissionWhenReady(callback) {
      const start = () => { if (!redirectForPasswordChange()) return callback(); };
      return boot ? boot.then(start) : start();
    },
    admissionTakeNotice() {
      try {
        const message = sessionStorage.getItem(noticeKey);
        sessionStorage.removeItem(noticeKey);
        return message;
      } catch {
        return null;
      }
    }
  });
  window.addEventListener('storage', event => {
    if (session && (event.key === null || event.key === (session.id ? marker(session) : 'ec_token'))) checkSession();
  });
  window.addEventListener('pageshow', checkSession);
  document.addEventListener('visibilitychange', () => { if (!document.hidden) checkSession(); });
  scheduleExpiry();
})();
