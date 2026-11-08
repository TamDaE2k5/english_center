package com.englishcenter.admission.dto.test;

import com.englishcenter.admission.entity.TestQuestion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO phản hồi chi tiết câu hỏi trắc nghiệm (Dành cho Quản trị viên/Học vụ - Có chứa correct_option).
 * Tuân thủ quy ước snake_case khi tuần tự hóa sang JSON.
 */
public class QuestionResponse {

    private Integer id;
    private Integer testSetId;
    private String skillType;
    private Integer displayOrder;
    private String audioUrl;
    private String passageText;
    private String questionText;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String correctOption;
    private BigDecimal scoreWeight;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public QuestionResponse() {
    }

    public static QuestionResponse fromEntity(TestQuestion entity) {
        if (entity == null) {
            return null;
        }
        QuestionResponse res = new QuestionResponse();
        res.id = entity.getId();
        res.testSetId = entity.getTestSetId();
        res.skillType = entity.getSkillType();
        res.displayOrder = entity.getDisplayOrder();
        res.audioUrl = entity.getAudioUrl();
        res.passageText = entity.getPassageText();
        res.questionText = entity.getQuestionText();
        res.optionA = entity.getOptionA();
        res.optionB = entity.getOptionB();
        res.optionC = entity.getOptionC();
        res.optionD = entity.getOptionD();
        res.correctOption = entity.getCorrectOption();
        res.scoreWeight = entity.getScoreWeight();
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

    public String getSkillType() {
        return skillType;
    }

    public void setSkillType(String skillType) {
        this.skillType = skillType;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getPassageText() {
        return passageText;
    }

    public void setPassageText(String passageText) {
        this.passageText = passageText;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getOptionA() {
        return optionA;
    }

    public void setOptionA(String optionA) {
        this.optionA = optionA;
    }

    public String getOptionB() {
        return optionB;
    }

    public void setOptionB(String optionB) {
        this.optionB = optionB;
    }

    public String getOptionC() {
        return optionC;
    }

    public void setOptionC(String optionC) {
        this.optionC = optionC;
    }

    public String getOptionD() {
        return optionD;
    }

    public void setOptionD(String optionD) {
        this.optionD = optionD;
    }

    public String getCorrectOption() {
        return correctOption;
    }

    public void setCorrectOption(String correctOption) {
        this.correctOption = correctOption;
    }

    public BigDecimal getScoreWeight() {
        return scoreWeight;
    }

    public void setScoreWeight(BigDecimal scoreWeight) {
        this.scoreWeight = scoreWeight;
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
