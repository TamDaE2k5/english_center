package com.englishcenter.admission.service;

import com.englishcenter.admission.entity.User;

/**
 * Giao diện định nghĩa các nghiệp vụ quản lý tài khoản người dùng và định danh.
 */
public interface UserService {

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
}
