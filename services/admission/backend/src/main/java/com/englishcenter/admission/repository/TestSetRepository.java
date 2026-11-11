package com.englishcenter.admission.repository;

import com.englishcenter.admission.entity.TestSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Kho truy vấn dữ liệu bộ đề thi khảo thí đầu vào (bảng admission.test_sets).
 */
@Repository
public interface TestSetRepository extends JpaRepository<TestSet, Integer> {

    /**
     * Tìm bộ đề theo mã duy nhất.
     *
     * @param code Mã bộ đề
     * @return Optional chứa TestSet
     */
    Optional<TestSet> findByCode(String code);

    /**
     * Kiểm tra sự tồn tại của mã bộ đề.
     *
     * @param code Mã cần kiểm tra
     * @return true nếu đã tồn tại
     */
    boolean existsByCode(String code);

    /**
     * Tìm danh sách bộ đề theo trạng thái kích hoạt (hoặc lấy tất cả nếu isActive null).
     *
     * @param isActive Trạng thái kích hoạt
     * @return Danh sách bộ đề
     */
    @Query("SELECT ts FROM TestSet ts WHERE (:isActive IS NULL OR ts.isActive = :isActive) ORDER BY ts.createdAt DESC")
    List<TestSet> findTestSets(@Param("isActive") Boolean isActive);
}
