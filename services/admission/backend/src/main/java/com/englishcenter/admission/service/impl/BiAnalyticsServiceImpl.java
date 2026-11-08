package com.englishcenter.admission.service.impl;

import com.englishcenter.admission.dto.bi.FunnelReportResponse;
import com.englishcenter.admission.dto.bi.RevenueOccupancyReportResponse;
import com.englishcenter.admission.dto.bi.SecurityEngagementReportResponse;
import com.englishcenter.admission.entity.ClassEntity;
import com.englishcenter.admission.entity.Course;
import com.englishcenter.admission.repository.*;
import com.englishcenter.admission.service.BiAnalyticsService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Triển khai dịch vụ phân tích dữ liệu kinh doanh và báo cáo BI thông minh (Phase 5: TASK-ADM-51, TASK-ADM-52, TASK-ADM-53).
 */
@Service
public class BiAnalyticsServiceImpl implements BiAnalyticsService {

    private final TestAttemptRepository testAttemptRepository;
    private final TestSubmissionRepository testSubmissionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassRepository classRepository;
    private final CourseRepository courseRepository;
    private final AuthAuditLogRepository authAuditLogRepository;

    public BiAnalyticsServiceImpl(TestAttemptRepository testAttemptRepository,
                                  TestSubmissionRepository testSubmissionRepository,
                                  EnrollmentRepository enrollmentRepository,
                                  ClassRepository classRepository,
                                  CourseRepository courseRepository,
                                  AuthAuditLogRepository authAuditLogRepository) {
        this.testAttemptRepository = testAttemptRepository;
        this.testSubmissionRepository = testSubmissionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.classRepository = classRepository;
        this.courseRepository = courseRepository;
        this.authAuditLogRepository = authAuditLogRepository;
    }

    /**
     * Báo cáo Phễu Chuyển đổi Tuyển sinh 4 giai đoạn (TASK-ADM-51, BI-01).
     */
    @Override
    @Transactional(readOnly = true)
    public FunnelReportResponse getFunnelReport(LocalDate fromDate, LocalDate toDate, Integer courseId) {
        LocalDate startD = fromDate != null ? fromDate : LocalDate.now().minusDays(30);
        LocalDate endD = toDate != null ? toDate : LocalDate.now();

        LocalDateTime start = startD.atStartOfDay();
        LocalDateTime end = endD.atTime(LocalTime.MAX);

        long s1Count = testAttemptRepository.countByStartedAtBetween(start, end);
        long s2Count = testSubmissionRepository.countBySubmittedAtBetween(start, end);
        long s3Count;
        long s4Count;
        long expiredCount;

        if (courseId != null) {
            List<Integer> classIds = classRepository.findByCourseId(courseId).stream()
                    .map(ClassEntity::getId).toList();
            if (classIds.isEmpty()) {
                s3Count = 0;
                s4Count = 0;
                expiredCount = 0;
            } else {
                s3Count = enrollmentRepository.countByClassIdInAndEnrolledAtBetween(classIds, start, end);
                s4Count = enrollmentRepository.countByClassIdInAndStatusAndEnrolledAtBetween(classIds, "APPROVED", start, end);
                expiredCount = enrollmentRepository.countByClassIdInAndStatusAndEnrolledAtBetween(classIds, "EXPIRED", start, end);
            }
        } else {
            s3Count = enrollmentRepository.countByEnrolledAtBetween(start, end);
            s4Count = enrollmentRepository.countByStatusAndEnrolledAtBetween("APPROVED", start, end);
            expiredCount = enrollmentRepository.countByStatusAndEnrolledAtBetween("EXPIRED", start, end);
        }

        double s1Rate = 100.0;
        double s2Rate = s1Count > 0 ? roundTwoDecimals((double) s2Count / s1Count * 100.0) : 0.0;
        double s3Rate = s2Count > 0 ? roundTwoDecimals((double) s3Count / s2Count * 100.0) : 0.0;
        double s4Rate = s3Count > 0 ? roundTwoDecimals((double) s4Count / s3Count * 100.0) : 0.0;
        double expiredRate = s3Count > 0 ? roundTwoDecimals((double) expiredCount / s3Count * 100.0) : 0.0;

        List<FunnelReportResponse.FunnelStageItem> stages = List.of(
                new FunnelReportResponse.FunnelStageItem(1, "Bắt đầu làm Test", s1Count, s1Rate),
                new FunnelReportResponse.FunnelStageItem(2, "Hoàn thành Test", s2Count, s2Rate),
                new FunnelReportResponse.FunnelStageItem(3, "Đăng ký Giữ chỗ 48h", s3Count, s3Rate),
                new FunnelReportResponse.FunnelStageItem(4, "Nhập học Chính thức (PAID)", s4Count, s4Rate)
        );

        FunnelReportResponse.FunnelKpis kpis = new FunnelReportResponse.FunnelKpis(
                s2Rate,
                s3Rate,
                s4Rate,
                expiredRate
        );

        boolean isHealthy = expiredRate <= 25.0;
        String warning = isHealthy ? null : String.format("Tỷ lệ giữ chỗ quá hạn cao (%.1f%% > 25%%), cần tăng cường chăm sóc học vụ", expiredRate);
        FunnelReportResponse.FunnelSummary summary = new FunnelReportResponse.FunnelSummary(isHealthy, warning);

        return new FunnelReportResponse(stages, kpis, summary);
    }

