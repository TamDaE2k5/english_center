package com.englishcenter.admission.security;

import com.englishcenter.admission.controller.PlacementTestController;
import com.englishcenter.admission.config.SecurityConfig;
import com.englishcenter.admission.service.*;
import com.englishcenter.admission.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Gọi API công khai qua security chain thật để chứng minh không thể giả danh. */
@WebMvcTest(value = PlacementTestController.class, properties = "internal.secret-key=qa-only-secret")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, PublicPlacementTestService.class, PlacementAttemptToken.class})
class PlacementPublicApiTest {
    @Autowired MockMvc mvc;
    @MockBean PlacementTestService placement;
    @MockBean TestManagementService management;
    @MockBean JwtTokenProvider jwt;
    @MockBean CustomUserDetailsService details;
    @MockBean UserRepository users;
    @MockBean TestAttemptRepository attempts;
    @MockBean UserService userService;

    /** Chỉ biết ID tăng dần của lượt thi không đủ để nộp bài. */
    @Test void anonymousCannotSubmitByGuessingAttemptId() throws Exception {
        mvc.perform(post("/api/admission/tests/public/submit").contentType("application/json")
                .content("{\"attempt_id\":1,\"answers\":[{\"question_id\":7,\"selected_option\":\"A\"}]}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(placement);
    }

    /** Khách không được khởi tạo lượt thi cho user_id tùy ý. */
    @Test void anonymousCannotStartForExistingUserId() throws Exception {
        mvc.perform(post("/api/admission/tests/public/2/start").contentType("application/json")
                .content("{\"user_id\":999}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(placement);
    }
}
