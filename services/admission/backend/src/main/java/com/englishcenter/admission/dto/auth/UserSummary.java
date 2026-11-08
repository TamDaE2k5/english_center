package com.englishcenter.admission.dto.auth;

import com.englishcenter.admission.entity.User;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO tóm tắt thông tin người dùng trả về trong phản hồi xác thực đăng nhập.
 */
public class UserSummary {

    @JsonProperty("user_id")
    private Integer userId;

    @JsonProperty("email")
    private String email;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("role")
    private String role;

    @JsonProperty("must_change_password")
    private Boolean mustChangePassword;

    public UserSummary() {
    }

    public static UserSummary from(User user) {
        if (user == null) {
            return null;
        }
        UserSummary summary = new UserSummary();
        summary.userId = user.getId();
        summary.email = user.getEmail();
        summary.fullName = user.getFullName();
        summary.role = user.getRole();
        summary.mustChangePassword = user.getMustChangePassword();
        return summary;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(Boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }
}
