package com.englishcenter.admission.service.impl;

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
import com.englishcenter.admission.security.SecurityUtils;
import com.englishcenter.admission.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

/**
 * Lớp triển khai các nghiệp vụ quản lý tài khoản người dùng và định danh.
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthAuditLogRepository authAuditLogRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           AuthAuditLogRepository authAuditLogRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.authAuditLogRepository = authAuditLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Tự động tìm kiếm hoặc khởi tạo tài khoản học viên từ email.
     *
     * @param email    Địa chỉ email của học viên
     * @param fullName Họ và tên đầy đủ
     * @param phone    Số điện thoại liên lạc
     * @return Thực thể User đại diện cho học viên
     */
    @Override
    @Transactional
    public User getOrCreateStudentUser(String email, String fullName, String phone) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email không được để trống khi khởi tạo tài khoản học viên");
        }

        String normalizedEmail = email.trim().toLowerCase();
        Optional<User> existingUserOpt = userRepository.findByEmail(normalizedEmail);

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            log.info("Học viên với email {} đã tồn tại (ID={}), tiến hành tái sử dụng tài khoản", normalizedEmail, existingUser.getId());
            if ((existingUser.getPhone() == null || existingUser.getPhone().isEmpty()) && phone != null && !phone.isEmpty()) {
                existingUser.setPhone(phone.trim());
                userRepository.save(existingUser);
            }
            return existingUser;
        }

        String rawPassword = generateRandomPassword(10);
        String passwordHash = passwordEncoder.encode(rawPassword);

        User newUser = new User();
        newUser.setEmail(normalizedEmail);
        newUser.setFullName(fullName != null ? fullName.trim() : "Học Viên Mới");
        newUser.setPhone(phone != null ? phone.trim() : null);
        newUser.setPasswordHash(passwordHash);
        newUser.setRole("STUDENT");
        newUser.setIsActive(true);
        newUser.setMustChangePassword(true);

        User savedUser = userRepository.save(newUser);
        log.info("Đã tự động khởi tạo tài khoản học viên mới: ID={}, email={}, role=STUDENT", savedUser.getId(), savedUser.getEmail());

        return savedUser;
    }

    /**
     * Tạo tài khoản nhân sự mới (STAFF hoặc TEACHER) bởi Quản trị viên (ADMIN).
     *
     * @param request DTO chứa thông tin nhân viên mới
     * @return DTO UserResponse thông tin tài khoản vừa tạo
     */
    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException(ErrorCode.ADM_USER_EMAIL_EXISTS, "Địa chỉ email '" + normalizedEmail + "' đã tồn tại trong hệ thống");
        }

        String role = request.getRole().toUpperCase();
        if (!"STAFF".equals(role) && !"TEACHER".equals(role)) {
            throw new BusinessException(ErrorCode.ADM_VALIDATION_ERROR, "Chỉ được phép tạo tài khoản có vai trò STAFF hoặc TEACHER qua API này");
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordHash);
        user.setRole(role);
        user.setFullName(request.getFullName().trim());
        user.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        user.setGender(request.getGender());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setIsActive(true);
        user.setMustChangePassword(true);

        User saved = userRepository.save(user);

        // Ghi nhật ký kiểm toán hành động tạo người dùng
        AuthAuditLog auditLog = new AuthAuditLog();
        auditLog.setUserId(SecurityUtils.getCurrentUserId());
        auditLog.setEmail(normalizedEmail);
        auditLog.setAction("CREATE_USER");
        auditLog.setDetails("Tạo mới tài khoản nhân sự ID=" + saved.getId() + ", vai trò=" + role);
        authAuditLogRepository.save(auditLog);

        log.info("Quản trị viên đã tạo mới tài khoản nhân sự: ID={}, email={}, role={}", saved.getId(), saved.getEmail(), saved.getRole());
        return UserResponse.from(saved);
    }

    /**
     * Lấy chi tiết thông tin tài khoản người dùng theo ID (kiểm tra ranh giới bảo mật INV-SEC-01).
     *
     * @param id ID người dùng cần xem
     * @return DTO UserResponse
     */
    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Integer id) {
        checkUserAccessPermission(id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_USER_NOT_FOUND, "Không tìm thấy người dùng với ID: " + id));

        return UserResponse.from(user);
    }

    /**
     * Truy vấn danh sách tài khoản người dùng có phân trang và lọc theo vai trò, từ khóa.
     *
     * @param role    Vai trò người dùng (ADMIN, STAFF, TEACHER, STUDENT) hoặc null
     * @param keyword Từ khóa tìm kiếm theo email hoặc họ tên (hoặc null)
     * @param page    Số trang (0-based)
     * @param size    Số lượng bản ghi mỗi trang
     * @return PageResponse chứa danh sách UserResponse
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(String role, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        String searchRole = (role != null && !role.trim().isEmpty()) ? role.trim().toUpperCase() : null;
        String searchKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;

        Page<User> userPage = userRepository.searchUsers(searchRole, searchKeyword, pageable);
        List<UserResponse> dtoList = userPage.getContent().stream()
                .map(UserResponse::from)
                .toList();

        return PageResponse.of(dtoList, page, size, userPage.getTotalElements());
    }

    /**
     * Cập nhật thông tin hồ sơ người dùng (chỉ ADMIN hoặc chính chủ tài khoản mới được cập nhật).
     *
     * @param id      ID người dùng cần cập nhật
     * @param request DTO chứa thông tin cập nhật
     * @return DTO UserResponse sau cập nhật
     */
    @Override
    @Transactional
    public UserResponse updateUser(Integer id, UpdateUserRequest request) {
        checkUserAccessPermission(id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_USER_NOT_FOUND, "Không tìm thấy người dùng với ID: " + id));

        user.setFullName(request.getFullName().trim());
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }

        User updated = userRepository.save(user);
        log.info("Cập nhật thông tin người dùng ID={} thành công", id);
        return UserResponse.from(updated);
    }

    /**
     * Khóa hoặc mở khóa tài khoản người dùng (chỉ ADMIN).
     * Khi khóa tài khoản, lập tức thu hồi toàn bộ Refresh Token đang hoạt động và ghi nhật ký kiểm toán.
     *
     * @param id       ID người dùng cần thao tác
     * @param isActive Trạng thái mong muốn (true: mở khóa, false: khóa)
     * @param reason   Lý do khóa hoặc mở khóa
     * @return DTO UserResponse sau khi cập nhật trạng thái
     */
    @Override
    @Transactional
    public UserResponse toggleUserStatus(Integer id, boolean isActive, String reason) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_USER_NOT_FOUND, "Không tìm thấy người dùng với ID: " + id));

        user.setIsActive(isActive);
        User saved = userRepository.save(user);

        String action = isActive ? "ACCOUNT_UNLOCKED" : "ACCOUNT_LOCKED";

        // Nếu khóa tài khoản, thu hồi lập tức toàn bộ Refresh Token của tài khoản đó
        if (!isActive) {
            refreshTokenRepository.revokeAllByUserId(id);
            log.info("Đã thu hồi toàn bộ Refresh Token của người dùng ID={}", id);
        }

        // Ghi nhật ký kiểm toán
        AuthAuditLog auditLog = new AuthAuditLog();
        auditLog.setUserId(SecurityUtils.getCurrentUserId());
        auditLog.setEmail(user.getEmail());
        auditLog.setAction(action);
        auditLog.setDetails(reason != null ? reason : (isActive ? "Mở khóa tài khoản" : "Khóa tài khoản bởi Admin"));
        authAuditLogRepository.save(auditLog);

        log.info("Thao tác {} tài khoản ID={}, lý do: {}", action, id, reason);
        return UserResponse.from(saved);
    }

    /**
     * Kiểm tra ranh giới bảo mật INV-SEC-01: Học viên và Giáo viên chỉ được xem/sửa hồ sơ của chính mình.
     * Quản trị viên (ADMIN) có quyền xem và thao tác trên mọi tài khoản.
     *
     * @param targetUserId ID người dùng đích cần truy cập
     */
    private void checkUserAccessPermission(Integer targetUserId) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.hasRole("ADMIN");

        if (!isAdmin && (currentUserId == null || !currentUserId.equals(targetUserId))) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN, "Bạn không có quyền truy cập hồ sơ của người dùng khác");
        }
    }

    /**
     * Hàm tiện ích sinh chuỗi mật khẩu ngẫu nhiên an toàn.
     *
     * @param length Độ dài mật khẩu mong muốn
     * @return Chuỗi mật khẩu ngẫu nhiên
     */
    private String generateRandomPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }
}
