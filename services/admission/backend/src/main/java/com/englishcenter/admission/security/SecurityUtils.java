package com.englishcenter.admission.security;

import com.englishcenter.admission.common.exception.BusinessException;
import com.englishcenter.admission.common.exception.ErrorCode;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Lớp tiện ích cung cấp các hàm hỗ trợ truy xuất ngữ cảnh xác thực và thông tin người dùng hiện tại.
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // Ngăn chặn khởi tạo lớp tiện ích
    }

    /**
     * Lấy đối tượng Authentication hiện tại từ SecurityContextHolder.
     *
     * @return Optional chứa Authentication hoặc rỗng
     */
    public static Optional<Authentication> getAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return Optional.of(auth);
    }

    /**
     * Lấy thông tin chi tiết UserPrincipal của người dùng đang đăng nhập.
     *
     * @return Optional chứa UserPrincipal hoặc rỗng nếu chưa đăng nhập
     */
    public static Optional<UserPrincipal> getCurrentUserPrincipal() {
        return getAuthentication()
                .map(Authentication::getPrincipal)
                .filter(principal -> principal instanceof UserPrincipal)
                .map(principal -> (UserPrincipal) principal);
    }

    /**
     * Lấy ID của người dùng đang đăng nhập.
     *
     * @return ID người dùng dạng Integer hoặc null nếu chưa đăng nhập
     */
    public static Integer getCurrentUserId() {
        return getCurrentUserPrincipal()
                .map(UserPrincipal::getId)
                .orElse(null);
    }

    /**
     * Bắt buộc phải có người dùng đăng nhập, trả về ID hoặc ném ngoại lệ AUTH_UNAUTHORIZED.
     *
     * @return ID người dùng dạng Integer
     * @throws BusinessException nếu chưa đăng nhập
     */
    public static Integer requireCurrentUserId() {
        Integer userId = getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED, "Yêu cầu đăng nhập để thực hiện thao tác này");
        }
        return userId;
    }

    /**
     * Lấy email của người dùng đang đăng nhập.
     *
     * @return Địa chỉ email hoặc null
     */
    public static String getCurrentUserEmail() {
        return getCurrentUserPrincipal()
                .map(UserPrincipal::getEmail)
                .orElse(null);
    }

    /**
     * Lấy vai trò (role) của người dùng đang đăng nhập.
     *
     * @return Tên vai trò (ADMIN, STAFF, TEACHER, STUDENT) hoặc null
     */
    public static String getCurrentUserRole() {
        return getCurrentUserPrincipal()
                .map(UserPrincipal::getRole)
                .orElse(null);
    }

    /**
     * Lấy họ tên người dùng đang đăng nhập.
     *
     * @return Họ tên đầy đủ hoặc null
     */
    public static String getCurrentUserFullName() {
        return getCurrentUserPrincipal()
                .map(UserPrincipal::getFullName)
                .orElse(null);
    }

    /**
     * Kiểm tra xem người dùng hiện tại đã đăng nhập hay chưa.
     *
     * @return true nếu đã đăng nhập và phiên hợp lệ
     */
    public static boolean isAuthenticated() {
        return getAuthentication().isPresent();
    }

    /**
     * Kiểm tra xem người dùng hiện tại có vai trò chỉ định hay không.
     *
     * @param role Tên vai trò cần kiểm tra (ví dụ: "ADMIN", "STAFF")
     * @return true nếu có vai trò
     */
    public static boolean hasRole(String role) {
        if (role == null) {
            return false;
        }
        String targetRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return getAuthentication()
                .map(auth -> auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equalsIgnoreCase(targetRole)))
                .orElse(false);
    }
}
