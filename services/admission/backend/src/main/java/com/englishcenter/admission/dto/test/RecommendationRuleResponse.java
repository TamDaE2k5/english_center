package com.englishcenter.admission.dto.test;

import com.englishcenter.admission.entity.TestRecommendationRule;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO phản hồi thông tin quy tắc đề xuất khóa học.
 * Tuân thủ quy ước snake_case khi tuần tự hóa sang JSON.
 */
public class RecommendationRuleResponse {

    private Integer id;
    private Integer testSetId;
    private BigDecimal minScore;
    private BigDecimal maxScore;
    private String assessedLevel;
    private Integer recommendedCourseId;
    private String recommendedCourseName;
    private String recommendedCourseCode;
    private String adviceText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RecommendationRuleResponse() {
    }

    public static RecommendationRuleResponse fromEntity(TestRecommendationRule entity) {
        if (entity == null) {
            return null;
        }
        RecommendationRuleResponse res = new RecommendationRuleResponse();
        res.id = entity.getId();
        res.testSetId = entity.getTestSetId();
        res.minScore = entity.getMinScore();
        res.maxScore = entity.getMaxScore();
        res.assessedLevel = entity.getAssessedLevel();
        res.recommendedCourseId = entity.getRecommendedCourseId();
        if (entity.getRecommendedCourse() != null) {
            res.recommendedCourseName = entity.getRecommendedCourse().getName();
            res.recommendedCourseCode = entity.getRecommendedCourse().getCode();
        }
        res.adviceText = entity.getAdviceText() != null ? entity.getAdviceText() : entity.getAdviceNote();
        res.createdAt = entity.getCreatedAt();
        res.updatedAt = entity.getUpdatedAt();
        return res;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public String getRecommendedCourseName() {
        return recommendedCourseName;
    }

    public void setRecommendedCourseName(String recommendedCourseName) {
        this.recommendedCourseName = recommendedCourseName;
    }

    public String getRecommendedCourseCode() {
        return recommendedCourseCode;
    }

    public void setRecommendedCourseCode(String recommendedCourseCode) {
        this.recommendedCourseCode = recommendedCourseCode;
    }

    public String getAdviceText() {
        return adviceText;
    }

    public void setAdviceText(String adviceText) {
        this.adviceText = adviceText;
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
