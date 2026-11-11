package com.englishcenter.admission.dto.test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO phản hồi kết quả chấm điểm bài thi khảo thí đầu vào và lộ trình đề xuất (FR-TST-04, FR-TST-05).
 * Tuân thủ quy ước snake_case khi tuần tự hóa sang JSON.
 */
public class TestResultResponse {

    private Integer submissionId;
    private Integer attemptId;
    private Integer testSetId;
    private String testTitle;
    private BigDecimal totalScore;
    private BigDecimal listeningScore;
    private BigDecimal readingScore;
    private BigDecimal grammarScore;
    private String assessedLevel;
    private RecommendationResultDTO.RecommendedCourseDTO recommendedCourse;
    private List<RecommendationResultDTO.UpcomingClassDTO> upcomingClasses;
    private LocalDateTime submittedAt;

    public TestResultResponse() {
    }

    public TestResultResponse(Integer submissionId, Integer attemptId, Integer testSetId,
                              String testTitle, BigDecimal totalScore, BigDecimal listeningScore,
                              BigDecimal readingScore, BigDecimal grammarScore, String assessedLevel,
                              RecommendationResultDTO.RecommendedCourseDTO recommendedCourse,
                              List<RecommendationResultDTO.UpcomingClassDTO> upcomingClasses,
                              LocalDateTime submittedAt) {
        this.submissionId = submissionId;
        this.attemptId = attemptId;
        this.testSetId = testSetId;
        this.testTitle = testTitle;
        this.totalScore = totalScore;
        this.listeningScore = listeningScore;
        this.readingScore = readingScore;
        this.grammarScore = grammarScore;
        this.assessedLevel = assessedLevel;
        this.recommendedCourse = recommendedCourse;
        this.upcomingClasses = upcomingClasses;
        this.submittedAt = submittedAt;
    }

    public Integer getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(Integer submissionId) {
        this.submissionId = submissionId;
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

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(BigDecimal totalScore) {
        this.totalScore = totalScore;
    }

    public BigDecimal getListeningScore() {
        return listeningScore;
    }

    public void setListeningScore(BigDecimal listeningScore) {
        this.listeningScore = listeningScore;
    }

    public BigDecimal getReadingScore() {
        return readingScore;
    }

    public void setReadingScore(BigDecimal readingScore) {
        this.readingScore = readingScore;
    }

    public BigDecimal getGrammarScore() {
        return grammarScore;
    }

    public void setGrammarScore(BigDecimal grammarScore) {
        this.grammarScore = grammarScore;
    }

    public String getAssessedLevel() {
        return assessedLevel;
    }

    public void setAssessedLevel(String assessedLevel) {
        this.assessedLevel = assessedLevel;
    }

    public RecommendationResultDTO.RecommendedCourseDTO getRecommendedCourse() {
        return recommendedCourse;
    }

    public void setRecommendedCourse(RecommendationResultDTO.RecommendedCourseDTO recommendedCourse) {
        this.recommendedCourse = recommendedCourse;
    }

    public List<RecommendationResultDTO.UpcomingClassDTO> getUpcomingClasses() {
        return upcomingClasses;
    }

    public void setUpcomingClasses(List<RecommendationResultDTO.UpcomingClassDTO> upcomingClasses) {
        this.upcomingClasses = upcomingClasses;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}
