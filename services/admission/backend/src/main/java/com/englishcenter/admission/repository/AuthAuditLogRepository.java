package com.englishcenter.admission.repository;

import com.englishcenter.admission.entity.AuthAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * Repository thao tác bảng admission.auth_audit_logs ghi vết kiểm toán bảo mật và phát hiện brute-force.
 */
@Repository
public interface AuthAuditLogRepository extends JpaRepository<AuthAuditLog, Integer> {

    /**
     * Lấy danh sách nhật ký bảo mật theo người dùng, sắp xếp giảm dần theo thời gian.
     *
     * @param userId   ID người dùng
     * @param pageable Tham số phân trang
     * @return Trang nhật ký
     */
    Page<AuthAuditLog> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    /**
     * Lấy toàn bộ nhật ký hệ thống phân trang (dành cho Admin).
     *
     * @param pageable Tham số phân trang
     * @return Trang nhật ký
     */
    Page<AuthAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Đếm số lần thực hiện hành động (như LOGIN_FAILED) của email trong khoảng thời gian để phát hiện bất thường.
     *
     * @param email  Email tài khoản
     * @param action Hành động kiểm toán (ví dụ LOGIN_FAILED)
     * @param after  Mốc thời gian bắt đầu tính
     * @return Số lượng sự kiện khớp
     */
    long countByEmailAndActionAndCreatedAtAfter(String email, String action, LocalDateTime after);

    /**
     * Đếm số lần thực hiện hành động thất bại từ một địa chỉ IP để phát hiện tấn công Brute-force.
     *
     * @param ipAddress Địa chỉ IP của client
     * @param action    Hành động kiểm toán (LOGIN_FAILED)
     * @param after     Mốc thời gian bắt đầu tính
     * @return Số lượng sự kiện khớp
     */
    long countByIpAddressAndActionAndCreatedAtAfter(String ipAddress, String action, LocalDateTime after);

    /**
     * Tìm kiếm và lọc danh sách nhật ký kiểm toán theo nhiều tiêu chí linh hoạt.
     *
     * @param userId   ID người dùng (hoặc null)
     * @param action   Loại hành động sự kiện (hoặc null)
     * @param fromDate Mốc thời gian bắt đầu (hoặc null)
     * @param toDate   Mốc thời gian kết thúc (hoặc null)
     * @param pageable Tham số phân trang
     * @return Trang nhật ký kiểm toán khớp điều kiện
     */
    @org.springframework.data.jpa.repository.Query("SELECT a FROM AuthAuditLog a WHERE " +
            "(:userId IS NULL OR a.userId = :userId) AND " +
            "(:action IS NULL OR a.action = :action) AND " +
            "(:fromDate IS NULL OR a.createdAt >= :fromDate) AND " +
            "(:toDate IS NULL OR a.createdAt <= :toDate)")
    Page<AuthAuditLog> searchAuditLogs(
            @org.springframework.data.repository.query.Param("userId") Integer userId,
            @org.springframework.data.repository.query.Param("action") String action,
            @org.springframework.data.repository.query.Param("fromDate") LocalDateTime fromDate,
            @org.springframework.data.repository.query.Param("toDate") LocalDateTime toDate,
            Pageable pageable);
}
