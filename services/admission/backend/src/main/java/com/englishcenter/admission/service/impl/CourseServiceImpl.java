package com.englishcenter.admission.service.impl;

import com.englishcenter.admission.common.exception.BusinessException;
import com.englishcenter.admission.common.exception.ErrorCode;
import com.englishcenter.admission.dto.course.CreateCourseRequest;
import com.englishcenter.admission.dto.course.CourseResponse;
import com.englishcenter.admission.dto.course.UpdateCourseRequest;
import com.englishcenter.admission.entity.Course;
import com.englishcenter.admission.repository.ClassRepository;
import com.englishcenter.admission.repository.CourseRepository;
import com.englishcenter.admission.service.CourseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Hiện thực nghiệp vụ quản lý khóa học và lộ trình đào tạo (FR-ACA-01, INV-ADM-02).
 */
@Service
public class CourseServiceImpl implements CourseService {

    private static final Logger log = LoggerFactory.getLogger(CourseServiceImpl.class);

    private final CourseRepository courseRepository;
    private final ClassRepository classRepository;

    public CourseServiceImpl(CourseRepository courseRepository, ClassRepository classRepository) {
        this.courseRepository = courseRepository;
        this.classRepository = classRepository;
    }

    @Override
    @Transactional
    public CourseResponse createCourse(CreateCourseRequest request) {
        String normalizedCode = request.getCode().trim().toUpperCase();

        // 1. Kiểm tra tính duy nhất của mã khóa học (Bảo vệ bất biến INV-ADM-02)
        if (courseRepository.existsByCode(normalizedCode)) {
            log.warn("Tạo khóa học thất bại: mã code {} đã tồn tại (INV-ADM-02)", normalizedCode);
            throw new BusinessException(ErrorCode.ADM_COURSE_CODE_EXISTS,
                    "Mã khóa học " + normalizedCode + " đã tồn tại trong hệ thống");
        }

        // 2. Kiểm tra khóa học tiên quyết (nếu có chỉ định)
        if (request.getPrerequisiteCourseId() != null) {
            boolean prereqExists = courseRepository.existsById(request.getPrerequisiteCourseId());
            if (!prereqExists) {
                log.warn("Khóa học tiên quyết ID={} không tồn tại", request.getPrerequisiteCourseId());
                throw new BusinessException(ErrorCode.ADM_COURSE_NOT_FOUND,
                        "Không tìm thấy khóa học tiên quyết với ID: " + request.getPrerequisiteCourseId());
            }
        }

        // 3. Khởi tạo đối tượng thực thể Course
        Course course = new Course();
        course.setCode(normalizedCode);
        course.setName(request.getName().trim());
        course.setDescription(request.getDescription());
        course.setLevel(request.getLevel().trim().toUpperCase());
        course.setTargetOutput(request.getTargetOutput());
        course.setDurationWeeks(request.getDurationWeeks());
        course.setTuitionAmount(request.getTuitionAmount());
        course.setPrerequisiteCourseId(request.getPrerequisiteCourseId());
        course.setIsActive(true);

        Course saved = courseRepository.save(course);
        log.info("Tạo mới khóa học thành công: ID={}, code={}, tuition_amount={}",
                saved.getId(), saved.getCode(), saved.getTuitionAmount());

        // Lấy lại entity kèm nạp thông tin khóa tiên quyết (nếu có)
        return getCourseById(saved.getId());
    }

