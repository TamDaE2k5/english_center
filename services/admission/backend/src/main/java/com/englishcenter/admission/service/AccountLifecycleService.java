package com.englishcenter.admission.service;

import com.englishcenter.admission.common.exception.*;
import com.englishcenter.admission.entity.*;
import com.englishcenter.admission.repository.*;
import com.englishcenter.admission.security.SecurityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDateTime;
import java.util.*;

/** Token email một lần, phân mục đích; DB chỉ lưu SHA-256, không lưu token gốc. */
@Service
public class AccountLifecycleService {
    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final RefreshTokenRepository refresh;
    private final AuthAuditLogRepository audits;
    private final PasswordEncoder passwords;
    private final AccountMailService mail;
    private final SecureRandom random = new SecureRandom();

    /** Nhận các cổng dữ liệu/SMTP của Admission, không gọi phân hệ khác. */
    public AccountLifecycleService(UserRepository users, PasswordResetTokenRepository tokens,
            RefreshTokenRepository refresh, AuthAuditLogRepository audits, PasswordEncoder passwords, AccountMailService mail) {
        this.users = users; this.tokens = tokens; this.refresh = refresh; this.audits = audits;
        this.passwords = passwords; this.mail = mail;
    }

    /** Tạo STUDENT chưa xác minh hoặc resend, không thay đổi tài khoản đã xác minh. */
    @Transactional
    public void register(String email, String fullName, String phone) {
        mail.requireAvailable();
        User user = users.findByEmail(email.trim().toLowerCase(Locale.ROOT)).orElse(null);
        if (user == null) {
            users.insertGuestIfAbsent(email.trim().toLowerCase(Locale.ROOT), passwords.encode(randomToken()), fullName.trim(), phone);
            user = users.findByEmail(email.trim().toLowerCase(Locale.ROOT)).orElseThrow(() -> new BusinessException(ErrorCode.AUTH_TOKEN_INVALID));
        }
        user = lockUser(user.getId());
        if (!"STUDENT".equals(user.getRole()) || user.getEmailVerifiedAt() != null || !Boolean.TRUE.equals(user.getIsActive())) return;
        // Resend trong 60 giây nhận cùng phản hồi chung, không phát thêm mail.
        if (recentlySent(user)) return;
        tokens.invalidateAllByUserId(user.getId());
        mail.send(user.getEmail(), "verify", issue(user.getId(), "VERIFY")); audit(user, "EMAIL_VERIFICATION_SENT");
    }

    /** Tiêu thụ VERIFY, xác minh email rồi cấp SETUP ngắn hạn cho trình duyệt chính chủ. */
    @Transactional
    public String verifyEmail(String raw) {
        PasswordResetToken token = lockValid(raw, "VERIFY"); User user = lockUser(token.getUserId());
        requireActive(user); token.setIsUsed(true); tokens.save(token);
        user.setEmailVerifiedAt(LocalDateTime.now()); users.save(user); audit(user, "EMAIL_VERIFIED");
        return issue(user.getId(), "SETUP");
    }

    /** Thiết lập mật khẩu đầu tiên; không dùng được VERIFY/RESET thay cho SETUP. */
    @Transactional public void setupPassword(String raw, String password) { completePassword(raw, password, "SETUP"); }

    /** Email chưa xác minh nhận VERIFY, đã xác minh nhận RESET; không tiết lộ tài khoản tồn tại. */
    @Transactional
    public void forgotPassword(String email) {
        mail.requireAvailable();
        User found = users.findByEmail(email.trim().toLowerCase(Locale.ROOT)).orElse(null);
        if (found == null) return;
        User user = lockUser(found.getId());
        if (!Boolean.TRUE.equals(user.getIsActive()) || recentlySent(user)) return;
        tokens.invalidateAllByUserId(user.getId());
        boolean verify = "STUDENT".equals(user.getRole()) && user.getEmailVerifiedAt() == null;
        mail.send(user.getEmail(), verify ? "verify" : "reset", issue(user.getId(), verify ? "VERIFY" : "RESET"));
        audit(user, "PASSWORD_HELP_REQUESTED");
    }

    /** Đặt lại mật khẩu bằng RESET một lần, thu hồi mọi refresh token cũ. */
    @Transactional public void resetPassword(String raw, String password) { completePassword(raw, password, "RESET"); }

