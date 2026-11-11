package com.englishcenter.admission.client;

import com.englishcenter.admission.client.dto.InvoiceData;
import com.englishcenter.admission.client.dto.StudentProfileData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@DisplayName("Kiểm thử giao tiếp liên service FinanceServiceClient & StudentServiceClient")
class InterServiceClientTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private FinanceServiceClient financeClient;
    private StudentServiceClient studentClient;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        RestClient restClient = restClientBuilder.build();

        financeClient = new FinanceServiceClient(restClient);
        ReflectionTestUtils.setField(financeClient, "financeServiceUrl", "http://finance-service:8084");
        ReflectionTestUtils.setField(financeClient, "internalSecretKey", "test_internal_secret_key");

        studentClient = new StudentServiceClient(restClient);
        ReflectionTestUtils.setField(studentClient, "studentServiceUrl", "http://student-service:8082");
        ReflectionTestUtils.setField(studentClient, "internalSecretKey", "test_internal_secret_key");
    }

    @Test
    @DisplayName("Sinh hóa đơn học phí tự động thành công qua FinanceServiceClient")
    void testGenerateInvoiceSuccess() {
        String responseJson = """
            {
                "success": true,
                "data": {
                    "invoice_id": 55,
                    "invoice_code": "INV-202611-0001",
                    "status": "UNPAID",
                    "amount": 5000000.00
                },
                "message": "Sinh hóa đơn học phí tự động thành công"
            }
            """;

        mockServer.expect(requestTo("http://finance-service:8084/api/finance/invoices/auto-generate"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("X-Internal-Secret", "test_internal_secret_key"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        InvoiceData data = financeClient.generateInvoice(100, 42, 2, "DISCOUNT10");

        assertNotNull(data);
        assertEquals(55, data.getInvoiceId());
        assertEquals("INV-202611-0001", data.getInvoiceCode());
        mockServer.verify();
    }

    @Test
    @DisplayName("Đồng bộ hồ sơ học viên thành công qua StudentServiceClient")
    void testSyncStudentProfileSuccess() {
        String responseJson = """
            {
                "success": true,
                "data": {
                    "student_id": 1,
                    "student_code": "STU-202611-0001",
                    "status": "STUDYING"
                },
                "message": "Đã tạo hồ sơ học viên thành công"
            }
            """;

        mockServer.expect(requestTo("http://student-service:8082/api/student/internal/students"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("X-Internal-Secret", "test_internal_secret_key"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        StudentProfileData profile = studentClient.syncStudentProfile(
                42, "Tran Thi Mai", "mai.tran@gmail.com", "0988776655", LocalDate.of(2000, 5, 15)
        );

        assertNotNull(profile);
        assertEquals(1, profile.getStudentId());
        assertEquals("STU-202611-0001", profile.getStudentCode());
        mockServer.verify();
    }

    @Test
    @DisplayName("Đồng bộ hồ sơ học viên an toàn (trySyncStudentProfile) trả về rỗng khi service lân cận lỗi")
    void testTrySyncStudentProfileFallbackOnError() {
        mockServer.expect(requestTo("http://student-service:8082/api/student/internal/students"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withServerError());

        Optional<StudentProfileData> result = studentClient.trySyncStudentProfile(
                42, "Tran Thi Mai", "mai.tran@gmail.com", "0988776655", LocalDate.of(2000, 5, 15)
        );

        assertTrue(result.isEmpty());
        mockServer.verify();
    }
}
