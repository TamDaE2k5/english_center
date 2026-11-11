package com.englishcenter.admission.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO yêu cầu đăng xuất và thu hồi Refresh Token hiện hành.
 */
public class LogoutRequest {

    @NotBlank(message = "Refresh token không được để trống")
    @JsonProperty("refresh_token")
    private String refreshToken;

    public LogoutRequest() {
    }

    public LogoutRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
