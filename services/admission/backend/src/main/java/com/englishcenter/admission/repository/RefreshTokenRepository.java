package com.englishcenter.admission.repository;

import com.englishcenter.admission.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository thao tác bảng admission.refresh_tokens phục vụ quản lý phiên và xoay vòng token.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {

    /**
     * Tìm kiếm refresh token theo chuỗi token.
     *
     * @param token Chuỗi token
     * @return Optional chứa RefreshToken
     */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findByToken(String token);

    /**
     * Lấy danh sách refresh token đang hoạt động (chưa bị thu hồi) của người dùng.
     *
     * @param userId ID người dùng
     * @return Danh sách các token còn hiệu lực
     */
    List<RefreshToken> findByUserIdAndIsRevokedFalse(Integer userId);

    /**
     * Thu hồi toàn bộ các Refresh Token của một người dùng (khi khóa tài khoản hoặc đăng xuất toàn bộ).
     *
     * @param userId ID người dùng
     */
    @Modifying
    @Query("UPDATE RefreshToken r SET r.isRevoked = true WHERE r.userId = :userId AND r.isRevoked = false")
    void revokeAllByUserId(@Param("userId") Integer userId);
}
