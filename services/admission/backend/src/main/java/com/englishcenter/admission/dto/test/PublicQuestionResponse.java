package com.englishcenter.admission.dto.test;

import com.englishcenter.admission.entity.TestQuestion;

import java.math.BigDecimal;

/**
 * DTO câu hỏi thi công khai gửi tới thí sinh (TUYỆT ĐỐI KHÔNG CHỨA correct_option để chống lộ đề).
 * Tuân thủ quy ước snake_case khi tuần tự hóa sang JSON.
 */
public class PublicQuestionResponse {

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
    private BigDecimal scoreWeight;

    public PublicQuestionResponse() {
    }

    public static PublicQuestionResponse fromEntity(TestQuestion entity) {
        if (entity == null) {
            return null;
        }
        PublicQuestionResponse res = new PublicQuestionResponse();
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
        res.scoreWeight = entity.getScoreWeight();
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

    public BigDecimal getScoreWeight() {
        return scoreWeight;
    }

    public void setScoreWeight(BigDecimal scoreWeight) {
        this.scoreWeight = scoreWeight;
    }
}
