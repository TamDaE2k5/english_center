package com.englishcenter.admission.repository;

import com.englishcenter.admission.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Kho truy vấn dữ liệu phòng học (bảng admission.rooms).
 */
@Repository
public interface RoomRepository extends JpaRepository<Room, Integer> {

    /**
     * Tìm phòng học theo mã phòng.
     *
     * @param code Mã phòng duy nhất
     * @return Optional chứa thực thể Room nếu tìm thấy
     */
    Optional<Room> findByCode(String code);

    /**
     * Kiểm tra xem mã phòng học đã tồn tại chưa.
     *
     * @param code Mã phòng cần kiểm tra
     * @return true nếu đã tồn tại
     */
    boolean existsByCode(String code);

    /**
     * Tìm kiếm và lọc danh sách phòng học theo các tiêu chí tùy chọn.
     *
     * @param branchName Tên cơ sở (tìm kiếm gần đúng, có thể null)
     * @param isActive Trạng thái hoạt động (có thể null)
     * @param minCapacity Sức chứa tối thiểu (có thể null)
     * @return Danh sách phòng học thỏa mãn điều kiện
     */
    @Query("SELECT r FROM Room r WHERE " +
           "(:branchName IS NULL OR LOWER(r.branchName) LIKE LOWER(CONCAT('%', :branchName, '%'))) AND " +
           "(:isActive IS NULL OR r.isActive = :isActive) AND " +
           "(:minCapacity IS NULL OR r.capacity >= :minCapacity) " +
           "ORDER BY r.code ASC")
    List<Room> searchRooms(@Param("branchName") String branchName,
                           @Param("isActive") Boolean isActive,
                           @Param("minCapacity") Integer minCapacity);
}
