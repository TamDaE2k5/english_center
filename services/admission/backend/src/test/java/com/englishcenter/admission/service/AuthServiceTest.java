package com.englishcenter.admission.service;

import com.englishcenter.admission.common.exception.BusinessException;
import com.englishcenter.admission.common.exception.ErrorCode;
import com.englishcenter.admission.dto.auth.LoginRequest;
import com.englishcenter.admission.dto.auth.LoginResponse;
import com.englishcenter.admission.entity.AuthAuditLog;
import com.englishcenter.admission.entity.PasswordResetToken;
import com.englishcenter.admission.entity.RefreshToken;
import com.englishcenter.admission.entity.User;
import com.englishcenter.admission.repository.AuthAuditLogRepository;
import com.englishcenter.admission.repository.PasswordResetTokenRepository;
import com.englishcenter.admission.repository.RefreshTokenRepository;
import com.englishcenter.admission.repository.UserRepository;
import com.englishcenter.admission.security.JwtTokenProvider;
import com.englishcenter.admission.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử nghiệp vụ xác thực đăng nhập AuthService")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private AuthAuditLogRepository authAuditLogRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository, refreshTokenRepository, passwordResetTokenRepository,
                authAuditLogRepository, jwtTokenProvider, passwordEncoder
        );
    }

    @Test
    @DisplayName("Đăng nhập thành công trả về bộ token kép và ghi nhận nhật ký LOGIN_SUCCESS")
    void testLogin_Success() {
        LoginRequest req = new LoginRequest("admin@englishcenter.edu.vn", "Password@123");

        User user = new User();
        user.setId(1);
        user.setEmail(req.getEmail());
        user.setPasswordHash("$2a$10$encodedPassword");
        user.setFullName("Nguyen Van Admin");
        user.setRole("ADMIN");
        user.setIsActive(true);
        user.setMustChangePassword(false);

        when(userRepository.findByEmail(req.getEmail().toLowerCase())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password@123", "$2a$10$encodedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(1, req.getEmail(), "ADMIN", "Nguyen Van Admin"))
                .thenReturn("mock_access_token_jwt");
        when(jwtTokenProvider.generateRefreshToken(eq(1), anyString()))
                .thenReturn("mock_refresh_token_jwt");
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(jwtTokenProvider.getRefreshTokenExpirationDays()).thenReturn(7L);

        LoginResponse response = authService.login(req, "192.168.1.100", "Mozilla/5.0");

        assertNotNull(response);
        assertEquals("mock_access_token_jwt", response.getAccessToken());
        assertEquals("mock_refresh_token_jwt", response.getRefreshToken());
        assertEquals(1, response.getUser().getUserId());
        assertEquals("ADMIN", response.getUser().getRole());

        // Kiểm tra lưu refresh token
        verify(refreshTokenRepository).save(any(RefreshToken.class));

        // Kiểm tra ghi log kiểm toán LOGIN_SUCCESS
        ArgumentCaptor<AuthAuditLog> logCaptor = ArgumentCaptor.forClass(AuthAuditLog.class);
        verify(authAuditLogRepository).save(logCaptor.capture());
        AuthAuditLog savedLog = logCaptor.getValue();
        assertEquals("LOGIN_SUCCESS", savedLog.getAction());
        assertEquals(1, savedLog.getUserId());
        assertEquals("192.168.1.100", savedLog.getIpAddress());
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi sai mật khẩu ném lỗi AUTH_INVALID_CREDENTIALS và ghi log LOGIN_FAILED")
    void testLogin_WrongPassword() {
        LoginRequest req = new LoginRequest("admin@englishcenter.edu.vn", "WrongPassword");

        User user = new User();
        user.setId(1);
        user.setEmail(req.getEmail());
        user.setPasswordHash("$2a$10$encodedPassword");

        when(userRepository.findByEmail(req.getEmail().toLowerCase())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "$2a$10$encodedPassword")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                authService.login(req, "10.0.0.1", "PostmanRuntime")
        );

        assertEquals(ErrorCode.AUTH_INVALID_CREDENTIALS, ex.getErrorCode());

        ArgumentCaptor<AuthAuditLog> logCaptor = ArgumentCaptor.forClass(AuthAuditLog.class);
        verify(authAuditLogRepository).save(logCaptor.capture());
        AuthAuditLog savedLog = logCaptor.getValue();
        assertEquals("LOGIN_FAILED", savedLog.getAction());
        assertEquals(1, savedLog.getUserId());
    }

    @Test
    @DisplayName("Đăng nhập bị chặn khi tài khoản bị khóa ném lỗi AUTH_ACCOUNT_LOCKED và ghi log LOGIN_BLOCKED")
    void testLogin_AccountLocked() {
        LoginRequest req = new LoginRequest("locked@englishcenter.edu.vn", "Password@123");

        User user = new User();
        user.setId(88);
        user.setEmail(req.getEmail());
        user.setPasswordHash("$2a$10$encodedPassword");
        user.setIsActive(false);

        when(userRepository.findByEmail(req.getEmail().toLowerCase())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password@123", "$2a$10$encodedPassword")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                authService.login(req, "10.0.0.1", "PostmanRuntime")
        );

        assertEquals(ErrorCode.AUTH_ACCOUNT_LOCKED, ex.getErrorCode());

        ArgumentCaptor<AuthAuditLog> logCaptor = ArgumentCaptor.forClass(AuthAuditLog.class);
        verify(authAuditLogRepository).save(logCaptor.capture());
        assertEquals("LOGIN_BLOCKED", logCaptor.getValue().getAction());
    }

    @Test
    @DisplayName("Làm mới token thành công với cơ chế Rotation thu hồi token cũ và sinh token mới")
    void testRefreshToken_Success() {
        String oldTokenStr = "valid_old_refresh_token";
        com.englishcenter.admission.dto.auth.RefreshTokenRequest req =
                new com.englishcenter.admission.dto.auth.RefreshTokenRequest(oldTokenStr);

        RefreshToken oldToken = new RefreshToken();
        oldToken.setId(10);
        oldToken.setUserId(5);
        oldToken.setToken(oldTokenStr);
        oldToken.setExpiresAt(java.time.LocalDateTime.now().plusDays(5));
        oldToken.setIsRevoked(false);

        User user = new User();
        user.setId(5);
        user.setEmail("user5@englishcenter.edu.vn");
        user.setRole("STUDENT");
        user.setFullName("Student Five");
        user.setIsActive(true);

        when(refreshTokenRepository.findByToken(oldTokenStr)).thenReturn(Optional.of(oldToken));
        when(userRepository.findById(5)).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(5, "user5@englishcenter.edu.vn", "STUDENT", "Student Five"))
                .thenReturn("new_access_token_jwt");
        when(jwtTokenProvider.generateRefreshToken(eq(5), anyString()))
                .thenReturn("new_refresh_token_jwt");
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(jwtTokenProvider.getRefreshTokenExpirationDays()).thenReturn(7L);

        com.englishcenter.admission.dto.auth.RefreshTokenResponse res =
                authService.refreshToken(req, "127.0.0.1", "TestClient");

        assertNotNull(res);
        assertEquals("new_access_token_jwt", res.getAccessToken());
        assertEquals("new_refresh_token_jwt", res.getRefreshToken());

        // Token cũ phải bị đánh dấu thu hồi
        assertTrue(oldToken.getIsRevoked());

        // Token mới được lưu
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Phát hiện Replay Attack: Dùng token đã thu hồi sẽ kích hoạt hủy toàn bộ phiên của user")
    void testRefreshToken_ReplayAttackDetection() {
        String compromisedToken = "revoked_refresh_token";
        com.englishcenter.admission.dto.auth.RefreshTokenRequest req =
                new com.englishcenter.admission.dto.auth.RefreshTokenRequest(compromisedToken);

        RefreshToken oldToken = new RefreshToken();
        oldToken.setId(15);
        oldToken.setUserId(8);
        oldToken.setToken(compromisedToken);
        oldToken.setExpiresAt(java.time.LocalDateTime.now().plusDays(2));
        oldToken.setIsRevoked(true); // Đã bị thu hồi từ trước

        when(refreshTokenRepository.findByToken(compromisedToken)).thenReturn(Optional.of(oldToken));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                authService.refreshToken(req, "1.2.3.4", "HackerAgent")
        );

        assertEquals(ErrorCode.AUTH_TOKEN_INVALID, ex.getErrorCode());

        // Phòng vệ an ninh: Thu hồi toàn bộ token của user 8
        verify(refreshTokenRepository).revokeAllByUserId(8);

        // Ghi nhật ký cảnh báo an ninh
        ArgumentCaptor<AuthAuditLog> logCaptor = ArgumentCaptor.forClass(AuthAuditLog.class);
        verify(authAuditLogRepository).save(logCaptor.capture());
        assertEquals("SECURITY_ALERT_REPLAY_ATTACK", logCaptor.getValue().getAction());
    }

    @Test
    @DisplayName("Đăng xuất thành công sẽ thu hồi refresh token")
    void testLogout() {
        String tokenStr = "active_token_to_logout";
        com.englishcenter.admission.dto.auth.LogoutRequest req =
                new com.englishcenter.admission.dto.auth.LogoutRequest(tokenStr);

        RefreshToken token = new RefreshToken();
        token.setId(25);
        token.setUserId(9);
        token.setToken(tokenStr);
        token.setIsRevoked(false);

        when(refreshTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(token));

        authService.logout(req, "127.0.0.1", "Browser");

        assertTrue(token.getIsRevoked());
        verify(refreshTokenRepository).save(token);
        verify(authAuditLogRepository).save(any(AuthAuditLog.class));
    }

    @Test
    @DisplayName("Yêu cầu quên mật khẩu thành công sinh token 15 phút khi email tồn tại")
    void testForgotPassword_WhenEmailExists() {
        String email = "student@englishcenter.edu.vn";
        com.englishcenter.admission.dto.auth.ForgotPasswordRequest req =
                new com.englishcenter.admission.dto.auth.ForgotPasswordRequest(email);

        User user = new User();
        user.setId(7);
        user.setEmail(email);

        when(userRepository.findByEmail(email.toLowerCase())).thenReturn(Optional.of(user));

        authService.forgotPassword(req, "127.0.0.1", "Browser");

        // Vô hiệu hóa token cũ và lưu token mới
        verify(passwordResetTokenRepository).invalidateAllByUserId(7);
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));

        ArgumentCaptor<AuthAuditLog> logCaptor = ArgumentCaptor.forClass(AuthAuditLog.class);
        verify(authAuditLogRepository).save(logCaptor.capture());
        assertEquals("FORGOT_PASSWORD_REQUEST", logCaptor.getValue().getAction());
    }

    @Test
    @DisplayName("Đặt lại mật khẩu thành công bằng token 15 phút và thu hồi mọi session cũ")
    void testResetPassword_Success() {
        String tokenStr = "valid-reset-token-uuid";
        com.englishcenter.admission.dto.auth.ResetPasswordRequest req =
                new com.englishcenter.admission.dto.auth.ResetPasswordRequest(tokenStr, "NewSecretPassword@123");

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setId(30);
        resetToken.setUserId(12);
        resetToken.setToken(tokenStr);
        resetToken.setExpiresAt(java.time.LocalDateTime.now().plusMinutes(10));
        resetToken.setIsUsed(false);

        User user = new User();
        user.setId(12);
        user.setEmail("user12@englishcenter.edu.vn");
        user.setPasswordHash("old_hash");

        when(passwordResetTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(resetToken));
        when(userRepository.findById(12)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewSecretPassword@123")).thenReturn("$2a$10$newHashedPassword");

        authService.resetPassword(req, "127.0.0.1", "Browser");

        // Token phải được đánh dấu đã sử dụng
        assertTrue(resetToken.getIsUsed());
        verify(passwordResetTokenRepository).save(resetToken);

        // Mật khẩu user được cập nhật
        assertEquals("$2a$10$newHashedPassword", user.getPasswordHash());
        verify(userRepository).save(user);

        // Toàn bộ refresh token cũ bị thu hồi
        verify(refreshTokenRepository).revokeAllByUserId(12);

        // Ghi log PASSWORD_CHANGED
        ArgumentCaptor<AuthAuditLog> logCaptor = ArgumentCaptor.forClass(AuthAuditLog.class);
        verify(authAuditLogRepository).save(logCaptor.capture());
        assertEquals("PASSWORD_CHANGED", logCaptor.getValue().getAction());
    }
}
