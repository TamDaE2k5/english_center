package com.englishcenter.admission.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Thực thể biểu diễn sự kiện thanh toán từ Finance Service đã được đối soát và xử lý (bảng admission.processed_finance_events).
 * Đảm bảo tính độc đẳng (Idempotency - INV-ENR-02) chống xử lý lặp lại webhook và đối soát thanh toán muộn (INV-ENR-03).
 */
@Entity
@Table(name = "processed_finance_events", schema = "admission")
public class ProcessedFinanceEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "enrollment_id", nullable = false)
    private Integer enrollmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", insertable = false, updatable = false)
    private Enrollment enrollment;

    @Column(name = "finance_event_id", nullable = false, unique = true)
    private String financeEventId;

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt = LocalDateTime.now();

    public ProcessedFinanceEvent() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(Integer enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public Enrollment getEnrollment() {
        return enrollment;
    }

    public void setEnrollment(Enrollment enrollment) {
        this.enrollment = enrollment;
    }

    public String getFinanceEventId() {
        return financeEventId;
    }

    public void setFinanceEventId(String financeEventId) {
        this.financeEventId = financeEventId;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
