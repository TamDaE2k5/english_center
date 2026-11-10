const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const test = require('node:test');
const root = path.resolve(__dirname, '..');

// Chạy JavaScript thực của trang trong DOM tối thiểu; không sao chép logic ứng dụng vào test.
function page(name, apiGet = async () => ({data: []})) {
  const elements = new Map();
  const el = id => {
    if (!elements.has(id)) elements.set(id, {id, innerHTML: '', textContent: '', value: '', style: {},
      classList: {add(){}, remove(){}, toggle(){}}, handlers: {}, addEventListener(name,fn){this.handlers[name]=fn;}, setAttribute(){}, getAttribute(){}, reset(){}});
    return elements.get(id);
  };
  const context = vm.createContext({console, Intl, Date, Math, JSON, URLSearchParams,
    setInterval: () => 1, clearInterval(){}, setTimeout(){}, confirm: () => true,
    localStorage: {getItem: () => null}, sessionStorage: {setItem(){}},
    window: {location: {pathname: '/admission/' + name, hash: ''}},
    document: {getElementById: el, querySelectorAll: () => [],
      documentElement: {dataset: {}, classList: {add(){}, remove(){}}}, body: {dataset: {}, classList: {add(){},remove(){}}}, addEventListener(){}},
    getToken: () => 'qa-token', isAuthenticated: () => true,
    getCurrentUser: () => ({role: 'STAFF'}), showToast(){}, apiGet,
    apiPost: async () => ({success: true}), apiPut: async () => ({success: true}), apiDelete: async () => ({success: true})});
  const helper = path.join(root, 'js', 'admission-ui.js');
  if (fs.existsSync(helper)) vm.runInContext(fs.readFileSync(helper, 'utf8'), context);
  const html = fs.readFileSync(path.join(root, name), 'utf8');
  for (const m of html.matchAll(/<script(?![^>]*\bsrc=)[^>]*>([\s\S]*?)<\/script>/g)) vm.runInContext(m[1], context);
  return {context, el};
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

test('Câu hỏi hiển thị question_text theo DTO thực', () => {
  const {context, el} = page('test.html');
  vm.runInContext('questionsList = [{id:1, question_text:"Where is the station?", skill_type:"GRAMMAR"}]; showQuestion(0)', context);
  assert.equal(el('questionContent').textContent, 'Where is the station?');
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
