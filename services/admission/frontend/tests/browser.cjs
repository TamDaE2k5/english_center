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
      const payload = btoa(JSON.stringify({sub:'2',role:'STAFF',exp:Math.floor(Date.now()/1000)+3600}));
      localStorage.setItem('ec_token', 'e30.'+payload+'.signature');
      localStorage.setItem('ec_user', JSON.stringify({id:2,email:'staff@example.test',role:'STAFF',name:'QA Staff'}));
    });
    const requests=[]; let cashRequested=false;
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
      else if(url.pathname.endsWith('/enrollments')) data={content:[{enrollment_id:15,enrollment_code:'ENR-15',status:'PENDING',remaining_seconds:3600,invoice_code:'INV-QA',
        cash_confirmation:cashRequested?{status:'NEEDS_FINANCE_CONFIRMATION'}:null}]};
      else if(url.pathname.endsWith('/bi/funnel')) data={metadata:{from_date:'2026-11-01',to_date:'2026-11-10',timezone:'Asia/Ho_Chi_Minh'},stages:[],kpis:{test_completion_rate:66.67,paid_conversion_rate:25,test_to_enrollment_rate:null,expired_hold_rate:25},counts:{total_enrollments:8}};
      else if(url.pathname.endsWith('/bi/courses-revenue')) data={finance_status:'UNAVAILABLE',average_occupancy_rate:5,top_courses:[{course_code:'BI-QA',title:'QA',enrolled_count:8,approved_enrollments:2,pending_enrollments:1,cancelled_enrollments:2,occupancy_rate:5}]};
      else if(url.pathname.endsWith('/bi/security-engagement')) data={engagement:{dau:3,mau:8,failed_login_rate:12.5},daily_by_role:[{period:'2026-11-10',role:'STUDENT',logged_in_users:3}],monthly_by_role:[]};
      else if(url.pathname.endsWith('/sets')) data=[{id:1,code:'T1',title:'Test QA',duration_minutes:10}];
      else if(url.pathname.includes('/exports/')) {await route.fulfill({status:200,contentType:'text/csv',body:'id,name\n1,QA'});return;}
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
        const download=page.waitForEvent('download'); await page.evaluate(()=>exportRoster(1)); await download;
        assert.ok(requests.find(r=>r.path.includes('/exports/') && r.headers.authorization?.startsWith('Bearer ')));
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
    const adminContext=await browser.newContext();
    await adminContext.addInitScript(()=>{const payload=btoa(JSON.stringify({sub:'1',role:'ADMIN',exp:Math.floor(Date.now()/1000)+3600}));localStorage.setItem('ec_token','e30.'+payload+'.signature');localStorage.setItem('ec_user',JSON.stringify({id:1,role:'ADMIN',name:'QA Admin'}));});
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
  } finally {await browser.close();server.close();}
})().catch(error=>{console.error(error.message);server.close();process.exitCode=1;});
