package com.englishcenter.admission.repository;

import com.englishcenter.admission.entity.ProcessedFinanceEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Kho truy vấn dữ liệu sự kiện thanh toán đã xử lý từ Finance Service (bảng admission.processed_finance_events).
 * Phục vụ cơ chế Idempotency INV-ENR-02.
 */
@Repository
public interface ProcessedFinanceEventRepository extends JpaRepository<ProcessedFinanceEvent, Integer> {

    /** Khóa event trước khóa lớp, serialize cả event dùng sai enrollment ở lớp khác. */
    @org.springframework.data.jpa.repository.Query(value = "SELECT 1 FROM pg_advisory_xact_lock(8081,2)", nativeQuery = true)
    Integer lockCallbacks();
    /** Audit từ chối commit độc lập; không FK để không đợi enrollment/event đang khóa. */
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "INSERT INTO admission.finance_callback_audit(event_id,enrollment_id,reason) VALUES (:event,:enrollment,:reason)", nativeQuery = true)
    int auditRejected(@org.springframework.data.repository.query.Param("event") String event,
                      @org.springframework.data.repository.query.Param("enrollment") Integer enrollment,
                      @org.springframework.data.repository.query.Param("reason") String reason);

    /**
     * Tìm bản ghi sự kiện theo mã finance_event_id.
     *
     * @param financeEventId Mã sự kiện từ Finance Service
     * @return Optional chứa ProcessedFinanceEvent
     */
    Optional<ProcessedFinanceEvent> findByFinanceEventId(String financeEventId);

    /**
     * Kiểm tra xem sự kiện thanh toán này đã được xử lý trước đó chưa.
     *
     * @param financeEventId Mã sự kiện
     * @return true nếu đã xử lý
     */
    boolean existsByFinanceEventId(String financeEventId);
}
