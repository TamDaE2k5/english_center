package com.englishcenter.admission.service;

import com.englishcenter.admission.common.exception.BusinessException;
import com.englishcenter.admission.dto.enrollment.*;
import com.englishcenter.admission.entity.Enrollment;
import com.englishcenter.admission.security.SecurityUtils;
import org.springframework.stereotype.Service;

/** Ghi yêu cầu tiền mặt, không tự công nhận khoản thu. Finance vẫn là nguồn xác thực duy nhất. */
@Service
public class CashConfirmationService {
    private final CashConfirmationTransactions transactions;

    /** Nhận bean transaction riêng; facade không mở transaction bao ngoài audit từ chối. */
    public CashConfirmationService(CashConfirmationTransactions transactions) { this.transactions=transactions; }

    /** STAFF/ADMIN gửi yêu cầu; lỗi nghiệp vụ được audit riêng rồi trả lỗi gốc, không báo duyệt thành công. */
    public EnrollmentResponse request(Integer enrollmentId,ManualApproveRequest request) {
        Integer actor=SecurityUtils.getCurrentUserId();
        try {
            return transactions.store(enrollmentId,actor,request);
        } catch(BusinessException ex) {
            transactions.rejected(enrollmentId,actor,request,ex.getErrorCode().name());
            throw ex;
        }
    }

    /** Bổ sung trạng thái yêu cầu bền vững khi hiển thị đơn, không suy diễn PAID hoặc MANUAL_CASH. */
    public EnrollmentResponse response(Enrollment enrollment) { return transactions.response(enrollment); }
}