    /**
     * Báo cáo Xếp hạng Khóa học, Doanh thu Tiềm năng & Tỷ lệ Lấp đầy Lớp học (TASK-ADM-52, BI-02).
     */
    @Override
    @Transactional(readOnly = true)
    public RevenueOccupancyReportResponse getRevenueOccupancyReport(LocalDate fromDate, LocalDate toDate) {
        LocalDate startD = fromDate != null ? fromDate : LocalDate.now().minusDays(30);
        LocalDate endD = toDate != null ? toDate : LocalDate.now();

        LocalDateTime start = startD.atStartOfDay();
        LocalDateTime end = endD.atTime(LocalTime.MAX);

        List<Course> courses = courseRepository.findAll();
        List<RevenueOccupancyReportResponse.TopCourseRevenueItem> topCourses = new ArrayList<>();

        BigDecimal totalPipelineRevenue = BigDecimal.ZERO;
        BigDecimal totalRealizedRevenue = BigDecimal.ZERO;

        for (Course course : courses) {
            List<ClassEntity> classes = classRepository.findByCourseId(course.getId());
            if (classes.isEmpty()) {
                continue;
            }

            List<Integer> classIds = classes.stream().map(ClassEntity::getId).toList();
            long approvedCount = enrollmentRepository.countByClassIdInAndStatusAndEnrolledAtBetween(classIds, "APPROVED", start, end);
            long pendingCount = enrollmentRepository.countByClassIdInAndStatusAndEnrolledAtBetween(classIds, "PENDING", start, end);

            BigDecimal tuition = course.getTuitionAmount() != null ? course.getTuitionAmount() : BigDecimal.ZERO;
            BigDecimal realizedRev = tuition.multiply(BigDecimal.valueOf(approvedCount));
            BigDecimal pipelineRev = tuition.multiply(BigDecimal.valueOf(pendingCount));

            totalRealizedRevenue = totalRealizedRevenue.add(realizedRev);
            totalPipelineRevenue = totalPipelineRevenue.add(pipelineRev);

            topCourses.add(new RevenueOccupancyReportResponse.TopCourseRevenueItem(
                    course.getId(),
                    course.getCode(),
                    course.getName(),
                    approvedCount,
                    pendingCount,
                    realizedRev,
                    pipelineRev
            ));
        }

        // Sắp xếp khóa học có doanh thu thực tế cao nhất lên đầu
        topCourses.sort((a, b) -> b.getRealizedRevenue().compareTo(a.getRealizedRevenue()));

        // Tính tỷ lệ lấp đầy sĩ số trung bình của các lớp OPEN hoặc IN_PROGRESS (BI-OCC-01)
        List<ClassEntity> activeClasses = classRepository.findByStatusIn(List.of("OPEN", "IN_PROGRESS"));
        long totalApprovedSlots = 0;
        long totalMaxCapacity = 0;

        for (ClassEntity c : activeClasses) {
            totalApprovedSlots += enrollmentRepository.countByClassIdAndStatus(c.getId(), "APPROVED");
            totalMaxCapacity += (c.getMaxStudents() != null ? c.getMaxStudents() : 0);
        }

        double averageOccupancyRate = totalMaxCapacity > 0
                ? roundTwoDecimals((double) totalApprovedSlots / totalMaxCapacity * 100.0)
                : 0.0;

        return new RevenueOccupancyReportResponse(
                totalPipelineRevenue,
                totalRealizedRevenue,
                averageOccupancyRate,
                topCourses
        );
    }

    /**
     * Báo cáo Giám sát Gắn kết (DAU/MAU) và An toàn Tấn công Đăng nhập (TASK-ADM-53, BI-03).
     */
    @Override
    @Transactional(readOnly = true)
    public SecurityEngagementReportResponse getSecurityEngagementReport() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime past24h = now.minusHours(24);
        LocalDateTime past30d = now.minusDays(30);

        long dau = authAuditLogRepository.countDistinctActiveUsersSince(past24h);
        long mau = authAuditLogRepository.countDistinctActiveUsersSince(past30d);

        long studentActive = authAuditLogRepository.countDistinctActiveUsersByRoleSince("STUDENT", past24h);
        long teacherActive = authAuditLogRepository.countDistinctActiveUsersByRoleSince("TEACHER", past24h);
        long staffActive = authAuditLogRepository.countDistinctActiveUsersByRoleSince("STAFF", past24h);

        Map<String, Long> activeByRole = Map.of(
                "student", studentActive,
                "teacher", teacherActive,
                "staff", staffActive
        );

        SecurityEngagementReportResponse.EngagementData engagementData =
                new SecurityEngagementReportResponse.EngagementData(dau, mau, activeByRole);

        long totalAttempts24h = authAuditLogRepository.countLoginAttemptsSince(past24h);
        long failedAttempts24h = authAuditLogRepository.countFailedLoginAttemptsSince(past24h);

        double failedLoginRate = totalAttempts24h > 0
                ? roundTwoDecimals((double) failedAttempts24h / totalAttempts24h * 100.0)
                : 0.0;

        String status = failedLoginRate > 5.0 ? "WARNING" : "NORMAL";

        List<Object[]> rawIps = authAuditLogRepository.findTopSuspiciousIps(past24h, PageRequest.of(0, 5));
        List<SecurityEngagementReportResponse.SuspiciousIpItem> suspiciousIps = new ArrayList<>();

        for (Object[] row : rawIps) {
            String ip = (String) row[0];
            Long count = ((Number) row[1]).longValue();
            LocalDateTime lastAttempt = (LocalDateTime) row[2];
            suspiciousIps.add(new SecurityEngagementReportResponse.SuspiciousIpItem(ip, count, lastAttempt));
        }

        SecurityEngagementReportResponse.SecurityData securityData =
                new SecurityEngagementReportResponse.SecurityData(
                        failedLoginRate,
                        totalAttempts24h,
                        failedAttempts24h,
                        status,
                        suspiciousIps
                );

        return new SecurityEngagementReportResponse(engagementData, securityData);
    }

    private double roundTwoDecimals(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
