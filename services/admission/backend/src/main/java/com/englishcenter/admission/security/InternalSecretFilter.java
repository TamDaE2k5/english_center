package com.englishcenter.admission.security;

import com.englishcenter.admission.common.dto.ApiResponse;
import com.englishcenter.admission.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Xác thực giao tiếp nội bộ trước khi webhook được phép thay đổi dữ liệu tuyển sinh. */
public class InternalSecretFilter extends OncePerRequestFilter {
    private final String secret;
    private final ObjectMapper mapper;

    /** Nhận secret cấu hình và bộ ghi JSON để trả lỗi theo API contract. */
    public InternalSecretFilter(String secret, ObjectMapper mapper) {
        this.secret = secret;
        this.mapper = mapper;
    }

    /** Chỉ bảo vệ hai nhóm URL nội bộ, không ảnh hưởng API công khai và CORS preflight. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return "OPTIONS".equals(request.getMethod())
                || !(path.equals("/api/admission/webhooks") || path.startsWith("/api/admission/webhooks/")
                || path.equals("/api/admission/internal") || path.startsWith("/api/admission/internal/"));
    }

    /** Secret thiếu hoặc sai trả 403; secret đúng cho phép gọi tiếp Controller. */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String provided = request.getHeader("X-Internal-Secret");
        if (secret == null || secret.isBlank() || provided == null
                || !MessageDigest.isEqual(secret.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            mapper.writeValue(response.getOutputStream(), ApiResponse.error(ErrorCode.AUTH_FORBIDDEN,
                    "Thông tin xác thực nội bộ không hợp lệ"));
            return;
        }
        chain.doFilter(request, response);
    }
}
