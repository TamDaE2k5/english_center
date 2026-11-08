package com.englishcenter.admission.service;

import com.englishcenter.admission.entity.User;
import com.englishcenter.admission.repository.UserRepository;
import com.englishcenter.admission.service.impl.UserServiceImpl;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử nghiệp vụ quản lý tài khoản người dùng UserService")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, passwordEncoder);
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

        // Không tạo mới mật khẩu và không gọi save tạo mới
        verify(passwordEncoder, never()).encode(anyString());
    }
}
