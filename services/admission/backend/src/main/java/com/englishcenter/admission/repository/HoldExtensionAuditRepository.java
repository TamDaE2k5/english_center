package com.englishcenter.admission.repository;

import com.englishcenter.admission.dto.enrollment.ExtendHoldRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

/** Nhật ký gia hạn chỉ trong Admission; không lưu body, JWT, secret hoặc ghi log lý do người dùng nhập. */
@Repository
public class HoldExtensionAuditRepository {
    private final JdbcTemplate jdbc;

    /** Nhận datasource cùng transaction JPA để audit thành công và deadline không thể commit riêng. */
    public HoldExtensionAuditRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** Lưu actor, thời điểm, hạn cũ/mới; lỗi DB phải rollback cả gia hạn. Lý do nghiệp vụ nằm ở enrollment.notes. */
    public void extended(Integer id, Integer actor, Integer hours, LocalDateTime previous, LocalDateTime next) {
        jdbc.update("INSERT INTO admission.hold_extension_audit(enrollment_id,actor_id,outcome,reason_code,extended_hours,previous_deadline,new_deadline) VALUES (?,?,'EXTENDED','EXTENDED',?,?,?)",
                id, actor, hours, previous, next);
    }

    /** Ghi từ chối sau khi transaction nghiệp vụ rollback; hỗ trợ cả actor/enrollment không tồn tại, không FK. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rejected(Integer id, Integer actor, ExtendHoldRequest request, String reasonCode) {
        jdbc.update("INSERT INTO admission.hold_extension_audit(enrollment_id,actor_id,outcome,reason_code,extended_hours) VALUES (?,?,'REJECTED',?,?)",
                id, actor, reasonCode, request == null ? null : request.getExtendedHours());
    }
}
