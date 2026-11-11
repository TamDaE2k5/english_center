package com.englishcenter.admission.dto.bi;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO phản hồi báo cáo Xếp hạng Khóa học, Doanh thu Tiềm năng & Tỷ lệ Lấp đầy (TASK-ADM-52, BI-02).
 */
public class RevenueOccupancyReportResponse {

    private BigDecimal pipelinePotentialRevenue;
    private BigDecimal realizedAdmissionRevenue;
    private Double averageOccupancyRate;
    private List<TopCourseRevenueItem> topCourses;

    public RevenueOccupancyReportResponse() {
    }

    public RevenueOccupancyReportResponse(BigDecimal pipelinePotentialRevenue,
                                          BigDecimal realizedAdmissionRevenue,
                                          Double averageOccupancyRate,
                                          List<TopCourseRevenueItem> topCourses) {
        this.pipelinePotentialRevenue = pipelinePotentialRevenue;
        this.realizedAdmissionRevenue = realizedAdmissionRevenue;
        this.averageOccupancyRate = averageOccupancyRate;
        this.topCourses = topCourses;
    }

    public BigDecimal getPipelinePotentialRevenue() {
        return pipelinePotentialRevenue;
    }

    public void setPipelinePotentialRevenue(BigDecimal pipelinePotentialRevenue) {
        this.pipelinePotentialRevenue = pipelinePotentialRevenue;
    }

    public BigDecimal getRealizedAdmissionRevenue() {
        return realizedAdmissionRevenue;
    }

    public void setRealizedAdmissionRevenue(BigDecimal realizedAdmissionRevenue) {
        this.realizedAdmissionRevenue = realizedAdmissionRevenue;
    }

    public Double getAverageOccupancyRate() {
        return averageOccupancyRate;
    }

    public void setAverageOccupancyRate(Double averageOccupancyRate) {
        this.averageOccupancyRate = averageOccupancyRate;
    }

    public List<TopCourseRevenueItem> getTopCourses() {
        return topCourses;
    }

    public void setTopCourses(List<TopCourseRevenueItem> topCourses) {
        this.topCourses = topCourses;
    }

    public static class TopCourseRevenueItem {
        private Integer courseId;
        private String courseCode;
        private String title;
        private Long approvedEnrollments;
        private Long pendingEnrollments;
        private BigDecimal realizedRevenue;
        private BigDecimal pipelineRevenue;

        public TopCourseRevenueItem() {
        }

        public TopCourseRevenueItem(Integer courseId, String courseCode, String title,
                                    Long approvedEnrollments, Long pendingEnrollments,
                                    BigDecimal realizedRevenue, BigDecimal pipelineRevenue) {
            this.courseId = courseId;
            this.courseCode = courseCode;
            this.title = title;
            this.approvedEnrollments = approvedEnrollments;
            this.pendingEnrollments = pendingEnrollments;
            this.realizedRevenue = realizedRevenue;
            this.pipelineRevenue = pipelineRevenue;
        }

        public Integer getCourseId() {
            return courseId;
        }

        public void setCourseId(Integer courseId) {
            this.courseId = courseId;
        }

        public String getCourseCode() {
            return courseCode;
        }

        public void setCourseCode(String courseCode) {
            this.courseCode = courseCode;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Long getApprovedEnrollments() {
            return approvedEnrollments;
        }

        public void setApprovedEnrollments(Long approvedEnrollments) {
            this.approvedEnrollments = approvedEnrollments;
        }

        public Long getPendingEnrollments() {
            return pendingEnrollments;
        }

        public void setPendingEnrollments(Long pendingEnrollments) {
            this.pendingEnrollments = pendingEnrollments;
        }

        public BigDecimal getRealizedRevenue() {
            return realizedRevenue;
        }

        public void setRealizedRevenue(BigDecimal realizedRevenue) {
            this.realizedRevenue = realizedRevenue;
        }

        public BigDecimal getPipelineRevenue() {
            return pipelineRevenue;
        }

        public void setPipelineRevenue(BigDecimal pipelineRevenue) {
            this.pipelineRevenue = pipelineRevenue;
        }
    }
}
