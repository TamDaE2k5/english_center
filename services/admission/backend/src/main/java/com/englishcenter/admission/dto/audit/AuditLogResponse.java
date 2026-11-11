package com.englishcenter.admission.dto.audit;

import com.englishcenter.admission.entity.AuthAuditLog;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * DTO dữ liệu trả về của một bản ghi nhật ký kiểm toán bảo mật.
 */
public class AuditLogResponse {

    @JsonProperty("log_id")
    private Integer logId;

    @JsonProperty("user_id")
    private Integer userId;

    @JsonProperty("email")
    private String email;

    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("ip_address")
    private String ipAddress;

    @JsonProperty("user_agent")
    private String userAgent;

    @JsonProperty("details")
    private String details;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    public AuditLogResponse() {
    }

    /**
     * Phương thức nhà máy chuyển đổi từ thực thể AuthAuditLog sang DTO AuditLogResponse.
     *
     * @param log Bản ghi thực thể kiểm toán
     * @return DTO AuditLogResponse
     */
    public static AuditLogResponse from(AuthAuditLog log) {
        if (log == null) {
            return null;
        }
        AuditLogResponse res = new AuditLogResponse();
        res.logId = log.getId();
        res.userId = log.getUserId();
        res.email = log.getEmail();
        res.eventType = log.getAction();
        res.ipAddress = log.getIpAddress();
        res.userAgent = log.getUserAgent();
        res.details = log.getDetails();
        res.createdAt = log.getCreatedAt();
        return res;
    }

    public Integer getLogId() {
        return logId;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getEventType() {
        return eventType;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getDetails() {
        return details;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
