package com.englishcenter.admission.service;

import com.englishcenter.admission.client.FinanceServiceClient;
import com.englishcenter.admission.common.exception.*;
import com.englishcenter.admission.dto.enrollment.CreateEnrollmentRequest;
import com.englishcenter.admission.helper.StudentServiceSyncHelper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.List;
import org.springframework.boot.test.mock.mockito.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;

/** Chạy có chủ đích trên DB QA riêng đã nạp scripts/init-db.sql; không dùng DB production. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "jobs.hold-expiration.cron=-")
@EnabledIfEnvironmentVariable(named = "ADMISSION_PG_TEST", matches = "true")
class AdmissionPostgresTest {
    private static final SmtpCapture mailbox = new SmtpCapture();
    @Autowired EnrollmentService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired TestRestTemplate http;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;
    @Autowired UserService staffUsers;
    @org.springframework.beans.factory.annotation.Value("${internal.secret-key}") String qaInternalSecret;
    @SpyBean com.englishcenter.admission.repository.EnrollmentRepository cancellationEnrollments;
    @SpyBean com.englishcenter.admission.repository.CashConfirmationRepository cashRequests;

    /** Nhân viên chỉ gửi yêu cầu; chưa có bằng chứng Finance thì đơn phải còn PENDING. */
    @Test void cashRequestCannotApproveWithoutFinanceEvidence() {
        int[] f=cancellationFixture();
        jdbc.update("UPDATE admission.enrollments SET invoice_code=? WHERE id=?","INV-CASH-"+f[1],f[1]);
        var response=http.postForEntity("/api/admission/enrollments/"+f[1]+"/approve-manual",
                cashHttpRequest("STAFF","RC-"+f[1],"5000000"),JsonNode.class);
        assertEquals("PENDING",jdbc.queryForObject("SELECT status FROM admission.enrollments WHERE id=?",String.class,f[1]));
        assertEquals(202,response.getStatusCode().value(),String.valueOf(response.getBody()));
    }

    /** Tạo danh tính QA thật, lấy JWT qua login; không giả lập filter phân quyền. */
    private org.springframework.http.HttpEntity<Map<String,Object>> cashHttpRequest(String role,String receipt,String amount) {
        String email="qa-cash-"+UUID.randomUUID()+"@example.test";
        jdbc.update("INSERT INTO admission.users(email,password_hash,full_name,role,email_verified_at,must_change_password) VALUES (?,?,'QA Cash',?,CURRENT_TIMESTAMP,FALSE)",
                email,encoder.encode("QA-cash-password-123"),role);
        var login=http.postForEntity("/api/admission/auth/login",Map.of("email",email,"password","QA-cash-password-123"),JsonNode.class);
        assertEquals(200,login.getStatusCode().value());
        var headers=new org.springframework.http.HttpHeaders(); headers.setBearerAuth(login.getBody().get("data").get("access_token").asText());
        return new org.springframework.http.HttpEntity<>(Map.of("receipt_number",receipt,"amount_received",amount,"payment_method","CASH","notes","QA cash request"),headers);
    }

    /** Cùng chứng từ là replay; đổi số tiền/chứng từ không được tạo khoản thu thứ hai. */
    @Test void cashReplayAndConflictsAreAudited() {
        int[] f=cashFixture(); String url=cashUrl(f[1]); var request=cashHttpRequest("STAFF","RC-"+f[1],"5000000");
        assertEquals(202,http.postForEntity(url,request,JsonNode.class).getStatusCode().value());
        assertEquals(202,http.postForEntity(url,request,JsonNode.class).getStatusCode().value());
        assertEquals(409,http.postForEntity(url,cashHttpRequest("STAFF","RC-"+f[1],"4000000"),JsonNode.class).getStatusCode().value());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM admission.cash_confirmation_requests WHERE enrollment_id=?",Integer.class,f[1]));
        assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM admission.cash_confirmation_audit WHERE enrollment_id=?",Integer.class,f[1]));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM admission.cash_confirmation_audit WHERE enrollment_id=? AND outcome='REJECTED'",Integer.class,f[1]));
        assertNotNull(jdbc.queryForObject("SELECT requested_by FROM admission.cash_confirmation_requests WHERE enrollment_id=?",Integer.class,f[1]));
        assertNotNull(jdbc.queryForObject("SELECT requested_at FROM admission.cash_confirmation_requests WHERE enrollment_id=?",java.time.LocalDateTime.class,f[1]));
        assertEquals("NEEDS_FINANCE_CONFIRMATION",jdbc.queryForObject("SELECT status FROM admission.cash_confirmation_requests WHERE enrollment_id=?",String.class,f[1]));
    }

    /** Quyền HTTP thật: STAFF/ADMIN được gửi; học viên, giáo viên và guest bị chặn. */
    @Test void cashRolesAndInputValidation() {
        int[] f=cashFixture(); String url=cashUrl(f[1]);
        for(String role:List.of("STUDENT","TEACHER"))
            assertEquals(403,http.postForEntity(url,cashHttpRequest(role,"RC-"+f[1],"5000000"),JsonNode.class).getStatusCode().value());
        assertEquals(401,http.postForEntity(url,Map.of("amount_received",5000000),JsonNode.class).getStatusCode().value());
        var staff=cashHttpRequest("STAFF","RC-"+f[1],"5000000");
        for(var invalid:List.of(Map.of("receipt_number"," ","amount_received",5000000),Map.of("receipt_number","RC","amount_received",0),
                Map.of("receipt_number","RC","amount_received",1,"payment_method","POS_CARD"),Map.of("receipt_number","RC","amount_received","1.123"))) {
            assertEquals(400,http.postForEntity(url,new org.springframework.http.HttpEntity<>(invalid,staff.getHeaders()),JsonNode.class).getStatusCode().value());
        }
        assertEquals(202,http.postForEntity(url,cashHttpRequest("ADMIN","RC-"+f[1],"5000000"),JsonNode.class).getStatusCode().value());
    }

    /** Audit từ chối phải commit dù giao dịch nghiệp vụ rollback; không hồi sinh trạng thái kết thúc. */
    @Test void cashRejectsInvalidEnrollmentAndKeepsRejectionAudit() {
        for(String status:List.of("CANCELLED","CANCELLED_REFUND_PENDING","EXPIRED","REJECTED","APPROVED")) {
            int[] f=cashFixture(); jdbc.update("UPDATE admission.enrollments SET status=? WHERE id=?",status,f[1]);
            assertEquals(400,http.postForEntity(cashUrl(f[1]),cashHttpRequest("STAFF","RC-"+f[1],"5000000"),JsonNode.class).getStatusCode().value());
            assertEquals(status,jdbc.queryForObject("SELECT status FROM admission.enrollments WHERE id=?",String.class,f[1]));
            assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM admission.cash_confirmation_audit WHERE enrollment_id=? AND outcome='REJECTED'",Integer.class,f[1]));
        }
        int[] expired=cashFixture(); jdbc.update("UPDATE admission.enrollments SET hold_expires_at=CURRENT_TIMESTAMP-INTERVAL '1 second' WHERE id=?",expired[1]);
        assertEquals(400,http.postForEntity(cashUrl(expired[1]),cashHttpRequest("STAFF","RC-"+expired[1],"5000000"),JsonNode.class).getStatusCode().value());
        assertEquals("PENDING",jdbc.queryForObject("SELECT status FROM admission.enrollments WHERE id=?",String.class,expired[1]));
        int[] cancelled=cashFixture(); capacity.cancelClass(cancelled[0],"QA");
        assertEquals(400,http.postForEntity(cashUrl(cancelled[1]),cashHttpRequest("STAFF","RC-"+cancelled[1],"5000000"),JsonNode.class).getStatusCode().value());
        int[] missing=cashFixture(); jdbc.update("UPDATE admission.enrollments SET invoice_code=NULL WHERE id=?",missing[1]);
        assertEquals(400,http.postForEntity(cashUrl(missing[1]),cashHttpRequest("STAFF","RC-"+missing[1],"5000000"),JsonNode.class).getStatusCode().value());
    }

    /** Hai HTTP requests đồng thời không được tạo hai yêu cầu cho cùng chứng từ. */
    @Test void concurrentCashRequestsAreIdempotent() throws Exception {
        int[] f=cashFixture(); var request=cashHttpRequest("STAFF","RC-"+f[1],"5000000");
        ExecutorService pool=Executors.newFixedThreadPool(2); CountDownLatch start=new CountDownLatch(1);
        try {
            Callable<Integer> send=()->{start.await();return http.postForEntity(cashUrl(f[1]),request,JsonNode.class).getStatusCode().value();};
            Future<Integer> a=pool.submit(send),b=pool.submit(send); start.countDown();
            assertEquals(202,a.get(15,TimeUnit.SECONDS));assertEquals(202,b.get(15,TimeUnit.SECONDS));
            assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM admission.cash_confirmation_requests WHERE enrollment_id=?",Integer.class,f[1]));
            assertEquals("PENDING",jdbc.queryForObject("SELECT status FROM admission.enrollments WHERE id=?",String.class,f[1]));
        } finally {pool.shutdownNow();}
    }

    /** Chứng từ dùng ở hai đơn khác lớp vẫn chỉ được nhận một lần nhờ UNIQUE ở PostgreSQL. */
    @Test void concurrentReceiptAcrossEnrollmentsCannotBeReused() throws Exception {
        int[] a=cashFixture(),b=cashFixture(); var request=cashHttpRequest("STAFF","SHARED-"+a[1],"5000000");
        ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
        try {
            Future<Integer> first=pool.submit(()->{start.await();return http.postForEntity(cashUrl(a[1]),request,JsonNode.class).getStatusCode().value();});
            Future<Integer> second=pool.submit(()->{start.await();return http.postForEntity(cashUrl(b[1]),request,JsonNode.class).getStatusCode().value();});
            start.countDown();var results=new java.util.ArrayList<>(List.of(first.get(15,TimeUnit.SECONDS),second.get(15,TimeUnit.SECONDS)));java.util.Collections.sort(results);
            assertEquals(List.of(202,409),results);
        } finally {pool.shutdownNow();}
    }

    /** Gửi yêu cầu cạnh tranh với cascade không thể duyệt hoặc ghi đè đơn đã hủy. */
    @Test void cashRacingCancellationPreservesCascade() throws Exception {
        int[] f=cashFixture(); var request=cashHttpRequest("STAFF","RC-"+f[1],"5000000"); ExecutorService pool=Executors.newFixedThreadPool(2);
        try {
            Future<Integer> cash=pool.submit(()->http.postForEntity(cashUrl(f[1]),request,JsonNode.class).getStatusCode().value());
            Future<?> cancel=pool.submit(()->capacity.cancelClass(f[0],"QA concurrent"));
            assertTrue(List.of(202,400).contains(cash.get(15,TimeUnit.SECONDS)));cancel.get(15,TimeUnit.SECONDS);
            assertEquals("CANCELLED",jdbc.queryForObject("SELECT status FROM admission.enrollments WHERE id=?",String.class,f[1]));
            assertEquals("CANCELLED_REFUND_PENDING",jdbc.queryForObject("SELECT status FROM admission.enrollments WHERE id=?",String.class,f[2]));
        } finally {pool.shutdownNow();}
    }

    /** Chỉ callback đã xác thực mới duyệt; yêu cầu tiền mặt tự nó không phải bằng chứng PAID. */
    @Test void cashRequestUsesExistingAuthenticatedPaymentCallback() {
        int[] f=cashFixture();assertEquals(202,http.postForEntity(cashUrl(f[1]),cashHttpRequest("STAFF","RC-"+f[1],"5000000"),JsonNode.class).getStatusCode().value());
        var event=Map.of("event_id","EVT-CASH-"+f[1],"enrollment_id",f[1],"invoice_code","INV-CASH-"+f[1],"amount_paid",5000000);
        assertEquals(403,http.postForEntity("/api/admission/webhooks/payment-success",event,JsonNode.class).getStatusCode().value());
        var headers=new org.springframework.http.HttpHeaders();headers.set("X-Internal-Secret",qaInternalSecret);
        var callback=new org.springframework.http.HttpEntity<>(event,headers);
        assertEquals(200,http.postForEntity("/api/admission/webhooks/payment-success",callback,JsonNode.class).getStatusCode().value());
        assertEquals(200,http.postForEntity("/api/admission/webhooks/payment-success",callback,JsonNode.class).getStatusCode().value());
        assertEquals("APPROVED",jdbc.queryForObject("SELECT status FROM admission.enrollments WHERE id=?",String.class,f[1]));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM admission.processed_finance_events WHERE finance_event_id=?",Integer.class,"EVT-CASH-"+f[1]));
    }

    /** Bổ sung hóa đơn đã được cấp cho fixture giữ chỗ; không tạo thanh toán Finance giả. */
    private int[] cashFixture() {int[] f=cancellationFixture();jdbc.update("UPDATE admission.enrollments SET invoice_code=? WHERE id=?","INV-CASH-"+f[1],f[1]);return f;}
    /** Endpoint tương thích của Admission dành cho yêu cầu xác nhận, không phải endpoint Finance. */
    private String cashUrl(int id) {return "/api/admission/enrollments/"+id+"/approve-manual";}

    /** Lỗi audit không được để lại yêu cầu đã commit hoặc báo tiếp nhận thành công. */
    @Test void cashAuditFailureRollsBackRequest() {
        int[] f=cashFixture();
        org.mockito.Mockito.doThrow(new org.springframework.dao.DataAccessResourceFailureException("QA audit unavailable"))
                .when(cashRequests).audit(org.mockito.ArgumentMatchers.eq(f[1]),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.eq("REQUESTED"),org.mockito.ArgumentMatchers.any());
        assertEquals(500,http.postForEntity(cashUrl(f[1]),cashHttpRequest("STAFF","RC-"+f[1],"5000000"),JsonNode.class).getStatusCode().value());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM admission.cash_confirmation_requests WHERE enrollment_id=?",Integer.class,f[1]));
        assertEquals("PENDING",jdbc.queryForObject("SELECT status FROM admission.enrollments WHERE id=?",String.class,f[1]));
    }

    /** Không có deadline hoặc đã đủ sĩ số thì không gửi yêu cầu thu tiền mới. */
    @Test void cashRejectsUnknownDeadlineAndFullClass() {
        int[] missing=cashFixture();jdbc.update("UPDATE admission.enrollments SET hold_expires_at=NULL WHERE id=?",missing[1]);
        assertEquals(400,http.postForEntity(cashUrl(missing[1]),cashHttpRequest("STAFF","RC-"+missing[1],"5000000"),JsonNode.class).getStatusCode().value());
        int[] full=cashFixture();jdbc.update("UPDATE admission.classes SET min_students=1,max_students=1 WHERE id=?",full[0]);
        assertEquals(400,http.postForEntity(cashUrl(full[1]),cashHttpRequest("STAFF","RC-"+full[1],"5000000"),JsonNode.class).getStatusCode().value());
    }

    /** PUT và alias cash_amount/note theo BA mới vẫn qua cùng guard, giữ chính xác số tiền. */
    @Test void cashPutAndBaAliasesAreCompatible() {
        int[] f=cashFixture();var auth=cashHttpRequest("STAFF","RC-"+f[1],"5000000");
        var payload=new org.springframework.http.HttpEntity<>(Map.of("receipt_number","RC-"+f[1],"cash_amount","12345.67","note","BA alias"),auth.getHeaders());
        var result=http.exchange(cashUrl(f[1]),org.springframework.http.HttpMethod.PUT,payload,JsonNode.class);
        assertEquals(202,result.getStatusCode().value(),String.valueOf(result.getBody()));
        assertEquals("NEEDS_FINANCE_CONFIRMATION",result.getBody().get("data").get("cash_confirmation").get("status").asText());
        assertEquals(0,new java.math.BigDecimal("12345.67").compareTo(jdbc.queryForObject("SELECT amount FROM admission.cash_confirmation_requests WHERE enrollment_id=?",java.math.BigDecimal.class,f[1])));
        var details=http.exchange("/api/admission/enrollments/"+f[1],org.springframework.http.HttpMethod.GET,new org.springframework.http.HttpEntity<>(auth.getHeaders()),JsonNode.class);
        assertEquals("NEEDS_FINANCE_CONFIRMATION",details.getBody().get("data").get("cash_confirmation").get("status").asText());
    }

    /** Request và callback tranh chấp: chỉ callback có secret mới đưa đơn sang APPROVED, không tạo hai event. */
    @Test void cashRacingFinanceCallbackUsesSameClassLock() throws Exception {
        int[] f=cashFixture();var request=cashHttpRequest("STAFF","RC-"+f[1],"5000000");
        var headers=new org.springframework.http.HttpHeaders();headers.set("X-Internal-Secret",qaInternalSecret);
        var callback=new org.springframework.http.HttpEntity<>(Map.of("event_id","EVT-CASH-RACE-"+f[1],"enrollment_id",f[1],"invoice_code","INV-CASH-"+f[1]),headers);
        ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
        try {
            Future<Integer> cash=pool.submit(()->{start.await();return http.postForEntity(cashUrl(f[1]),request,JsonNode.class).getStatusCode().value();});
            Future<Integer> paid=pool.submit(()->{start.await();return http.postForEntity("/api/admission/webhooks/payment-success",callback,JsonNode.class).getStatusCode().value();});
            start.countDown();assertTrue(List.of(202,400).contains(cash.get(15,TimeUnit.SECONDS)));assertEquals(200,paid.get(15,TimeUnit.SECONDS));
            assertEquals("APPROVED",jdbc.queryForObject("SELECT status FROM admission.enrollments WHERE id=?",String.class,f[1]));
            assertEquals("ONLINE_PAYMENT",jdbc.queryForObject("SELECT approval_method FROM admission.enrollments WHERE id=?",String.class,f[1]));
        } finally {pool.shutdownNow();}
    }

    /** Duyệt thật tạo snapshot outbox; worker mới vẫn đọc được sau restart. */
    private int retryFixture(){int[] f=cashFixture();service.approveEnrollment(f[1]);return jdbc.queryForObject("SELECT student_id FROM admission.enrollments WHERE id=?",Integer.class,f[1]);}
    private StudentSyncRetryWorker retryWorker(boolean verified){var w=new StudentSyncRetryWorker(studentOutbox,studentClient);org.springframework.test.util.ReflectionTestUtils.setField(w,"enabled",true);org.springframework.test.util.ReflectionTestUtils.setField(w,"verified",verified);return w;}
    private String retryState(int u){return jdbc.queryForObject("SELECT state FROM admission.student_sync_outbox WHERE user_id=?",String.class,u);}
    private void retryDue(int u){jdbc.update("UPDATE admission.student_sync_outbox SET next_attempt_at=CURRENT_TIMESTAMP-INTERVAL '1 second' WHERE user_id=?",u);}
    private org.mockito.stubbing.OngoingStubbing<com.englishcenter.admission.client.dto.StudentProfileData> replyFor(int u){return org.mockito.Mockito.when(studentClient.syncStudentProfile(org.mockito.ArgumentMatchers.eq(u),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any()));}
    /** Mất kết nối trước gửi: chờ backoff, instance mới retry thành công; replay không gửi thêm. */
    @Test void retryOfflineRestartThenSuccess(){int u=retryFixture();replyFor(u).thenThrow(new org.springframework.web.client.ResourceAccessException("offline",new java.net.ConnectException())).thenReturn(new com.englishcenter.admission.client.dto.StudentProfileData(500,"STU-QA","STUDYING"));
      retryWorker(false).runForUser(u);assertEquals("RETRY",retryState(u));
      assertTrue(jdbc.queryForObject("SELECT next_attempt_at>CURRENT_TIMESTAMP FROM admission.student_sync_outbox WHERE user_id=?",Boolean.class,u));
      retryWorker(false).runForUser(u);retryDue(u);retryWorker(false).runForUser(u);assertEquals("SUCCEEDED",retryState(u));retryWorker(false).runForUser(u);
      assertEquals(2,jdbc.queryForObject("SELECT attempts FROM admission.student_sync_outbox WHERE user_id=?",Integer.class,u));assertEquals("SYNCED",jdbc.queryForObject("SELECT student_sync_status FROM admission.enrollments WHERE student_id=?",String.class,u));}
    /** Timeout có thể Student đã nhận: phải REVIEW khi chưa xác minh idempotency. */
    @Test void retryAmbiguousTimeoutIsNotReplayed(){int u=retryFixture();replyFor(u).thenThrow(new org.springframework.web.client.ResourceAccessException("timeout",new java.net.SocketTimeoutException()));retryWorker(false).runForUser(u);assertEquals("REVIEW",retryState(u));retryDue(u);retryWorker(false).runForUser(u);assertEquals(1,jdbc.queryForObject("SELECT attempts FROM admission.student_sync_outbox WHERE user_id=?",Integer.class,u));}
    /** HTTP 503 chỉ retry khi idempotency được xác minh, tối đa 5 lần. */
    @Test void retryHttp503IsBounded(){int u=retryFixture();replyFor(u).thenThrow(new org.springframework.web.client.HttpServerErrorException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE));for(int i=0;i<5;i++){retryDue(u);retryWorker(true).runForUser(u);}assertEquals("EXHAUSTED",retryState(u));retryDue(u);retryWorker(true).runForUser(u);assertEquals(5,jdbc.queryForObject("SELECT attempts FROM admission.student_sync_outbox WHERE user_id=?",Integer.class,u));}
    /** Hai worker chỉ lấy một job; crash và lease hết hạn không cho worker cũ báo success. */
    @Test void retryConcurrentClaimAndCrashRecovery() throws Exception {int u=retryFixture();var pool=Executors.newFixedThreadPool(2);try{var a=pool.submit(()->studentOutbox.claim(u));var b=pool.submit(()->studentOutbox.claim(u));var x=a.get(10,TimeUnit.SECONDS);var y=b.get(10,TimeUnit.SECONDS);assertEquals(1,(x.isPresent()?1:0)+(y.isPresent()?1:0));var old=x.orElseGet(y::orElseThrow);jdbc.update("UPDATE admission.student_sync_outbox SET lease_until=CURRENT_TIMESTAMP-INTERVAL '1 second' WHERE user_id=?",u);studentOutbox.recover(false);studentOutbox.finish(old,"SUCCEEDED",123,null);assertEquals("REVIEW",retryState(u));}finally{pool.shutdownNow();}}
    /** Cascade trước gửi không được dispatch hoặc hồi sinh đơn. */
    @Test void retryCancelledEnrollmentIsNotSent(){int[] f=cashFixture();service.approveEnrollment(f[1]);int u=jdbc.queryForObject("SELECT student_id FROM admission.enrollments WHERE id=?",Integer.class,f[1]);capacity.cancelClass(f[0],"QA retry");retryWorker(true).runForUser(u);assertEquals("CANCELLED",retryState(u));org.mockito.Mockito.verifyNoInteractions(studentClient);}
    /** Outbox lỗi phải rollback duyệt, không commit APPROVED thiếu yêu cầu bền vững. */
    @Test void retryOutboxFailureRollsBackApproval(){int[] f=cashFixture();org.mockito.Mockito.doThrow(new org.springframework.dao.DataAccessResourceFailureException("QA outbox")).when(studentOutbox).enqueue(org.mockito.ArgumentMatchers.argThat(e->e!=null&&e.getId().equals(f[1])),org.mockito.ArgumentMatchers.any());assertThrows(org.springframework.dao.DataAccessResourceFailureException.class,()->service.approveEnrollment(f[1]));assertEquals("PENDING",jdbc.queryForObject("SELECT status FROM admission.enrollments WHERE id=?",String.class,f[1]));}
    /** ACK rỗng và HTTP 404 không được coi là đã đồng bộ. */
    @Test void retryInvalidAckAndHttp404RequireReview(){int u=retryFixture();retryWorker(true).runForUser(u);assertEquals("REVIEW",retryState(u));int v=retryFixture();replyFor(v).thenThrow(new org.springframework.web.client.HttpClientErrorException(org.springframework.http.HttpStatus.NOT_FOUND));retryWorker(true).runForUser(v);assertEquals("REVIEW",retryState(v));}
    /** Gửi lại enqueue giữ một job/user; mặc định dispatcher tắt. */
    @Test void retryEnqueueIsIdempotentAndDefaultDisabled(){int u=retryFixture();int id=jdbc.queryForObject("SELECT enrollment_id FROM admission.student_sync_outbox WHERE user_id=?",Integer.class,u);var e=cancellationEnrollments.findById(id).orElseThrow();var user=new com.englishcenter.admission.entity.User();user.setId(u);user.setFullName("QA");user.setEmail("qa@example.test");studentOutbox.enqueue(e,user);assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM admission.student_sync_outbox WHERE user_id=?",Integer.class,u));new StudentSyncRetryWorker(studentOutbox,studentClient).runForUser(u);org.mockito.Mockito.verifyNoInteractions(studentClient);}

    /** Cờ legacy đã synced nhưng thiếu journal/ID không cho gửi lại hồ sơ có thể đã tồn tại. */
    @Test void retryLegacySyncedFlagRequiresReconciliation(){int[] f=cashFixture();jdbc.update("UPDATE admission.enrollments SET is_synced_to_student=TRUE WHERE id=?",f[1]);service.approveEnrollment(f[1]);int u=jdbc.queryForObject("SELECT student_id FROM admission.enrollments WHERE id=?",Integer.class,f[1]);assertEquals("REVIEW",retryState(u));retryWorker(false).runForUser(u);org.mockito.Mockito.verifyNoInteractions(studentClient);}

    @Autowired BiAnalyticsService bi;
    /** Dataset độc lập: 8 đơn, 2 approved, 2 expired hiệu lực, capacity 40; expected tính tay. */
    private int[] biFixture(){
      String key="qa-bi-"+UUID.randomUUID();
      int course=jdbc.queryForObject("INSERT INTO admission.courses(code,name,level,duration_weeks,tuition_amount) VALUES (?,?,'BEGINNER',1,9000000) RETURNING id",Integer.class,key,key);
      int a=jdbc.queryForObject("INSERT INTO admission.classes(course_id,code,name,day_pattern,start_time,end_time,start_date,end_date,status,min_students,max_students) VALUES (?,?,?,'DAILY','08:00','09:00','2020-01-01','2020-12-31','OPEN',1,10) RETURNING id",Integer.class,course,key+"a",key);
      int b=jdbc.queryForObject("INSERT INTO admission.classes(course_id,code,name,day_pattern,start_time,end_time,start_date,end_date,status,min_students,max_students) VALUES (?,?,?,'DAILY','08:00','09:00','2020-01-01','2020-12-31','IN_PROGRESS',1,30) RETURNING id",Integer.class,course,key+"b",key);
      int cancelled=jdbc.queryForObject("INSERT INTO admission.classes(course_id,code,name,day_pattern,start_time,end_time,start_date,end_date,status,min_students,max_students) VALUES (?,?,?,'DAILY','08:00','09:00','2020-01-01','2020-12-31','CANCELLED',1,100) RETURNING id",Integer.class,course,key+"c",key);
      int first=0;
      String[] states={"APPROVED","APPROVED","PENDING","PENDING","EXPIRED","CANCELLED","CANCELLED_REFUND_PENDING","REJECTED"};
      for(int i=0;i<states.length;i++){
        int u=jdbc.queryForObject("INSERT INTO admission.users(email,password_hash,full_name,role) VALUES (?,'unused','QA BI','STUDENT') RETURNING id",Integer.class,key+i+"@example.test");if(i==0)first=u;
        jdbc.update("INSERT INTO admission.enrollments(student_id,class_id,status,enrolled_at,approved_at,hold_expires_at,enrollment_channel) VALUES (?,?,?,?::timestamptz,?::timestamptz,?::timestamptz,?)",u,i==1?b:i==6?cancelled:a,states[i],"2020-02-01T10:00:00+07:00",i<2?"2020-02-02T10:00:00+07:00":null,i==2?"2020-03-01T10:00:00+07:00":"2020-02-02T10:00:00+07:00",i<2?"AT_COUNTER":"ONLINE");
      }
      int set=jdbc.queryForObject("INSERT INTO admission.test_sets(code,title,duration_minutes) VALUES (?,?,10) RETURNING id",Integer.class,key,key);
      for(int i=0;i<3;i++)jdbc.update("INSERT INTO admission.test_attempts(user_id,test_set_id,status,started_at,expires_at,submitted_at) VALUES (?,?,?,?::timestamptz,?::timestamptz,?::timestamptz)",i<2?first:jdbc.queryForObject("SELECT student_id FROM admission.enrollments WHERE class_id=? AND status='APPROVED'",Integer.class,b),set,i<2?"SUBMITTED":"STARTED","2020-01-31T23:50:00+07:00","2020-02-01T00:20:00+07:00",i<2?"2020-02-01T00:05:00+07:00":null);
      return new int[]{course,a,b,first};
    }
    private com.englishcenter.admission.dto.bi.BiWindow biWindow(int course){return new com.englishcenter.admission.dto.bi.BiWindow(java.time.LocalDate.of(2020,1,31),java.time.LocalDate.of(2020,2,1),course,null,null,10,java.time.OffsetDateTime.parse("2020-02-20T12:00:00+07:00"));}
    /** Cohort STARTED, distinct user, 14 ngày, mọi trạng thái và quá hạn trước scheduler. */
    @Test @org.springframework.transaction.annotation.Transactional void biCohortFormulasUseIndependentExpectedValues(){
      int[] f=biFixture();var d=bi.funnel(biWindow(f[0]));var k=(Map<?,?>)d.get("kpis");var c=(Map<?,?>)d.get("counts");
      assertEquals(66.67,k.get("test_completion_rate"));assertEquals(100.0,k.get("test_to_enrollment_rate"));assertEquals(25.0,k.get("paid_conversion_rate"));assertEquals(25.0,k.get("expired_hold_rate"));
      assertEquals(8L,((Number)c.get("total_enrollments")).longValue());assertEquals(2L,((Number)c.get("cancelled_enrollments")).longValue());assertEquals(1L,((Number)c.get("submitted_users")).longValue());
      assertEquals(2L,((Number)c.get("counter_enrollments")).longValue());
    }
    /** Không dùng tiền niêm yết 9 triệu để giả doanh thu; occupancy 2/40=5%, không lấy trung bình 10% và 3.33%. */
    @Test @org.springframework.transaction.annotation.Transactional void biRankingOccupancyAndFinanceUnavailable(){
      int[] f=biFixture();var d=bi.courses(biWindow(f[0]));assertNull(d.get("realized_admission_revenue"));assertNull(d.get("pipeline_potential_revenue"));assertEquals("UNAVAILABLE",d.get("finance_status"));assertEquals(5.0,d.get("average_occupancy_rate"));
      var row=(Map<?,?>)((List<?>)d.get("top_courses")).getFirst();assertEquals(8L,((Number)row.get("enrolled_count")).longValue());assertEquals(1L,((Number)row.get("pending_enrollments")).longValue());assertNull(row.get("realized_revenue"));
    }
    /** Nửa khoảng ngày Việt Nam: đúng 00:00 ngày sau bị loại dù instant UTC thuộc ngày trước. */
    @Test @org.springframework.transaction.annotation.Transactional void biDayBoundaryAndChannelFilter(){
      int[] f=biFixture();jdbc.update("UPDATE admission.enrollments SET enrolled_at='2020-02-02T00:00:00+07:00'::timestamptz WHERE class_id=? AND status='REJECTED'",f[1]);
      var w=biWindow(f[0]);var c=(Map<?,?>)bi.funnel(w).get("counts");assertEquals(7L,((Number)c.get("total_enrollments")).longValue());
      var filter=new com.englishcenter.admission.dto.bi.BiWindow(w.from(),w.to(),w.courseId(),"AT_COUNTER",null,10,w.asOf());assertEquals(100.0,((Map<?,?>)bi.funnel(filter).get("kpis")).get("paid_conversion_rate"));
    }
    /** Cùng một người đăng nhập lặp: distinct theo từng ngày/tháng, failure không đếm refresh. */
    @Test @org.springframework.transaction.annotation.Transactional void biCalendarActivityAndFailedLoginRate(){
      String key="qa-bi-"+UUID.randomUUID();int u=jdbc.queryForObject("INSERT INTO admission.users(email,password_hash,full_name,role) VALUES (?,'unused','QA BI','TEACHER') RETURNING id",Integer.class,key+"@example.test");
      for(String time:List.of("2020-01-31T23:59:59+07:00","2020-01-31T23:59:59.5+07:00","2020-02-01T00:00:00+07:00","2020-02-02T00:00:00+07:00"))jdbc.update("INSERT INTO admission.auth_audit_logs(user_id,action,created_at) VALUES (?,'LOGIN_SUCCESS',?::timestamptz)",u,time);
      jdbc.update("INSERT INTO admission.auth_audit_logs(user_id,action,created_at) VALUES (?,'LOGIN_FAILED','2020-02-01T12:00:00+07:00')",u);
      jdbc.update("INSERT INTO admission.auth_audit_logs(user_id,action,created_at) VALUES (?,'REFRESH_TOKEN','2020-02-01T12:00:00+07:00')",u);
      var base=biWindow(1);var w=new com.englishcenter.admission.dto.bi.BiWindow(base.from(),base.to(),null,null,"TEACHER",10,base.asOf());var d=bi.security(w);var e=(Map<?,?>)d.get("engagement");
      assertEquals(1L,((Number)e.get("dau")).longValue());assertEquals(1L,((Number)e.get("mau")).longValue());assertEquals(25.0,e.get("failed_login_rate"));assertEquals(2,((List<?>)d.get("daily_by_role")).size());assertEquals(2,((List<?>)d.get("monthly_by_role")).size());
    }
    /** Đúng 14 ngày được tính, sau một microsecond không tính; nhiều lượt cùng user không nhân đôi. */
    @Test @org.springframework.transaction.annotation.Transactional void biConversionWindowBoundary(){int[] f=biFixture();jdbc.update("UPDATE admission.enrollments SET enrolled_at='2020-02-15T00:05:00+07:00' WHERE student_id=?",f[3]);assertEquals(100.0,((Map<?,?>)bi.funnel(biWindow(f[0])).get("kpis")).get("test_to_enrollment_rate"));jdbc.update("UPDATE admission.enrollments SET enrolled_at=enrolled_at+INTERVAL '1 microsecond' WHERE student_id=?",f[3]);assertEquals(0.0,((Map<?,?>)bi.funnel(biWindow(f[0])).get("kpis")).get("test_to_enrollment_rate"));}

    /** Cảnh báo hơn 5 lần sai liên tiếp trong 10 phút, đăng nhập thành công reset chuỗi. */
    @Test @org.springframework.transaction.annotation.Transactional void biConsecutiveFailureSignalResetsOnSuccess(){int[] f=biFixture();for(int i=0;i<6;i++)jdbc.update("INSERT INTO admission.auth_audit_logs(user_id,action,created_at) VALUES (?,'LOGIN_FAILED','2020-02-20T11:59:00+07:00')",f[3]);assertEquals(1,((List<?>)bi.security(biWindow(f[0])).get("consecutive_failed_accounts")).size());jdbc.update("INSERT INTO admission.auth_audit_logs(user_id,action,created_at) VALUES (?,'LOGIN_SUCCESS','2020-02-20T11:59:30+07:00')",f[3]);assertTrue(((List<?>)bi.security(biWindow(f[0])).get("consecutive_failed_accounts")).isEmpty());for(int i=0;i<6;i++)jdbc.update("INSERT INTO admission.auth_audit_logs(user_id,action,created_at) VALUES (?,'LOGIN_FAILED','2020-02-20T11:59:30+07:00')",f[3]);assertEquals(1,((List<?>)bi.security(biWindow(f[0])).get("consecutive_failed_accounts")).size());}

    /** Bộ dữ liệu trống: null rate/tiền, không 0% giả, không có khóa học. */
    @Test @org.springframework.transaction.annotation.Transactional void biEmptyReportsReturnNA(){var d=bi.courses(biWindow(Integer.MAX_VALUE));assertTrue(((List<?>)d.get("top_courses")).isEmpty());assertNull(d.get("average_occupancy_rate"));assertNull(((Map<?,?>)bi.funnel(biWindow(Integer.MAX_VALUE)).get("kpis")).get("paid_conversion_rate"));}
    /** HTTP/JWT thực: ADMIN-only cho an ninh ở cả alias BA; không rò IP cho STAFF/Student. */
    @Test void biApiPermissionsAndFilters(){
      var staff=cashHttpRequest("STAFF","BI","1");var student=cashHttpRequest("STUDENT","BI","1");var admin=cashHttpRequest("ADMIN","BI","1");
      for(String url:List.of("/api/admission/bi/security-engagement","/api/admission/analytics/user-activity")){
       assertEquals(403,http.exchange(url,org.springframework.http.HttpMethod.GET,new org.springframework.http.HttpEntity<>(staff.getHeaders()),JsonNode.class).getStatusCode().value());
       assertEquals(200,http.exchange(url,org.springframework.http.HttpMethod.GET,new org.springframework.http.HttpEntity<>(admin.getHeaders()),JsonNode.class).getStatusCode().value());}
      assertEquals(403,http.exchange("/api/admission/bi/funnel",org.springframework.http.HttpMethod.GET,new org.springframework.http.HttpEntity<>(student.getHeaders()),JsonNode.class).getStatusCode().value());
      assertEquals(200,http.exchange("/api/admission/analytics/courses-ranking?limit=1",org.springframework.http.HttpMethod.GET,new org.springframework.http.HttpEntity<>(staff.getHeaders()),JsonNode.class).getStatusCode().value());
      assertEquals(400,http.exchange("/api/admission/bi/funnel?from_date=2020-02-02&to_date=2020-01-01",org.springframework.http.HttpMethod.GET,new org.springframework.http.HttpEntity<>(staff.getHeaders()),JsonNode.class).getStatusCode().value());
      for(String query:List.of("from_date=bad-date","limit=0","as_of=2020-01-01","days=367"))assertEquals(400,http.exchange("/api/admission/bi/funnel?"+query,org.springframework.http.HttpMethod.GET,new org.springframework.http.HttpEntity<>(staff.getHeaders()),JsonNode.class).getStatusCode().value());
      assertEquals(401,http.getForEntity("/api/admission/bi/funnel",JsonNode.class).getStatusCode().value());

    }

    @Autowired RoomService bookingRooms;
    @SpyBean com.englishcenter.admission.engine.ConflictEngine bookingEngine;
    /** Cơ sở QA riêng: phòng và giảng viên chưa có lịch, không thay dữ liệu cũ. */
    private int[] bookingFixture(int capacity){String key="qa-book-"+UUID.randomUUID();int course=jdbc.queryForObject("INSERT INTO admission.courses(code,name,level,duration_weeks) VALUES (?,?,'BEGINNER',1) RETURNING id",Integer.class,key,key);int room=jdbc.queryForObject("INSERT INTO admission.rooms(code,name,capacity) VALUES (?,?,?) RETURNING id",Integer.class,key,key,capacity);int teacher=jdbc.queryForObject("INSERT INTO admission.users(email,password_hash,full_name,role) VALUES (?,'unused','QA booking','TEACHER') RETURNING id",Integer.class,key+"@example.test");return new int[]{course,room,teacher};}
    private com.englishcenter.admission.dto.clazz.CreateClassRequest bookingRequest(int[] f,int room,Integer teacher,String pattern,java.time.LocalTime start){return new com.englishcenter.admission.dto.clazz.CreateClassRequest("BOOK-"+UUID.randomUUID(),"QA booking",f[0],room,teacher,pattern,start,start.plusHours(1),java.time.LocalDate.now().plusDays(7),java.time.LocalDate.now().plusDays(35),1,25);}
    private com.englishcenter.admission.dto.room.UpdateRoomRequest resizeRequest(int size){var r=new com.englishcenter.admission.dto.room.UpdateRoomRequest();r.setName("QA resized");r.setCapacity(size);r.setIsActive(true);return r;}
    private String bookingOutcome(Runnable action){try{action.run();return "OK";}catch(BusinessException ex){return ex.getErrorCode().name();}}
    /** Giữ transaction đầu sau validation, quan sát transaction thứ hai trước khi cho commit. */
    private List<String> bookingRace(java.util.function.Predicate<com.englishcenter.admission.engine.dto.ConflictCheckRequest> target,Runnable first,Runnable second) throws Exception {
      CountDownLatch checked=new CountDownLatch(1),release=new CountDownLatch(1),secondStarted=new CountDownLatch(1);AtomicInteger checks=new AtomicInteger();
      org.mockito.Mockito.doAnswer(inv->{Object result=inv.callRealMethod();if(target.test(inv.getArgument(0))&&checks.incrementAndGet()==1){checked.countDown();if(!release.await(10,TimeUnit.SECONDS))throw new IllegalStateException("QA booking synchronization");}return result;}).when(bookingEngine).validateScheduleConflict(org.mockito.ArgumentMatchers.any());
      ExecutorService pool=Executors.newFixedThreadPool(2);try{Future<String> a=pool.submit(()->bookingOutcome(first));assertTrue(checked.await(10,TimeUnit.SECONDS));Future<String> b=pool.submit(()->{secondStarted.countDown();return bookingOutcome(second);});assertTrue(secondStarted.await(5,TimeUnit.SECONDS));boolean early;try{b.get(1,TimeUnit.SECONDS);early=true;}catch(TimeoutException expected){early=false;}finally{release.countDown();}var result=List.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS));assertFalse(early,"Second scheduling writer must wait until first transaction commits");return result;}finally{release.countDown();pool.shutdownNow();}
    }
    /** H02: không cho giảm phòng30 xuống10 khi đã có lớp max25; phải rollback. */
    @Test void bookingCannotShrinkRoomBelowAssignedClass(){int[] f=bookingFixture(30);capacity.createClass(bookingRequest(f,f[1],null,"DAILY",java.time.LocalTime.of(8,0)));assertThrows(BusinessException.class,()->bookingRooms.updateRoom(f[1],resizeRequest(10)));assertEquals(30,jdbc.queryForObject("SELECT capacity FROM admission.rooms WHERE id=?",Integer.class,f[1]));bookingRooms.updateRoom(f[1],resizeRequest(25));assertEquals(25,jdbc.queryForObject("SELECT capacity FROM admission.rooms WHERE id=?",Integer.class,f[1]));}
    /** H03: hai create cùng phòng và ca chỉ một được commit. */
    @Test void bookingConcurrentRoomCreatesSerialize() throws Exception {int[] f=bookingFixture(30);var a=bookingRequest(f,f[1],null,"DAILY",java.time.LocalTime.of(8,0));var b=bookingRequest(f,f[1],null,"DAILY",java.time.LocalTime.of(8,0));assertEquals(List.of("OK","ADM_ROOM_CONFLICT"),bookingRace(q->java.util.Objects.equals(q.getRoomId(),f[1]),()->capacity.createClass(a),()->capacity.createClass(b)));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM admission.classes WHERE room_id=?",Integer.class,f[1]));}
    /** Cùng teacher dù khác phòng vẫn serialize và từ chối lịch trùng. */
    @Test void bookingConcurrentTeacherCreatesSerialize() throws Exception {int[] f=bookingFixture(30),other=bookingFixture(30);var a=bookingRequest(f,f[1],f[2],"DAILY",java.time.LocalTime.of(8,0));var b=bookingRequest(f,other[1],f[2],"DAILY",java.time.LocalTime.of(8,0));assertEquals(List.of("OK","ADM_TEACHER_CONFLICT"),bookingRace(q->java.util.Objects.equals(q.getTeacherId(),f[2]),()->capacity.createClass(a),()->capacity.createClass(b)));}
    /** Check capacity/create cũng phải dùng cùng khóa với giảm sức chứa phòng. */
    @Test void bookingRoomResizeRacingCreatePreservesCapacity() throws Exception {int[] f=bookingFixture(30);var a=bookingRequest(f,f[1],null,"DAILY",java.time.LocalTime.of(8,0));assertEquals(List.of("OK","ADM_ROOM_CAPACITY_EXCEEDED"),bookingRace(q->java.util.Objects.equals(q.getRoomId(),f[1]),()->capacity.createClass(a),()->bookingRooms.updateRoom(f[1],resizeRequest(10))));}
    /** Hai update lớp riêng cùng chuyển tới một ca không được cùng commit. */
    @Test void bookingConcurrentClassUpdatesSerialize() throws Exception {int[] f=bookingFixture(30);int a=capacity.createClass(bookingRequest(f,f[1],null,"DAILY",java.time.LocalTime.of(8,0))).getId();int b=capacity.createClass(bookingRequest(f,f[1],null,"DAILY",java.time.LocalTime.of(10,0))).getId();var patch=new com.englishcenter.admission.dto.clazz.UpdateClassRequest();patch.setStartTime(java.time.LocalTime.of(12,0));patch.setEndTime(java.time.LocalTime.of(13,0));assertEquals(List.of("OK","ADM_ROOM_CONFLICT"),bookingRace(q->java.util.Objects.equals(q.getRoomId(),f[1]),()->capacity.updateClass(a,patch),()->capacity.updateClass(b,patch)));}

    @SpyBean com.englishcenter.admission.repository.RoomRepository bookingRoomRepo;
    @jakarta.persistence.PersistenceContext jakarta.persistence.EntityManager bookingEntityManager;
    /** Class update tăng max phải phối hợp với transaction giảm phòng. */
    @Test void bookingRoomResizeRacingClassUpdatePreservesCapacity() throws Exception {int[] f=bookingFixture(30);var request=bookingRequest(f,f[1],null,"DAILY",java.time.LocalTime.of(8,0));request.setMaxStudents(10);int id=capacity.createClass(request).getId();var patch=new com.englishcenter.admission.dto.clazz.UpdateClassRequest();patch.setMaxStudents(25);assertEquals(List.of("OK","ADM_ROOM_CAPACITY_EXCEEDED"),bookingRace(q->java.util.Objects.equals(q.getClassId(),id),()->capacity.updateClass(id,patch),()->bookingRooms.updateRoom(f[1],resizeRequest(10))));}
    /** PATCH status phòng không được ghi lại capacity từ entity cũ sau resize/create đồng thời. */
    @Test void bookingRoomStatusCannotRestoreStaleCapacity() throws Exception {
      int[] f=bookingFixture(10);CountDownLatch read=new CountDownLatch(1),release=new CountDownLatch(1);AtomicInteger reads=new AtomicInteger();
      // Spring Data findById là interface: đọc EntityManager thật cùng transaction thay vì callRealMethod abstract.
      org.mockito.Mockito.doAnswer(inv->{Object value=java.util.Optional.ofNullable(bookingEntityManager.find(com.englishcenter.admission.entity.Room.class,inv.getArgument(0)));if(reads.incrementAndGet()==1){read.countDown();if(!release.await(10,TimeUnit.SECONDS))throw new IllegalStateException("QA room read");}return value;}).when(bookingRoomRepo).findById(f[1]);
      ExecutorService pool=Executors.newFixedThreadPool(2);try{Future<?> first=pool.submit(()->bookingRooms.updateRoomStatus(f[1],false));if(!read.await(5,TimeUnit.SECONDS)){first.get(1,TimeUnit.SECONDS);fail("Room status transaction never reached read barrier");}Future<?> second=pool.submit(()->{bookingRooms.updateRoom(f[1],resizeRequest(30));capacity.createClass(bookingRequest(f,f[1],null,"DAILY",java.time.LocalTime.of(8,0)));});try{second.get(1,TimeUnit.SECONDS);}catch(TimeoutException expected){}finally{release.countDown();}first.get(15,TimeUnit.SECONDS);second.get(15,TimeUnit.SECONDS);assertEquals(30,jdbc.queryForObject("SELECT capacity FROM admission.rooms WHERE id=?",Integer.class,f[1]));assertTrue(jdbc.queryForObject("SELECT r.capacity>=MAX(c.max_students) FROM admission.rooms r JOIN admission.classes c ON c.room_id=r.id WHERE r.id=? GROUP BY r.id",Boolean.class,f[1]));}finally{release.countDown();pool.shutdownNow();}
    }

    /** Giờ kề nhau không giao thoa theo BA: end1==start2 vẫn được đặt, kể cả concurrent. */
    @Test void bookingAdjacentTimesRemainAllowed() throws Exception {int[] f=bookingFixture(30);var a=bookingRequest(f,f[1],f[2],"DAILY",java.time.LocalTime.of(8,0));var b=bookingRequest(f,f[1],f[2],"DAILY",java.time.LocalTime.of(9,0));assertEquals(List.of("OK","OK"),bookingRace(q->java.util.Objects.equals(q.getRoomId(),f[1]),()->capacity.createClass(a),()->capacity.createClass(b)));}
    /** Mẫu ngày không giao nhau và khoảng ngày rời nhau không bị khóa nghiệp vụ giả. */
    @Test void bookingDisjointDaysAndDateRangesRemainAllowed(){int[] f=bookingFixture(30);capacity.createClass(bookingRequest(f,f[1],f[2],"MON_WED_FRI",java.time.LocalTime.of(8,0)));capacity.createClass(bookingRequest(f,f[1],f[2],"TUE_THU_SAT",java.time.LocalTime.of(8,0)));var later=bookingRequest(f,f[1],f[2],"DAILY",java.time.LocalTime.of(8,0));later.setStartDate(java.time.LocalDate.now().plusDays(40));later.setEndDate(java.time.LocalDate.now().plusDays(60));capacity.createClass(later);assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM admission.classes WHERE room_id=?",Integer.class,f[1]));}
    /** Rollback validation phải nhả khóa transaction, request khác vẫn ghi lịch được. */
    @Test void bookingRollbackReleasesSchedulingLock() throws Exception {int[] f=bookingFixture(30);capacity.createClass(bookingRequest(f,f[1],null,"DAILY",java.time.LocalTime.of(8,0)));assertThrows(BusinessException.class,()->capacity.createClass(bookingRequest(f,f[1],null,"DAILY",java.time.LocalTime.of(8,0))));ExecutorService pool=Executors.newSingleThreadExecutor();try{pool.submit(()->capacity.createClass(bookingRequest(f,f[1],null,"DAILY",java.time.LocalTime.of(10,0)))).get(5,TimeUnit.SECONDS);}finally{pool.shutdownNow();}}

    /** Fixture lớp chưa khai giảng, một đơn giữ chỗ và một đơn đã duyệt. */
    private int[] cancellationFixture() {
        String key="qa-"+UUID.randomUUID();
        int course=jdbc.queryForObject("INSERT INTO admission.courses(code,name,level,duration_weeks) VALUES (?,?,'BEGINNER',1) RETURNING id",Integer.class,key,key);
        int clazz=jdbc.queryForObject("INSERT INTO admission.classes(course_id,code,name,day_pattern,start_time,end_time,start_date,end_date,min_students,max_students) VALUES (?,?,?,'DAILY','08:00','09:00',CURRENT_DATE+3,CURRENT_DATE+10,5,10) RETURNING id",Integer.class,course,key,key);
        int a=jdbc.queryForObject("INSERT INTO admission.users(email,password_hash,full_name,role) VALUES (?,'unused','QA','STUDENT') RETURNING id",Integer.class,key+"a@example.test");
        int b=jdbc.queryForObject("INSERT INTO admission.users(email,password_hash,full_name,role) VALUES (?,'unused','QA','STUDENT') RETURNING id",Integer.class,key+"b@example.test");
        int pending=jdbc.queryForObject("INSERT INTO admission.enrollments(student_id,class_id,hold_expires_at) VALUES (?,?,CURRENT_TIMESTAMP+INTERVAL '48 hours') RETURNING id",Integer.class,a,clazz);
        int approved=jdbc.queryForObject("INSERT INTO admission.enrollments(student_id,class_id,status,invoice_code) VALUES (?,?,'APPROVED','INV-QA') RETURNING id",Integer.class,b,clazz);
        return new int[]{clazz,pending,approved};
    }
    /** Cascade, replay và giải phóng slot đều phải được commit cùng nhau. */
    @Test void cancellationCascadesAndIsIdempotentOnPostgres() {
        int[] f=cancellationFixture(); capacity.cancelClass(f[0],"Thiếu sĩ số"); capacity.cancelClass(f[0],"Gửi lại");
        assertEquals("CANCELLED",jdbc.queryForObject("SELECT status FROM admission.enrollments WHERE id=?",String.class,f[1]));
        assertEquals("CANCELLED_REFUND_PENDING",jdbc.queryForObject("SELECT status FROM admission.enrollments WHERE id=?",String.class,f[2]));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM admission.class_cancellation_requests WHERE class_id=?",Integer.class,f[0]));
        assertEquals(0,cancellationEnrollments.countOccupiedSlots(f[0],java.time.LocalDateTime.now()));
        assertThrows(BusinessException.class,()->service.approveEnrollment(f[1]));
        service.cancelEnrollment(f[2],"Yêu cầu lại");
        assertEquals("CANCELLED_REFUND_PENDING",jdbc.queryForObject("SELECT status FROM admission.enrollments WHERE id=?",String.class,f[2]));
    }
    /** Lỗi lưu yêu cầu hoàn phí phải rollback cả đơn PENDING đã cập nhật trước đó. */
    @Test void cancellationFailureRollsBackAllLocalChanges() {
        int[] f=cancellationFixture();
        org.mockito.Mockito.doThrow(new org.springframework.dao.DataAccessResourceFailureException("QA queue unavailable")).when(cancellationEnrollments)
                .enqueueClassRefund(org.mockito.ArgumentMatchers.eq(f[2]),org.mockito.ArgumentMatchers.eq(f[0]),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any());
        assertThrows(org.springframework.dao.DataAccessResourceFailureException.class,()->capacity.cancelClass(f[0],"Thiếu sĩ số"));
        assertEquals("OPEN",jdbc.queryForObject("SELECT status FROM admission.classes WHERE id=?",String.class,f[0]));
        assertEquals("PENDING",jdbc.queryForObject("SELECT status FROM admission.enrollments WHERE id=?",String.class,f[1]));
        assertEquals("APPROVED",jdbc.queryForObject("SELECT status FROM admission.enrollments WHERE id=?",String.class,f[2]));
    }
    /** Hai thao tác hủy đồng thời chỉ tạo một yêu cầu cho mỗi đơn đã duyệt. */
    @Test void concurrentCancellationCreatesOneRequest() throws Exception {
        int[] f=cancellationFixture(); ExecutorService pool=Executors.newFixedThreadPool(2);
        try {
            Future<?> a=pool.submit(()->capacity.cancelClass(f[0],"A")); Future<?> b=pool.submit(()->capacity.cancelClass(f[0],"B"));
            a.get(15,TimeUnit.SECONDS); b.get(15,TimeUnit.SECONDS);
            assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM admission.class_cancellation_requests WHERE class_id=?",Integer.class,f[0]));
        } finally {pool.shutdownNow();}
    }
    /** Duyệt và hủy tranh chấp cùng khóa lớp; sau cùng không còn đơn APPROVED. */
    @Test void cancellationRacingApprovalCannotResurrectEnrollment() throws Exception {
        int[] f=cancellationFixture(); ExecutorService pool=Executors.newFixedThreadPool(2);
        try {
            Future<?> a=pool.submit(()->capacity.cancelClass(f[0],"Hủy"));
            Future<?> b=pool.submit(()->{try{service.approveEnrollment(f[1]);}catch(BusinessException expected){}});
            a.get(15,TimeUnit.SECONDS);b.get(15,TimeUnit.SECONDS);
            assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM admission.enrollments WHERE class_id=? AND status='APPROVED'",Integer.class,f[0]));
            int refunds=jdbc.queryForObject("SELECT COUNT(*) FROM admission.enrollments WHERE class_id=? AND status='CANCELLED_REFUND_PENDING'",Integer.class,f[0]);
            assertEquals(refunds,jdbc.queryForObject("SELECT COUNT(*) FROM admission.class_cancellation_requests WHERE class_id=?",Integer.class,f[0]));
        } finally {pool.shutdownNow();}
    }
    /** Không hủy lớp đã khai giảng hoặc đã đủ ngưỡng tối thiểu. */
    @Test void cancellationRejectsStartedAndSufficientClasses() {
        int[] f=cancellationFixture(); jdbc.update("UPDATE admission.classes SET start_date=CURRENT_DATE WHERE id=?",f[0]);
        assertThrows(BusinessException.class,()->capacity.cancelClass(f[0],"Không hợp lệ"));
        jdbc.update("UPDATE admission.classes SET start_date=CURRENT_DATE+3,min_students=1 WHERE id=?",f[0]);
        assertThrows(BusinessException.class,()->capacity.cancelClass(f[0],"Đủ sĩ số"));
    }
    @SpyBean ClassService capacity;
    @MockBean FinanceServiceClient finance;
    @SpyBean StudentServiceSyncHelper sync;
    @MockBean com.englishcenter.admission.client.StudentServiceClient studentClient;
    @SpyBean com.englishcenter.admission.repository.StudentSyncOutbox studentOutbox;

    /** Client JDK không dùng chế độ streaming retry của HttpURLConnection khi test response 401. */
    @BeforeEach void configureHttpClient() {
        http.getRestTemplate().setRequestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory());
        // Mỗi test là một nguồn truy cập riêng; audit cũ vẫn được giữ, không vô hiệu hóa brute-force guard.
        String testIp = "198.18." + ThreadLocalRandom.current().nextInt(256) + "." + ThreadLocalRandom.current().nextInt(1, 255);
        http.getRestTemplate().setInterceptors(List.of((request, body, execution) -> {
            request.getHeaders().set("X-Forwarded-For", testIp);
            return execution.execute(request, body);
        }));
    }

    /** Tái hiện thiếu endpoint đăng ký khách trước khi triển khai luồng email. */
    @Test void guestRegistrationEndpointIsAvailable() {
        var response = http.postForEntity("/api/admission/auth/register",
                Map.of("email", "qa-" + UUID.randomUUID() + "@example.test", "full_name", "QA Guest"), JsonNode.class);
        assertEquals(200, response.getStatusCode().value(), String.valueOf(response.getBody()));
    }

    /** URL bắt buộc trỏ DB admission_qa_v1 riêng để tránh chạm dữ liệu thật. */
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("admission.mail.enabled", () -> "true");
        r.add("spring.mail.host", () -> "127.0.0.1");
        r.add("spring.mail.port", mailbox::port);
        r.add("spring.mail.properties.mail.smtp.starttls.enable", () -> "false");
        r.add("spring.mail.properties.mail.smtp.starttls.required", () -> "false");
        String url = System.getenv("ADMISSION_QA_JDBC_URL");
        if (url == null || !"/admission_qa_v1".equals(java.net.URI.create(url.substring(5)).getPath())) {
            throw new IllegalStateException("Use admission_qa_v1 database only");
        }
        r.add("spring.datasource.url", () -> url);
        r.add("spring.datasource.username", () -> "postgres");
        r.add("spring.datasource.password", () -> System.getenv("ADMISSION_QA_DB_PASSWORD"));
    }

    /** Hai transaction cùng đăng ký slot cuối: chỉ một thành công, bên còn lại nhận ADM_CLASS_FULL. */
    @Test void concurrentRegistrationCannotOversellLastSlot() throws Exception {
        String key = "qa-" + UUID.randomUUID();
        Integer course = jdbc.queryForObject("INSERT INTO admission.courses(code,name,level,duration_weeks) VALUES (?,?,'BEGINNER',1) RETURNING id", Integer.class, key, key);
        Integer clazz = jdbc.queryForObject("INSERT INTO admission.classes(course_id,code,name,day_pattern,start_time,end_time,start_date,end_date,min_students,max_students) VALUES (?,?,?,'DAILY','08:00','09:00',CURRENT_DATE,CURRENT_DATE+7,1,1) RETURNING id", Integer.class, course, key, key);
        CountDownLatch firstRead = new CountDownLatch(1), releaseFirst = new CountDownLatch(1);
        AtomicInteger reads = new AtomicInteger();
        doAnswer(inv -> {
            Object result = inv.callRealMethod();
            if (reads.incrementAndGet() == 1) {
                firstRead.countDown();
                if (!releaseFirst.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("QA synchronization timeout");
            }
            return result;
        }).when(capacity).getClassCapacity(anyInt());
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<String> first = pool.submit(() -> register(clazz, key + "-a@example.test"));
            assertTrue(firstRead.await(10, TimeUnit.SECONDS));
            Future<String> second = pool.submit(() -> register(clazz, key + "-b@example.test"));
            boolean completedBeforeCommit;
            try { second.get(700, TimeUnit.MILLISECONDS); completedBeforeCommit = true; }
            catch (TimeoutException expected) { completedBeforeCommit = false; }
            finally { releaseFirst.countDown(); }
            String a = first.get(15, TimeUnit.SECONDS), b = second.get(15, TimeUnit.SECONDS);
            assertFalse(completedBeforeCommit, "Second registration must wait for the class lock");
            assertEquals("OK", a);
            assertEquals("ADM_CLASS_FULL", b);
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM admission.enrollments WHERE class_id=? AND status='PENDING'", Integer.class, clazz));
        } finally { releaseFirst.countDown(); pool.shutdownNow(); }
    }

    /** Chạy service Spring thật trong transaction riêng, giữ lại mã lỗi để kiểm tra nghiệp vụ. */
    private String register(Integer clazz, String email) {
        try { service.createEnrollment(new CreateEnrollmentRequest(clazz, "QA", email, null, null)); return "OK"; }
        catch (BusinessException ex) { return ex.getErrorCode().name(); }
    }

    /** DB thật xác nhận rollback khi duyệt tay và commit đối soát khi webhook đến muộn. */
    @Test void overduePendingCannotBeApprovedAndLateWebhookIsReconciled() {
        String key = "qa-" + UUID.randomUUID();
        Integer course = jdbc.queryForObject("INSERT INTO admission.courses(code,name,level,duration_weeks) VALUES (?,?,'BEGINNER',1) RETURNING id", Integer.class, key, key);
        Integer clazz = jdbc.queryForObject("INSERT INTO admission.classes(course_id,code,name,day_pattern,start_time,end_time,start_date,end_date,min_students,max_students) VALUES (?,?,?,'DAILY','08:00','09:00',CURRENT_DATE,CURRENT_DATE+7,1,1) RETURNING id", Integer.class, course, key, key);
        Integer student = jdbc.queryForObject("INSERT INTO admission.users(email,password_hash,full_name,role) VALUES (?,'unused','QA','STUDENT') RETURNING id", Integer.class, key + "@example.test");
        Integer enrollment = jdbc.queryForObject("INSERT INTO admission.enrollments(student_id,class_id,hold_expires_at) VALUES (?,?,CURRENT_TIMESTAMP-INTERVAL '1 hour') RETURNING id", Integer.class, student, clazz);
        assertThrows(BusinessException.class, () -> service.approveEnrollment(enrollment));
        assertEquals("PENDING", jdbc.queryForObject("SELECT status FROM admission.enrollments WHERE id=?", String.class, enrollment));
        var request = new com.englishcenter.admission.dto.webhook.PaymentSuccessWebhookRequest(key, enrollment, null, null, null, null);
        assertEquals("EXPIRED", service.processPaymentSuccess(request).getStatus());
        assertEquals("EXPIRED", jdbc.queryForObject("SELECT status FROM admission.enrollments WHERE id=?", String.class, enrollment));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM admission.processed_finance_events WHERE finance_event_id=?", Integer.class, key));
        service.processPaymentSuccess(request);
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM admission.processed_finance_events WHERE finance_event_id=?", Integer.class, key));
    }

    /** HTTP thật + PostgreSQL thật: token hợp lệ lưu được JSONB, thiếu token và nộp lặp bị chặn. */
    @Test void guestPlacementRoundTripUsesCapabilityAndPersistsResult() {
        String key = "qa-" + UUID.randomUUID();
        Integer set = jdbc.queryForObject("INSERT INTO admission.test_sets(code,title,duration_minutes) VALUES (?,?,10) RETURNING id", Integer.class, key, key);
        Integer question = jdbc.queryForObject("INSERT INTO admission.test_questions(test_set_id,skill_type,question_text,option_a,option_b,option_c,option_d,correct_option) VALUES (?,'GRAMMAR','QA','A','B','C','D','A') RETURNING id", Integer.class, set);
        String email = key + "@example.test";
        String access = registerAndLogin(email);
        var headers = new org.springframework.http.HttpHeaders(); headers.setBearerAuth(access);
        var start = http.postForEntity("/api/admission/tests/public/" + set + "/start",
                new org.springframework.http.HttpEntity<>(Map.of("email", email, "full_name", "QA Guest"), headers), JsonNode.class);
        assertEquals(200, start.getStatusCode().value(), String.valueOf(start.getBody()));
        JsonNode data = start.getBody().get("data");
        int attempt = data.get("attempt_id").asInt();
        String token = data.get("attempt_token").asText();
        assertFalse(data.get("questions").get(0).has("correct_option"));
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("attempt_id", attempt);
        body.put("answers", java.util.List.of(Map.of("question_id", question, "selected_option", "A")));
        assertEquals(403, http.postForEntity("/api/admission/tests/public/submit", body, JsonNode.class).getStatusCode().value());
        body.put("attempt_token", token);
        var submitted = http.postForEntity("/api/admission/tests/public/submit", body, JsonNode.class);
        assertEquals(200, submitted.getStatusCode().value(), String.valueOf(submitted.getBody()));
        assertEquals(1, submitted.getBody().get("data").get("total_score").asInt());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM admission.test_submissions WHERE attempt_id=? AND jsonb_array_length(answers_detail)=1", Integer.class, attempt));
        var repeated = http.postForEntity("/api/admission/tests/public/submit", body, JsonNode.class);
        assertEquals(400, repeated.getStatusCode().value());
        assertEquals("ADM_TEST_ALREADY_SUBMITTED", repeated.getBody().get("error").get("code").asText());
        assertEquals(403, http.postForEntity("/api/admission/tests/public/" + set + "/start",
                Map.of("email", key + "@example.test", "full_name", "Impersonator"), JsonNode.class).getStatusCode().value());
    }

    /** Đăng ký -> SMTP -> xác minh -> setup -> login, dùng HTTP thật và DB thật. */
    private String registerAndLogin(String email) {
        assertEquals(200, http.postForEntity("/api/admission/auth/register", Map.of("email", email, "full_name", "QA"), JsonNode.class).getStatusCode().value());
        String verify = mailbox.token(email, "verify");
        var verified = http.postForEntity("/api/admission/auth/verify-email", Map.of("token", verify), JsonNode.class);
        assertEquals(200, verified.getStatusCode().value());
        String setup = verified.getBody().get("data").get("setup_token").asText();
        assertEquals(200, http.postForEntity("/api/admission/auth/setup-password", Map.of("token", setup, "new_password", "QaSecure-Password-123"), JsonNode.class).getStatusCode().value());
        var login = http.postForEntity("/api/admission/auth/login", Map.of("email", email, "password", "QaSecure-Password-123"), JsonNode.class);
        assertEquals(200, login.getStatusCode().value()); return login.getBody().get("data").get("access_token").asText();
    }

    /** Token không lưu gốc, phân mục đích, không dùng lại; reset thu hồi refresh cũ. */
    @Test @org.junit.jupiter.api.extension.ExtendWith(org.springframework.boot.test.system.OutputCaptureExtension.class)
    void emailSetupAndPasswordRecoveryAreSingleUse(org.springframework.boot.test.system.CapturedOutput output) {
        String email = "qa-" + UUID.randomUUID() + "@example.test";
        var registration = Map.of("email", email, "full_name", "QA", "role", "ADMIN");
        assertEquals(200, http.postForEntity("/api/admission/auth/register", registration, JsonNode.class).getStatusCode().value());
        assertEquals("STUDENT", jdbc.queryForObject("SELECT role FROM admission.users WHERE email=?", String.class, email));
        String verify = mailbox.token(email, "verify");
        String stored = jdbc.queryForObject("SELECT token FROM admission.password_reset_tokens WHERE user_id=(SELECT id FROM admission.users WHERE email=?)", String.class, email);
        assertNotEquals(verify, stored); assertTrue(stored.startsWith("VERIFY:"));
        http.postForEntity("/api/admission/auth/register", registration, JsonNode.class);
        assertEquals(1, mailbox.count(email));
        assertEquals(401, http.postForEntity("/api/admission/auth/reset-password", Map.of("token", verify, "new_password", "QaSecure-Password-123"), JsonNode.class).getStatusCode().value());
        var verified = http.postForEntity("/api/admission/auth/verify-email", Map.of("token", verify), JsonNode.class);
        assertEquals(200, verified.getStatusCode().value());
        String setup = verified.getBody().get("data").get("setup_token").asText();
        assertEquals(401, http.postForEntity("/api/admission/auth/verify-email", Map.of("token", verify), JsonNode.class).getStatusCode().value());
        assertEquals(400, http.postForEntity("/api/admission/auth/setup-password", Map.of("token", setup, "new_password", "short"), JsonNode.class).getStatusCode().value());
        assertEquals(400, http.postForEntity("/api/admission/auth/setup-password", Map.of("token", setup, "new_password", "ấ".repeat(30)), JsonNode.class).getStatusCode().value());
        assertEquals(200, http.postForEntity("/api/admission/auth/setup-password", Map.of("token", setup, "new_password", "QaSecure-Password-123"), JsonNode.class).getStatusCode().value());
        assertEquals(401, http.postForEntity("/api/admission/auth/setup-password", Map.of("token", setup, "new_password", "Different-password-123"), JsonNode.class).getStatusCode().value());
        var login = http.postForEntity("/api/admission/auth/login", Map.of("email", email, "password", "QaSecure-Password-123"), JsonNode.class);
        assertEquals(200, login.getStatusCode().value());
        String refreshToken = login.getBody().get("data").get("refresh_token").asText();
        jdbc.update("UPDATE admission.password_reset_tokens SET created_at=CURRENT_TIMESTAMP-INTERVAL '2 minutes' WHERE user_id=(SELECT id FROM admission.users WHERE email=?)", email);
        assertEquals(200, http.postForEntity("/api/admission/auth/forgot-password", Map.of("email", email), JsonNode.class).getStatusCode().value());
        String reset = mailbox.token(email, "reset");
        assertEquals(200, http.postForEntity("/api/admission/auth/reset-password", Map.of("token", reset, "new_password", "New-password-456"), JsonNode.class).getStatusCode().value());
        assertEquals(401, http.postForEntity("/api/admission/auth/reset-password", Map.of("token", reset, "new_password", "Again-password-789"), JsonNode.class).getStatusCode().value());
        assertEquals(401, http.postForEntity("/api/admission/auth/refresh", Map.of("refresh_token", refreshToken), JsonNode.class).getStatusCode().value());
        assertEquals(200, http.postForEntity("/api/admission/auth/login", Map.of("email", email, "password", "New-password-456"), JsonNode.class).getStatusCode().value());
        for (String secret : List.of(verify, setup, reset, "QaSecure-Password-123", "New-password-456")) assertFalse(output.getAll().contains(secret));
    }

    /** Link hết hạn không xác minh hoặc kích hoạt tài khoản. */
    @Test void expiredEmailLinkIsRejected() {
        String email = "qa-" + UUID.randomUUID() + "@example.test";
        http.postForEntity("/api/admission/auth/register", Map.of("email", email, "full_name", "QA"), JsonNode.class);
        String token = mailbox.token(email, "verify");
        jdbc.update("UPDATE admission.password_reset_tokens SET expires_at=CURRENT_TIMESTAMP-INTERVAL '1 second' WHERE user_id=(SELECT id FROM admission.users WHERE email=?)", email);
        assertEquals(401, http.postForEntity("/api/admission/auth/verify-email", Map.of("token", token), JsonNode.class).getStatusCode().value());
        assertNull(jdbc.queryForObject("SELECT email_verified_at FROM admission.users WHERE email=?", java.sql.Timestamp.class, email));
    }

    /** Login sai phải giữ audit LOGIN_FAILED sau response 401 để brute-force guard đếm được. */
    @Test void failedLoginAuditSurvivesBusinessRejection() {
        String email = "qa-" + UUID.randomUUID() + "@example.test";
        registerAndLogin(email);
        assertEquals(401, http.postForEntity("/api/admission/auth/login", Map.of("email", email, "password", "Wrong-password-123"), JsonNode.class).getStatusCode().value());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM admission.auth_audit_logs WHERE email=? AND action='LOGIN_FAILED'", Integer.class, email));
    }

    /** Audit đã commit phải kích hoạt bảo vệ sau năm lần sai, kể cả lần sau dùng mật khẩu đúng. */
    @Test void repeatedFailedLoginsStillTriggerBruteForceProtection() {
        String email = "qa-" + UUID.randomUUID() + "@example.test";
        registerAndLogin(email);
        for (int i = 0; i < 5; i++) {
            assertEquals(401, http.postForEntity("/api/admission/auth/login",
                    Map.of("email", email, "password", "Wrong-password-123"), JsonNode.class).getStatusCode().value());
        }
        var blocked = http.postForEntity("/api/admission/auth/login",
                Map.of("email", email, "password", "QaSecure-Password-123"), JsonNode.class);
        assertEquals(403, blocked.getStatusCode().value());
        assertEquals("AUTH_ACCOUNT_LOCKED", blocked.getBody().get("error").get("code").asText());
        assertEquals(5, jdbc.queryForObject("SELECT COUNT(*) FROM admission.auth_audit_logs WHERE email=? AND action='LOGIN_FAILED'", Integer.class, email));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM admission.auth_audit_logs WHERE email=? AND action='LOGIN_BLOCKED_BRUTEFORCE'", Integer.class, email));
    }

    /** Hai request cùng link email: chỉ một request được cấp SETUP. */
    @Test void verificationIsAtomicUnderConcurrentRequests() throws Exception {
        String email = "qa-" + UUID.randomUUID() + "@example.test";
        http.postForEntity("/api/admission/auth/register", Map.of("email", email, "full_name", "QA"), JsonNode.class);
        String raw = mailbox.token(email, "verify");
        ExecutorService pool = Executors.newFixedThreadPool(2); CountDownLatch start = new CountDownLatch(1);
        try {
            Callable<Integer> verify = () -> { start.await(); return http.postForEntity("/api/admission/auth/verify-email", Map.of("token", raw), JsonNode.class).getStatusCode().value(); };
            Future<Integer> a = pool.submit(verify), b = pool.submit(verify); start.countDown();
            var statuses = new java.util.ArrayList<>(List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS)));
            statuses.sort(Integer::compareTo); assertEquals(List.of(200, 401), statuses);
        } finally { pool.shutdownNow(); }
    }

    /** Email token không có quyền mở khóa tài khoản Admin đã khóa. */
    @Test void emailLinkCannotUnlockDisabledAccount() {
        String email = "qa-" + UUID.randomUUID() + "@example.test";
        http.postForEntity("/api/admission/auth/register", Map.of("email", email, "full_name", "QA"), JsonNode.class);
        String raw = mailbox.token(email, "verify");
        jdbc.update("UPDATE admission.users SET is_active=false WHERE email=?", email);
        assertEquals(403, http.postForEntity("/api/admission/auth/verify-email", Map.of("token", raw), JsonNode.class).getStatusCode().value());
        assertEquals(false, jdbc.queryForObject("SELECT is_active FROM admission.users WHERE email=?", Boolean.class, email));
    }

    /** Học vụ dùng lookup giáo viên tối thiểu nhưng vẫn không đọc được danh sách người dùng Admin. */
    @Test void staffTeacherLookupDoesNotExposeAdminUserDirectory() {
        String email = "qa-" + UUID.randomUUID() + "@example.test";
        jdbc.update("INSERT INTO admission.users(email,password_hash,full_name,role) VALUES (?,?,?,'STAFF')", email, encoder.encode("QA-staff-password"), "QA Staff");
        var login = http.postForEntity("/api/admission/auth/login", Map.of("email", email, "password", "QA-staff-password"), JsonNode.class);
        assertEquals(200, login.getStatusCode().value());
        var headers = new org.springframework.http.HttpHeaders(); headers.setBearerAuth(login.getBody().get("data").get("access_token").asText());
        var request = new org.springframework.http.HttpEntity<>(headers);
        var teachers = http.exchange("/api/admission/users/teachers", org.springframework.http.HttpMethod.GET, request, JsonNode.class);
        assertEquals(200, teachers.getStatusCode().value());
        for (JsonNode teacher : teachers.getBody().get("data")) { assertTrue(teacher.has("full_name")); assertFalse(teacher.has("email")); assertFalse(teacher.has("password_hash")); }
        assertEquals(403, http.exchange("/api/admission/users", org.springframework.http.HttpMethod.GET, request, JsonNode.class).getStatusCode().value());
    }

    /** Access JWT cũ cũng bị chặn ngay khi Admin khóa user, không đợi token hết hạn. */
    @Test void disabledUserCannotUsePreviouslyIssuedAccessToken() {
        String email = "qa-" + UUID.randomUUID() + "@example.test";
        String access = registerAndLogin(email);
        jdbc.update("UPDATE admission.users SET is_active=false WHERE email=?", email);
        var headers = new org.springframework.http.HttpHeaders(); headers.setBearerAuth(access);
        assertEquals(401, http.exchange("/api/admission/enrollments/my", org.springframework.http.HttpMethod.GET,
                new org.springframework.http.HttpEntity<>(headers), JsonNode.class).getStatusCode().value());
    }

    /** Đổi mật khẩu cần mật khẩu hiện tại, sau đó mật khẩu cũ không còn đăng nhập được. */
    @Test void currentPasswordIsRequiredForPasswordChange() {
        String email = "qa-" + UUID.randomUUID() + "@example.test";
        String access = registerAndLogin(email);
        var headers = new org.springframework.http.HttpHeaders(); headers.setBearerAuth(access);
        assertEquals(401, http.postForEntity("/api/admission/auth/change-password",
                new org.springframework.http.HttpEntity<>(Map.of("current_password", "Wrong-password", "new_password", "Changed-password-123"), headers), JsonNode.class).getStatusCode().value());
        assertEquals(200, http.postForEntity("/api/admission/auth/change-password",
                new org.springframework.http.HttpEntity<>(Map.of("current_password", "QaSecure-Password-123", "new_password", "Changed-password-123"), headers), JsonNode.class).getStatusCode().value());
        assertEquals(401, http.postForEntity("/api/admission/auth/login", Map.of("email", email, "password", "QaSecure-Password-123"), JsonNode.class).getStatusCode().value());
        assertEquals(200, http.postForEntity("/api/admission/auth/login", Map.of("email", email, "password", "Changed-password-123"), JsonNode.class).getStatusCode().value());
    }

    @AfterAll static void closeSmtpCapture() throws java.io.IOException { mailbox.close(); }

    /** Audit tạo nhân sự phải tương thích CHECK constraint PostgreSQL, không chỉ pass với mock. */
    @Test void creatingStaffPersistsUserAndAudit() {
        var request = new com.englishcenter.admission.dto.user.CreateUserRequest();
        String email = "qa-" + UUID.randomUUID() + "@example.test";
        request.setEmail(email); request.setFullName("QA Teacher"); request.setRole("TEACHER"); request.setPassword("QA-teacher-password");
        var user = staffUsers.createUser(request);
        assertNotNull(user.getId());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM admission.auth_audit_logs WHERE email=? AND action='CREATE_USER'", Integer.class, email));
    }
}
