package com.englishcenter.admission.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Dữ liệu phản hồi chi tiết về hồ sơ học viên được tạo từ Student Service.
 */
public class StudentProfileData {

    @JsonProperty("student_id")
    private Integer studentId;

    @JsonProperty("student_code")
    private String studentCode;

    @JsonProperty("status")
    private String status;

    public StudentProfileData() {
    }

    public StudentProfileData(Integer studentId, String studentCode, String status) {
        this.studentId = studentId;
        this.studentCode = studentCode;
        this.status = status;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
