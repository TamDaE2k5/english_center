package com.englishcenter.admission.service;

import com.englishcenter.admission.dto.clazz.*;

import java.util.List;

/**
 * Giao diện nghiệp vụ quản lý lớp học đào tạo, sức chứa phòng và vòng đời lớp (FR-ACA-02, FR-ACA-04, FR-FAC-03, FR-ACA-05).
 */
public interface ClassService {
    /** Hủy lớp chưa khai giảng thiếu sĩ số; cascade và ghi yêu cầu Finance trong cùng transaction. */
    ClassResponse cancelClass(Integer id, String reason);

    /**
     * Mở một lớp học mới vào hệ thống.
     * Bắt buộc kiểm tra:
     * 1. Sức chứa phòng: max_students <= room.capacity (FR-FAC-03)
     * 2. Không xung đột lịch phòng và giáo viên (FR-ACA-03)
     * 3. Tính hợp lệ của thời gian (end_date > start_date, end_time > start_time)
     *
     * @param request Dữ liệu yêu cầu mở lớp
     * @return DTO ClassResponse của lớp học vừa tạo
     */
    ClassResponse createClass(CreateClassRequest request);

    /**
     * Cập nhật thông tin chi tiết của một lớp học.
     *
     * @param id ID của lớp học
     * @param request Dữ liệu cập nhật
     * @return Thông tin lớp học sau cập nhật
     */
    ClassResponse updateClass(Integer id, UpdateClassRequest request);

    /**
     * Cập nhật trạng thái vòng đời của lớp học (OPEN, IN_PROGRESS, COMPLETED, CANCELLED).
     * Tuân thủ sơ đồ chuyển trạng thái nghiêm ngặt (FR-ACA-04).
     *
     * @param id ID của lớp học
     * @param newStatus Trạng thái mới
     * @return Thông tin lớp học sau chuyển trạng thái
     */
    ClassResponse updateClassStatus(Integer id, String newStatus);

    /**
     * Lấy thông tin chi tiết của một lớp học theo ID kèm sĩ số và slot khả dụng.
     *
     * @param id ID của lớp học
     * @return Chi tiết lớp học
     */
    ClassResponse getClassById(Integer id);

    /**
     * Tra cứu danh sách lớp học theo bộ lọc linh hoạt.
     *
     * @param courseId ID khóa học (tùy chọn)
     * @param status Trạng thái lớp (tùy chọn)
     * @return Danh sách lớp học
     */
    List<ClassResponse> getClasses(Integer courseId, String status);

    /**
     * Tính toán sĩ số thực tế và số slot khả dụng theo thời gian thực (TASK-ADM-24, FR-ACA-05).
     *
     * @param classId ID lớp học
     * @return DTO ClassCapacityResponse
     */
    ClassCapacityResponse getClassCapacity(Integer classId);
}
