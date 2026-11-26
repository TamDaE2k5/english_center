package com.englishcenter.admission.security;

import com.englishcenter.admission.common.dto.ApiResponse;
import com.englishcenter.admission.common.exception.ErrorCode;
import com.englishcenter.admission.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Filter chặn mọi HTTP request đến hệ thống để trích xuất và xác thực JSON Web Token.
 * Nếu token hợp lệ, thiết lập thông tin Authentication vào SecurityContextHolder.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final Set<String> FIRST_PASSWORD_POST_PATHS = Set.of(
            "/auth/change-password", "/auth/login", "/auth/refresh", "/auth/logout",
            "/auth/forgot-password", "/auth/reset-password");

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider,
                                   CustomUserDetailsService customUserDetailsService,
                                   ObjectMapper objectMapper) {
        this.tokenProvider = tokenProvider;
        this.customUserDetailsService = customUserDetailsService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        boolean passwordChangeBlocked = false;
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt) && !tokenProvider.isRefreshToken(jwt)) {
                Integer userId = tokenProvider.getUserIdFromToken(jwt);

                if (userId != null) {
                    UserDetails userDetails = customUserDetailsService.loadUserById(userId);

                    if (!userDetails.isEnabled()) {
                        log.warn("Tài khoản người dùng ID {} đã bị vô hiệu hóa hoặc khóa", userId);
                    } else if (userDetails instanceof UserPrincipal principal) {
                        boolean required = principal.requiresFirstPasswordChange();
                        Boolean issuedForChange = tokenProvider.getPasswordChangeRequirement(jwt);
                        if (required && !allowsFirstPasswordPath(request)) {
                            passwordChangeBlocked = true;
                        } else if (required || isNormalAccess(principal, issuedForChange)) {
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Không thể thiết lập xác thực người dùng: {}", ex.getClass().getSimpleName());
        }

        if (passwordChangeBlocked) {
            ErrorCode error = ErrorCode.AUTH_PASSWORD_CHANGE_REQUIRED;
            response.setStatus(error.getHttpStatus().value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getOutputStream(), ApiResponse.error(error, error.getDefaultMessage()));
            return;
        }
        filterChain.doFilter(request, response);
    }

    /** Chỉ cấp quyền thông thường cho JWT nhân sự đã xác nhận phạm vi lúc cấp. */
    private boolean isNormalAccess(UserPrincipal user, Boolean issuedForChange) {
        return !Boolean.TRUE.equals(issuedForChange) && (!User.isStaffRole(user.getRole()) || issuedForChange != null);
    }

    /** Danh sách đúng route và HTTP method cần thiết để đổi, khôi phục hoặc thoát phiên. */
    private boolean allowsFirstPasswordPath(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.startsWith("/api/admission/auth/")) {
            path = path.substring("/api/admission".length());
        }
        return ("GET".equals(request.getMethod()) && "/auth/me".equals(path))
                || ("POST".equals(request.getMethod()) && FIRST_PASSWORD_POST_PATHS.contains(path));
    }

    /**
     * Trích xuất JWT từ header Authorization dạng 'Bearer <token>'.
     *
     * @param request HTTP Servlet Request
     * @return Chuỗi token JWT thuần túy hoặc null nếu không có
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
