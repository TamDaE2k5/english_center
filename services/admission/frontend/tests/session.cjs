const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');

const frontend = path.resolve(__dirname, '..');
const repo = path.resolve(frontend, '../../..');
const auth = '/api/admission/auth/';
const rooms = '/api/admission/rooms';
const user = {user_id: 23, email: 'qa@example.test', full_name: 'QA Staff', role: 'STAFF'};
const now = Date.UTC(2030, 0, 1);
const jwt = (claims, signature) => 'e30.' + Buffer.from(JSON.stringify(claims)).toString('base64url') + '.' + signature;
const access = (signature, exp = now / 1000 + 900) => jwt({sub:'23', exp, role:'STAFF'}, signature);
const refresh = signature => jwt({sub:'23', exp:now / 1000 + 3600, token_type:'REFRESH'}, signature);
const initial = {access_token:access('initial'), refresh_token:refresh('initial'), user};
const rotated = {access_token:access('rotated'), refresh_token:refresh('rotated')};
const ok = data => Response.json({success:true, data});
const fail = (status, message) => Response.json({success:false, error:{message}}, {status});

function browser({fetch, local = new Map(), tab = new Map(), locks, time = {now}} = {}) {
  const events = new Map();
  const values = new Map();
  const calls = [];
  const location = {pathname:'/admission/account.html', href:'/admission/account.html', hash:'',
    replace(value) { this.href = value; }};
  const messages = [];
  const storage = items => ({
    getItem: key => items.get(key) ?? null,
    setItem: (key, value) => items.set(key, String(value)),
    removeItem: key => items.delete(key)
  });
  const el = id => {
    if (!values.has(id)) values.set(id, {id, textContent:'', dataset:{}, addEventListener(name, fn) { this[name] = fn; }});
    return values.get(id);
  };
  const form = el('login');
  const change = el('change');
  change.hidden = true;
  for (const item of [form, change]) {
    item.querySelector = () => el(item.id + '-submit');
    item.reset = () => {};
  }
  const context = vm.createContext({
    atob, URL, URLSearchParams, Response, Headers, AbortSignal, crypto, TextEncoder,
    Date: class extends Date { static now() { return time.now; } },
    navigator: {locks: locks === undefined ? {request: (_key, _options, callback) => Promise.resolve().then(callback)} : locks},
    window: {location, addEventListener: (name, callback) => events.set(name, callback)},
    document: {documentElement:{dataset:{}}, body:{}, getElementById:el,
      querySelectorAll: selector => selector === '.account-form' ? [form, change] : [], addEventListener(){}},
    console: {error: (...args) => messages.push(args.join(' '))},
    setTimeout(){}, clearTimeout(){}, history:{replaceState(){}}, confirm: () => true,
    localStorage: storage(local), sessionStorage: storage(tab),
    FormData: function (item) {
      return item === change ? [['current_password','qa-input-password'],['new_password','qa-changed-password']] :
        [['email',user.email],['password','qa-input-password']];
    },
    fetch: async (url, options) => {
      const request = {url, body:options?.body ? JSON.parse(options.body) : null, token:new Headers(options?.headers).get('Authorization')};
      calls.push(request);
      return fetch(request);
    }
  });
  for (const file of ['shared/js/auth.js', 'shared/js/api.js']) {
    vm.runInContext(fs.readFileSync(path.join(repo, file), 'utf8'), context);
  }
  context.showToast = (type, message) => messages.push({type, message});
  // Đọc đúng script mà trang account tải, bao gồm adapter khi bản sửa được thêm.
  const html = fs.readFileSync(path.join(frontend, 'account.html'), 'utf8');
  for (const [, file] of html.matchAll(/src="\/admission\/(js\/[^"]+)"/g)) {
    vm.runInContext(fs.readFileSync(path.join(frontend, file), 'utf8'), context);
  }
  return {context, calls, local, tab, location, messages, events, el,
    login: () => form.submit({preventDefault(){}}), changePassword: () => change.submit({preventDefault(){}})};
}

test('Login hợp lệ lưu cặp token theo tab và chuẩn hóa user_id', async () => {
  const page = browser({fetch: () => ok(initial)});
  await page.login();
  assert.equal(page.location.href, '/admission/test.html');
  assert.equal(page.context.getCurrentUser()?.id, user.user_id);
  assert.ok(!JSON.stringify([...page.local]).includes(initial.access_token));
  assert.ok(!JSON.stringify([...page.local]).includes(initial.refresh_token));
  assert.ok(JSON.stringify([...page.tab]).includes(initial.refresh_token));
  assert.ok(!JSON.stringify([...page.tab]).includes('qa-input-password'));
});

