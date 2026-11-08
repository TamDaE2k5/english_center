package com.englishcenter.admission.controller;

import com.englishcenter.admission.common.dto.ApiResponse;
import com.englishcenter.admission.dto.clazz.*;
import com.englishcenter.admission.service.ClassService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller xử lý các yêu cầu RESTful quản lý lớp học, sức chứa và vòng đời (FR-ACA-02, FR-ACA-04, FR-FAC-03, FR-ACA-05).
 */
@RestController
@RequestMapping("/api/admission/classes")
public class ClassController {

    private final ClassService classService;

    public ClassController(ClassService classService) {
        this.classService = classService;
    }

    /**
     * Tra cứu danh sách lớp học (Công khai cho khách truy cập và học viên).
     *
     * @param courseId ID khóa học (tùy chọn)
     * @param status Trạng thái lớp học (tùy chọn, ví dụ: OPEN)
     * @return Danh sách lớp học kèm thông tin slot khả dụng
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ClassResponse>>> getClasses(
            @RequestParam(name = "course_id", required = false) Integer courseId,
            @RequestParam(name = "status", required = false) String status) {
        List<ClassResponse> classes = classService.getClasses(courseId, status);
        return ResponseEntity.ok(ApiResponse.success(classes, "Lấy danh sách lớp học thành công"));
    }

    /**
     * Xem thông tin chi tiết một lớp học (Công khai).
     *
     * @param id ID lớp học
     * @return Chi tiết lớp học
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClassResponse>> getClassById(@PathVariable("id") Integer id) {
        ClassResponse response = classService.getClassById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy chi tiết lớp học thành công"));
    }

    /**
     * Tra cứu thông tin sức chứa và số slot khả dụng theo thời gian thực (TASK-ADM-24, FR-ACA-05).
     *
     * @param id ID lớp học
     * @return Sức chứa, số lượng đã duyệt, số lượng đang giữ chỗ 48h, và số slot còn lại
     */
    @GetMapping("/{id}/capacity")
    public ResponseEntity<ApiResponse<ClassCapacityResponse>> getClassCapacity(@PathVariable("id") Integer id) {
        ClassCapacityResponse response = classService.getClassCapacity(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Tra cứu sĩ số lớp thành công"));
    }

    /**
     * Alias tra cứu slot khả dụng theo thời gian thực.
     */
    @GetMapping("/{id}/available-slots")
    public ResponseEntity<ApiResponse<ClassCapacityResponse>> getAvailableSlots(@PathVariable("id") Integer id) {
        return getClassCapacity(id);
    }

    /**
     * Mở một lớp học mới vào hệ thống.
     * Kiểm tra chặt chẽ:
     * - Sức chứa phòng: max_students <= room.capacity (400 ADM_ROOM_CAPACITY_EXCEEDED)
     * - Chống trùng lịch phòng (409 ADM_ROOM_CONFLICT)
     * - Chống trùng lịch giáo viên (409 ADM_TEACHER_CONFLICT)
     *
     * @param request Dữ liệu mở lớp học
     * @return Thông tin lớp học vừa mở với HTTP 201 Created
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<ClassResponse>> createClass(@Valid @RequestBody CreateClassRequest request) {
        ClassResponse response = classService.createClass(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Mở lớp học mới thành công"));
    }

    /**
     * Cập nhật thông tin chi tiết lớp học (chặn sửa nếu gây xung đột).
     *
     * @param id ID lớp học cần sửa
     * @param request Dữ liệu cập nhật
     * @return Thông tin lớp học sau cập nhật
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<ClassResponse>> updateClass(
            @PathVariable("id") Integer id,
            @Valid @RequestBody UpdateClassRequest request) {
        ClassResponse response = classService.updateClass(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật lớp học thành công"));
    }

    /**
     * Chuyển trạng thái vòng đời của lớp học (OPEN -> IN_PROGRESS -> COMPLETED / CANCELLED).
     *
     * @param id ID lớp học
     * @param request Yêu cầu trạng thái mới
     * @return Thông tin lớp học sau chuyển trạng thái
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<ClassResponse>> updateClassStatus(
            @PathVariable("id") Integer id,
            @Valid @RequestBody UpdateClassStatusRequest request) {
        ClassResponse response = classService.updateClassStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật trạng thái lớp học thành công"));
    }
}
