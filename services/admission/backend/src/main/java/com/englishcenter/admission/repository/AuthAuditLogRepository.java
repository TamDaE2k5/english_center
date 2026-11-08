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
}
