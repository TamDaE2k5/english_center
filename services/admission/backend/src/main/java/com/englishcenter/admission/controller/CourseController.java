package com.englishcenter.admission.controller;

import com.englishcenter.admission.common.dto.ApiResponse;
import com.englishcenter.admission.dto.course.CreateCourseRequest;
import com.englishcenter.admission.dto.course.CourseResponse;
import com.englishcenter.admission.dto.course.UpdateCourseRequest;
import com.englishcenter.admission.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller xử lý các yêu cầu RESTful quản lý khóa học và lộ trình đào tạo (FR-ACA-01, INV-ADM-02).
 * Phân quyền:
 * - Tra cứu danh mục và chi tiết khóa học: Công khai (Public).
 * - Thêm mới và cập nhật khóa học: Nhân viên học vụ (STAFF) và Quản trị viên (ADMIN).
 * - Xóa/Ngừng khóa học: Quản trị viên (ADMIN).
 */
@RestController
@RequestMapping("/api/admission")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    /**
     * Endpoint kiểm tra trạng thái hoạt động của Admission Service.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "admission-service",
                "framework", "Spring Boot 3.3.4 (Java 21)"
        ));
    }

    /**
     * Lấy danh mục toàn bộ khóa học (Công khai cho khách truy cập và học viên).
     *
     * @param level Trình độ (tùy chọn: BEGINNER, INTERMEDIATE, ADVANCED)
     * @param isActive Trạng thái hoạt động (tùy chọn: true/false)
     * @return Danh sách khóa học
     */
    @GetMapping("/courses")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> listCourses(
            @RequestParam(name = "level", required = false) String level,
            @RequestParam(name = "is_active", required = false) Boolean isActive) {
        List<CourseResponse> courses = courseService.getCourses(level, isActive);
        return ResponseEntity.ok(ApiResponse.success(courses, "Lấy danh mục khóa học thành công"));
    }

    /**
     * Xem chi tiết thông tin và lộ trình của một khóa học (Công khai).
     *
     * @param id ID khóa học
     * @return Chi tiết khóa học và thông tin khóa tiên quyết
     */
    @GetMapping("/courses/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> getCourse(@PathVariable("id") Integer id) {
        CourseResponse response = courseService.getCourseById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy chi tiết khóa học thành công"));
    }

    /**
     * Tạo mới một khóa học trong hệ thống.
     * Bắt buộc kiểm tra mã khóa học duy nhất trên toàn hệ thống (INV-ADM-02).
     *
     * @param request Dữ liệu khóa học cần tạo
     * @return Thông tin khóa học vừa tạo với HTTP 201 Created
     */
    @PostMapping("/courses")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<CourseResponse>> createCourse(@Valid @RequestBody CreateCourseRequest request) {
        CourseResponse response = courseService.createCourse(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tạo mới khóa học thành công"));
    }

    /**
     * Cập nhật thông tin chi tiết của một khóa học.
     *
     * @param id ID của khóa học cần cập nhật
     * @param request Dữ liệu cập nhật
     * @return Thông tin khóa học sau khi cập nhật
     */
    @PutMapping("/courses/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<CourseResponse>> updateCourse(
            @PathVariable("id") Integer id,
            @Valid @RequestBody UpdateCourseRequest request) {
        CourseResponse response = courseService.updateCourse(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật thông tin khóa học thành công"));
    }

    /**
     * Xóa khóa học khỏi hệ thống.
     * Chặn xóa nếu đang có lớp học mở hoặc đang là điều kiện tiên quyết của khóa khác.
     *
     * @param id ID của khóa học cần xóa
     * @return Phản hồi thông báo kết quả xóa
     */
    @DeleteMapping("/courses/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(@PathVariable("id") Integer id) {
        courseService.deleteCourse(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa khóa học thành công"));
    }
}