    @Override
    @Transactional
    public CourseResponse updateCourse(Integer id, UpdateCourseRequest request) {
        // 1. Kiểm tra khóa học tồn tại
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_COURSE_NOT_FOUND,
                        "Không tìm thấy khóa học với ID: " + id));

        // 2. Kiểm tra logic khóa học tiên quyết và chống vòng lặp phụ thuộc (Circular Dependency)
        if (request.getPrerequisiteCourseId() != null) {
            Integer prereqId = request.getPrerequisiteCourseId();

            // Khóa học không thể tự yêu cầu chính nó
            if (prereqId.equals(id)) {
                log.warn("Khóa học ID={} không thể tự tham chiếu làm khóa học tiên quyết", id);
                throw new BusinessException(ErrorCode.ADM_VALIDATION_ERROR,
                        "Khóa học không thể tự đặt chính nó làm điều kiện tiên quyết");
            }

            // Kiểm tra xem khóa tiên quyết có tồn tại không
            Course prereqCourse = courseRepository.findById(prereqId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ADM_COURSE_NOT_FOUND,
                            "Không tìm thấy khóa học tiên quyết với ID: " + prereqId));

            // Kiểm tra vòng lặp phụ thuộc bắc cầu (A -> B -> ... -> A)
            validateNoCircularDependency(id, prereqCourse);
        }

        // 3. Cập nhật các trường thông tin
        course.setName(request.getName().trim());
        course.setDescription(request.getDescription());
        course.setLevel(request.getLevel().trim().toUpperCase());
        course.setTargetOutput(request.getTargetOutput());
        course.setDurationWeeks(request.getDurationWeeks());
        course.setTuitionAmount(request.getTuitionAmount());
        course.setPrerequisiteCourseId(request.getPrerequisiteCourseId());
        if (request.getIsActive() != null) {
            course.setIsActive(request.getIsActive());
        }

        Course updated = courseRepository.save(course);
        log.info("Cập nhật khóa học thành công: ID={}, code={}", updated.getId(), updated.getCode());

        return getCourseById(updated.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public CourseResponse getCourseById(Integer id) {
        Course course = courseRepository.findByIdWithPrerequisite(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_COURSE_NOT_FOUND,
                        "Không tìm thấy khóa học với ID: " + id));

        return CourseResponse.fromEntity(course);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getCourses(String level, Boolean isActive) {
        String normalizedLevel = (level != null && !level.trim().isEmpty()) ? level.trim().toUpperCase() : null;
        return courseRepository.searchCourses(normalizedLevel, isActive)
                .stream()
                .map(CourseResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteCourse(Integer id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_COURSE_NOT_FOUND,
                        "Không tìm thấy khóa học với ID: " + id));

        // 1. Kiểm tra nếu có lớp học đang liên kết với khóa này
        if (classRepository.existsByCourseId(id)) {
            log.warn("Không thể xóa khóa học ID={}: đang có lớp học tham chiếu", id);
            throw new BusinessException(ErrorCode.ADM_COURSE_HAS_CLASSES,
                    "Không thể xóa khóa học đang có lớp học trong hệ thống");
        }

        // 2. Kiểm tra nếu có khóa học khác đang nhận khóa này làm điều kiện tiên quyết
        if (courseRepository.existsByPrerequisiteCourseId(id)) {
            log.warn("Không thể xóa khóa học ID={}: đang là tiên quyết của khóa khác", id);
            throw new BusinessException(ErrorCode.ADM_COURSE_HAS_CLASSES,
                    "Không thể xóa khóa học đang là điều kiện tiên quyết của khóa học khác");
        }

        // Xóa thực thể khỏi cơ sở dữ liệu
        courseRepository.delete(course);
        log.info("Đã xóa hoàn toàn khóa học ID={}, code={}", id, course.getCode());
    }

    /**
     * Thuật toán kiểm tra và ngăn chặn vòng lặp phụ thuộc (Circular Dependency).
     * Duyệt ngược cây điều kiện tiên quyết, nếu gặp lại currentCourseId thì báo lỗi.
     *
     * @param currentCourseId ID khóa học đang cập nhật
     * @param targetPrereq Khóa học được gán làm điều kiện tiên quyết
     */
    private void validateNoCircularDependency(Integer currentCourseId, Course targetPrereq) {
        Set<Integer> visited = new HashSet<>();
        visited.add(currentCourseId);

        Course step = targetPrereq;
        while (step != null) {
            if (visited.contains(step.getId())) {
                log.warn("Phát hiện vòng lặp phụ thuộc tiên quyết: courseId={} liên kết chu trình tới courseId={}",
                        currentCourseId, step.getId());
                throw new BusinessException(ErrorCode.ADM_VALIDATION_ERROR,
                        "Phát hiện vòng lặp phụ thuộc điều kiện tiên quyết giữa các khóa học");
            }
            visited.add(step.getId());

            if (step.getPrerequisiteCourseId() != null) {
                step = courseRepository.findById(step.getPrerequisiteCourseId()).orElse(null);
            } else {
                step = null;
            }
        }
    }
}
