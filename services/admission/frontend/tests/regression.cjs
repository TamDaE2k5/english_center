const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const test = require('node:test');
const root = path.resolve(__dirname, '..');

// Chạy JavaScript thực của trang trong DOM tối thiểu; không sao chép logic ứng dụng vào test.
function page(name, apiGet = async () => ({data: []}), overrides = {}) {
  const elements = new Map();
  const documentHandlers = new Map();
  const el = id => {
    if (!elements.has(id)) elements.set(id, {id, innerHTML: '', textContent: '', value: '', style: {},
      classList: {add(){}, remove(){}, toggle(){}}, handlers: {}, addEventListener(name,fn){this.handlers[name]=fn;}, setAttribute(){}, getAttribute(){}, reset(){}});
    return elements.get(id);
  };
  const context = vm.createContext({console, Intl, Date, Math, JSON, URLSearchParams,
    setInterval: () => 1, clearInterval(){}, setTimeout(){}, confirm: () => true,
    localStorage: {getItem: () => null}, sessionStorage: {setItem(){}},
    window: {location: {pathname: '/admission/' + name, hash: ''}, addEventListener(){}},
    document: {getElementById: el, querySelectorAll: () => [],
      documentElement: {dataset: {}, classList: {add(){}, remove(){}}}, body: {dataset: {}, classList: {add(){},remove(){}}},
      addEventListener: (name, callback) => documentHandlers.set(name, callback)},
    getToken: () => 'qa-token', isAuthenticated: () => true,
    getCurrentUser: () => ({role: 'STAFF'}), admissionWhenReady: callback => callback(),
    admissionHasSession: async () => true, showToast(){}, apiGet,
    apiPost: async () => ({success: true}), apiPut: async () => ({success: true}), apiDelete: async () => ({success: true}),
    ...overrides});
  const helper = path.join(root, 'js', 'admission-ui.js');
  if (fs.existsSync(helper)) vm.runInContext(fs.readFileSync(helper, 'utf8'), context);
  const html = fs.readFileSync(path.join(root, name), 'utf8');
  for (const m of html.matchAll(/<script(?![^>]*\bsrc=)[^>]*>([\s\S]*?)<\/script>/g)) vm.runInContext(m[1], context);
  return {context, el, documentHandlers};
}

test('Yêu cầu tiền mặt chỉ báo chờ Finance, không báo đã thu hoặc đã duyệt', async () => {
  const {context,el}=page('enrollments.html'); const messages=[]; const sent=[];
  context.showToast=(type,message)=>messages.push(message);
  context.apiPost=async (url,body)=>{sent.push({url,body});return {success:true,data:{status:'PENDING'}};};
  el('approveEnrollmentId').value='15'; el('approveReceiptNumber').value='RC-QA';
  el('approvePaymentMethod').value='CASH';el('approveAmount').value='5000000';
  await el('manualApproveForm').handlers.submit({preventDefault(){}});
  assert.equal(sent[0].body.payment_method,'CASH');
  assert.ok(messages.some(m=>m.includes('chờ Finance')));
  assert.ok(messages.every(m=>!m.includes('thu học phí thành công')));
  assert.equal(el('btnConfirmApprove').disabled,false);
});

test('Danh sách thể hiện yêu cầu tiền mặt chờ đối soát và giữ trạng thái hủy hoàn phí', async () => {
  const {context,el}=page('enrollments.html',async ()=>({data:[
    {enrollment_id:1,status:'PENDING',remaining_seconds:3600,cash_confirmation:{status:'NEEDS_FINANCE_CONFIRMATION'}},
    {enrollment_id:2,status:'CANCELLED_REFUND_PENDING',remaining_seconds:0}
  ]}));
  await context.loadEnrollments();const html=el('enrollment-table-body').innerHTML;
  assert.ok(html.includes('Chờ Finance')); assert.ok(html.includes('Chờ hoàn phí'));
  assert.ok(!html.includes('openApproveModal(1)')); assert.ok(!html.includes('openApproveModal(2)'));
});

