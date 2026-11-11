package com.englishcenter.admission.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO yêu cầu khởi tạo tài khoản học viên tự động khi nộp đơn hoặc làm bài test.
 */
public class CreateStudentAccountRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Định dạng email không hợp lệ")
    @JsonProperty("email")
    private String email;

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 255, message = "Họ và tên không được vượt quá 255 ký tự")
    @JsonProperty("full_name")
    private String fullName;

    @Size(max = 50, message = "Số điện thoại không được vượt quá 50 ký tự")
    @JsonProperty("phone")
    private String phone;

    public CreateStudentAccountRequest() {
    }

    public CreateStudentAccountRequest(String email, String fullName, String phone) {
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
