package com.englishcenter.admission.controller;

import com.englishcenter.admission.common.dto.ApiResponse;
import com.englishcenter.admission.dto.bi.FunnelReportResponse;
import com.englishcenter.admission.dto.bi.RevenueOccupancyReportResponse;
import com.englishcenter.admission.dto.bi.SecurityEngagementReportResponse;
import com.englishcenter.admission.service.BiAnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Controller cung cấp các API báo cáo thông minh và phân tích dữ liệu tuyển sinh BI (Phase 5).
 * Tuân thủ phân quyền chặt chẽ: Báo cáo an ninh chỉ dành cho ADMIN, báo cáo kinh doanh dành cho ADMIN và STAFF.
 */
@RestController
@RequestMapping("/api/admission/bi")
public class BiAnalyticsController {

    private final BiAnalyticsService biAnalyticsService;

    public BiAnalyticsController(BiAnalyticsService biAnalyticsService) {
        this.biAnalyticsService = biAnalyticsService;
    }

    /**
     * API Phân tích Phễu Chuyển đổi Tuyển sinh 4 Giai đoạn (TASK-ADM-51, BI-01).
     *
     * @param fromDate Ngày bắt đầu (ISO: yyyy-MM-dd)
     * @param toDate   Ngày kết thúc (ISO: yyyy-MM-dd)
     * @param courseId ID khóa học cần lọc (tùy chọn)
     * @return DTO FunnelReportResponse
     */
    @GetMapping("/funnel")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<FunnelReportResponse>> getFunnelReport(
            @RequestParam(name = "from_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(name = "to_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(name = "course_id", required = false) Integer courseId) {
        FunnelReportResponse report = biAnalyticsService.getFunnelReport(fromDate, toDate, courseId);
        return ResponseEntity.ok(ApiResponse.success(report, "Phân tích phễu chuyển đổi tuyển sinh thành công"));
    }

    /**
     * API Báo cáo Xếp hạng Khóa học, Doanh thu Tiềm năng & Tỷ lệ Lấp đầy Lớp học (TASK-ADM-52, BI-02).
     *
     * @param fromDate Ngày bắt đầu
     * @param toDate   Ngày kết thúc
     * @return DTO RevenueOccupancyReportResponse
     */
    @GetMapping("/courses-revenue")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<RevenueOccupancyReportResponse>> getRevenueOccupancyReport(
            @RequestParam(name = "from_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(name = "to_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        RevenueOccupancyReportResponse report = biAnalyticsService.getRevenueOccupancyReport(fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.success(report, "Báo cáo doanh thu tuyển sinh và tỷ lệ lấp đầy thành công"));
    }

    /**
     * API Giám sát DAU/MAU & Phát hiện Bất thường Tấn công Đăng nhập (TASK-ADM-53, BI-03).
     * Phân quyền: Chỉ Quản trị viên (ADMIN) mới có quyền truy cập.
     *
     * @return DTO SecurityEngagementReportResponse
     */
    @GetMapping("/security-engagement")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SecurityEngagementReportResponse>> getSecurityEngagementReport() {
        SecurityEngagementReportResponse report = biAnalyticsService.getSecurityEngagementReport();
        return ResponseEntity.ok(ApiResponse.success(report, "Truy vấn báo cáo an toàn và gắn kết thành công"));
    }
}
