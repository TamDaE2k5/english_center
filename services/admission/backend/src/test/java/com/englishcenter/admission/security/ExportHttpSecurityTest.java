package com.englishcenter.admission.security;

import com.englishcenter.admission.config.SecurityConfig;
import com.englishcenter.admission.controller.ExportController;
import com.englishcenter.admission.entity.User;
import com.englishcenter.admission.service.ExportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Kiểm tra ma trận BA và đầu vào export qua security chain thật của Spring MVC. */
@WebMvcTest(value = ExportController.class, properties = "internal.secret-key=qa-only-secret")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ExportHttpSecurityTest {
    @Autowired MockMvc mvc;
    @MockBean ExportService exports;
    @MockBean JwtTokenProvider jwt;
    @MockBean CustomUserDetailsService details;

    @Test
    void teacherCannotRequestRosterUnderBaMatrix() throws Exception {
        allowToken("TEACHER");
        mvc.perform(get("/api/admission/exports/classes/10/roster").header("Authorization", "Bearer qa"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("AUTH_FORBIDDEN"));
        verifyNoInteractions(exports);
    }

    @Test
    void invalidExportIdsReturnBadRequestBeforeReadingData() throws Exception {
        allowToken("STAFF");
        for (String path : new String[]{"/classes/abc/roster", "/classes/0/roster", "/tests/-5/results"}) {
            mvc.perform(get("/api/admission/exports" + path).header("Authorization", "Bearer qa"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("ADM_VALIDATION_ERROR"));
        }
        verifyNoInteractions(exports);
    }

    /** Cấp principal test để kiểm tra matcher và method security theo role, không cần DB. */
    private void allowToken(String role) {
        User user = new User();
        user.setId(20);
        user.setRole(role);
        user.setIsActive(true);
        when(jwt.validateToken("qa")).thenReturn(true);
        when(jwt.getUserIdFromToken("qa")).thenReturn(20);
        when(jwt.getPasswordChangeRequirement("qa")).thenReturn(false);
        when(details.loadUserById(20)).thenReturn(UserPrincipal.create(user));
    }
}
