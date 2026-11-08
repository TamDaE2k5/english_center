package com.englishcenter.admission.service;

import com.englishcenter.admission.dto.bi.FunnelReportResponse;
import com.englishcenter.admission.dto.bi.RevenueOccupancyReportResponse;
import com.englishcenter.admission.dto.bi.SecurityEngagementReportResponse;

import java.time.LocalDate;

/**
 * Giao diện dịch vụ phân tích dữ liệu thông minh và báo cáo BI (Phase 5: TASK-ADM-51, TASK-ADM-52, TASK-ADM-53).
 */
public interface BiAnalyticsService {

    /**
     * Báo cáo Phễu Chuyển đổi Tuyển sinh 4 giai đoạn (BI-01).
     *
     * @param fromDate Ngày bắt đầu
     * @param toDate   Ngày kết thúc
     * @param courseId ID khóa học (tùy chọn lọc)
     * @return DTO FunnelReportResponse
     */
    FunnelReportResponse getFunnelReport(LocalDate fromDate, LocalDate toDate, Integer courseId);

    /**
     * Báo cáo Xếp hạng Khóa học, Doanh thu Tiềm năng & Tỷ lệ Lấp đầy Lớp học (BI-02).
     *
     * @param fromDate Ngày bắt đầu
     * @param toDate   Ngày kết thúc
     * @return DTO RevenueOccupancyReportResponse
     */
    RevenueOccupancyReportResponse getRevenueOccupancyReport(LocalDate fromDate, LocalDate toDate);

    /**
     * Báo cáo Giám sát Gắn kết (DAU/MAU) và An toàn Tấn công Đăng nhập (BI-03).
     *
     * @return DTO SecurityEngagementReportResponse
     */
    SecurityEngagementReportResponse getSecurityEngagementReport();
}
