package com.englishcenter.admission.service.impl;

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
import com.englishcenter.admission.repository.HoldExtensionAuditRepository;
import com.englishcenter.admission.security.SecurityUtils;
import com.englishcenter.admission.service.ClassService;
import com.englishcenter.admission.service.EnrollmentService;
import com.englishcenter.admission.service.UserService;
import com.englishcenter.admission.service.CashConfirmationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Triển khai chi tiết nghiệp vụ Tuyển sinh, Giữ chỗ 48h và Xét duyệt (Phase 4).
 * Bảo vệ nghiêm ngặt các ranh giới bất biến cốt lõi:
 * - INV-ADM-01: Không duyệt đơn nếu lớp đã đủ sĩ số trần max_students.
 * - INV-ADM-03: Không chuyển trạng thái trực tiếp từ CANCELLED/REJECTED sang APPROVED.
 * - INV-ENR-02: Idempotency chống duyệt đúp webhook từ Finance Service.
 * - INV-ENR-03: Đối soát ghi nhận thanh toán đến muộn khi đơn đã hết hạn.
 * - INV-SEC-01: Cách ly dữ liệu học viên (học viên chỉ xem đơn của chính mình).
 */
@Service
public class EnrollmentServiceImpl implements EnrollmentService {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentServiceImpl.class);

    private final EnrollmentRepository enrollmentRepository;
    private final ClassRepository classRepository;
    private final ProcessedFinanceEventRepository processedFinanceEventRepository;
    private final ClassService classService;
    private final UserService userService;
    private final FinanceServiceClient financeServiceClient;
    private final StudentServiceSyncHelper studentServiceSyncHelper;
    private final CashConfirmationService cashConfirmationService;
    private final TransactionTemplate callbackTransaction;
    private final TransactionTemplate holdTransaction;
    private final HoldExtensionAuditRepository holdAudit;

    public EnrollmentServiceImpl(EnrollmentRepository enrollmentRepository,
                                 ClassRepository classRepository,
                                 ProcessedFinanceEventRepository processedFinanceEventRepository,
                                 ClassService classService,
                                 UserService userService,
                                 FinanceServiceClient financeServiceClient,
                                 StudentServiceSyncHelper studentServiceSyncHelper,
                                 CashConfirmationService cashConfirmationService,
                                 PlatformTransactionManager transactionManager,
                                 HoldExtensionAuditRepository holdAudit) {
        this.enrollmentRepository = enrollmentRepository;
        this.classRepository = classRepository;
        this.processedFinanceEventRepository = processedFinanceEventRepository;
        this.classService = classService;
        this.userService = userService;
        this.financeServiceClient = financeServiceClient;
        this.studentServiceSyncHelper = studentServiceSyncHelper;
        this.cashConfirmationService = cashConfirmationService;
        this.callbackTransaction = new TransactionTemplate(transactionManager);
        this.callbackTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.holdAudit = holdAudit;
        this.holdTransaction = new TransactionTemplate(transactionManager);
        this.holdTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Tạo đơn đăng ký tuyển sinh nhập học và cấp slot giữ chỗ 48h (TASK-ADM-40, TASK-ADM-42).
     */
    @Override
    @Transactional
    public EnrollmentResponse createEnrollment(CreateEnrollmentRequest request) {
        if (SecurityUtils.hasRole("STUDENT") && !request.getEmail().equalsIgnoreCase(SecurityUtils.getCurrentUserEmail())) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN, "Chỉ được đăng ký cho tài khoản của chính mình");
        }
        if (request.getClassId() == null) {
            throw new BusinessException(ErrorCode.ADM_VALIDATION_ERROR, "Mã lớp học không được để trống");
        }

        // 1. Kiểm tra lớp học tồn tại và ở trạng thái OPEN
        // Khóa lớp trước khi đếm slot; transaction cạnh tranh phải chờ lần đăng ký trước commit.
        ClassEntity clazz = classRepository.findByIdWithLock(request.getClassId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_CLASS_NOT_FOUND, "Không tìm thấy lớp học ID: " + request.getClassId()));

        if (!"OPEN".equalsIgnoreCase(clazz.getStatus())) {
            throw new BusinessException(ErrorCode.ADM_CLASS_NOT_OPEN, "Lớp học " + clazz.getCode() + " hiện không ở trạng thái mở tuyển sinh (status=" + clazz.getStatus() + ")");
        }

        // 2. Kiểm tra slot khả dụng theo thời gian thực (TASK-ADM-24)
        ClassCapacityResponse capacity = classService.getClassCapacity(clazz.getId());
        if (capacity.getAvailableSlots() <= 0) {
            throw new BusinessException(ErrorCode.ADM_CLASS_FULL, "Lớp học đã đạt sĩ số tối đa (" + clazz.getMaxStudents() + " học viên), không thể nhận thêm đơn");
        }

        // 3. Tự động tìm hoặc sinh tài khoản STUDENT cho người nộp đơn (TASK-ADM-11)
        User student = userService.getOrCreateStudentUser(request.getEmail(), request.getFullName(), request.getPhone());

        // 4. Kiểm tra đơn đăng ký hiện tại của học viên trong lớp
        Optional<Enrollment> existingOpt = enrollmentRepository.findByStudentIdAndClassId(student.getId(), clazz.getId());
        Enrollment enrollment;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime holdExpiry = now.plusHours(48);

        if (existingOpt.isPresent()) {
            Enrollment existing = existingOpt.get();
            if ("APPROVED".equalsIgnoreCase(existing.getStatus())) {
                throw new BusinessException(ErrorCode.ADM_ENROLLMENT_EXISTS, "Học viên đã có đơn đăng ký đã được duyệt vào lớp này");
            }
            if ("PENDING".equalsIgnoreCase(existing.getStatus()) &&
                    (existing.getHoldExpiresAt() == null || existing.getHoldExpiresAt().isAfter(now))) {
                throw new BusinessException(ErrorCode.ADM_ENROLLMENT_EXISTS, "Học viên đang có đơn giữ chỗ còn hiệu lực cho lớp học này");
            }

            // Finance auto-generate hiện idempotent theo enrollment_id; chưa có contract thế hệ hóa đơn mới.
            // Không tái dùng ID/hoá đơn/receipt cũ cho một chu kỳ khác, không xóa lịch sử để giả chu kỳ mới.
            throw new BusinessException(ErrorCode.ADM_INVALID_STATE_TRANSITION,
                    "Đơn cũ cần đối soát chu kỳ hóa đơn với Finance trước khi đăng ký lại");
        } else {
            // Khởi tạo đơn tuyển sinh mới
            Enrollment newEnrollment = new Enrollment();
            newEnrollment.setStudentId(student.getId());
            newEnrollment.setClassId(clazz.getId());
            newEnrollment.setStatus("PENDING");
            newEnrollment.setHoldExpiresAt(holdExpiry);
            newEnrollment.setEnrolledAt(now);
            newEnrollment.setSource(request.getSource() != null ? request.getSource() : "ONLINE");
            newEnrollment.setEnrollmentChannel(request.getEnrollmentChannel() != null ? request.getEnrollmentChannel() : "ONLINE");
            newEnrollment.setNotes(request.getNotes());
            newEnrollment.setNote(request.getNotes());
            newEnrollment.setIsSyncedToStudent(false);
            newEnrollment.setStudentSyncStatus("NOT_STARTED");
            enrollment = enrollmentRepository.save(newEnrollment);
        }

        // 5. Yêu cầu Finance Service sinh mã hóa đơn học phí tương ứng (nếu kết nối được)
        try {
            InvoiceData invoice = financeServiceClient.generateInvoice(enrollment.getId(), student.getId(), clazz.getCourseId(), null);
            if (invoice != null && invoice.getInvoiceCode() != null) {
                enrollment.setInvoiceCode(invoice.getInvoiceCode());
                enrollment = enrollmentRepository.save(enrollment);
            }
        } catch (Exception ex) {
            log.warn("Chưa thể sinh hóa đơn tự động từ Finance Service cho đơn ID={}: {}", enrollment.getId(), ex.getMessage());
        }

        enrollment.setStudent(student);
        enrollment.setClazz(clazz);
        log.info("Tạo đơn tuyển sinh thành công: enrollment_id={}, student_email={}, class_code={}",
                enrollment.getId(), student.getEmail(), clazz.getCode());

        return EnrollmentResponse.fromEntity(enrollment);
    }

    /**
     * Lõi xét duyệt đơn đăng ký sang APPROVED kèm bảo vệ sĩ số trần INV-ADM-01 (TASK-ADM-41).
     */
    @Override
    @Transactional
    public Enrollment approveEnrollment(Integer enrollmentId) {
        // 1. Tìm đơn đăng ký
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_ENROLLMENT_NOT_FOUND, "Không tìm thấy đơn đăng ký ID: " + enrollmentId));

        // Idempotency cũng phải chờ khóa lớp để không ghi đè cascade đang chạy.

        // 3. INV-ADM-03: Chỉ đơn PENDING được duyệt; mọi trạng thái kết thúc, kể cả EXPIRED, đều bị từ chối.
        if (!"PENDING".equalsIgnoreCase(enrollment.getStatus()) && !"APPROVED".equalsIgnoreCase(enrollment.getStatus())) {
            throw new BusinessException(ErrorCode.ADM_INVALID_STATE_TRANSITION,
                    "Không thể chuyển đơn từ trạng thái " + enrollment.getStatus() + " sang APPROVED (INV-ADM-03)");
        }

        // 4. Khóa dòng lớp học bằng PESSIMISTIC_WRITE để chống xung đột race condition
        ClassEntity clazz = classRepository.findByIdWithLock(enrollment.getClassId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_CLASS_NOT_FOUND, "Không tìm thấy lớp học của đơn đăng ký"));

        enrollmentRepository.refreshForMutation(enrollment);
        if ("CANCELLED".equals(clazz.getStatus())) throw new BusinessException(ErrorCode.ADM_CLASS_NOT_OPEN);
        if ("APPROVED".equalsIgnoreCase(enrollment.getStatus())) return enrollment;
        if (!"PENDING".equalsIgnoreCase(enrollment.getStatus())) {
            throw new BusinessException(ErrorCode.ADM_INVALID_STATE_TRANSITION);
        }

        // Deadline là nguồn quyết định, không phụ thuộc scheduler đã quét hay chưa.
        if (isHoldOverdue(enrollment, LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.ADM_INVALID_STATE_TRANSITION, "Đơn đăng ký đã quá thời hạn giữ chỗ");
        }

        // 5. Ràng buộc bất biến Sĩ số trần INV-ADM-01
        long currentApprovedCount = enrollmentRepository.countByClassIdAndStatus(clazz.getId(), "APPROVED");
        if (currentApprovedCount >= clazz.getMaxStudents()) {
            throw new BusinessException(ErrorCode.ADM_CLASS_FULL,
                    "Không thể duyệt đơn: Lớp " + clazz.getName() + " đã đủ sĩ số tối đa (" + clazz.getMaxStudents() + " học viên)");
        }

        // 6. Cho phép chuyển sang trạng thái APPROVED
        enrollment.setStatus("APPROVED");
        enrollment.setApprovedAt(LocalDateTime.now());
        enrollment = enrollmentRepository.save(enrollment);

        // 7. Đồng bộ hồ sơ học viên sang Student Service (TASK-ADM-47, INV-SYS-02)
        studentServiceSyncHelper.syncStudentProfile(enrollment);

        log.info("Duyệt đơn đăng ký thành công: enrollment_id={}, class_id={}, approved_count={}/{}",
                enrollment.getId(), clazz.getId(), currentApprovedCount + 1, clazz.getMaxStudents());

        return enrollment;
    }

    /**
     * Gửi yêu cầu đối soát tiền mặt. Service riêng quản lý transaction và audit từ chối.
     */
    @Override
    public EnrollmentResponse approveManual(Integer enrollmentId, ManualApproveRequest request) {
        return cashConfirmationService.request(enrollmentId, request);
    }

    /**
     * Webhook tiếp nhận thanh toán thành công từ Finance Service (TASK-ADM-45, INV-ENR-02, INV-ENR-03).
     */
    @Override
    public EnrollmentResponse processPaymentSuccess(PaymentSuccessWebhookRequest request) {
        Integer id = null;
        try {
            try {
                id = request.parseNumericEnrollmentId();
            } catch (IllegalArgumentException ex) {
                throw new BusinessException(ErrorCode.ADM_VALIDATION_ERROR, "Mã đơn callback không hợp lệ");
            }
            if (id == null || request.getEventId() == null || request.getEventId().isBlank() || request.getEventId().length() > 255
                    || request.getInvoiceId() == null || request.getInvoiceId().isBlank() || request.getInvoiceId().length() > 50
                    || request.getAmountPaid() == null || request.getAmountPaid().signum() <= 0
                    || request.getPaidAt() == null || request.getInvoiceStatus() != null && !"PAID".equals(request.getInvoiceStatus())) {
                throw new BusinessException(ErrorCode.ADM_VALIDATION_ERROR, "Callback thiếu bằng chứng hoặc không phải PAID");
            }
            Integer enrollmentId = id;
            // Kết thúc commit/rollback và nhả connection trước khi ghi audit độc lập.
            // Không giữ khóa callback trong lúc chờ connection thứ hai để ghi audit khi tải cao.
            return callbackTransaction.execute(status -> applyPaymentSuccess(enrollmentId, request));
        } catch (BusinessException ex) {
            String event = request.getEventId();
            if (event != null && event.length() > 255) event = event.substring(0, 255);
            processedFinanceEventRepository.auditRejected(event, id, ex.getErrorCode().name());
            throw ex;
        }
    }

    /**
     * Xử lý evidence đã qua validation trong transaction callback riêng.
     * Khóa event rồi lớp; receipt, chuyển trạng thái và Student outbox cùng commit hoặc rollback.
     * ADM_VALIDATION_ERROR khi event/binding/chu kỳ sai; các guard duyệt giữ nguyên mã lỗi cũ.
     */
    private EnrollmentResponse applyPaymentSuccess(Integer id, PaymentSuccessWebhookRequest request) {
        // Endpoint payment-success xác nhận PAID ngầm theo TASK-ADM-45, không thay sổ thu Finance.
        processedFinanceEventRepository.lockCallbacks();
        var previous = processedFinanceEventRepository.findByFinanceEventId(request.getEventId());
        if (previous.isPresent()) {
            var receipt = previous.get();
            if (!id.equals(receipt.getEnrollmentId()) || !request.fingerprint().equals(receipt.getPayloadHash())) {
                throw new BusinessException(ErrorCode.ADM_VALIDATION_ERROR,
                        "Event đã có nhưng bằng chứng/binding khác hoặc receipt legacy cần đối soát");
            }
            return EnrollmentResponse.fromEntity(enrollmentRepository.findById(id)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ADM_ENROLLMENT_NOT_FOUND)));
        }
        var enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_ENROLLMENT_NOT_FOUND));
        var clazz = classRepository.findByIdWithLock(enrollment.getClassId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_CLASS_NOT_FOUND));
        enrollmentRepository.refreshForMutation(enrollment);
        if (!request.getInvoiceId().equals(enrollment.getInvoiceCode()) || enrollment.getEnrolledAt() == null
                || request.getPaidAt().isBefore(enrollment.getEnrolledAt()) || request.getPaidAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.ADM_VALIDATION_ERROR, "Invoice/thời điểm không khớp chu kỳ hiện hành; cần đối soát");
        }
        if (isHoldOverdue(enrollment, LocalDateTime.now())) enrollment.setStatus("EXPIRED");
        if (!"PENDING".equals(enrollment.getStatus()) || "CANCELLED".equals(clazz.getStatus())) {
            // Ghi journal cả REJECTED/refund-pending/APPROVED, không hồi sinh hoặc đổi approval method.
            String note = "[Thanh toán muộn - INV-ENR-03]: Finance callback cần đối soát; trạng thái " + enrollment.getStatus();
            enrollment.setNotes((enrollment.getNotes() == null ? "" : enrollment.getNotes() + " | ") + note);
            enrollment.setNote(enrollment.getNotes());
            enrollmentRepository.save(enrollment);
        } else {
            enrollment = approveEnrollment(id);
            enrollment.setApprovalMethod("ONLINE_PAYMENT");
            enrollmentRepository.save(enrollment);
        }
        ProcessedFinanceEvent receipt = new ProcessedFinanceEvent();
        receipt.setEnrollmentId(id);
        receipt.setFinanceEventId(request.getEventId());
        receipt.setPaidAt(request.getPaidAt());
        receipt.setPayloadHash(request.fingerprint());
        processedFinanceEventRepository.save(receipt);
        return EnrollmentResponse.fromEntity(enrollment);
    }

    /**
     * Gia hạn thời gian giữ chỗ 48h bởi Học vụ (TASK-ADM-44).
     */
    @Override
    public EnrollmentResponse extendHold(Integer enrollmentId, ExtendHoldRequest request) {
        Integer actor = SecurityUtils.getCurrentUserId();
        try {
            // Transaction riêng kết thúc trước audit từ chối, không giữ khóa lớp/connection khi ghi audit khác.
            return holdTransaction.execute(status -> applyHoldExtension(enrollmentId, actor, request));
        } catch (BusinessException ex) {
            holdAudit.rejected(enrollmentId, actor, request, ex.getErrorCode().name());
            throw ex;
        }
    }

    /**
     * Gia hạn PENDING còn hiệu lực theo FR-ENR-05, trong transaction gọi từ extendHold.
     * Khóa lớp rồi refresh/khóa đơn như callback/hủy/duyệt; ADM_HOLD_CONFLICT khi snapshot deadline cũ.
     * Không quyết định giới hạn số lần hoặc khôi phục đơn đã quá hạn thay cho nhóm nghiệp vụ.
     */
    private EnrollmentResponse applyHoldExtension(Integer enrollmentId, Integer actor, ExtendHoldRequest request) {
        if (actor == null) throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED);
        if (!SecurityUtils.hasRole("STAFF") && !SecurityUtils.hasRole("ADMIN")) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        if (request == null || request.getExtendedHours() == null || request.getExtendedHours() < 1 || request.getExtendedHours() > 72
                || request.getReason() == null || request.getReason().trim().length() < 5 || request.getExpectedHoldExpiresAt() == null) {
            throw new BusinessException(ErrorCode.ADM_VALIDATION_ERROR);
        }
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_ENROLLMENT_NOT_FOUND));
        ClassEntity clazz = classRepository.findByIdWithLock(enrollment.getClassId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_CLASS_NOT_FOUND, "Không tìm thấy lớp học"));
        enrollmentRepository.refreshForMutation(enrollment);
        if ("CANCELLED".equals(clazz.getStatus())) throw new BusinessException(ErrorCode.ADM_CLASS_NOT_OPEN);
        if (!"PENDING".equalsIgnoreCase(enrollment.getStatus())) {
            throw new BusinessException(ErrorCode.ADM_INVALID_STATE_TRANSITION);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime previous = enrollment.getHoldExpiresAt();
        // Deadline quyết định hiệu lực, không dựa vào job đã đổi status hay chưa. NULL không đủ bằng chứng còn hạn.
        if (previous == null || !previous.isAfter(now)) throw new BusinessException(ErrorCode.ADM_HOLD_EXPIRED);
        // Compare-and-set dưới row lock: double click/retry/cùng snapshot chỉ một lần được tăng deadline.
        if (!previous.equals(request.getExpectedHoldExpiresAt())) throw new BusinessException(ErrorCode.ADM_HOLD_CONFLICT);
        long occupied = enrollmentRepository.countOccupiedSlots(clazz.getId(), now);
        // Slot của đơn còn hạn đã nằm trong occupied; gia hạn không xin thêm slot mới.
        if (occupied > clazz.getMaxStudents()) {
            throw new BusinessException(ErrorCode.ADM_CLASS_FULL, "Không còn chỗ để gia hạn giữ chỗ");
        }

        LocalDateTime newHoldExpiresAt = previous.plusHours(request.getExtendedHours());
        enrollment.setHoldExpiresAt(newHoldExpiresAt);
        enrollment.setHoldExtendedByUserId(actor);
        enrollment.setHoldExtensionReason(request.getReason().trim());

        String staffName = SecurityUtils.getCurrentUserFullName() != null ? SecurityUtils.getCurrentUserFullName() : "Học vụ";
        String noteStr = String.format("[Gia hạn %dh bởi %s (ID=%d) lúc %s]: %s", request.getExtendedHours(), staffName, actor, now, request.getReason().trim());
        String existingNotes = enrollment.getNotes() != null ? enrollment.getNotes() : "";
        enrollment.setNotes(existingNotes.isEmpty() ? noteStr : existingNotes + " | " + noteStr);
        enrollment.setNote(enrollment.getNotes());

        enrollment = enrollmentRepository.save(enrollment);
        holdAudit.extended(enrollmentId, actor, request.getExtendedHours(), previous, newHoldExpiresAt);
        log.info("Gia hạn giữ chỗ thành công: enrollment_id={}, new_hold_expires_at={}", enrollment.getId(), newHoldExpiresAt);

        return EnrollmentResponse.fromEntity(enrollment);
    }

    /**
     * Hủy đơn đăng ký tuyển sinh.
     */
    @Override
    @Transactional
    public EnrollmentResponse cancelEnrollment(Integer enrollmentId, String reason) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_ENROLLMENT_NOT_FOUND, "Không tìm thấy đơn đăng ký ID: " + enrollmentId));

        // Kiểm tra quyền hạn nếu là học viên
        if (SecurityUtils.isAuthenticated() && !SecurityUtils.hasRole("ADMIN") && !SecurityUtils.hasRole("STAFF")) {
            Integer currentUserId = SecurityUtils.getCurrentUserId();
            if (!enrollment.getStudentId().equals(currentUserId)) {
                throw new BusinessException(ErrorCode.AUTH_FORBIDDEN, "Bạn không có quyền hủy đơn đăng ký của người khác");
            }
        }

        // Cùng thứ tự khóa lớp -> đơn như duyệt/gia hạn, tránh hủy bị ghi đè bởi trạng thái cũ.
        classRepository.findByIdWithLock(enrollment.getClassId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_CLASS_NOT_FOUND));
        enrollmentRepository.refreshForMutation(enrollment);
        if ("CANCELLED".equalsIgnoreCase(enrollment.getStatus()) || "CANCELLED_REFUND_PENDING".equalsIgnoreCase(enrollment.getStatus())) {
            return EnrollmentResponse.fromEntity(enrollment);
        }

        enrollment.setStatus("CANCELLED");
        String cancelNote = String.format("[Hủy đơn]: %s", reason != null ? reason : "Người dùng yêu cầu hủy đơn");
        String existing = enrollment.getNotes() != null ? enrollment.getNotes() : "";
        enrollment.setNotes(existing.isEmpty() ? cancelNote : existing + " | " + cancelNote);
        enrollment.setNote(enrollment.getNotes());

        enrollment = enrollmentRepository.save(enrollment);
        log.info("Đã hủy đơn đăng ký: enrollment_id={}, reason={}", enrollment.getId(), reason);

        return EnrollmentResponse.fromEntity(enrollment);
    }

    /**
     * Quét và cập nhật hàng loạt đơn PENDING quá hạn sang EXPIRED (TASK-ADM-43).
     */
    @Override
    @Transactional
    public int expireOverdueEnrollments() {
        return enrollmentRepository.expireOverdueEnrollments(LocalDateTime.now());
    }

    /** Trả true khi đơn PENDING có deadline không còn ở tương lai; null giữ quy ước legacy. */
    private boolean isHoldOverdue(Enrollment enrollment, LocalDateTime now) {
        return "PENDING".equalsIgnoreCase(enrollment.getStatus())
                && enrollment.getHoldExpiresAt() != null && !enrollment.getHoldExpiresAt().isAfter(now);
    }

    /**
     * Tra cứu chi tiết một đơn đăng ký kèm kiểm tra phân quyền cách ly dữ liệu INV-SEC-01.
     */
    @Override
    @Transactional(readOnly = true)
    public EnrollmentResponse getEnrollmentById(Integer id) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_ENROLLMENT_NOT_FOUND, "Không tìm thấy đơn đăng ký ID: " + id));

        // Kiểm tra cách ly dữ liệu INV-SEC-01
        if (SecurityUtils.isAuthenticated() && !SecurityUtils.hasRole("ADMIN") && !SecurityUtils.hasRole("STAFF")) {
            Integer currentUserId = SecurityUtils.getCurrentUserId();
            if (!enrollment.getStudentId().equals(currentUserId)) {
                throw new BusinessException(ErrorCode.AUTH_FORBIDDEN, "Bạn chỉ có thể xem thông tin đơn đăng ký của chính mình (INV-SEC-01)");
            }
        }

        return cashConfirmationService.response(enrollment);
    }

    /**
     * Tra cứu và phân trang danh sách đơn đăng ký.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> searchEnrollments(Integer classId, String status, Pageable pageable) {
        if (SecurityUtils.isAuthenticated() && !SecurityUtils.hasRole("ADMIN") && !SecurityUtils.hasRole("STAFF")) {
            Integer currentUserId = SecurityUtils.requireCurrentUserId();
            List<Enrollment> list = enrollmentRepository.findByStudentIdOrderByCreatedAtDesc(currentUserId);
            List<EnrollmentResponse> responses = list.stream().map(EnrollmentResponse::fromEntity).toList();
            return new PageImpl<>(responses, pageable, responses.size());
        }

        Page<Enrollment> page = enrollmentRepository.searchEnrollments(classId, status, pageable);
        return page.map(cashConfirmationService::response);
    }

    /**
     * Lấy toàn bộ đơn đăng ký của học viên hiện tại.
     */
    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getMyEnrollments() {
        Integer currentUserId = SecurityUtils.requireCurrentUserId();
        List<Enrollment> list = enrollmentRepository.findByStudentIdOrderByCreatedAtDesc(currentUserId);
        return list.stream().map(EnrollmentResponse::fromEntity).toList();
    }
}
