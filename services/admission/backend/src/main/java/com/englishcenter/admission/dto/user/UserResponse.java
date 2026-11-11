package com.englishcenter.admission.dto.user;

import com.englishcenter.admission.entity.User;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO dữ liệu trả về thông tin người dùng cho client (không chứa password_hash).
 */
public class UserResponse {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("email")
    private String email;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("role")
    private String role;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("gender")
    private String gender;

    @JsonProperty("date_of_birth")
    private LocalDate dateOfBirth;

    @JsonProperty("avatar_url")
    private String avatarUrl;

    @JsonProperty("must_change_password")
    private Boolean mustChangePassword;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    public UserResponse() {
    }

    /**
     * Phương thức nhà máy chuyển đổi từ thực thể User sang DTO UserResponse an toàn.
     *
     * @param user Thực thể người dùng
     * @return Đối tượng UserResponse
     */
    public static UserResponse from(User user) {
        if (user == null) {
            return null;
        }
        UserResponse res = new UserResponse();
        res.id = user.getId();
        res.email = user.getEmail();
        res.fullName = user.getFullName();
        res.role = user.getRole();
        res.phone = user.getPhone();
        res.gender = user.getGender();
        res.dateOfBirth = user.getDateOfBirth();
        res.avatarUrl = user.getAvatarUrl();
        res.mustChangePassword = user.getMustChangePassword();
        res.isActive = user.getIsActive();
        res.createdAt = user.getCreatedAt();
        res.updatedAt = user.getUpdatedAt();
        return res;
    }

    public Integer getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    public String getPhone() {
        return phone;
    }

    public String getGender() {
        return gender;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public Boolean getMustChangePassword() {
        return mustChangePassword;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
