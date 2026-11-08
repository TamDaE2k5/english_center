package com.englishcenter.admission.service;

import com.englishcenter.admission.dto.auth.LoginRequest;
import com.englishcenter.admission.dto.auth.LoginResponse;

/**
 * Giao diện định nghĩa các nghiệp vụ xác thực đăng nhập, quản lý phiên và bảo mật tài khoản.
 */
public interface AuthService {

    /**
     * Xác thực thông tin đăng nhập và cấp phát bộ token kép (Access Token & Refresh Token).
     * Ghi nhận nhật ký kiểm toán LOGIN_SUCCESS hoặc LOGIN_FAILED.
     *
     * @param request   DTO chứa email và mật khẩu
     * @param ipAddress Địa chỉ IP của client gửi yêu cầu
     * @param userAgent Thông tin trình duyệt/thiết bị của client
     * @return DTO LoginResponse chứa access token, refresh token và thông tin người dùng
     */
    LoginResponse login(LoginRequest request, String ipAddress, String userAgent);
}
