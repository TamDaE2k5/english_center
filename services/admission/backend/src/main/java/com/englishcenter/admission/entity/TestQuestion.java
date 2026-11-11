package com.englishcenter.admission.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Thực thể biểu diễn câu hỏi trắc nghiệm 3 kỹ năng trong bộ đề khảo thí (bảng admission.test_questions).
 * Bóc tách: LISTENING (audio_url), READING (passage_text), GRAMMAR.
 * Trường correct_option bị ẩn khỏi client khi làm bài trực tuyến.
 */
@Entity
@Table(name = "test_questions", schema = "admission")
public class TestQuestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "test_set_id", nullable = false)
    private Integer testSetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_set_id", insertable = false, updatable = false)
    private TestSet testSet;

    @Column(name = "skill_type", nullable = false, length = 50)
    private String skillType; // LISTENING, READING, GRAMMAR

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 1;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 1;

    @Column(name = "audio_url", columnDefinition = "TEXT")
    private String audioUrl;

    @Column(name = "passage_text", columnDefinition = "TEXT")
    private String passageText;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "option_a", nullable = false, columnDefinition = "TEXT")
    private String optionA;

    @Column(name = "option_b", nullable = false, columnDefinition = "TEXT")
    private String optionB;

    @Column(name = "option_c", nullable = false, columnDefinition = "TEXT")
    private String optionC;

    @Column(name = "option_d", nullable = false, columnDefinition = "TEXT")
    private String optionD;

    @Column(name = "correct_option", nullable = false, length = 1)
    private String correctOption; // A, B, C, D

    @Column(name = "score_weight", nullable = false, precision = 4, scale = 2)
    private BigDecimal scoreWeight = new BigDecimal("1.0");

    public TestQuestion() {
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

    public String getSkillType() {
        return skillType;
    }

    public void setSkillType(String skillType) {
        this.skillType = skillType;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
        if (this.displayOrder == null || this.displayOrder == 1) {
            this.displayOrder = orderIndex;
        }
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
        if (this.orderIndex == null || this.orderIndex == 1) {
            this.orderIndex = displayOrder;
        }
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
}
