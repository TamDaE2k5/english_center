package com.englishcenter.admission.service.impl;

import com.englishcenter.admission.common.exception.BusinessException;
import com.englishcenter.admission.common.exception.ErrorCode;
import com.englishcenter.admission.dto.test.*;
import com.englishcenter.admission.entity.*;
import com.englishcenter.admission.repository.*;
import com.englishcenter.admission.security.SecurityUtils;
import com.englishcenter.admission.service.PlacementTestService;
import com.englishcenter.admission.service.RecommendationService;
import com.englishcenter.admission.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Hiện thực động cơ khảo thí trực tuyến, theo dõi countdown và tự động chấm điểm 3 kỹ năng
 * (FR-TST-03, FR-TST-04, INV-TST-02, BI-FUN-01).
 */
@Service
public class PlacementTestServiceImpl implements PlacementTestService {

    private static final Logger log = LoggerFactory.getLogger(PlacementTestServiceImpl.class);

    private final TestSetRepository testSetRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final TestSubmissionRepository testSubmissionRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final RecommendationService recommendationService;
    private final ObjectMapper objectMapper;

    public PlacementTestServiceImpl(TestSetRepository testSetRepository,
                                  TestQuestionRepository testQuestionRepository,
                                  TestAttemptRepository testAttemptRepository,
                                  TestSubmissionRepository testSubmissionRepository,
                                  UserRepository userRepository,
                                  UserService userService,
                                  RecommendationService recommendationService,
                                  ObjectMapper objectMapper) {
        this.testSetRepository = testSetRepository;
        this.testQuestionRepository = testQuestionRepository;
        this.testAttemptRepository = testAttemptRepository;
        this.testSubmissionRepository = testSubmissionRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.recommendationService = recommendationService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public TakeTestResponse startTest(Integer testSetId, StartTestRequest request) {
        // 1. Kiểm tra bộ đề thi tồn tại và đang hoạt động
        TestSet testSet = testSetRepository.findById(testSetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_TEST_NOT_FOUND,
                        "Không tìm thấy bộ đề thi với ID: " + testSetId));

        if (!Boolean.TRUE.equals(testSet.getIsActive())) {
            throw new BusinessException(ErrorCode.ADM_VALIDATION_ERROR,
                    "Bộ đề thi hiện đang tạm khóa hoặc không mở cho thí sinh làm bài");
        }

        // 2. Xác định danh tính thí sinh (tự động tạo tài khoản nếu là thí sinh mới)
        User studentUser = resolveCandidateUser(request);

        // 3. Khởi tạo bản ghi lượt thi Countdown (INV-TST-02)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(testSet.getDurationMinutes());

        TestAttempt attempt = new TestAttempt();
        attempt.setUserId(studentUser.getId());
        attempt.setTestSetId(testSet.getId());
        attempt.setStatus("STARTED");
        attempt.setStartedAt(now);
        attempt.setExpiresAt(expiresAt);

        TestAttempt savedAttempt = testAttemptRepository.save(attempt);
        log.info("Thí sinh ID={} đã bắt đầu lượt thi attempt_id={}, bộ đề ID={}, hết hạn lúc {}",
                studentUser.getId(), savedAttempt.getId(), testSetId, expiresAt);

        // 4. Lấy danh sách câu hỏi và ẨN HOÀN TOÀN correct_option để bảo mật đề thi
        List<PublicQuestionResponse> publicQuestions = testQuestionRepository
                .findByTestSetIdOrderByDisplayOrderAsc(testSetId)
                .stream()
                .map(PublicQuestionResponse::fromEntity)
                .collect(Collectors.toList());

        return new TakeTestResponse(
                savedAttempt.getId(),
                testSet.getId(),
                testSet.getTitle(),
                testSet.getDurationMinutes(),
                now,
                expiresAt,
                publicQuestions
        );
    }

