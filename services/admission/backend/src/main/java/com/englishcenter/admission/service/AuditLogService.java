package com.englishcenter.admission.service;

import com.englishcenter.admission.common.dto.PageResponse;
import com.englishcenter.admission.dto.audit.AuditLogResponse;

import java.time.LocalDateTime;

/**
 * Giao diện định nghĩa các nghiệp vụ ghi nhận và tra cứu nhật ký kiểm toán bảo mật hệ thống.
 */
public interface AuditLogService {

    /**
     * Ghi nhận một sự kiện kiểm toán bảo mật mới vào cơ sở dữ liệu.
     *
     * @param userId    ID người dùng (nếu xác định được, hoặc null)
     * @param email     Email liên quan đến sự kiện
     * @param action    Hành động kiểm toán (LOGIN_SUCCESS, LOGIN_FAILED, ...)
     * @param ipAddress Địa chỉ IP của client
     * @param userAgent Chuỗi User-Agent của client
     * @param details   Thông tin chi tiết bổ sung
     */
    void logEvent(Integer userId, String email, String action, String ipAddress, String userAgent, String details);

    /**
     * Kiểm tra xem địa chỉ IP hoặc Email có dấu hiệu bị tấn công Brute-force hay không
     * (quá 5 lần đăng nhập thất bại trong 10 phút gần nhất).
     *
     * @param ipAddress Địa chỉ IP cần kiểm tra
     * @param email     Email cần kiểm tra
     * @return true nếu vượt ngưỡng cảnh báo tấn công, false nếu bình thường
     */
    boolean isBruteForceAttempt(String ipAddress, String email);

    /**
     * Truy vấn nhật ký kiểm toán bảo mật với bộ lọc linh hoạt và phân trang (dành cho Admin).
     *
     * @param userId    Lọc theo ID người dùng (hoặc null)
     * @param eventType Lọc theo loại hành động sự kiện (hoặc null)
     * @param fromDate  Mốc thời gian bắt đầu (hoặc null)
     * @param toDate    Mốc thời gian kết thúc (hoặc null)
     * @param page      Số trang (0-based)
     * @param size      Số bản ghi mỗi trang
     * @return PageResponse chứa danh sách AuditLogResponse
     */
    PageResponse<AuditLogResponse> getAuditLogs(Integer userId, String eventType,
                                                LocalDateTime fromDate, LocalDateTime toDate,
                                                int page, int size);
}
