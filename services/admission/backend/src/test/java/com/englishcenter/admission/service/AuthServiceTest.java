package com.englishcenter.admission.service;

import com.englishcenter.admission.common.exception.BusinessException;
import com.englishcenter.admission.common.exception.ErrorCode;
import com.englishcenter.admission.dto.auth.LoginRequest;
import com.englishcenter.admission.dto.auth.LoginResponse;
import com.englishcenter.admission.entity.AuthAuditLog;
import com.englishcenter.admission.entity.RefreshToken;
import com.englishcenter.admission.entity.User;
import com.englishcenter.admission.repository.AuthAuditLogRepository;
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
    private AuthAuditLogRepository authAuditLogRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository, refreshTokenRepository, authAuditLogRepository, jwtTokenProvider, passwordEncoder
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
}