test('Chỉ hiện gia hạn cho nhân viên với PENDING còn deadline hiệu lực', async () => {
  const {context,el}=page('enrollments.html',async ()=>({data:[
    {enrollment_id:1,status:'PENDING',remaining_seconds:3600,hold_expires_at:'2030-01-01T12:00:00'},
    {enrollment_id:2,status:'PENDING',remaining_seconds:0,hold_expires_at:'2020-01-01T12:00:00'},
    {enrollment_id:3,status:'PENDING',remaining_seconds:3600,hold_expires_at:null}
  ]}));
  await context.loadEnrollments();
  let html=el('enrollment-table-body').innerHTML;
  assert.ok(html.includes('openExtendModal(1)'));
  assert.ok(!html.includes('openExtendModal(2)'));assert.ok(!html.includes('openExtendModal(3)'));
  context.getCurrentUser=()=>({role:'STUDENT'});await context.loadEnrollments();
  assert.ok(!el('enrollment-table-body').innerHTML.includes('openExtendModal('));
});

test('Gia hạn gửi lại sau mất phản hồi giữ nguyên deadline gốc và không báo thành công giả', async () => {
  const deadline='2030-01-01T12:00:00';const sent=[],messages=[];
  const {context,el}=page('enrollments.html',async ()=>({data:[{enrollment_id:15,status:'PENDING',remaining_seconds:3600,hold_expires_at:deadline}]}));
  context.apiPost=async (url,body)=>{sent.push({url,body});if(sent.length===1)throw Error('Mất phản hồi');return null;};
  context.showToast=(type,message)=>messages.push({type,message});
  await context.loadEnrollments();context.openExtendModal(15);
  el('extendHours').value='24';el('extendReason').value='Chờ chuyển khoản';
  await el('extendHoldForm').handlers.submit({preventDefault(){}});
  await el('extendHoldForm').handlers.submit({preventDefault(){}});
  assert.equal(sent.length,2);assert.equal(sent[0].body.expected_hold_expires_at,deadline);
  assert.equal(sent[1].body.expected_hold_expires_at,deadline);
  assert.ok(messages.every(m=>m.type!=='success'));assert.equal(el('btnConfirmExtend').disabled,false);
});

test('Câu hỏi hiển thị question_text theo DTO thực', () => {
  const {context, el} = page('test.html');
  vm.runInContext('questionsList = [{id:1, question_text:"Where is the station?", skill_type:"GRAMMAR"}]; showQuestion(0)', context);
  assert.equal(el('questionContent').textContent, 'Where is the station?');
});

function placementPage({storage = new Map(), remaining = 12, delay = 0, submit} = {}) {
  let now = 0;
  let tick;
  const posts = [];
  const gets = [];
  const data = {
    attempt_id: 71, attempt_token: 'qa-capability', status: 'STARTED',
    started_at: '2030-01-01T11:50:00', expires_at: '2030-01-01T12:00:00',
    server_now: new Date(Date.UTC(2030, 0, 1, 12, 0, -remaining)).toISOString().slice(0, -1),
    questions: [{id: 8, question_text: 'Where is the station?', skill_type: 'GRAMMAR'}]
  };
  const {context, el, documentHandlers} = page('test.html', async url => {
    gets.push(url);
    return {data: url.endsWith('/sets') ? [] : data};
  }, {
    Date: class extends Date { static now() { return 0; } },
    performance: {now: () => now},
    sessionStorage: {getItem: key => storage.get(key) ?? null, setItem: (key, value) => storage.set(key, value),
      removeItem: key => storage.delete(key)},
    getCurrentUser: () => ({id: 3, role: 'STUDENT', email: 'qa@example.test'}),
    setInterval: callback => { tick = callback; return 1; },
    clearInterval: () => { tick = undefined; },
    apiPost: async (url, body) => {
      posts.push({url, body});
      if (url.endsWith('/submit')) return submit?.();
      now += delay;
      return {data};
    }
  });
  el('candidateTestSet').value = '1';
  return {context, el, posts, gets, storage, data,
    start: () => el('introForm').handlers.submit({preventDefault(){}}),
    advance: ms => { now += ms; tick?.(); },
    returnToTab: ms => { now += ms; documentHandlers.get('visibilitychange')?.(); }};
}

test('Countdown lấy giờ server, trừ độ trễ và bắt kịp khi timer bị tạm dừng', async () => {
  const exam = placementPage({delay: 4000});
  await exam.start();
  assert.equal(exam.el('examTimer').textContent, '00:10');
  exam.returnToTab(8000);
  assert.equal(exam.el('examTimer').textContent, '00:02');
  exam.advance(2000);
  assert.equal(exam.el('examTimer').textContent, '00:00');
  assert.equal(exam.el('btnSubmitExam').disabled, true);
  assert.match(exam.el('examStatus').textContent, /Hết thời gian/);
  assert.equal(exam.posts.length, 1);
});

