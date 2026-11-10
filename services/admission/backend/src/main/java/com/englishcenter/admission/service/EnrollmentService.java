package com.englishcenter.admission.service;

import com.englishcenter.admission.dto.enrollment.CreateEnrollmentRequest;
import com.englishcenter.admission.dto.enrollment.EnrollmentResponse;
import com.englishcenter.admission.dto.enrollment.ExtendHoldRequest;
import com.englishcenter.admission.dto.enrollment.ManualApproveRequest;
import com.englishcenter.admission.dto.webhook.PaymentSuccessWebhookRequest;
import com.englishcenter.admission.entity.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Giao diện dịch vụ quản lý Tuyển sinh, Giữ chỗ 48h và Xét duyệt (Phase 4: TASK-ADM-40 -> TASK-ADM-47).
 * Đảm bảo các quy tắc bất biến:
 * - INV-ADM-01: Không bao giờ duyệt đơn nếu lớp đã đủ sĩ số tối đa.
 * - INV-ADM-03: Không cho phép đơn CANCELLED/REJECTED chuyển trực tiếp sang APPROVED.
 * - INV-ENR-02: Độc đẳng Idempotency khi nhận webhook từ Finance Service.
 * - INV-ENR-03: Xử lý đối soát thanh toán đến muộn sau khi đơn đã EXPIRED.
 * - INV-SEC-01: Cách ly dữ liệu học viên (học viên chỉ xem đơn của chính mình).
 */
public interface EnrollmentService {

    /**
     * Tạo đơn đăng ký tuyển sinh nhập học (Online hoặc tại quầy) kèm giữ chỗ 48h (TASK-ADM-40, TASK-ADM-42).
     *
     * @param request DTO chứa thông tin học viên và lớp học đăng ký
     * @return DTO thông tin đơn đăng ký vừa được tạo
     */
    EnrollmentResponse createEnrollment(CreateEnrollmentRequest request);

    /**
     * Lõi duyệt đơn đăng ký tuyển sinh sang APPROVED kèm khóa bi quan PESSIMISTIC_WRITE (TASK-ADM-41, INV-ADM-01).
     *
     * @param enrollmentId ID đơn đăng ký
     * @return Thực thể Enrollment đã chuyển trạng thái APPROVED
     */
    Enrollment approveEnrollment(Integer enrollmentId);

    /**
     * STAFF/ADMIN gửi yêu cầu đối soát tiền mặt, chưa tự công nhận PAID/APPROVED (FR-ENR-07 mới).
     *
     * @param enrollmentId ID đơn đăng ký
     * @param request      DTO chứa thông tin phiếu thu và phương thức thanh toán
     * @return DTO đơn vẫn PENDING kèm yêu cầu chờ Finance xác nhận
     */
    EnrollmentResponse approveManual(Integer enrollmentId, ManualApproveRequest request);

    /**
     * Tiếp nhận và xử lý webhook xác nhận thanh toán thành công từ Finance Service (TASK-ADM-45, INV-ENR-02, INV-ENR-03).
     *
     * @param request DTO webhook từ Finance Service
     * @return DTO kết quả xử lý đơn đăng ký
     */
    EnrollmentResponse processPaymentSuccess(PaymentSuccessWebhookRequest request);

    /**
     * Gia hạn thời gian giữ chỗ tuyển sinh thêm số giờ chỉ định bởi Học vụ (TASK-ADM-44).
     *
     * @param enrollmentId ID đơn đăng ký cần gia hạn
     * @param request      DTO số giờ gia hạn và lý do nghiệp vụ
     * @return DTO thông tin đơn đăng ký với thời hạn mới
     */
    EnrollmentResponse extendHold(Integer enrollmentId, ExtendHoldRequest request);

    /**
     * Hủy đơn đăng ký tuyển sinh theo yêu cầu học viên hoặc học vụ.
     *
     * @param enrollmentId ID đơn đăng ký
     * @param reason       Lý do hủy đơn
     * @return DTO đơn sau khi hủy
     */
    EnrollmentResponse cancelEnrollment(Integer enrollmentId, String reason);

    /**
     * Quét và cập nhật hàng loạt các đơn giữ chỗ PENDING đã quá hạn 48h sang EXPIRED (TASK-ADM-43).
     *
     * @return Số lượng đơn đã được chuyển sang EXPIRED
     */
    int expireOverdueEnrollments();

    /**
     * Tra cứu chi tiết một đơn đăng ký kèm kiểm tra phân quyền cách ly dữ liệu (INV-SEC-01).
     *
     * @param id ID đơn đăng ký
     * @return DTO thông tin chi tiết đơn
     */
    EnrollmentResponse getEnrollmentById(Integer id);

    /**
     * Tra cứu và phân trang danh sách đơn đăng ký phục vụ bàn tuyển sinh học vụ (TASK-ADM-47).
     *
     * @param classId  Lọc theo ID lớp học (tùy chọn)
     * @param status   Lọc theo trạng thái đơn (tùy chọn)
     * @param pageable Tham số phân trang
     * @return Trang danh sách đơn đăng ký
     */
    Page<EnrollmentResponse> searchEnrollments(Integer classId, String status, Pageable pageable);

    /**
     * Lấy danh sách toàn bộ đơn đăng ký của học viên đang đăng nhập.
     *
     * @return Danh sách đơn đăng ký của chính học viên
     */
    List<EnrollmentResponse> getMyEnrollments();
}