    /** Đổi mật khẩu chính chủ, yêu cầu mật khẩu hiện tại và thu hồi phiên cũ. */
    @Transactional
    public void changePassword(String current, String next) {
        validatePassword(next); User user = lockUser(SecurityUtils.requireCurrentUserId()); requireActive(user);
        if (!passwords.matches(current, user.getPasswordHash())) throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        if (user.requiresFirstPasswordChange() && current.equals(next)) {
            throw new BusinessException(ErrorCode.ADM_VALIDATION_ERROR, "Mật khẩu mới phải khác mật khẩu hiện tại");
        }
        user.setPasswordHash(passwords.encode(next)); user.setMustChangePassword(false); users.save(user);
        tokens.invalidateAllByUserId(user.getId()); refresh.revokeAllByUserId(user.getId()); audit(user, "PASSWORD_CHANGED");
    }

    /** Áp dụng mật khẩu và vô hiệu mọi link/refresh trong cùng transaction. */
    private void completePassword(String raw, String password, String purpose) {
        validatePassword(password); PasswordResetToken token = lockValid(raw, purpose); User user = lockUser(token.getUserId());
        requireActive(user);
        if ("STUDENT".equals(user.getRole()) && user.getEmailVerifiedAt() == null) throw new BusinessException(ErrorCode.AUTH_EMAIL_UNVERIFIED);
        user.setPasswordHash(passwords.encode(password));
        // Khôi phục qua email chưa được BA xác nhận là hoàn tất đổi mật khẩu lần đầu của nhân sự.
        if (!user.requiresFirstPasswordChange()) user.setMustChangePassword(false);
        users.save(user);
        tokens.invalidateAllByUserId(user.getId()); token.setIsUsed(true); tokens.save(token);
        refresh.revokeAllByUserId(user.getId()); audit(user, "PASSWORD_CHANGED");
    }

    /** Khóa user trước token, refresh sau chờ khóa để không dùng trạng thái isUsed cũ. */
    private PasswordResetToken lockValid(String raw, String purpose) {
        String hash = hash(purpose, raw);
        PasswordResetToken initial = tokens.findByToken(hash).orElseThrow(() -> new BusinessException(ErrorCode.AUTH_TOKEN_INVALID));
        lockUser(initial.getUserId());
        PasswordResetToken token = tokens.findByTokenForUpdate(hash).orElseThrow(() -> new BusinessException(ErrorCode.AUTH_TOKEN_INVALID));
        tokens.refreshForUse(token);
        if (Boolean.TRUE.equals(token.getIsUsed())) throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID);
        if (!token.getExpiresAt().isAfter(LocalDateTime.now())) throw new BusinessException(ErrorCode.AUTH_TOKEN_EXPIRED);
        return token;
    }

    /** Cấp 256 bit entropy, lưu hash có tiền tố purpose vào hai cột tương thích hiện có. */
    private String issue(Integer userId, String purpose) {
        String raw = randomToken(); PasswordResetToken token = new PasswordResetToken();
        token.setUserId(userId); token.setToken(hash(purpose, raw)); token.setTokenHash(token.getToken());
        token.setExpiresAt(LocalDateTime.now().plusMinutes(15)); token.setIsUsed(false); tokens.saveAndFlush(token); return raw;
    }
    /** Sinh token URL-safe, không tạo mật khẩu mặc định hoặc xuất token vào log. */
    private String randomToken() { byte[] bytes = new byte[32]; random.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    /** Hash token gốc; input sai chỉ trả lỗi cố định không chứa giá trị đầu vào. */
    private String hash(String purpose, String raw) {
        if (raw == null || raw.length() > 128 || raw.isBlank()) throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID);
        try { return purpose + ":" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 unavailable"); }
    }
    /** Chính sách mật khẩu không vượt giới hạn BCrypt 72 byte UTF-8. */
    private void validatePassword(String value) {
        if (value == null || value.length() < 10 || value.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new BusinessException(ErrorCode.ADM_VALIDATION_ERROR, "Mật khẩu cần tối thiểu 10 ký tự và không quá 72 byte UTF-8");
    }
    private User lockUser(Integer id) {
        User user = users.findByIdForUpdate(id).orElseThrow(() -> new BusinessException(ErrorCode.AUTH_TOKEN_INVALID));
        users.refreshForAccountUse(user); return user;
    }
    private boolean recentlySent(User user) { return tokens.existsByUserIdAndCreatedAtAfter(user.getId(), LocalDateTime.now().minusSeconds(60)); }
    /** Link email không có quyền mở khóa tài khoản do Admin khóa. */
    private void requireActive(User user) { if (!Boolean.TRUE.equals(user.getIsActive())) throw new BusinessException(ErrorCode.AUTH_ACCOUNT_LOCKED); }
    /** Audit chỉ chứa user ID/hành động, không có token/mật khẩu/link. */
    private void audit(User user, String action) { AuthAuditLog a = new AuthAuditLog(); a.setUserId(user.getId()); a.setAction(action); a.setDetails("Thao tác tài khoản Admission"); audits.save(a); }
}
