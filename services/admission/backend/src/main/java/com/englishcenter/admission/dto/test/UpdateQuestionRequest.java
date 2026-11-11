package com.englishcenter.admission.dto.test;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * DTO yêu cầu cập nhật câu hỏi trắc nghiệm trong bộ đề.
 */
public class UpdateQuestionRequest {

    @JsonAlias({"skill"})
    private String skillType;

    private String audioUrl;
    private String passageText;
    private String questionText;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;

    @Pattern(regexp = "^[A-D]$", message = "Đáp án đúng phải là một trong các ký tự: A, B, C, D")
    private String correctOption;

    @DecimalMin(value = "0.1", message = "Trọng số điểm câu hỏi phải lớn hơn 0")
    private BigDecimal scoreWeight;

    @JsonAlias({"order_index"})
    private Integer displayOrder;

    public UpdateQuestionRequest() {
    }

    public String getSkillType() {
        return skillType;
    }

    public void setSkillType(String skillType) {
        this.skillType = skillType;
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

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
