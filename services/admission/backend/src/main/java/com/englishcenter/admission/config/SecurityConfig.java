package com.englishcenter.admission.config;

import com.englishcenter.admission.common.dto.ApiResponse;
import com.englishcenter.admission.common.exception.ErrorCode;
import com.englishcenter.admission.security.JwtAuthenticationFilter;
import com.englishcenter.admission.security.InternalSecretFilter;
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Cấu hình bảo mật tập trung cho Admission Service sử dụng Spring Security 6.
 * Thiết lập kiến trúc Stateless không session, lọc JWT trên mỗi request, mã hóa BCrypt
 * và chuẩn hóa phản hồi lỗi 401/403 theo cấu trúc ApiResponse chuẩn.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Value("${internal.secret-key}")
    private String internalSecretKey;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    /**
     * Cấu hình chuỗi lọc bảo mật chính (Security Filter Chain).
     *
     * @param http Đối tượng HttpSecurity của Spring Security
     * @return Chuỗi bộ lọc bảo mật đã cấu hình
     * @throws Exception nếu xảy ra lỗi cấu hình
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Vô hiệu hóa CSRF vì hệ thống sử dụng REST API không lưu trạng thái (Stateless)
                .csrf(AbstractHttpConfigurer::disable)
                // Kích hoạt cấu hình CORS linh hoạt cho Gateway và Frontend
                .cors(Customizer.withDefaults())
                // Không tạo hoặc lưu giữ HTTP Session ở phía server
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Xử lý ngoại lệ bảo mật trả về định dạng JSON ApiResponse chuẩn
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(customAuthenticationEntryPoint())
                        .accessDeniedHandler(customAccessDeniedHandler())
                )
                // Phân quyền chi tiết cho các đường dẫn API
                .authorizeHttpRequests(auth -> auth
                        // 1. Nhóm API công khai xác thực (Public Auth Endpoints)
                        .requestMatchers(
                                "/api/admission/auth/login",
                                "/api/admission/auth/register",
                                "/api/admission/auth/verify-email",
                                "/api/admission/auth/setup-password",
                                "/api/admission/auth/refresh",
                                "/api/admission/auth/forgot-password",
                                "/api/admission/auth/reset-password",
                                "/auth/**"
                        ).permitAll()

                        // 2. Nhóm API tra cứu công khai cho học viên & khách truy cập (Public Read)
                        .requestMatchers(HttpMethod.GET, "/api/admission/courses/**", "/courses/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/admission/classes/**", "/classes/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/admission/enrollments").permitAll()
                        .requestMatchers("/api/admission/tests/public/**").permitAll()

                        // 3. Nhóm Webhook và Giao tiếp Nội bộ (Webhook & Internal East-West Calls)
                        .requestMatchers("/api/admission/webhooks/**").permitAll()
                        .requestMatchers("/api/admission/internal/**").permitAll()
                        .requestMatchers("/error").permitAll()

                        // 4. Nhóm API dành riêng cho Quản trị viên (ADMIN only)
                        .requestMatchers(HttpMethod.GET, "/api/admission/users/teachers").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/admission/users/**").hasRole("ADMIN")
                        .requestMatchers("/api/admission/audit-logs/**").hasRole("ADMIN")
                        .requestMatchers("/api/admission/bi/security-engagement").hasRole("ADMIN")

                        // 5. Nhóm API dành cho Học vụ & Quản trị viên (STAFF & ADMIN)
                        .requestMatchers("/api/admission/rooms/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/admission/enrollments/*/approve-manual").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/admission/bi/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/admission/exports/**").hasAnyRole("ADMIN", "STAFF", "TEACHER")

                        // 6. Mọi yêu cầu còn lại bắt buộc phải đăng nhập hợp lệ
                        .anyRequest().authenticated()
                )
                // Đặt bộ lọc JWT Filter đứng trước bộ lọc xác thực mặc định
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new InternalSecretFilter(internalSecretKey, objectMapper), JwtAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Bean mã hóa mật khẩu theo chuẩn thuật toán BCrypt.
     *
     * @return Bộ mã hóa BCryptPasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Bean quản lý xác thực trung tâm của Spring Security.
     *
     * @param authConfig Cấu hình xác thực Spring
     * @return Đối tượng AuthenticationManager
     * @throws Exception nếu xảy ra lỗi
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Cấu hình CORS cho phép API Gateway và Frontend truy cập qua browser.
     *
     * @return Nguồn cấu hình CORS
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Internal-Secret", "Accept"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Điểm đón lỗi khi người dùng chưa đăng nhập hoặc token không hợp lệ (HTTP 401).
     * Trả về JSON chuẩn ApiResponse thay vì trang HTML 401 mặc định.
     */
    @Bean
    public AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            ApiResponse<?> errorResponse = ApiResponse.error(
                    ErrorCode.AUTH_UNAUTHORIZED,
                    "Yêu cầu đăng nhập để truy cập tài nguyên này"
            );
            objectMapper.writeValue(response.getOutputStream(), errorResponse);
        };
    }

    /**
     * Điểm đón lỗi khi người dùng không đủ quyền truy cập tài nguyên (HTTP 403).
     * Trả về JSON chuẩn ApiResponse thay vì trang HTML 403 mặc định.
     */
    @Bean
    public AccessDeniedHandler customAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            ApiResponse<?> errorResponse = ApiResponse.error(
                    ErrorCode.AUTH_FORBIDDEN,
                    "Bạn không có quyền thực hiện thao tác này"
            );
            objectMapper.writeValue(response.getOutputStream(), errorResponse);
        };
    }
}
