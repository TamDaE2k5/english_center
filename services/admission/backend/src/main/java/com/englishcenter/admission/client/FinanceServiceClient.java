package com.englishcenter.admission.client;

import com.englishcenter.admission.client.dto.CreateInvoiceRequest;
import com.englishcenter.admission.client.dto.InvoiceData;
import com.englishcenter.admission.common.dto.ApiResponse;
import com.englishcenter.admission.common.exception.BusinessException;
import com.englishcenter.admission.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Client giao tiếp nội bộ trực tiếp với Finance Service (Port 8084).
 * Chịu trách nhiệm yêu cầu sinh hóa đơn học phí tự động khi có đơn đăng ký PENDING.
 * Tuân thủ quy tắc không đi vòng qua API Gateway và kèm header bảo mật X-Internal-Secret.
 */
@Service
public class FinanceServiceClient {

    private static final Logger log = LoggerFactory.getLogger(FinanceServiceClient.class);

    private final RestClient restClient;

    @Value("${services.finance.url:http://finance-service:8084}")
    private String financeServiceUrl;

    @Value("${internal.secret-key:english_center_internal_secret_2026}")
    private String internalSecretKey;

    public FinanceServiceClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Gửi yêu cầu sinh hóa đơn học phí tự động sang Finance Service.
     *
     * @param enrollmentId ID đơn đăng ký tuyển sinh
     * @param studentId    ID người dùng học viên
     * @param courseId     ID khóa học đăng ký
     * @param voucherCode  Mã giảm giá (nếu có, có thể null)
     * @return Dữ liệu hóa đơn được sinh (InvoiceData)
     * @throws BusinessException nếu Finance Service phản hồi lỗi hoặc không kết nối được
     */
    public InvoiceData generateInvoice(Integer enrollmentId, Integer studentId, Integer courseId, String voucherCode) {
        String endpoint = financeServiceUrl + "/api/finance/invoices/auto-generate";
        CreateInvoiceRequest request = new CreateInvoiceRequest(enrollmentId, studentId, courseId, voucherCode);

        log.info("Gọi Finance Service tạo hóa đơn cho enrollment_id: {}, url: {}", enrollmentId, endpoint);

        try {
            ApiResponse<InvoiceData> response = restClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Internal-Secret", internalSecretKey)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<InvoiceData>>() {});

            if (response != null && response.isSuccess() && response.getData() != null) {
                log.info("Finance Service sinh hóa đơn thành công: invoice_code={}", response.getData().getInvoiceCode());
                return response.getData();
            } else {
                String errorMsg = (response != null && response.getError() != null)
                        ? response.getError().getMessage()
                        : "Phản hồi không hợp lệ từ Finance Service";
                log.error("Finance Service trả về lỗi nghiệp vụ: {}", errorMsg);
                throw new BusinessException(ErrorCode.ADM_SERVICE_UNAVAILABLE, "Lỗi từ Finance Service: " + errorMsg);
            }
        } catch (RestClientResponseException ex) {
            log.error("Lỗi HTTP khi gọi Finance Service: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.ADM_SERVICE_UNAVAILABLE, "Không thể tạo hóa đơn học phí từ Finance Service: HTTP " + ex.getStatusCode());
        } catch (Exception ex) {
            log.error("Không thể kết nối đến Finance Service tại {}: {}", endpoint, ex.getMessage());
            throw new BusinessException(ErrorCode.ADM_SERVICE_UNAVAILABLE, "Hệ thống tài chính tạm thời không khả dụng, vui lòng thử lại sau");
        }
    }
}
