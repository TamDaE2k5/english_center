package com.englishcenter.admission.service;

import com.englishcenter.admission.dto.auth.LoginRequest;
import com.englishcenter.admission.dto.auth.LoginResponse;
import com.englishcenter.admission.dto.auth.LogoutRequest;
import com.englishcenter.admission.dto.auth.RefreshTokenRequest;
import com.englishcenter.admission.dto.auth.RefreshTokenResponse;

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

    /**
     * Làm mới phiên làm việc bằng cơ chế Refresh Token Rotation.
     * Thu hồi token cũ, cấp phát cặp token mới. Nếu phát hiện token đã thu hồi bị dùng lại,
     * lập tức hủy bỏ toàn bộ phiên làm việc của người dùng để chống tấn công Replay Attack.
     *
     * @param request   DTO chứa chuỗi refresh token
     * @param ipAddress Địa chỉ IP của client
     * @param userAgent Thiết bị của client
     * @return DTO RefreshTokenResponse chứa cặp token mới
     */
    RefreshTokenResponse refreshToken(RefreshTokenRequest request, String ipAddress, String userAgent);

    /**
     * Đăng xuất và thu hồi Refresh Token đang sử dụng.
     *
     * @param request   DTO chứa refresh token cần thu hồi
     * @param ipAddress Địa chỉ IP
     * @param userAgent Thiết bị của client
     */
    void logout(LogoutRequest request, String ipAddress, String userAgent);

    /**
     * Yêu cầu đặt lại mật khẩu qua email (thời hạn token 15 phút).
     * Tránh tiết lộ thông tin tài khoản (Email Enumeration Prevention).
     *
     * @param request   DTO chứa email yêu cầu
     * @param ipAddress Địa chỉ IP của client
     * @param userAgent Thiết bị client
     */
    void forgotPassword(com.englishcenter.admission.dto.auth.ForgotPasswordRequest request, String ipAddress, String userAgent);

    /**
     * Thiết lập mật khẩu mới bằng token 15 phút.
     * Cập nhật mật khẩu băm, vô hiệu hóa token và hủy bỏ toàn bộ refresh token cũ để buộc đăng nhập lại.
     *
     * @param request   DTO chứa token và mật khẩu mới
     * @param ipAddress Địa chỉ IP
     * @param userAgent Thiết bị client
     */
    void resetPassword(com.englishcenter.admission.dto.auth.ResetPasswordRequest request, String ipAddress, String userAgent);
}