    @Override
    @Transactional
    public TestResultResponse submitAndGrade(SubmitTestRequest request) {
        // Bắt buộc có lượt thi do server cấp để không thể bỏ qua thời hạn countdown.
        if (request.getAttemptId() == null) {
            throw new BusinessException(ErrorCode.ADM_VALIDATION_ERROR, "Bắt buộc cung cấp attempt_id để nộp bài thi");
        }
        TestAttempt attempt = null;
        TestSet testSet = null;
        User studentUser = null;
        LocalDateTime now = LocalDateTime.now();

        // 1. Kiểm tra tính hợp lệ của lượt thi (nếu có attempt_id)
        if (request.getAttemptId() != null) {
            attempt = testAttemptRepository.findById(request.getAttemptId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ADM_ATTEMPT_EXPIRED,
                            "Không tìm thấy lượt thi với ID: " + request.getAttemptId()));

            // Chống nộp bài lần 2 cùng 1 lượt thi (INV-TST-02)
            if ("SUBMITTED".equalsIgnoreCase(attempt.getStatus())
                    || testSubmissionRepository.existsByAttemptId(attempt.getId())) {
                log.warn("Lượt thi ID={} đã nộp bài trước đó, từ chối nộp lại (INV-TST-02)", attempt.getId());
                throw new BusinessException(ErrorCode.ADM_TEST_ALREADY_SUBMITTED,
                        "Lượt thi này đã nộp bài trước đó, không thể nộp lại");
            }

            // Kiểm tra thời gian hết hạn làm bài của server
            if (now.isAfter(attempt.getExpiresAt())) {
                log.warn("Lượt thi ID={} đã hết thời gian (expires_at={})", attempt.getId(), attempt.getExpiresAt());
                attempt.setStatus("EXPIRED");
                testAttemptRepository.save(attempt);
                throw new BusinessException(ErrorCode.ADM_ATTEMPT_EXPIRED,
                        "Lượt thi đã hết thời gian làm bài quy định");
            }

            final Integer attemptUserId = attempt.getUserId();
            final Integer attemptTestSetId = attempt.getTestSetId();

            studentUser = userRepository.findById(attemptUserId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ADM_USER_NOT_FOUND,
                            "Không tìm thấy thí sinh với ID: " + attemptUserId));

