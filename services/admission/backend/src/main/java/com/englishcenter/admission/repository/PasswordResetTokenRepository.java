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
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {

    /**
     * Tìm kiếm token đặt lại mật khẩu theo chuỗi token.
     *
     * @param token Chuỗi token
     * @return Optional chứa PasswordResetToken
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Vô hiệu hóa toàn bộ các token đặt lại mật khẩu cũ của người dùng.
     *
     * @param userId ID người dùng
     */
    @Modifying
    @Query("UPDATE PasswordResetToken p SET p.isUsed = true WHERE p.userId = :userId AND p.isUsed = false")
    void invalidateAllByUserId(@Param("userId") Integer userId);
}
