package com.englishcenter.admission.service.impl;

import com.englishcenter.admission.common.exception.BusinessException;
import com.englishcenter.admission.common.exception.ErrorCode;
import com.englishcenter.admission.dto.test.CreateRuleRequest;
import com.englishcenter.admission.dto.test.RecommendationResultDTO;
import com.englishcenter.admission.dto.test.RecommendationRuleResponse;
import com.englishcenter.admission.entity.ClassEntity;
import com.englishcenter.admission.entity.Course;
import com.englishcenter.admission.entity.TestRecommendationRule;
import com.englishcenter.admission.repository.*;
import com.englishcenter.admission.service.RecommendationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Hiện thực động cơ đề xuất khóa học theo điểm khảo thí đầu vào (FR-TST-05, TASK-ADM-32).
 */
@Service
public class RecommendationServiceImpl implements RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationServiceImpl.class);

    private final TestRecommendationRuleRepository ruleRepository;
    private final TestSetRepository testSetRepository;
    private final CourseRepository courseRepository;
    private final ClassRepository classRepository;
    private final EnrollmentRepository enrollmentRepository;

    public RecommendationServiceImpl(TestRecommendationRuleRepository ruleRepository,
                                     TestSetRepository testSetRepository,
                                     CourseRepository courseRepository,
                                     ClassRepository classRepository,
                                     EnrollmentRepository enrollmentRepository) {
        this.ruleRepository = ruleRepository;
        this.testSetRepository = testSetRepository;
        this.courseRepository = courseRepository;
        this.classRepository = classRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Override
    @Transactional
    public RecommendationRuleResponse createRule(CreateRuleRequest request) {
        if (request.getMaxScore().compareTo(request.getMinScore()) <= 0) {
            throw new BusinessException(ErrorCode.ADM_VALIDATION_ERROR,
                    "Điểm tối đa (max_score) phải lớn hơn điểm tối thiểu (min_score)");
        }

        // Kiểm tra bộ đề tồn tại
        if (!testSetRepository.existsById(request.getTestSetId())) {
            throw new BusinessException(ErrorCode.ADM_TEST_NOT_FOUND,
                    "Không tìm thấy bộ đề thi với ID: " + request.getTestSetId());
        }

        // Kiểm tra khóa học đề xuất tồn tại
        Course course = courseRepository.findById(request.getRecommendedCourseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_COURSE_NOT_FOUND,
                        "Không tìm thấy khóa học đề xuất với ID: " + request.getRecommendedCourseId()));

        // Kiểm tra chồng lấn dải điểm (Rule Overlap)
        List<TestRecommendationRule> overlaps = ruleRepository.findOverlappingRules(
                request.getTestSetId(), null, request.getMinScore(), request.getMaxScore()
        );
        if (!overlaps.isEmpty()) {
            TestRecommendationRule existing = overlaps.get(0);
            log.warn("Dải điểm [{} - {}] bị chồng lấn với rule ID={} [{} - {}]",
                    request.getMinScore(), request.getMaxScore(),
                    existing.getId(), existing.getMinScore(), existing.getMaxScore());
            throw new BusinessException(ErrorCode.ADM_RULE_OVERLAP,
                    String.format("Dải điểm [%s - %s] bị chồng lấn với quy tắc đã có (ID=%d: [%s - %s])",
                            request.getMinScore(), request.getMaxScore(),
                            existing.getId(), existing.getMinScore(), existing.getMaxScore()));
        }

        TestRecommendationRule rule = new TestRecommendationRule();
        rule.setTestSetId(request.getTestSetId());
        rule.setMinScore(request.getMinScore());
        rule.setMaxScore(request.getMaxScore());
        rule.setAssessedLevel(request.getAssessedLevel().trim().toUpperCase());
        rule.setRecommendedCourseId(course.getId());
        rule.setAdviceText(request.getAdviceText());
        rule.setAdviceNote(request.getAdviceText());

        TestRecommendationRule saved = ruleRepository.save(rule);
        log.info("Tạo mới quy tắc đề xuất thành công: ID={}, test_set_id={}, [{} - {}] -> course={}",
                saved.getId(), saved.getTestSetId(), saved.getMinScore(), saved.getMaxScore(), course.getCode());

        saved.setRecommendedCourse(course);
        return RecommendationRuleResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationRuleResponse> getRules(Integer testSetId) {
        return ruleRepository.findByTestSetIdOrderByMinScoreAsc(testSetId)
                .stream()
                .map(RecommendationRuleResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteRule(Integer id) {
        TestRecommendationRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_NOT_FOUND,
                        "Không tìm thấy quy tắc đề xuất với ID: " + id));

        ruleRepository.delete(rule);
        log.info("Đã xóa quy tắc đề xuất ID={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public RecommendationResultDTO getRecommendation(Integer testSetId, BigDecimal totalScore) {
        Optional<TestRecommendationRule> matchingOpt = ruleRepository.findMatchingRule(testSetId, totalScore);

        if (matchingOpt.isEmpty()) {
            log.info("Không tìm thấy quy tắc đề xuất phù hợp cho bộ đề ID={} với điểm={}", testSetId, totalScore);
            return new RecommendationResultDTO(totalScore, "GENERAL", null, List.of());
        }

        TestRecommendationRule rule = matchingOpt.get();
        Course course = rule.getRecommendedCourse();

        // 1. Thông tin khóa học đề xuất
        RecommendationResultDTO.RecommendedCourseDTO courseDTO = null;
        if (course != null) {
            String advice = rule.getAdviceText() != null ? rule.getAdviceText() : rule.getAdviceNote();
            courseDTO = new RecommendationResultDTO.RecommendedCourseDTO(
                    course.getId(),
                    course.getCode(),
                    course.getName(),
                    course.getTuitionAmount(),
                    advice
            );
        }

        // 2. Tra cứu các lớp học sắp khai giảng của khóa học này còn slot
        List<RecommendationResultDTO.UpcomingClassDTO> upcomingClasses = new ArrayList<>();
        if (course != null) {
            List<ClassEntity> openClasses = classRepository.searchClasses(course.getId(), "OPEN");
            LocalDate today = LocalDate.now();

            for (ClassEntity cls : openClasses) {
                if (cls.getStartDate() != null && !cls.getStartDate().isBefore(today)) {
                    long occupied = enrollmentRepository.countOccupiedSlots(cls.getId(), LocalDateTime.now());
                    int available = (int) Math.max(0, cls.getMaxStudents() - occupied);

                    if (available > 0) {
                        upcomingClasses.add(new RecommendationResultDTO.UpcomingClassDTO(
                                cls.getId(),
                                cls.getCode(),
                                cls.getName(),
                                cls.getStartDate(),
                                available
                        ));
                        if (upcomingClasses.size() >= 5) {
                            break; // Tối đa 5 lớp sắp khai giảng
                        }
                    }
                }
            }
        }

        return new RecommendationResultDTO(totalScore, rule.getAssessedLevel(), courseDTO, upcomingClasses);
    }
}
