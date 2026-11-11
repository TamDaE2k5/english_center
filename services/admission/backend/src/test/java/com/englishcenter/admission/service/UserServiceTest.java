package com.englishcenter.admission.service;

import com.englishcenter.admission.common.dto.PageResponse;
import com.englishcenter.admission.common.exception.BusinessException;
import com.englishcenter.admission.common.exception.ErrorCode;
import com.englishcenter.admission.dto.user.CreateUserRequest;
import com.englishcenter.admission.dto.user.UpdateUserRequest;
import com.englishcenter.admission.dto.user.UserResponse;
import com.englishcenter.admission.entity.AuthAuditLog;
import com.englishcenter.admission.entity.User;
import com.englishcenter.admission.repository.AuthAuditLogRepository;
import com.englishcenter.admission.repository.RefreshTokenRepository;
import com.englishcenter.admission.repository.UserRepository;
import com.englishcenter.admission.security.UserPrincipal;
import com.englishcenter.admission.service.impl.UserServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử nghiệp vụ quản lý tài khoản người dùng UserService")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private AuthAuditLogRepository authAuditLogRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, refreshTokenRepository, authAuditLogRepository, passwordEncoder);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityContext(Integer userId, String email, String role) {
        UserPrincipal principal = new UserPrincipal(
                userId, email, "pwd", "Test User", role, true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
        );
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Tự động tạo mới tài khoản học viên khi email chưa tồn tại")
    void testGetOrCreateStudentUser_WhenNotExists() {
        String email = "new.student@englishcenter.edu.vn";
        String fullName = "Tran Van New";
        String phone = "0912345678";

        when(userRepository.findByEmail(email.toLowerCase())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedPasswordMock");

        User savedMockUser = new User();
        savedMockUser.setId(99);
        savedMockUser.setEmail(email.toLowerCase());
        savedMockUser.setFullName(fullName);
        savedMockUser.setRole("STUDENT");
        savedMockUser.setPasswordHash("$2a$10$hashedPasswordMock");
        savedMockUser.setIsActive(true);

        when(userRepository.save(any(User.class))).thenReturn(savedMockUser);

        User result = userService.getOrCreateStudentUser(email, fullName, phone);

        assertNotNull(result);
        assertEquals(99, result.getId());
        assertEquals("STUDENT", result.getRole());
        assertEquals(email.toLowerCase(), result.getEmail());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertEquals("STUDENT", capturedUser.getRole());
        assertTrue(capturedUser.getMustChangePassword());
        assertTrue(capturedUser.getIsActive());
    }

    @Test
    @DisplayName("Tái sử dụng tài khoản hiện có khi email đã tồn tại trong hệ thống")
    void testGetOrCreateStudentUser_WhenAlreadyExists() {
        String email = "existing.student@englishcenter.edu.vn";
        User existing = new User();
        existing.setId(42);
        existing.setEmail(email);
        existing.setFullName("Existing Student");
        existing.setRole("STUDENT");
        existing.setPhone("0988888888");

        when(userRepository.findByEmail(email.toLowerCase())).thenReturn(Optional.of(existing));

        User result = userService.getOrCreateStudentUser(email, "New Name Ignored", "0988888888");

        assertNotNull(result);
        assertEquals(42, result.getId());
        assertEquals(email, result.getEmail());

        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Tạo tài khoản nhân viên thành công và ghi log kiểm toán")
    void testCreateUser_Success() {
        mockSecurityContext(1, "admin@englishcenter.edu.vn", "ADMIN");

        CreateUserRequest req = new CreateUserRequest();
        req.setEmail("teacher.lan@englishcenter.edu.vn");
        req.setPassword("Password@123");
        req.setRole("TEACHER");
        req.setFullName("Nguyen Thi Lan");
        req.setPhone("0987654321");
        req.setGender("FEMALE");
        req.setDateOfBirth(LocalDate.of(1992, 5, 15));

        when(userRepository.existsByEmail("teacher.lan@englishcenter.edu.vn")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("$2a$10$encodedPwd");

        User savedUser = new User();
        savedUser.setId(10);
        savedUser.setEmail(req.getEmail());
        savedUser.setFullName(req.getFullName());
        savedUser.setRole("TEACHER");
        savedUser.setIsActive(true);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse res = userService.createUser(req);

        assertNotNull(res);
        assertEquals(10, res.getId());
        assertEquals("TEACHER", res.getRole());
        verify(authAuditLogRepository).save(any(AuthAuditLog.class));
    }

    @Test
    @DisplayName("Tạo tài khoản thất bại khi email đã tồn tại (ADM_USER_EMAIL_EXISTS)")
    void testCreateUser_EmailExists() {
        CreateUserRequest req = new CreateUserRequest();
        req.setEmail("duplicate@englishcenter.edu.vn");
        req.setRole("STAFF");
        req.setPassword("password123");
        req.setFullName("Duplicate User");

        when(userRepository.existsByEmail("duplicate@englishcenter.edu.vn")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.createUser(req));
        assertEquals(ErrorCode.ADM_USER_EMAIL_EXISTS, ex.getErrorCode());
    }

    @Test
    @DisplayName("INV-SEC-01: Học viên không được phép xem hồ sơ của học viên khác")
    void testGetUserById_IsolationForbidden() {
        mockSecurityContext(10, "student10@englishcenter.edu.vn", "STUDENT");

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.getUserById(99));
        assertEquals(ErrorCode.AUTH_FORBIDDEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("Khóa tài khoản thành công sẽ thu hồi toàn bộ Refresh Token của user")
    void testToggleUserStatus_LockUser() {
        mockSecurityContext(1, "admin@englishcenter.edu.vn", "ADMIN");

        User user = new User();
        user.setId(20);
        user.setEmail("staff20@englishcenter.edu.vn");
        user.setIsActive(true);
        user.setRole("STAFF");

        when(userRepository.findById(20)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.toggleUserStatus(20, false, "Vi phạm quy định");

        assertNotNull(response);
        assertFalse(response.getIsActive());
        verify(refreshTokenRepository).revokeAllByUserId(20);
        verify(authAuditLogRepository).save(any(AuthAuditLog.class));
    }
}
