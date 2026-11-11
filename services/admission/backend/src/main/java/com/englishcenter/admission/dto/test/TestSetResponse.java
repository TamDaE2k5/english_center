package com.englishcenter.admission.dto.test;

import com.englishcenter.admission.entity.TestSet;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO phản hồi thông tin chi tiết bộ đề thi khảo thí đầu vào.
 * Tuân thủ quy ước snake_case khi tuần tự hóa sang JSON.
 */
public class TestSetResponse {

    private Integer id;
    private String code;
    private String title;
    private String description;
    private Integer durationMinutes;
    private BigDecimal totalMaxScore;
    private Boolean isActive;
    private Long questionCount;
    private List<QuestionResponse> questions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public TestSetResponse() {
    }

    public static TestSetResponse fromEntity(TestSet entity, long count, List<QuestionResponse> questions) {
        if (entity == null) {
            return null;
        }
        TestSetResponse res = new TestSetResponse();
        res.id = entity.getId();
        res.code = entity.getCode();
        res.title = entity.getTitle();
        res.description = entity.getDescription();
        res.durationMinutes = entity.getDurationMinutes();
        res.totalMaxScore = entity.getTotalMaxScore();
        res.isActive = entity.getIsActive();
        res.questionCount = count;
        res.questions = questions;
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

    public Long getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(Long questionCount) {
        this.questionCount = questionCount;
    }

    public List<QuestionResponse> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionResponse> questions) {
        this.questions = questions;
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
