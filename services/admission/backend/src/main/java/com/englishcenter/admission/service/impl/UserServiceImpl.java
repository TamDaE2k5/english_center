package com.englishcenter.admission.service.impl;

import com.englishcenter.admission.entity.User;
import com.englishcenter.admission.repository.UserRepository;
import com.englishcenter.admission.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;

/**
 * Lớp triển khai các nghiệp vụ quản lý tài khoản người dùng và định danh.
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Tự động tìm kiếm hoặc khởi tạo tài khoản học viên từ email.
     * Quy trình xử lý từng bước:
     * 1. Chuẩn hóa email (xóa khoảng trắng thừa, chuyển về chữ thường).
     * 2. Kiểm tra trong cơ sở dữ liệu xem email đã tồn tại hay chưa.
     * 3. Nếu đã tồn tại: Tái sử dụng tài khoản cũ, cập nhật số điện thoại nếu tài khoản cũ chưa có.
     * 4. Nếu chưa tồn tại:
     *    - Sinh mật khẩu ngẫu nhiên an toàn gồm 10 ký tự.
     *    - Băm mật khẩu bằng thuật toán BCrypt.
     *    - Gán vai trò mặc định là STUDENT, kích hoạt tài khoản và bật cờ yêu cầu đổi mật khẩu.
     *    - Lưu vào bảng admission.users và trả về thực thể vừa tạo.
     *
     * @param email    Địa chỉ email của học viên
     * @param fullName Họ và tên đầy đủ
     * @param phone    Số điện thoại liên lạc
     * @return Thực thể User đại diện cho học viên
     */
    @Override
    @Transactional
    public User getOrCreateStudentUser(String email, String fullName, String phone) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email không được để trống khi khởi tạo tài khoản học viên");
        }

        String normalizedEmail = email.trim().toLowerCase();
        Optional<User> existingUserOpt = userRepository.findByEmail(normalizedEmail);

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            log.info("Học viên với email {} đã tồn tại (ID={}), tiến hành tái sử dụng tài khoản", normalizedEmail, existingUser.getId());
            // Cập nhật số điện thoại nếu thông tin trước đó chưa có
            if ((existingUser.getPhone() == null || existingUser.getPhone().isEmpty()) && phone != null && !phone.isEmpty()) {
                existingUser.setPhone(phone.trim());
                userRepository.save(existingUser);
            }
            return existingUser;
        }

        // Tạo mật khẩu ngẫu nhiên tạm thời cho học viên
        String rawPassword = generateRandomPassword(10);
        String passwordHash = passwordEncoder.encode(rawPassword);

        User newUser = new User();
        newUser.setEmail(normalizedEmail);
        newUser.setFullName(fullName != null ? fullName.trim() : "Học Viên Mới");
        newUser.setPhone(phone != null ? phone.trim() : null);
        newUser.setPasswordHash(passwordHash);
        newUser.setRole("STUDENT");
        newUser.setIsActive(true);
        newUser.setMustChangePassword(true);

        User savedUser = userRepository.save(newUser);
        log.info("Đã tự động khởi tạo tài khoản học viên mới: ID={}, email={}, role=STUDENT", savedUser.getId(), savedUser.getEmail());

        return savedUser;
    }

    /**
     * Hàm tiện ích sinh chuỗi mật khẩu ngẫu nhiên an toàn.
     *
     * @param length Độ dài mật khẩu mong muốn
     * @return Chuỗi mật khẩu ngẫu nhiên
     */
    private String generateRandomPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }
}
