package com.englishcenter.admission.controller;

import com.englishcenter.admission.common.dto.ApiResponse;
import com.englishcenter.admission.dto.test.CreateRuleRequest;
import com.englishcenter.admission.dto.test.RecommendationRuleResponse;
import com.englishcenter.admission.service.RecommendationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller cấu hình các quy tắc đề xuất khóa học theo điểm khảo thí (FR-TST-05, TASK-ADM-32).
 * Phân quyền: STAFF và ADMIN.
 */
@RestController
@RequestMapping("/api/admission/tests/rules")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class RecommendationRuleController {

    private final RecommendationService recommendationService;

    public RecommendationRuleController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    /**
     * Lấy danh sách quy tắc đề xuất khóa học của bộ đề.
     *
     * @param testSetId ID bộ đề thi (tùy chọn)
     * @return Danh sách quy tắc đã sắp xếp theo dải điểm
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<RecommendationRuleResponse>>> getRules(
            @RequestParam(name = "test_set_id", required = false) Integer testSetId) {
        List<RecommendationRuleResponse> rules = recommendationService.getRules(testSetId);
        return ResponseEntity.ok(ApiResponse.success(rules, "Lấy danh sách quy tắc đề xuất thành công"));
    }

    /**
     * Tạo mới quy tắc đề xuất khóa học theo dải điểm.
     *
     * @param request Dữ liệu quy tắc
     * @return Thông tin quy tắc vừa tạo với HTTP 201 Created
     */
    @PostMapping
    public ResponseEntity<ApiResponse<RecommendationRuleResponse>> createRule(
            @Valid @RequestBody CreateRuleRequest request) {
        RecommendationRuleResponse response = recommendationService.createRule(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tạo quy tắc đề xuất thành công"));
    }

    /**
     * Xóa một quy tắc đề xuất.
     *
     * @param id ID quy tắc
     * @return Phản hồi thông báo xóa thành công
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable("id") Integer id) {
        recommendationService.deleteRule(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa quy tắc đề xuất thành công"));
    }
}
