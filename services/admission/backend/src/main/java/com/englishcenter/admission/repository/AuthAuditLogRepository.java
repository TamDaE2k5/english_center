package com.englishcenter.admission.repository;

import com.englishcenter.admission.entity.AuthAuditLog;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository thao tác bảng admission.auth_audit_logs ghi vết kiểm toán bảo mật và phát hiện brute-force.
 */
@Repository
public interface AuthAuditLogRepository extends JpaRepository<AuthAuditLog, Integer>, JpaSpecificationExecutor<AuthAuditLog> {

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
    default Page<AuthAuditLog> searchAuditLogs(
            Integer userId,
            String action,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable) {
        return findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (userId != null) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }
            if (action != null && !action.isBlank()) {
                predicates.add(cb.equal(root.get("action"), action));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }

    /**
     * Đếm số lượng người dùng duy nhất đăng nhập thành công từ mốc thời gian (DAU / MAU, TASK-ADM-53).
     */
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT a.userId) FROM AuthAuditLog a WHERE a.action = 'LOGIN_SUCCESS' AND a.userId IS NOT NULL AND a.createdAt >= :since")
    long countDistinctActiveUsersSince(@org.springframework.data.repository.query.Param("since") LocalDateTime since);

    /**
     * Đếm số lượng người dùng duy nhất theo vai trò đăng nhập thành công từ mốc thời gian.
     */
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT a.userId) FROM AuthAuditLog a, User u WHERE a.userId = u.id AND a.action = 'LOGIN_SUCCESS' AND u.role = :role AND a.createdAt >= :since")
    long countDistinctActiveUsersByRoleSince(@org.springframework.data.repository.query.Param("role") String role, @org.springframework.data.repository.query.Param("since") LocalDateTime since);

    /**
     * Đếm tổng số lượt thử đăng nhập (thành công hoặc thất bại) từ mốc thời gian.
     */
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(a) FROM AuthAuditLog a WHERE a.action IN ('LOGIN_SUCCESS', 'LOGIN_FAILED') AND a.createdAt >= :since")
    long countLoginAttemptsSince(@org.springframework.data.repository.query.Param("since") LocalDateTime since);

    /**
     * Đếm số lượt đăng nhập thất bại từ mốc thời gian.
     */
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(a) FROM AuthAuditLog a WHERE a.action = 'LOGIN_FAILED' AND a.createdAt >= :since")
    long countFailedLoginAttemptsSince(@org.springframework.data.repository.query.Param("since") LocalDateTime since);

    /**
     * Lấy danh sách Top các địa chỉ IP có lượt đăng nhập thất bại nhiều nhất (phát hiện tấn công brute-force).
     */
    @org.springframework.data.jpa.repository.Query("SELECT a.ipAddress, COUNT(a.id), MAX(a.createdAt) FROM AuthAuditLog a " +
            "WHERE a.action = 'LOGIN_FAILED' AND a.ipAddress IS NOT NULL AND a.createdAt >= :since " +
            "GROUP BY a.ipAddress ORDER BY COUNT(a.id) DESC")
    java.util.List<Object[]> findTopSuspiciousIps(@org.springframework.data.repository.query.Param("since") LocalDateTime since, Pageable pageable);
}
