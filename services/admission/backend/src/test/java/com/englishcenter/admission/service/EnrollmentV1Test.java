package com.englishcenter.admission.service;

import com.englishcenter.admission.client.FinanceServiceClient;
import com.englishcenter.admission.common.exception.*;
import com.englishcenter.admission.dto.enrollment.*;
import com.englishcenter.admission.dto.webhook.PaymentSuccessWebhookRequest;
import com.englishcenter.admission.entity.*;
import com.englishcenter.admission.helper.StudentServiceSyncHelper;
import com.englishcenter.admission.repository.*;
import com.englishcenter.admission.service.impl.EnrollmentServiceImpl;
import org.junit.jupiter.api.*;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Tái hiện các lỗ hổng giữ chỗ và thanh toán trước khi scheduler chạy. */
class EnrollmentV1Test {
    private EnrollmentRepository enrollments;
    private ClassRepository classes;
    private ProcessedFinanceEventRepository events;
    private StudentServiceSyncHelper sync;
    private EnrollmentServiceImpl service;
    private Enrollment enrollment;
    private ClassEntity clazz;

    /** Dữ liệu đơn đã hết hạn nhưng scheduler chưa chuyển trạng thái. */
    @BeforeEach void setUp() {
        enrollments = mock(EnrollmentRepository.class); classes = mock(ClassRepository.class);
        events = mock(ProcessedFinanceEventRepository.class); sync = mock(StudentServiceSyncHelper.class);
        service = new EnrollmentServiceImpl(enrollments, classes, events, mock(ClassService.class),
                mock(UserService.class), mock(FinanceServiceClient.class), sync, mock(CashConfirmationService.class),
                mock(org.springframework.transaction.PlatformTransactionManager.class), mock(HoldExtensionAuditRepository.class));
        enrollment = new Enrollment(); enrollment.setId(1); enrollment.setClassId(2);
        enrollment.setStudentId(3); enrollment.setStatus("PENDING");enrollment.setInvoiceCode("INV-V1");enrollment.setEnrolledAt(LocalDateTime.now().minusDays(3));
        enrollment.setHoldExpiresAt(LocalDateTime.now().minusSeconds(1));
        clazz = new ClassEntity(); clazz.setId(2); clazz.setMaxStudents(2); clazz.setStatus("OPEN");
        when(enrollments.findById(1)).thenReturn(Optional.of(enrollment));
        when(classes.findById(2)).thenReturn(Optional.of(clazz));
        when(classes.findByIdWithLock(2)).thenReturn(Optional.of(clazz));
        when(enrollments.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    /** Không thể duyệt thủ công khi deadline đã qua dù trạng thái còn PENDING. */
    @Test void rejectsOverduePendingApproval() {
        BusinessException error = assertThrows(BusinessException.class, () -> service.approveEnrollment(1));
        assertEquals(ErrorCode.ADM_INVALID_STATE_TRANSITION, error.getErrorCode());
        verifyNoInteractions(sync);
    }

    /** Tiền đến muộn phải được đối soát và đánh dấu hết hạn trong cùng transaction. */
    @Test void latePaymentIsRecordedWithoutApproval() {
        service.processPaymentSuccess(new PaymentSuccessWebhookRequest("late-v1", 1, "INV-V1", java.math.BigDecimal.TEN, LocalDateTime.now(), null));
        assertEquals("EXPIRED", enrollment.getStatus());
        assertTrue(enrollment.getNotes().contains("Thanh toán muộn"));
        verify(events).save(any(ProcessedFinanceEvent.class));
        verifyNoInteractions(sync);
    }

    /** FR-ENR-05: đơn quá hạn bị từ chối trước bước xin slot, kể cả khi lớp còn chỗ. */
    @Test void cannotRestoreExpiredHoldEvenWithFreeCapacity() {
        var principal = new com.englishcenter.admission.security.UserPrincipal(3, "qa@example.test", "unused", "QA Staff", "STAFF", true,
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_STAFF")));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        try {
            var request = new ExtendHoldRequest(24, "Gia hạn học phí");
            request.setExpectedHoldExpiresAt(enrollment.getHoldExpiresAt());
            BusinessException error = assertThrows(BusinessException.class, () -> service.extendHold(1, request));
            assertEquals(ErrorCode.ADM_HOLD_EXPIRED, error.getErrorCode());
            verify(enrollments, never()).save(any());
            verify(enrollments, never()).countOccupiedSlots(anyInt(), any());
        } finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
    }
}
