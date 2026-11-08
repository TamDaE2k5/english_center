package com.englishcenter.admission.service.impl;

import com.englishcenter.admission.common.exception.BusinessException;
import com.englishcenter.admission.common.exception.ErrorCode;
import com.englishcenter.admission.dto.auth.LoginRequest;
import com.englishcenter.admission.dto.auth.LoginResponse;
import com.englishcenter.admission.dto.auth.LogoutRequest;
import com.englishcenter.admission.dto.auth.RefreshTokenRequest;
import com.englishcenter.admission.dto.auth.RefreshTokenResponse;
import com.englishcenter.admission.dto.auth.UserSummary;
import com.englishcenter.admission.entity.AuthAuditLog;
import com.englishcenter.admission.entity.PasswordResetToken;
import com.englishcenter.admission.entity.RefreshToken;
import com.englishcenter.admission.entity.User;
import com.englishcenter.admission.repository.AuthAuditLogRepository;
import com.englishcenter.admission.repository.PasswordResetTokenRepository;
import com.englishcenter.admission.repository.RefreshTokenRepository;
import com.englishcenter.admission.repository.UserRepository;
import com.englishcenter.admission.security.JwtTokenProvider;
import com.englishcenter.admission.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Lớp triển khai các nghiệp vụ xác thực đăng nhập, cấp phát JWT và kiểm toán an toàn.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AuthAuditLogRepository authAuditLogRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserRepository userRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           PasswordResetTokenRepository passwordResetTokenRepository,
                           AuthAuditLogRepository authAuditLogRepository,
                           JwtTokenProvider jwtTokenProvider,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.authAuditLogRepository = authAuditLogRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Xác thực đăng nhập người dùng và cấp phát bộ token kép.
     * Quy trình xử lý từng bước:
     * 1. Chuẩn hóa email, tìm kiếm người dùng trong cơ sở dữ liệu.
     * 2. Nếu người dùng không tồn tại hoặc sai mật khẩu:
     *    - Ghi nhận nhật ký kiểm toán hành vi thất bại (LOGIN_FAILED).
     *    - Ném ngoại lệ AUTH_INVALID_CREDENTIALS (HTTP 401).
     * 3. Nếu tài khoản bị khóa (is_active = false):
     *    - Ghi nhận nhật ký kiểm toán hành vi bị chặn (LOGIN_BLOCKED).
     *    - Ném ngoại lệ AUTH_ACCOUNT_LOCKED (HTTP 403).
     * 4. Khi thông tin đăng nhập hoàn toàn chính xác:
     *    - Cấp phát Access Token (hạn 15 phút).
     *    - Cấp phát Refresh Token (hạn 7 ngày) và lưu vào bảng admission.refresh_tokens.
     *    - Ghi nhận nhật ký kiểm toán đăng nhập thành công (LOGIN_SUCCESS).
     *    - Đóng gói dữ liệu trả về cho client.
     *
     * @param request   DTO chứa thông tin email và mật khẩu
     * @param ipAddress Địa chỉ IP của client
     * @param userAgent Chuỗi nhận diện trình duyệt/thiết bị
     * @return DTO LoginResponse
     */
    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);

        // 1. Kiểm tra tài khoản tồn tại và khớp mật khẩu BCrypt
        if (userOpt.isEmpty() || !passwordEncoder.matches(request.getPassword(), userOpt.get().getPasswordHash())) {
            recordAuditLog(userOpt.map(User::getId).orElse(null), normalizedEmail, "LOGIN_FAILED",
                    ipAddress, userAgent, "Đăng nhập thất bại: Sai email hoặc mật khẩu");
            log.warn("Đăng nhập thất bại cho email {}: sai thông tin xác thực từ IP {}", normalizedEmail, ipAddress);
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS, "Email hoặc mật khẩu không chính xác");
        }

        User user = userOpt.get();

        // 2. Kiểm tra tài khoản có đang bị khóa hay không
        if (user.getIsActive() != null && !user.getIsActive()) {
            recordAuditLog(user.getId(), normalizedEmail, "LOGIN_BLOCKED",
                    ipAddress, userAgent, "Đăng nhập bị từ chối: Tài khoản đang bị khóa");
            log.warn("Đăng nhập bị chặn cho email {}: tài khoản đã bị vô hiệu hóa", normalizedEmail);
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_LOCKED, "Tài khoản của bạn đã bị khóa, vui lòng liên hệ quản trị viên");
        }

        // 3. Cấp phát bộ token kép (Dual JWT)
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole(), user.getFullName()
        );

        String jti = UUID.randomUUID().toString();
        String refreshTokenStr = jwtTokenProvider.generateRefreshToken(user.getId(), jti);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(user.getId());
        refreshToken.setToken(refreshTokenStr);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(jwtTokenProvider.getRefreshTokenExpirationDays()));
        refreshToken.setIsRevoked(false);
        refreshTokenRepository.save(refreshToken);

        // 4. Ghi nhận nhật ký kiểm toán đăng nhập thành công
        recordAuditLog(user.getId(), normalizedEmail, "LOGIN_SUCCESS",
                ipAddress, userAgent, "Đăng nhập thành công với vai trò " + user.getRole());

        log.info("Người dùng ID={} ({}) đăng nhập thành công từ IP {}", user.getId(), user.getEmail(), ipAddress);

        return new LoginResponse(
                accessToken,
                refreshTokenStr,
                jwtTokenProvider.getAccessTokenExpirationSeconds(),
                UserSummary.from(user)
        );
    }

    /**
     * Làm mới phiên làm việc an toàn với cơ chế Refresh Token Rotation (RTR).
     * Quy trình xử lý từng bước:
     * 1. Tra cứu refresh token trong cơ sở dữ liệu.
     * 2. Nếu token không tồn tại -> Báo lỗi AUTH_TOKEN_INVALID.
     * 3. Phát hiện Replay Attack: Nếu token đã từng bị thu hồi (is_revoked = true) mà lại được gửi lên:
     *    - Kích hoạt cơ chế phòng vệ tự động: Thu hồi toàn bộ token còn lại của user_id đó.
     *    - Ghi nhận nhật ký cảnh báo an ninh SECURITY_ALERT_REPLAY_ATTACK.
     *    - Ném lỗi AUTH_TOKEN_INVALID.
     * 4. Kiểm tra hạn sử dụng: Nếu expires_at < NOW() -> Đánh dấu thu hồi và ném lỗi AUTH_TOKEN_EXPIRED.
     * 5. Kiểm tra tài khoản người dùng có đang hoạt động hay không.
     * 6. Thực hiện xoay vòng token (Rotation):
     *    - Đánh dấu token hiện tại thành is_revoked = true.
     *    - Cấp phát Access Token mới (15 phút) và Refresh Token mới (7 ngày).
     *    - Lưu Refresh Token mới vào database.
     *    - Ghi nhận nhật ký REFRESH_TOKEN và trả về cặp token mới.
     *
     * @param request   DTO chứa refresh token
     * @param ipAddress Địa chỉ IP của client
     * @param userAgent Thiết bị của client
     * @return DTO RefreshTokenResponse
     */
    @Override
    @Transactional
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request, String ipAddress, String userAgent) {
        String tokenStr = request.getRefreshToken().trim();
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByToken(tokenStr);

        // 1. Kiểm tra sự tồn tại của token
        if (tokenOpt.isEmpty()) {
            recordAuditLog(null, null, "REFRESH_FAILED", ipAddress, userAgent, "Refresh token không tồn tại trong hệ thống");
            throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID, "Phiên làm việc không hợp lệ hoặc đã bị thu hồi");
        }

        RefreshToken tokenEntity = tokenOpt.get();

        // 2. Phát hiện tái sử dụng token đã thu hồi (Replay Attack Fraud Detection)
        if (Boolean.TRUE.equals(tokenEntity.getIsRevoked())) {
            log.error("CẢNH BÁO BẢO MẬT: Phát hiện tái sử dụng Refresh Token đã bị thu hồi của user ID={} từ IP {}",
                    tokenEntity.getUserId(), ipAddress);

            // Phòng vệ khẩn cấp: Hủy toàn bộ phiên đang hoạt động của người dùng này
            refreshTokenRepository.revokeAllByUserId(tokenEntity.getUserId());

            recordAuditLog(tokenEntity.getUserId(), null, "SECURITY_ALERT_REPLAY_ATTACK",
                    ipAddress, userAgent, "Phát hiện tái sử dụng token đã thu hồi. Toàn bộ phiên làm việc của user đã bị vô hiệu hóa.");

            throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID, "Phiên làm việc đã bị thu hồi hoặc phát hiện bất thường bảo mật");
        }

        // 3. Kiểm tra hạn sử dụng của token
        if (tokenEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            tokenEntity.setIsRevoked(true);
            refreshTokenRepository.save(tokenEntity);
            recordAuditLog(tokenEntity.getUserId(), null, "REFRESH_EXPIRED",
                    ipAddress, userAgent, "Refresh token đã hết hạn");
            throw new BusinessException(ErrorCode.AUTH_TOKEN_EXPIRED, "Phiên làm việc đã hết hạn, vui lòng đăng nhập lại");
        }

        // 4. Kiểm tra tài khoản người dùng
        User user = userRepository.findById(tokenEntity.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_USER_NOT_FOUND, "Không tìm thấy thông tin tài khoản người dùng"));

        if (user.getIsActive() != null && !user.getIsActive()) {
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_LOCKED, "Tài khoản của bạn đã bị khóa, không thể làm mới phiên");
        }

        // 5. Thực hiện Refresh Token Rotation (Thu hồi token cũ, cấp token mới)
        tokenEntity.setIsRevoked(true);
        refreshTokenRepository.save(tokenEntity);

        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole(), user.getFullName()
        );

        String newJti = UUID.randomUUID().toString();
        String newRefreshTokenStr = jwtTokenProvider.generateRefreshToken(user.getId(), newJti);

        RefreshToken newTokenEntity = new RefreshToken();
        newTokenEntity.setUserId(user.getId());
        newTokenEntity.setToken(newRefreshTokenStr);
        newTokenEntity.setExpiresAt(LocalDateTime.now().plusDays(jwtTokenProvider.getRefreshTokenExpirationDays()));
        newTokenEntity.setIsRevoked(false);
        refreshTokenRepository.save(newTokenEntity);

        recordAuditLog(user.getId(), user.getEmail(), "REFRESH_TOKEN",
                ipAddress, userAgent, "Làm mới phiên làm việc thành công");

        log.info("Làm mới token thành công cho user ID={} qua cơ chế Rotation", user.getId());

        return new RefreshTokenResponse(
                newAccessToken,
                newRefreshTokenStr,
                jwtTokenProvider.getAccessTokenExpirationSeconds()
        );
    }

    /**
     * Thu hồi Refresh Token khi người dùng chủ động đăng xuất.
     *
     * @param request   DTO chứa refresh token cần thu hồi
     * @param ipAddress Địa chỉ IP
     * @param userAgent Thiết bị client
     */
    @Override
    @Transactional
    public void logout(LogoutRequest request, String ipAddress, String userAgent) {
        String tokenStr = request.getRefreshToken().trim();
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByToken(tokenStr);

        if (tokenOpt.isPresent()) {
            RefreshToken tokenEntity = tokenOpt.get();
            tokenEntity.setIsRevoked(true);
            refreshTokenRepository.save(tokenEntity);

            recordAuditLog(tokenEntity.getUserId(), null, "LOGOUT",
                    ipAddress, userAgent, "Đăng xuất và thu hồi Refresh Token thành công");

            log.info("Người dùng ID={} đã đăng xuất và thu hồi refresh token", tokenEntity.getUserId());
        }
    }

    /**
     * Quy trình yêu cầu cấp lại mật khẩu qua email.
     * Áp dụng nguyên tắc Email Enumeration Prevention: Luôn trả về thành công dù email có tồn tại hay không.
     * Nếu email tồn tại, sinh token 15 phút lưu vào password_reset_tokens và phát tín hiệu gửi mail.
     *
     * @param request   DTO chứa địa chỉ email
     * @param ipAddress Địa chỉ IP
     * @param userAgent Thiết bị client
     */
    @Override
    @Transactional
    public void forgotPassword(com.englishcenter.admission.dto.auth.ForgotPasswordRequest request, String ipAddress, String userAgent) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Vô hiệu hóa mọi token đặt lại mật khẩu chưa sử dụng trước đó
            passwordResetTokenRepository.invalidateAllByUserId(user.getId());

            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUserId(user.getId());
            resetToken.setToken(token);
            resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(15));
            resetToken.setIsUsed(false);
            passwordResetTokenRepository.save(resetToken);

            recordAuditLog(user.getId(), normalizedEmail, "FORGOT_PASSWORD_REQUEST",
                    ipAddress, userAgent, "Đã tạo mã đặt lại mật khẩu có thời hạn 15 phút");

            log.info("Khởi tạo token đặt lại mật khẩu 15 phút cho email {}: token={}", normalizedEmail, token);
        } else {
            recordAuditLog(null, normalizedEmail, "FORGOT_PASSWORD_NONEXISTENT",
                    ipAddress, userAgent, "Yêu cầu đặt lại mật khẩu cho email không tồn tại trong hệ thống");
            log.info("Yêu cầu quên mật khẩu cho email không tồn tại: {}", normalizedEmail);
        }
    }

    /**
     * Thiết lập mật khẩu mới bằng token 15 phút.
     * Quy trình xử lý:
     * 1. Kiểm tra tính hợp lệ của token trong database (phải tồn tại, chưa dùng, chưa hết hạn).
     * 2. Băm mật khẩu mới bằng thuật toán BCrypt và lưu vào User.
     * 3. Đánh dấu token đã được sử dụng (is_used = true).
     * 4. Thu hồi toàn bộ Refresh Token của user đó để buộc đăng nhập lại trên mọi thiết bị.
     * 5. Ghi nhật ký kiểm toán PASSWORD_CHANGED.
     *
     * @param request   DTO chứa token và mật khẩu mới
     * @param ipAddress Địa chỉ IP
     * @param userAgent Thiết bị client
     */
    @Override
    @Transactional
    public void resetPassword(com.englishcenter.admission.dto.auth.ResetPasswordRequest request, String ipAddress, String userAgent) {
        String tokenStr = request.getToken().trim();
        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(tokenStr);

        if (tokenOpt.isEmpty() || Boolean.TRUE.equals(tokenOpt.get().getIsUsed())) {
            recordAuditLog(null, null, "RESET_PASSWORD_FAILED", ipAddress, userAgent, "Token đặt lại mật khẩu không hợp lệ hoặc đã qua sử dụng");
            throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID, "Mã token đặt lại mật khẩu không hợp lệ hoặc đã được sử dụng");
        }

        PasswordResetToken resetToken = tokenOpt.get();

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            resetToken.setIsUsed(true);
            passwordResetTokenRepository.save(resetToken);
            recordAuditLog(resetToken.getUserId(), null, "RESET_PASSWORD_EXPIRED", ipAddress, userAgent, "Token đặt lại mật khẩu đã hết hạn 15 phút");
            throw new BusinessException(ErrorCode.AUTH_TOKEN_EXPIRED, "Mã token đặt lại mật khẩu đã hết hạn 15 phút, vui lòng gửi lại yêu cầu");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_USER_NOT_FOUND, "Không tìm thấy người dùng sở hữu token"));

        // Cập nhật mật khẩu mới băm BCrypt
        String passwordHash = passwordEncoder.encode(request.getNewPassword());
        user.setPasswordHash(passwordHash);
        user.setMustChangePassword(false);
        userRepository.save(user);

        // Đánh dấu token đã sử dụng
        resetToken.setIsUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // Thu hồi toàn bộ phiên đăng nhập cũ để bảo đảm an toàn
        refreshTokenRepository.revokeAllByUserId(user.getId());

        recordAuditLog(user.getId(), user.getEmail(), "PASSWORD_CHANGED",
                ipAddress, userAgent, "Đặt lại mật khẩu thành công qua token 15 phút và hủy bỏ mọi phiên cũ");

        log.info("Đặt lại mật khẩu thành công cho người dùng ID={}", user.getId());
    }

    /**
     * Hàm tiện ích ghi nhận bản ghi nhật ký kiểm toán an toàn thông tin.
     *
     * @param userId    ID người dùng (có thể null nếu tài khoản chưa tồn tại)
     * @param email     Địa chỉ email
     * @param action    Hành động kiểm toán (LOGIN_SUCCESS, LOGIN_FAILED, ...)
     * @param ipAddress Địa chỉ IP
     * @param userAgent Thiết bị/trình duyệt
     * @param details   Thông tin chi tiết
     */
    private void recordAuditLog(Integer userId, String email, String action,
                                String ipAddress, String userAgent, String details) {
        try {
            AuthAuditLog logEntry = new AuthAuditLog();
            logEntry.setUserId(userId);
            logEntry.setEmail(email);
            logEntry.setAction(action);
            logEntry.setIpAddress(ipAddress);
            logEntry.setUserAgent(userAgent);
            logEntry.setDetails(details);
            authAuditLogRepository.save(logEntry);
        } catch (Exception ex) {
            log.error("Không thể ghi nhật ký kiểm toán: {}", ex.getMessage());
        }
    }
}
