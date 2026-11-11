package com.englishcenter.admission.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

/**
 * Payload yêu cầu tạo hồ sơ học viên chính thức gửi sang Student Service.
 * Tuân thủ hợp đồng liên service tại Mục 9.2 docs/MASTER_DB_AND_API_RULES.md.
 */
public class CreateStudentProfileRequest {

    @JsonProperty("user_id")
    private Integer userId;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("email")
    private String email;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("date_of_birth")
    private LocalDate dateOfBirth;

    public CreateStudentProfileRequest() {
    }

    public CreateStudentProfileRequest(Integer userId, String fullName, String email, String phone, LocalDate dateOfBirth) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.dateOfBirth = dateOfBirth;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
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

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }
}
