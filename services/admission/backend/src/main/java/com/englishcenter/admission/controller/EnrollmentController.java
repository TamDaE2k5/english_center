package com.englishcenter.admission.controller;

import com.englishcenter.admission.common.dto.ApiResponse;
import com.englishcenter.admission.dto.enrollment.CreateEnrollmentRequest;
import com.englishcenter.admission.dto.enrollment.EnrollmentResponse;
import com.englishcenter.admission.dto.enrollment.ExtendHoldRequest;
import com.englishcenter.admission.dto.enrollment.ManualApproveRequest;
import com.englishcenter.admission.service.EnrollmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller tiếp nhận và xử lý các yêu cầu tuyển sinh, giữ chỗ 48h và xét duyệt đơn nhập học (Phase 4).
 * Tuân thủ chuẩn RESTful API, định dạng JSON snake_case, và bảo vệ các quy tắc bất biến INV-ADM-01, INV-ADM-03, INV-SEC-01.
 */
@RestController
@RequestMapping("/api/admission/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    /**
     * Nộp đơn đăng ký tuyển sinh nhập học (Online hoặc Trực tiếp tại quầy) kèm giữ chỗ 48h (TASK-ADM-40, TASK-ADM-42).
     *
     * @param request DTO thông tin học viên và lớp học đăng ký
     * @return DTO thông tin đơn đăng ký vừa được khởi tạo
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','STUDENT')")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> createEnrollment(@Valid @RequestBody CreateEnrollmentRequest request) {
        EnrollmentResponse response = enrollmentService.createEnrollment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tạo đơn đăng ký thành công, thời gian giữ chỗ của bạn là 48 giờ"));
    }

    /**
     * Tra cứu và phân trang danh sách đơn đăng ký (Học vụ / Quản trị viên).
     *
     * @param classId Lọc theo ID lớp học (tùy chọn)
     * @param status  Lọc theo trạng thái đơn (tùy chọn)
     * @param page    Chỉ số trang (mặc định: 0)
     * @param size    Kích thước trang (mặc định: 20)
     * @return Trang danh sách đơn đăng ký
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<EnrollmentResponse>>> searchEnrollments(
            @RequestParam(name = "class_id", required = false) Integer classId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<EnrollmentResponse> responses = enrollmentService.searchEnrollments(classId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(responses, "Tra cứu danh sách đơn tuyển sinh thành công"));
    }

    /**
     * Lấy danh sách toàn bộ đơn đăng ký của chính học viên đang đăng nhập (INV-SEC-01).
     *
     * @return Danh sách đơn đăng ký của học viên
     */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<EnrollmentResponse>>> getMyEnrollments() {
        List<EnrollmentResponse> list = enrollmentService.getMyEnrollments();
        return ResponseEntity.ok(ApiResponse.success(list, "Lấy danh sách đơn đăng ký của học viên thành công"));
    }

    /**
     * Xem thông tin chi tiết một đơn đăng ký theo ID (INV-SEC-01).
     *
     * @param id ID đơn đăng ký
     * @return Thông tin chi tiết đơn đăng ký
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> getEnrollmentById(@PathVariable("id") Integer id) {
        EnrollmentResponse response = enrollmentService.getEnrollmentById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin chi tiết đơn đăng ký thành công"));
    }

    /**
     * Gia hạn thời gian giữ chỗ thêm giờ theo lý do nghiệp vụ học vụ (TASK-ADM-44).
     *
     * @param id      ID đơn đăng ký
     * @param request DTO chứa số giờ gia hạn và lý do nghiệp vụ
     * @return Đơn đăng ký sau khi cập nhật thời hạn giữ chỗ mới
     */
    @PostMapping("/{id}/extend-hold")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> extendHold(
            @PathVariable("id") Integer id,
            @Valid @RequestBody ExtendHoldRequest request) {
        EnrollmentResponse response = enrollmentService.extendHold(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Gia hạn giữ chỗ thành công thêm " + request.getExtendedHours() + " giờ"));
    }

    /**
     * STAFF/ADMIN gửi yêu cầu Finance đối soát tiền mặt; chỉ callback hợp lệ mới được duyệt.
     *
     * @param id      ID đơn đăng ký
     * @param request DTO chứa mã phiếu thu và số tiền thực nhận
     * @return HTTP 202, đơn vẫn PENDING và yêu cầu NEEDS_FINANCE_CONFIRMATION
     */
    @RequestMapping(value = "/{id}/approve-manual", method = {RequestMethod.POST, RequestMethod.PUT})
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> approveManual(
            @PathVariable("id") Integer id,
            @Valid @RequestBody ManualApproveRequest request) {
        EnrollmentResponse response = enrollmentService.approveManual(id, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(response,
                "Đã ghi nhận yêu cầu, chờ Finance xác nhận; chưa duyệt nhập học"));
    }

    /**
     * Hủy đơn đăng ký tuyển sinh theo yêu cầu học viên hoặc học vụ.
     *
     * @param id      ID đơn đăng ký
     * @param payload Request body tùy chọn chứa trường "reason"
     * @return Đơn đăng ký sau khi chuyển sang trạng thái CANCELLED
     */
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> cancelEnrollment(
            @PathVariable("id") Integer id,
            @RequestBody(required = false) Map<String, String> payload) {
        String reason = (payload != null && payload.containsKey("reason")) ? payload.get("reason") : "Người dùng yêu cầu hủy đơn";
        EnrollmentResponse response = enrollmentService.cancelEnrollment(id, reason);
        return ResponseEntity.ok(ApiResponse.success(response, "Hủy đơn đăng ký thành công"));
    }
}
