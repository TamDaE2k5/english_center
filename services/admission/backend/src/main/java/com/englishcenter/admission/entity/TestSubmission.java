package com.englishcenter.admission.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Thực thể biểu diễn kết quả làm bài khảo thí đầu vào (bảng admission.test_submissions).
 * Lưu điểm số bóc tách 3 kỹ năng (Listening, Reading, Grammar), tổng điểm và khóa học đề xuất.
 */
@Entity
@Table(name = "test_submissions", schema = "admission")
public class TestSubmission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "attempt_id", unique = true)
    private Integer attemptId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", insertable = false, updatable = false)
    private TestAttempt attempt;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Column(name = "test_set_id", nullable = false)
    private Integer testSetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_set_id", insertable = false, updatable = false)
    private TestSet testSet;

    @Column(name = "full_name")
    private String fullName;

    @Column
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(name = "listening_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal listeningScore = BigDecimal.ZERO;

    @Column(name = "reading_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal readingScore = BigDecimal.ZERO;

    @Column(name = "grammar_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal grammarScore = BigDecimal.ZERO;

    @Column(name = "total_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal totalScore;

    @Column(name = "assessed_level", length = 50)
    private String assessedLevel;

    @Column(name = "recommended_course_id")
    private Integer recommendedCourseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommended_course_id", insertable = false, updatable = false)
    private Course recommendedCourse;

    @Column(name = "answers_detail", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String answersDetail;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt = LocalDateTime.now();

    public TestSubmission() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Integer attemptId) {
        this.attemptId = attemptId;
    }

    public TestAttempt getAttempt() {
        return attempt;
    }

    public void setAttempt(TestAttempt attempt) {
        this.attempt = attempt;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getTestSetId() {
        return testSetId;
    }

    public void setTestSetId(Integer testSetId) {
        this.testSetId = testSetId;
    }

    public TestSet getTestSet() {
        return testSet;
    }

    public void setTestSet(TestSet testSet) {
        this.testSet = testSet;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
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

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(BigDecimal totalScore) {
        this.totalScore = totalScore;
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

    public Course getRecommendedCourse() {
        return recommendedCourse;
    }

    public void setRecommendedCourse(Course recommendedCourse) {
        this.recommendedCourse = recommendedCourse;
    }

    public String getAnswersDetail() {
        return answersDetail;
    }

    public void setAnswersDetail(String answersDetail) {
        this.answersDetail = answersDetail;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}
