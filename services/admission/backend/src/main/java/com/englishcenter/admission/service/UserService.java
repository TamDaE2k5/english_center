package com.englishcenter.admission.service;

import com.englishcenter.admission.common.dto.PageResponse;
import com.englishcenter.admission.dto.user.CreateUserRequest;
import com.englishcenter.admission.dto.user.UpdateUserRequest;
import com.englishcenter.admission.dto.user.UserResponse;
import com.englishcenter.admission.entity.User;

/**
 * Giao diện định nghĩa các nghiệp vụ quản lý tài khoản người dùng và định danh.
 */
public interface UserService {
    /** Lookup tối thiểu cho học vụ xếp giáo viên, không lộ hồ sơ quản trị người dùng. */
    java.util.List<TeacherOption> listTeacherOptions();
    record TeacherOption(Integer id, String fullName) {}

    /** Tạo mới tài khoản khách thi; email tồn tại bắt buộc đăng nhập, tuyệt đối không tái sử dụng. */
    User createGuestStudentUser(String email, String fullName, String phone);

    /**
     * Tự động tìm kiếm hoặc khởi tạo tài khoản học viên từ thông tin nộp đơn hoặc làm bài khảo thí.
     * Nếu email đã tồn tại, trả về tài khoản hiện có; nếu chưa, sinh tài khoản STUDENT với mật khẩu ngẫu nhiên.
     *
     * @param email    Địa chỉ email của học viên
     * @param fullName Họ và tên đầy đủ
     * @param phone    Số điện thoại liên lạc
     * @return Thực thể User đại diện cho học viên
     */
    User getOrCreateStudentUser(String email, String fullName, String phone);

    /**
     * Tạo tài khoản nhân sự mới (STAFF hoặc TEACHER) bởi Quản trị viên (ADMIN).
     *
     * @param request DTO chứa thông tin nhân viên mới
     * @return DTO UserResponse thông tin tài khoản vừa tạo
     */
    UserResponse createUser(CreateUserRequest request);

    /**
     * Lấy chi tiết thông tin tài khoản người dùng theo ID (kiểm tra ranh giới bảo mật INV-SEC-01).
     *
     * @param id ID người dùng cần xem
     * @return DTO UserResponse
     */
    UserResponse getUserById(Integer id);

    /**
     * Truy vấn danh sách tài khoản người dùng có phân trang và lọc theo vai trò, từ khóa.
     *
     * @param role    Vai trò người dùng (ADMIN, STAFF, TEACHER, STUDENT) hoặc null
     * @param keyword Từ khóa tìm kiếm theo email hoặc họ tên (hoặc null)
     * @param page    Số trang (0-based)
     * @param size    Số lượng bản ghi mỗi trang
     * @return PageResponse chứa danh sách UserResponse
     */
    PageResponse<UserResponse> listUsers(String role, String keyword, int page, int size);

    /**
     * Cập nhật thông tin hồ sơ người dùng (chỉ ADMIN hoặc chính chủ tài khoản mới được cập nhật).
     *
     * @param id      ID người dùng cần cập nhật
     * @param request DTO chứa thông tin cập nhật
     * @return DTO UserResponse sau cập nhật
     */
    UserResponse updateUser(Integer id, UpdateUserRequest request);

    /**
     * Khóa hoặc mở khóa tài khoản người dùng (chỉ ADMIN).
     * Khi khóa tài khoản, lập tức thu hồi toàn bộ Refresh Token đang hoạt động và ghi nhật ký kiểm toán.
     *
     * @param id       ID người dùng cần thao tác
     * @param isActive Trạng thái mong muốn (true: mở khóa, false: khóa)
     * @param reason   Lý do khóa hoặc mở khóa
     * @return DTO UserResponse sau khi cập nhật trạng thái
     */
    UserResponse toggleUserStatus(Integer id, boolean isActive, String reason);
}
