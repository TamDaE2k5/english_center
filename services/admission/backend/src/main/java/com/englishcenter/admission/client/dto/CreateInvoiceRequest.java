package com.englishcenter.admission.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload yêu cầu tạo hóa đơn học phí tự động gửi sang Finance Service.
 * Tuân thủ hợp đồng liên service tại Mục 9.1 docs/MASTER_DB_AND_API_RULES.md.
 */
public class CreateInvoiceRequest {

    @JsonProperty("enrollment_id")
    private Integer enrollmentId;

    @JsonProperty("student_id")
    private Integer studentId;

    @JsonProperty("course_id")
    private Integer courseId;

    @JsonProperty("voucher_code")
    private String voucherCode;

    public CreateInvoiceRequest() {
    }

    public CreateInvoiceRequest(Integer enrollmentId, Integer studentId, Integer courseId, String voucherCode) {
        this.enrollmentId = enrollmentId;
        this.studentId = studentId;
        this.courseId = courseId;
        this.voucherCode = voucherCode;
    }

    public Integer getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(Integer enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }
}