test('Response đến khi đã hết giờ sẽ khóa ngay và không gửi bài nộp', async () => {
  const exam = placementPage({remaining: 1, delay: 4000});
  await exam.start();
  assert.equal(exam.el('examTimer').textContent, '00:00');
  assert.equal(exam.el('btnSubmitExam').disabled, true);
  await exam.context.submitExam();
  exam.context.selectOption('A');
  assert.equal(exam.posts.length, 1);
  assert.equal(vm.runInContext('Object.keys(studentAnswers).length', exam.context), 0);
});

test('Refresh lấy cùng lượt và khôi phục đáp án trong tab với deadline hiện tại', async () => {
  const exam = placementPage();
  await exam.start();
  exam.context.selectOption('B');
  const refreshed = placementPage({storage: exam.storage, remaining: 5});
  await new Promise(setImmediate);
  assert.ok(refreshed.gets.includes('/api/admission/tests/public/attempts/71'));
  assert.equal(refreshed.el('examTimer').textContent, '00:05');
  assert.equal(refreshed.el('answeredCounter').textContent, 'Đã làm: 1/1 câu');
  assert.equal(refreshed.posts.length, 0);
});

test('Lỗi gửi bài khi còn hạn giữ countdown và cho phép thử lại', async () => {
  const exam = placementPage({submit: () => { throw Error('Mất kết nối'); }});
  await exam.start();
  exam.context.selectOption('A');
  await exam.context.submitExam();
  exam.advance(5000);
  assert.equal(exam.el('examTimer').textContent, '00:07');
  assert.equal(exam.el('btnSubmitExam').disabled, false);
  assert.equal(exam.el('answeredCounter').textContent, 'Đã làm: 1/1 câu');
});

test('Server báo lượt hết hạn khi UI còn giờ thì khóa lượt khi kiểm tra lại', async () => {
  const exam = placementPage({submit: () => {
    exam.data.status = 'EXPIRED';
    throw Error('Lượt thi đã hết thời gian làm bài quy định');
  }});
  await exam.start();
  exam.context.selectOption('A');
  await exam.context.submitExam();
  assert.equal(exam.el('examTimer').textContent, '00:00');
  assert.equal(exam.el('btnSubmitExam').disabled, true);
  assert.match(exam.el('examStatus').textContent, /Hết thời gian/);
  assert.equal(exam.el('view-result').style.display, undefined);
});

test('Gửi sát hạn chỉ một lần và chờ kết quả server qua mốc 0', async () => {
  let resolve;
  const response = new Promise(r => { resolve = r; });
  const exam = placementPage({remaining: 2, submit: () => response});
  await exam.start();
  exam.context.selectOption('A');
  exam.advance(1500);
  const pending = exam.context.submitExam();
  const duplicate = exam.context.submitExam();
  exam.advance(1000);
  assert.equal(exam.el('examTimer').textContent, '00:00');
  assert.match(exam.el('examStatus').textContent, /Đang chờ/);
  assert.equal(exam.posts.length, 2);
  resolve({success: true, data: {submission_id: 101, total_score: 1}});
  await Promise.all([pending, duplicate]);
  assert.equal(exam.el('view-result').style.display, 'block');
  assert.equal(exam.el('resTotal').textContent, '1.0');
});

test('Tên khóa học không được thực thi HTML trong bảng và dropdown', async () => {
  const {context, el} = page('courses.html', async () => ({data: [{id: 1, name: '<img src=x onerror=alert(1)>', code: 'QA', level: 'BEGINNER'}]}));
  await context.loadCourses();
  assert.ok(!el('course-table-body').innerHTML.includes('<img'));
  assert.ok(!el('coursePrerequisite').innerHTML.includes('<img'));
});

test('Học vụ tải được giáo viên qua API lookup, không gọi danh sách người dùng Admin', async () => {
  const calls = [];
  const {context, el} = page('classes.html', async url => {
    calls.push(url);
    if (url === '/api/admission/users?role=TEACHER') throw Error('403 Admin only');
    return {data: url.includes('teachers') ? [{id: 3, full_name: 'Teacher QA'}] : []};
  });
  await context.initSelects();
  assert.ok(el('classTeacherId').innerHTML.includes('Teacher QA'));
  assert.ok(!calls.includes('/api/admission/users?role=TEACHER'));
});

