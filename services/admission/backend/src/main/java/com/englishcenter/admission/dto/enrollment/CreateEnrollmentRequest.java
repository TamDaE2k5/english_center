package com.englishcenter.admission.dto.enrollment;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO yêu cầu nộp đơn tuyển sinh nhập học (FR-ENR-01, TASK-ADM-40).
 */
public class CreateEnrollmentRequest {

    @NotNull(message = "ID lớp học không được để trống")
    private Integer classId;

    @NotBlank(message = "Họ và tên thí sinh không được để trống")
    @Size(max = 255, message = "Họ và tên không vượt quá 255 ký tự")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Định dạng email không hợp lệ")
    private String email;

    @Size(max = 50, message = "Số điện thoại không vượt quá 50 ký tự")
    private String phone;

    @JsonAlias({"note"})
    private String notes;

    private String source = "ONLINE"; // ONLINE, WALK_IN, HOTLINE, FANPAGE, REFERRAL

    @JsonAlias({"channel"})
    private String enrollmentChannel = "ONLINE"; // ONLINE, AT_COUNTER

    public CreateEnrollmentRequest() {
    }

    public CreateEnrollmentRequest(Integer classId, String fullName, String email, String phone, String notes) {
        this.classId = classId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.notes = notes;
        this.source = "ONLINE";
        this.enrollmentChannel = "ONLINE";
    }

    public Integer getClassId() {
        return classId;
    }

    public void setClassId(Integer classId) {
        this.classId = classId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getEnrollmentChannel() {
        return enrollmentChannel;
    }

    public void setEnrollmentChannel(String enrollmentChannel) {
        this.enrollmentChannel = enrollmentChannel;
    }
}
