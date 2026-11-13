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
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.List;
import org.springframework.boot.test.mock.mockito.*;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
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
    @SpyBean com.englishcenter.admission.repository.ProcessedFinanceEventRepository callbackEvents;
    @SpyBean com.englishcenter.admission.repository.HoldExtensionAuditRepository holdAudits;
    @SpyBean RecommendationService placementRecommendations;
    @Autowired org.springframework.transaction.PlatformTransactionManager holdTxManager;

    /** Payload lấy deadline thật đang hiển thị; precondition chống gửi lặp mà không đặt giới hạn số lần gia hạn. */
    private org.springframework.http.HttpEntity<Map<String,Object>> holdRequest(int id, String role) {
        var auth = cashHttpRequest(role, "HOLD", "1");
        return new org.springframework.http.HttpEntity<>(Map.of("extended_hours", 24, "reason", "QA gia hạn hợp lệ",
                "expected_hold_expires_at", holdDeadline(id).toString()), auth.getHeaders());
    }

    /** Đọc chính xác microseconds PostgreSQL để kiểm tra deadline không tăng ngoài ý muốn. */
    private java.time.LocalDateTime holdDeadline(int id) {
        return jdbc.queryForObject("SELECT hold_expires_at FROM admission.enrollments WHERE id=?", java.time.LocalDateTime.class, id);
    }

    private String holdUrl(int id) { return "/api/admission/enrollments/" + id + "/extend-hold"; }

    /** H06: job chưa chạy không được dùng làm cơ hội khôi phục hold quá hạn. */
    @Test void holdRejectsOverdueBeforeScheduler() {
        int[] f = cashFixture();
        jdbc.update("UPDATE admission.enrollments SET hold_expires_at=CURRENT_TIMESTAMP-INTERVAL '1 second' WHERE id=?", f[1]);
        var before = holdDeadline(f[1]);
        assertEquals(400, http.postForEntity(holdUrl(f[1]), holdRequest(f[1], "STAFF"), JsonNode.class).getStatusCode().value());
        assertEquals(before, holdDeadline(f[1]));
        assertEquals("PENDING", jdbc.queryForObject("SELECT status FROM admission.enrollments WHERE id=?", String.class, f[1]));
    }

    /** Gửi lại cùng snapshot chỉ được cộng giờ một lần; lần sau phải refresh trước khi chủ ý gia hạn mới. */
    @Test void holdDuplicateCannotExtendTwice() {
        int[] f = cashFixture(); var request = holdRequest(f[1], "STAFF"); var before = holdDeadline(f[1]);
        assertEquals(200, http.postForEntity(holdUrl(f[1]), request, JsonNode.class).getStatusCode().value());
        assertEquals(409, http.postForEntity(holdUrl(f[1]), request, JsonNode.class).getStatusCode().value());
        assertEquals(before.plusHours(24), holdDeadline(f[1]));
    }

    /** Deadline NULL không chứng minh đơn còn hiệu lực; không tự tạo hạn từ NOW(). */
    @Test void holdRejectsUnknownDeadline() {
        int[] f = cashFixture(); var request = holdRequest(f[1], "STAFF");
        jdbc.update("UPDATE admission.enrollments SET hold_expires_at=NULL WHERE id=?", f[1]);
        assertEquals(400, http.postForEntity(holdUrl(f[1]), request, JsonNode.class).getStatusCode().value());
        assertNull(holdDeadline(f[1]));
    }

    /** Dữ liệu legacy PENDING ở lớp đã hủy không được kéo dài hoặc chiếm slot trở lại. */
    @Test void holdRejectsCancelledClass() {
        int[] f = cashFixture(); var request = holdRequest(f[1], "STAFF"); var before = holdDeadline(f[1]);
        jdbc.update("UPDATE admission.classes SET status='CANCELLED' WHERE id=?", f[0]);
        assertEquals(400, http.postForEntity(holdUrl(f[1]), request, JsonNode.class).getStatusCode().value());
        assertEquals(before, holdDeadline(f[1]));
    }

    /** Không chỉ dựa controller: lời gọi service không có actor phải bị từ chối, không lưu user_id NULL. */
    @Test void holdServiceRequiresStaffIdentity() {
        int[] f = cashFixture();
        var before = holdDeadline(f[1]);
        var request = new com.englishcenter.admission.dto.enrollment.ExtendHoldRequest(24, "QA lý do hợp lệ");
        request.setExpectedHoldExpiresAt(before);
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        assertEquals(ErrorCode.AUTH_UNAUTHORIZED, assertThrows(BusinessException.class, () -> service.extendHold(f[1], request)).getErrorCode());
        int student = jdbc.queryForObject("SELECT student_id FROM admission.enrollments WHERE id=?", Integer.class, f[1]);
        var principal = new com.englishcenter.admission.security.UserPrincipal(student, "qa@example.test", "unused", "QA", "STUDENT", true,
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_STUDENT")));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        try {
            assertEquals(ErrorCode.AUTH_FORBIDDEN, assertThrows(BusinessException.class, () -> service.extendHold(f[1], request)).getErrorCode());
            assertEquals(before, holdDeadline(f[1]));
        } finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
    }

    /** HTTP role thật và DTO: thiếu snapshot/giờ/lý do không được đi vào cập nhật; trạng thái đóng bị từ chối. */
    @Test void holdRolesValidationAndTerminalStates() {
        int[] f = cashFixture(); var staff = holdRequest(f[1], "STAFF");
        assertEquals(401, http.postForEntity(holdUrl(f[1]), staff.getBody(), JsonNode.class).getStatusCode().value());
        for (String role : List.of("STUDENT", "TEACHER"))
            assertEquals(403, http.postForEntity(holdUrl(f[1]), holdRequest(f[1], role), JsonNode.class).getStatusCode().value());
        for (var change : List.of(Map.of("extended_hours", 0), Map.of("extended_hours", 73), Map.of("reason", " "))) {
            var invalid = new java.util.HashMap<String,Object>(staff.getBody()); invalid.putAll(change);
            assertEquals(400, http.postForEntity(holdUrl(f[1]), new org.springframework.http.HttpEntity<>(invalid, staff.getHeaders()), JsonNode.class).getStatusCode().value());
        }
        var missing = new java.util.HashMap<>(staff.getBody()); missing.remove("expected_hold_expires_at");
        assertEquals(400, http.postForEntity(holdUrl(f[1]), new org.springframework.http.HttpEntity<>(missing, staff.getHeaders()), JsonNode.class).getStatusCode().value());
        for (String status : List.of("EXPIRED", "CANCELLED", "APPROVED", "REJECTED", "CANCELLED_REFUND_PENDING")) {
            jdbc.update("UPDATE admission.enrollments SET status=? WHERE id=?", status, f[1]);
            var before = holdDeadline(f[1]);
            assertEquals(400, http.postForEntity(holdUrl(f[1]), staff, JsonNode.class).getStatusCode().value());
            assertEquals(before, holdDeadline(f[1]));
            assertEquals(status, jdbc.queryForObject("SELECT status FROM admission.enrollments WHERE id=?", String.class, f[1]));
        }
        assertEquals(5, jdbc.queryForObject("SELECT COUNT(*) FROM admission.hold_extension_audit WHERE enrollment_id=? AND outcome='REJECTED'", Integer.class, f[1]));
    }

    /** Mỗi lần chủ ý gia hạn phải đọc snapshot mới; lưu actor, timestamp, reason, hạn cũ/mới đầy đủ. */
    @Test void holdIntentionalRepeatWithFreshSnapshotAndAudit() {
        int[] f = cashFixture(); var before = holdDeadline(f[1]); var first = holdRequest(f[1], "STAFF");
        assertEquals(200, http.postForEntity(holdUrl(f[1]), first, JsonNode.class).getStatusCode().value());
        var second = holdRequest(f[1], "ADMIN");
        assertEquals(200, http.postForEntity(holdUrl(f[1]), second, JsonNode.class).getStatusCode().value());
        assertEquals(before.plusHours(48), holdDeadline(f[1]));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM admission.hold_extension_audit WHERE enrollment_id=? AND outcome='EXTENDED' AND actor_id IS NOT NULL AND previous_deadline IS NOT NULL AND new_deadline=previous_deadline+INTERVAL '24 hours' AND created_at IS NOT NULL", Integer.class, f[1]));
        assertNotNull(jdbc.queryForObject("SELECT hold_extended_by_user_id FROM admission.enrollments WHERE id=?", Integer.class, f[1]));
        assertEquals("QA gia hạn hợp lệ", jdbc.queryForObject("SELECT hold_extension_reason FROM admission.enrollments WHERE id=?", String.class, f[1]));
        String notes = jdbc.queryForObject("SELECT notes FROM admission.enrollments WHERE id=?", String.class, f[1]);
        assertEquals(2, notes.split("Gia hạn 24h", -1).length - 1);
        assertTrue(notes.contains("lúc "));
    }

    /** Cùng deadline ở lớp vừa đầy vẫn được kéo dài slot của chính đơn; dữ liệu đã vượt trần bị chặn. */
    @Test void holdCapacityIncludesOwnSlotAndRejectsOverbookedData() {
        int[] f = cashFixture();
        jdbc.update("UPDATE admission.classes SET min_students=1,max_students=2 WHERE id=?", f[0]);
        assertEquals(200, http.postForEntity(holdUrl(f[1]), holdRequest(f[1], "STAFF"), JsonNode.class).getStatusCode().value());
        jdbc.update("UPDATE admission.classes SET max_students=1 WHERE id=?", f[0]);
        var before = holdDeadline(f[1]);
        assertEquals(400, http.postForEntity(holdUrl(f[1]), holdRequest(f[1], "STAFF"), JsonNode.class).getStatusCode().value());
        assertEquals(before, holdDeadline(f[1]));
    }

    /** Failure audit phải rollback deadline/notes và không ghi nhận thành công giả. */
    @Test void holdAuditFailureRollsBackDeadline() {
        int[] f = cashFixture(); var request = holdRequest(f[1], "STAFF"); var before = holdDeadline(f[1]);
        org.mockito.Mockito.doThrow(new org.springframework.dao.DataAccessResourceFailureException("QA hold audit"))
                .when(holdAudits).extended(org.mockito.ArgumentMatchers.eq(f[1]), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        assertEquals(500, http.postForEntity(holdUrl(f[1]), request, JsonNode.class).getStatusCode().value());
        assertEquals(before, holdDeadline(f[1]));
        assertNull(jdbc.queryForObject("SELECT hold_extended_by_user_id FROM admission.enrollments WHERE id=?", Integer.class, f[1]));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM admission.hold_extension_audit WHERE enrollment_id=?", Integer.class, f[1]));
    }

    /** Hai nhân viên dùng cùng snapshot chỉ một người thành công; từ chối được audit sau rollback. */
    @Test void holdConcurrentSnapshotsOnlyExtendOnce() throws Exception {
        int[] f = cashFixture(); var first = holdRequest(f[1], "STAFF"); var second = holdRequest(f[1], "ADMIN");
        var before = holdDeadline(f[1]); var pool = Executors.newFixedThreadPool(2); var start = new CountDownLatch(1);
        try {
            var a = pool.submit(() -> { start.await(); return http.postForEntity(holdUrl(f[1]), first, JsonNode.class).getStatusCode().value(); });
            var b = pool.submit(() -> { start.await(); return http.postForEntity(holdUrl(f[1]), second, JsonNode.class).getStatusCode().value(); });
            start.countDown(); var statuses = new java.util.ArrayList<>(List.of(a.get(15,TimeUnit.SECONDS), b.get(15,TimeUnit.SECONDS)));
            java.util.Collections.sort(statuses); assertEquals(List.of(200,409), statuses);
            assertEquals(before.plusHours(24), holdDeadline(f[1]));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM admission.hold_extension_audit WHERE enrollment_id=? AND outcome='EXTENDED'", Integer.class, f[1]));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM admission.hold_extension_audit WHERE enrollment_id=? AND reason_code='ADM_HOLD_CONFLICT'", Integer.class, f[1]));
        } finally { pool.shutdownNow(); }
    }

    /** Khóa row sau gia hạn nhưng trước commit: expiry UPDATE phải recheck deadline mới sau khi chờ. */
    @Test void holdExpirationWaitsForExtensionAndRechecksDeadline() throws Exception {
        int[] f = cashFixture(); var auth = holdRequest(f[1], "STAFF");
        jdbc.update("UPDATE admission.enrollments SET hold_expires_at=CURRENT_TIMESTAMP+INTERVAL '3 seconds' WHERE id=?", f[1]);
        var before = holdDeadline(f[1]);
        var payload = new java.util.HashMap<String,Object>(auth.getBody()); payload.put("expected_hold_expires_at", before.toString());
        var request = new org.springframework.http.HttpEntity<>(payload, auth.getHeaders());
        var locked = new CountDownLatch(1); var release = new CountDownLatch(1);
        org.mockito.Mockito.doAnswer(inv -> { inv.callRealMethod(); locked.countDown(); if (!release.await(10,TimeUnit.SECONDS)) throw new IllegalStateException("QA hold barrier"); return null; })
                .when(holdAudits).extended(org.mockito.ArgumentMatchers.eq(f[1]), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        var pool = Executors.newFixedThreadPool(2);
        try {
            var extend = pool.submit(() -> http.postForEntity(holdUrl(f[1]), request, JsonNode.class).getStatusCode().value());
            assertTrue(locked.await(5,TimeUnit.SECONDS));
            long waitMillis = java.time.Duration.between(java.time.LocalDateTime.now(), before.plusNanos(100_000_000)).toMillis();
            if (waitMillis > 0) Thread.sleep(waitMillis);
            var expire = pool.submit(() -> service.expireOverdueEnrollments());
            assertThrows(TimeoutException.class, () -> expire.get(200,TimeUnit.MILLISECONDS));
            release.countDown(); assertEquals(200, extend.get(15,TimeUnit.SECONDS)); expire.get(15,TimeUnit.SECONDS);
            assertEquals("PENDING", jdbc.queryForObject("SELECT status FROM admission.enrollments WHERE id=?", String.class, f[1]));
            assertEquals(before.plusHours(24), holdDeadline(f[1]));
        } finally { release.countDown(); pool.shutdownNow(); }
    }

    /** Expiry đã giữ row và sắp commit EXPIRED: extension phải chờ rồi từ chối, không ghi state cũ đè lại. */
    @Test void holdExpirationWinningTransactionCannotBeResurrected() throws Exception {
        int[] f = cashFixture(); var request = holdRequest(f[1], "STAFF");
        var locked = new CountDownLatch(1); var release = new CountDownLatch(1); var pool = Executors.newFixedThreadPool(2);
        try {
            var expire = pool.submit(() -> new org.springframework.transaction.support.TransactionTemplate(holdTxManager).execute(status -> {
                jdbc.update("UPDATE admission.enrollments SET hold_expires_at=CURRENT_TIMESTAMP-INTERVAL '1 second' WHERE id=?", f[1]);
                service.expireOverdueEnrollments(); locked.countDown();
                try { if (!release.await(10,TimeUnit.SECONDS)) throw new IllegalStateException("QA expiry barrier"); }
                catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException(ex); }
                return null;
            }));
            assertTrue(locked.await(5,TimeUnit.SECONDS));
            var extend = pool.submit(() -> http.postForEntity(holdUrl(f[1]), request, JsonNode.class).getStatusCode().value());
            assertThrows(TimeoutException.class, () -> extend.get(200,TimeUnit.MILLISECONDS));
            release.countDown(); expire.get(15,TimeUnit.SECONDS); assertEquals(400, extend.get(15,TimeUnit.SECONDS));
            assertEquals("EXPIRED", jdbc.queryForObject("SELECT status FROM admission.enrollments WHERE id=?", String.class, f[1]));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM admission.hold_extension_audit WHERE enrollment_id=? AND outcome='EXTENDED'", Integer.class, f[1]));
        } finally { release.countDown(); pool.shutdownNow(); }
    }

    /** Đúng mốc deadline cũng là hết hạn, thống nhất với slot count và guard gia hạn. */
    @Test void holdExpirationIncludesExactDeadlineAndIsRepeatable() {
        int[] f = cashFixture();
        jdbc.update("UPDATE admission.enrollments SET hold_expires_at=CURRENT_TIMESTAMP-INTERVAL '1 second' WHERE id=?", f[1]);
        var cutoff = holdDeadline(f[1]);
        new org.springframework.transaction.support.TransactionTemplate(holdTxManager).execute(status -> cancellationEnrollments.expireOverdueEnrollments(cutoff));
        assertEquals("EXPIRED", jdbc.queryForObject("SELECT status FROM admission.enrollments WHERE id=?", String.class, f[1]));
        Integer repeated = new org.springframework.transaction.support.TransactionTemplate(holdTxManager).execute(status -> cancellationEnrollments.expireOverdueEnrollments(cutoff));
        assertEquals(Integer.valueOf(0), repeated);
        assertEquals("APPROVED", jdbc.queryForObject("SELECT status FROM admission.enrollments WHERE id=?", String.class, f[2]));
    }

    /** Ba writer hiện có dùng class->row lock; không bị extension ghi đè trạng thái cuối. */
    @Test void holdRacesApprovalCancellationAndPaymentCallback() throws Exception {
        for (String operation : List.of("APPROVE", "CANCEL", "CANCEL_CLASS", "PAYMENT")) {
            int[] f = cashFixture(); var request = holdRequest(f[1], "STAFF"); var before = holdDeadline(f[1]);
            var raceStarted = java.time.LocalDateTime.now();
            var pool = Executors.newFixedThreadPool(2); var start = new CountDownLatch(1);
            try {
                var extend = pool.submit(() -> { start.await(); return http.postForEntity(holdUrl(f[1]), request, JsonNode.class).getStatusCode().value(); });
                var mutate = pool.submit(() -> {
                    start.await();
                    switch (operation) {
                        case "APPROVE" -> service.approveEnrollment(f[1]);
                        case "CANCEL" -> service.cancelEnrollment(f[1], "QA race");
                        case "CANCEL_CLASS" -> capacity.cancelClass(f[0], "QA race");
                        case "PAYMENT" -> service.processPaymentSuccess(paymentFixture(f[1], "EV-HOLD-" + UUID.randomUUID(), "INV-CASH-" + f[1]));
                    }
                    return null;
                });
                start.countDown(); int result = extend.get(15,TimeUnit.SECONDS); mutate.get(15,TimeUnit.SECONDS);
                assertTrue(List.of(200,400).contains(result));
                assertEquals(operation.startsWith("CANCEL") ? "CANCELLED" : "APPROVED", jdbc.queryForObject("SELECT status FROM admission.enrollments WHERE id=?", String.class, f[1]));
                if (operation.equals("CANCEL_CLASS")) {
                    // Cascade hiện có chủ động đặt deadline về thời điểm hủy, không phải giữ hạn 48h.
                    assertFalse(holdDeadline(f[1]).isBefore(raceStarted.minusNanos(1000)));
                    assertFalse(holdDeadline(f[1]).isAfter(java.time.LocalDateTime.now()));
                    assertEquals("CANCELLED_REFUND_PENDING", jdbc.queryForObject("SELECT status FROM admission.enrollments WHERE id=?", String.class, f[2]));
                } else assertEquals(result == 200 ? before.plusHours(24) : before, holdDeadline(f[1]));
                assertEquals(result == 200 ? 1 : 0, jdbc.queryForObject("SELECT COUNT(*) FROM admission.hold_extension_audit WHERE enrollment_id=? AND outcome='EXTENDED'", Integer.class, f[1]));
                assertTrue(jdbc.queryForObject("SELECT COUNT(*)<=10 FROM admission.enrollments WHERE class_id=? AND status='APPROVED'", Boolean.class, f[0]));
            } finally { pool.shutdownNow(); }
        }
    }

    /** Người mới có thể lấy slot hết hạn; yêu cầu gia hạn cũ cạnh tranh không được lấy lại slot đó. */
    @Test void holdExpiredExtensionRacingLastSlotRegistrationCannotOversell() throws Exception {
        int[] f = cashFixture();
        jdbc.update("UPDATE admission.classes SET min_students=1,max_students=2 WHERE id=?", f[0]);
        jdbc.update("UPDATE admission.enrollments SET hold_expires_at=CURRENT_TIMESTAMP-INTERVAL '1 second' WHERE id=?", f[1]);
        var request = holdRequest(f[1], "STAFF"); var before = holdDeadline(f[1]);
        var pool = Executors.newFixedThreadPool(2); var start = new CountDownLatch(1);
        try {
            var extend = pool.submit(() -> { start.await(); return http.postForEntity(holdUrl(f[1]), request, JsonNode.class).getStatusCode().value(); });
            var register = pool.submit(() -> { start.await(); return service.createEnrollment(new CreateEnrollmentRequest(f[0], "QA Last Slot", "qa-hold-" + UUID.randomUUID() + "@example.test", null, null)); });
            start.countDown(); assertEquals(400, extend.get(15,TimeUnit.SECONDS)); assertEquals("PENDING", register.get(15,TimeUnit.SECONDS).getStatus());
            assertEquals(before, holdDeadline(f[1]));
            assertEquals(2L, cancellationEnrollments.countOccupiedSlots(f[0], java.time.LocalDateTime.now()));
        } finally { pool.shutdownNow(); }
    }

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
        var event=Map.of("event_id","EVT-CASH-"+f[1],"enrollment_id",f[1],"invoice_code","INV-CASH-"+f[1],"amount_paid",5000000,"paid_at",java.time.LocalDateTime.now().toString());
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
        var callback=new org.springframework.http.HttpEntity<>(Map.of("event_id","EVT-CASH-RACE-"+f[1],"enrollment_id",f[1],"invoice_code","INV-CASH-"+f[1],"amount_paid",5000000,"paid_at",java.time.LocalDateTime.now().toString()),headers);
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

    /** Callback khác invoice, số tiền không hợp lệ hoặc event bị đổi binding không được duyệt. */
    private com.englishcenter.admission.dto.webhook.PaymentSuccessWebhookRequest paymentFixture(int id,String event,String invoice){return new com.englishcenter.admission.dto.webhook.PaymentSuccessWebhookRequest(event,id,invoice,new java.math.BigDecimal("5000000"),java.time.LocalDateTime.now(),"TX-"+event);}
    @Test void callbackWrongInvoiceMustNotApprove(){int[] f=cashFixture();var request=paymentFixture(f[1],"QA-EVT-"+UUID.randomUUID(),"INV-WRONG");assertThrows(BusinessException.class,()->service.processPaymentSuccess(request));assertEquals("PENDING",jdbc.queryForObject("SELECT status FROM admission.enrollments WHERE id=?",String.class,f[1]));}
    @Test void callbackNegativeAmountMustNotApprove(){int[] f=cashFixture();var request=paymentFixture(f[1],"QA-EVT-"+UUID.randomUUID(),"INV-CASH-"+f[1]);request.setAmountPaid(new java.math.BigDecimal("-1"));assertThrows(BusinessException.class,()->service.processPaymentSuccess(request));}
    @Test void callbackEventCannotMoveToAnotherEnrollment(){int[] a=cashFixture(),b=cashFixture();String event="QA-EVT-"+UUID.randomUUID();service.processPaymentSuccess(paymentFixture(a[1],event,"INV-CASH-"+a[1]));assertThrows(BusinessException.class,()->service.processPaymentSuccess(paymentFixture(b[1],event,"INV-CASH-"+b[1])));assertEquals("PENDING",jdbc.queryForObject("SELECT status FROM admission.enrollments WHERE id=?",String.class,b[1]));}

    /** Replay cùng evidence idempotent, đổi amount hoặc invoice bị từ chối và audit vẫn commit. */
    @Test void callbackChangedReplayAndRejectionAudit(){int[] f=cashFixture();var request=paymentFixture(f[1],"EV-"+UUID.randomUUID(),"INV-CASH-"+f[1]);service.processPaymentSuccess(request);service.processPaymentSuccess(request);request.setAmountPaid(java.math.BigDecimal.ONE);assertThrows(BusinessException.class,()->service.processPaymentSuccess(request));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM admission.processed_finance_events WHERE finance_event_id=?",Integer.class,request.getEventId()));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM admission.finance_callback_audit WHERE event_id=?",Integer.class,request.getEventId()));}
    @Test void callbackExplicitPartialStatusAndOldPaidAtAreRejected(){int[] f=cashFixture();var request=paymentFixture(f[1],"EV-"+UUID.randomUUID(),"INV-CASH-"+f[1]);request.setInvoiceStatus("PARTIAL");assertThrows(BusinessException.class,()->service.processPaymentSuccess(request));request.setInvoiceStatus("PAID");request.setPaidAt(java.time.LocalDateTime.now().minusDays(1));assertThrows(BusinessException.class,()->service.processPaymentSuccess(request));assertEquals("PENDING",jdbc.queryForObject("SELECT status FROM admission.enrollments WHERE id=?",String.class,f[1]));}
    @Test void callbackTerminalStatesAreJournaledWithoutResurrection(){for(String status:List.of("REJECTED","CANCELLED_REFUND_PENDING","CANCELLED","EXPIRED")){int[] f=cashFixture();jdbc.update("UPDATE admission.enrollments SET status=? WHERE id=?",status,f[1]);var request=paymentFixture(f[1],"EV-"+UUID.randomUUID(),"INV-CASH-"+f[1]);assertEquals(status,service.processPaymentSuccess(request).getStatus());service.processPaymentSuccess(request);assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM admission.processed_finance_events WHERE finance_event_id=?",Integer.class,request.getEventId()));}}
    @Test void callbackConcurrentDuplicateCreatesOneReceipt() throws Exception {int[] f=cashFixture();var request=paymentFixture(f[1],"EV-"+UUID.randomUUID(),"INV-CASH-"+f[1]);var pool=Executors.newFixedThreadPool(2);try{var a=pool.submit(()->service.processPaymentSuccess(request));var b=pool.submit(()->service.processPaymentSuccess(request));assertEquals("APPROVED",a.get(15,TimeUnit.SECONDS).getStatus());assertEquals("APPROVED",b.get(15,TimeUnit.SECONDS).getStatus());assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM admission.processed_finance_events WHERE finance_event_id=?",Integer.class,request.getEventId()));}finally{pool.shutdownNow();}}
    @Test void callbackEnrollmentIdentityIsStrict(){for(Object id:List.of(1.5,-1,"1foo","ENR-nope-1","0")){var r=paymentFixture(1,"EV","INV");r.setEnrollmentId(id);assertThrows(IllegalArgumentException.class,r::parseNumericEnrollmentId);}}

    /** Không tái dùng enrollment_id/invoice khi Finance chỉ có idempotency theo enrollment_id. */
    @Test void callbackCycleCannotReuseClosedEnrollment(){int[] f=cashFixture();jdbc.update("UPDATE admission.enrollments SET status='EXPIRED' WHERE id=?",f[1]);String email=jdbc.queryForObject("SELECT u.email FROM admission.users u JOIN admission.enrollments e ON e.student_id=u.id WHERE e.id=?",String.class,f[1]);assertThrows(BusinessException.class,()->service.createEnrollment(new CreateEnrollmentRequest(f[0],"QA",email,null,null)));assertEquals("EXPIRED",jdbc.queryForObject("SELECT status FROM admission.enrollments WHERE id=?",String.class,f[1]));assertEquals("INV-CASH-"+f[1],jdbc.queryForObject("SELECT invoice_code FROM admission.enrollments WHERE id=?",String.class,f[1]));}

    /** Lỗi lưu receipt phải rollback cả duyệt và Student outbox; request sau được thử lại an toàn. */
    @Test void callbackReceiptFailureRollsBackApprovalAndOutbox() {
        int[] f = cashFixture();
        var request = paymentFixture(f[1], "EV-" + UUID.randomUUID(), "INV-CASH-" + f[1]);
        org.mockito.Mockito.doThrow(new org.springframework.dao.DataAccessResourceFailureException("QA receipt unavailable"))
                .when(callbackEvents).save(org.mockito.ArgumentMatchers.any(com.englishcenter.admission.entity.ProcessedFinanceEvent.class));
        assertThrows(org.springframework.dao.DataAccessResourceFailureException.class, () -> service.processPaymentSuccess(request));
        assertEquals("PENDING", jdbc.queryForObject("SELECT status FROM admission.enrollments WHERE id=?", String.class, f[1]));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM admission.processed_finance_events WHERE finance_event_id=?", Integer.class, request.getEventId()));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM admission.student_sync_outbox WHERE enrollment_id=?", Integer.class, f[1]));
        org.mockito.Mockito.reset(callbackEvents);
        assertEquals("APPROVED", service.processPaymentSuccess(request).getStatus());
    }

    /** Hai lớp khác nhau cạnh tranh cùng event chỉ có một binding được commit, bên thua có audit. */
    @Test void callbackConcurrentEventCannotMoveAcrossClasses() throws Exception {
        int[] a = cashFixture(), b = cashFixture();
        String event = "EV-" + UUID.randomUUID();
        var pool = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            List<Future<String>> futures = new java.util.ArrayList<>();
            for (int[] f : List.of(a, b)) futures.add(pool.submit(() -> {
                start.await();
                try { return service.processPaymentSuccess(paymentFixture(f[1], event, "INV-CASH-" + f[1])).getStatus(); }
                catch (BusinessException ex) { return "REJECTED"; }
            }));
            start.countDown();
            var outcomes = new java.util.ArrayList<String>();
            for (var future : futures) outcomes.add(future.get(15, TimeUnit.SECONDS));
            java.util.Collections.sort(outcomes);
            assertEquals(List.of("APPROVED", "REJECTED"), outcomes);
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM admission.processed_finance_events WHERE finance_event_id=?", Integer.class, event));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM admission.finance_callback_audit WHERE event_id=?", Integer.class, event));
        } finally { pool.shutdownNow(); }
    }

    /** Callback cũ tới sau event mới hoặc receipt legacy không được thay đổi quyết định đã lưu. */
    @Test void callbackOutOfOrderAndLegacyReceiptNeedReconciliation() {
        int[] f = cashFixture();
        var first = paymentFixture(f[1], "EV-" + UUID.randomUUID(), "INV-CASH-" + f[1]);
        var older = paymentFixture(f[1], "EV-" + UUID.randomUUID(), "INV-CASH-" + f[1]);
        older.setPaidAt(first.getPaidAt().minusNanos(1000));
        service.processPaymentSuccess(first);
        java.time.LocalDateTime approvedAt = jdbc.queryForObject("SELECT approved_at FROM admission.enrollments WHERE id=?", java.time.LocalDateTime.class, f[1]);
        service.processPaymentSuccess(older);
        assertEquals(approvedAt, jdbc.queryForObject("SELECT approved_at FROM admission.enrollments WHERE id=?", java.time.LocalDateTime.class, f[1]));
        assertEquals("ONLINE_PAYMENT", jdbc.queryForObject("SELECT approval_method FROM admission.enrollments WHERE id=?", String.class, f[1]));
        jdbc.update("UPDATE admission.processed_finance_events SET payload_hash=NULL WHERE finance_event_id=?", first.getEventId());
        assertThrows(BusinessException.class, () -> service.processPaymentSuccess(first));
        assertEquals("APPROVED", jdbc.queryForObject("SELECT status FROM admission.enrollments WHERE id=?", String.class, f[1]));
    }

    /** HTTP thật: secret sai bị chặn; thiếu/zero/future evidence và trạng thái khác PAID trả 400, không duyệt. */
    @Test void callbackHttpEvidenceAndAuthentication() {
        int[] f = cashFixture();
        String event = "EV-" + UUID.randomUUID();
        var payload = new java.util.HashMap<String, Object>(Map.of("event_id", event, "enrollment_id", f[1],
                "invoice_id", "INV-CASH-" + f[1], "amount_paid", 5000000, "paid_at", java.time.LocalDateTime.now().toString()));
        String url = "/api/admission/webhooks/payment-success";
        assertEquals(403, http.postForEntity(url, payload, JsonNode.class).getStatusCode().value());
        var headers = new org.springframework.http.HttpHeaders();
        headers.set("X-Internal-Secret", "wrong");
        assertEquals(403, http.postForEntity(url, new org.springframework.http.HttpEntity<>(payload, headers), JsonNode.class).getStatusCode().value());
        headers.set("X-Internal-Secret", qaInternalSecret);
        for (String missing : List.of("invoice_id", "amount_paid", "paid_at")) {
            var invalid = new java.util.HashMap<>(payload); invalid.remove(missing);
            assertEquals(400, http.postForEntity(url, new org.springframework.http.HttpEntity<>(invalid, headers), JsonNode.class).getStatusCode().value());
        }
        for (var change : List.of(Map.of("amount_paid", 0), Map.of("paid_at", java.time.LocalDateTime.now().plusDays(1).toString()), Map.of("status", "PARTIAL"))) {
            var invalid = new java.util.HashMap<String, Object>(payload); invalid.putAll(change);
            assertEquals(400, http.postForEntity(url, new org.springframework.http.HttpEntity<>(invalid, headers), JsonNode.class).getStatusCode().value());
        }
        assertEquals("PENDING", jdbc.queryForObject("SELECT status FROM admission.enrollments WHERE id=?", String.class, f[1]));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM admission.processed_finance_events WHERE finance_event_id=?", Integer.class, event));
        assertEquals(6, jdbc.queryForObject("SELECT COUNT(*) FROM admission.finance_callback_audit WHERE event_id=?", Integer.class, event));
        assertEquals(200, http.postForEntity(url, new org.springframework.http.HttpEntity<>(payload, headers), JsonNode.class).getStatusCode().value());
    }

    /** Nhiều callback bị từ chối không chiếm hết pool trong lúc chờ connection ghi audit. */
    @Test void callbackConcurrentRejectionsKeepAuditAndReleaseConnections() throws Exception {
        int[] f = cashFixture();
        String event = "EV-" + UUID.randomUUID();
        var request = paymentFixture(f[1], event, "INV-WRONG");
        var pool = Executors.newFixedThreadPool(12);
        var start = new CountDownLatch(1);
        try {
            var futures = new java.util.ArrayList<Future<?>>();
            for (int i = 0; i < 12; i++) futures.add(pool.submit(() -> {
                start.await();
                assertThrows(BusinessException.class, () -> service.processPaymentSuccess(request));
                return null;
            }));
            start.countDown();
            for (var future : futures) future.get(15, TimeUnit.SECONDS);
            assertEquals(12, jdbc.queryForObject("SELECT COUNT(*) FROM admission.finance_callback_audit WHERE event_id=?", Integer.class, event));
            assertEquals("PENDING", jdbc.queryForObject("SELECT status FROM admission.enrollments WHERE id=?", String.class, f[1]));
        } finally { pool.shutdownNow(); }
    }

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
        jdbc.update("UPDATE admission.enrollments SET invoice_code=? WHERE id=?","INV-"+key,enrollment);
        var request = paymentFixture(enrollment,key,"INV-"+key);
        assertEquals("EXPIRED", service.processPaymentSuccess(request).getStatus());
        assertEquals("EXPIRED", jdbc.queryForObject("SELECT status FROM admission.enrollments WHERE id=?", String.class, enrollment));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM admission.processed_finance_events WHERE finance_event_id=?", Integer.class, key));
        service.processPaymentSuccess(request);
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM admission.processed_finance_events WHERE finance_event_id=?", Integer.class, key));
    }

    @Autowired com.englishcenter.admission.security.JwtTokenProvider exportJwt;

    /** Roster xuất đúng FK Admission, đúng lớp và giữ nguyên bản ghi enrollment. */
    @Test void exportRosterUsesAdmissionUserIdsAndDoesNotWrite() {
        int[] target = cancellationFixture();
        int[] other = cancellationFixture();
        int studentId = exportEnrollmentUser(target[1]);
        int approvedStudentId = exportEnrollmentUser(target[2]);
        int otherStudentId = exportEnrollmentUser(other[1]);
        jdbc.update("UPDATE admission.users SET full_name=? WHERE id=?", "Nguyễn, \"Ánh\"\nHòa", studentId);
        jdbc.update("UPDATE admission.users SET full_name=? WHERE id=?", "=1+1", approvedStudentId);
        jdbc.update("UPDATE admission.users SET full_name=? WHERE id=?", "QA OUTSIDE CLASS", otherStudentId);
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM admission.student_sync_outbox WHERE user_id IN (?,?)", Integer.class, studentId, approvedStudentId));
        String snapshots = "SELECT to_jsonb(e)::text FROM admission.enrollments e WHERE class_id=? ORDER BY id";
        var before = jdbc.queryForList(snapshots, String.class, target[0]);

        String csv = assertCsv(exportGet(rosterExportPath(target[0]), exportToken("STAFF"), byte[].class),
                "class_" + target[0] + "_roster.csv");

        assertTrue(csv.startsWith("\uFEFFSTT,Mã đơn,ID tài khoản Admission,Họ và tên,Email,Số điện thoại,"
                + "Trạng thái đơn,Kênh đăng ký,Hạn giữ chỗ,Thời gian đăng ký\r\n"));
        assertTrue(csv.contains("ENR-" + target[1] + "," + studentId + ",\"Nguyễn, \"\"Ánh\"\"\nHòa\","));
        assertTrue(csv.contains("ENR-" + target[2] + "," + approvedStudentId + ",'=1+1,"));
        assertTrue(csv.contains(",,PENDING,"));
        assertTrue(csv.contains(",,APPROVED,"));
        assertFalse(csv.contains("STU-"));
        assertFalse(csv.contains("QA OUTSIDE CLASS"));
        assertFalse(csv.contains("ENR-" + other[1] + ","));
        assertFalse(csv.contains("unused"));
        assertEquals(before, jdbc.queryForList(snapshots, String.class, target[0]));
    }

    /** Bảng điểm đọc snapshot và điểm của bộ đề yêu cầu, bỏ qua đáp án chi tiết và bộ đề khác. */
    @Test void exportResultsUsesSubmissionSnapshotsAndTestSetScope() {
        int set = exportTestSet();
        int otherSet = exportTestSet();
        int candidate = exportUser("STUDENT");
        int submission = jdbc.queryForObject("""
                INSERT INTO admission.test_submissions(user_id,test_set_id,full_name,email,
                    listening_score,reading_score,grammar_score,total_score,assessed_level,answers_detail)
                VALUES (?,?,'Thí sinh QA','snapshot@example.test',1.25,2.00,3.75,7.00,'QA','{"private_answer":"QA-SECRET"}')
                RETURNING id
                """, Integer.class, candidate, set);
        jdbc.update("INSERT INTO admission.test_submissions(user_id,test_set_id,full_name,total_score) VALUES (?,?,'QA OUTSIDE SET',0)",
                candidate, otherSet);

        String csv = assertCsv(exportGet(resultsExportPath(set), exportToken("ADMIN"), byte[].class),
                "test_set_" + set + "_results.csv");

        assertTrue(csv.startsWith("\uFEFFSTT,Mã bài nộp,Mã thí sinh,Họ và tên,Email,Số điện thoại,"
                + "Điểm Nghe,Điểm Đọc,Điểm Ngữ pháp,Tổng điểm,Xếp loại,Khóa học đề xuất,Thời gian nộp bài\r\n"));
        assertTrue(csv.contains("SUB-" + submission + ",USR-" + candidate + ",Thí sinh QA,snapshot@example.test,,1.25,2.00,3.75,7.00,QA,"));
        assertFalse(csv.contains("QA OUTSIDE SET"));
        assertFalse(csv.contains("QA-SECRET"));
        assertEquals(200, exportGet(resultsExportPath(set), exportToken("STAFF"), byte[].class).getStatusCode().value());
    }

    /** Kiểm tra ma trận BA và bộ lọc JWT trên cả hai đường dẫn export. */
    @Test void exportRequiresActiveAdminOrStaffForBothEndpoints() {
        int[] fixture = cancellationFixture();
        int assignedTeacher = exportUser("TEACHER");
        jdbc.update("UPDATE admission.classes SET teacher_id=? WHERE id=?", assignedTeacher, fixture[0]);
        String roster = rosterExportPath(fixture[0]);
        String results = resultsExportPath(exportTestSet());
        int staffId = exportUser("STAFF");
        String staff = exportJwtFor(staffId, "STAFF");
        var expiredSigner = new com.englishcenter.admission.security.JwtTokenProvider();
        ReflectionTestUtils.setField(expiredSigner, "jwtSecret", ReflectionTestUtils.getField(exportJwt, "jwtSecret"));
        ReflectionTestUtils.setField(expiredSigner, "accessTokenExpirationSeconds", -60L);
        String expired = expiredSigner.generateAccessToken(staffId, "qa-export@example.test", "STAFF", "QA Export");
        int disabledId = exportUser("ADMIN");
        String disabled = exportJwtFor(disabledId, "ADMIN");
        jdbc.update("UPDATE admission.users SET is_active=FALSE WHERE id=?", disabledId);
        String missing = exportJwtFor(Integer.MAX_VALUE, "ADMIN");
        String badSignature = staff.substring(0, staff.lastIndexOf('.') + 1) + "wrong-signature";
        int enrollmentOwner = exportEnrollmentUser(fixture[1]);
        jdbc.update("UPDATE admission.users SET email_verified_at=CURRENT_TIMESTAMP, must_change_password=FALSE WHERE id=?", enrollmentOwner);
        var forbidden = List.of(exportJwtFor(assignedTeacher, "TEACHER"), exportToken("TEACHER"),
                exportToken("STUDENT"), exportJwtFor(enrollmentOwner, "ADMIN"));

        assertEquals(200, exportGet(roster, staff, byte[].class).getStatusCode().value());
        assertEquals(200, exportGet(roster, exportToken("ADMIN"), byte[].class).getStatusCode().value());
        for (String path : List.of(roster, results)) {
            assertExportError(path, null, 401, "AUTH_UNAUTHORIZED");
            for (String token : List.of("invalid.jwt", badSignature, expired, disabled, missing)) {
                assertExportError(path, token, 401, "AUTH_UNAUTHORIZED");
            }
            for (String token : forbidden) {
                assertExportError(path, token, 403, "AUTH_FORBIDDEN");
            }
        }
    }

    /** ID sai cú pháp hoặc miền trả 400, ID hợp lệ nhưng thiếu bản ghi trả 404. */
    @Test void exportRejectsInvalidAndMissingTargets() {
        String staff = exportToken("STAFF");
        for (String rawId : List.of("abc", "2147483648", "0", "-1")) {
            assertExportError("/api/admission/exports/classes/" + rawId + "/roster", staff, 400, "ADM_VALIDATION_ERROR");
            assertExportError("/api/admission/exports/tests/" + rawId + "/results", staff, 400, "ADM_VALIDATION_ERROR");
        }
        assertExportError(rosterExportPath(Integer.MAX_VALUE), staff, 404, "ADM_CLASS_NOT_FOUND");
        assertExportError(resultsExportPath(Integer.MAX_VALUE), staff, 404, "ADM_TEST_NOT_FOUND");
    }

    private int exportEnrollmentUser(int enrollmentId) {
        return jdbc.queryForObject("SELECT student_id FROM admission.enrollments WHERE id=?", Integer.class, enrollmentId);
    }

    private int exportUser(String role) {
        return jdbc.queryForObject("""
                INSERT INTO admission.users(email,password_hash,full_name,role,email_verified_at,must_change_password)
                VALUES (?,'unused','QA Export',?,CURRENT_TIMESTAMP,FALSE) RETURNING id
                """, Integer.class, "qa-export-" + UUID.randomUUID() + "@example.test", role);
    }

    private int exportTestSet() {
        String key = "qa-" + UUID.randomUUID();
        return jdbc.queryForObject("INSERT INTO admission.test_sets(code,title) VALUES (?,?) RETURNING id", Integer.class, key, key);
    }

    private String exportToken(String role) {
        return exportJwtFor(exportUser(role), role);
    }

    private String exportJwtFor(int userId, String role) {
        return exportJwt.generateAccessToken(userId, "qa-export@example.test", role, "QA Export");
    }

    private String rosterExportPath(int classId) {
        return "/api/admission/exports/classes/" + classId + "/roster?format=csv";
    }

    private String resultsExportPath(int testSetId) {
        return "/api/admission/exports/tests/" + testSetId + "/results?format=csv";
    }

    private <T> ResponseEntity<T> exportGet(String path, String token, Class<T> responseType) {
        var headers = new HttpHeaders();
        if (token != null) headers.setBearerAuth(token);
        return http.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), responseType);
    }

    private void assertExportError(String path, String token, int status, String errorCode) {
        var result = exportGet(path, token, JsonNode.class);
        assertEquals(status, result.getStatusCode().value(), path);
        assertEquals(errorCode, result.getBody().at("/error/code").asText());
        assertFalse(result.getHeaders().containsKey(HttpHeaders.CONTENT_DISPOSITION));
    }

    private String assertCsv(ResponseEntity<byte[]> result, String fileName) {
        assertEquals(200, result.getStatusCode().value());
        assertEquals("text", result.getHeaders().getContentType().getType());
        assertEquals("csv", result.getHeaders().getContentType().getSubtype());
        assertEquals(StandardCharsets.UTF_8, result.getHeaders().getContentType().getCharset());
        assertEquals("attachment", result.getHeaders().getContentDisposition().getType());
        assertEquals(fileName, result.getHeaders().getContentDisposition().getFilename());
        return new String(result.getBody(), StandardCharsets.UTF_8);
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

    /** Tạo lượt thi thật qua HTTP cho tài khoản đã xác minh và một câu có trọng số mặc định. */
    private PlacementFixture placementFixture() {
        String key = "qa-" + UUID.randomUUID();
        int set = jdbc.queryForObject("INSERT INTO admission.test_sets(code,title,duration_minutes) VALUES (?,?,10) RETURNING id",
                Integer.class, key, key);
        int question = jdbc.queryForObject("""
                INSERT INTO admission.test_questions(test_set_id,skill_type,question_text,option_a,option_b,option_c,option_d,correct_option)
                VALUES (?,'GRAMMAR','QA','A','B','C','D','A') RETURNING id
                """, Integer.class, set);
        var headers = new org.springframework.http.HttpHeaders();
        headers.setBearerAuth(registerAndLogin(key + "@example.test"));
        var start = http.postForEntity("/api/admission/tests/public/" + set + "/start",
                new org.springframework.http.HttpEntity<>(headers), JsonNode.class);
        assertEquals(200, start.getStatusCode().value(), String.valueOf(start.getBody()));
        return new PlacementFixture(set, question, start.getBody().get("data"), headers);
    }

    private record PlacementFixture(int set, int question, JsonNode start, org.springframework.http.HttpHeaders headers) {
        int attempt() { return start.get("attempt_id").asInt(); }
        String resumeUrl() { return "/api/admission/tests/public/attempts/" + attempt(); }
        Map<String, Object> answers(String option) {
            return Map.of("attempt_id", attempt(), "answers", List.of(Map.of("question_id", question, "selected_option", option)));
        }
    }

    private org.springframework.http.ResponseEntity<JsonNode> placementSubmit(PlacementFixture exam, Map<String, Object> body) {
        return http.postForEntity("/api/admission/tests/public/submit",
                new org.springframework.http.HttpEntity<>(body, exam.headers()), JsonNode.class);
    }

    private String placementStatus(PlacementFixture exam) {
        return jdbc.queryForObject("SELECT status FROM admission.test_attempts WHERE id=?", String.class, exam.attempt());
    }

    private int placementSubmissions(PlacementFixture exam) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM admission.test_submissions WHERE attempt_id=?", Integer.class, exam.attempt());
    }

    private void movePlacementDeadlineIntoPast(PlacementFixture exam) {
        jdbc.update("""
                UPDATE admission.test_attempts SET started_at=clock_timestamp()-interval '11 minutes',
                    expires_at=clock_timestamp()-interval '1 minute' WHERE id=?
                """, exam.attempt());
    }

    /** Refresh lấy giờ mới trên cùng deadline, chỉ chính chủ có quyền xem đề đang làm. */
    @Test void placementResumeKeepsDeadlineAndEnforcesOwnership() {
        var exam = placementFixture();
        var resumed = http.exchange(exam.resumeUrl(), org.springframework.http.HttpMethod.GET,
                new org.springframework.http.HttpEntity<>(exam.headers()), JsonNode.class);
        assertEquals(200, resumed.getStatusCode().value());
        JsonNode data = resumed.getBody().get("data");
        assertEquals("STARTED", data.get("status").asText());
        assertEquals(exam.attempt(), data.get("attempt_id").asInt());
        assertEquals(java.time.LocalDateTime.parse(exam.start().get("expires_at").asText()),
                java.time.LocalDateTime.parse(data.get("expires_at").asText()));
        assertFalse(java.time.LocalDateTime.parse(data.get("server_now").asText())
                .isBefore(java.time.LocalDateTime.parse(exam.start().get("server_now").asText())));
        assertFalse(data.get("questions").get(0).has("correct_option"));
        assertFalse(data.hasNonNull("attempt_token"));
        assertEquals("no-store", resumed.getHeaders().getCacheControl());
        assertEquals(403, http.getForEntity(exam.resumeUrl(), JsonNode.class).getStatusCode().value());

        var other = new org.springframework.http.HttpHeaders();
        other.setBearerAuth(registerAndLogin("qa-placement-other-" + UUID.randomUUID() + "@example.test"));
        assertEquals(403, http.exchange(exam.resumeUrl(), org.springframework.http.HttpMethod.GET,
                new org.springframework.http.HttpEntity<>(other), JsonNode.class).getStatusCode().value());
        assertEquals(403, http.postForEntity("/api/admission/tests/public/submit",
                new org.springframework.http.HttpEntity<>(exam.answers("A"), other), JsonNode.class).getStatusCode().value());

        movePlacementDeadlineIntoPast(exam);
        var expired = http.exchange(exam.resumeUrl(), org.springframework.http.HttpMethod.GET,
                new org.springframework.http.HttpEntity<>(exam.headers()), JsonNode.class);
        assertEquals("EXPIRED", expired.getBody().get("data").get("status").asText());
        assertTrue(expired.getBody().get("data").get("questions").isEmpty());
        assertEquals(0, placementSubmissions(exam));
    }

    /** Request đến khi còn hạn nhưng bị chặn bởi khóa phải kiểm tra lại deadline sau khi chờ. */
    @Test void placementSubmitWaitingPastDeadlineCannotGrade() throws Exception {
        var exam = placementFixture();
        jdbc.update("UPDATE admission.test_attempts SET expires_at=clock_timestamp()+interval '3 seconds' WHERE id=?", exam.attempt());
        try (var pool = Executors.newSingleThreadExecutor()) {
            var pending = new org.springframework.transaction.support.TransactionTemplate(holdTxManager).execute(transaction -> {
                jdbc.queryForObject("SELECT id FROM admission.test_attempts WHERE id=? FOR UPDATE", Integer.class, exam.attempt());
                var response = pool.submit(() -> placementSubmit(exam, exam.answers("A")));
                try {
                    assertThrows(TimeoutException.class, () -> response.get(500, TimeUnit.MILLISECONDS));
                    long remainingMs = jdbc.queryForObject("""
                            SELECT greatest(0, extract(epoch FROM expires_at-clock_timestamp())*1000)::bigint
                            FROM admission.test_attempts WHERE id=?
                            """, Long.class, exam.attempt());
                    assertTrue(remainingMs > 0);
                    Thread.sleep(remainingMs + 30);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ex);
                }
                return response;
            });
            var late = pending.get(10, TimeUnit.SECONDS);
            assertEquals(400, late.getStatusCode().value());
            assertEquals("ADM_ATTEMPT_EXPIRED", late.getBody().get("error").get("code").asText());
            assertEquals("EXPIRED", placementStatus(exam));
            assertEquals(0, placementSubmissions(exam));
        }
    }

    /** Lượt quá hạn vẫn phải commit EXPIRED khi API báo lỗi nghiệp vụ và không tạo kết quả. */
    @Test void placementLateSubmitCommitsExpiryWithoutGrading() {
        var exam = placementFixture();
        movePlacementDeadlineIntoPast(exam);
        var late = placementSubmit(exam, exam.answers("A"));
        assertEquals(400, late.getStatusCode().value());
        assertEquals("ADM_ATTEMPT_EXPIRED", late.getBody().get("error").get("code").asText());
        assertEquals("EXPIRED", placementStatus(exam));
        assertEquals(0, placementSubmissions(exam));
    }

    /** Hai request khác đáp án chỉ gọi chấm điểm một lần và chỉ lưu kết quả của request thắng. */
    @Test void placementConcurrentSubmitGradesOnce() throws Exception {
        var exam = placementFixture();
        CountDownLatch grading = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        doAnswer(invocation -> {
            if (calls.incrementAndGet() == 1) {
                grading.countDown();
                if (!release.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("QA synchronization timeout");
            }
            return invocation.callRealMethod();
        }).when(placementRecommendations).getRecommendation(org.mockito.ArgumentMatchers.eq(exam.set()), org.mockito.ArgumentMatchers.any());

        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> placementSubmit(exam, exam.answers("A")));
            try {
                assertTrue(grading.await(10, TimeUnit.SECONDS));
                var second = pool.submit(() -> placementSubmit(exam, exam.answers("B")));
                assertThrows(TimeoutException.class, () -> second.get(1, TimeUnit.SECONDS));
                release.countDown();
                assertEquals(200, first.get(10, TimeUnit.SECONDS).getStatusCode().value());
                var repeated = second.get(10, TimeUnit.SECONDS);
                assertEquals(400, repeated.getStatusCode().value());
                assertEquals("ADM_TEST_ALREADY_SUBMITTED", repeated.getBody().get("error").get("code").asText());
                assertEquals(1, calls.get());
                assertEquals(1, placementSubmissions(exam));
                assertEquals("SUBMITTED", placementStatus(exam));
                assertEquals("A", jdbc.queryForObject("""
                        SELECT answers_detail->0->>'selected_option' FROM admission.test_submissions WHERE attempt_id=?
                        """, String.class, exam.attempt()));
            } finally {
                release.countDown();
            }
        }
    }

    /** Lỗi chấm điểm giải phóng khóa, không lưu bài và cho phép thử lại khi còn thời hạn. */
    @Test void placementGradingFailureLeavesAttemptRetryable() {
        var exam = placementFixture();
        var answers = exam.answers("A").get("answers");
        assertEquals(400, placementSubmit(exam, Map.of("answers", answers)).getStatusCode().value());
        assertEquals(403, placementSubmit(exam, Map.of("attempt_id", -1, "answers", answers)).getStatusCode().value());
        assertEquals(400, placementSubmit(exam, Map.of("attempt_id", "invalid", "answers", answers)).getStatusCode().value());
        AtomicInteger calls = new AtomicInteger();
        doAnswer(invocation -> {
            if (calls.incrementAndGet() == 1) throw new IllegalStateException("QA grading failure");
            return invocation.callRealMethod();
        }).when(placementRecommendations).getRecommendation(org.mockito.ArgumentMatchers.eq(exam.set()), org.mockito.ArgumentMatchers.any());
        assertEquals(500, placementSubmit(exam, exam.answers("A")).getStatusCode().value());
        assertEquals("STARTED", placementStatus(exam));
        assertEquals(0, placementSubmissions(exam));
        var payload = new java.util.HashMap<>(exam.answers("A"));
        payload.put("total_score", 999);
        payload.put("status", "SUBMITTED");
        payload.put("submitted_at", "2000-01-01T00:00:00");
        var retried = placementSubmit(exam, payload);
        assertEquals(200, retried.getStatusCode().value());
        assertEquals(1, retried.getBody().get("data").get("total_score").asInt());
        assertEquals("SUBMITTED", placementStatus(exam));
        assertEquals(1, placementSubmissions(exam));
        var completed = http.exchange(exam.resumeUrl(), org.springframework.http.HttpMethod.GET,
                new org.springframework.http.HttpEntity<>(exam.headers()), JsonNode.class).getBody().get("data");
        assertEquals("SUBMITTED", completed.get("status").asText());
        assertTrue(completed.get("questions").isEmpty());
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

    /** Endpoint me chỉ trả thông tin của principal thật và từ chối refresh Bearer. */
    @Test void sessionMeUsesAccessTokenAndCurrentPrincipal() {
        var login = sessionLogin();
        var url = "/api/admission/auth/me";
        var response = http.exchange(url, HttpMethod.GET, new HttpEntity<>(sessionHeaders(login.access())), JsonNode.class);
        assertEquals(200, response.getStatusCode().value());
        var data = response.getBody().get("data");
        assertEquals(login.userId(), data.get("id").asInt());
        assertEquals("QA Session", data.get("full_name").asText());
        assertEquals("STAFF", data.get("role").asText());
        assertEquals(4, data.size());
        assertEquals(401, http.getForEntity(url, JsonNode.class).getStatusCode().value());
        jdbc.update("UPDATE admission.users SET is_active=FALSE WHERE id=?", login.userId());
        assertEquals(401, http.exchange(url, HttpMethod.GET, new HttpEntity<>(sessionHeaders(login.access())), JsonNode.class)
                .getStatusCode().value());
        assertEquals(403, sessionPost("/refresh", login.refresh(), null).getStatusCode().value());
    }

    /** Refresh token đang hiệu lực hoặc đã thu hồi đều không thể làm Bearer. */
    @Test void sessionRejectsRefreshTokenAsBearer() {
        var login = sessionLogin();
        var url = rosterExportPath(cancellationFixture()[0]);
        assertEquals(200, http.exchange(url, HttpMethod.GET, new HttpEntity<>(sessionHeaders(login.access())), byte[].class)
                .getStatusCode().value());
        assertEquals(401, http.exchange(url, HttpMethod.GET, new HttpEntity<>(sessionHeaders(login.refresh())), byte[].class)
                .getStatusCode().value());
        assertEquals(200, sessionPost("/logout", login.refresh(), login.access()).getStatusCode().value());
        assertEquals(401, http.exchange(url, HttpMethod.GET, new HttpEntity<>(sessionHeaders(login.refresh())), JsonNode.class)
                .getStatusCode().value());
    }

    /** Hết access token thì rotate trước, thu hồi cặp vừa cấp và kiểm tra replay bị từ chối. */
    @Test void sessionExpiredAccessCanRotateThenLogout() {
        var login = sessionLogin();
        var signer = new com.englishcenter.admission.security.JwtTokenProvider();
        ReflectionTestUtils.setField(signer, "jwtSecret", ReflectionTestUtils.getField(exportJwt, "jwtSecret"));
        ReflectionTestUtils.setField(signer, "accessTokenExpirationSeconds", -60L);
        String expiredAccess = signer.generateAccessToken(login.userId(), "qa@example.test", "STAFF", "QA Session");
        assertEquals(401, sessionPost("/logout", login.refresh(), expiredAccess).getStatusCode().value());
        var rotated = sessionPost("/refresh", login.refresh(), null);
        assertEquals(200, rotated.getStatusCode().value());
        var pair = rotated.getBody().get("data");
        String replacement = pair.get("refresh_token").asText();
        assertEquals(200, sessionPost("/logout", replacement, pair.get("access_token").asText()).getStatusCode().value());
        assertEquals(1, sessionAuditCount(login.userId(), "LOGOUT"));
        assertEquals(401, sessionPost("/refresh", replacement, null).getStatusCode().value());
    }

    /** Đăng xuất chính chủ thu hồi phiên một lần; token của tài khoản khác không bị đụng tới. */
    @Test void sessionLogoutRevokesOnlyOwnedTokenAndAuditsOnce() {
        var login = sessionLogin();
        var other = sessionLogin();
        assertEquals(403, sessionPost("/logout", login.refresh(), other.access()).getStatusCode().value());
        assertFalse(jdbc.queryForObject("SELECT is_revoked FROM admission.refresh_tokens WHERE token=?", Boolean.class, login.refresh()));
        assertEquals(401, http.postForEntity("/auth/logout", Map.of("refresh_token", login.refresh()), JsonNode.class)
                .getStatusCode().value());
        assertEquals(401, sessionPost("/logout", login.refresh(), null).getStatusCode().value());
        assertEquals(200, sessionPost("/logout", login.refresh(), login.access()).getStatusCode().value());
        assertEquals(200, sessionPost("/logout", login.refresh(), login.access()).getStatusCode().value());
        assertEquals(1, sessionAuditCount(login.userId(), "LOGOUT"));
        assertTrue(jdbc.queryForObject("SELECT is_revoked FROM admission.refresh_tokens WHERE token=?", Boolean.class, login.refresh()));
        assertEquals(401, sessionPost("/refresh", login.refresh(), null).getStatusCode().value());
    }

    /** Hai request chờ cùng khóa PostgreSQL. Replay phải thu hồi cả token vừa được cấp. */
    @Test @org.junit.jupiter.api.extension.ExtendWith(org.springframework.boot.test.system.OutputCaptureExtension.class)
    void sessionConcurrentRefreshRotatesOnceAndRevokesOnReplay(org.springframework.boot.test.system.CapturedOutput output) throws Exception {
        var login = sessionLogin();
        try (var pool = Executors.newFixedThreadPool(2)) {
            var requests = new org.springframework.transaction.support.TransactionTemplate(holdTxManager).execute(transaction -> {
                jdbc.queryForObject("SELECT id FROM admission.refresh_tokens WHERE token=? FOR UPDATE", Integer.class, login.refresh());
                var first = pool.submit(() -> sessionPost("/refresh", login.refresh(), null));
                var second = pool.submit(() -> sessionPost("/refresh", login.refresh(), null));
                assertThrows(TimeoutException.class, () -> first.get(300, TimeUnit.MILLISECONDS));
                assertThrows(TimeoutException.class, () -> second.get(300, TimeUnit.MILLISECONDS));
                return List.of(first, second);
            });
            var first = requests.get(0).get(10, TimeUnit.SECONDS);
            var second = requests.get(1).get(10, TimeUnit.SECONDS);
            assertEquals(List.of(200, 401), java.util.stream.Stream.of(first, second)
                    .map(r -> r.getStatusCode().value()).sorted().toList());
            var winner = first.getStatusCode().value() == 200 ? first : second;
            String replacement = winner.getBody().at("/data/refresh_token").asText();
            assertFalse(replacement.isBlank());
            assertFalse(login.refresh().equals(replacement));
            assertEquals(1, sessionAuditCount(login.userId(), "REFRESH_TOKEN"));
            assertEquals(1, sessionAuditCount(login.userId(), "SECURITY_ALERT_REPLAY_ATTACK"));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM admission.refresh_tokens WHERE user_id=? AND NOT is_revoked",
                    Integer.class, login.userId()));
            assertEquals(401, sessionPost("/refresh", replacement, null).getStatusCode().value());
            assertFalse(output.getAll().contains(login.access()));
            assertFalse(output.getAll().contains(login.refresh()));
            assertFalse(output.getAll().contains(replacement));
            assertFalse(output.getAll().contains(winner.getBody().at("/data/access_token").asText()));
        }
    }

    /** Dùng hợp đồng đăng nhập thật với tài khoản QA riêng cho từng kiểm thử phiên. */
    private SessionLogin sessionLogin() {
        String email = "qa-session-" + UUID.randomUUID() + "@example.test";
        String password = "qa-session-test-password";
        int id = jdbc.queryForObject("""
                INSERT INTO admission.users(email,password_hash,full_name,role,email_verified_at,must_change_password)
                VALUES (?,?,'QA Session','STAFF',CURRENT_TIMESTAMP,FALSE) RETURNING id
                """, Integer.class, email, encoder.encode(password));
        var login = http.postForEntity("/api/admission/auth/login", Map.of("email", email, "password", password), JsonNode.class);
        assertEquals(200, login.getStatusCode().value());
        var data = login.getBody().get("data");
        return new SessionLogin(id, data.get("access_token").asText(), data.get("refresh_token").asText());
    }

    private record SessionLogin(int userId, String access, String refresh) {}

    private HttpHeaders sessionHeaders(String access) {
        var headers = new HttpHeaders();
        if (access != null) headers.setBearerAuth(access);
        return headers;
    }

    private ResponseEntity<JsonNode> sessionPost(String path, String refresh, String access) {
        return http.postForEntity("/api/admission/auth" + path,
                new HttpEntity<>(Map.of("refresh_token", refresh), sessionHeaders(access)), JsonNode.class);
    }

    private int sessionAuditCount(int userId, String action) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM admission.auth_audit_logs WHERE user_id=? AND action=?",
                Integer.class, userId, action);
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

    /** Request mặc định của giao diện phải liệt kê cả phòng có branch_name NULL và giữ thứ tự mã. */
    @org.springframework.test.annotation.DirtiesContext(methodMode = org.springframework.test.annotation.DirtiesContext.MethodMode.BEFORE_METHOD)
    @Test void roomsHttpWithoutFiltersReturnsNullableBranchForStaffAndAdmin() {
        String prefix = "QA-R-" + UUID.randomUUID();
        int first = insertRoomForSearch(prefix + "-A", null, true, 20);
        int second = insertRoomForSearch(prefix + "-B", prefix, false, 30);
        for (String role : List.of("STAFF", "ADMIN")) {
            JsonNode result = roomsHttpList("", exportToken(role));
            var matches = new java.util.ArrayList<JsonNode>();
            result.forEach(row -> {
                int id = row.path("id").asInt();
                if (id == first || id == second) matches.add(row);
            });
            assertEquals(List.of(first, second), matches.stream().map(row -> row.path("id").asInt()).toList());
            assertTrue(matches.get(0).path("branch_name").isNull());
            assertEquals(prefix + "-A", matches.get(0).path("code").asText());
            assertEquals(20, matches.get(0).path("capacity").asInt());
            assertTrue(matches.get(0).path("is_active").asBoolean());
            assertFalse(matches.get(1).path("is_active").asBoolean());
        }
    }

    /** Khi chỉ lọc trạng thái và sức chứa, branch_name NULL không được loại khỏi kết quả. */
    @Test void roomsHttpFiltersWithoutBranchIncludeNullableBranch() {
        String prefix = "QA-R-" + UUID.randomUUID();
        int match = insertRoomForSearch(prefix + "-A", null, true, 25);
        int tooSmall = insertRoomForSearch(prefix + "-B", prefix, true, 24);
        int inactive = insertRoomForSearch(prefix + "-C", prefix, false, 30);
        JsonNode result = roomsHttpList("?is_active=true&min_capacity=25", exportToken("STAFF"));
        var ids = new java.util.HashSet<Integer>();
        result.forEach(row -> ids.add(row.path("id").asInt()));
        assertTrue(ids.contains(match));
        assertFalse(ids.contains(tooSmall));
        assertFalse(ids.contains(inactive));
    }

    /** Tìm chi nhánh theo chuỗi con không phân biệt hoa thường, có thể kết hợp các bộ lọc khác. */
    @Test void roomsHttpFiltersByBranchAndOtherOptions() {
        String prefix = "QA-R-" + UUID.randomUUID();
        int first = insertRoomForSearch(prefix + "-A", "Site " + prefix + " North", true, 30);
        int second = insertRoomForSearch(prefix + "-B", "Site " + prefix + " South", false, 45);
        String branchFilter = "?branch_name=" + prefix.toLowerCase(java.util.Locale.ROOT);
        String staff = exportToken("STAFF");
        JsonNode byBranch = roomsHttpList(branchFilter, staff);
        assertEquals(2, byBranch.size());
        assertEquals(first, byBranch.get(0).path("id").asInt());
        assertEquals(second, byBranch.get(1).path("id").asInt());

        JsonNode combined = roomsHttpList(branchFilter + "&is_active=false&min_capacity=45", staff);
        assertEquals(1, combined.size());
        assertEquals(second, combined.get(0).path("id").asInt());
        assertEquals("Site " + prefix + " South", combined.get(0).path("branch_name").asText());
        assertEquals(45, combined.get(0).path("capacity").asInt());
    }

    /** Chuỗi rỗng là bộ lọc LIKE hiện có; không khớp trả mảng rỗng. */
    @Test void roomsHttpHandlesBlankAndUnmatchedFilters() {
        String prefix = "QA-R-" + UUID.randomUUID();
        int named = insertRoomForSearch(prefix + "-A", prefix, true, 20);
        int unnamed = insertRoomForSearch(prefix + "-B", null, true, 20);
        String staff = exportToken("STAFF");
        JsonNode blank = roomsHttpList("?branch_name=&is_active=&min_capacity=", staff);
        var ids = new java.util.HashSet<Integer>();
        blank.forEach(row -> ids.add(row.path("id").asInt()));
        assertTrue(ids.contains(named));
        assertFalse(ids.contains(unnamed));
        assertTrue(roomsHttpList("?branch_name=no-match-" + UUID.randomUUID(), staff).isEmpty());
    }

    /** Bảo toàn quyền đọc danh mục theo BA: STAFF/ADMIN, các vai trò khác bị chặn. */
    @Test void roomsHttpRejectsUnauthorizedRoles() {
        String path = "/api/admission/rooms";
        assertEquals(401, http.getForEntity(path, JsonNode.class).getStatusCode().value());
        for (String role : List.of("STUDENT", "TEACHER")) {
            var response = http.exchange(path, HttpMethod.GET, new HttpEntity<>(sessionHeaders(exportToken(role))), JsonNode.class);
            assertEquals(403, response.getStatusCode().value(), role);
            assertEquals("AUTH_FORBIDDEN", response.getBody().at("/error/code").asText());
        }
    }

    /** Chèn phòng với mã riêng của ca thử, chỉ trong DB QA. */
    private int insertRoomForSearch(String code, String branch, boolean active, int capacity) {
        return jdbc.queryForObject("""
                INSERT INTO admission.rooms(code,name,branch_name,is_active,capacity)
                VALUES (?,'QA Room Search',?,?,?) RETURNING id
                """, Integer.class, code, branch, active, capacity);
    }

    /** Kiểm tra hợp đồng HTTP trước khi đọc danh sách JSON. */
    private JsonNode roomsHttpList(String query, String token) {
        var response = http.exchange("/api/admission/rooms" + query, HttpMethod.GET,
                new HttpEntity<>(sessionHeaders(token)), JsonNode.class);
        assertEquals(200, response.getStatusCode().value(), () -> query + " " + response.getBody());
        assertTrue(response.getBody().path("success").asBoolean());
        assertTrue(response.getBody().path("data").isArray());
        return response.getBody().path("data");
    }

    @Test void staffCreateRequiresBaFieldsAndRejectsInvalidInput() {
        var admin = sessionHeaders(exportToken("ADMIN"));
        for (String field : List.of("phone", "gender", "date_of_birth", "full_name", "email", "password", "role")) {
            var payload = staffPayload("STAFF");
            String email = payload.get("email");
            payload.remove(field);
            assertEquals(400, http.postForEntity("/api/admission/users", new HttpEntity<>(payload, admin), JsonNode.class)
                    .getStatusCode().value(), field);
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM admission.users WHERE email=?", Integer.class, email));
        }
        for (var invalid : List.of(Map.of("phone", " "), Map.of("gender", "UNKNOWN"),
                Map.of("date_of_birth", "invalid"), Map.of("role", "ADMIN"), Map.of("role", "STUDENT"),
                Map.of("email", "invalid"), Map.of("password", "é".repeat(37)))) {
            var payload = staffPayload("TEACHER");
            payload.putAll(invalid);
            assertEquals(400, http.postForEntity("/api/admission/users", new HttpEntity<>(payload, admin), JsonNode.class)
                    .getStatusCode().value(), invalid.keySet().toString());
        }
    }

    @Test void staffCreateRequiresAdminAndRejectsDuplicateEmail() {
        var payload = staffPayload("TEACHER");
        var staff = sessionHeaders(exportToken("STAFF"));
        var admin = sessionHeaders(exportToken("ADMIN"));
        assertEquals(403, http.postForEntity("/api/admission/users", new HttpEntity<>(payload, staff), JsonNode.class).getStatusCode().value());
        var created = http.postForEntity("/api/admission/users", new HttpEntity<>(payload, admin), JsonNode.class);
        assertEquals(201, created.getStatusCode().value());
        assertTrue(created.getBody().at("/data/must_change_password").asBoolean());
        assertFalse(created.getBody().get("data").has("password_hash"));
        assertFalse(created.getBody().get("data").has("password"));
        int id = created.getBody().at("/data/id").asInt();
        assertTrue(encoder.matches(payload.get("password"), jdbc.queryForObject(
                "SELECT password_hash FROM admission.users WHERE id=?", String.class, id)));
        payload.put("email", payload.get("email").toUpperCase(java.util.Locale.ROOT));
        assertEquals(409, http.postForEntity("/api/admission/users", new HttpEntity<>(payload, admin), JsonNode.class).getStatusCode().value());
    }

    @Test void staffFirstPasswordGateAndFreshLogin() {
        for (String role : List.of("STAFF", "TEACHER")) {
            var employee = createFirstPasswordAccount(role);
            var login = employeeLogin(employee, employee.password());
            assertTrue(login.getBody().at("/data/user/must_change_password").asBoolean());
            String access = login.getBody().at("/data/access_token").asText();
            String refresh = login.getBody().at("/data/refresh_token").asText();
            String legacy = oldStaffAccess(employee.id(), role);
            assertFirstPasswordGate(access);
            assertFirstPasswordGate(legacy);
            assertTrue(http.exchange("/api/admission/auth/me", HttpMethod.GET, new HttpEntity<>(sessionHeaders(access)),
                    JsonNode.class).getBody().at("/data/must_change_password").asBoolean());
            assertEquals(401, employeeChange(access, "wrong-current-password", "New-password-234").getStatusCode().value());
            assertEquals(400, employeeChange(access, employee.password(), "short").getStatusCode().value());
            assertEquals(400, employeeChange(access, employee.password(), employee.password()).getStatusCode().value());
            assertTrue(firstPasswordPending(employee.id()));
            assertEquals(0, sessionAuditCount(employee.id(), "PASSWORD_CHANGED"));
            assertEquals(200, employeeChange(access, employee.password(), "New-password-234").getStatusCode().value());
            assertFalse(firstPasswordPending(employee.id()));
            assertEquals(1, sessionAuditCount(employee.id(), "PASSWORD_CHANGED"));
            assertEquals(401, sessionPost("/refresh", refresh, null).getStatusCode().value());
            // Mô phỏng token giới hạn được ghi muộn sau transaction đổi mật khẩu đã thu hồi các phiên cũ.
            String lateRefresh = exportJwt.generateRefreshToken(employee.id(), UUID.randomUUID().toString(), true);
            jdbc.update("""
                    INSERT INTO admission.refresh_tokens(user_id,token,expires_at,is_revoked)
                    VALUES (?,?,CURRENT_TIMESTAMP+INTERVAL '1 hour',FALSE)
                    """, employee.id(), lateRefresh);
            assertEquals(401, sessionPost("/refresh", lateRefresh, null).getStatusCode().value());
            assertTrue(jdbc.queryForObject("SELECT is_revoked FROM admission.refresh_tokens WHERE token=?", Boolean.class, lateRefresh));
            for (String obsolete : List.of(access, legacy)) {
                assertEquals(401, http.exchange("/api/admission/auth/me", HttpMethod.GET, new HttpEntity<>(sessionHeaders(obsolete)),
                        JsonNode.class).getStatusCode().value());
            }
            assertEquals(401, employeeLogin(employee, employee.password()).getStatusCode().value());
            var fresh = employeeLogin(employee, "New-password-234");
            assertEquals(200, fresh.getStatusCode().value());
            String newAccess = fresh.getBody().at("/data/access_token").asText();
            assertEquals(200, http.exchange("/api/admission/tests/public/sets", HttpMethod.GET, new HttpEntity<>(sessionHeaders(newAccess)),
                    JsonNode.class).getStatusCode().value());
        }
    }

    @Test void staffFirstPasswordCanRefreshAndLogoutWithoutUnlocking() {
        var employee = createFirstPasswordAccount("STAFF");
        var login = employeeLogin(employee, employee.password()).getBody().path("data");
        var rotated = sessionPost("/refresh", login.get("refresh_token").asText(), null);
        assertEquals(200, rotated.getStatusCode().value());
        var pair = rotated.getBody().path("data");
        String access = pair.get("access_token").asText();
        assertFirstPasswordGate(access);
        assertEquals(200, sessionPost("/logout", pair.get("refresh_token").asText(), access).getStatusCode().value());
        assertTrue(firstPasswordPending(employee.id()));
        jdbc.update("UPDATE admission.users SET is_active=FALSE WHERE id=?", employee.id());
        assertEquals(401, employeeChange(access, employee.password(), "New-password-234").getStatusCode().value());
        assertEquals(403, employeeLogin(employee, employee.password()).getStatusCode().value());
    }

    @Test void staffFirstPasswordResetPreservesRequirement() {
        var employee = createFirstPasswordAccount("STAFF");
        assertEquals(200, http.postForEntity("/api/admission/auth/forgot-password", Map.of("email", employee.email()), JsonNode.class)
                .getStatusCode().value());
        String reset = mailbox.token(employee.email(), "reset");
        assertEquals(200, http.postForEntity("/api/admission/auth/reset-password",
                Map.of("token", reset, "new_password", "Recovered-password-234"), JsonNode.class).getStatusCode().value());
        assertTrue(firstPasswordPending(employee.id()));
        var login = employeeLogin(employee, "Recovered-password-234");
        assertEquals(200, login.getStatusCode().value());
        assertFirstPasswordGate(login.getBody().at("/data/access_token").asText());
    }

    @Test void staffFirstPasswordConcurrentChangesCommitOnce() throws Exception {
        var employee = createFirstPasswordAccount("STAFF");
        String access = employeeLogin(employee, employee.password()).getBody().at("/data/access_token").asText();
        try (var pool = Executors.newFixedThreadPool(2)) {
            var requests = new org.springframework.transaction.support.TransactionTemplate(holdTxManager).execute(transaction -> {
                jdbc.queryForObject("SELECT id FROM admission.users WHERE id=? FOR UPDATE", Integer.class, employee.id());
                var a = pool.submit(() -> employeeChange(access, employee.password(), "New-password-234"));
                var b = pool.submit(() -> employeeChange(access, employee.password(), "Other-password-345"));
                assertThrows(TimeoutException.class, () -> a.get(300, TimeUnit.MILLISECONDS));
                assertThrows(TimeoutException.class, () -> b.get(300, TimeUnit.MILLISECONDS));
                return List.of(a, b);
            });
            var first = requests.get(0).get(10, TimeUnit.SECONDS);
            var second = requests.get(1).get(10, TimeUnit.SECONDS);
            assertEquals(List.of(200, 401), java.util.stream.Stream.of(first, second)
                    .map(response -> response.getStatusCode().value()).sorted().toList());
        }
        assertFalse(firstPasswordPending(employee.id()));
        assertEquals(1, sessionAuditCount(employee.id(), "PASSWORD_CHANGED"));
    }

    private java.util.HashMap<String, String> staffPayload(String role) {
        return new java.util.HashMap<>(Map.of("email", "qa-staff-" + UUID.randomUUID() + "@example.test",
                "password", "QA-initial-password", "role", role, "full_name", "QA First Password", "phone", "0900000000",
                "gender", "OTHER", "date_of_birth", "1994-01-02"));
    }

    private record FirstPasswordAccount(int id, String email, String password) {}

    private FirstPasswordAccount createFirstPasswordAccount(String role) {
        var payload = staffPayload(role);
        var created = http.postForEntity("/api/admission/users",
                new HttpEntity<>(payload, sessionHeaders(exportToken("ADMIN"))), JsonNode.class);
        assertEquals(201, created.getStatusCode().value());
        return new FirstPasswordAccount(created.getBody().at("/data/id").asInt(), payload.get("email"), payload.get("password"));
    }

    private ResponseEntity<JsonNode> employeeLogin(FirstPasswordAccount employee, String password) {
        return http.postForEntity("/api/admission/auth/login", Map.of("email", employee.email(), "password", password), JsonNode.class);
    }

    private ResponseEntity<JsonNode> employeeChange(String access, String current, String next) {
        return http.postForEntity("/api/admission/auth/change-password",
                new HttpEntity<>(Map.of("current_password", current, "new_password", next), sessionHeaders(access)), JsonNode.class);
    }

    private boolean firstPasswordPending(int id) {
        return jdbc.queryForObject("SELECT must_change_password FROM admission.users WHERE id=?", Boolean.class, id);
    }

    private void assertFirstPasswordGate(String access) {
        var blocked = http.exchange("/api/admission/tests/public/sets", HttpMethod.GET, new HttpEntity<>(sessionHeaders(access)), JsonNode.class);
        assertEquals(403, blocked.getStatusCode().value());
        assertEquals("AUTH_PASSWORD_CHANGE_REQUIRED", blocked.getBody().at("/error/code").asText());
    }

    private String oldStaffAccess(int id, String role) {
        byte[] secret = ((String) ReflectionTestUtils.getField(exportJwt, "jwtSecret")).getBytes(StandardCharsets.UTF_8);
        var key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(java.util.Arrays.copyOf(secret, Math.max(secret.length, 32)));
        return io.jsonwebtoken.Jwts.builder().setSubject(String.valueOf(id)).claim("role", role)
                .setExpiration(new java.util.Date(System.currentTimeMillis() + 60_000))
                .signWith(key, io.jsonwebtoken.SignatureAlgorithm.HS256).compact();
    }

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
