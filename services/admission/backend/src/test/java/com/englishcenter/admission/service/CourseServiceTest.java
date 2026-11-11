package com.englishcenter.admission.service;

import com.englishcenter.admission.common.exception.BusinessException;
import com.englishcenter.admission.common.exception.ErrorCode;
import com.englishcenter.admission.dto.course.CreateCourseRequest;
import com.englishcenter.admission.dto.course.CourseResponse;
import com.englishcenter.admission.dto.course.UpdateCourseRequest;
import com.englishcenter.admission.entity.Course;
import com.englishcenter.admission.repository.ClassRepository;
import com.englishcenter.admission.repository.CourseRepository;
import com.englishcenter.admission.service.impl.CourseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private ClassRepository classRepository;

    private CourseServiceImpl courseService;

    @BeforeEach
    void setUp() {
        courseService = new CourseServiceImpl(courseRepository, classRepository);
    }

    @Test
    @DisplayName("Tạo mới khóa học thành công khi mã khóa học chưa tồn tại (INV-ADM-02)")
    void createCourse_Success() {
        CreateCourseRequest request = new CreateCourseRequest(
                "IELTS-6.5", "IELTS Mục tiêu 6.5+", "ADVANCED", 12,
                BigDecimal.valueOf(7500000), null, "Luyện thi chuyên sâu"
        );

        when(courseRepository.existsByCode("IELTS-6.5")).thenReturn(false);

        Course saved = new Course();
        saved.setId(10);
        saved.setCode("IELTS-6.5");
        saved.setName("IELTS Mục tiêu 6.5+");
        saved.setLevel("ADVANCED");
        saved.setDurationWeeks(12);
        saved.setTuitionAmount(BigDecimal.valueOf(7500000));
        saved.setIsActive(true);

        when(courseRepository.save(any(Course.class))).thenReturn(saved);
        when(courseRepository.findByIdWithPrerequisite(10)).thenReturn(Optional.of(saved));

        CourseResponse response = courseService.createCourse(request);

        assertNotNull(response);
        assertEquals("IELTS-6.5", response.getCode());
        assertEquals(BigDecimal.valueOf(7500000), response.getTuitionAmount());
        verify(courseRepository, times(1)).save(any(Course.class));
    }

    @Test
    @DisplayName("Tạo khóa học ném lỗi 409 ADM_COURSE_CODE_EXISTS khi trùng mã code (Bảo vệ bất biến INV-ADM-02)")
    void createCourse_DuplicateCode_ThrowsConflict() {
        CreateCourseRequest request = new CreateCourseRequest(
                "IELTS-6.5", "IELTS Trùng Mã", "ADVANCED", 12,
                BigDecimal.valueOf(7500000), null, "Mô tả"
        );

        when(courseRepository.existsByCode("IELTS-6.5")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> courseService.createCourse(request));

        assertEquals(ErrorCode.ADM_COURSE_CODE_EXISTS, exception.getErrorCode());
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    @DisplayName("Tạo khóa học ném lỗi khi khóa học tiên quyết chỉ định không tồn tại")
    void createCourse_PrerequisiteNotFound_ThrowsException() {
        CreateCourseRequest request = new CreateCourseRequest(
                "IELTS-7.0", "IELTS 7.0", "ADVANCED", 12,
                BigDecimal.valueOf(8500000), 999, "Mô tả"
        );

        when(courseRepository.existsByCode("IELTS-7.0")).thenReturn(false);
        when(courseRepository.existsById(999)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> courseService.createCourse(request));

        assertEquals(ErrorCode.ADM_COURSE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("Cập nhật khóa học ném lỗi khi khóa học tự nhận chính nó làm điều kiện tiên quyết")
    void updateCourse_SelfPrerequisite_ThrowsValidation() {
        Course existing = new Course();
        existing.setId(5);
        existing.setCode("ENG-A1");

        when(courseRepository.findById(5)).thenReturn(Optional.of(existing));

        UpdateCourseRequest request = new UpdateCourseRequest(
                "ENG A1 Mới", "BEGINNER", 8, BigDecimal.valueOf(3000000),
                5, "Tự tham chiếu", true
        );

        BusinessException exception = assertThrows(BusinessException.class,
                () -> courseService.updateCourse(5, request));

        assertEquals(ErrorCode.ADM_VALIDATION_ERROR, exception.getErrorCode());
    }

    @Test
    @DisplayName("Cập nhật khóa học ném lỗi khi phát hiện vòng lặp phụ thuộc (Circular Dependency)")
    void updateCourse_CircularDependency_ThrowsValidation() {
        // Course 1 muốn đặt Course 2 làm tiên quyết
        // Nhưng Course 2 đã có Course 1 làm tiên quyết -> Chu trình 1 -> 2 -> 1
        Course course1 = new Course();
        course1.setId(1);
        course1.setCode("C-1");

        Course course2 = new Course();
        course2.setId(2);
        course2.setCode("C-2");
        course2.setPrerequisiteCourseId(1); // C-2 đã trỏ về C-1

        when(courseRepository.findById(1)).thenReturn(Optional.of(course1));
        when(courseRepository.findById(2)).thenReturn(Optional.of(course2));

        UpdateCourseRequest request = new UpdateCourseRequest(
                "C-1 Updated", "INTERMEDIATE", 10, BigDecimal.valueOf(4000000),
                2, "Vòng lặp", true
        );

        BusinessException exception = assertThrows(BusinessException.class,
                () -> courseService.updateCourse(1, request));

        assertEquals(ErrorCode.ADM_VALIDATION_ERROR, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("vòng lặp"));
    }

    @Test
    @DisplayName("Xóa khóa học bị chặn khi đang có lớp học mở liên kết")
    void deleteCourse_HasClasses_ThrowsConflict() {
        Course course = new Course();
        course.setId(7);
        course.setCode("ENG-B2");

        when(courseRepository.findById(7)).thenReturn(Optional.of(course));
        when(classRepository.existsByCourseId(7)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> courseService.deleteCourse(7));

        assertEquals(ErrorCode.ADM_COURSE_HAS_CLASSES, exception.getErrorCode());
        verify(courseRepository, never()).delete(any(Course.class));
    }

    @Test
    @DisplayName("Xóa khóa học bị chặn khi đang là điều kiện tiên quyết của khóa học khác")
    void deleteCourse_IsPrerequisiteOfAnother_ThrowsConflict() {
        Course course = new Course();
        course.setId(8);
        course.setCode("ENG-B1");

        when(courseRepository.findById(8)).thenReturn(Optional.of(course));
        when(classRepository.existsByCourseId(8)).thenReturn(false);
        when(courseRepository.existsByPrerequisiteCourseId(8)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> courseService.deleteCourse(8));

        assertEquals(ErrorCode.ADM_COURSE_HAS_CLASSES, exception.getErrorCode());
        verify(courseRepository, never()).delete(any(Course.class));
    }

    @Test
    @DisplayName("Xóa khóa học thành công khi không có lớp học hay phụ thuộc nào")
    void deleteCourse_Success() {
        Course course = new Course();
        course.setId(9);
        course.setCode("ENG-TOEIC");

        when(courseRepository.findById(9)).thenReturn(Optional.of(course));
        when(classRepository.existsByCourseId(9)).thenReturn(false);
        when(courseRepository.existsByPrerequisiteCourseId(9)).thenReturn(false);

        courseService.deleteCourse(9);

        verify(courseRepository, times(1)).delete(course);
    }
}
