package com.englishcenter.admission.service.impl;

import com.englishcenter.admission.common.exception.BusinessException;
import com.englishcenter.admission.common.exception.ErrorCode;
import com.englishcenter.admission.dto.test.*;
import com.englishcenter.admission.entity.TestQuestion;
import com.englishcenter.admission.entity.TestSet;
import com.englishcenter.admission.repository.TestQuestionRepository;
import com.englishcenter.admission.repository.TestSetRepository;
import com.englishcenter.admission.service.TestManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Hiện thực nghiệp vụ quản lý ngân hàng bộ đề và câu hỏi khảo thí 3 kỹ năng (FR-TST-01, FR-TST-02).
 */
@Service
public class TestManagementServiceImpl implements TestManagementService {

    private static final Logger log = LoggerFactory.getLogger(TestManagementServiceImpl.class);

    private final TestSetRepository testSetRepository;
    private final TestQuestionRepository testQuestionRepository;

    public TestManagementServiceImpl(TestSetRepository testSetRepository,
                                    TestQuestionRepository testQuestionRepository) {
        this.testSetRepository = testSetRepository;
        this.testQuestionRepository = testQuestionRepository;
    }

    @Override
    @Transactional
    public TestSetResponse createTestSet(CreateTestSetRequest request) {
        String code = request.getCode();
        if (code == null || code.isBlank()) {
            code = String.format("TEST-%s-%03d",
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")),
                    (int) (testSetRepository.count() + 1));
        } else {
            code = code.trim().toUpperCase();
            if (testSetRepository.existsByCode(code)) {
                log.warn("Mã bộ đề thi {} đã tồn tại", code);
                throw new BusinessException(ErrorCode.ADM_VALIDATION_ERROR,
                        "Mã bộ đề thi " + code + " đã tồn tại trong hệ thống");
            }
        }

        TestSet testSet = new TestSet();
        testSet.setCode(code);
        testSet.setTitle(request.getTitle().trim());
        testSet.setDescription(request.getDescription());
        testSet.setDurationMinutes(request.getDurationMinutes());
        testSet.setTotalMaxScore(request.getTotalMaxScore() != null ? request.getTotalMaxScore() : new BigDecimal("100.0"));
        testSet.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        TestSet saved = testSetRepository.save(testSet);
        log.info("Tạo mới bộ đề thi thành công: ID={}, code={}, duration={} phút",
                saved.getId(), saved.getCode(), saved.getDurationMinutes());

        return TestSetResponse.fromEntity(saved, 0L, List.of());
    }

