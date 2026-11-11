package com.englishcenter.admission.controller;

import com.englishcenter.admission.common.dto.ApiResponse;
import com.englishcenter.admission.common.dto.PageResponse;
import com.englishcenter.admission.dto.audit.AuditLogResponse;
import com.englishcenter.admission.service.AuditLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Controller cung cấp API tra cứu nhật ký kiểm toán bảo mật và cảnh báo an ninh cho Quản trị viên.
 */
@RestController
@RequestMapping("/api/admission/audit-logs")
@CrossOrigin(origins = "*")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * Truy vấn danh sách nhật ký kiểm toán bảo mật có phân trang và lọc theo nhiều tiêu chí.
     * Chỉ người dùng có vai trò Quản trị viên (ADMIN) mới có quyền truy cập.
     *
     * @param userId    Lọc theo ID người dùng
     * @param eventType Lọc theo loại sự kiện (LOGIN_SUCCESS, LOGIN_FAILED, REFRESH_TOKEN, ...)
     * @param fromDate  Mốc thời gian bắt đầu lọc (định dạng ISO)
     * @param toDate    Mốc thời gian kết thúc lọc (định dạng ISO)
     * @param page      Số thứ tự trang (mặc định 0)
     * @param size      Số lượng bản ghi mỗi trang (mặc định 20)
     * @return ApiResponse chứa PageResponse các bản ghi kiểm toán
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getAuditLogs(
            @RequestParam(name = "user_id", required = false) Integer userId,
            @RequestParam(name = "event_type", required = false) String eventType,
            @RequestParam(name = "from_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(name = "to_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResponse<AuditLogResponse> response =
                auditLogService.getAuditLogs(userId, eventType, fromDate, toDate, page, size);

        return ResponseEntity.ok(ApiResponse.success(response, "Truy vấn nhật ký kiểm toán thành công"));
    }
}
