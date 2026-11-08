package com.englishcenter.admission.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Thực thể biểu diễn khóa học trong phân hệ Admission (bảng admission.courses).
 * Chứa mã khóa học duy nhất (INV-ADM-02), học phí niêm yết, thời lượng và khóa học tiên quyết.
 */
@Entity
@Table(name = "courses", schema = "admission")
public class Course extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 50)
    private String level; // BEGINNER, INTERMEDIATE, ADVANCED

    @Column(name = "target_output", length = 100)
    private String targetOutput;

    @Column(name = "duration_weeks", nullable = false)
    private Integer durationWeeks;

    @Column(name = "tuition_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal tuitionAmount = BigDecimal.ZERO;

    @Column(name = "prerequisite_course_id")
    private Integer prerequisiteCourseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prerequisite_course_id", insertable = false, updatable = false)
    private Course prerequisiteCourse;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public Course() {
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getTargetOutput() {
        return targetOutput;
    }

    public void setTargetOutput(String targetOutput) {
        this.targetOutput = targetOutput;
    }

    public Integer getDurationWeeks() {
        return durationWeeks;
    }

    public void setDurationWeeks(Integer durationWeeks) {
        this.durationWeeks = durationWeeks;
    }

    public BigDecimal getTuitionAmount() {
        return tuitionAmount;
    }

    public void setTuitionAmount(BigDecimal tuitionAmount) {
        this.tuitionAmount = tuitionAmount;
    }

    public Integer getPrerequisiteCourseId() {
        return prerequisiteCourseId;
    }

    public void setPrerequisiteCourseId(Integer prerequisiteCourseId) {
        this.prerequisiteCourseId = prerequisiteCourseId;
    }

    public Course getPrerequisiteCourse() {
        return prerequisiteCourse;
    }

    public void setPrerequisiteCourse(Course prerequisiteCourse) {
        this.prerequisiteCourse = prerequisiteCourse;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
