const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const test = require('node:test');
const root = path.resolve(__dirname, '..');

function page(name, apiGet = async () => ({data: []})) {
  const elements = new Map();
  const el = id => {
    if (!elements.has(id)) {
      elements.set(id, {
        id,
        innerHTML: '',
        textContent: '',
        value: '',
        style: {},
        classList: { add(){}, remove(){}, toggle(){} },
        handlers: {},
        addEventListener(evt, fn) { this.handlers[evt] = fn; },
        setAttribute(){},
        getAttribute(){},
        reset(){}
      });
    }
    return elements.get(id);
  };

  const context = vm.createContext({
    console, Intl, Date, Math, JSON, URLSearchParams, Number, String,
    setInterval: () => 1, clearInterval(){}, setTimeout(){}, confirm: () => true, prompt: () => '3000000',
    localStorage: { getItem: () => null }, sessionStorage: { setItem(){} },
    window: { location: { pathname: '/finance/' + name, hash: '' } },
    document: {
      getElementById: el,
      querySelectorAll: () => [],
      documentElement: { dataset: {}, classList: { add(){}, remove(){} } },
      body: { dataset: {}, classList: { add(){}, remove(){} } },
      addEventListener(){}
    },
    getToken: () => 'qa-token',
    isAuthenticated: () => true,
    getCurrentUser: () => ({ role: 'STAFF', id: 2 }),
    showToast(){},
    formatCurrency: (val) => `${Number(val || 0).toLocaleString('vi-VN')} ₫`,
    formatDate: (d) => d || '-',
    apiGet,
    apiPost: async () => ({ success: true, data: {} }),
    apiPut: async () => ({ success: true, data: {} }),
    apiDelete: async () => ({ success: true })
  });

  const helper = path.join(root, 'js', 'finance-ui.js');
  if (fs.existsSync(helper)) {
    vm.runInContext(fs.readFileSync(helper, 'utf8'), context);
  }
  const html = fs.readFileSync(path.join(root, name), 'utf8');
  for (const m of html.matchAll(/<script(?![^>]*\bsrc=)[^>]*>([\s\S]*?)<\/script>/g)) {
    vm.runInContext(m[1], context);
  }
  return { context, el };
}

test('Tên khóa học và ghi chú biểu phí được escape HTML chống XSS trong tuition.html', async () => {
  const { context, el } = page('tuition.html', async (url) => {
    if (url.includes('tuition-fees')) {
      return {
        data: [{
          course_id: 1,
          course_name: '<img src=x onerror=alert(1)>',
          amount: 6500000,
          discount_pct: 10,
          note: '<script>alert(2)</script>',
          is_active: true
        }]
      };
    }
    return { data: [] };
  });
  await context.loadTuitionFees();
  assert.ok(!el('tuition-tbody').innerHTML.includes('<img'));
  assert.ok(!el('tuition-tbody').innerHTML.includes('<script>'));
  assert.ok(el('tuition-tbody').innerHTML.includes('&lt;img'));
});

test('Danh sách hóa đơn hiển thị đúng trạng thái và escape mã hóa đơn trong invoices.html', async () => {
  const { context, el } = page('invoices.html', async (url) => {
    if (url === '/api/finance/invoices') {
      return {
        data: [{
          id: 9,
          invoice_code: 'INV-2026-009<img src=x>',
          student_id: 4,
          amount: 8500000,
          discount_applied: 850000,
          final_amount: 7650000,
          paid_amount: 3000000,
          remaining_amount: 4650000,
          due_date: '2026-11-15',
          status: 'PARTIAL'
        }]
      };
    }
    return { data: [] };
  });
  await context.loadInvoices();
  const html = el('invoices-tbody').innerHTML;
  assert.ok(!html.includes('<img'));
  assert.ok(html.includes('PARTIAL'));
  assert.ok(html.includes('payCash(9, 4650000)'));
});

test('Dashboard BI Tài chính hiển thị đúng KPI thực thu, dư nợ chờ thu và chặn response cũ', async () => {
  const { context, el } = page('bi-dashboard.html', async (url) => {
    if (url.includes('admission-metrics')) {
      return { data: { realized_revenue: 13000000, pipeline_revenue: 7650000 } };
    }
    if (url.includes('refund-summary')) {
      return { data: { wallets: { total_reserved_balance: 2500000 }, refunds: { total_disbursed_amount: 1200000 } } };
    }
    if (url.includes('voucher-campaigns')) {
      return { data: [{ code: 'KHAI_GIANG<img src=x>', campaign_name: 'Khai giảng', current_uses: 2, max_uses: 50, usage_rate_pct: 4, total_invoices: 2, total_discount_given: 850000, actual_cash_collected: 6500000 }] };
    }
    return { data: [] };
  });
  await context.loadFinanceDashboard();
  assert.ok(el('kpiNetCash').textContent.includes('13.000.000'));
  assert.ok(el('kpiPipelineRevenue').textContent.includes('7.650.000'));
  assert.ok(!el('voucher-bi-tbody').innerHTML.includes('<img'));
});
