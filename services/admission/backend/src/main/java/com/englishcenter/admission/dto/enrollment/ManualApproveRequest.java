package com.englishcenter.admission.dto.enrollment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * DTO yêu cầu duyệt đơn nộp tiền mặt hoặc thẻ POS tại quầy (FR-ENR-07, TASK-ADM-46).
 */
public class ManualApproveRequest {

    private String receiptNumber;

    private String paymentMethod = "CASH"; // CASH, POS_CARD, DIRECT_TRANSFER

    @NotNull(message = "Số tiền thực thu không được để trống")
    @Positive(message = "Số tiền thực thu phải lớn hơn 0")
    private BigDecimal amountReceived;

    private String notes;

    public ManualApproveRequest() {
    }

    public ManualApproveRequest(String receiptNumber, String paymentMethod, BigDecimal amountReceived, String notes) {
        this.receiptNumber = receiptNumber;
        this.paymentMethod = paymentMethod != null ? paymentMethod : "CASH";
        this.amountReceived = amountReceived;
        this.notes = notes;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getAmountReceived() {
        return amountReceived;
    }

    public void setAmountReceived(BigDecimal amountReceived) {
        this.amountReceived = amountReceived;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