test('Nhân sự lần đầu ở màn đổi mật khẩu rồi phải đăng nhập lại', async () => {
  const page = browser({fetch: request => request.url === auth + 'login' ?
    ok({...initial, user:{...user, must_change_password:true}}) :
    Response.json({success:true, message:'Đã đổi mật khẩu. Vui lòng đăng nhập lại'})});
  await page.login();
  assert.equal(page.location.href, '/admission/account.html');
  assert.equal(page.el('change').hidden, false);
  assert.match(page.el('accountMessage').textContent, /đổi mật khẩu/i);
  await page.changePassword();
  assert.equal(page.context.isAuthenticated(), false);
  assert.equal(page.el('login').hidden, false);
  assert.match(page.el('accountMessage').textContent, /đăng nhập lại/i);
});

test('Login thiếu token hoặc user không lưu phiên và không redirect thành công', async () => {
  for (const payload of [{...initial, refresh_token:undefined}, {...initial, user:undefined}]) {
    const page = browser({fetch: () => ok(payload)});
    await page.login();
    assert.equal(page.location.href, '/admission/account.html');
    assert.equal(page.context.isAuthenticated(), false);
    assert.match(page.el('accountMessage').textContent, /phiên|dữ liệu/i);
  }
});

test('Login 401, 403 hoặc 500 hiện lỗi tại form, không refresh hoặc logout', async () => {
  for (const [status, message] of [[401,'Sai mật khẩu'],[403,'Tài khoản đã khóa'],[500,'Không thể đăng nhập']]) {
    const page = browser({fetch: () => fail(status, message)});
    await page.login();
    assert.equal(page.location.href, '/admission/account.html');
    assert.equal(page.el('accountMessage').textContent, message);
    assert.equal(page.calls.length, 1);
    assert.equal(page.context.isAuthenticated(), false);
  }
});

test('Hai API 401 cùng dùng một lần rotation rồi retry với access token mới', async () => {
  const page = browser({fetch: request => {
    if (request.url === auth + 'login') return ok(initial);
    if (request.url === auth + 'refresh') {
      assert.ok(request.body.refresh_token === initial.refresh_token);
      return ok(rotated);
    }
    return request.token === 'Bearer ' + rotated.access_token ? ok([]) : fail(401, 'Expired');
  }});
  await page.login();
  await Promise.all([page.context.apiGet(rooms), page.context.apiGet(rooms)]);
  assert.equal(page.calls.filter(r => r.url === auth + 'refresh').length, 1);
  assert.equal(page.calls.filter(r => r.url === rooms && r.token === 'Bearer ' + rotated.access_token).length, 2);
});

test('403 giữ phiên và thông báo lỗi quyền, không rotation', async () => {
  const page = browser({fetch: request => request.url === auth + 'login' ? ok(initial) : fail(403, 'Không đủ quyền')});
  await page.login();
  await assert.rejects(page.context.apiGet(rooms), /Không đủ quyền/);
  assert.equal(page.context.isAuthenticated(), true);
  assert.equal(page.calls.filter(r => r.url === auth + 'refresh').length, 0);
});

test('Đổi mật khẩu sai mật khẩu hiện tại giữ phiên và không gửi lại request', async () => {
  const page = browser({fetch: request => request.url === auth + 'login' ? ok(initial) :
    Response.json({success:false, error:{code:'AUTH_INVALID_CREDENTIALS', message:'Sai mật khẩu hiện tại'}}, {status:401})});
  await page.login();
  await assert.rejects(page.context.apiPost(auth + 'change-password', {}), /Sai mật khẩu hiện tại/);
  assert.equal(page.context.isAuthenticated(), true);
  assert.equal(page.calls.length, 2);
});

test('Logout thu hồi refresh token và xóa phiên phía trình duyệt', async () => {
  const page = browser({fetch: request => request.url === auth + 'login' ? ok(initial) : ok(null)});
  await page.login();
  await page.context.logout();
  const call = page.calls.find(r => r.url === auth + 'logout');
  assert.ok(call && call.token === 'Bearer ' + initial.access_token && call.body.refresh_token === initial.refresh_token);
  assert.equal(page.context.isAuthenticated(), false);
  assert.ok(!JSON.stringify([...page.tab]).includes(initial.refresh_token));
  assert.equal(page.location.href, '/admission/account.html');
});

