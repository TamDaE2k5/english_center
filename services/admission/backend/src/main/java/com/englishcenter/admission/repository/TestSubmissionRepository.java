package com.englishcenter.admission.repository;

import com.englishcenter.admission.entity.TestSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Kho truy vấn dữ liệu kết quả bài làm khảo thí đầu vào (bảng admission.test_submissions).
 */
@Repository
public interface TestSubmissionRepository extends JpaRepository<TestSubmission, Integer> {

    /**
     * Tìm bài thi nộp theo ID lượt thi (chống chấm điểm lặp lại cùng lượt).
     *
     * @param attemptId ID lượt thi
     * @return Optional chứa TestSubmission
     */
    Optional<TestSubmission> findByAttemptId(Integer attemptId);

    /**
     * Kiểm tra xem lượt thi này đã nộp bài chưa (INV-TST-02).
     *
     * @param attemptId ID lượt thi
     * @return true nếu đã có bài nộp
     */
    boolean existsByAttemptId(Integer attemptId);

    /**
     * Lấy danh sách kết quả bài thi của một học viên theo thời gian nộp giảm dần.
     *
     * @param userId ID học viên
     * @return Danh sách kết quả bài làm
     */
    List<TestSubmission> findByUserIdOrderBySubmittedAtDesc(Integer userId);

    /**
     * Lấy chi tiết bài thi nộp kèm nạp khóa học đề xuất (nếu có).
     *
     * @param id ID bài thi nộp
     * @return Optional chứa TestSubmission
     */
    @Query("SELECT s FROM TestSubmission s LEFT JOIN FETCH s.recommendedCourse WHERE s.id = :id")
    Optional<TestSubmission> findByIdWithDetails(@Param("id") Integer id);
}
