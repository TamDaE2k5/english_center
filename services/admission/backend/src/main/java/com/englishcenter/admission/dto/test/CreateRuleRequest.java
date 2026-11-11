package com.englishcenter.admission.dto.test;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO yêu cầu cấu hình quy tắc đề xuất khóa học theo thang điểm (FR-TST-05, TASK-ADM-32).
 */
public class CreateRuleRequest {

    @NotNull(message = "ID bộ đề thi không được để trống")
    private Integer testSetId;

    @NotNull(message = "Điểm tối thiểu không được để trống")
    @DecimalMin(value = "0.0", message = "Điểm tối thiểu phải >= 0")
    private BigDecimal minScore;

    @NotNull(message = "Điểm tối đa không được để trống")
    @DecimalMin(value = "0.0", message = "Điểm tối đa phải >= 0")
    private BigDecimal maxScore;

    @NotBlank(message = "Trình độ đánh giá không được để trống")
    @Size(max = 50, message = "Trình độ đánh giá không vượt quá 50 ký tự")
    private String assessedLevel; // BEGINNER, INTERMEDIATE, ADVANCED

    @NotNull(message = "ID khóa học đề xuất không được để trống")
    private Integer recommendedCourseId;

    @JsonAlias({"advice_note", "advice"})
    private String adviceText;

    public CreateRuleRequest() {
    }

    public CreateRuleRequest(Integer testSetId, BigDecimal minScore, BigDecimal maxScore,
                             String assessedLevel, Integer recommendedCourseId, String adviceText) {
        this.testSetId = testSetId;
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.assessedLevel = assessedLevel;
        this.recommendedCourseId = recommendedCourseId;
        this.adviceText = adviceText;
    }

    public Integer getTestSetId() {
        return testSetId;
    }

    public void setTestSetId(Integer testSetId) {
        this.testSetId = testSetId;
    }

    public BigDecimal getMinScore() {
        return minScore;
    }

    public void setMinScore(BigDecimal minScore) {
        this.minScore = minScore;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(BigDecimal maxScore) {
        this.maxScore = maxScore;
    }

    public String getAssessedLevel() {
        return assessedLevel;
    }

    public void setAssessedLevel(String assessedLevel) {
        this.assessedLevel = assessedLevel;
    }

    public Integer getRecommendedCourseId() {
        return recommendedCourseId;
    }

    public void setRecommendedCourseId(Integer recommendedCourseId) {
        this.recommendedCourseId = recommendedCourseId;
    }

    public String getAdviceText() {
        return adviceText;
    }

    public void setAdviceText(String adviceText) {
        this.adviceText = adviceText;
    }
}
