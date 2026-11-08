package com.englishcenter.admission.service;

import com.englishcenter.admission.dto.test.StartTestRequest;
import com.englishcenter.admission.dto.test.SubmitTestRequest;
import com.englishcenter.admission.dto.test.TakeTestResponse;
import com.englishcenter.admission.dto.test.TestResultResponse;

/**
 * Giao diện động cơ khảo thí trực tuyến, theo dõi lượt thi countdown và tự động chấm điểm (FR-TST-03, FR-TST-04, INV-TST-02).
 */
public interface PlacementTestService {

    /**
     * Bắt đầu một lượt làm bài khảo thí trực tuyến.
     * Khởi tạo bản ghi test_attempts với thời gian đếm ngược phía server (INV-TST-02)
     * và trả về danh sách câu hỏi ĐÃ ẨN ĐÁP ÁN ĐÚNG.
     *
     * @param testSetId ID bộ đề thi
     * @param request Thông tin thí sinh (user_id hoặc email)
     * @return DTO TakeTestResponse
     */
    TakeTestResponse startTest(Integer testSetId, StartTestRequest request);

    /**
     * Nộp bài thi, đối chiếu thời hạn lượt thi, tự động chấm điểm 3 kỹ năng
     * và tích hợp động cơ đề xuất khóa học phù hợp.
     *
     * @param request Dữ liệu bài thi nộp kèm câu trả lời
     * @return DTO TestResultResponse chứa điểm số và khóa học gợi ý
     */
    TestResultResponse submitAndGrade(SubmitTestRequest request);

    /**
     * Tra cứu chi tiết kết quả bài thi theo ID bản ghi nộp bài (INV-SEC-01).
     *
     * @param submissionId ID bài nộp
     * @return Chi tiết bài thi và điểm bóc tách 3 kỹ năng
     */
    TestResultResponse getSubmissionById(Integer submissionId);
}
