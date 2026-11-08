package com.englishcenter.admission.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

/**
 * DTO yêu cầu khóa hoặc mở khóa tài khoản người dùng.
 */
public class ToggleActiveRequest {

    @NotNull(message = "Trạng thái is_active không được để trống")
    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("reason")
    private String reason;

    public ToggleActiveRequest() {
    }

    public ToggleActiveRequest(Boolean isActive, String reason) {
        this.isActive = isActive;
        this.reason = reason;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
