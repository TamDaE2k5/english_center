package com.englishcenter.admission.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Danh mục toàn bộ mã lỗi chuẩn của phân hệ Admission & Identity Service.
 * Tuân thủ mục 8.4 trong docs/MASTER_DB_AND_API_RULES.md:
 * - Nhóm xác thực dùng tiền tố AUTH_
 * - Nhóm tuyển sinh & đào tạo dùng tiền tố ADM_
 */
public enum ErrorCode {

    // ==========================================
    // 1. Nhóm Xác Thực & Bảo Mật (AUTH_*)
    // ==========================================
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không chính xác"),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Phiên làm việc đã hết hạn, vui lòng làm mới token"),
    AUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Token xác thực không hợp lệ hoặc đã bị thu hồi"),
    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Yêu cầu đăng nhập để truy cập tài nguyên này"),
    AUTH_ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "Tài khoản của bạn đã bị khóa, vui lòng liên hệ quản trị viên"),
    AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện chức năng này"),
    AUTH_EMAIL_UNVERIFIED(HttpStatus.FORBIDDEN, "Vui lòng xác minh email và thiết lập mật khẩu trước khi tiếp tục"),
    AUTH_EMAIL_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Chưa thể gửi email. Vui lòng thử lại sau"),

    // ==========================================
    // 2. Nhóm Người Dùng & Tài Khoản (ADM_USER_*)
    // ==========================================
    ADM_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin người dùng"),
    ADM_USER_EMAIL_EXISTS(HttpStatus.CONFLICT, "Địa chỉ email đã tồn tại trong hệ thống"),

    // ==========================================
    // 3. Nhóm Phòng Học (ADM_ROOM_*)
    // ==========================================
    ADM_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy phòng học"),
    ADM_ROOM_CODE_EXISTS(HttpStatus.CONFLICT, "Mã phòng học đã tồn tại"),
    ADM_ROOM_CAPACITY_EXCEEDED(HttpStatus.BAD_REQUEST, "Sĩ số lớp vượt quá sức chứa tối đa của phòng học"),
    ADM_ROOM_CONFLICT(HttpStatus.CONFLICT, "Phòng học bị trùng lịch với lớp học khác đang mở"),

    // ==========================================
    // 4. Nhóm Khóa Học (ADM_COURSE_*)
    // ==========================================
    ADM_COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học"),
    ADM_COURSE_CODE_EXISTS(HttpStatus.CONFLICT, "Mã khóa học đã tồn tại trong hệ thống"),
    ADM_COURSE_HAS_CLASSES(HttpStatus.CONFLICT, "Không thể xóa hoặc ngưng khóa học đang có lớp mở"),

    // ==========================================
    // 5. Nhóm Lớp Học (ADM_CLASS_*)
    // ==========================================
    ADM_CLASS_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy lớp học"),
    ADM_CLASS_FULL(HttpStatus.BAD_REQUEST, "Lớp học đã đạt sĩ số tối đa, không thể nhận thêm"),
    ADM_CLASS_NOT_OPEN(HttpStatus.BAD_REQUEST, "Lớp học hiện không ở trạng thái mở đăng ký tuyển sinh"),
    ADM_TEACHER_CONFLICT(HttpStatus.CONFLICT, "Giảng viên bị trùng lịch dạy với lớp học khác trong cùng khung giờ"),

    // ==========================================
    // 6. Nhóm Tuyển Sinh & Giữ Chỗ (ADM_ENROLLMENT_*)
    // ==========================================
    ADM_ENROLLMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy đơn đăng ký tuyển sinh"),
    ADM_ENROLLMENT_EXISTS(HttpStatus.CONFLICT, "Học viên đã có đơn đăng ký cho lớp học này"),
    ADM_CASH_CONFLICT(HttpStatus.CONFLICT, "Chứng từ hoặc yêu cầu tiền mặt đã tồn tại với dữ liệu khác; cần đối soát"),
    ADM_HOLD_EXPIRED(HttpStatus.BAD_REQUEST, "Thời hạn giữ chỗ 48h của đơn đăng ký đã hết hạn"),
    ADM_INVALID_STATE_TRANSITION(HttpStatus.BAD_REQUEST, "Chuyển trạng thái đơn không hợp lệ theo quy trình nghiệp vụ"),

    // ==========================================
    // 7. Nhóm Khảo Thí Đầu Vào (ADM_TEST_*)
    // ==========================================
    ADM_TEST_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy bộ đề khảo thí"),
    ADM_QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy câu hỏi khảo thí"),
    ADM_SUBMISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy kết quả bài thi"),
    ADM_TEST_ALREADY_SUBMITTED(HttpStatus.BAD_REQUEST, "Lượt thi này đã nộp bài trước đó, không thể nộp lại"),
    ADM_ATTEMPT_EXPIRED(HttpStatus.BAD_REQUEST, "Lượt thi đã hết thời gian làm bài quy định"),
    ADM_RULE_OVERLAP(HttpStatus.CONFLICT, "Dải điểm của quy tắc đề xuất bị chồng lấn với quy tắc đã có"),
    ADM_RULE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy quy tắc đề xuất khóa học"),
    ADM_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên yêu cầu"),

    // ==========================================
    // 8. Lỗi Hệ Thống & Tích Hợp Liên Service
    // ==========================================
    ADM_VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Dữ liệu yêu cầu không hợp lệ"),
    ADM_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Dịch vụ liên kết tạm thời không khả dụng, vui lòng thử lại sau"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Đã xảy ra lỗi nội bộ hệ thống, vui lòng thử lại sau");

    /** Mã trạng thái HTTP tương ứng */
    private final HttpStatus httpStatus;

    /** Thông điệp lỗi mặc định bằng tiếng Việt */
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
