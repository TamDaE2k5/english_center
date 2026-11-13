package com.englishcenter.admission.repository;

import com.englishcenter.admission.entity.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Kho truy vấn dữ liệu đơn đăng ký tuyển sinh (bảng admission.enrollments).
 * Quản lý cơ chế giữ chỗ 48h, bảo vệ sĩ số trần INV-ADM-01 và kiểm soát luồng trạng thái đơn.
 */
@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Integer>, EnrollmentStateRepository {
    /** Ghi yêu cầu đối soát hoàn phí bền vững, một đơn chỉ tạo một yêu cầu khi hủy lớp. */
    @Modifying
    @Query(value="INSERT INTO admission.class_cancellation_requests(enrollment_id,class_id,invoice_code,reason,requested_by) VALUES (:enrollment,:clazz,:invoice,:reason,:actor) ON CONFLICT(enrollment_id) DO NOTHING", nativeQuery=true)
    int enqueueClassRefund(@Param("enrollment") Integer enrollment, @Param("clazz") Integer clazz,
        @Param("invoice") String invoice, @Param("reason") String reason, @Param("actor") Integer actor);

    /**
     * Đếm số lượng đơn theo lớp và trạng thái cụ thể (ví dụ: APPROVED).
     *
     * @param classId ID lớp học
     * @param status Trạng thái đơn
     * @return Số lượng bản ghi
     */
    long countByClassIdAndStatus(Integer classId, String status);

    /**
     * Đếm tổng số slot đang bị chiếm chỗ trong lớp:
     * Gồm các đơn APPROVED + các đơn PENDING còn trong thời hạn 48h (hold_expires_at > now hoặc null).
     *
     * @param classId ID lớp học
     * @param now Thời điểm hiện tại
     * @return Tổng số slot đang bị giữ
     */
    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.classId = :classId " +
           "AND (e.status = 'APPROVED' OR (e.status = 'PENDING' AND (e.holdExpiresAt IS NULL OR e.holdExpiresAt > :now)))")
    long countOccupiedSlots(@Param("classId") Integer classId, @Param("now") LocalDateTime now);

    /**
     * Đếm số đơn PENDING còn trong thời hạn 48h giữ chỗ.
     *
     * @param classId ID lớp học
     * @param now Thời điểm hiện tại
     * @return Số lượng đơn giữ chỗ đang có hiệu lực
     */
    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.classId = :classId " +
           "AND e.status = 'PENDING' AND (e.holdExpiresAt IS NULL OR e.holdExpiresAt > :now)")
    long countActiveHeldSlots(@Param("classId") Integer classId, @Param("now") LocalDateTime now);

    /**
     * Kiểm tra xem học viên đã có đơn đăng ký vào lớp này chưa.
     *
     * @param studentId ID học viên
     * @param classId ID lớp học
     * @return true nếu đã có đơn đăng ký
     */
    boolean existsByStudentIdAndClassId(Integer studentId, Integer classId);

    /**
     * Tìm đơn đăng ký theo cặp học viên và lớp học.
     *
     * @param studentId ID học viên
     * @param classId ID lớp học
     * @return Optional chứa Enrollment
     */
    Optional<Enrollment> findByStudentIdAndClassId(Integer studentId, Integer classId);

    /**
     * Cập nhật hàng loạt các đơn giữ chỗ PENDING quá hạn sang trạng thái EXPIRED (TASK-ADM-43).
     *
     * @param now Thời điểm hiện tại
     * @return Số lượng bản ghi đã được cập nhật sang EXPIRED
     */
    @Modifying
    @Query("UPDATE Enrollment e SET e.status = 'EXPIRED', e.updatedAt = :now " +
           "WHERE e.status = 'PENDING' AND e.holdExpiresAt IS NOT NULL AND e.holdExpiresAt <= :now")
    int expireOverdueEnrollments(@Param("now") LocalDateTime now);

    /**
     * Tìm danh sách đơn giữ chỗ PENDING đã quá hạn 48h cần giải phóng.
     *
     * @param now Thời điểm hiện tại
     * @return Danh sách đơn quá hạn
     */
    @Query("SELECT e FROM Enrollment e WHERE e.status = 'PENDING' AND e.holdExpiresAt IS NOT NULL AND e.holdExpiresAt <= :now")
    List<Enrollment> findExpiredPendingEnrollments(@Param("now") LocalDateTime now);

    /**
     * Lấy danh sách đơn đăng ký của một học viên cụ thể (phục vụ cách ly dữ liệu INV-SEC-01).
     *
     * @param studentId ID học viên
     * @return Danh sách đơn của học viên
     */
    List<Enrollment> findByStudentIdOrderByCreatedAtDesc(Integer studentId);

    /**
     * Tìm kiếm và phân trang danh sách đơn đăng ký phục vụ nghiệp vụ tuyển sinh học vụ.
     *
     * @param classId ID lớp học (tùy chọn)
     * @param status Trạng thái đơn (tùy chọn)
     * @param pageable Phân trang
     * @return Trang kết quả đơn đăng ký
     */
    @Query("SELECT e FROM Enrollment e LEFT JOIN FETCH e.student LEFT JOIN FETCH e.clazz WHERE " +
           "(:classId IS NULL OR e.classId = :classId) AND " +
           "(:status IS NULL OR e.status = :status)")
    Page<Enrollment> searchEnrollments(@Param("classId") Integer classId,
                                       @Param("status") String status,
                                       Pageable pageable);

    /**
     * Lấy danh sách toàn bộ đơn đăng ký của một lớp học (phục vụ xuất danh sách CSV).
     *
     * @param classId ID lớp học
     * @return Danh sách đơn đăng ký
     */
    @Query("SELECT e FROM Enrollment e LEFT JOIN FETCH e.student WHERE e.classId = :classId ORDER BY e.id ASC")
    List<Enrollment> findByClassIdOrderByCreatedAtAsc(@Param("classId") Integer classId);

    /**
     * Đếm tổng số đơn đăng ký phát sinh trong khoảng thời gian.
     */
    long countByEnrolledAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Đếm số đơn đăng ký theo trạng thái trong khoảng thời gian.
     */
    long countByStatusAndEnrolledAtBetween(String status, LocalDateTime start, LocalDateTime end);

    /**
     * Đếm tổng số đơn đăng ký của danh sách lớp trong khoảng thời gian.
     */
    long countByClassIdInAndEnrolledAtBetween(List<Integer> classIds, LocalDateTime start, LocalDateTime end);

    /**
     * Đếm số đơn theo trạng thái của danh sách lớp trong khoảng thời gian.
     */
    long countByClassIdInAndStatusAndEnrolledAtBetween(List<Integer> classIds, String status, LocalDateTime start, LocalDateTime end);
}
