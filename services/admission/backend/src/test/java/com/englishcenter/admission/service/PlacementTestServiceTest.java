package com.englishcenter.admission.service;

import com.englishcenter.admission.common.exception.BusinessException;
import com.englishcenter.admission.common.exception.ErrorCode;
import com.englishcenter.admission.dto.test.*;
import com.englishcenter.admission.entity.*;
import com.englishcenter.admission.repository.*;
import com.englishcenter.admission.service.impl.PlacementTestServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlacementTestServiceTest {

    @Mock
    private TestSetRepository testSetRepository;

    @Mock
    private TestQuestionRepository testQuestionRepository;

    @Mock
    private TestAttemptRepository testAttemptRepository;

    @Mock
    private TestSubmissionRepository testSubmissionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private RecommendationService recommendationService;

    private ObjectMapper objectMapper;
    private PlacementTestServiceImpl placementTestService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        placementTestService = new PlacementTestServiceImpl(
                testSetRepository,
                testQuestionRepository,
                testAttemptRepository,
                testSubmissionRepository,
                userRepository,
                userService,
                recommendationService,
                objectMapper
        );
    }

    @Test
    @DisplayName("Bắt đầu lượt thi thành công và ẩn hoàn toàn đáp án đúng khỏi response (INV-TST-02)")
    void startTest_Success_HidesCorrectOption() {
        TestSet testSet = new TestSet();
        testSet.setId(1);
        testSet.setTitle("Đề Thi Đầu Vào Test");
        testSet.setDurationMinutes(45);
        testSet.setIsActive(true);

        when(testSetRepository.findById(1)).thenReturn(Optional.of(testSet));

        User student = new User();
        student.setId(42);
        student.setEmail("student@test.com");
        when(userService.getOrCreateStudentUser(anyString(), any(), any())).thenReturn(student);

        TestAttempt attempt = new TestAttempt();
        attempt.setId(1001);
        attempt.setUserId(42);
        attempt.setTestSetId(1);
        attempt.setStatus("STARTED");
        attempt.setStartedAt(LocalDateTime.now());
        attempt.setExpiresAt(LocalDateTime.now().plusMinutes(45));

        when(testAttemptRepository.save(any(TestAttempt.class))).thenReturn(attempt);

        TestQuestion q1 = new TestQuestion();
        q1.setId(11);
        q1.setTestSetId(1);
        q1.setSkillType("LISTENING");
        q1.setQuestionText("Where is the station?");
        q1.setOptionA("Left");
        q1.setOptionB("Right");
        q1.setOptionC("Straight");
        q1.setOptionD("Behind");
        q1.setCorrectOption("C"); // Đáp án C bí mật!
        q1.setScoreWeight(new BigDecimal("2.0"));

        when(testQuestionRepository.findByTestSetIdOrderByDisplayOrderAsc(1)).thenReturn(List.of(q1));

        StartTestRequest request = new StartTestRequest(null, "student@test.com", "Học Viên", "0900000000");
        TakeTestResponse response = placementTestService.startTest(1, request);

        assertNotNull(response);
        assertEquals(1001, response.getAttemptId());
        assertEquals(1, response.getQuestions().size());

        PublicQuestionResponse pubQ = response.getQuestions().get(0);
        assertEquals(11, pubQ.getId());
        assertEquals("Where is the station?", pubQ.getQuestionText());
        // Khẳng định đáp án bí mật không bao giờ xuất hiện ở DTO công khai
        assertFalse(pubQ.toString().contains("correctOption"));
    }

    @Test
    @DisplayName("Nộp bài thi trắc nghiệm và chấm điểm chính xác bóc tách 3 kỹ năng (Listening, Reading, Grammar)")
    void submitAndGrade_Success_CalculatesThreeSkills() {
        TestAttempt attempt = new TestAttempt();
        attempt.setId(500);
        attempt.setUserId(42);
        attempt.setTestSetId(1);
        attempt.setStatus("STARTED");
        attempt.setExpiresAt(LocalDateTime.now().plusMinutes(30)); // Chưa hết hạn

        when(testAttemptRepository.findById(500)).thenReturn(Optional.of(attempt));
        when(testSubmissionRepository.existsByAttemptId(500)).thenReturn(false);

        User student = new User();
        student.setId(42);
        student.setFullName("Nguyen Van A");
        student.setEmail("nva@test.com");
        when(userRepository.findById(42)).thenReturn(Optional.of(student));

        TestSet testSet = new TestSet();
        testSet.setId(1);
        testSet.setTitle("Đề Khảo Thí Chuẩn");
        when(testSetRepository.findById(1)).thenReturn(Optional.of(testSet));

        // 3 câu hỏi thuộc 3 kỹ năng
        TestQuestion qListen = new TestQuestion();
        qListen.setId(1);
        qListen.setSkillType("LISTENING");
        qListen.setCorrectOption("A");
        qListen.setScoreWeight(new BigDecimal("30.0"));

        TestQuestion qRead = new TestQuestion();
        qRead.setId(2);
        qRead.setSkillType("READING");
        qRead.setCorrectOption("B");
        qRead.setScoreWeight(new BigDecimal("40.0"));

        TestQuestion qGram = new TestQuestion();
        qGram.setId(3);
        qGram.setSkillType("GRAMMAR");
        qGram.setCorrectOption("C");
        qGram.setScoreWeight(new BigDecimal("30.0"));

        when(testQuestionRepository.findByTestSetIdOrderByDisplayOrderAsc(1))
                .thenReturn(List.of(qListen, qRead, qGram));

        RecommendationResultDTO rec = new RecommendationResultDTO(
                new BigDecimal("70.0"), "INTERMEDIATE", null, List.of()
        );
        when(recommendationService.getRecommendation(eq(1), any())).thenReturn(rec);

        when(testSubmissionRepository.save(any(TestSubmission.class))).thenAnswer(i -> {
            TestSubmission s = i.getArgument(0);
            s.setId(999);
            return s;
        });

        // Thí sinh trả lời đúng câu 1 (LISTENING) và câu 2 (READING), trả lời sai câu 3
        SubmitTestRequest submitReq = new SubmitTestRequest(
                500, 1, "nva@test.com", "Nguyen Van A", "0912345678",
                List.of(
                        new SubmitAnswerItem(1, "A"), // Đúng -> +30
                        new SubmitAnswerItem(2, "B"), // Đúng -> +40
                        new SubmitAnswerItem(3, "D")  // Sai (đúng là C) -> +0
                )
        );

        TestResultResponse result = placementTestService.submitAndGrade(submitReq);

        assertNotNull(result);
        assertEquals(0, new BigDecimal("70.0").compareTo(result.getTotalScore())); // 30 + 40 = 70
        assertEquals(0, new BigDecimal("30.0").compareTo(result.getListeningScore()));
        assertEquals(0, new BigDecimal("40.0").compareTo(result.getReadingScore()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getGrammarScore()));
        assertEquals("INTERMEDIATE", result.getAssessedLevel());
        assertEquals("SUBMITTED", attempt.getStatus());
        verify(testAttemptRepository, times(1)).save(attempt);
    }

    @Test
    @DisplayName("Nộp bài lần thứ 2 của cùng một lượt thi ném mã lỗi 400 ADM_TEST_ALREADY_SUBMITTED (INV-TST-02)")
    void submitAndGrade_AlreadySubmitted_ThrowsConflict() {
        TestAttempt attempt = new TestAttempt();
        attempt.setId(501);
        attempt.setStatus("SUBMITTED"); // Đã nộp rồi!

        when(testAttemptRepository.findById(501)).thenReturn(Optional.of(attempt));

        SubmitTestRequest request = new SubmitTestRequest(501, 1, "a@b.com", "A", "090", List.of());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> placementTestService.submitAndGrade(request));

        assertEquals(ErrorCode.ADM_TEST_ALREADY_SUBMITTED, exception.getErrorCode());
        verify(testSubmissionRepository, never()).save(any(TestSubmission.class));
    }

    @Test
    @DisplayName("Nộp bài thi khi thời gian đã quá expires_at ném lỗi 400 ADM_ATTEMPT_EXPIRED")
    void submitAndGrade_Expired_ThrowsException() {
        TestAttempt attempt = new TestAttempt();
        attempt.setId(502);
        attempt.setStatus("STARTED");
        attempt.setExpiresAt(LocalDateTime.now().minusMinutes(5)); // Đã quá hạn 5 phút!

        when(testAttemptRepository.findById(502)).thenReturn(Optional.of(attempt));

        SubmitTestRequest request = new SubmitTestRequest(502, 1, "a@b.com", "A", "090", List.of());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> placementTestService.submitAndGrade(request));

        assertEquals(ErrorCode.ADM_ATTEMPT_EXPIRED, exception.getErrorCode());
        assertEquals("EXPIRED", attempt.getStatus());
        verify(testAttemptRepository, times(1)).save(attempt);
    }
}
