package com.englishcenter.admission.service;

import com.englishcenter.admission.common.exception.BusinessException;
import com.englishcenter.admission.common.exception.ErrorCode;
import com.englishcenter.admission.dto.test.CreateQuestionRequest;
import com.englishcenter.admission.dto.test.CreateTestSetRequest;
import com.englishcenter.admission.dto.test.QuestionResponse;
import com.englishcenter.admission.dto.test.TestSetResponse;
import com.englishcenter.admission.entity.TestQuestion;
import com.englishcenter.admission.entity.TestSet;
import com.englishcenter.admission.repository.TestQuestionRepository;
import com.englishcenter.admission.repository.TestSetRepository;
import com.englishcenter.admission.service.impl.TestManagementServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestManagementServiceTest {

    @Mock
    private TestSetRepository testSetRepository;

    @Mock
    private TestQuestionRepository testQuestionRepository;

    private TestManagementServiceImpl testManagementService;

    @BeforeEach
    void setUp() {
        testManagementService = new TestManagementServiceImpl(testSetRepository, testQuestionRepository);
    }

    @Test
    @DisplayName("Tạo mới bộ đề thi khảo thí thành công")
    void createTestSet_Success() {
        CreateTestSetRequest request = new CreateTestSetRequest(
                "TEST-2026-A1", "Đề Khảo Thí Đợt 1", "Mô tả đề thi", 45, new BigDecimal("100.0")
        );

        when(testSetRepository.existsByCode("TEST-2026-A1")).thenReturn(false);

        TestSet saved = new TestSet();
        saved.setId(1);
        saved.setCode("TEST-2026-A1");
        saved.setTitle("Đề Khảo Thí Đợt 1");
        saved.setDurationMinutes(45);
        saved.setTotalMaxScore(new BigDecimal("100.0"));
        saved.setIsActive(true);

        when(testSetRepository.save(any(TestSet.class))).thenReturn(saved);

        TestSetResponse response = testManagementService.createTestSet(request);

        assertNotNull(response);
        assertEquals("TEST-2026-A1", response.getCode());
        assertEquals(45, response.getDurationMinutes());
        verify(testSetRepository, times(1)).save(any(TestSet.class));
    }

    @Test
    @DisplayName("Thêm câu hỏi LISTENING thành công khi có audio_url và tự động cộng dồn totalMaxScore")
    void addQuestion_Listening_Success() {
        TestSet testSet = new TestSet();
        testSet.setId(1);
        testSet.setTitle("Bộ đề 1");

        when(testSetRepository.findById(1)).thenReturn(Optional.of(testSet));

        CreateQuestionRequest request = new CreateQuestionRequest(
                "LISTENING", "https://cdn.englishcenter.edu.vn/audio/part1.mp3", null,
                "What time does the train leave?", "8:00", "8:30", "9:00", "9:30", "B",
                new BigDecimal("2.0"), 1
        );

        TestQuestion saved = new TestQuestion();
        saved.setId(101);
        saved.setTestSetId(1);
        saved.setSkillType("LISTENING");
        saved.setAudioUrl(request.getAudioUrl());
        saved.setQuestionText(request.getQuestionText());
        saved.setOptionA(request.getOptionA());
        saved.setOptionB(request.getOptionB());
        saved.setOptionC(request.getOptionC());
        saved.setOptionD(request.getOptionD());
        saved.setCorrectOption("B");
        saved.setScoreWeight(new BigDecimal("2.0"));
        saved.setDisplayOrder(1);

        when(testQuestionRepository.save(any(TestQuestion.class))).thenReturn(saved);
        when(testQuestionRepository.sumScoreWeightByTestSetId(1)).thenReturn(new BigDecimal("2.0"));

        QuestionResponse response = testManagementService.addQuestion(1, request);

        assertNotNull(response);
        assertEquals("LISTENING", response.getSkillType());
        assertEquals("B", response.getCorrectOption());
        assertEquals(new BigDecimal("2.0"), response.getScoreWeight());
        verify(testQuestionRepository, times(1)).save(any(TestQuestion.class));
    }

    @Test
    @DisplayName("Thêm câu hỏi LISTENING ném lỗi 400 khi thiếu audio_url")
    void addQuestion_Listening_MissingAudio_ThrowsValidation() {
        TestSet testSet = new TestSet();
        testSet.setId(1);
        when(testSetRepository.findById(1)).thenReturn(Optional.of(testSet));

        CreateQuestionRequest request = new CreateQuestionRequest(
                "LISTENING", null, null, // Thiếu audio_url!
                "What time does the train leave?", "8:00", "8:30", "9:00", "9:30", "B",
                new BigDecimal("2.0"), 1
        );

        BusinessException exception = assertThrows(BusinessException.class,
                () -> testManagementService.addQuestion(1, request));

        assertEquals(ErrorCode.ADM_VALIDATION_ERROR, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("audio_url"));
    }

    @Test
    @DisplayName("Thêm câu hỏi READING ném lỗi 400 khi thiếu passage_text")
    void addQuestion_Reading_MissingPassage_ThrowsValidation() {
        TestSet testSet = new TestSet();
        testSet.setId(1);
        when(testSetRepository.findById(1)).thenReturn(Optional.of(testSet));

        CreateQuestionRequest request = new CreateQuestionRequest(
                "READING", null, null, // Thiếu passage_text!
                "According to the passage, why did the company expand?", "A", "B", "C", "D", "A",
                new BigDecimal("2.0"), 1
        );

        BusinessException exception = assertThrows(BusinessException.class,
                () -> testManagementService.addQuestion(1, request));

        assertEquals(ErrorCode.ADM_VALIDATION_ERROR, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("passage_text"));
    }

    @Test
    @DisplayName("Xóa câu hỏi thành công và cập nhật lại điểm tối đa bộ đề")
    void deleteQuestion_Success() {
        TestQuestion question = new TestQuestion();
        question.setId(50);
        question.setTestSetId(2);

        TestSet testSet = new TestSet();
        testSet.setId(2);

        when(testQuestionRepository.findById(50)).thenReturn(Optional.of(question));
        when(testSetRepository.findById(2)).thenReturn(Optional.of(testSet));
        when(testQuestionRepository.sumScoreWeightByTestSetId(2)).thenReturn(new BigDecimal("80.0"));

        testManagementService.deleteQuestion(50);

        verify(testQuestionRepository, times(1)).delete(question);
        verify(testSetRepository, times(1)).save(testSet);
    }
}