    @Override
    @Transactional
    public TestSetResponse updateTestSet(Integer id, UpdateTestSetRequest request) {
        TestSet testSet = testSetRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_TEST_NOT_FOUND,
                        "Không tìm thấy bộ đề thi với ID: " + id));

        testSet.setTitle(request.getTitle().trim());
        testSet.setDescription(request.getDescription());
        testSet.setDurationMinutes(request.getDurationMinutes());
        if (request.getTotalMaxScore() != null) {
            testSet.setTotalMaxScore(request.getTotalMaxScore());
        }
        if (request.getIsActive() != null) {
            testSet.setIsActive(request.getIsActive());
        }

        TestSet updated = testSetRepository.save(testSet);
        log.info("Cập nhật thông tin bộ đề thi ID={} thành công", updated.getId());

        long count = testQuestionRepository.countByTestSetId(id);
        List<QuestionResponse> questions = testQuestionRepository.findByTestSetIdOrderByDisplayOrderAsc(id)
                .stream().map(QuestionResponse::fromEntity).collect(Collectors.toList());

        return TestSetResponse.fromEntity(updated, count, questions);
    }

    @Override
    @Transactional(readOnly = true)
    public TestSetResponse getTestSetById(Integer id) {
        TestSet testSet = testSetRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_TEST_NOT_FOUND,
                        "Không tìm thấy bộ đề thi với ID: " + id));

        long count = testQuestionRepository.countByTestSetId(id);
        List<QuestionResponse> questions = testQuestionRepository.findByTestSetIdOrderByDisplayOrderAsc(id)
                .stream().map(QuestionResponse::fromEntity).collect(Collectors.toList());

        return TestSetResponse.fromEntity(testSet, count, questions);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestSetResponse> listTestSets(Boolean isActive) {
        return testSetRepository.findTestSets(isActive)
                .stream()
                .map(ts -> {
                    long count = testQuestionRepository.countByTestSetId(ts.getId());
                    return TestSetResponse.fromEntity(ts, count, null);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public QuestionResponse addQuestion(Integer testSetId, CreateQuestionRequest request) {
        TestSet testSet = testSetRepository.findById(testSetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_TEST_NOT_FOUND,
                        "Không tìm thấy bộ đề thi với ID: " + testSetId));

        String skill = request.getSkillType().trim().toUpperCase();
        validateSkillSpecificFields(skill, request.getAudioUrl(), request.getPassageText());

        int nextOrder = request.getDisplayOrder() != null && request.getDisplayOrder() > 0
                ? request.getDisplayOrder()
                : (int) (testQuestionRepository.countByTestSetId(testSetId) + 1);

        TestQuestion question = new TestQuestion();
        question.setTestSetId(testSetId);
        question.setSkillType(skill);
        question.setDisplayOrder(nextOrder);
        question.setOrderIndex(nextOrder);
        question.setAudioUrl(request.getAudioUrl());
        question.setPassageText(request.getPassageText());
        question.setQuestionText(request.getQuestionText().trim());
        question.setOptionA(request.getOptionA().trim());
        question.setOptionB(request.getOptionB().trim());
        question.setOptionC(request.getOptionC().trim());
        question.setOptionD(request.getOptionD().trim());
        question.setCorrectOption(request.getCorrectOption().trim().toUpperCase());
        question.setScoreWeight(request.getScoreWeight() != null ? request.getScoreWeight() : new BigDecimal("1.0"));

        TestQuestion saved = testQuestionRepository.save(question);
        log.info("Thêm câu hỏi mới thành công: ID={}, test_set_id={}, skill={}, weight={}",
                saved.getId(), testSetId, saved.getSkillType(), saved.getScoreWeight());

        // Tự động cộng dồn và cập nhật điểm tối đa của bộ đề
        recalculateTotalScore(testSet);

        return QuestionResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public QuestionResponse updateQuestion(Integer questionId, UpdateQuestionRequest request) {
        TestQuestion question = testQuestionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_QUESTION_NOT_FOUND,
                        "Không tìm thấy câu hỏi với ID: " + questionId));

        if (request.getSkillType() != null) {
            String skill = request.getSkillType().trim().toUpperCase();
            String audio = request.getAudioUrl() != null ? request.getAudioUrl() : question.getAudioUrl();
            String passage = request.getPassageText() != null ? request.getPassageText() : question.getPassageText();
            validateSkillSpecificFields(skill, audio, passage);
            question.setSkillType(skill);
        }

        if (request.getAudioUrl() != null) question.setAudioUrl(request.getAudioUrl());
        if (request.getPassageText() != null) question.setPassageText(request.getPassageText());
        if (request.getQuestionText() != null) question.setQuestionText(request.getQuestionText().trim());
        if (request.getOptionA() != null) question.setOptionA(request.getOptionA().trim());
        if (request.getOptionB() != null) question.setOptionB(request.getOptionB().trim());
        if (request.getOptionC() != null) question.setOptionC(request.getOptionC().trim());
        if (request.getOptionD() != null) question.setOptionD(request.getOptionD().trim());
        if (request.getCorrectOption() != null) question.setCorrectOption(request.getCorrectOption().trim().toUpperCase());
        if (request.getScoreWeight() != null) question.setScoreWeight(request.getScoreWeight());
        if (request.getDisplayOrder() != null) {
            question.setDisplayOrder(request.getDisplayOrder());
            question.setOrderIndex(request.getDisplayOrder());
        }

        TestQuestion updated = testQuestionRepository.save(question);
        log.info("Cập nhật câu hỏi ID={} thành công", updated.getId());

        testSetRepository.findById(question.getTestSetId()).ifPresent(this::recalculateTotalScore);

        return QuestionResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteQuestion(Integer questionId) {
        TestQuestion question = testQuestionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_QUESTION_NOT_FOUND,
                        "Không tìm thấy câu hỏi với ID: " + questionId));

        Integer testSetId = question.getTestSetId();
        testQuestionRepository.delete(question);
        log.info("Đã xóa câu hỏi ID={} khỏi bộ đề ID={}", questionId, testSetId);

        testSetRepository.findById(testSetId).ifPresent(this::recalculateTotalScore);
    }

    /**
     * Kiểm tra trường dữ liệu bắt buộc theo từng kỹ năng:
     * - LISTENING bắt buộc có audio_url
     * - READING bắt buộc có passage_text
     */
    private void validateSkillSpecificFields(String skill, String audioUrl, String passageText) {
        if ("LISTENING".equalsIgnoreCase(skill)) {
            if (audioUrl == null || audioUrl.isBlank()) {
                throw new BusinessException(ErrorCode.ADM_VALIDATION_ERROR,
                        "Câu hỏi kỹ năng LISTENING bắt buộc phải cung cấp đường dẫn tệp âm thanh (audio_url)");
            }
        } else if ("READING".equalsIgnoreCase(skill)) {
            if (passageText == null || passageText.isBlank()) {
                throw new BusinessException(ErrorCode.ADM_VALIDATION_ERROR,
                        "Câu hỏi kỹ năng READING bắt buộc phải cung cấp văn bản đoạn đọc (passage_text)");
            }
        }
    }

    /**
     * Tự động tính toán lại totalMaxScore của bộ đề từ tổng trọng số điểm của tất cả câu hỏi.
     */
    private void recalculateTotalScore(TestSet testSet) {
        BigDecimal sum = testQuestionRepository.sumScoreWeightByTestSetId(testSet.getId());
        if (sum.compareTo(BigDecimal.ZERO) > 0) {
            testSet.setTotalMaxScore(sum);
            testSetRepository.save(testSet);
        }
    }
}
