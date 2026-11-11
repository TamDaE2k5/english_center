package com.englishcenter.admission.dto.test;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * DTO yêu cầu nộp bài thi và chấm điểm tự động (FR-TST-04, INV-TST-02).
 */
public class SubmitTestRequest {

    private Integer attemptId;
    private String attemptToken;

    /** Bằng chứng sở hữu lượt thi dành cho khách chưa đăng nhập. */
    public String getAttemptToken() { return attemptToken; }
    public void setAttemptToken(String attemptToken) { this.attemptToken = attemptToken; }
    private Integer testSetId;
    private String email;
    private String fullName;
    private String phone;

    @NotEmpty(message = "Danh sách câu trả lời không được để trống")
    @Valid
    private List<SubmitAnswerItem> answers;

    public SubmitTestRequest() {
    }

    public SubmitTestRequest(Integer attemptId, Integer testSetId, String email,
                             String fullName, String phone, List<SubmitAnswerItem> answers) {
        this.attemptId = attemptId;
        this.testSetId = testSetId;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.answers = answers;
    }

    public Integer getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Integer attemptId) {
        this.attemptId = attemptId;
    }

    public Integer getTestSetId() {
        return testSetId;
    }

    public void setTestSetId(Integer testSetId) {
        this.testSetId = testSetId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public List<SubmitAnswerItem> getAnswers() {
        return answers;
    }

    public void setAnswers(List<SubmitAnswerItem> answers) {
        this.answers = answers;
    }
}
