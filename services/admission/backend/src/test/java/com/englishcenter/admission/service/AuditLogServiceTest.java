package com.englishcenter.admission.service;

import com.englishcenter.admission.common.dto.PageResponse;
import com.englishcenter.admission.dto.audit.AuditLogResponse;
import com.englishcenter.admission.entity.AuthAuditLog;
import com.englishcenter.admission.repository.AuthAuditLogRepository;
import com.englishcenter.admission.service.impl.AuditLogServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử nghiệp vụ nhật ký kiểm toán AuditLogService")
class AuditLogServiceTest {

    @Mock
    private AuthAuditLogRepository authAuditLogRepository;

    private AuditLogServiceImpl auditLogService;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogServiceImpl(authAuditLogRepository);
    }

    @Test
    @DisplayName("Ghi nhận sự kiện kiểm toán bảo mật thành công")
    void testLogEvent_Success() {
        auditLogService.logEvent(
                1, "admin@englishcenter.edu.vn", "LOGIN_SUCCESS", "192.168.1.10", "Chrome", "Đăng nhập thành công"
        );

        ArgumentCaptor<AuthAuditLog> logCaptor = ArgumentCaptor.forClass(AuthAuditLog.class);
        verify(authAuditLogRepository).save(logCaptor.capture());
        AuthAuditLog saved = logCaptor.getValue();

        assertEquals(1, saved.getUserId());
        assertEquals("admin@englishcenter.edu.vn", saved.getEmail());
        assertEquals("LOGIN_SUCCESS", saved.getAction());
        assertEquals("192.168.1.10", saved.getIpAddress());
    }

    @Test
    @DisplayName("Phát hiện tấn công Brute-force khi có >= 5 lần đăng nhập thất bại trong 10 phút")
    void testIsBruteForceAttempt_Detected() {
        when(authAuditLogRepository.countByIpAddressAndActionAndCreatedAtAfter(
                eq("192.168.1.99"), eq("LOGIN_FAILED"), any(LocalDateTime.class)
        )).thenReturn(5L);

        boolean isAttack = auditLogService.isBruteForceAttempt("192.168.1.99", "victim@englishcenter.edu.vn");

        assertTrue(isAttack);
    }

    @Test
    @DisplayName("Không kích hoạt cảnh báo Brute-force khi số lần thất bại < 5")
    void testIsBruteForceAttempt_Normal() {
        when(authAuditLogRepository.countByIpAddressAndActionAndCreatedAtAfter(
                eq("192.168.1.50"), eq("LOGIN_FAILED"), any(LocalDateTime.class)
        )).thenReturn(2L);
        when(authAuditLogRepository.countByEmailAndActionAndCreatedAtAfter(
                eq("normal@englishcenter.edu.vn"), eq("LOGIN_FAILED"), any(LocalDateTime.class)
        )).thenReturn(2L);

        boolean isAttack = auditLogService.isBruteForceAttempt("192.168.1.50", "normal@englishcenter.edu.vn");

        assertFalse(isAttack);
    }

    @Test
    @DisplayName("Admin truy vấn danh sách nhật ký kiểm toán có phân trang và lọc theo hành động")
    void testGetAuditLogs() {
        AuthAuditLog log1 = new AuthAuditLog();
        log1.setId(101);
        log1.setUserId(2);
        log1.setEmail("staff@englishcenter.edu.vn");
        log1.setAction("LOGIN_SUCCESS");

        when(authAuditLogRepository.searchAuditLogs(
                eq(2), eq("LOGIN_SUCCESS"), any(), any(), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(log1)));

        PageResponse<AuditLogResponse> response = auditLogService.getAuditLogs(
                2, "LOGIN_SUCCESS", null, null, 0, 10
        );

        assertNotNull(response);
        assertEquals(1, response.getItems().size());
        assertEquals("LOGIN_SUCCESS", response.getItems().get(0).getEventType());
    }
}
