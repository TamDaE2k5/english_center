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
