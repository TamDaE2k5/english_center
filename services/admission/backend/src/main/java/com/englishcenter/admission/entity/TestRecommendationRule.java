package com.englishcenter.admission.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Thực thể biểu diễn quy tắc đề xuất khóa học theo thang điểm khảo thí (bảng admission.test_recommendation_rules).
 * Khớp nối điểm thi đầu vào với khóa học và lời khuyên lộ trình phù hợp.
 */
@Entity
@Table(name = "test_recommendation_rules", schema = "admission")
public class TestRecommendationRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "test_set_id", nullable = false)
    private Integer testSetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_set_id", insertable = false, updatable = false)
    private TestSet testSet;

    @Column(name = "min_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal minScore;

    @Column(name = "max_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxScore;

    @Column(name = "assessed_level", nullable = false, length = 50)
    private String assessedLevel;

    @Column(name = "recommended_course_id", nullable = false)
    private Integer recommendedCourseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommended_course_id", insertable = false, updatable = false)
    private Course recommendedCourse;

    @Column(name = "advice_text", columnDefinition = "TEXT")
    private String adviceText;

    @Column(name = "advice_note", columnDefinition = "TEXT")
    private String adviceNote;

    public TestRecommendationRule() {
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

    public TestSet getTestSet() {
        return testSet;
    }

    public void setTestSet(TestSet testSet) {
        this.testSet = testSet;
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

    public Course getRecommendedCourse() {
        return recommendedCourse;
    }

    public void setRecommendedCourse(Course recommendedCourse) {
        this.recommendedCourse = recommendedCourse;
    }

    public String getAdviceText() {
        return adviceText;
    }

    public void setAdviceText(String adviceText) {
        this.adviceText = adviceText;
        if (this.adviceNote == null) {
            this.adviceNote = adviceText;
        }
    }

    public String getAdviceNote() {
        return adviceNote;
    }

    public void setAdviceNote(String adviceNote) {
        this.adviceNote = adviceNote;
        if (this.adviceText == null) {
            this.adviceText = adviceNote;
        }
    }
}
