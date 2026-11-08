package com.englishcenter.admission.service;

import com.englishcenter.admission.client.FinanceServiceClient;
import com.englishcenter.admission.client.dto.InvoiceData;
import com.englishcenter.admission.common.exception.BusinessException;
import com.englishcenter.admission.common.exception.ErrorCode;
import com.englishcenter.admission.dto.clazz.ClassCapacityResponse;
import com.englishcenter.admission.dto.enrollment.CreateEnrollmentRequest;
import com.englishcenter.admission.dto.enrollment.EnrollmentResponse;
import com.englishcenter.admission.dto.enrollment.ExtendHoldRequest;
import com.englishcenter.admission.dto.enrollment.ManualApproveRequest;
import com.englishcenter.admission.dto.webhook.PaymentSuccessWebhookRequest;
import com.englishcenter.admission.entity.ClassEntity;
import com.englishcenter.admission.entity.Enrollment;
import com.englishcenter.admission.entity.ProcessedFinanceEvent;
import com.englishcenter.admission.entity.User;
import com.englishcenter.admission.helper.StudentServiceSyncHelper;
import com.englishcenter.admission.repository.ClassRepository;
import com.englishcenter.admission.repository.EnrollmentRepository;
import com.englishcenter.admission.repository.ProcessedFinanceEventRepository;
import com.englishcenter.admission.service.impl.EnrollmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Kiểm thử đơn vị cho EnrollmentService, kiểm định toàn bộ ranh giới bất biến (Phase 4):
 * - INV-ADM-01: Không duyệt đơn nếu lớp đã đủ sĩ số tối đa (max_students).
 * - INV-ADM-03: Không cho phép đơn CANCELLED/REJECTED chuyển trực tiếp sang APPROVED.
 * - INV-ENR-02: Idempotency chống duyệt đúp webhook từ Finance Service.
 * - INV-ENR-03: Không duyệt tự động khi tiền về cho đơn đã EXPIRED (ghi sổ đối soát).
 */
