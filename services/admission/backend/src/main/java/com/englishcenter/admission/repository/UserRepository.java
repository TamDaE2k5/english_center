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
public interface UserRepository extends JpaRepository<User, Integer> {

    /**
     * Tìm kiếm người dùng theo địa chỉ email.
     *
     * @param email Địa chỉ email
     * @return Optional chứa User hoặc rỗng nếu không tìm thấy
     */
    Optional<User> findByEmail(String email);

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
}
