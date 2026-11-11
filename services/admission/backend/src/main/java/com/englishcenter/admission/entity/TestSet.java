package com.englishcenter.admission.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Thực thể biểu diễn bộ đề thi khảo thí đầu vào trong phân hệ Admission (bảng admission.test_sets).
 * Quản lý thời lượng làm bài đếm ngược (duration_minutes) và tổng điểm tối đa (total_max_score).
 */
@Entity
@Table(name = "test_sets", schema = "admission")
public class TestSet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes = 45;

    @Column(name = "total_max_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal totalMaxScore = new BigDecimal("100.0");

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public TestSet() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public BigDecimal getTotalMaxScore() {
        return totalMaxScore;
    }

    public void setTotalMaxScore(BigDecimal totalMaxScore) {
        this.totalMaxScore = totalMaxScore;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
