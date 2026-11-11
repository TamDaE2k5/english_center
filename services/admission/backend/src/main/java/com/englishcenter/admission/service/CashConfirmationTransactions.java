package com.englishcenter.admission.service;

import com.englishcenter.admission.common.exception.*;
import com.englishcenter.admission.dto.enrollment.*;
import com.englishcenter.admission.entity.Enrollment;
import com.englishcenter.admission.repository.*;
import com.englishcenter.admission.security.SecurityUtils;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

/** Giao dịch ghi yêu cầu và audit; tách bean để Spring áp dụng transaction qua proxy. */
@Service
public class CashConfirmationTransactions {
    private final EnrollmentRepository enrollments;
    private final ClassRepository classes;
    private final CashConfirmationRepository requests;
    private final Validator validator;

    /** Nhận các repository cùng datasource Admission và bộ kiểm tra DTO. */
    public CashConfirmationTransactions(EnrollmentRepository enrollments, ClassRepository classes,
            CashConfirmationRepository requests, Validator validator) {
        this.enrollments=enrollments; this.classes=classes; this.requests=requests; this.validator=validator;
    }

    /** Khóa lớp rồi refresh đơn như cascade; kiểm tra xong mới ghi yêu cầu và audit nguyên tử. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EnrollmentResponse store(Integer enrollmentId,Integer actor,ManualApproveRequest request) {
        if(actor==null) throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED);
        if(!SecurityUtils.hasRole("STAFF")&&!SecurityUtils.hasRole("ADMIN")) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        if(request==null||!validator.validate(request).isEmpty()) throw new BusinessException(ErrorCode.ADM_VALIDATION_ERROR);
        Enrollment e=enrollments.findById(enrollmentId).orElseThrow(()->new BusinessException(ErrorCode.ADM_ENROLLMENT_NOT_FOUND));
        var clazz=classes.findByIdWithLock(e.getClassId()).orElseThrow(()->new BusinessException(ErrorCode.ADM_CLASS_NOT_FOUND));
        enrollments.refreshForMutation(e);
        if(!"OPEN".equals(clazz.getStatus())) throw new BusinessException(ErrorCode.ADM_CLASS_NOT_OPEN);
        if(!"PENDING".equals(e.getStatus())) throw new BusinessException(ErrorCode.ADM_INVALID_STATE_TRANSITION);
        if(e.getHoldExpiresAt()==null||!e.getHoldExpiresAt().isAfter(LocalDateTime.now())) throw new BusinessException(ErrorCode.ADM_HOLD_EXPIRED);
        if(e.getInvoiceCode()==null||e.getInvoiceCode().isBlank())
            throw new BusinessException(ErrorCode.ADM_VALIDATION_ERROR,"Chưa có hóa đơn Finance để đối soát");
        if(enrollments.countByClassIdAndStatus(e.getClassId(),"APPROVED")>=clazz.getMaxStudents())
            throw new BusinessException(ErrorCode.ADM_CLASS_FULL);
        var existing=requests.find(enrollmentId,e.getInvoiceCode());
        String outcome;
        if(existing.isPresent()) {
            var old=existing.get();
            if(!old.receiptNumber().equals(request.getReceiptNumber().trim())||old.amount().compareTo(request.getAmountReceived())!=0)
                throw new BusinessException(ErrorCode.ADM_CASH_CONFLICT);
            outcome="REPLAYED";
        } else {
            if(!requests.insert(enrollmentId,e.getInvoiceCode(),actor,request)) throw new BusinessException(ErrorCode.ADM_CASH_CONFLICT);
            outcome="REQUESTED";
        }
        requests.audit(enrollmentId,actor,request,outcome,"NEEDS_FINANCE_CONFIRMATION");
        return response(e);
    }

    /** Bổ sung trạng thái yêu cầu bền vững khi hiển thị đơn, không suy diễn PAID hoặc MANUAL_CASH. */
    @Transactional(readOnly = true)
    public EnrollmentResponse response(Enrollment enrollment) {
        var response=EnrollmentResponse.fromEntity(enrollment);
        requests.find(enrollment.getId(),enrollment.getInvoiceCode()).ifPresent(response::setCashConfirmation);
        return response;
    }

    /** Audit từ chối commit riêng sau khi transaction nghiệp vụ rollback; lỗi ghi audit không bị nuốt. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rejected(Integer enrollmentId,Integer actor,ManualApproveRequest request,String reason) {
        requests.audit(enrollmentId,actor,request,"REJECTED",reason);
    }
}
