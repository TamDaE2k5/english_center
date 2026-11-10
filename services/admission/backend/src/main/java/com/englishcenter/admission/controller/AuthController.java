package com.englishcenter.admission.controller;

import com.englishcenter.admission.common.dto.ApiResponse;
import com.englishcenter.admission.dto.auth.ForgotPasswordRequest;
import com.englishcenter.admission.dto.auth.LoginRequest;
import com.englishcenter.admission.dto.auth.LoginResponse;
import com.englishcenter.admission.dto.auth.LogoutRequest;
import com.englishcenter.admission.dto.auth.RefreshTokenRequest;
import com.englishcenter.admission.dto.auth.RefreshTokenResponse;
import com.englishcenter.admission.dto.auth.ResetPasswordRequest;
import com.englishcenter.admission.service.AuthService;
import com.englishcenter.admission.service.AccountLifecycleService;
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
    private final AccountLifecycleService accounts;

    public AuthController(AuthService authService, AccountLifecycleService accounts) {
        this.authService = authService;
        this.accounts = accounts;
    }

    /** Đăng ký/gửi lại xác minh, không trả token hay mật khẩu trong response. */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterAccount request) {
        accounts.register(request.email(), request.fullName(), request.phone());
        return ResponseEntity.ok(ApiResponse.success(null, "Nếu phù hợp, hướng dẫn tài khoản đã được gửi qua email"));
    }

    /** Xác minh email một lần và trả setup token chỉ cho client cầm link email. */
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> verifyEmail(@Valid @RequestBody EmailToken request) {
        return ResponseEntity.ok(ApiResponse.success(java.util.Map.of("setup_token", accounts.verifyEmail(request.token()))));
    }

    /** Thiết lập mật khẩu đầu tiên sau bước xác minh email. */
    @PostMapping("/setup-password")
    public ResponseEntity<ApiResponse<Void>> setupPassword(@Valid @RequestBody ResetPasswordRequest request) {
        accounts.setupPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success(null, "Đã thiết lập mật khẩu. Vui lòng đăng nhập"));
    }

    /** Đổi mật khẩu chính chủ, bắt buộc xác thực và mật khẩu hiện tại. */
    @PostMapping("/change-password")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePassword request) {
        accounts.changePassword(request.currentPassword(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.success(null, "Đã đổi mật khẩu. Vui lòng đăng nhập lại"));
    }

    /** DTO không nhận role/is_active từ trình duyệt để tránh nâng quyền khi tự đăng ký. */
    public record RegisterAccount(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Email
        @jakarta.validation.constraints.Size(max=255) String email,
        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=255) String fullName,
        @jakarta.validation.constraints.Size(max=50) String phone) {}
    public record EmailToken(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=128) String token) {}
    public record ChangePassword(@jakarta.validation.constraints.NotBlank String currentPassword,
        @jakarta.validation.constraints.NotBlank String newPassword) {}

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
     * Yêu cầu gửi liên kết đặt lại mật khẩu qua email (thời hạn 15 phút).
     *
     * @param request     DTO chứa email
     * @param httpRequest Đối tượng HttpServletRequest
     * @return ApiResponse thông báo thành công chung
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        accounts.forgotPassword(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(
                null, "Nếu email tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu đã được gửi đến bạn"
        ));
    }

    /**
     * Thiết lập mật khẩu mới bằng mã token 15 phút.
     *
     * @param request     DTO chứa token và mật khẩu mới
     * @param httpRequest Đối tượng HttpServletRequest
     * @return ApiResponse thông báo thành công
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        accounts.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success(
                null, "Đặt lại mật khẩu thành công. Vui lòng đăng nhập bằng mật khẩu mới"
        ));
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
