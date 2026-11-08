package com.englishcenter.admission.dto.test;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * DTO từng câu trả lời trắc nghiệm của thí sinh.
 */
public class SubmitAnswerItem {

    @NotNull(message = "ID câu hỏi không được để trống")
    private Integer questionId;

    @NotBlank(message = "Đáp án lựa chọn không được để trống")
    @Pattern(regexp = "^[A-D]$", message = "Đáp án lựa chọn phải là A, B, C hoặc D")
    @JsonAlias({"selected", "option"})
    private String selectedOption;

    public SubmitAnswerItem() {
    }

    public SubmitAnswerItem(Integer questionId, String selectedOption) {
        this.questionId = questionId;
        this.selectedOption = selectedOption;
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
    }

    public String getSelectedOption() {
        return selectedOption;
    }

    public void setSelectedOption(String selectedOption) {
        this.selectedOption = selectedOption;
    }
}
