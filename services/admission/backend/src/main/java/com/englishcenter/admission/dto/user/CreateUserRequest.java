package com.englishcenter.admission.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTO yêu cầu tạo tài khoản nhân sự (STAFF hoặc TEACHER) bởi Quản trị viên.
 */
public class CreateUserRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Định dạng email không hợp lệ")
    @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
    @JsonProperty("email")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, message = "Mật khẩu phải có độ dài tối thiểu 8 ký tự")
    @JsonProperty("password")
    private String password;

    @NotBlank(message = "Vai trò không được để trống")
    @Pattern(regexp = "^(STAFF|TEACHER)$", message = "Vai trò chỉ được phép là STAFF hoặc TEACHER")
    @JsonProperty("role")
    private String role;

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 255, message = "Họ và tên không được vượt quá 255 ký tự")
    @JsonProperty("full_name")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Size(max = 50, message = "Số điện thoại không được vượt quá 50 ký tự")
    @JsonProperty("phone")
    private String phone;

    @NotBlank(message = "Giới tính không được để trống")
    @Pattern(regexp = "^(MALE|FEMALE|OTHER)$", message = "Giới tính phải là MALE, FEMALE hoặc OTHER")
    @JsonProperty("gender")
    private String gender;

    @NotNull(message = "Ngày sinh không được để trống")
    @JsonProperty("date_of_birth")
    private LocalDate dateOfBirth;

    public CreateUserRequest() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }
}
