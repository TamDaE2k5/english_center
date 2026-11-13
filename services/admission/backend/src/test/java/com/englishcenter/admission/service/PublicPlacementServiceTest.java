package com.englishcenter.admission.service;

import com.englishcenter.admission.common.exception.*;
import com.englishcenter.admission.dto.test.*;
import com.englishcenter.admission.entity.*;
import com.englishcenter.admission.repository.*;
import com.englishcenter.admission.security.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Kiểm tra cả đường khách hợp lệ, tài khoản đăng nhập, token giả và token dùng sai lượt thi. */
class PublicPlacementServiceTest {
    private PlacementTestService engine;
    private TestAttemptRepository attempts;
    private UserService users;
    private UserRepository userRepository;
    private PlacementAttemptToken tokens;
    private PublicPlacementTestService service;
    @BeforeEach void setUp() {
        engine = mock(PlacementTestService.class); users = mock(UserService.class);
        attempts = mock(TestAttemptRepository.class);
        tokens = new PlacementAttemptToken("qa-only-secret-with-sufficient-entropy-12345");
        userRepository = mock(UserRepository.class);
        User verified = new User(); verified.setId(3); verified.setRole("STUDENT"); verified.setIsActive(true);
        verified.setEmailVerifiedAt(LocalDateTime.now()); verified.setMustChangePassword(false);
        when(userRepository.findById(3)).thenReturn(Optional.of(verified));
        when(attempts.findUserIdById(anyInt())).thenReturn(Optional.of(3));
        service = new PublicPlacementTestService(engine, users, attempts, tokens, userRepository);
    }
    @AfterEach void clearSecurity() { SecurityContextHolder.clearContext(); }

    /** Token đúng chỉ cho phép nộp đúng lượt thi, không cần JWT đăng nhập. */
    @Test void guestWithIssuedTokenCanSubmit() {
        SubmitTestRequest r = request(1); r.setAttemptToken(tokens.issue(1, LocalDateTime.now().plusMinutes(10)));
        service.submitAndGrade(r); verify(engine).submitAndGrade(r);
    }
    @Test void tokenCannotBeReusedForAnotherAttempt() {
        SubmitTestRequest r = request(2); r.setAttemptToken(tokens.issue(1, LocalDateTime.now().plusMinutes(10)));
        assertThrows(BusinessException.class, () -> service.submitAndGrade(r)); verifyNoInteractions(engine);
    }
    @Test void expiredTokenIsRejected() {
        SubmitTestRequest r = request(1); r.setAttemptToken(tokens.issue(1, LocalDateTime.now().minusSeconds(10)));
        assertThrows(BusinessException.class, () -> service.submitAndGrade(r)); verifyNoInteractions(engine);
    }
    @Test void forgedTokenIsRejected() {
        SubmitTestRequest r = request(1); r.setAttemptToken(new PlacementAttemptToken("different-key").issue(1, LocalDateTime.now().plusMinutes(10)));
        assertThrows(BusinessException.class, () -> service.submitAndGrade(r)); verifyNoInteractions(engine);
    }
    @Test void authenticatedOwnerCanSubmitWithoutGuestToken() {
        login(3);
        SubmitTestRequest r = request(1); service.submitAndGrade(r); verify(engine).submitAndGrade(r);
    }
    @Test void anotherAuthenticatedUserCannotSubmit() {
        login(4);
        assertThrows(BusinessException.class, () -> service.submitAndGrade(request(1))); verifyNoInteractions(engine);
    }
    @Test void newGuestMustVerifyAndLoginBeforeOfficialAttempt() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.startTest(2, new StartTestRequest(null, "new@example.test", "QA", null)));
        assertEquals(ErrorCode.AUTH_EMAIL_UNVERIFIED, error.getErrorCode());
        verifyNoInteractions(engine);
    }
    /** Principal của người đăng nhập quyết định danh tính, không lấy email tự khai từ request. */
    @Test void loggedInStartUsesPrincipal() {
        login(3);
        when(engine.startTest(eq(2), any())).thenReturn(new TakeTestResponse(1, 2, "QA", 10, LocalDateTime.now(), LocalDateTime.now().plusMinutes(10), List.of()));
        service.startTest(2, new StartTestRequest(null, "victim@example.test", "Other", null));
        verify(engine).startTest(eq(2), argThat(r -> r.getUserId().equals(3) && r.getEmail() == null));
        verifyNoInteractions(users);
    }
    private SubmitTestRequest request(int id) { return new SubmitTestRequest(id, 2, null, null, null, List.of(new SubmitAnswerItem(7, "A"))); }
    private void login(int id) {
        User u = new User(); u.setId(id); u.setRole("STUDENT"); u.setIsActive(true);
        UserPrincipal p = UserPrincipal.create(u);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(p, null, p.getAuthorities()));
    }

    /** Email đã có tài khoản không được nhận thành danh tính khách chỉ bằng cách tự khai. */
    @Test void guestCannotReuseExistingEmail() {
        UserRepository repository = mock(UserRepository.class);
        when(repository.existsByEmail("victim@example.test")).thenReturn(true);
        var accounts = new com.englishcenter.admission.service.impl.UserServiceImpl(repository,
                mock(RefreshTokenRepository.class), mock(AuthAuditLogRepository.class),
                mock(org.springframework.security.crypto.password.PasswordEncoder.class));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> accounts.createGuestStudentUser("Victim@example.test", "QA", null));
        assertEquals(ErrorCode.AUTH_FORBIDDEN, ex.getErrorCode());
        verify(repository, never()).saveAndFlush(any());
    }

    /** UNIQUE(email) vẫn chặn giả danh nếu tài khoản được tạo giữa bước kiểm tra và INSERT. */
    @Test void concurrentGuestCreationNeverFallsBackToExistingAccount() {
        UserRepository repository = mock(UserRepository.class);
        when(repository.saveAndFlush(any())).thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate"));
        var accounts = new com.englishcenter.admission.service.impl.UserServiceImpl(repository,
                mock(RefreshTokenRepository.class), mock(AuthAuditLogRepository.class),
                mock(org.springframework.security.crypto.password.PasswordEncoder.class));
        assertThrows(BusinessException.class, () -> accounts.createGuestStudentUser("race@example.test", "QA", null));
        verify(repository, never()).findByEmail(any());
    }
}
