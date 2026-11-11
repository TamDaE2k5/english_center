package com.englishcenter.admission.dto.enrollment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Digits;
import com.fasterxml.jackson.annotation.JsonAlias;

import java.math.BigDecimal;

/**
 * Yêu cầu đối soát tiền mặt theo FR-ENR-07 mới; không phải bằng chứng Finance đã thu tiền.
 */
public class ManualApproveRequest {

    @NotBlank(message = "Cần mã chứng từ để chống ghi nhận trùng")
    @Size(max = 80)
    private String receiptNumber;

    @NotNull @Pattern(regexp = "CASH", message = "Luồng này chỉ tiếp nhận tiền mặt CASH")
    private String paymentMethod = "CASH";

    @NotNull(message = "Số tiền thực thu không được để trống")
    @Positive(message = "Số tiền thực thu phải lớn hơn 0")
    @Digits(integer = 12, fraction = 2)
    @JsonAlias("cash_amount")
    private BigDecimal amountReceived;

    @Size(max = 2000)
    @JsonAlias("note")
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
