package com.englishcenter.admission.security;

import com.englishcenter.admission.config.SecurityConfig;
import com.englishcenter.admission.controller.WebhookController;
import com.englishcenter.admission.service.EnrollmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Kiểm thử HTTP qua chuỗi Spring Security thật, không cần cơ sở dữ liệu. */
@WebMvcTest(value = WebhookController.class, properties = "internal.secret-key=qa-internal-secret")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class WebhookSecurityTest {
    @Autowired private MockMvc mvc;
    @MockBean private EnrollmentService enrollments;
    @MockBean private JwtTokenProvider tokens;
    @MockBean private CustomUserDetailsService users;
    private static final String BODY = "{\"event_id\":\"qa-event\",\"enrollment_id\":1}";

    /** Không có secret phải nhận 403 và không chạy nghiệp vụ thanh toán. */
    @Test
    void rejectsMissingSecret() throws Exception {
        mvc.perform(post("/api/admission/webhooks/payment-success")
                .contentType("application/json").content(BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("error.code").value("AUTH_FORBIDDEN"));
        verifyNoInteractions(enrollments);
    }

    /** Secret sai phải bị chặn trước khi Controller nhận request. */
    @Test
    void rejectsWrongSecret() throws Exception {
        mvc.perform(post("/api/admission/webhooks/payment-success")
                .header("X-Internal-Secret", "wrong")
                .contentType("application/json").content(BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(enrollments);
    }

    /** Finance gửi secret đúng vẫn gọi được webhook mà không cần JWT người dùng. */
    @Test
    void acceptsCorrectSecret() throws Exception {
        mvc.perform(post("/api/admission/webhooks/payment-success")
                .header("X-Internal-Secret", "qa-internal-secret")
                .contentType("application/json").content(BODY))
                .andExpect(status().isOk());
        verify(enrollments).processPaymentSuccess(any());
    }
}
