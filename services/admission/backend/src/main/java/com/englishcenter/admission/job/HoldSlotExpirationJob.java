package com.englishcenter.admission.job;

import com.englishcenter.admission.service.EnrollmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tiến trình nền tự động quét và thu hồi các slot giữ chỗ 48h quá hạn (TASK-ADM-43, FR-ENR-04).
 * Chuyển các đơn PENDING có hold_expires_at < now sang trạng thái EXPIRED để giải phóng slot trống cho học viên khác.
 */
@Component
public class HoldSlotExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(HoldSlotExpirationJob.class);

    private final EnrollmentService enrollmentService;

    public HoldSlotExpirationJob(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    /**
     * Quét định kỳ 5 phút một lần để giải phóng các slot giữ chỗ quá hạn 48h.
     */
    @Scheduled(cron = "${jobs.hold-expiration.cron:0 */5 * * * *}")
    public void scanAndExpireOverdueSlots() {
        log.info("Bắt đầu tiến trình định kỳ quét thu hồi đơn giữ chỗ quá hạn 48h...");
        try {
            int expiredCount = enrollmentService.expireOverdueEnrollments();
            if (expiredCount > 0) {
                log.info("Đã thu hồi thành công {} đơn giữ chỗ quá hạn, chuyển sang trạng thái EXPIRED.", expiredCount);
            } else {
                log.info("Không có đơn giữ chỗ nào quá hạn cần thu hồi.");
            }
        } catch (Exception ex) {
            log.error("Lỗi trong quá trình quét thu hồi đơn giữ chỗ quá hạn: {}", ex.getMessage(), ex);
        }
    }
}
