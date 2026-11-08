package com.englishcenter.admission.repository;

import com.englishcenter.admission.entity.TestRecommendationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Kho truy vấn dữ liệu quy tắc đề xuất khóa học theo điểm khảo thí (bảng admission.test_recommendation_rules).
 */
@Repository
public interface TestRecommendationRuleRepository extends JpaRepository<TestRecommendationRule, Integer> {

    /**
     * Tìm quy tắc đề xuất phù hợp nhất với điểm số đạt được của bài thi.
     *
     * @param testSetId ID bộ đề thi
     * @param score Tổng điểm bài thi
     * @return Optional chứa TestRecommendationRule
     */
    @Query("SELECT r FROM TestRecommendationRule r LEFT JOIN FETCH r.recommendedCourse WHERE " +
           "r.testSetId = :testSetId AND :score >= r.minScore AND :score <= r.maxScore")
    Optional<TestRecommendationRule> findMatchingRule(@Param("testSetId") Integer testSetId,
                                                      @Param("score") BigDecimal score);

    /**
     * Lấy toàn bộ danh sách quy tắc của một bộ đề sắp xếp theo điểm tăng dần.
     *
     * @param testSetId ID bộ đề thi
     * @return Danh sách quy tắc
     */
    @Query("SELECT r FROM TestRecommendationRule r LEFT JOIN FETCH r.recommendedCourse WHERE " +
           "(:testSetId IS NULL OR r.testSetId = :testSetId) ORDER BY r.minScore ASC")
    List<TestRecommendationRule> findByTestSetIdOrderByMinScoreAsc(@Param("testSetId") Integer testSetId);

    /**
     * Kiểm tra chồng lấn dải điểm [minScore, maxScore] với các quy tắc đã có của cùng bộ đề.
     *
     * @param testSetId ID bộ đề
     * @param excludeRuleId ID quy tắc cần loại trừ (khi cập nhật)
     * @param minScore Điểm cận dưới
     * @param maxScore Điểm cận trên
     * @return Danh sách quy tắc bị chồng lấn
     */
    @Query("SELECT r FROM TestRecommendationRule r WHERE r.testSetId = :testSetId " +
           "AND (:excludeRuleId IS NULL OR r.id <> :excludeRuleId) " +
           "AND r.minScore < :maxScore AND r.maxScore > :minScore")
    List<TestRecommendationRule> findOverlappingRules(@Param("testSetId") Integer testSetId,
                                                      @Param("excludeRuleId") Integer excludeRuleId,
                                                      @Param("minScore") BigDecimal minScore,
                                                      @Param("maxScore") BigDecimal maxScore);
}