@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private ClassRepository classRepository;

    @Mock
    private ProcessedFinanceEventRepository processedFinanceEventRepository;

    @Mock
    private ClassService classService;

    @Mock
    private UserService userService;

    @Mock
    private FinanceServiceClient financeServiceClient;

    @Mock
    private StudentServiceSyncHelper studentServiceSyncHelper;

    @InjectMocks
    private EnrollmentServiceImpl enrollmentService;

    private ClassEntity mockClass;
    private User mockStudent;
    private Enrollment mockEnrollment;

    @BeforeEach
    void setUp() {
        mockClass = new ClassEntity();
        mockClass.setId(10);
        mockClass.setCode("CLS-TEST-01");
        mockClass.setName("Lớp Tiếng Anh Giao Tiếp");
        mockClass.setStatus("OPEN");
        mockClass.setMaxStudents(20);
        mockClass.setMinStudents(5);
        mockClass.setCourseId(1);

        mockStudent = new User();
        mockStudent.setId(101);
        mockStudent.setEmail("student@test.com");
        mockStudent.setFullName("Nguyen Van A");
        mockStudent.setPhone("0987654321");

        mockEnrollment = new Enrollment();
        mockEnrollment.setId(50);
        mockEnrollment.setClassId(10);
        mockEnrollment.setStudentId(101);
        mockEnrollment.setStatus("PENDING");
        mockEnrollment.setHoldExpiresAt(LocalDateTime.now().plusHours(48));
        mockEnrollment.setClazz(mockClass);
        mockEnrollment.setStudent(mockStudent);
    }

    @Test
    @DisplayName("Tạo đơn tuyển sinh thành công kèm giữ chỗ 48h")
    void testCreateEnrollment_Success_Hold48h() {
        CreateEnrollmentRequest req = new CreateEnrollmentRequest(10, "Nguyen Van A", "student@test.com", "0987654321", "Đăng ký ca tối");

        ClassCapacityResponse cap = new ClassCapacityResponse(10, "CLS-TEST-01", 20, 5L, 2L, 13L, false);

        when(classRepository.findById(10)).thenReturn(Optional.of(mockClass));
        when(classService.getClassCapacity(10)).thenReturn(cap);
        when(userService.getOrCreateStudentUser(req.getEmail(), req.getFullName(), req.getPhone())).thenReturn(mockStudent);
        when(enrollmentRepository.findByStudentIdAndClassId(101, 10)).thenReturn(Optional.empty());
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> {
            Enrollment e = invocation.getArgument(0);
            e.setId(50);
            return e;
        });
        when(financeServiceClient.generateInvoice(any(), eq(101), eq(1), any()))
                .thenReturn(new InvoiceData(999, "INV-2026-0001", "UNPAID", new BigDecimal("5000000.00")));

        EnrollmentResponse res = enrollmentService.createEnrollment(req);

        assertNotNull(res);
        assertEquals("PENDING", res.getStatus());
        assertNotNull(res.getHoldExpiresAt());
        assertTrue(res.getHoldExpiresAt().isAfter(LocalDateTime.now().plusHours(47)));
        verify(enrollmentRepository, atLeastOnce()).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("Tạo đơn thất bại khi lớp đã kín chỗ (available_slots <= 0)")
    void testCreateEnrollment_ClassFull_ThrowsException() {
        CreateEnrollmentRequest req = new CreateEnrollmentRequest(10, "Nguyen Van A", "student@test.com", "0987654321", "Đăng ký ca tối");

        ClassCapacityResponse cap = new ClassCapacityResponse(10, "CLS-TEST-01", 20, 20L, 0L, 0L, true);

        when(classRepository.findById(10)).thenReturn(Optional.of(mockClass));
        when(classService.getClassCapacity(10)).thenReturn(cap);

        BusinessException ex = assertThrows(BusinessException.class, () -> enrollmentService.createEnrollment(req));
        assertEquals(ErrorCode.ADM_CLASS_FULL, ex.getErrorCode());
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tạo đơn thất bại khi lớp không ở trạng thái OPEN")
    void testCreateEnrollment_ClassNotOpen_ThrowsException() {
        mockClass.setStatus("IN_PROGRESS");
        CreateEnrollmentRequest req = new CreateEnrollmentRequest(10, "Nguyen Van A", "student@test.com", "0987654321", "Đăng ký");

        when(classRepository.findById(10)).thenReturn(Optional.of(mockClass));

        BusinessException ex = assertThrows(BusinessException.class, () -> enrollmentService.createEnrollment(req));
        assertEquals(ErrorCode.ADM_CLASS_NOT_OPEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("Duyệt đơn thành công với Invariant Guard INV-ADM-01 (sĩ số chưa đạt trần)")
    void testApproveEnrollment_Success_GuardInvAdm01() {
        when(enrollmentRepository.findById(50)).thenReturn(Optional.of(mockEnrollment));
        when(classRepository.findByIdWithLock(10)).thenReturn(Optional.of(mockClass));
        when(enrollmentRepository.countByClassIdAndStatus(10, "APPROVED")).thenReturn(15L);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Enrollment approved = enrollmentService.approveEnrollment(50);

        assertEquals("APPROVED", approved.getStatus());
        assertNotNull(approved.getApprovedAt());
        verify(studentServiceSyncHelper).syncStudentProfile(approved);
    }

    @Test
    @DisplayName("Duyệt đơn bị chặn bởi Invariant Guard INV-ADM-01 khi lớp đã đạt trần sĩ số")
    void testApproveEnrollment_FullCapacity_ThrowsInvAdm01() {
        when(enrollmentRepository.findById(50)).thenReturn(Optional.of(mockEnrollment));
        when(classRepository.findByIdWithLock(10)).thenReturn(Optional.of(mockClass));
        // maxStudents = 20, đã có 20 học viên APPROVED
        when(enrollmentRepository.countByClassIdAndStatus(10, "APPROVED")).thenReturn(20L);

        BusinessException ex = assertThrows(BusinessException.class, () -> enrollmentService.approveEnrollment(50));
        assertEquals(ErrorCode.ADM_CLASS_FULL, ex.getErrorCode());
        assertEquals("PENDING", mockEnrollment.getStatus());
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Duyệt đơn bị chặn bởi Invariant Guard INV-ADM-03 khi đơn đang CANCELLED hoặc REJECTED")
    void testApproveEnrollment_FromCancelled_ThrowsInvAdm03() {
        mockEnrollment.setStatus("CANCELLED");
        when(enrollmentRepository.findById(50)).thenReturn(Optional.of(mockEnrollment));

        BusinessException ex = assertThrows(BusinessException.class, () -> enrollmentService.approveEnrollment(50));
        assertEquals(ErrorCode.ADM_INVALID_STATE_TRANSITION, ex.getErrorCode());
        verify(classRepository, never()).findByIdWithLock(any());
    }

    @Test
    @DisplayName("Duyệt đơn Idempotent khi đơn đã APPROVED từ trước")
    void testApproveEnrollment_Idempotency() {
        mockEnrollment.setStatus("APPROVED");
        when(enrollmentRepository.findById(50)).thenReturn(Optional.of(mockEnrollment));

        Enrollment result = enrollmentService.approveEnrollment(50);

        assertEquals("APPROVED", result.getStatus());
        verify(classRepository, never()).findByIdWithLock(any());
    }

    @Test
    @DisplayName("Học vụ duyệt nộp tiền mặt tại quầy (TASK-ADM-46)")
    void testManualApprove_Success() {
        ManualApproveRequest req = new ManualApproveRequest("PT-2026-0001", "CASH", new BigDecimal("5000000"), "Nộp tại quầy Cầu Giấy");

        when(enrollmentRepository.findById(50)).thenReturn(Optional.of(mockEnrollment));
        when(classRepository.findByIdWithLock(10)).thenReturn(Optional.of(mockClass));
        when(enrollmentRepository.countByClassIdAndStatus(10, "APPROVED")).thenReturn(5L);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EnrollmentResponse res = enrollmentService.approveManual(50, req);

        assertNotNull(res);
        assertEquals("APPROVED", res.getStatus());
        assertTrue(res.getNotes().contains("PT-2026-0001"));
    }

    @Test
    @DisplayName("Tiếp nhận Webhook thanh toán thành công từ Finance Service (TASK-ADM-45)")
    void testProcessPaymentSuccess_Success_Webhook() {
        PaymentSuccessWebhookRequest req = new PaymentSuccessWebhookRequest(
                "EVT-FIN-001", 50, "INV-2026-0001", new BigDecimal("5000000"), LocalDateTime.now(), "TXN-001");

        when(processedFinanceEventRepository.existsByFinanceEventId("EVT-FIN-001")).thenReturn(false);
        when(enrollmentRepository.findById(50)).thenReturn(Optional.of(mockEnrollment));
        when(classRepository.findByIdWithLock(10)).thenReturn(Optional.of(mockClass));
        when(enrollmentRepository.countByClassIdAndStatus(10, "APPROVED")).thenReturn(5L);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EnrollmentResponse res = enrollmentService.processPaymentSuccess(req);

        assertNotNull(res);
        assertEquals("APPROVED", res.getStatus());
        verify(processedFinanceEventRepository).save(any(ProcessedFinanceEvent.class));
    }

    @Test
    @DisplayName("Webhook Idempotency (INV-ENR-02): Bỏ qua khi eventId đã tồn tại")
    void testProcessPaymentSuccess_Idempotency_InvEnr02() {
        PaymentSuccessWebhookRequest req = new PaymentSuccessWebhookRequest(
                "EVT-FIN-001", 50, "INV-2026-0001", new BigDecimal("5000000"), LocalDateTime.now(), "TXN-001");

        when(processedFinanceEventRepository.existsByFinanceEventId("EVT-FIN-001")).thenReturn(true);
        when(enrollmentRepository.findById(50)).thenReturn(Optional.of(mockEnrollment));

        EnrollmentResponse res = enrollmentService.processPaymentSuccess(req);

        assertNotNull(res);
        verify(classRepository, never()).findByIdWithLock(any());
        verify(processedFinanceEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Thanh toán muộn (INV-ENR-03): Đơn EXPIRED không được tự duyệt mà ghi sổ đối soát")
    void testProcessPaymentSuccess_LatePayment_InvEnr03() {
        mockEnrollment.setStatus("EXPIRED");
        PaymentSuccessWebhookRequest req = new PaymentSuccessWebhookRequest(
                "EVT-FIN-LATE", 50, "INV-2026-0001", new BigDecimal("5000000"), LocalDateTime.now(), "TXN-001");

        when(processedFinanceEventRepository.existsByFinanceEventId("EVT-FIN-LATE")).thenReturn(false);
        when(enrollmentRepository.findById(50)).thenReturn(Optional.of(mockEnrollment));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EnrollmentResponse res = enrollmentService.processPaymentSuccess(req);

        assertNotNull(res);
        assertEquals("EXPIRED", res.getStatus()); // Không chuyển sang APPROVED!
        assertTrue(res.getNotes().contains("INV-ENR-03"));
        verify(processedFinanceEventRepository).save(any(ProcessedFinanceEvent.class));
        verify(studentServiceSyncHelper, never()).syncStudentProfile(any());
    }

    @Test
    @DisplayName("Gia hạn thời gian giữ chỗ thành công (TASK-ADM-44)")
    void testExtendHold_Success() {
        ExtendHoldRequest req = new ExtendHoldRequest(24, "Chờ người nhà chuyển tiền");
        when(enrollmentRepository.findById(50)).thenReturn(Optional.of(mockEnrollment));
        when(classRepository.findById(10)).thenReturn(Optional.of(mockClass));
        when(enrollmentRepository.countByClassIdAndStatus(10, "APPROVED")).thenReturn(5L);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EnrollmentResponse res = enrollmentService.extendHold(50, req);

        assertNotNull(res);
        assertEquals("PENDING", res.getStatus());
        assertTrue(res.getNotes().contains("Gia hạn 24h"));
    }

    @Test
    @DisplayName("Gia hạn thất bại khi đơn không ở trạng thái PENDING")
    void testExtendHold_NotPending_ThrowsException() {
        mockEnrollment.setStatus("EXPIRED");
        ExtendHoldRequest req = new ExtendHoldRequest(24, "Lý do");
        when(enrollmentRepository.findById(50)).thenReturn(Optional.of(mockEnrollment));

        BusinessException ex = assertThrows(BusinessException.class, () -> enrollmentService.extendHold(50, req));
        assertEquals(ErrorCode.ADM_INVALID_STATE_TRANSITION, ex.getErrorCode());
    }

    @Test
    @DisplayName("Quét và thu hồi các đơn giữ chỗ quá hạn (TASK-ADM-43)")
    void testExpireOverdueEnrollments() {
        when(enrollmentRepository.expireOverdueEnrollments(any(LocalDateTime.class))).thenReturn(3);

        int count = enrollmentService.expireOverdueEnrollments();

        assertEquals(3, count);
        verify(enrollmentRepository).expireOverdueEnrollments(any(LocalDateTime.class));
    }
}
