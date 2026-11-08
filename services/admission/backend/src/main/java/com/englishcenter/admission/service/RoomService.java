package com.englishcenter.admission.service;

import com.englishcenter.admission.dto.room.CreateRoomRequest;
import com.englishcenter.admission.dto.room.RoomResponse;
import com.englishcenter.admission.dto.room.UpdateRoomRequest;

import java.util.List;

/**
 * Giao diện nghiệp vụ quản lý phòng học và cơ sở vật chất (FR-FAC-01, FR-FAC-02).
 */
public interface RoomService {

    /**
     * Tạo mới một phòng học vào hệ thống.
     *
     * @param request Thông tin phòng học cần tạo
     * @return Thông tin phòng học đã tạo
     */
    RoomResponse createRoom(CreateRoomRequest request);

    /**
     * Cập nhật thông tin chi tiết phòng học.
     *
     * @param id ID của phòng học
     * @param request Dữ liệu cập nhật
     * @return Thông tin phòng học sau cập nhật
     */
    RoomResponse updateRoom(Integer id, UpdateRoomRequest request);

    /**
     * Cập nhật trạng thái kích hoạt/vô hiệu hóa phòng học.
     *
     * @param id ID của phòng học
     * @param isActive Trạng thái hoạt động mới
     * @return Thông tin phòng học sau cập nhật
     */
    RoomResponse updateRoomStatus(Integer id, Boolean isActive);

    /**
     * Lấy thông tin chi tiết của một phòng học theo ID.
     *
     * @param id ID của phòng học
     * @return Thông tin phòng học
     */
    RoomResponse getRoomById(Integer id);

    /**
     * Lấy danh sách phòng học theo các bộ lọc tiêu chí.
     *
     * @param branchName Chi nhánh cần lọc (có thể null)
     * @param isActive Trạng thái hoạt động (có thể null)
     * @param minCapacity Sức chứa tối thiểu (có thể null)
     * @return Danh sách phòng học
     */
    List<RoomResponse> getRooms(String branchName, Boolean isActive, Integer minCapacity);
}
