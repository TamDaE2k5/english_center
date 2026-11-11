package com.englishcenter.admission.dto.test;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO yêu cầu tạo mới bộ đề thi khảo thí đầu vào (FR-TST-01).
 */
public class CreateTestSetRequest {

    @Size(max = 50, message = "Mã bộ đề không vượt quá 50 ký tự")
    private String code;

    @NotBlank(message = "Tiêu đề bộ đề không được để trống")
    @Size(max = 255, message = "Tiêu đề không vượt quá 255 ký tự")
    private String title;

    private String description;

    @NotNull(message = "Thời lượng làm bài không được để trống")
    @Min(value = 1, message = "Thời lượng làm bài phải từ 1 phút trở lên")
    private Integer durationMinutes = 45;

    @DecimalMin(value = "1.0", message = "Tổng điểm tối đa phải lớn hơn 0")
    private BigDecimal totalMaxScore = new BigDecimal("100.0");

    private Boolean isActive = true;

    public CreateTestSetRequest() {
    }

    public CreateTestSetRequest(String code, String title, String description,
                                Integer durationMinutes, BigDecimal totalMaxScore) {
        this.code = code;
        this.title = title;
        this.description = description;
        this.durationMinutes = durationMinutes != null ? durationMinutes : 45;
        this.totalMaxScore = totalMaxScore != null ? totalMaxScore : new BigDecimal("100.0");
        this.isActive = true;
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
