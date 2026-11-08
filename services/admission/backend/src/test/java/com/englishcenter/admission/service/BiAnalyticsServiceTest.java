package com.englishcenter.admission.service;

import com.englishcenter.admission.dto.bi.FunnelReportResponse;
import com.englishcenter.admission.dto.bi.RevenueOccupancyReportResponse;
import com.englishcenter.admission.dto.bi.SecurityEngagementReportResponse;
import com.englishcenter.admission.entity.ClassEntity;
import com.englishcenter.admission.entity.Course;
import com.englishcenter.admission.repository.*;
import com.englishcenter.admission.service.impl.BiAnalyticsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Kiểm thử đơn vị cho BiAnalyticsService (TASK-ADM-51, TASK-ADM-52, TASK-ADM-53).
 */
@ExtendWith(MockitoExtension.class)
class BiAnalyticsServiceTest {

    @Mock
    private TestAttemptRepository testAttemptRepository;

    @Mock
    private TestSubmissionRepository testSubmissionRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private ClassRepository classRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private AuthAuditLogRepository authAuditLogRepository;

    @InjectMocks
    private BiAnalyticsServiceImpl biAnalyticsService;

    private Course mockCourse;
    private ClassEntity mockClass;

    @BeforeEach
    void setUp() {
        mockCourse = new Course();
        mockCourse.setId(1);
        mockCourse.setCode("ENG-IELTS-6.5");
        mockCourse.setName("IELTS Chuyên Sâu 6.5");
        mockCourse.setTuitionAmount(new BigDecimal("5000000.00"));

        mockClass = new ClassEntity();
        mockClass.setId(10);
        mockClass.setCode("CLS-01");
        mockClass.setCourseId(1);
        mockClass.setMaxStudents(20);
        mockClass.setStatus("OPEN");
    }

    @Test
    @DisplayName("Báo cáo Phễu tuyển sinh 4 giai đoạn hoạt động bình thường (Healthy)")
    void testGetFunnelReport_Healthy() {
        when(testAttemptRepository.countByStartedAtBetween(any(), any())).thenReturn(100L);
        when(testSubmissionRepository.countBySubmittedAtBetween(any(), any())).thenReturn(80L);
        when(enrollmentRepository.countByEnrolledAtBetween(any(), any())).thenReturn(40L);
        when(enrollmentRepository.countByStatusAndEnrolledAtBetween(eq("APPROVED"), any(), any())).thenReturn(30L);
        when(enrollmentRepository.countByStatusAndEnrolledAtBetween(eq("EXPIRED"), any(), any())).thenReturn(5L);

        FunnelReportResponse report = biAnalyticsService.getFunnelReport(LocalDate.now().minusDays(30), LocalDate.now(), null);

        assertNotNull(report);
        assertEquals(4, report.getStages().size());
        assertEquals(100L, report.getStages().get(0).getCount());
        assertEquals(80L, report.getStages().get(1).getCount());
        assertEquals(40L, report.getStages().get(2).getCount());
        assertEquals(30L, report.getStages().get(3).getCount());

        assertEquals(80.0, report.getKpis().getTestCompletionRate());
        assertEquals(50.0, report.getKpis().getTestToEnrollmentRate());
        assertEquals(75.0, report.getKpis().getPaidConversionRate());
        assertEquals(12.5, report.getKpis().getExpiredHoldRate());

        assertTrue(report.getSummary().getIsHealthy());
        assertNull(report.getSummary().getBottleneckWarning());
    }

    @Test
    @DisplayName("Báo cáo Phễu tuyển sinh phát hiện cảnh báo tắc nghẽn khi tỷ lệ quá hạn > 25%")
    void testGetFunnelReport_BottleneckWarning() {
        when(testAttemptRepository.countByStartedAtBetween(any(), any())).thenReturn(100L);
        when(testSubmissionRepository.countBySubmittedAtBetween(any(), any())).thenReturn(80L);
        when(enrollmentRepository.countByEnrolledAtBetween(any(), any())).thenReturn(40L);
        when(enrollmentRepository.countByStatusAndEnrolledAtBetween(eq("APPROVED"), any(), any())).thenReturn(20L);
        when(enrollmentRepository.countByStatusAndEnrolledAtBetween(eq("EXPIRED"), any(), any())).thenReturn(15L); // 15/40 = 37.5% > 25%

        FunnelReportResponse report = biAnalyticsService.getFunnelReport(null, null, null);

        assertNotNull(report);
        assertEquals(37.5, report.getKpis().getExpiredHoldRate());
        assertFalse(report.getSummary().getIsHealthy());
        assertNotNull(report.getSummary().getBottleneckWarning());
    }

