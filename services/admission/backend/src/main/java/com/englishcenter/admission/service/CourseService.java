package com.englishcenter.admission.service;

import com.englishcenter.admission.dto.course.CreateCourseRequest;
import com.englishcenter.admission.dto.course.CourseResponse;
import com.englishcenter.admission.dto.course.UpdateCourseRequest;

import java.util.List;

/**
 * Giao diện nghiệp vụ quản lý danh mục khóa học và lộ trình đào tạo (FR-ACA-01, INV-ADM-02).
 */
public interface CourseService {

    /**
     * Tạo mới một khóa học trong hệ thống.
     * Bắt buộc kiểm tra tính duy nhất của mã khóa học (INV-ADM-02).
     *
     * @param request DTO chứa thông tin khóa học
     * @return DTO CourseResponse của khóa học vừa tạo
     */
    CourseResponse createCourse(CreateCourseRequest request);

    /**
     * Cập nhật thông tin chi tiết của một khóa học.
     *
     * @param id ID của khóa học cần cập nhật
     * @param request Dữ liệu cập nhật
     * @return Thông tin khóa học sau cập nhật
     */
    CourseResponse updateCourse(Integer id, UpdateCourseRequest request);

    /**
     * Lấy thông tin chi tiết một khóa học theo ID.
     *
     * @param id ID của khóa học
     * @return Thông tin chi tiết khóa học
     */
    CourseResponse getCourseById(Integer id);

    /**
     * Lấy danh sách khóa học theo các tiêu chí bộ lọc.
     *
     * @param level Trình độ đào tạo (BEGINNER, INTERMEDIATE, ADVANCED - tùy chọn)
     * @param isActive Trạng thái hoạt động (tùy chọn)
     * @return Danh sách khóa học
     */
    List<CourseResponse> getCourses(String level, Boolean isActive);

    /**
     * Xóa hoặc vô hiệu hóa khóa học khỏi hệ thống.
     * Ngăn chặn nếu khóa học đang có lớp hoặc đang là điều kiện tiên quyết của khóa khác.
     *
     * @param id ID khóa học cần xóa
     */
    void deleteCourse(Integer id);
}