            testSet = testSetRepository.findById(attemptTestSetId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ADM_TEST_NOT_FOUND,
                            "Không tìm thấy bộ đề thi với ID: " + attemptTestSetId));
        }

        // 2. Tải danh sách câu hỏi gốc và đáp án chuẩn từ DB
        List<TestQuestion> originalQuestions = testQuestionRepository.findByTestSetIdOrderByDisplayOrderAsc(testSet.getId());
        Map<Integer, TestQuestion> questionMap = originalQuestions.stream()
                .collect(Collectors.toMap(TestQuestion::getId, Function.identity()));

        // 3. Động cơ tự động chấm điểm và bóc tách 3 kỹ năng
        BigDecimal listeningScore = BigDecimal.ZERO;
        BigDecimal readingScore = BigDecimal.ZERO;
        BigDecimal grammarScore = BigDecimal.ZERO;
        BigDecimal totalScore = BigDecimal.ZERO;

        List<Map<String, Object>> answerDetails = new ArrayList<>();
        Set<Integer> answeredQuestionIds = new HashSet<>();

        if (request.getAnswers() != null) {
            for (SubmitAnswerItem item : request.getAnswers()) {
                // Từ chối toàn bộ payload nếu một câu xuất hiện hai lần; không ghi điểm sai.
                if (!answeredQuestionIds.add(item.getQuestionId())) {
                    throw new BusinessException(ErrorCode.ADM_VALIDATION_ERROR,
                            "Mỗi câu hỏi chỉ được trả lời một lần");
                }
                TestQuestion question = questionMap.get(item.getQuestionId());
                if (question == null) {
                    continue;
                }

                String selected = item.getSelectedOption() != null ? item.getSelectedOption().trim().toUpperCase() : "";
                boolean isCorrect = selected.equalsIgnoreCase(question.getCorrectOption());
                BigDecimal weight = question.getScoreWeight() != null ? question.getScoreWeight() : BigDecimal.ONE;

                if (isCorrect) {
                    totalScore = totalScore.add(weight);
                    String skill = question.getSkillType().toUpperCase();
                    if ("LISTENING".equals(skill)) {
                        listeningScore = listeningScore.add(weight);
                    } else if ("READING".equals(skill)) {
                        readingScore = readingScore.add(weight);
                    } else if ("GRAMMAR".equals(skill)) {
                        grammarScore = grammarScore.add(weight);
                    }
                }

                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("question_id", question.getId());
                detail.put("skill_type", question.getSkillType());
                detail.put("selected_option", selected);
                detail.put("is_correct", isCorrect);
                detail.put("score_weight", weight);
                answerDetails.add(detail);
            }
        }

        log.info("Chấm điểm bài thi thí sinh ID={}: Tổng={}, Nghe={}, Đọc={}, Ngữ pháp={}",
                studentUser.getId(), totalScore, listeningScore, readingScore, grammarScore);

        // 4. Kích hoạt Động cơ đề xuất khóa học phù hợp (TASK-ADM-32)
        RecommendationResultDTO recommendation = recommendationService.getRecommendation(testSet.getId(), totalScore);

        // 5. Lưu kết quả bài thi vào admission.test_submissions
        TestSubmission submission = new TestSubmission();
        submission.setAttemptId(attempt != null ? attempt.getId() : null);
        submission.setUserId(studentUser.getId());
        submission.setTestSetId(testSet.getId());
        submission.setFullName(studentUser.getFullName());
        submission.setEmail(studentUser.getEmail());
        submission.setPhone(studentUser.getPhone());
        submission.setListeningScore(listeningScore);
        submission.setReadingScore(readingScore);
        submission.setGrammarScore(grammarScore);
        submission.setTotalScore(totalScore);
        submission.setAssessedLevel(recommendation.getAssessedLevel());
        submission.setRecommendedCourseId(recommendation.getRecommendedCourse() != null
                ? recommendation.getRecommendedCourse().getCourseId()
                : null);

        try {
            submission.setAnswersDetail(objectMapper.writeValueAsString(answerDetails));
        } catch (JsonProcessingException e) {
            submission.setAnswersDetail("[]");
        }

        submission.setStartedAt(attempt != null ? attempt.getStartedAt() : now);
        submission.setSubmittedAt(now);

        TestSubmission savedSubmission = testSubmissionRepository.save(submission);

        // 6. Cập nhật trạng thái lượt thi sang SUBMITTED
        if (attempt != null) {
            attempt.setStatus("SUBMITTED");
            attempt.setSubmittedAt(now);
            testAttemptRepository.save(attempt);
        }

        log.info("Đã ghi nhận nộp bài thi thành công: submission_id={}, attempt_id={}, total_score={}",
                savedSubmission.getId(), savedSubmission.getAttemptId(), totalScore);

        return new TestResultResponse(
                savedSubmission.getId(),
                savedSubmission.getAttemptId(),
                testSet.getId(),
                testSet.getTitle(),
                totalScore,
                listeningScore,
                readingScore,
                grammarScore,
                recommendation.getAssessedLevel(),
                recommendation.getRecommendedCourse(),
                recommendation.getUpcomingClasses(),
                now
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TestResultResponse getSubmissionById(Integer submissionId) {
        TestSubmission submission = testSubmissionRepository.findByIdWithDetails(submissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_SUBMISSION_NOT_FOUND,
                        "Không tìm thấy kết quả bài thi với ID: " + submissionId));

        // Bảo vệ cách ly dữ liệu cá nhân (INV-SEC-01)
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        String currentRole = SecurityUtils.getCurrentUserRole();
        if ("STUDENT".equalsIgnoreCase(currentRole) && currentUserId != null
                && !currentUserId.equals(submission.getUserId())) {
            log.warn("Cảnh báo bảo mật: Thí sinh ID={} cố gắng xem kết quả bài thi của ID={}",
                    currentUserId, submission.getUserId());
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN,
                    "Bạn chỉ được xem kết quả bài thi của chính mình");
        }

        TestSet testSet = testSetRepository.findById(submission.getTestSetId()).orElse(null);
        String testTitle = testSet != null ? testSet.getTitle() : "Đề Khảo Thí Đầu Vào";

        RecommendationResultDTO rec = recommendationService.getRecommendation(
                submission.getTestSetId(), submission.getTotalScore()
        );

        return new TestResultResponse(
                submission.getId(),
                submission.getAttemptId(),
                submission.getTestSetId(),
                testTitle,
                submission.getTotalScore(),
                submission.getListeningScore(),
                submission.getReadingScore(),
                submission.getGrammarScore(),
                submission.getAssessedLevel(),
                rec.getRecommendedCourse(),
                rec.getUpcomingClasses(),
                submission.getSubmittedAt()
        );
    }

    /**
     * Xác định hoặc tự động tạo tài khoản học viên (STUDENT) phục vụ bài thi.
     */
    private User resolveCandidateUser(StartTestRequest request) {
        if (request != null && request.getUserId() != null) {
            return userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ADM_USER_NOT_FOUND,
                            "Không tìm thấy thí sinh với ID: " + request.getUserId()));
        }

        if (request != null && request.getEmail() != null && !request.getEmail().isBlank()) {
            return userService.getOrCreateStudentUser(
                    request.getEmail(),
                    request.getFullName() != null ? request.getFullName() : "Thí sinh Khảo thí",
                    request.getPhone()
            );
        }

        Integer currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId != null) {
            return userRepository.findById(currentUserId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ADM_USER_NOT_FOUND,
                            "Không tìm thấy thông tin thí sinh đăng nhập"));
        }

        throw new BusinessException(ErrorCode.ADM_VALIDATION_ERROR,
                "Bắt buộc cung cấp địa chỉ email hoặc đăng nhập để tham gia bài thi khảo thí");
    }
}
