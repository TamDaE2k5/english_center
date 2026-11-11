package com.englishcenter.admission.controller;

import com.englishcenter.admission.common.dto.ApiResponse;
import com.englishcenter.admission.dto.enrollment.EnrollmentResponse;
import com.englishcenter.admission.dto.webhook.PaymentSuccessWebhookRequest;
import com.englishcenter.admission.service.EnrollmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller tiếp nhận các sự kiện Webhook nội bộ từ các microservice khác (TASK-ADM-45).
 * Chịu trách nhiệm xử lý callback thanh toán thành công từ Finance Service,
 * kích hoạt Invariant Guard INV-ADM-01 và đối soát Idempotency INV-ENR-02.
 */
@RestController
@RequestMapping("/api/admission/webhooks")
public class WebhookController {

    private final EnrollmentService enrollmentService;

    public WebhookController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    /**
     * Webhook tiếp nhận sự kiện thanh toán hóa đơn học phí thành công từ Finance Service (TASK-ADM-45, INV-ENR-02, INV-ENR-03).
     *
     * @param request DTO webhook chứa event_id, enrollment_id, invoice_id, amount_paid, paid_at
     * @return Phản hồi xác nhận xử lý thành công
     */
    @PostMapping("/payment-success")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> handlePaymentSuccess(
            @Valid @RequestBody PaymentSuccessWebhookRequest request) {
        EnrollmentResponse response = enrollmentService.processPaymentSuccess(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Xác nhận thanh toán và cập nhật đơn tuyển sinh thành công"));
    }
}
