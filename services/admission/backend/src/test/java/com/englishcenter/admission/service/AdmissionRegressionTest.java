package com.englishcenter.admission.service;

import com.englishcenter.admission.client.FinanceServiceClient;
import com.englishcenter.admission.common.exception.BusinessException;
import com.englishcenter.admission.common.exception.ErrorCode;
import com.englishcenter.admission.dto.test.*;
import com.englishcenter.admission.entity.*;
import com.englishcenter.admission.helper.StudentServiceSyncHelper;
import com.englishcenter.admission.repository.*;
import com.englishcenter.admission.service.impl.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Kiểm chứng trực tiếp service Java, không mô phỏng lại nghiệp vụ trong test. */
class AdmissionRegressionTest {
    private EnrollmentRepository enrollments;
    private ClassRepository classes;
    private EnrollmentServiceImpl enrollmentService;
    private TestSetRepository sets;
    private TestQuestionRepository questions;
    private TestAttemptRepository attempts;
    private TestSubmissionRepository submissions;
    private UserRepository users;
    private PlacementTestServiceImpl placement;

    /** Tạo các cổng dữ liệu giả lập; đối tượng được kiểm thử là service thật. */
    @BeforeEach
    void setUp() {
        enrollments = mock(EnrollmentRepository.class);
        classes = mock(ClassRepository.class);
        enrollmentService = new EnrollmentServiceImpl(enrollments, classes,
                mock(ProcessedFinanceEventRepository.class), mock(ClassService.class),
                mock(UserService.class), mock(FinanceServiceClient.class), mock(StudentServiceSyncHelper.class), mock(CashConfirmationService.class),
                mock(org.springframework.transaction.PlatformTransactionManager.class), mock(HoldExtensionAuditRepository.class));
        sets = mock(TestSetRepository.class);
        questions = mock(TestQuestionRepository.class);
        attempts = mock(TestAttemptRepository.class);
        submissions = mock(TestSubmissionRepository.class);
        users = mock(UserRepository.class);
        RecommendationService recommendations = mock(RecommendationService.class);
        when(recommendations.getRecommendation(anyInt(), any())).thenReturn(new RecommendationResultDTO());
        when(submissions.save(any())).thenAnswer(inv -> inv.getArgument(0));
        UserService candidates = mock(UserService.class);
        User candidate = new User(); candidate.setId(3);
        when(candidates.getOrCreateStudentUser(anyString(), anyString(), any())).thenReturn(candidate);
        placement = new PlacementTestServiceImpl(sets, questions, attempts, submissions, users,
                candidates, recommendations, new ObjectMapper());
    }

    /** Đơn EXPIRED phải bị từ chối trước mọi thao tác ghi hoặc đồng bộ. */
    @Test
    void cannotApproveExpiredEnrollment() {
        Enrollment e = new Enrollment();
        e.setId(1); e.setClassId(2); e.setStatus("EXPIRED");
        when(enrollments.findById(1)).thenReturn(Optional.of(e));
        ClassEntity c = new ClassEntity(); c.setId(2); c.setMaxStudents(10);
        when(classes.findByIdWithLock(2)).thenReturn(Optional.of(c));
        when(enrollments.save(any())).thenAnswer(inv -> inv.getArgument(0));
        BusinessException error = assertThrows(BusinessException.class, () -> enrollmentService.approveEnrollment(1));
        assertEquals(ErrorCode.ADM_INVALID_STATE_TRANSITION, error.getErrorCode());
        verify(enrollments, never()).save(any());
    }

    /** Một câu hỏi lặp trong payload không được cộng điểm nhiều lần. */
    @Test
    void rejectsDuplicateQuestionAnswers() {
        prepareAttempt();
        SubmitTestRequest request = request(1, List.of(new SubmitAnswerItem(7, "A"), new SubmitAnswerItem(7, "A")));
        BusinessException error = assertThrows(BusinessException.class, () -> placement.submitAndGrade(request));
        assertEquals(ErrorCode.ADM_VALIDATION_ERROR, error.getErrorCode());
        verify(submissions, never()).save(any());
    }

    /** Bỏ attempt_id không được phép bỏ qua thời gian do server quản lý. */
    @Test
    void requiresServerAttempt() {
        prepareAttempt();
        SubmitTestRequest request = request(null, List.of(new SubmitAnswerItem(7, "A")));
        BusinessException error = assertThrows(BusinessException.class, () -> placement.submitAndGrade(request));
        assertEquals(ErrorCode.ADM_VALIDATION_ERROR, error.getErrorCode());
        verify(submissions, never()).save(any());
        verifyNoInteractions(questions);
    }

    /** Payload hợp lệ vẫn được chấm đúng trọng số của câu hỏi. */
    @Test
    void gradesValidAttempt() {
        prepareAttempt();
        TestResultResponse result = placement.submitAndGrade(request(1, List.of(new SubmitAnswerItem(7, "A"))));
        assertEquals(0, BigDecimal.ONE.compareTo(result.getTotalScore()));
        verify(submissions).save(any());
    }

    /** Chuẩn bị lượt thi thật còn hạn với một câu hỏi một điểm. */
    private void prepareAttempt() {
        TestAttempt a = new TestAttempt();
        a.setId(1); a.setUserId(3); a.setTestSetId(2); a.setStatus("STARTED");
        a.setStartedAt(LocalDateTime.now()); a.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        when(attempts.findLockedById(1)).thenReturn(Optional.of(a));
        User u = new User(); u.setId(3);
        when(users.findById(3)).thenReturn(Optional.of(u));
        TestSet s = new TestSet(); s.setId(2);
        when(sets.findById(2)).thenReturn(Optional.of(s));
        TestQuestion q = new TestQuestion();
        q.setId(7); q.setCorrectOption("A"); q.setSkillType("GRAMMAR"); q.setScoreWeight(BigDecimal.ONE);
        when(questions.findByTestSetIdOrderByDisplayOrderAsc(2)).thenReturn(List.of(q));
    }

    /** Tạo yêu cầu nộp bài, giữ test_set_id để tái hiện đường đi bỏ countdown. */
    private SubmitTestRequest request(Integer attemptId, List<SubmitAnswerItem> answers) {
        return new SubmitTestRequest(attemptId, 2, "qa@example.test", "QA", null, answers);
    }
}
