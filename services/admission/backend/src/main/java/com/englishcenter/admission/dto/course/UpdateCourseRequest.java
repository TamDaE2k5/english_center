package com.englishcenter.admission.dto.course;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO yêu cầu cập nhật thông tin khóa học.
 */
public class UpdateCourseRequest {

    @NotBlank(message = "Tên khóa học không được để trống")
    @Size(max = 255, message = "Tên khóa học không vượt quá 255 ký tự")
    @JsonAlias({"title"})
    private String name;

    private String description;

    @NotBlank(message = "Trình độ khóa học không được để trống")
    @Size(max = 50, message = "Trình độ không vượt quá 50 ký tự")
    private String level;

    @Size(max = 100, message = "Chuẩn đầu ra không vượt quá 100 ký tự")
    private String targetOutput;

    @NotNull(message = "Thời lượng khóa học (tuần) không được để trống")
    @Min(value = 1, message = "Thời lượng khóa học phải từ 1 tuần trở lên")
    private Integer durationWeeks;

    @NotNull(message = "Học phí niêm yết không được để trống")
    @DecimalMin(value = "0.0", inclusive = true, message = "Học phí niêm yết phải >= 0")
    @JsonAlias({"tuition_fee"})
    private BigDecimal tuitionAmount;

    private Integer prerequisiteCourseId;

    private Boolean isActive;

    public UpdateCourseRequest() {
    }

    public UpdateCourseRequest(String name, String level, Integer durationWeeks, BigDecimal tuitionAmount,
                               Integer prerequisiteCourseId, String description, Boolean isActive) {
        this.name = name;
        this.level = level;
        this.durationWeeks = durationWeeks;
        this.tuitionAmount = tuitionAmount;
        this.prerequisiteCourseId = prerequisiteCourseId;
        this.description = description;
        this.isActive = isActive;
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
