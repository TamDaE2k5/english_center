package com.englishcenter.admission.controller;

import com.englishcenter.admission.common.dto.ApiResponse;
import com.englishcenter.admission.dto.test.StartTestRequest;
import com.englishcenter.admission.dto.test.SubmitTestRequest;
import com.englishcenter.admission.dto.test.TakeTestResponse;
import com.englishcenter.admission.dto.test.TestResultResponse;
import com.englishcenter.admission.service.PlacementTestService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller xử lý các yêu cầu khảo thí trực tuyến cho học viên (FR-TST-03, FR-TST-04, INV-TST-02).
 * Hỗ trợ làm bài thi công khai, đếm ngược thời gian và tự động chấm điểm tức thì.
 */
@RestController
@RequestMapping("/api/admission/tests")
public class PlacementTestController {

    private final PlacementTestService placementTestService;

    public PlacementTestController(PlacementTestService placementTestService) {
        this.placementTestService = placementTestService;
    }

    /**
     * Bắt đầu lượt làm bài thi khảo thí trực tuyến (Public API).
     * Khởi tạo lượt thi đếm ngược (attempt_id) và trả về đề thi đã ẩn đáp án đúng.
     *
     * @param id ID bộ đề thi
     * @param request Thông tin thí sinh (user_id hoặc email, họ tên)
     * @return DTO TakeTestResponse
     */
    @PostMapping("/public/{id}/start")
    public ResponseEntity<ApiResponse<TakeTestResponse>> startTest(
            @PathVariable("id") Integer id,
            @RequestBody(required = false) StartTestRequest request) {
        TakeTestResponse response = placementTestService.startTest(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Bắt đầu lượt thi thành công"));
    }

    /**
     * Hỗ trợ phương thức GET để lấy đề thi bắt đầu làm bài tiện lợi.
     */
    @GetMapping("/public/{id}/start")
    public ResponseEntity<ApiResponse<TakeTestResponse>> startTestGet(
            @PathVariable("id") Integer id,
            @RequestParam(name = "email", required = false) String email,
            @RequestParam(name = "full_name", required = false) String fullName,
            @RequestParam(name = "phone", required = false) String phone) {
        StartTestRequest request = new StartTestRequest(null, email, fullName, phone);
        TakeTestResponse response = placementTestService.startTest(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy đề thi bắt đầu làm bài thành công"));
    }

    /**
     * Nộp bài thi trắc nghiệm và chấm điểm tức thì (Public API).
     * Tự động kiểm tra thời hạn countdown (INV-TST-02), bóc tách điểm 3 kỹ năng
     * và trả về khóa học phù hợp được đề xuất.
     *
     * @param request Dữ liệu nộp bài thi
     * @return DTO TestResultResponse
     */
    @PostMapping("/public/submit")
    public ResponseEntity<ApiResponse<TestResultResponse>> submitTest(
            @Valid @RequestBody SubmitTestRequest request) {
        TestResultResponse response = placementTestService.submitAndGrade(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Nộp bài và chấm điểm thành công"));
    }

    /**
     * Tra cứu chi tiết kết quả bài thi theo ID bài nộp (Đảm bảo cách ly dữ liệu cá nhân INV-SEC-01).
     *
     * @param id ID kết quả bài thi (submission_id)
     * @return Chi tiết bài thi và điểm bóc tách từng kỹ năng
     */
    @GetMapping("/submissions/{id}")
    public ResponseEntity<ApiResponse<TestResultResponse>> getSubmission(@PathVariable("id") Integer id) {
        TestResultResponse response = placementTestService.getSubmissionById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy chi tiết kết quả bài thi thành công"));
    }
}