test('API với access hợp lệ dùng token hiện hành, không refresh', async () => {
  const page = browser({fetch: request => request.url === auth + 'login' ? ok(initial) : ok([])});
  await page.login();
  await page.context.apiGet(rooms);
  assert.equal(page.calls.length, 2);
  assert.ok(page.calls[1].token === 'Bearer ' + initial.access_token);
});

test('Access hết hạn không được gửi; retry 401 sau rotation kết thúc phiên', async () => {
  const time = {now};
  const page = browser({time, fetch: request => {
    if (request.url === auth + 'login') return ok({...initial, access_token:access('short', now / 1000 + 1)});
    if (request.url === auth + 'refresh') return ok(rotated);
    return fail(401, 'Hết hạn');
  }});
  await page.login();
  time.now += 2000;
  assert.equal(page.context.isAuthenticated(), false);
  await assert.rejects(page.context.apiGet(rooms));
  assert.equal(page.calls.length, 3);
  assert.ok(page.calls[2].token === 'Bearer ' + rotated.access_token);
  assert.equal(page.context.getToken(), null);
});

test('Refresh lỗi mạng, bị revoke hoặc payload sai thì xóa phiên, không lặp request', async () => {
  for (const refreshResult of [() => { throw Error('Offline'); }, () => fail(401, 'Revoked'), () => ok({access_token:rotated.access_token})]) {
    const page = browser({fetch: request => {
      if (request.url === auth + 'login') return ok(initial);
      if (request.url === auth + 'refresh') return refreshResult();
      return fail(401, 'Expired');
    }});
    await page.login();
    await assert.rejects(page.context.apiGet(rooms), /hết hạn/);
    assert.equal(page.context.getToken(), null);
    assert.equal(page.calls.filter(r => r.url === auth + 'refresh').length, 1);
    assert.equal(page.calls.filter(r => r.url === rooms).length, 1);
  }
});

test('401 đến muộn dùng cặp đã rotate, rồi lần rotation tiếp theo dùng refresh token mới', async () => {
  let release;
  const late = new Promise(resolve => { release = resolve; });
  let oldRequests = 0;
  let rotations = 0;
  let replaceAgain = false;
  const latest = {access_token:access('latest'), refresh_token:refresh('latest')};
  const page = browser({fetch: request => {
    if (request.url === auth + 'login') return ok(initial);
    if (request.url === auth + 'refresh') {
      rotations++;
      assert.ok(request.body.refresh_token === (rotations === 1 ? initial.refresh_token : rotated.refresh_token));
      return ok(rotations === 1 ? rotated : latest);
    }
    if (request.token === 'Bearer ' + initial.access_token) return ++oldRequests === 1 ? fail(401, 'Expired') : late;
    if (replaceAgain && request.token === 'Bearer ' + rotated.access_token) return fail(401, 'Expired again');
    return ok([]);
  }});
  await page.login();
  const first = page.context.apiGet(rooms);
  const second = page.context.apiGet(rooms);
  await first;
  release(fail(401, 'Late 401'));
  await second;
  assert.equal(rotations, 1);
  replaceAgain = true;
  await page.context.apiGet(rooms);
  assert.equal(rotations, 2);
  assert.ok(page.context.getToken() === latest.access_token);
});

test('Logout access hết hạn xoay vòng một lần rồi thu hồi cặp mới', async () => {
  const time = {now};
  const page = browser({time, fetch: request => {
    if (request.url === auth + 'login') return ok({...initial, access_token:access('short', now / 1000 + 1)});
    return request.url === auth + 'refresh' ? ok(rotated) : ok(null);
  }});
  await page.login();
  time.now += 2000;
  await page.context.logout();
  assert.deepEqual(page.calls.map(r => r.url), [auth + 'login', auth + 'refresh', auth + 'logout']);
  assert.ok(page.calls[2].token === 'Bearer ' + rotated.access_token);
  assert.ok(page.calls[2].body.refresh_token === rotated.refresh_token);
  assert.equal(page.context.isAuthenticated(), false);
});

test('Logout offline xóa local ngay và giữ thông báo chưa xác nhận máy chủ', async () => {
  const page = browser({fetch: request => {
    if (request.url === auth + 'login') return ok(initial);
    throw Error('Offline');
  }});
  await page.login();
  const signingOut = page.context.logout();
  assert.equal(page.context.getToken(), null);
  assert.ok(!JSON.stringify([...page.tab]).includes(initial.refresh_token));
  await signingOut;
  assert.match(page.context.admissionTakeNotice(), /Chưa xác nhận/);
  assert.equal(page.calls.length, 2);
});

