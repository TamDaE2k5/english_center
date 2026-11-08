package com.englishcenter.admission.repository;

import com.englishcenter.admission.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Kho truy vấn dữ liệu khóa học và lộ trình đào tạo (bảng admission.courses).
 * Đảm bảo bảo vệ quy tắc bất biến INV-ADM-02 (Mã khóa học duy nhất).
 */
@Repository
public interface CourseRepository extends JpaRepository<Course, Integer> {

    /**
     * Tìm khóa học theo mã code duy nhất.
     *
     * @param code Mã khóa học
     * @return Optional chứa Course nếu tìm thấy
     */
    Optional<Course> findByCode(String code);

    /**
     * Kiểm tra sự tồn tại của mã khóa học (INV-ADM-02).
     *
     * @param code Mã khóa học cần kiểm tra
     * @return true nếu đã tồn tại
     */
    boolean existsByCode(String code);

    /**
     * Kiểm tra sự tồn tại của mã khóa học loại trừ khóa hiện tại (phục vụ khi update).
     *
     * @param code Mã khóa học
     * @param id ID của khóa học hiện tại đang xét
     * @return true nếu mã đã được dùng bởi một khóa khác
     */
    boolean existsByCodeAndIdNot(String code, Integer id);

    /**
     * Kiểm tra xem một khóa học có đang là điều kiện tiên quyết của khóa khác không.
     *
     * @param prerequisiteCourseId ID của khóa học
     * @return true nếu đang được tham chiếu làm khóa tiên quyết
     */
    boolean existsByPrerequisiteCourseId(Integer prerequisiteCourseId);

    /**
     * Tìm kiếm danh mục khóa học kèm eager fetch khóa tiên quyết theo bộ lọc.
     *
     * @param level Trình độ (BEGINNER, INTERMEDIATE, ADVANCED)
     * @param isActive Trạng thái hoạt động
     * @return Danh sách khóa học phù hợp
     */
    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.prerequisiteCourse WHERE " +
           "(:level IS NULL OR UPPER(c.level) = UPPER(:level)) AND " +
           "(:isActive IS NULL OR c.isActive = :isActive) " +
           "ORDER BY c.code ASC")
    List<Course> searchCourses(@Param("level") String level, @Param("isActive") Boolean isActive);

    /**
     * Lấy chi tiết khóa học kèm nạp trước khóa học tiên quyết (nếu có).
     *
     * @param id ID khóa học
     * @return Optional chứa Course và PrerequisiteCourse
     */
    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.prerequisiteCourse WHERE c.id = :id")
    Optional<Course> findByIdWithPrerequisite(@Param("id") Integer id);
}
