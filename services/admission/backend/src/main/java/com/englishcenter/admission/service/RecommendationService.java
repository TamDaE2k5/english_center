package com.englishcenter.admission.service;

import com.englishcenter.admission.dto.test.CreateRuleRequest;
import com.englishcenter.admission.dto.test.RecommendationResultDTO;
import com.englishcenter.admission.dto.test.RecommendationRuleResponse;

import java.math.BigDecimal;
import java.util.List;

/**
 * Giao diện động cơ đề xuất khóa học theo điểm khảo thí đầu vào (FR-TST-05, TASK-ADM-32).
 */
public interface RecommendationService {

    /**
     * Tạo mới quy tắc đề xuất khóa học theo dải điểm.
     * Bắt buộc kiểm tra chồng lấn dải điểm (ADM_RULE_OVERLAP).
     *
     * @param request Dữ liệu quy tắc
     * @return Thông tin quy tắc vừa tạo
     */
    RecommendationRuleResponse createRule(CreateRuleRequest request);

    /**
     * Lấy danh sách quy tắc đề xuất khóa học của bộ đề.
     *
     * @param testSetId ID bộ đề (tùy chọn)
     * @return Danh sách quy tắc
     */
    List<RecommendationRuleResponse> getRules(Integer testSetId);

    /**
     * Xóa một quy tắc đề xuất.
     *
     * @param id ID quy tắc cần xóa
     */
    void deleteRule(Integer id);

    /**
     * Khớp nối điểm thi và đề xuất khóa học kèm danh sách lớp học phù hợp.
     *
     * @param testSetId ID bộ đề thi
     * @param totalScore Tổng điểm bài thi
     * @return DTO RecommendationResultDTO chứa khóa học đề xuất và các lớp sắp khai giảng
     */
    RecommendationResultDTO getRecommendation(Integer testSetId, BigDecimal totalScore);
}
