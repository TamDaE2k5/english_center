package com.englishcenter.admission.repository;

import com.englishcenter.admission.entity.TestQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Kho truy vấn dữ liệu câu hỏi trắc nghiệm 3 kỹ năng (bảng admission.test_questions).
 */
@Repository
public interface TestQuestionRepository extends JpaRepository<TestQuestion, Integer> {

    /**
     * Lấy danh sách câu hỏi thuộc một bộ đề sắp xếp theo thứ tự hiển thị.
     *
     * @param testSetId ID bộ đề
     * @return Danh sách câu hỏi
     */
    List<TestQuestion> findByTestSetIdOrderByDisplayOrderAsc(Integer testSetId);

    /**
     * Đếm tổng số câu hỏi trong bộ đề.
     *
     * @param testSetId ID bộ đề
     * @return Số lượng câu hỏi
     */
    long countByTestSetId(Integer testSetId);

    /**
     * Tính tổng trọng số điểm của tất cả câu hỏi trong một bộ đề.
     *
     * @param testSetId ID bộ đề
     * @return Tổng điểm câu hỏi
     */
    @Query("SELECT COALESCE(SUM(q.scoreWeight), 0.0) FROM TestQuestion q WHERE q.testSetId = :testSetId")
    BigDecimal sumScoreWeightByTestSetId(@Param("testSetId") Integer testSetId);
}
