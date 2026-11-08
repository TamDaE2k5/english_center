package com.englishcenter.admission.service.impl;

import com.englishcenter.admission.common.exception.BusinessException;
import com.englishcenter.admission.common.exception.ErrorCode;
import com.englishcenter.admission.dto.auth.LoginRequest;
import com.englishcenter.admission.dto.auth.LoginResponse;
import com.englishcenter.admission.dto.auth.UserSummary;
import com.englishcenter.admission.entity.AuthAuditLog;
import com.englishcenter.admission.entity.RefreshToken;
import com.englishcenter.admission.entity.User;
import com.englishcenter.admission.repository.AuthAuditLogRepository;
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
    private final AuthAuditLogRepository authAuditLogRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserRepository userRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           AuthAuditLogRepository authAuditLogRepository,
                           JwtTokenProvider jwtTokenProvider,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
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
