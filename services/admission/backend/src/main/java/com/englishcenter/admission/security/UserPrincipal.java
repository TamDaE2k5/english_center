package com.englishcenter.admission.security;

import com.englishcenter.admission.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Đối tượng đại diện cho thông tin định danh của người dùng trong hệ thống Spring Security.
 * Chuyển đổi từ thực thể User sang UserDetails để phục vụ xác thực và phân quyền.
 */
public class UserPrincipal implements UserDetails {

    private final Integer id;
    private final String email;
    private final String password;
    private final String fullName;
    private final String role;
    private final boolean isActive;
    private final boolean firstPasswordChangeRequired;
    private final Collection<? extends GrantedAuthority> authorities;

    /**
     * Khởi tạo đối tượng UserPrincipal với đầy đủ thông tin định danh.
     *
     * @param id          ID định danh duy nhất của người dùng
     * @param email       Địa chỉ email (dùng làm tên đăng nhập)
     * @param password    Mật khẩu đã được mã hóa BCrypt
     * @param fullName    Họ và tên đầy đủ
     * @param role        Vai trò hệ thống (ADMIN, STAFF, TEACHER, STUDENT)
     * @param isActive    Trạng thái tài khoản (đang hoạt động hay bị khóa)
     * @param authorities Danh sách quyền hạn Spring Security (tiền tố ROLE_)
     */
    public UserPrincipal(Integer id, String email, String password, String fullName,
                         String role, boolean isActive,
                         Collection<? extends GrantedAuthority> authorities) {
        this(id, email, password, fullName, role, isActive, false, authorities);
    }

    private UserPrincipal(Integer id, String email, String password, String fullName,
                          String role, boolean isActive, boolean firstPasswordChangeRequired,
                         Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.isActive = isActive;
        this.firstPasswordChangeRequired = firstPasswordChangeRequired;
        this.authorities = authorities;
    }

    /**
     * Phương thức nhà máy (factory method) để tạo UserPrincipal từ thực thể User.
     *
     * @param user Thực thể người dùng từ cơ sở dữ liệu
     * @return Đối tượng UserPrincipal chuẩn Spring Security
     */
    public static UserPrincipal create(User user) {
        String roleName = user.getRole() != null ? user.getRole().toUpperCase() : "STUDENT";
        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + roleName)
        );

        boolean active = user.getIsActive() != null && user.getIsActive();
        if ("STUDENT".equals(roleName)) {
            active = active && user.getEmailVerifiedAt() != null && !Boolean.TRUE.equals(user.getMustChangePassword());
        }

        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getFullName(),
                roleName,
                active,
                user.requiresFirstPasswordChange(),
                authorities
        );
    }

    public Integer getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    /** Trạng thái hiện tại đọc từ tài khoản khi xác thực request. */
    public boolean requiresFirstPasswordChange() {
        return firstPasswordChangeRequired;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return isActive;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }
}
