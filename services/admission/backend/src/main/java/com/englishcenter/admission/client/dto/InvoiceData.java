package com.englishcenter.admission.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * Dữ liệu phản hồi chi tiết về hóa đơn được sinh từ Finance Service.
 */
public class InvoiceData {

    @JsonProperty("invoice_id")
    private Integer invoiceId;

    @JsonProperty("invoice_code")
    private String invoiceCode;

    @JsonProperty("status")
    private String status;

    @JsonProperty("amount")
    private BigDecimal amount;

    public InvoiceData() {
    }

    public InvoiceData(Integer invoiceId, String invoiceCode, String status, BigDecimal amount) {
        this.invoiceId = invoiceId;
        this.invoiceCode = invoiceCode;
        this.status = status;
        this.amount = amount;
    }

    public Integer getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Integer invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getInvoiceCode() {
        return invoiceCode;
    }

    public void setInvoiceCode(String invoiceCode) {
        this.invoiceCode = invoiceCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
