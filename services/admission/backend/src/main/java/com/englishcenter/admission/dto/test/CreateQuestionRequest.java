package com.englishcenter.admission.dto.test;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO yêu cầu thêm mới câu hỏi trắc nghiệm 3 kỹ năng vào bộ đề (FR-TST-02).
 */
public class CreateQuestionRequest {

    @NotBlank(message = "Kỹ năng khảo thí không được để trống (LISTENING, READING, GRAMMAR)")
    @JsonAlias({"skill"})
    private String skillType;

    private String audioUrl;

    private String passageText;

    @NotBlank(message = "Nội dung câu hỏi không được để trống")
    private String questionText;

    @NotBlank(message = "Lựa chọn A không được để trống")
    private String optionA;

    @NotBlank(message = "Lựa chọn B không được để trống")
    private String optionB;

    @NotBlank(message = "Lựa chọn C không được để trống")
    private String optionC;

    @NotBlank(message = "Lựa chọn D không được để trống")
    private String optionD;

    @NotBlank(message = "Đáp án đúng không được để trống")
    @Pattern(regexp = "^[A-D]$", message = "Đáp án đúng phải là một trong các ký tự: A, B, C, D")
    private String correctOption;

    @DecimalMin(value = "0.1", message = "Trọng số điểm câu hỏi phải lớn hơn 0")
    private BigDecimal scoreWeight = new BigDecimal("1.0");

    @JsonAlias({"order_index"})
    private Integer displayOrder = 1;

    public CreateQuestionRequest() {
    }

    public CreateQuestionRequest(String skillType, String audioUrl, String passageText,
                                 String questionText, String optionA, String optionB,
                                 String optionC, String optionD, String correctOption,
                                 BigDecimal scoreWeight, Integer displayOrder) {
        this.skillType = skillType;
        this.audioUrl = audioUrl;
        this.passageText = passageText;
        this.questionText = questionText;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.correctOption = correctOption;
        this.scoreWeight = scoreWeight != null ? scoreWeight : new BigDecimal("1.0");
        this.displayOrder = displayOrder != null ? displayOrder : 1;
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
