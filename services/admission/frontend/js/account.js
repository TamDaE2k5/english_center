// Token của link email chỉ nằm trong bộ nhớ. Xóa fragment ngay khi đọc.
let accountToken = null;
let passwordAction = null;
const accountMessage = document.getElementById('accountMessage');
const accountLogout = document.getElementById('admissionAccountLogout');
const firstPasswordMessage = 'Bạn cần đổi mật khẩu ban đầu trước khi sử dụng Admission.';
function updateAccountSession() {
  accountLogout.hidden = !isAuthenticated();
  if (getCurrentUser()?.mustChangePassword && !passwordAction && document.getElementById('change').hidden) {
    accountMessage.textContent = firstPasswordMessage;
    accountView('change');
  }
}
accountLogout.addEventListener('click', () => logout());
accountMessage.textContent = admissionTakeNotice() || '';
updateAccountSession();
function accountView(id) { document.querySelectorAll('.account-form').forEach(f => { f.hidden = f.id !== id; }); }
document.querySelectorAll('[data-view]').forEach(b => b.addEventListener('click', () => {
  accountMessage.textContent = '';
  if (b.dataset.view === 'change' && !isAuthenticated()) { accountMessage.textContent = 'Vui lòng đăng nhập trước.'; return; }
  accountView(b.dataset.view);
}));
document.querySelectorAll('.account-form').forEach(form => form.addEventListener('submit', async event => {
  event.preventDefault(); const button = form.querySelector('button[type="submit"]'); button.disabled = true;
  const data = Object.fromEntries(new FormData(form));
  try {
    if (data.new_password && (data.new_password.length < 10 || new TextEncoder().encode(data.new_password).length > 72))
      throw new Error('Mật khẩu cần tối thiểu 10 ký tự và không quá 72 byte UTF-8.');
    if (form.id === 'password' && data.confirm !== data.new_password) throw new Error('Hai mật khẩu không khớp.');
    if (form.id === 'change' && getCurrentUser()?.mustChangePassword && data.current_password === data.new_password)
      throw new Error('Mật khẩu mới phải khác mật khẩu hiện tại.');
    let endpoint = form.id;
    if (form.id === 'forgot') endpoint = 'forgot-password';
    if (form.id === 'change') endpoint = 'change-password';
    if (form.id === 'password') {
      if (!accountToken || !passwordAction) throw new Error('Liên kết không hợp lệ. Vui lòng yêu cầu email mới.');
      endpoint = passwordAction; data.token = accountToken; delete data.confirm;
    }
    const response = await apiPost('/api/admission/auth/' + endpoint, data);
    if (!response) throw new Error('Yêu cầu chưa thành công. Vui lòng kiểm tra lại.');
    if (form.id === 'login') {
      admissionSaveLogin(response.data);
      form.reset();
      if (getCurrentUser()?.mustChangePassword) return;
      window.location.href = '/admission/test.html'; return;
    }
    accountMessage.textContent = response.message || 'Đã xử lý yêu cầu. Vui lòng kiểm tra email.';
    if (form.id === 'password' || form.id === 'change') {
      accountToken = null; passwordAction = null; form.reset();
      admissionClearSession(); accountView('login');
    }
  } catch (error) { accountMessage.textContent = error.message || 'Không thể xử lý yêu cầu.'; }
  finally { button.disabled = false; }
}));
async function consumeEmailLink() {
  const link = new URLSearchParams(window.location.hash.slice(1));
  history.replaceState(null, '', window.location.pathname);
  try {
    if (link.has('verify')) {
      const response = await apiPost('/api/admission/auth/verify-email', {token: link.get('verify')});
      if (!response?.data?.setup_token) throw new Error('Liên kết không hợp lệ hoặc đã hết hạn.');
      accountToken = response.data.setup_token; passwordAction = 'setup-password';
      accountMessage.textContent = 'Email đã được xác minh. Hãy thiết lập mật khẩu.'; accountView('password');
    } else if (link.has('reset')) {
      accountToken = link.get('reset'); passwordAction = 'reset-password'; accountView('password');
    }
  } catch (error) { accountMessage.textContent = error.message; accountView('forgot'); }
}
consumeEmailLink();
window.addEventListener('hashchange', consumeEmailLink);