test('401 đăng nhập không gọi logout/redirect của shared wrapper', async () => {
  let originalCalls=0;
  const ctx=vm.createContext({document:{documentElement:{dataset:{}}},
    getToken:()=>null, getCurrentUser:()=>null, isAuthenticated:()=>false,
    apiRequest:async()=>{originalCalls++;return null;},
    fetch:async()=>({ok:false,json:async()=>({success:false,error:{message:'Sai thông tin đăng nhập'}})})});
  vm.runInContext(fs.readFileSync(path.join(root,'js/admission-ui.js'),'utf8'),ctx);
  await assert.rejects(ctx.apiRequest('/api/admission/auth/login',{}),/Sai thông tin đăng nhập/);
  assert.equal(originalCalls,0);
});

test('Nút đăng ký lớp đề xuất dùng class_id, không ghép tên lớp vào JavaScript', () => {
  const {context,el}=page('test.html');
  context.displayResult({total_score:1,upcoming_classes:[{class_id:7,class_code:'QA',class_name:"O'Reilly <img>",available_slots:2,start_date:'2026-11-10'}]});
  assert.ok(el('suggestedClassesContainer').innerHTML.includes('quickEnroll(7)'));
  assert.ok(!el('suggestedClassesContainer').innerHTML.includes('<img>'));
});


test('BI hiển thị KPI API và N/A, không suy ra tiền từ APPROVED/CASH',async()=>{
 const calls=[];const {context,el}=page('bi-dashboard.html',async url=>{calls.push(url);return {success:true,data:url.includes('/funnel')?{metadata:{timezone:'Asia/Ho_Chi_Minh'},kpis:{test_completion_rate:66.67,paid_conversion_rate:25,test_to_enrollment_rate:null,expired_hold_rate:25},counts:{total_enrollments:8},stages:[]}:{average_occupancy_rate:5,finance_status:'UNAVAILABLE',top_courses:[{course_code:'<img src=x>',title:'QA',enrolled_count:8,approved_enrollments:2,pending_enrollments:1,cancelled_enrollments:2,occupancy_rate:5}]}};});
 el('filterFromDate').value='2020-01-31';el('filterToDate').value='2020-02-01';el('filterCourse').value='7';await context.loadDashboard();
 assert.equal(el('kpiTestComplete').textContent,'66.67%');assert.equal(el('kpiTestToEnroll').textContent,'N/A');assert.equal(el('kpiOccupancyRate').textContent,'5.00%');assert.equal(el('kpiRealizedRevenue').textContent,'Chưa khả dụng');assert.ok(el('top-courses-body').innerHTML.includes('&lt;img'));assert.ok(calls.every(u=>!u.includes('security')));assert.ok(calls.some(u=>u.includes('course_id=7')));
});

test('BI ADMIN hiển thị login thật từ API, lỗi không giữ lại số cũ',async()=>{
 const {context,el}=page('bi-dashboard.html',async url=>({success:true,data:url.includes('security')?{engagement:{dau:3,mau:8,failed_login_rate:null},daily_by_role:[],monthly_by_role:[]}:{}}));
 context.getCurrentUser=()=>({role:'ADMIN'});await context.loadDashboard();assert.equal(el('valDau').textContent,3);assert.equal(el('valFailedRate').textContent,'N/A');assert.equal(el('securityCard').hidden,false);
 context.apiGet=async()=>{throw Error('QA unavailable');};await context.loadDashboard();assert.ok(el('biError').textContent.includes('QA unavailable'));assert.equal(el('kpiTestComplete').textContent,'N/A');assert.equal(el('biLoad').disabled,false);
});

test('BI response bộ lọc cũ đến muộn không được ghi đè kết quả mới',async()=>{
 const {context,el}=page('bi-dashboard.html');let release;
 context.apiGet=async url=>url.includes('funnel')?await new Promise(resolve=>{release=resolve;}):{success:true,data:{}};
 const old=context.loadDashboard();context.apiGet=async()=>({success:true,data:{kpis:{test_completion_rate:75}}});await context.loadDashboard();release({success:true,data:{kpis:{test_completion_rate:10}}});await old;assert.equal(el('kpiTestComplete').textContent,'75.00%');
});