test('Logout chờ rotation đang gửi và chỉ dùng cặp mới để revoke, response cũ không mở lại UI', async () => {
  let release;
  let started;
  const rotation = new Promise(resolve => { release = resolve; });
  const refreshing = new Promise(resolve => { started = resolve; });
  const page = browser({fetch: request => {
    if (request.url === auth + 'login') return ok(initial);
    if (request.url === auth + 'refresh') { started(); return rotation; }
    if (request.url === auth + 'logout') return ok(null);
    return fail(401, 'Expired');
  }});
  await page.login();
  const result = assert.rejects(page.context.apiGet(rooms));
  await refreshing;
  const signingOut = page.context.logout();
  assert.equal(page.context.isAuthenticated(), false);
  release(ok(rotated));
  await Promise.all([signingOut, result]);
  assert.ok(page.calls.find(r => r.url === auth + 'logout')?.body.refresh_token === rotated.refresh_token);
  assert.equal(page.calls.filter(r => r.url === rooms).length, 1);
  assert.equal(page.context.getToken(), null);
  assert.ok(!JSON.stringify([...page.tab]).includes(rotated.refresh_token));
});

test('Hai tab giữ cùng snapshot logout một lần, không trao đổi token qua localStorage', async () => {
  let queued = Promise.resolve();
  const locks = {request: (_key, _options, callback) => {
    const task = queued.then(callback);
    queued = task.catch(() => {});
    return task;
  }};
  let revokes = 0;
  const local = new Map();
  const fetch = request => {
    if (request.url === auth + 'login') return ok(initial);
    if (request.url === auth + 'me') return ok({...user, id:user.user_id});
    if (request.url === auth + 'logout') { revokes++; return ok(null); }
    throw Error('Unexpected request');
  };
  const first = browser({fetch, local, locks});
  await first.login();
  const copy = browser({fetch, local, locks, tab:new Map(first.tab)});
  await copy.context.admissionWhenReady(() => {});
  assert.equal(copy.context.isAuthenticated(), true);
  await Promise.all([first.context.logout(), copy.context.logout()]);
  assert.equal(revokes, 1);
  assert.equal(first.context.getToken(), null);
  assert.equal(copy.context.getToken(), null);
  assert.ok(!JSON.stringify([...local]).includes(initial.refresh_token));
});

test('Marker pending còn lại sau tab gián đoạn ngăn gửi lại refresh token cũ', async () => {
  const page = browser({fetch: request => request.url === auth + 'login' ? ok(initial) : fail(401, 'Expired')});
  await page.login();
  const marker = [...page.local.keys()].find(key => key.startsWith('admission_session_revision:'));
  page.local.set(marker, 'pending:0');
  await assert.rejects(page.context.apiGet(rooms), /hết hạn/);
  assert.equal(page.calls.length, 2);
  assert.equal(page.context.getToken(), null);
});

test('Trình duyệt thiếu Web Locks không gửi refresh có nguy cơ replay', async () => {
  const page = browser({locks:null, fetch: request => request.url === auth + 'login' ? ok(initial) : fail(401, 'Expired')});
  await page.login();
  await assert.rejects(page.context.apiGet(rooms), /hết hạn/);
  assert.equal(page.calls.length, 2);
  assert.equal(page.context.getToken(), null);
});

test('Response rotation từ phiên trước không ghi đè lần đăng nhập mới', async () => {
  let release;
  let started;
  let loginCount = 0;
  const delayed = new Promise(resolve => { release = resolve; });
  const sent = new Promise(resolve => { started = resolve; });
  const latest = {access_token:access('new-login'), refresh_token:refresh('new-login'), user};
  const page = browser({fetch: request => {
    if (request.url === auth + 'login') return ok(++loginCount === 1 ? initial : latest);
    if (request.url === auth + 'refresh') { started(); return delayed; }
    return fail(401, 'Expired');
  }});
  await page.login();
  const obsolete = assert.rejects(page.context.apiGet(rooms));
  await sent;
  await page.login();
  release(ok(rotated));
  await obsolete;
  assert.ok(page.context.getToken() === latest.access_token);
  assert.ok(!JSON.stringify([...page.tab]).includes(rotated.refresh_token));
});
