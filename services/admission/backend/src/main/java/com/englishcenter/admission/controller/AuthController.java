package com.englishcenter.admission.controller;

import com.englishcenter.admission.common.dto.ApiResponse;
import com.englishcenter.admission.dto.auth.LoginRequest;
import com.englishcenter.admission.dto.auth.LoginResponse;
import com.englishcenter.admission.dto.auth.LogoutRequest;
import com.englishcenter.admission.dto.auth.RefreshTokenRequest;
import com.englishcenter.admission.dto.auth.RefreshTokenResponse;
import com.englishcenter.admission.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller xử lý các tác vụ xác thực danh tính, phiên làm việc và bảo mật tài khoản.
 * Định tuyến chính thức: /api/admission/auth
 */
@RestController
@RequestMapping({"/api/admission/auth", "/auth"})
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Xác thực đăng nhập người dùng và cấp phát bộ token kép (Access Token & Refresh Token).
     *
     * @param request     DTO chứa email và mật khẩu
     * @param httpRequest Đối tượng HttpServletRequest để trích xuất IP và User-Agent
     * @return ApiResponse chứa LoginResponse và thông tin người dùng
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        LoginResponse response = authService.login(request, ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.success(response, "Đăng nhập thành công"));
    }

    /**
     * Làm mới phiên làm việc với cơ chế Refresh Token Rotation (RTR).
     *
     * @param request     DTO chứa chuỗi refresh token
     * @param httpRequest Đối tượng HttpServletRequest
     * @return ApiResponse chứa RefreshTokenResponse với access token và refresh token mới
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        RefreshTokenResponse response = authService.refreshToken(request, ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.success(response, "Làm mới phiên làm việc thành công"));
    }

    /**
     * Đăng xuất và thu hồi Refresh Token hiện hành.
     *
     * @param request     DTO chứa refresh token
     * @param httpRequest Đối tượng HttpServletRequest
     * @return ApiResponse rỗng thông báo đăng xuất thành công
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody LogoutRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        authService.logout(request, ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.success(null, "Đăng xuất thành công"));
    }

    /**
     * Trích xuất địa chỉ IP thực của client hỗ trợ trường hợp chạy sau Reverse Proxy / API Gateway.
     *
     * @param request HTTP Servlet Request
     * @return Địa chỉ IP thực
     */
    private String extractClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
