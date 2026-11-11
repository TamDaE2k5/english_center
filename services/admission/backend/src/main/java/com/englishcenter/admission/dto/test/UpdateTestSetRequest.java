package com.englishcenter.admission.dto.test;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO yêu cầu cập nhật thông tin bộ đề thi khảo thí.
 */
public class UpdateTestSetRequest {

    @NotBlank(message = "Tiêu đề bộ đề không được để trống")
    @Size(max = 255, message = "Tiêu đề không vượt quá 255 ký tự")
    private String title;

    private String description;

    @NotNull(message = "Thời lượng làm bài không được để trống")
    @Min(value = 1, message = "Thời lượng làm bài phải từ 1 phút trở lên")
    private Integer durationMinutes;

    @DecimalMin(value = "1.0", message = "Tổng điểm tối đa phải lớn hơn 0")
    private BigDecimal totalMaxScore;

    private Boolean isActive;

    public UpdateTestSetRequest() {
    }

    public UpdateTestSetRequest(String title, String description, Integer durationMinutes,
                                BigDecimal totalMaxScore, Boolean isActive) {
        this.title = title;
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.totalMaxScore = totalMaxScore;
        this.isActive = isActive;
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
