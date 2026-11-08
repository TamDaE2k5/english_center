package com.englishcenter.admission.dto.bi;

import java.util.List;

/**
 * DTO phản hồi báo cáo Phễu Chuyển đổi Tuyển sinh 4 giai đoạn (TASK-ADM-51, BI-01).
 * Tuân thủ quy ước snake_case khi tuần tự hóa sang JSON.
 */
public class FunnelReportResponse {

    private List<FunnelStageItem> stages;
    private FunnelKpis kpis;
    private FunnelSummary summary;

    public FunnelReportResponse() {
    }

    public FunnelReportResponse(List<FunnelStageItem> stages, FunnelKpis kpis, FunnelSummary summary) {
        this.stages = stages;
        this.kpis = kpis;
        this.summary = summary;
    }

    public List<FunnelStageItem> getStages() {
        return stages;
    }

    public void setStages(List<FunnelStageItem> stages) {
        this.stages = stages;
    }

    public FunnelKpis getKpis() {
        return kpis;
    }

    public void setKpis(FunnelKpis kpis) {
        this.kpis = kpis;
    }

    public FunnelSummary getSummary() {
        return summary;
    }

    public void setSummary(FunnelSummary summary) {
        this.summary = summary;
    }

    public static class FunnelStageItem {
        private Integer stageId;
        private String name;
        private Long count;
        private Double conversionRate;

        public FunnelStageItem() {
        }

        public FunnelStageItem(Integer stageId, String name, Long count, Double conversionRate) {
            this.stageId = stageId;
            this.name = name;
            this.count = count;
            this.conversionRate = conversionRate;
        }

        public Integer getStageId() {
            return stageId;
        }

        public void setStageId(Integer stageId) {
            this.stageId = stageId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Long getCount() {
            return count;
        }

        public void setCount(Long count) {
            this.count = count;
        }

        public Double getConversionRate() {
            return conversionRate;
        }

        public void setConversionRate(Double conversionRate) {
            this.conversionRate = conversionRate;
        }
    }

    public static class FunnelKpis {
        private Double testCompletionRate;
        private Double testToEnrollmentRate;
        private Double paidConversionRate;
        private Double expiredHoldRate;

        public FunnelKpis() {
        }

        public FunnelKpis(Double testCompletionRate, Double testToEnrollmentRate, Double paidConversionRate, Double expiredHoldRate) {
            this.testCompletionRate = testCompletionRate;
            this.testToEnrollmentRate = testToEnrollmentRate;
            this.paidConversionRate = paidConversionRate;
            this.expiredHoldRate = expiredHoldRate;
        }

        public Double getTestCompletionRate() {
            return testCompletionRate;
        }

        public void setTestCompletionRate(Double testCompletionRate) {
            this.testCompletionRate = testCompletionRate;
        }

        public Double getTestToEnrollmentRate() {
            return testToEnrollmentRate;
        }

        public void setTestToEnrollmentRate(Double testToEnrollmentRate) {
            this.testToEnrollmentRate = testToEnrollmentRate;
        }

        public Double getPaidConversionRate() {
            return paidConversionRate;
        }

        public void setPaidConversionRate(Double paidConversionRate) {
            this.paidConversionRate = paidConversionRate;
        }

        public Double getExpiredHoldRate() {
            return expiredHoldRate;
        }

        public void setExpiredHoldRate(Double expiredHoldRate) {
            this.expiredHoldRate = expiredHoldRate;
        }
    }

    public static class FunnelSummary {
        private Boolean isHealthy;
        private String bottleneckWarning;

        public FunnelSummary() {
        }

        public FunnelSummary(Boolean isHealthy, String bottleneckWarning) {
            this.isHealthy = isHealthy;
            this.bottleneckWarning = bottleneckWarning;
        }

        public Boolean getIsHealthy() {
            return isHealthy;
        }

        public void setIsHealthy(Boolean isHealthy) {
            this.isHealthy = isHealthy;
        }

        public String getBottleneckWarning() {
            return bottleneckWarning;
        }

        public void setBottleneckWarning(String bottleneckWarning) {
            this.bottleneckWarning = bottleneckWarning;
        }
    }
}
