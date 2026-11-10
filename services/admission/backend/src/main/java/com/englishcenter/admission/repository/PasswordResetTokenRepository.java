package com.englishcenter.admission.repository;

import com.englishcenter.admission.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository thao tác bảng admission.password_reset_tokens hỗ trợ quy trình quên mật khẩu 15 phút.
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer>, AccountTokenStateRepository {

    /**
     * Tìm kiếm token đặt lại mật khẩu theo chuỗi token.
     *
     * @param token Chuỗi token
     * @return Optional chứa PasswordResetToken
     */
    Optional<PasswordResetToken> findByToken(String token);

    /** Khóa token sau khi khóa user để bảo đảm chỉ dùng một lần. */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PasswordResetToken p WHERE p.token = :token")
    Optional<PasswordResetToken> findByTokenForUpdate(@Param("token") String token);

    /** Giới hạn tần suất cấp link theo tài khoản. */
    boolean existsByUserIdAndCreatedAtAfter(Integer userId, java.time.LocalDateTime after);

    /**
     * Vô hiệu hóa toàn bộ các token đặt lại mật khẩu cũ của người dùng.
     *
     * @param userId ID người dùng
     */
    @Modifying
    @Query("UPDATE PasswordResetToken p SET p.isUsed = true WHERE p.userId = :userId AND p.isUsed = false")
    void invalidateAllByUserId(@Param("userId") Integer userId);
}
