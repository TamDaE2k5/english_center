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

    @JsonAlias({"status"})
    private String invoiceStatus;

    public String getInvoiceStatus() { return invoiceStatus; }
    public void setInvoiceStatus(String value) { invoiceStatus = value; }

    /** Canonical evidence để replay phải giữ nguyên binding, amount, thời gian và chứng từ. */
    public String fingerprint() {
        try {
            String[] values = {eventId, String.valueOf(parseNumericEnrollmentId()), invoiceId,
                    amountPaid.stripTrailingZeros().toPlainString(), paidAt.toString(),
                    transactionRef == null ? "" : transactionRef, invoiceStatus == null ? "PAID" : invoiceStatus};
            StringBuilder canonical = new StringBuilder();
            for (String value : values) canonical.append(value.length()).append(':').append(value);
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

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
        String value = enrollmentId.toString();
        if (!value.matches("(?:[1-9][0-9]*|ENR-(?:[0-9]{4}-)?[0-9]+)")) {
            throw new IllegalArgumentException("Mã đơn không hợp lệ");
        }
        String last = value.substring(value.lastIndexOf('-') + 1);
        int id = Integer.parseInt(last);
        if (id <= 0) throw new IllegalArgumentException("Mã đơn phải dương");
        return id;
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
