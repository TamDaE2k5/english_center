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
import com.englishcenter.admission.security.SecurityUtils;
import com.englishcenter.admission.service.ClassService;
import com.englishcenter.admission.service.EnrollmentService;
import com.englishcenter.admission.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public EnrollmentServiceImpl(EnrollmentRepository enrollmentRepository,
                                 ClassRepository classRepository,
                                 ProcessedFinanceEventRepository processedFinanceEventRepository,
                                 ClassService classService,
                                 UserService userService,
                                 FinanceServiceClient financeServiceClient,
                                 StudentServiceSyncHelper studentServiceSyncHelper) {
        this.enrollmentRepository = enrollmentRepository;
        this.classRepository = classRepository;
        this.processedFinanceEventRepository = processedFinanceEventRepository;
        this.classService = classService;
        this.userService = userService;
        this.financeServiceClient = financeServiceClient;
        this.studentServiceSyncHelper = studentServiceSyncHelper;
    }

    /**
     * Tạo đơn đăng ký tuyển sinh nhập học và cấp slot giữ chỗ 48h (TASK-ADM-40, TASK-ADM-42).
     */
    @Override
    @Transactional
    public EnrollmentResponse createEnrollment(CreateEnrollmentRequest request) {
        if (request.getClassId() == null) {
            throw new BusinessException(ErrorCode.ADM_VALIDATION_ERROR, "Mã lớp học không được để trống");
        }

        // 1. Kiểm tra lớp học tồn tại và ở trạng thái OPEN
        ClassEntity clazz = classRepository.findById(request.getClassId())
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

            // Trường hợp đơn cũ đã CANCELLED hoặc EXPIRED: Kích hoạt lại bản ghi để tái sử dụng
            existing.setStatus("PENDING");
            existing.setHoldExpiresAt(holdExpiry);
            existing.setEnrolledAt(now);
            existing.setSource(request.getSource() != null ? request.getSource() : "ONLINE");
            existing.setEnrollmentChannel(request.getEnrollmentChannel() != null ? request.getEnrollmentChannel() : "ONLINE");
            existing.setNotes(request.getNotes());
            existing.setNote(request.getNotes());
            existing.setIsSyncedToStudent(false);
            existing.setStudentSyncStatus("NOT_STARTED");
            existing.setApprovedAt(null);
            existing.setApprovedByUserId(null);
            existing.setApprovalMethod(null);
            enrollment = enrollmentRepository.save(existing);
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

        // 2. Nếu đơn đã APPROVED thì bỏ qua (Idempotency)
        if ("APPROVED".equalsIgnoreCase(enrollment.getStatus())) {
            return enrollment;
        }

        // 3. Ràng buộc bất biến State Machine INV-ADM-03: CANCELLED hoặc REJECTED không được chuyển trực tiếp sang APPROVED
        if ("CANCELLED".equalsIgnoreCase(enrollment.getStatus()) ||
                "REJECTED".equalsIgnoreCase(enrollment.getStatus()) ||
                "CANCELLED_REFUND_PENDING".equalsIgnoreCase(enrollment.getStatus())) {
            throw new BusinessException(ErrorCode.ADM_INVALID_STATE_TRANSITION,
                    "Không thể chuyển đơn từ trạng thái " + enrollment.getStatus() + " sang APPROVED (INV-ADM-03)");
        }

        // 4. Khóa dòng lớp học bằng PESSIMISTIC_WRITE để chống xung đột race condition
        ClassEntity clazz = classRepository.findByIdWithLock(enrollment.getClassId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_CLASS_NOT_FOUND, "Không tìm thấy lớp học của đơn đăng ký"));

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
     * Duyệt đơn thu tiền mặt tại quầy bởi Học vụ (TASK-ADM-46).
     */
    @Override
    @Transactional
    public EnrollmentResponse approveManual(Integer enrollmentId, ManualApproveRequest request) {
        // Thực hiện duyệt qua lõi approveEnrollment có Invariant Guard bảo vệ
        Enrollment enrollment = approveEnrollment(enrollmentId);

        Integer staffUserId = SecurityUtils.getCurrentUserId();
        String staffName = SecurityUtils.getCurrentUserFullName() != null ? SecurityUtils.getCurrentUserFullName() : "Học vụ";

        enrollment.setApprovedByUserId(staffUserId);
        enrollment.setApprovalMethod("MANUAL_CASH");

        String receipt = request.getReceiptNumber() != null ? request.getReceiptNumber() : "N/A";
        String method = request.getPaymentMethod() != null ? request.getPaymentMethod() : "CASH";
        String noteStr = String.format("[Thu tại quầy bởi %s]: Phiếu thu=%s, Phương thức=%s, Thực thu=%s. %s",
                staffName, receipt, method, request.getAmountReceived(), request.getNotes() != null ? request.getNotes() : "");

        String existing = enrollment.getNotes() != null ? enrollment.getNotes() : "";
        String updated = existing.isEmpty() ? noteStr : existing + " | " + noteStr;
        enrollment.setNotes(updated);
        enrollment.setNote(updated);

        enrollment = enrollmentRepository.save(enrollment);
        return EnrollmentResponse.fromEntity(enrollment);
    }

    /**
     * Webhook tiếp nhận thanh toán thành công từ Finance Service (TASK-ADM-45, INV-ENR-02, INV-ENR-03).
     */
    @Override
    @Transactional
    public EnrollmentResponse processPaymentSuccess(PaymentSuccessWebhookRequest request) {
        // 1. Kiểm tra Idempotency (INV-ENR-02): Nếu eventId đã xử lý thì trả về kết quả ngay
        if (processedFinanceEventRepository.existsByFinanceEventId(request.getEventId())) {
            log.info("Sự kiện thanh toán {} đã được xử lý trước đó (Idempotency - INV-ENR-02)", request.getEventId());
            Enrollment existing = enrollmentRepository.findById(request.parseNumericEnrollmentId()).orElse(null);
            return EnrollmentResponse.fromEntity(existing);
        }

        // 2. Tra cứu đơn đăng ký
        Integer enrollmentId = request.parseNumericEnrollmentId();
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_ENROLLMENT_NOT_FOUND, "Không tìm thấy đơn đăng ký ID: " + enrollmentId));

        // 3. Xử lý thanh toán đến muộn (INV-ENR-03): Đơn đã EXPIRED hoặc CANCELLED
        if ("EXPIRED".equalsIgnoreCase(enrollment.getStatus()) || "CANCELLED".equalsIgnoreCase(enrollment.getStatus())) {
            log.warn("Tiếp nhận thanh toán cho đơn {} đã ở trạng thái {} (INV-ENR-03). Ghi nhận đối soát, không tự động duyệt.",
                    enrollment.getId(), enrollment.getStatus());

            ProcessedFinanceEvent event = new ProcessedFinanceEvent();
            event.setEnrollmentId(enrollment.getId());
            event.setFinanceEventId(request.getEventId());
            event.setPaidAt(request.getPaidAt() != null ? request.getPaidAt() : LocalDateTime.now());
            processedFinanceEventRepository.save(event);

            String lateNote = String.format("[Thanh toán muộn - INV-ENR-03]: Nhận tiền khi đơn đã %s. Chờ học vụ đối soát hoặc hoàn tiền.", enrollment.getStatus());
            String existingNotes = enrollment.getNotes() != null ? enrollment.getNotes() : "";
            enrollment.setNotes(existingNotes.isEmpty() ? lateNote : existingNotes + " | " + lateNote);
            enrollment.setNote(enrollment.getNotes());
            enrollmentRepository.save(enrollment);

            return EnrollmentResponse.fromEntity(enrollment);
        }

        // 4. Nếu đơn PENDING: Tiến hành duyệt đơn an toàn với Invariant Guard
        enrollment = approveEnrollment(enrollment.getId());
        enrollment.setApprovalMethod("ONLINE_PAYMENT");
        if (request.getInvoiceId() != null) {
            enrollment.setInvoiceCode(request.getInvoiceId());
        }
        enrollment = enrollmentRepository.save(enrollment);

        // 5. Ghi nhận sự kiện thanh toán thành công để đối soát Idempotency
        ProcessedFinanceEvent event = new ProcessedFinanceEvent();
        event.setEnrollmentId(enrollment.getId());
        event.setFinanceEventId(request.getEventId());
        event.setPaidAt(request.getPaidAt() != null ? request.getPaidAt() : LocalDateTime.now());
        processedFinanceEventRepository.save(event);

        return EnrollmentResponse.fromEntity(enrollment);
    }

    /**
     * Gia hạn thời gian giữ chỗ 48h bởi Học vụ (TASK-ADM-44).
     */
    @Override
    @Transactional
    public EnrollmentResponse extendHold(Integer enrollmentId, ExtendHoldRequest request) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_ENROLLMENT_NOT_FOUND, "Không tìm thấy đơn đăng ký ID: " + enrollmentId));

        if (!"PENDING".equalsIgnoreCase(enrollment.getStatus())) {
            throw new BusinessException(ErrorCode.ADM_INVALID_STATE_TRANSITION,
                    "Chỉ có thể gia hạn cho đơn đăng ký đang ở trạng thái PENDING (hiện tại: " + enrollment.getStatus() + ")");
        }

        ClassEntity clazz = classRepository.findById(enrollment.getClassId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_CLASS_NOT_FOUND, "Không tìm thấy lớp học"));

        long approvedCount = enrollmentRepository.countByClassIdAndStatus(clazz.getId(), "APPROVED");
        if (approvedCount >= clazz.getMaxStudents()) {
            throw new BusinessException(ErrorCode.ADM_CLASS_FULL, "Lớp học đã đủ sĩ số học viên chính thức, không thể gia hạn giữ chỗ");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime base = (enrollment.getHoldExpiresAt() != null && enrollment.getHoldExpiresAt().isAfter(now))
                ? enrollment.getHoldExpiresAt()
                : now;

        LocalDateTime newHoldExpiresAt = base.plusHours(request.getExtendedHours());
        enrollment.setHoldExpiresAt(newHoldExpiresAt);
        enrollment.setHoldExtendedByUserId(SecurityUtils.getCurrentUserId());
        enrollment.setHoldExtensionReason(request.getReason());

        String staffName = SecurityUtils.getCurrentUserFullName() != null ? SecurityUtils.getCurrentUserFullName() : "Học vụ";
        String noteStr = String.format("[Gia hạn %dh bởi %s]: %s", request.getExtendedHours(), staffName, request.getReason());
        String existingNotes = enrollment.getNotes() != null ? enrollment.getNotes() : "";
        enrollment.setNotes(existingNotes.isEmpty() ? noteStr : existingNotes + " | " + noteStr);
        enrollment.setNote(enrollment.getNotes());

        enrollment = enrollmentRepository.save(enrollment);
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

        if ("CANCELLED".equalsIgnoreCase(enrollment.getStatus())) {
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

        return EnrollmentResponse.fromEntity(enrollment);
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
        return page.map(EnrollmentResponse::fromEntity);
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
