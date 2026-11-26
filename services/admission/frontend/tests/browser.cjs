// Chạy Chrome headless với API fixtures: kiểm tra frontend Admission độc lập các service khác.
const fs = require('node:fs');
const http = require('node:http');
const path = require('node:path');
const assert = require('node:assert/strict');
const {chromium} = require('playwright');
const root = path.resolve(__dirname, '../../../..');
const course = {id:1,code:'QA',name:'<img src=x onerror="window.xss=1">',level:'BEGINNER',is_active:true,duration_weeks:4,tuition_amount:100};
const clazz = {id:1,code:'QA-1',name:'QA Class',course_name:'QA',status:'OPEN',enrolled_students:3,max_students:10,
  available_slots:7,day_pattern:'DAILY',start_time:'08:00:00',end_time:'09:00:00',start_date:'2026-11-09',end_date:'2026-12-09',room_id:1};
const server = http.createServer((req,res) => {
  const pathname = new URL(req.url, 'http://localhost').pathname;
  const base = pathname.startsWith('/admission/') ? path.join(root,'services/admission/frontend') : path.join(root,'shared');
  const suffix = pathname.replace(/^\/(admission|shared)\//, '');
  const file = path.resolve(base, suffix);
  if (!file.startsWith(base + path.sep) || !fs.existsSync(file) || !fs.statSync(file).isFile()) {res.writeHead(404);res.end();return;}
  res.setHeader('Content-Type', file.endsWith('.html')?'text/html; charset=utf-8':file.endsWith('.js')?'text/javascript; charset=utf-8':'text/css');
  fs.createReadStream(file).pipe(res);
});
(async () => {
  await new Promise(resolve => server.listen(0,'127.0.0.1',resolve));
  const browser = await chromium.launch({headless:true,channel:'chrome'});
  const origin = `http://127.0.0.1:${server.address().port}`;
  try {
    const context = await browser.newContext();
    await context.addInitScript(() => {
      const payload = btoa(JSON.stringify({sub:'2',role:'STAFF',exp:Math.floor(Date.now()/1000)+3600})).replace(/=+$/, '');
      localStorage.setItem('ec_token', 'e30.'+payload+'.signature');
      localStorage.setItem('ec_user', JSON.stringify({id:2,email:'staff@example.test',role:'STAFF',name:'QA Staff'}));
    });
    const requests=[]; let cashRequested=false; let holdDeadline='2030-01-01T12:00:00';
    let rosterStatus = 200;
    const rosterCsv = '\uFEFFSTT,Mã đơn,ID tài khoản Admission,Họ và tên\r\n1,ENR-15,42,Nguyễn An\r\n';
    const admissionFixtures = async route => {
      const url=new URL(route.request().url()); requests.push({path:url.pathname,query:url.search,headers:route.request().headers(),body:route.request().postDataJSON()});
      let data=[];
      if(url.pathname.endsWith('/verify-email')) data={setup_token:'fixture-setup'};
      else if(url.pathname.endsWith('/courses')) data=[course];
      else if(url.pathname.endsWith('/classes')) data=[clazz];
      else if(url.pathname.endsWith('/rooms')) data=[{id:1,code:'R1',name:'Room 1',capacity:20,is_active:true}];
      else if(url.pathname.endsWith('/teachers')) data=[{id:3,full_name:'Teacher QA'}];
      else if(url.pathname.endsWith('/approve-manual')) {
        cashRequested=true;
        await route.fulfill({status:202,contentType:'application/json',body:JSON.stringify({success:true,data:{status:'PENDING'}})});return;
      }
      else if(url.pathname.endsWith('/extend-hold')) {
        assert.equal(route.request().postDataJSON().expected_hold_expires_at,holdDeadline);
        holdDeadline='2030-01-02T12:00:00'; data={status:'PENDING',hold_expires_at:holdDeadline};
      }
      else if(url.pathname.endsWith('/enrollments')) data={content:[{enrollment_id:15,enrollment_code:'ENR-15',status:'PENDING',remaining_seconds:3600,hold_expires_at:holdDeadline,invoice_code:'INV-QA',
        cash_confirmation:cashRequested?{status:'NEEDS_FINANCE_CONFIRMATION'}:null}]};
      else if(url.pathname.endsWith('/bi/funnel')) data={metadata:{from_date:'2026-11-01',to_date:'2026-11-10',timezone:'Asia/Ho_Chi_Minh'},stages:[],kpis:{test_completion_rate:66.67,paid_conversion_rate:25,test_to_enrollment_rate:null,expired_hold_rate:25},counts:{total_enrollments:8}};
      else if(url.pathname.endsWith('/bi/courses-revenue')) data={finance_status:'UNAVAILABLE',average_occupancy_rate:5,top_courses:[{course_code:'BI-QA',title:'QA',enrolled_count:8,approved_enrollments:2,pending_enrollments:1,cancelled_enrollments:2,occupancy_rate:5}]};
      else if(url.pathname.endsWith('/bi/security-engagement')) data={engagement:{dau:3,mau:8,failed_login_rate:12.5},daily_by_role:[{period:'2026-11-10',role:'STUDENT',logged_in_users:3}],monthly_by_role:[]};
      else if(url.pathname.endsWith('/sets')) data=[{id:1,code:'T1',title:'Test QA',duration_minutes:10}];
      else if (url.pathname.endsWith('/exports/classes/1/roster')) {
        if (rosterStatus === 200) {
          await route.fulfill({status:200, contentType:'text/csv; charset=UTF-8', body:rosterCsv});
        } else {
          await route.fulfill({status:rosterStatus, json:{success:false, error:{message:'QA export error'}}});
        }
        return;
      }
      await route.fulfill({status:200,contentType:'application/json',body:JSON.stringify({success:true,data})});
    };
    await context.route('**/api/admission/**', admissionFixtures);
    for(const name of ['courses','classes','rooms','enrollments','bi-dashboard','test','account']) {
      const page=await context.newPage(); const errors=[]; page.on('pageerror',e=>errors.push(e.message));
      await page.goto(`${origin}/admission/${name}.html`); await page.waitForLoadState('networkidle');
      assert.deepEqual(errors, [], `${name}: JavaScript runtime errors`);
      if(name==='courses') {
        assert.equal(await page.locator('#course-table-body img').count(),0);
        assert.ok((await page.locator('#course-table-body').innerText()).includes('<img'));
        assert.equal(await page.locator('[onclick^="deleteCourse"]').isVisible(),false);
      }
      if(name==='classes') {
        assert.ok((await page.locator('#classTeacherId').innerText()).includes('Teacher QA'));
        assert.ok((await page.locator('tbody').first().innerText()).includes('3 / 10'));
        await page.evaluate(() => {
          window.rosterToasts = [];
          showToast = type => window.rosterToasts.push(type);
        });
        const downloads = [];
        page.on('download', item => downloads.push(item));
        const downloading = page.waitForEvent('download');
        await page.locator('[onclick="exportRoster(1)"]').click();
        const download = await downloading;
        assert.equal(download.suggestedFilename(), 'class-1.csv');
        assert.equal(fs.readFileSync(await download.path(), 'utf8'), rosterCsv);
        const rosterRequest = requests.find(r => r.path === '/api/admission/exports/classes/1/roster');
        assert.equal(rosterRequest.query, '?format=csv');
        assert.ok(rosterRequest.headers.authorization?.startsWith('Bearer '));
        console.log('PASS Chrome roster download path, JWT, filename and CSV content (API fixtures)');

        for (const status of [403, 500]) {
          rosterStatus = status;
          const failed = page.waitForResponse(r => r.url().includes('/exports/classes/1/roster') && r.status() === status);
          await page.evaluate(() => exportRoster(1));
          await failed;
        }
        assert.deepEqual(await page.evaluate(() => window.rosterToasts), ['info', 'error', 'error']);
        rosterStatus = 401;
        const signedOut = page.waitForURL('**/admission/account.html');
        await page.locator('[onclick="exportRoster(1)"]').click();
        await signedOut;
        await page.locator('#accountMessage').filter({hasText:'Vui lòng đăng nhập lại'}).waitFor();
        assert.equal(downloads.length, 1);
        rosterStatus = 200;
        console.log('PASS Chrome roster 401/403/500 show errors without a download (API fixtures)');
      }
      if(name==='enrollments') {
        await page.locator('[onclick="openApproveModal(15)"]').click();
        assert.ok((await page.locator('#manualApproveModal').innerText()).includes('chưa gửi Finance tự động'));
        assert.equal(await page.locator('#approvePaymentMethod option').count(),1);
        await page.locator('#approveReceiptNumber').fill('RC-QA-15');
        await page.locator('#approveAmount').fill('5000000');
        await page.locator('#btnConfirmApprove').click();
        await page.waitForFunction(()=>document.getElementById('enrollment-table-body').textContent.includes('Chờ Finance'));
        assert.equal(await page.locator('[onclick="openApproveModal(15)"]').count(),0);
        const cash=requests.find(r=>r.path.endsWith('/approve-manual'));
        assert.equal(cash.body.payment_method,'CASH');assert.equal(cash.body.amount_received,'5000000');
        assert.ok(cash.headers.authorization?.startsWith('Bearer '));
        assert.ok(!(await page.locator('#enrollment-table-body').innerText()).includes('Đã nhập học'));
        console.log('PASS Chrome cash confirmation pending Finance (API fixture)');
        await page.locator('[onclick="openExtendModal(15)"]').click();
        await page.locator('#extendReason').fill('Chờ chuyển khoản');
        await page.locator('#btnConfirmExtend').click();
        await page.waitForFunction(()=>!document.getElementById('extendHoldModal').classList.contains('active'));
        const hold=requests.find(r=>r.path.endsWith('/extend-hold'));
        assert.equal(hold.body.expected_hold_expires_at,'2030-01-01T12:00:00');assert.equal(hold.body.extended_hours,24);
        assert.ok(hold.headers.authorization?.startsWith('Bearer '));
        console.log('PASS Chrome hold extension snapshot/precondition/JWT (API fixture)');
      }
      if(name==='bi-dashboard') {
        assert.equal(await page.locator('#kpiTestComplete').innerText(),'66.67%');assert.equal(await page.locator('#kpiTestToEnroll').innerText(),'N/A');
        assert.equal(await page.locator('#kpiRealizedRevenue').innerText(),'Chưa khả dụng');assert.ok((await page.locator('#top-courses-body').innerText()).includes('BI-QA'));
        assert.equal(await page.locator('#securityCard').isVisible(),false);assert.ok(!requests.some(r=>r.path.endsWith('/security-engagement')));
        await page.locator('#filterCourse').selectOption('1');await page.locator('#biLoad').click();await page.waitForLoadState('networkidle');
        assert.ok(requests.some(r=>r.path.endsWith('/bi/funnel')));console.log('PASS Chrome BI KPI/render/filter/Finance unavailable/Staff boundary');
      }
      if(name==='account') {
        await page.goto(`${origin}/admission/account.html#verify=fixture-verify`); await page.waitForLoadState('networkidle');
        await page.waitForFunction(()=>window.location.hash === '', {timeout:5000});
        await page.locator('#password').waitFor({state:'visible'});
        assert.equal(new URL(page.url()).hash,'');
        await page.locator('#password [name="new_password"]').fill('Strong-password-123');
        await page.locator('#password [name="confirm"]').fill('Different-password-123');
        await page.locator('#password button').click();
        assert.ok((await page.locator('#accountMessage').innerText()).includes('không khớp'));
        await page.locator('#password [name="confirm"]').fill('Strong-password-123');
        await page.locator('#password button').click(); await page.waitForLoadState('networkidle');
        assert.ok(requests.some(r=>r.path.endsWith('/setup-password')));
        assert.equal(await page.evaluate(()=>JSON.stringify({...localStorage}).includes('fixture-setup')),false);
      }
      console.log(`PASS Chrome ${name}.html`); await page.close();
    }
    const studentContext = await browser.newContext({timezoneId: 'America/Los_Angeles'});
    await studentContext.addInitScript(() => {
      const claims = btoa(JSON.stringify({sub:'3', role:'STUDENT', exp:Math.floor(Date.parse('2041-01-01T00:00:00Z')/1000)})).replace(/=+$/, '');
      localStorage.setItem('ec_token', 'e30.' + claims + '.signature');
      localStorage.setItem('ec_user', JSON.stringify({id:3, email:'placement@example.test', role:'STUDENT', name:'QA Student'}));
    });
    const placementRequests = [];
    let attemptId = 70;
    await studentContext.route('**/api/admission/tests/public/**', async route => {
      const request = route.request();
      const url = new URL(request.url());
      placementRequests.push(request);
      let data;
      if (url.pathname.endsWith('/sets')) {
        data = [{id:1, code:'QA', title:'Placement QA', duration_minutes:10}];
      } else if (url.pathname.endsWith('/submit')) {
        assert.equal(request.postDataJSON().attempt_id, attemptId);
        assert.equal(request.postDataJSON().attempt_token, 'fixture-capability');
        assert.deepEqual(request.postDataJSON().answers, [{question_id:8, selected_option:'B'}]);
        data = {submission_id:10, total_score:1, grammar_score:1};
      } else {
        const start = url.pathname.endsWith('/start');
        if (start) attemptId++;
        else assert.equal(url.pathname, '/api/admission/tests/public/attempts/' + attemptId);
        data = {attempt_id:attemptId, status:'STARTED', started_at:'2030-01-01T11:50:00',
          expires_at:'2030-01-01T12:00:00', server_now:start ? '2030-01-01T11:59:45' : '2030-01-01T11:59:55',
          questions:[{id:8, question_text:'Where is the station?', skill_type:'GRAMMAR', option_b:'On the right'}]};
        if (start) data.attempt_token = 'fixture-capability';
      }
      await route.fulfill({json:{success:true, data}});
    });
    const examPage = await studentContext.newPage();
    const placementErrors = [];
    examPage.on('pageerror', error => placementErrors.push(error.message));
    await examPage.clock.install({time:new Date('2040-02-01T08:00:00Z')});
    await examPage.goto(`${origin}/admission/test.html`);
    const startPlacement = async () => {
      await examPage.locator('#candidateTestSet').selectOption('1');
      await examPage.locator('#candidateName').fill('QA Student');
      await examPage.locator('#candidateEmail').fill('placement@example.test');
      await examPage.locator('#candidatePhone').fill('0900000000');
      await examPage.locator('#btnStartExam').click();
      await examPage.locator('#optionsContainer').waitFor({state:'visible'});
      await examPage.locator('.option-card').nth(1).click();
    };
    await startPlacement();
    assert.match(await examPage.locator('#examTimer').innerText(), /^00:1[0-5]$/);
    await examPage.reload();
    await examPage.locator('.option-card.selected').waitFor({state:'visible'});
    assert.equal(await examPage.locator('#answeredCounter').innerText(), 'Đã làm: 1/1 câu');
    assert.match(await examPage.locator('#examTimer').innerText(), /^00:0[1-5]$/);
    assert.equal(placementRequests.filter(r => r.url().endsWith('/start')).length, 1);
    assert.ok(placementRequests.find(r => r.url().endsWith('/attempts/71')).headers().authorization?.startsWith('Bearer '));
    await examPage.clock.fastForward(7000);
    assert.equal(await examPage.locator('#examTimer').innerText(), '00:00');
    assert.match(await examPage.locator('#examStatus').innerText(), /Hết thời gian/);
    assert.equal(await examPage.locator('#btnSubmitExam').isDisabled(), true);
    assert.equal(await examPage.locator('#optionsContainer').isVisible(), false);
    assert.equal(placementRequests.filter(r => r.url().endsWith('/submit')).length, 0);
    console.log('PASS Chrome Placement countdown, refresh, restored answer and timeout (API fixtures)');

    await examPage.locator('#btnExitExam').click();
    await startPlacement();
    examPage.once('dialog', dialog => dialog.accept());
    await examPage.locator('#btnSubmitExam').click();
    await examPage.locator('#resTotal').waitFor({state:'visible'});
    assert.equal(await examPage.locator('#resTotal').innerText(), '1.0');
    assert.equal(placementRequests.filter(r => r.url().endsWith('/submit')).length, 1);
    assert.deepEqual(placementErrors, []);
    console.log('PASS Chrome Placement manual submit and confirmed result (API fixtures)');
    await studentContext.close();

    const adminContext=await browser.newContext();
    await adminContext.addInitScript(()=>{const payload=btoa(JSON.stringify({sub:'1',role:'ADMIN',exp:Math.floor(Date.now()/1000)+3600})).replace(/=+$/, '');localStorage.setItem('ec_token','e30.'+payload+'.signature');localStorage.setItem('ec_user',JSON.stringify({id:1,role:'ADMIN',name:'QA Admin',email:'admin@example.test'}));});
    await adminContext.route('**/api/admission/**',admissionFixtures);const adminPage=await adminContext.newPage();
    await adminPage.goto(`${origin}/admission/bi-dashboard.html`);await adminPage.waitForLoadState('networkidle');
    assert.equal(await adminPage.locator('#securityCard').isVisible(),true);assert.equal(await adminPage.locator('#valDau').innerText(),'3');assert.equal(await adminPage.locator('#valMau').innerText(),'8');assert.equal(await adminPage.locator('#valFailedRate').innerText(),'12.50%');
    assert.ok((await adminPage.locator('#dailyActivity').innerText()).includes('STUDENT'));
    const roleRequest=adminPage.waitForRequest(r=>r.url().includes('/bi/security-engagement')&&r.url().includes('role=TEACHER'));await adminPage.locator('#filterRole').selectOption('TEACHER');await roleRequest;await adminPage.waitForFunction(()=>document.getElementById('biLoad').disabled===false);assert.ok(requests.some(r=>r.path.endsWith('/bi/security-engagement')&&r.query.includes('role=TEACHER')));
    console.log('PASS Chrome BI ADMIN login trends and role filter');await adminContext.close();
    const guest=await browser.newContext(); const p=await guest.newPage();
    await p.goto(`${origin}/admission/enrollments.html`);
    assert.ok((await p.locator('body').innerText()).includes('dành cho Học vụ'));
    await p.goto(`${origin}/admission/bi-dashboard.html`);assert.ok((await p.locator('body').innerText()).includes('dành cho Học vụ'));
    console.log('PASS Chrome guest role boundary');
    await guest.close(); await context.close();

    const teacher = await browser.newContext();
    await teacher.addInitScript(() => {
      const claims = btoa(JSON.stringify({sub:'4',role:'TEACHER',exp:Math.floor(Date.now()/1000)+3600})).replace(/=+$/, '');
      localStorage.setItem('ec_token','e30.'+claims+'.signature');
      localStorage.setItem('ec_user',JSON.stringify({id:4,role:'TEACHER',name:'QA Teacher',email:'teacher@example.test'}));
    });
    await teacher.route('**/api/admission/**', admissionFixtures);
    const teacherPage = await teacher.newPage();
    await teacherPage.goto(`${origin}/admission/classes.html`);
    assert.equal(await teacherPage.locator('html').getAttribute('data-admission-role'), 'TEACHER');
    assert.equal(await teacherPage.locator('.admission-write').first().isVisible(), false);
    await teacherPage.goto(`${origin}/admission/enrollments.html`);
    await teacherPage.getByText('Trang này dành cho Học vụ hoặc Quản trị viên.').waitFor();
    console.log('PASS Chrome teacher can read public classes and cannot open staff pages (API fixtures)');
    await teacher.close();

    const sessions = await browser.newContext();
    const principal = {id:23, email:'session@example.test', role:'STAFF', full_name:'QA Session'};
    const jwt = (claims, label) => 'e30.' + Buffer.from(JSON.stringify(claims)).toString('base64url') + '.' + label;
    const exp = Math.floor(Date.now() / 1000) + 3600;
    const access = label => jwt({sub:'23', exp, role:'STAFF'}, label);
    const refresh = label => jwt({sub:'23', exp, token_type:'REFRESH'}, label);
    let refreshCalls = 0;
    let logoutCalls = 0;
    let firstRequests = 0;
    let releaseRotation;
    const bothSent = new Promise(resolve => { releaseRotation = resolve; });
    await sessions.route('**/api/admission/**', async route => {
      const request = route.request();
      const {pathname} = new URL(request.url());
      const bearer = request.headers().authorization;
      let data = [];
      if (pathname.endsWith('/auth/login')) {
        data = {access_token:access('initial'), refresh_token:refresh('initial'), user:{...principal, user_id:23}};
      } else if (pathname.endsWith('/auth/me')) {
        assert.ok([access('initial'), access('rotated')].some(token => bearer === 'Bearer ' + token));
        data = principal;
      } else if (pathname.endsWith('/auth/refresh')) {
        refreshCalls++;
        assert.ok(request.postDataJSON().refresh_token === refresh('initial'));
        await bothSent;
        data = {access_token:access('rotated'), refresh_token:refresh('rotated')};
      } else if (pathname.endsWith('/auth/logout')) {
        logoutCalls++;
        assert.ok(bearer === 'Bearer ' + access('rotated'));
        assert.ok(request.postDataJSON().refresh_token === refresh('rotated'));
        data = null;
      } else if (pathname.endsWith('/rooms/qa-session') && bearer === 'Bearer ' + access('initial')) {
        if (++firstRequests === 2) releaseRotation();
        await route.fulfill({status:401, json:{success:false, error:{message:'Hết hạn'}}});
        return;
      }
      await route.fulfill({json:{success:true, data}});
    });
    const primary = await sessions.newPage();
    await primary.goto(`${origin}/admission/account.html`);
    await primary.locator('#login [name="email"]').fill(principal.email);
    await primary.locator('#login [name="password"]').fill('qa-session-test-password');
    await primary.locator('#login button').click();
    await primary.waitForURL('**/admission/test.html');
    await primary.locator('#header .user-name').filter({hasText:principal.full_name}).waitFor();
    assert.equal(await primary.evaluate(() => localStorage.getItem('ec_token')), null);
    console.log('PASS Chrome Admission login and auth/me after navigation (API fixtures)');

    // Sao chép sessionStorage như một tab trùng, rồi để Chrome điều phối Web Locks thật.
    const clone = async source => {
      const snapshot = await source.evaluate(() => sessionStorage.getItem('admission_session'));
      const copy = await sessions.newPage();
      await copy.addInitScript(value => {
        if (sessionStorage.getItem('admission_session') === null) sessionStorage.setItem('admission_session', value);
      }, snapshot);
      await copy.goto(`${origin}/admission/test.html`);
      await copy.locator('#header .user-name').filter({hasText:principal.full_name}).waitFor();
      return copy;
    };
    const duplicate = await clone(primary);
    const callProtected = page => page.evaluate(() => {
      window.refreshTestDone = apiGet('/api/admission/rooms/qa-session').then(() => true, () => false);
    });
    const rotationReply = sessions.waitForEvent('response', {predicate: response => response.url().endsWith('/auth/refresh')});
    await Promise.all([callProtected(primary), callProtected(duplicate)]);
    await rotationReply;
    await Promise.race([primary.waitForURL('**/admission/account.html'), duplicate.waitForURL('**/admission/account.html')]);
    const winner = primary.url().endsWith('/test.html') ? primary : duplicate;
    assert.equal(await winner.evaluate(() => window.refreshTestDone), true);
    assert.equal(refreshCalls, 1);
    console.log('PASS Chrome duplicate tabs serialize rotation without sharing tokens (API fixtures)');

    const peer = await clone(winner);
    winner.once('dialog', dialog => dialog.accept());
    await winner.locator('#header button[onclick="handleLogout()"]').click();
    await Promise.all([winner.waitForURL('**/admission/account.html'), peer.waitForURL('**/admission/account.html')]);
    await winner.locator('#accountMessage').filter({hasText:'thu hồi phiên làm mới'}).waitFor();
    assert.equal(await peer.evaluate(() => getToken()), null);
    await peer.locator('#admissionAccountLogout').waitFor({state:'hidden'});
    assert.equal(logoutCalls, 1);
    console.log('PASS Chrome logout revokes once and clears cloned tab UI (API fixtures)');
    await sessions.close();

    const employees = await browser.newContext();
    const temporary = 'qa-initial-password';
    const changed = 'qa-updated-password';
    let pendingPassword = true;
    let enrollmentCalls = 0;
    let changeCalls = 0;
    await employees.route('**/api/admission/**', async route => {
      const request = route.request();
      const pathname = new URL(request.url()).pathname;
      let data = [];
      if (pathname.endsWith('/auth/login')) {
        assert.ok(request.postDataJSON().password === (pendingPassword ? temporary : changed));
        data = {
          access_token:jwt({sub:'23', exp, role:'STAFF', must_change_password:pendingPassword}, pendingPassword ? 'limited' : 'normal'),
          refresh_token:refresh(pendingPassword ? 'limited' : 'normal'),
          user:{...principal, user_id:23, must_change_password:pendingPassword}
        };
      } else if (pathname.endsWith('/auth/me')) {
        data = pendingPassword ? {...principal, must_change_password:true} : principal;
      } else if (pathname.endsWith('/auth/change-password')) {
        changeCalls++;
        if (request.postDataJSON().current_password !== temporary) {
          await route.fulfill({status:401, json:{success:false, error:{code:'AUTH_INVALID_CREDENTIALS', message:'Sai mật khẩu hiện tại'}}});
          return;
        }
        assert.ok(request.postDataJSON().new_password === changed);
        pendingPassword = false;
        await route.fulfill({json:{success:true, message:'Đã đổi mật khẩu. Vui lòng đăng nhập lại'}});
        return;
      } else if (pathname.endsWith('/enrollments')) {
        enrollmentCalls++;
      }
      await route.fulfill({json:{success:true, data}});
    });
    const employee = await employees.newPage();
    await employee.goto(`${origin}/admission/account.html`);
    const submitLogin = async password => {
      await employee.locator('#login [name="email"]').fill(principal.email);
      await employee.locator('#login [name="password"]').fill(password);
      await employee.locator('#login button').click();
    };
    await submitLogin(temporary);
    await employee.locator('#change').waitFor({state:'visible'});
    assert.ok(employee.url().endsWith('/account.html'));
    await employee.goto(`${origin}/admission/enrollments.html`);
    await employee.waitForURL('**/admission/account.html');
    await employee.locator('#change').waitFor({state:'visible'});
    assert.equal(enrollmentCalls, 0);
    console.log('PASS Chrome initial staff login and direct navigation lead to password change (API fixtures)');

    await employee.locator('#change [name="current_password"]').fill('qa-wrong-password');
    await employee.locator('#change [name="new_password"]').fill(changed);
    await employee.locator('#change button').click();
    await employee.locator('#accountMessage').filter({hasText:'Sai mật khẩu hiện tại'}).waitFor();
    await employee.locator('#change [name="current_password"]').fill(temporary);
    await employee.locator('#change button').click();
    await employee.locator('#accountMessage').filter({hasText:'Vui lòng đăng nhập lại'}).waitFor();
    assert.equal(await employee.evaluate(() => getToken()), null);
    await submitLogin(changed);
    await employee.waitForURL('**/admission/test.html');
    await employee.locator('#header .user-name').filter({hasText:principal.full_name}).waitFor();
    assert.equal(changeCalls, 2);
    console.log('PASS Chrome staff password change errors, session cleanup and fresh login (API fixtures)');
    await employees.close();
  } finally {await browser.close();server.close();}
})().catch(error=>{console.error(error.message);server.close();process.exitCode=1;});
