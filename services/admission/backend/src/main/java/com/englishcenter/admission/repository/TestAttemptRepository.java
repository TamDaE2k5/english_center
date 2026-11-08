package com.englishcenter.admission.repository;

import com.englishcenter.admission.entity.TestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Kho truy vấn dữ liệu lượt làm bài khảo thí trực tuyến (bảng admission.test_attempts).
 */
@Repository
public interface TestAttemptRepository extends JpaRepository<TestAttempt, Integer> {

    /**
     * Tìm lượt thi theo ID và ID người dùng (kiểm soát bảo mật).
     *
     * @param id ID lượt thi
     * @param userId ID người dùng
     * @return Optional chứa TestAttempt
     */
    Optional<TestAttempt> findByIdAndUserId(Integer id, Integer userId);

    /**
     * Lấy danh sách lượt thi của người dùng theo thời gian bắt đầu giảm dần.
     *
     * @param userId ID người dùng
     * @return Danh sách lượt thi
     */
    List<TestAttempt> findByUserIdOrderByStartedAtDesc(Integer userId);
}
