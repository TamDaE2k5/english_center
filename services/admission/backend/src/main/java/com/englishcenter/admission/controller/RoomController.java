package com.englishcenter.admission.controller;

import com.englishcenter.admission.common.dto.ApiResponse;
import com.englishcenter.admission.dto.room.CreateRoomRequest;
import com.englishcenter.admission.dto.room.RoomResponse;
import com.englishcenter.admission.dto.room.UpdateRoomRequest;
import com.englishcenter.admission.dto.room.UpdateRoomStatusRequest;
import com.englishcenter.admission.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller xử lý các yêu cầu RESTful quản lý phòng học và cơ sở vật chất (FR-FAC-01, FR-FAC-02).
 */
@RestController
@RequestMapping("/api/admission/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    /**
     * Thêm mới phòng học vào danh mục cơ sở vật chất.
     *
     * @param request Dữ liệu phòng học cần tạo
     * @return Thông tin phòng học vừa tạo với HTTP 201 Created
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        RoomResponse response = roomService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tạo phòng học mới thành công"));
    }

    /**
     * Lấy danh sách phòng học theo các tiêu chí tìm kiếm.
     *
     * @param branchName Tên cơ sở/chi nhánh (tùy chọn)
     * @param isActive Trạng thái hoạt động (tùy chọn)
     * @param minCapacity Sức chứa tối thiểu (tùy chọn)
     * @return Danh sách phòng học phù hợp
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getRooms(
            @RequestParam(name = "branch_name", required = false) String branchName,
            @RequestParam(name = "is_active", required = false) Boolean isActive,
            @RequestParam(name = "min_capacity", required = false) Integer minCapacity) {
        List<RoomResponse> rooms = roomService.getRooms(branchName, isActive, minCapacity);
        return ResponseEntity.ok(ApiResponse.success(rooms, "Lấy danh sách phòng học thành công"));
    }

    /**
     * Lấy thông tin chi tiết một phòng học theo ID.
     *
     * @param id ID của phòng học
     * @return Chi tiết phòng học
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoomById(@PathVariable("id") Integer id) {
        RoomResponse response = roomService.getRoomById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin phòng học thành công"));
    }

    /**
     * Cập nhật thông tin chi tiết của phòng học.
     *
     * @param id ID của phòng học cần cập nhật
     * @param request Dữ liệu cập nhật
     * @return Thông tin phòng học sau khi cập nhật
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoom(
            @PathVariable("id") Integer id,
            @Valid @RequestBody UpdateRoomRequest request) {
        RoomResponse response = roomService.updateRoom(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật phòng học thành công"));
    }

    /**
     * Kích hoạt hoặc tạm ngưng hoạt động của phòng học.
     *
     * @param id ID của phòng học
     * @param request Yêu cầu đổi trạng thái
     * @return Thông tin phòng học sau khi đổi trạng thái
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoomStatus(
            @PathVariable("id") Integer id,
            @Valid @RequestBody UpdateRoomStatusRequest request) {
        RoomResponse response = roomService.updateRoomStatus(id, request.getIsActive());
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật trạng thái phòng học thành công"));
    }
}
