package com.englishcenter.admission.controller;

import com.englishcenter.admission.common.dto.ApiResponse;
import com.englishcenter.admission.dto.test.*;
import com.englishcenter.admission.service.TestManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller quản lý ngân hàng bộ đề và câu hỏi khảo thí 3 kỹ năng (FR-TST-01, FR-TST-02).
 * Phân quyền: Dành riêng cho Nhân viên học vụ (STAFF) và Quản trị viên (ADMIN).
 */
@RestController
@RequestMapping("/api/admission/tests")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class TestManagementController {

    private final TestManagementService testManagementService;

    public TestManagementController(TestManagementService testManagementService) {
        this.testManagementService = testManagementService;
    }

    /**
     * Lấy danh sách các bộ đề thi khảo thí trong hệ thống.
     *
     * @param isActive Bộ lọc trạng thái kích hoạt (tùy chọn)
     * @return Danh sách bộ đề kèm số lượng câu hỏi
     */
    @GetMapping("/sets")
    public ResponseEntity<ApiResponse<List<TestSetResponse>>> listTestSets(
            @RequestParam(name = "is_active", required = false) Boolean isActive) {
        List<TestSetResponse> sets = testManagementService.listTestSets(isActive);
        return ResponseEntity.ok(ApiResponse.success(sets, "Lấy danh sách bộ đề thi thành công"));
    }

    /**
     * Tạo mới một bộ đề thi khảo thí đầu vào.
     *
     * @param request Dữ liệu bộ đề
     * @return Thông tin bộ đề vừa tạo với HTTP 201 Created
     */
    @PostMapping("/sets")
    public ResponseEntity<ApiResponse<TestSetResponse>> createTestSet(
            @Valid @RequestBody CreateTestSetRequest request) {
        TestSetResponse response = testManagementService.createTestSet(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tạo bộ đề thi thành công"));
    }

    /**
     * Xem chi tiết thông tin bộ đề thi và toàn bộ câu hỏi gốc (kèm đáp án đúng).
     *
     * @param id ID bộ đề thi
     * @return Chi tiết bộ đề và danh sách câu hỏi
     */
    @GetMapping("/sets/{id}")
    public ResponseEntity<ApiResponse<TestSetResponse>> getTestSetById(@PathVariable("id") Integer id) {
        TestSetResponse response = testManagementService.getTestSetById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin bộ đề thi thành công"));
    }

    /**
     * Cập nhật thông tin bộ đề thi.
     *
     * @param id ID bộ đề cần cập nhật
     * @param request Dữ liệu cập nhật
     * @return Thông tin bộ đề sau khi cập nhật
     */
    @PutMapping("/sets/{id}")
    public ResponseEntity<ApiResponse<TestSetResponse>> updateTestSet(
            @PathVariable("id") Integer id,
            @Valid @RequestBody UpdateTestSetRequest request) {
        TestSetResponse response = testManagementService.updateTestSet(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật bộ đề thi thành công"));
    }

    /**
     * Thêm mới một câu hỏi trắc nghiệm vào bộ đề thi.
     *
     * @param id ID bộ đề thi
     * @param request Dữ liệu câu hỏi cần thêm
     * @return Thông tin câu hỏi vừa thêm với HTTP 201 Created
     */
    @PostMapping("/sets/{id}/questions")
    public ResponseEntity<ApiResponse<QuestionResponse>> addQuestion(
            @PathVariable("id") Integer id,
            @Valid @RequestBody CreateQuestionRequest request) {
        QuestionResponse response = testManagementService.addQuestion(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Thêm câu hỏi vào bộ đề thành công"));
    }

    /**
     * Cập nhật nội dung câu hỏi trong bộ đề thi.
     *
     * @param id ID câu hỏi cần chỉnh sửa
     * @param request Dữ liệu cập nhật
     * @return Thông tin câu hỏi sau cập nhật
     */
    @PutMapping("/questions/{id}")
    public ResponseEntity<ApiResponse<QuestionResponse>> updateQuestion(
            @PathVariable("id") Integer id,
            @Valid @RequestBody UpdateQuestionRequest request) {
        QuestionResponse response = testManagementService.updateQuestion(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật câu hỏi thành công"));
    }

    /**
     * Xóa một câu hỏi khỏi bộ đề thi.
     *
     * @param id ID câu hỏi cần xóa
     * @return Phản hồi thông báo xóa thành công
     */
    @DeleteMapping("/questions/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(@PathVariable("id") Integer id) {
        testManagementService.deleteQuestion(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa câu hỏi thành công"));
    }
}
