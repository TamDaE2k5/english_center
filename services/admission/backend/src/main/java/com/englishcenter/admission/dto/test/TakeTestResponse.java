package com.englishcenter.admission.dto.test;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO dữ liệu đề thi trả về cho thí sinh khi bắt đầu làm bài.
 * ĐẢM BẢO TUYỆT ĐỐI KHÔNG ĐỂ LỘ ĐÁP ÁN ĐÚNG.
 * Tuân thủ quy ước snake_case khi tuần tự hóa sang JSON.
 */
public class TakeTestResponse {

    private Integer attemptId;
    private String attemptToken;

    /** Token riêng cho thao tác nộp lượt thi, không phải token đăng nhập. */
    public String getAttemptToken() { return attemptToken; }
    public void setAttemptToken(String attemptToken) { this.attemptToken = attemptToken; }
    private Integer testSetId;
    private String testTitle;
    private Integer durationMinutes;
    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime serverNow;
    private String status;
    private List<PublicQuestionResponse> questions;

    public TakeTestResponse() {
    }

    public TakeTestResponse(Integer attemptId, Integer testSetId, String testTitle,
                            Integer durationMinutes, LocalDateTime startedAt,
                            LocalDateTime expiresAt, List<PublicQuestionResponse> questions) {
        this.attemptId = attemptId;
        this.testSetId = testSetId;
        this.testTitle = testTitle;
        this.durationMinutes = durationMinutes;
        this.startedAt = startedAt;
        this.expiresAt = expiresAt;
        this.questions = questions;
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

    public String getTestTitle() {
        return testTitle;
    }

    public void setTestTitle(String testTitle) {
        this.testTitle = testTitle;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    /** Giờ máy chủ tại lúc chuẩn bị trả lượt thi, cùng quy ước với expires_at. */
    public LocalDateTime getServerNow() {
        return serverNow;
    }

    public void setServerNow(LocalDateTime serverNow) {
        this.serverNow = serverNow;
    }

    /** Trạng thái lượt thi tại thời điểm server_now. */
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<PublicQuestionResponse> getQuestions() {
        return questions;
    }

    public void setQuestions(List<PublicQuestionResponse> questions) {
        this.questions = questions;
    }
}
