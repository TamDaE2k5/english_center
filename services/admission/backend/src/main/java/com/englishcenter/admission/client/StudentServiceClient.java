package com.englishcenter.admission.client;

import com.englishcenter.admission.client.dto.CreateStudentProfileRequest;
import com.englishcenter.admission.client.dto.StudentProfileData;
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

import java.time.LocalDate;
import java.util.Optional;

/**
 * Client giao tiếp nội bộ trực tiếp với Student Service (Port 8082).
 * Chịu trách nhiệm đồng bộ hồ sơ học viên chính thức khi đơn tuyển sinh được duyệt (APPROVED).
 * Tuân thủ quy tắc không đi vòng qua API Gateway và kèm header bảo mật X-Internal-Secret.
 */
@Service
public class StudentServiceClient {

    private static final Logger log = LoggerFactory.getLogger(StudentServiceClient.class);

    private final RestClient restClient;

    @Value("${services.student.url:http://student-service:8082}")
    private String studentServiceUrl;

    @Value("${internal.secret-key:english_center_internal_secret_2026}")
    private String internalSecretKey;

    public StudentServiceClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Gửi yêu cầu khởi tạo hoặc đồng bộ hồ sơ học viên sang Student Service.
     *
     * @param userId      ID người dùng tài khoản
     * @param fullName    Họ và tên học viên
     * @param email       Email học viên
     * @param phone       Số điện thoại liên lạc
     * @param dateOfBirth Ngày tháng năm sinh (có thể null)
     * @return Dữ liệu hồ sơ học viên được tạo (StudentProfileData)
     * @throws BusinessException nếu Student Service phản hồi lỗi hoặc không kết nối được
     */
    public StudentProfileData syncStudentProfile(Integer userId, String fullName, String email, String phone, LocalDate dateOfBirth) {
        String endpoint = studentServiceUrl + "/api/student/internal/students";
        CreateStudentProfileRequest request = new CreateStudentProfileRequest(userId, fullName, email, phone, dateOfBirth);

        log.info("Gọi Student Service tạo hồ sơ cho user_id: {}, url: {}", userId, endpoint);

        try {
            ApiResponse<StudentProfileData> response = restClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Internal-Secret", internalSecretKey)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<StudentProfileData>>() {});

            if (response != null && response.isSuccess() && response.getData() != null) {
                log.info("Student Service tạo hồ sơ thành công: student_code={}", response.getData().getStudentCode());
                return response.getData();
            } else {
                String errorMsg = (response != null && response.getError() != null)
                        ? response.getError().getMessage()
                        : "Phản hồi không hợp lệ từ Student Service";
                log.error("Student Service trả về lỗi nghiệp vụ: {}", errorMsg);
                throw new BusinessException(ErrorCode.ADM_SERVICE_UNAVAILABLE, "Lỗi từ Student Service: " + errorMsg);
            }
        } catch (RestClientResponseException ex) {
            log.error("Lỗi HTTP khi gọi Student Service: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.ADM_SERVICE_UNAVAILABLE, "Không thể tạo hồ sơ học viên từ Student Service: HTTP " + ex.getStatusCode());
        } catch (Exception ex) {
            log.error("Không thể kết nối đến Student Service tại {}: {}", endpoint, ex.getMessage());
            throw new BusinessException(ErrorCode.ADM_SERVICE_UNAVAILABLE, "Hệ thống quản lý học viên tạm thời không khả dụng, vui lòng thử lại sau");
        }
    }

    /**
     * Thực hiện đồng bộ an toàn (Safe Sync), không ném ngoại lệ nếu Student Service gián đoạn.
     * Giúp tiến trình duyệt đơn tuyển sinh vẫn hoàn tất và đánh dấu trạng thái đồng bộ để bù trừ sau.
     *
     * @param userId      ID người dùng tài khoản
     * @param fullName    Họ và tên học viên
     * @param email       Email học viên
     * @param phone       Số điện thoại
     * @param dateOfBirth Ngày sinh
     * @return Optional chứa dữ liệu hồ sơ nếu thành công, hoặc Optional.empty() nếu thất bại
     */
    public Optional<StudentProfileData> trySyncStudentProfile(Integer userId, String fullName, String email, String phone, LocalDate dateOfBirth) {
        try {
            StudentProfileData data = syncStudentProfile(userId, fullName, email, phone, dateOfBirth);
            return Optional.of(data);
        } catch (Exception ex) {
            log.warn("Đồng bộ hồ sơ học viên thất bại (sẽ được thử lại sau): user_id={}, lỗi={}", userId, ex.getMessage());
            return Optional.empty();
        }
    }
}
