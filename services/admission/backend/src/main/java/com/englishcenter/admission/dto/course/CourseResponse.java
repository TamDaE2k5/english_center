package com.englishcenter.admission.dto.course;

import com.englishcenter.admission.entity.Course;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO phản hồi chi tiết thông tin khóa học.
 * Tuân thủ quy ước snake_case khi tuần tự hóa sang JSON.
 */
public class CourseResponse {

    private Integer id;
    private String code;
    private String name;
    private String description;
    private String level;
    private String targetOutput;
    private Integer durationWeeks;
    private BigDecimal tuitionAmount;
    private Integer prerequisiteCourseId;
    private String prerequisiteCourseName;
    private String prerequisiteCourseCode;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CourseResponse() {
    }

    public CourseResponse(Integer id, String code, String name, String description, String level,
                          String targetOutput, Integer durationWeeks, BigDecimal tuitionAmount,
                          Integer prerequisiteCourseId, String prerequisiteCourseName,
                          String prerequisiteCourseCode, Boolean isActive,
                          LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.level = level;
        this.targetOutput = targetOutput;
        this.durationWeeks = durationWeeks;
        this.tuitionAmount = tuitionAmount;
        this.prerequisiteCourseId = prerequisiteCourseId;
        this.prerequisiteCourseName = prerequisiteCourseName;
        this.prerequisiteCourseCode = prerequisiteCourseCode;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Chuyển đổi từ Course Entity sang CourseResponse DTO.
     *
     * @param course Thực thể Course
     * @return DTO CourseResponse
     */
    public static CourseResponse fromEntity(Course course) {
        if (course == null) {
            return null;
        }

        String prereqName = null;
        String prereqCode = null;
        if (course.getPrerequisiteCourse() != null) {
            prereqName = course.getPrerequisiteCourse().getName();
            prereqCode = course.getPrerequisiteCourse().getCode();
        }

        return new CourseResponse(
                course.getId(),
                course.getCode(),
                course.getName(),
                course.getDescription(),
                course.getLevel(),
                course.getTargetOutput(),
                course.getDurationWeeks(),
                course.getTuitionAmount(),
                course.getPrerequisiteCourseId(),
                prereqName,
                prereqCode,
                course.getIsActive(),
                course.getCreatedAt(),
                course.getUpdatedAt()
        );
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

    public String getPrerequisiteCourseName() {
        return prerequisiteCourseName;
    }

    public void setPrerequisiteCourseName(String prerequisiteCourseName) {
        this.prerequisiteCourseName = prerequisiteCourseName;
    }

    public String getPrerequisiteCourseCode() {
        return prerequisiteCourseCode;
    }

    public void setPrerequisiteCourseCode(String prerequisiteCourseCode) {
        this.prerequisiteCourseCode = prerequisiteCourseCode;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