    @Test
    @DisplayName("Báo cáo Doanh thu tiềm năng và Tỷ lệ lấp đầy tính toán chính xác (TASK-ADM-52)")
    void testGetRevenueOccupancyReport_Success() {
        when(courseRepository.findAll()).thenReturn(List.of(mockCourse));
        when(classRepository.findByCourseId(1)).thenReturn(List.of(mockClass));
        when(enrollmentRepository.countByClassIdInAndStatusAndEnrolledAtBetween(any(), eq("APPROVED"), any(), any())).thenReturn(10L);
        when(enrollmentRepository.countByClassIdInAndStatusAndEnrolledAtBetween(any(), eq("PENDING"), any(), any())).thenReturn(4L);

        when(classRepository.findByStatusIn(any())).thenReturn(List.of(mockClass));
        when(enrollmentRepository.countByClassIdAndStatus(10, "APPROVED")).thenReturn(15L);

        RevenueOccupancyReportResponse report = biAnalyticsService.getRevenueOccupancyReport(null, null);

        assertNotNull(report);
        // 10 * 5,000,000 = 50,000,000
        assertEquals(new BigDecimal("50000000.00"), report.getRealizedAdmissionRevenue());
        // 4 * 5,000,000 = 20,000,000
        assertEquals(new BigDecimal("20000000.00"), report.getPipelinePotentialRevenue());
        // 15 / 20 = 75.0%
        assertEquals(75.0, report.getAverageOccupancyRate());
        assertEquals(1, report.getTopCourses().size());
        assertEquals("ENG-IELTS-6.5", report.getTopCourses().get(0).getCourseCode());
    }

    @Test
    @DisplayName("Báo cáo Giám sát An toàn & Gắn kết DAU/MAU phát hiện tấn công (TASK-ADM-53)")
    void testGetSecurityEngagementReport_Success() {
        when(authAuditLogRepository.countDistinctActiveUsersSince(any(LocalDateTime.class))).thenReturn(150L, 1200L);
        when(authAuditLogRepository.countDistinctActiveUsersByRoleSince(eq("STUDENT"), any())).thenReturn(100L);
        when(authAuditLogRepository.countDistinctActiveUsersByRoleSince(eq("TEACHER"), any())).thenReturn(30L);
        when(authAuditLogRepository.countDistinctActiveUsersByRoleSince(eq("STAFF"), any())).thenReturn(20L);

        when(authAuditLogRepository.countLoginAttemptsSince(any())).thenReturn(200L);
        when(authAuditLogRepository.countFailedLoginAttemptsSince(any())).thenReturn(16L); // 16/200 = 8.0% > 5.0% -> WARNING

        Object[] fakeIpRow = new Object[]{"192.168.1.100", 12L, LocalDateTime.now()};
        List<Object[]> fakeList = new java.util.ArrayList<>();
        fakeList.add(fakeIpRow);
        when(authAuditLogRepository.findTopSuspiciousIps(any(), any(Pageable.class))).thenReturn(fakeList);

        SecurityEngagementReportResponse report = biAnalyticsService.getSecurityEngagementReport();

        assertNotNull(report);
        assertEquals(150L, report.getEngagement().getDau());
        assertEquals(1200L, report.getEngagement().getMau());
        assertEquals(100L, report.getEngagement().getActiveByRole().get("student"));
        assertEquals(8.0, report.getSecurity().getFailedLoginRate());
        assertEquals("WARNING", report.getSecurity().getStatus());
        assertEquals(1, report.getSecurity().getSuspiciousIps().size());
        assertEquals("192.168.1.100", report.getSecurity().getSuspiciousIps().get(0).getIpAddress());
    }
}
