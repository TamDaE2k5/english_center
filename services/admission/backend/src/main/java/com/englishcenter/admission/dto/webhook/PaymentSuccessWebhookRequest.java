package com.englishcenter.admission.dto.webhook;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO nhận thông báo webhook thanh toán thành công từ Finance Service (FR-ENR-06, TASK-ADM-45).
 * Hỗ trợ nhận diện enrollment_id ở cả dạng số nguyên lẫn chuỗi có tiền tố (ví dụ: ENR-2026-0089).
 */
public class PaymentSuccessWebhookRequest {

    @NotBlank(message = "event_id không được để trống")
    private String eventId;

    @NotNull(message = "enrollment_id không được để trống")
    private Object enrollmentId;

    @JsonAlias({"invoice_code"})
    private String invoiceId;

    private BigDecimal amountPaid;

    private LocalDateTime paidAt;

    private String transactionRef;

    public PaymentSuccessWebhookRequest() {
    }

    public PaymentSuccessWebhookRequest(String eventId, Object enrollmentId, String invoiceId,
                                       BigDecimal amountPaid, LocalDateTime paidAt, String transactionRef) {
        this.eventId = eventId;
        this.enrollmentId = enrollmentId;
        this.invoiceId = invoiceId;
        this.amountPaid = amountPaid;
        this.paidAt = paidAt;
        this.transactionRef = transactionRef;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public Object getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(Object enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    /**
     * Bóc tách enrollmentId dạng số nguyên từ dữ liệu đầu vào (hỗ trợ cả int và chuỗi prefix).
     *
     * @return Integer ID của đơn đăng ký
     */
    public Integer parseNumericEnrollmentId() {
        if (enrollmentId == null) {
            return null;
        }
        if (enrollmentId instanceof Number number) {
            return number.intValue();
        }
        String str = enrollmentId.toString().trim();
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            // Trường hợp có định dạng ENR-yyyy-XXXX hoặc ENR-XXXX
            String clean = str.replaceAll("[^0-9]", " ");
            String[] tokens = clean.trim().split("\\s+");
            if (tokens.length > 0) {
                String lastToken = tokens[tokens.length - 1];
                if (!lastToken.isEmpty()) {
                    return Integer.parseInt(lastToken);
                }
            }
            throw new IllegalArgumentException("Không thể nhận diện mã đơn tuyển sinh từ: " + str);
        }
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public void setTransactionRef(String transactionRef) {
        this.transactionRef = transactionRef;
    }
}
