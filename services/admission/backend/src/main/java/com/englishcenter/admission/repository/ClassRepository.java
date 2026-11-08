package com.englishcenter.admission.repository;

import com.englishcenter.admission.entity.ClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Kho truy vấn dữ liệu lớp học (bảng admission.classes).
 * Quản lý thông tin lớp, sĩ số trần, phòng học, giảng viên và kiểm tra xung đột thời khóa biểu.
 */
@Repository
public interface ClassRepository extends JpaRepository<ClassEntity, Integer> {

    /**
     * Tìm lớp học theo mã lớp duy nhất.
     *
     * @param code Mã lớp
     * @return Optional chứa thông tin lớp
     */
    Optional<ClassEntity> findByCode(String code);

    /**
     * Kiểm tra sự tồn tại của mã lớp học.
     *
     * @param code Mã lớp cần kiểm tra
     * @return true nếu đã tồn tại
     */
    boolean existsByCode(String code);

    /**
     * Kiểm tra xem khóa học có đang được dùng bởi bất kỳ lớp nào không.
     *
     * @param courseId ID khóa học
     * @return true nếu có lớp học tham chiếu tới khóa học này
     */
    boolean existsByCourseId(Integer courseId);

    /**
     * Kiểm tra xem phòng học có đang được sử dụng bởi các lớp có trạng thái chỉ định không.
     *
     * @param roomId ID phòng học
     * @param statuses Danh sách trạng thái lớp
     * @return true nếu phòng đang có lớp học
     */
    boolean existsByRoomIdAndStatusIn(Integer roomId, Collection<String> statuses);

    /**
     * Tìm kiếm danh sách lớp học theo bộ lọc linh hoạt.
     *
     * @param courseId ID khóa học (tùy chọn)
     * @param status Trạng thái lớp (tùy chọn)
     * @return Danh sách lớp học
     */
    @Query("SELECT c FROM ClassEntity c LEFT JOIN FETCH c.course LEFT JOIN FETCH c.roomEntity LEFT JOIN FETCH c.teacher WHERE " +
           "(:courseId IS NULL OR c.courseId = :courseId) AND " +
           "(:status IS NULL OR c.status = :status) " +
           "ORDER BY c.startDate DESC, c.code ASC")
    List<ClassEntity> searchClasses(@Param("courseId") Integer courseId, @Param("status") String status);

    /**
     * Tìm các lớp học trùng phòng có khả năng xung đột lịch:
     * Cùng phòng học, trạng thái OPEN hoặc IN_PROGRESS, có khoảng ngày học giao cắt.
     *
     * @param roomId ID phòng học
     * @param excludeClassId ID lớp cần loại trừ (khi cập nhật lớp hiện tại, có thể null khi tạo mới)
     * @param startDate Ngày bắt đầu lớp cần xếp
     * @param endDate Ngày kết thúc lớp cần xếp
     * @return Danh sách các lớp học có khả năng xung đột phòng
     */
    @Query("SELECT c FROM ClassEntity c WHERE c.roomId = :roomId " +
           "AND (:excludeClassId IS NULL OR c.id <> :excludeClassId) " +
           "AND c.status IN ('OPEN', 'IN_PROGRESS') " +
           "AND c.startDate <= :endDate AND c.endDate >= :startDate")
    List<ClassEntity> findPotentialRoomConflicts(
            @Param("roomId") Integer roomId,
            @Param("excludeClassId") Integer excludeClassId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Tìm các lớp học trùng giảng viên có khả năng xung đột lịch:
     * Cùng giảng viên, trạng thái OPEN hoặc IN_PROGRESS, có khoảng ngày học giao cắt.
     *
     * @param teacherId ID giảng viên
     * @param excludeClassId ID lớp cần loại trừ (khi cập nhật)
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @return Danh sách các lớp học có khả năng xung đột giảng viên
     */
    @Query("SELECT c FROM ClassEntity c WHERE c.teacherId = :teacherId " +
           "AND (:excludeClassId IS NULL OR c.id <> :excludeClassId) " +
           "AND c.status IN ('OPEN', 'IN_PROGRESS') " +
           "AND c.startDate <= :endDate AND c.endDate >= :startDate")
    List<ClassEntity> findPotentialTeacherConflicts(
            @Param("teacherId") Integer teacherId,
            @Param("excludeClassId") Integer excludeClassId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
