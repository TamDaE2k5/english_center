package com.englishcenter.admission.service;

import com.englishcenter.admission.dto.test.*;

import java.util.List;

/**
 * Giao diện quản lý ngân hàng bộ đề và câu hỏi khảo thí 3 kỹ năng (FR-TST-01, FR-TST-02).
 */
public interface TestManagementService {

    /**
     * Tạo mới một bộ đề thi khảo thí đầu vào.
     *
     * @param request Dữ liệu bộ đề
     * @return Thông tin bộ đề vừa tạo
     */
    TestSetResponse createTestSet(CreateTestSetRequest request);

    /**
     * Cập nhật thông tin bộ đề thi.
     *
     * @param id ID bộ đề
     * @param request Dữ liệu cập nhật
     * @return Thông tin bộ đề sau cập nhật
     */
    TestSetResponse updateTestSet(Integer id, UpdateTestSetRequest request);

    /**
     * Lấy chi tiết bộ đề thi kèm toàn bộ câu hỏi gốc (dành cho Staff/Admin).
     *
     * @param id ID bộ đề
     * @return Chi tiết bộ đề và danh sách câu hỏi
     */
    TestSetResponse getTestSetById(Integer id);

    /**
     * Lấy danh sách các bộ đề thi khảo thí trong hệ thống.
     *
     * @param isActive Trạng thái kích hoạt (tùy chọn)
     * @return Danh sách bộ đề
     */
    List<TestSetResponse> listTestSets(Boolean isActive);

    /**
     * Thêm mới câu hỏi trắc nghiệm vào bộ đề thi.
     * Tự động kiểm tra tính hợp lệ của âm thanh/đoạn văn và cộng dồn total_max_score.
     *
     * @param testSetId ID bộ đề
     * @param request Dữ liệu câu hỏi
     * @return Thông tin câu hỏi vừa tạo
     */
    QuestionResponse addQuestion(Integer testSetId, CreateQuestionRequest request);

    /**
     * Cập nhật nội dung câu hỏi trong bộ đề.
     *
     * @param questionId ID câu hỏi
     * @param request Dữ liệu cập nhật
     * @return Thông tin câu hỏi sau cập nhật
     */
    QuestionResponse updateQuestion(Integer questionId, UpdateQuestionRequest request);

    /**
     * Xóa một câu hỏi khỏi bộ đề thi.
     * Tự động trừ điểm tương ứng khỏi total_max_score.
     *
     * @param questionId ID câu hỏi cần xóa
     */
    void deleteQuestion(Integer questionId);
}
