package com.englishcenter.admission.controller;

import com.englishcenter.admission.common.dto.ApiResponse;
import com.englishcenter.admission.dto.clazz.ClassResponse;
import com.englishcenter.admission.dto.course.CourseResponse;
import com.englishcenter.admission.dto.enrollment.EnrollmentResponse;
import com.englishcenter.admission.service.ClassService;
import com.englishcenter.admission.service.CourseService;
import com.englishcenter.admission.service.EnrollmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller cung cấp các API nội bộ (East-West) cho các microservice khác gọi qua X-Internal-Secret.
 * Bảo vệ bởi InternalSecretFilter trước khi vào tầng Controller.
 */
@RestController
@RequestMapping("/api/admission/internal")
public class InternalController {

    private final CourseService courseService;
    private final ClassService classService;
    private final EnrollmentService enrollmentService;

    public InternalController(CourseService courseService,
                              ClassService classService,
                              EnrollmentService enrollmentService) {
        this.courseService = courseService;
        this.classService = classService;
        this.enrollmentService = enrollmentService;
    }

    /**
     * Tra cứu chi tiết khóa học nội bộ cho Finance Service.
     *
     * @param id ID khóa học
     * @return ApiResponse chứa CourseResponse
     */
    @GetMapping("/courses/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> getCourseInternal(@PathVariable("id") Integer id) {
        CourseResponse course = courseService.getCourseById(id);
        return ResponseEntity.ok(ApiResponse.success(course, "Lấy thông tin khóa học nội bộ thành công"));
    }

    /**
     * Tra cứu chi tiết lớp học nội bộ cho Learning Service và Student Service.
     *
     * @param id ID lớp học
     * @return ApiResponse chứa ClassResponse
     */
    @GetMapping("/classes/{id}")
    public ResponseEntity<ApiResponse<ClassResponse>> getClassInternal(@PathVariable("id") Integer id) {
        ClassResponse clazz = classService.getClassById(id);
        return ResponseEntity.ok(ApiResponse.success(clazz, "Lấy thông tin lớp học nội bộ thành công"));
    }

    /**
     * Tra cứu danh sách đơn tuyển sinh nội bộ theo student_id (admission.users.id), class_id, status.
     *
     * @param studentId ID tài khoản học viên (tùy chọn)
     * @param classId   ID lớp học (tùy chọn)
     * @param status    Trạng thái đơn (tùy chọn, ví dụ APPROVED)
     * @return Danh sách EnrollmentResponse khớp bộ lọc
     */
    @GetMapping("/enrollments")
    public ResponseEntity<ApiResponse<List<EnrollmentResponse>>> getEnrollmentsInternal(
            @RequestParam(name = "student_id", required = false) Integer studentId,
            @RequestParam(name = "class_id", required = false) Integer classId,
            @RequestParam(name = "status", required = false) String status) {
        List<EnrollmentResponse> list = enrollmentService.getInternalEnrollments(studentId, classId, status);
        return ResponseEntity.ok(ApiResponse.success(list, "Tra cứu đơn tuyển sinh nội bộ thành công"));
    }

    /**
     * Kiểm tra học viên (user_id) có đơn APPROVED trong lớp (class_id) hay không.
     *
     * @param userId  ID tài khoản học viên (admission.users.id)
     * @param classId ID lớp học
     * @return Thông tin trạng thái ghi danh (enrolled: true/false, status)
     */
    @GetMapping("/enrollments/check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkEnrollmentInternal(
            @RequestParam(name = "user_id") Integer userId,
            @RequestParam(name = "class_id") Integer classId) {
        Map<String, Object> result = enrollmentService.checkStudentEnrollment(userId, classId);
        return ResponseEntity.ok(ApiResponse.success(result, "Kiểm tra trạng thái ghi danh thành công"));
    }
}
