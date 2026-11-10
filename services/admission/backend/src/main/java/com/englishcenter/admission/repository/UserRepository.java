package com.englishcenter.admission.repository;

import com.englishcenter.admission.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository truy vấn dữ liệu thực thể User trong bảng admission.users.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Integer>, AccountUserStateRepository {

    /**
     * Tìm kiếm người dùng theo địa chỉ email.
     *
     * @param email Địa chỉ email
     * @return Optional chứa User hoặc rỗng nếu không tìm thấy
     */
    Optional<User> findByEmail(String email);

    /** Khóa tài khoản trước khi cấp hoặc dùng token email. */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Integer id);

    /** INSERT nguyên tử cho đăng ký khách, không ghi đè danh tính nếu email được tạo đồng thời. */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "INSERT INTO admission.users(email,password_hash,full_name,phone,role,is_active,must_change_password) "
            + "VALUES (:email,:password,:name,:phone,'STUDENT',true,true) ON CONFLICT(email) DO NOTHING", nativeQuery = true)
    int insertGuestIfAbsent(@org.springframework.data.repository.query.Param("email") String email,
        @org.springframework.data.repository.query.Param("password") String passwordHash,
        @org.springframework.data.repository.query.Param("name") String name,
        @org.springframework.data.repository.query.Param("phone") String phone);

    /**
     * Kiểm tra sự tồn tại của người dùng theo địa chỉ email.
     *
     * @param email Địa chỉ email
     * @return true nếu email đã tồn tại, false nếu chưa
     */
    boolean existsByEmail(String email);

    /**
     * Lọc danh sách người dùng theo vai trò (phục vụ phân trang quản trị).
     *
     * @param role     Tên vai trò (ADMIN, STAFF, TEACHER, STUDENT)
     * @param pageable Tham số phân trang
     * @return Trang danh sách người dùng
     */
    Page<User> findByRole(String role, Pageable pageable);

    /**
     * Tìm kiếm và lọc danh sách người dùng theo vai trò và từ khóa (email hoặc họ tên).
     *
     * @param role     Vai trò người dùng (hoặc null nếu lấy tất cả)
     * @param keyword  Từ khóa tìm kiếm (hoặc null)
     * @param pageable Tham số phân trang
     * @return Trang danh sách người dùng khớp điều kiện
     */
    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE " +
            "(:role IS NULL OR u.role = :role) AND " +
            "(:keyword IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<User> searchUsers(@org.springframework.data.repository.query.Param("role") String role,
                           @org.springframework.data.repository.query.Param("keyword") String keyword,
                           Pageable pageable);
}
