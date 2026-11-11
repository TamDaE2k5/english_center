package com.englishcenter.admission.service;

import com.englishcenter.admission.common.exception.BusinessException;
import com.englishcenter.admission.common.exception.ErrorCode;
import com.englishcenter.admission.dto.test.CreateRuleRequest;
import com.englishcenter.admission.dto.test.RecommendationResultDTO;
import com.englishcenter.admission.dto.test.RecommendationRuleResponse;
import com.englishcenter.admission.entity.ClassEntity;
import com.englishcenter.admission.entity.Course;
import com.englishcenter.admission.entity.TestRecommendationRule;
import com.englishcenter.admission.repository.*;
import com.englishcenter.admission.service.impl.RecommendationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private TestRecommendationRuleRepository ruleRepository;

    @Mock
    private TestSetRepository testSetRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private ClassRepository classRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    private RecommendationServiceImpl recommendationService;

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationServiceImpl(
                ruleRepository, testSetRepository, courseRepository, classRepository, enrollmentRepository
        );
    }

    @Test
    @DisplayName("Tạo mới quy tắc đề xuất thành công khi không có chồng lấn dải điểm")
    void createRule_Success() {
        CreateRuleRequest request = new CreateRuleRequest(
                1, new BigDecimal("60.0"), new BigDecimal("79.9"),
                "INTERMEDIATE", 2, "Khóa học phù hợp năng lực hiện tại"
        );

        when(testSetRepository.existsById(1)).thenReturn(true);

        Course course = new Course();
        course.setId(2);
        course.setCode("IELTS-5.5");
        course.setName("IELTS Pre-Intermediate 5.5");
        when(courseRepository.findById(2)).thenReturn(Optional.of(course));

        when(ruleRepository.findOverlappingRules(eq(1), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        TestRecommendationRule saved = new TestRecommendationRule();
        saved.setId(10);
        saved.setTestSetId(1);
        saved.setMinScore(new BigDecimal("60.0"));
        saved.setMaxScore(new BigDecimal("79.9"));
        saved.setAssessedLevel("INTERMEDIATE");
        saved.setRecommendedCourseId(2);
        saved.setRecommendedCourse(course);

        when(ruleRepository.save(any(TestRecommendationRule.class))).thenReturn(saved);

        RecommendationRuleResponse response = recommendationService.createRule(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("60.0"), response.getMinScore());
        assertEquals("IELTS-5.5", response.getRecommendedCourseCode());
        verify(ruleRepository, times(1)).save(any(TestRecommendationRule.class));
    }

    @Test
    @DisplayName("Tạo quy tắc ném lỗi 409 ADM_RULE_OVERLAP khi dải điểm bị chồng lấn với quy tắc đã có")
    void createRule_Overlap_ThrowsConflict() {
        CreateRuleRequest request = new CreateRuleRequest(
                1, new BigDecimal("65.0"), new BigDecimal("85.0"),
                "ADVANCED", 3, "Chồng lấn"
        );

        when(testSetRepository.existsById(1)).thenReturn(true);
        Course course = new Course();
        course.setId(3);
        when(courseRepository.findById(3)).thenReturn(Optional.of(course));

        TestRecommendationRule existing = new TestRecommendationRule();
        existing.setId(5);
        existing.setMinScore(new BigDecimal("60.0"));
        existing.setMaxScore(new BigDecimal("79.9"));

        when(ruleRepository.findOverlappingRules(eq(1), any(), any(), any()))
                .thenReturn(List.of(existing));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> recommendationService.createRule(request));

        assertEquals(ErrorCode.ADM_RULE_OVERLAP, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("chồng lấn"));
        verify(ruleRepository, never()).save(any(TestRecommendationRule.class));
    }

    @Test
    @DisplayName("Khớp nối điểm thi và trả về khóa học đề xuất kèm danh sách lớp còn slot (TASK-ADM-32)")
    void getRecommendation_MatchingRule_ReturnsCourseAndClasses() {
        TestRecommendationRule rule = new TestRecommendationRule();
        rule.setId(1);
        rule.setAssessedLevel("ADVANCED");
        rule.setAdviceText("Lộ trình bứt phá điểm số");

        Course course = new Course();
        course.setId(10);
        course.setCode("IELTS-6.5");
        course.setName("IELTS Chuyên Sâu 6.5");
        course.setTuitionAmount(new BigDecimal("7500000"));
        rule.setRecommendedCourse(course);

        when(ruleRepository.findMatchingRule(eq(1), eq(new BigDecimal("75.0"))))
                .thenReturn(Optional.of(rule));

        ClassEntity upcoming = new ClassEntity();
        upcoming.setId(101);
        upcoming.setCode("CLS-IELTS-65");
        upcoming.setName("Lớp IELTS 6.5 Tối");
        upcoming.setStartDate(LocalDate.now().plusDays(5));
        upcoming.setMaxStudents(20);

        when(classRepository.searchClasses(10, "OPEN")).thenReturn(List.of(upcoming));
        when(enrollmentRepository.countOccupiedSlots(eq(101), any())).thenReturn(15L);

        RecommendationResultDTO result = recommendationService.getRecommendation(1, new BigDecimal("75.0"));

        assertNotNull(result);
        assertEquals("ADVANCED", result.getAssessedLevel());
        assertNotNull(result.getRecommendedCourse());
        assertEquals("IELTS-6.5", result.getRecommendedCourse().getCourseCode());
        assertEquals(1, result.getUpcomingClasses().size());
        assertEquals(5, result.getUpcomingClasses().get(0).getAvailableSlots()); // 20 - 15 = 5
    }
}
