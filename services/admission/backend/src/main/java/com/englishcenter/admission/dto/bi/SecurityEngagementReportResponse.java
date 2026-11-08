package com.englishcenter.admission.dto.bi;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO phản hồi báo cáo Giám sát Gắn kết (DAU/MAU) và An toàn Tấn công Đăng nhập (TASK-ADM-53, BI-03).
 */
public class SecurityEngagementReportResponse {

    private EngagementData engagement;
    private SecurityData security;

    public SecurityEngagementReportResponse() {
    }

    public SecurityEngagementReportResponse(EngagementData engagement, SecurityData security) {
        this.engagement = engagement;
        this.security = security;
    }

    public EngagementData getEngagement() {
        return engagement;
    }

    public void setEngagement(EngagementData engagement) {
        this.engagement = engagement;
    }

    public SecurityData getSecurity() {
        return security;
    }

    public void setSecurity(SecurityData security) {
        this.security = security;
    }

    public static class EngagementData {
        private Long dau;
        private Long mau;
        private Map<String, Long> activeByRole;

        public EngagementData() {
        }

        public EngagementData(Long dau, Long mau, Map<String, Long> activeByRole) {
            this.dau = dau;
            this.mau = mau;
            this.activeByRole = activeByRole;
        }

        public Long getDau() {
            return dau;
        }

        public void setDau(Long dau) {
            this.dau = dau;
        }

        public Long getMau() {
            return mau;
        }

        public void setMau(Long mau) {
            this.mau = mau;
        }

        public Map<String, Long> getActiveByRole() {
            return activeByRole;
        }

        public void setActiveByRole(Map<String, Long> activeByRole) {
            this.activeByRole = activeByRole;
        }
    }

    public static class SecurityData {
        private Double failedLoginRate;
        private Long totalAttempts24h;
        private Long failedAttempts24h;
        private String status; // NORMAL, WARNING
        private List<SuspiciousIpItem> suspiciousIps;

        public SecurityData() {
        }

        public SecurityData(Double failedLoginRate, Long totalAttempts24h, Long failedAttempts24h, String status, List<SuspiciousIpItem> suspiciousIps) {
            this.failedLoginRate = failedLoginRate;
            this.totalAttempts24h = totalAttempts24h;
            this.failedAttempts24h = failedAttempts24h;
            this.status = status;
            this.suspiciousIps = suspiciousIps;
        }

        public Double getFailedLoginRate() {
            return failedLoginRate;
        }

        public void setFailedLoginRate(Double failedLoginRate) {
            this.failedLoginRate = failedLoginRate;
        }

        public Long getTotalAttempts24h() {
            return totalAttempts24h;
        }

        public void setTotalAttempts24h(Long totalAttempts24h) {
            this.totalAttempts24h = totalAttempts24h;
        }

        public Long getFailedAttempts24h() {
            return failedAttempts24h;
        }

        public void setFailedAttempts24h(Long failedAttempts24h) {
            this.failedAttempts24h = failedAttempts24h;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public List<SuspiciousIpItem> getSuspiciousIps() {
            return suspiciousIps;
        }

        public void setSuspiciousIps(List<SuspiciousIpItem> suspiciousIps) {
            this.suspiciousIps = suspiciousIps;
        }
    }

    public static class SuspiciousIpItem {
        private String ipAddress;
        private Long failedCount;
        private LocalDateTime lastAttempt;

        public SuspiciousIpItem() {
        }

        public SuspiciousIpItem(String ipAddress, Long failedCount, LocalDateTime lastAttempt) {
            this.ipAddress = ipAddress;
            this.failedCount = failedCount;
            this.lastAttempt = lastAttempt;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }

        public Long getFailedCount() {
            return failedCount;
        }

        public void setFailedCount(Long failedCount) {
            this.failedCount = failedCount;
        }

        public LocalDateTime getLastAttempt() {
            return lastAttempt;
        }

        public void setLastAttempt(LocalDateTime lastAttempt) {
            this.lastAttempt = lastAttempt;
        }
    }
}
