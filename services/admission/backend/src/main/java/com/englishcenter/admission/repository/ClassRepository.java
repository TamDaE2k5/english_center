package com.englishcenter.admission.repository;

import com.englishcenter.admission.entity.ClassEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
     * Lấy danh sách các lớp học thuộc về một khóa học.
     *
     * @param courseId ID khóa học
     * @return Danh sách lớp học
     */
    List<ClassEntity> findByCourseId(Integer courseId);

    /**
     * Lấy danh sách các lớp học theo danh sách trạng thái (ví dụ: OPEN, IN_PROGRESS).
     *
     * @param statuses Danh sách trạng thái
     * @return Danh sách lớp học
     */
    List<ClassEntity> findByStatusIn(Collection<String> statuses);

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
     * Tìm lớp học kèm khóa bi quan PESSIMISTIC_WRITE để chống race-condition khi duyệt đơn (INV-ADM-01).
     *
     * @param id ID lớp học
     * @return Optional chứa ClassEntity
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ClassEntity c WHERE c.id = :id")
    Optional<ClassEntity> findByIdWithLock(@Param("id") Integer id);

    /**
     * Tìm các lớp học trùng phòng có giao cắt cả về khoảng ngày và khung giờ học.
     *
     * @param roomId ID phòng học
     * @param excludeClassId ID lớp cần loại trừ (khi cập nhật)
     * @param startDate Ngày bắt đầu lớp cần xếp
     * @param endDate Ngày kết thúc lớp cần xếp
     * @param startTime Giờ bắt đầu ca học
     * @param endTime Giờ kết thúc ca học
     * @return Danh sách các lớp học có nguy cơ xung đột phòng
     */
    @Query("SELECT c FROM ClassEntity c LEFT JOIN FETCH c.roomEntity WHERE " +
           "c.roomId = :roomId " +
           "AND (:excludeClassId IS NULL OR c.id <> :excludeClassId) " +
           "AND c.status IN ('OPEN', 'IN_PROGRESS') " +
           "AND c.startDate <= :endDate AND c.endDate >= :startDate " +
           "AND c.startTime < :endTime AND c.endTime > :startTime")
    List<ClassEntity> findConflictingRoomClasses(
            @Param("roomId") Integer roomId,
            @Param("excludeClassId") Integer excludeClassId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    /**
     * Tìm các lớp học trùng giảng viên có giao cắt cả về khoảng ngày và khung giờ học.
     *
     * @param teacherId ID giảng viên
     * @param excludeClassId ID lớp cần loại trừ (khi cập nhật)
     * @param startDate Ngày bắt đầu lớp cần xếp
     * @param endDate Ngày kết thúc lớp cần xếp
     * @param startTime Giờ bắt đầu ca học
     * @param endTime Giờ kết thúc ca học
     * @return Danh sách các lớp học có nguy cơ xung đột giảng viên
     */
    @Query("SELECT c FROM ClassEntity c LEFT JOIN FETCH c.teacher WHERE " +
           "c.teacherId = :teacherId " +
           "AND (:excludeClassId IS NULL OR c.id <> :excludeClassId) " +
           "AND c.status IN ('OPEN', 'IN_PROGRESS') " +
           "AND c.startDate <= :endDate AND c.endDate >= :startDate " +
           "AND c.startTime < :endTime AND c.endTime > :startTime")
    List<ClassEntity> findConflictingTeacherClasses(
            @Param("teacherId") Integer teacherId,
            @Param("excludeClassId") Integer excludeClassId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);
}
