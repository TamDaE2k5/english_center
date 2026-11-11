package com.englishcenter.admission.service.impl;

import com.englishcenter.admission.common.dto.PageResponse;
import com.englishcenter.admission.dto.audit.AuditLogResponse;
import com.englishcenter.admission.entity.AuthAuditLog;
import com.englishcenter.admission.repository.AuthAuditLogRepository;
import com.englishcenter.admission.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Lớp triển khai các nghiệp vụ ghi vết và tra cứu nhật ký kiểm toán bảo mật hệ thống.
 */
@Service
public class AuditLogServiceImpl implements AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogServiceImpl.class);

    private final AuthAuditLogRepository authAuditLogRepository;

    public AuditLogServiceImpl(AuthAuditLogRepository authAuditLogRepository) {
        this.authAuditLogRepository = authAuditLogRepository;
    }

    /**
     * Ghi nhận một sự kiện kiểm toán bảo mật mới vào cơ sở dữ liệu.
     *
     * @param userId    ID người dùng (nếu có)
     * @param email     Email liên quan
     * @param action    Hành động kiểm toán
     * @param ipAddress Địa chỉ IP của client
     * @param userAgent Thiết bị client
     * @param details   Thông tin chi tiết
     */
    @Override
    @Transactional
    public void logEvent(Integer userId, String email, String action, String ipAddress, String userAgent, String details) {
        try {
            AuthAuditLog auditLog = new AuthAuditLog();
            auditLog.setUserId(userId);
            auditLog.setEmail(email != null ? email.trim().toLowerCase() : null);
            auditLog.setAction(action);
            auditLog.setIpAddress(ipAddress);
            auditLog.setUserAgent(userAgent);
            auditLog.setDetails(details);

            authAuditLogRepository.save(auditLog);
            log.debug("Ghi nhận nhật ký kiểm toán [{}]: email={}, IP={}", action, email, ipAddress);
        } catch (Exception ex) {
            log.error("Lỗi khi lưu nhật ký kiểm toán: {}", ex.getMessage());
        }
    }

    /**
     * Kiểm tra dấu hiệu tấn công Brute-force (đăng nhập sai quá 5 lần trong 10 phút gần nhất).
     *
     * @param ipAddress Địa chỉ IP client
     * @param email     Email đang thử đăng nhập
     * @return true nếu phát hiện tần suất thất bại bất thường, false nếu an toàn
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isBruteForceAttempt(String ipAddress, String email) {
        LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);

        if (ipAddress != null && !ipAddress.isEmpty()) {
            long ipFailCount = authAuditLogRepository.countByIpAddressAndActionAndCreatedAtAfter(
                    ipAddress, "LOGIN_FAILED", tenMinutesAgo
            );
            if (ipFailCount >= 5) {
                log.warn("CẢNH BÁO BRUTE-FORCE: Địa chỉ IP {} đã đăng nhập thất bại {} lần trong 10 phút", ipAddress, ipFailCount);
                return true;
            }
        }

        if (email != null && !email.trim().isEmpty()) {
            String normalizedEmail = email.trim().toLowerCase();
            long emailFailCount = authAuditLogRepository.countByEmailAndActionAndCreatedAtAfter(
                    normalizedEmail, "LOGIN_FAILED", tenMinutesAgo
            );
            if (emailFailCount >= 5) {
                log.warn("CẢNH BÁO BRUTE-FORCE: Tài khoản email {} đã bị thử sai mật khẩu {} lần trong 10 phút", normalizedEmail, emailFailCount);
                return true;
            }
        }

        return false;
    }

    /**
     * Truy vấn nhật ký kiểm toán bảo mật với bộ lọc linh hoạt và phân trang (dành cho Admin).
     *
     * @param userId    Lọc theo ID người dùng (hoặc null)
     * @param eventType Lọc theo loại hành động sự kiện (hoặc null)
     * @param fromDate  Mốc thời gian bắt đầu (hoặc null)
     * @param toDate    Mốc thời gian kết thúc (hoặc null)
     * @param page      Số trang (0-based)
     * @param size      Số bản ghi mỗi trang
     * @return PageResponse chứa danh sách AuditLogResponse
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> getAuditLogs(Integer userId, String eventType,
                                                        LocalDateTime fromDate, LocalDateTime toDate,
                                                        int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        String actionFilter = (eventType != null && !eventType.trim().isEmpty()) ? eventType.trim().toUpperCase() : null;

        Page<AuthAuditLog> logPage = authAuditLogRepository.searchAuditLogs(userId, actionFilter, fromDate, toDate, pageable);
        List<AuditLogResponse> dtoList = logPage.getContent().stream()
                .map(AuditLogResponse::from)
                .toList();

        return PageResponse.of(dtoList, page, size, logPage.getTotalElements());
    }
}
