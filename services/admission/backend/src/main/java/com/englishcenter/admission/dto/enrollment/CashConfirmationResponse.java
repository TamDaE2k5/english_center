package com.englishcenter.admission.dto.enrollment;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Yêu cầu nội bộ Admission: người/thời điểm gửi không có nghĩa Finance đã xác nhận khoản thu. */
public record CashConfirmationResponse(Long id, String status, String invoiceCode, String receiptNumber,
        BigDecimal amount, Integer requestedBy, OffsetDateTime requestedAt) { }
