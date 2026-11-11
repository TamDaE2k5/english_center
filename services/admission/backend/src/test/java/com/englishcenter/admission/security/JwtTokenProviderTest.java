package com.englishcenter.admission.security;

import com.englishcenter.admission.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử thành phần bảo mật JwtTokenProvider & UserPrincipal")
class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "jwtSecret", "test_super_secret_jwt_key_at_least_32_bytes_long_2026");
        ReflectionTestUtils.setField(tokenProvider, "accessTokenExpirationSeconds", 900L);
        ReflectionTestUtils.setField(tokenProvider, "refreshTokenExpirationDays", 7L);
    }

    @Test
    @DisplayName("Tạo Access Token và trích xuất đúng thông tin claims")
    void testGenerateAccessTokenAndExtractClaims() {
        Integer userId = 101;
        String email = "student.test@englishcenter.edu.vn";
        String role = "STUDENT";
        String fullName = "Nguyen Van Test";

        String token = tokenProvider.generateAccessToken(userId, email, role, fullName);
        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));

        assertEquals(userId, tokenProvider.getUserIdFromToken(token));
        assertEquals(email, tokenProvider.getEmailFromToken(token));
        assertEquals(role, tokenProvider.getRoleFromToken(token));
        assertEquals(fullName, tokenProvider.getFullNameFromToken(token));
    }

    @Test
    @DisplayName("Tạo Refresh Token chứa đúng jti và type")
    void testGenerateRefreshToken() {
        Integer userId = 102;
        String jti = "custom-uuid-12345";

        String token = tokenProvider.generateRefreshToken(userId, jti);
        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));

        assertEquals(userId, tokenProvider.getUserIdFromToken(token));
        assertEquals(jti, tokenProvider.getJtiFromToken(token));
    }

    @Test
    @DisplayName("Token không hợp lệ hoặc bị sửa đổi phải bị từ chối")
    void testInvalidToken() {
        assertFalse(tokenProvider.validateToken("invalid.token.string"));
        assertFalse(tokenProvider.validateToken(""));
        assertFalse(tokenProvider.validateToken(null));

        String validToken = tokenProvider.generateAccessToken(1, "test@test.com", "ADMIN", "Admin User");
        String tamperedToken = validToken + "tamper";
        assertFalse(tokenProvider.validateToken(tamperedToken));
    }

    @Test
    @DisplayName("Khởi tạo UserPrincipal từ thực thể User đúng vai trò và quyền hạn")
    void testUserPrincipalCreation() {
        User user = new User();
        user.setId(5);
        user.setEmail("staff@english.edu.vn");
        user.setPasswordHash("hashed_pwd");
        user.setFullName("Nguyen Thi Hoc Vu");
        user.setRole("STAFF");
        user.setIsActive(true);

        UserPrincipal principal = UserPrincipal.create(user);
        assertEquals(5, principal.getId());
        assertEquals("staff@english.edu.vn", principal.getUsername());
        assertEquals("STAFF", principal.getRole());
        assertEquals("Nguyen Thi Hoc Vu", principal.getFullName());
        assertTrue(principal.isEnabled());
        assertTrue(principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STAFF")));
    }
}
