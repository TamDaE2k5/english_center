package com.englishcenter.admission.service.impl;

import com.englishcenter.admission.common.exception.BusinessException;
import com.englishcenter.admission.common.exception.ErrorCode;
import com.englishcenter.admission.dto.room.CreateRoomRequest;
import com.englishcenter.admission.dto.room.RoomResponse;
import com.englishcenter.admission.dto.room.UpdateRoomRequest;
import com.englishcenter.admission.entity.Room;
import com.englishcenter.admission.repository.RoomRepository;
import com.englishcenter.admission.service.RoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Hiện thực nghiệp vụ quản lý phòng học và cơ sở vật chất (FR-FAC-01, FR-FAC-02).
 */
@Service
public class RoomServiceImpl implements RoomService {

    private static final Logger log = LoggerFactory.getLogger(RoomServiceImpl.class);

    private final RoomRepository roomRepository;

    public RoomServiceImpl(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Override
    @Transactional
    public RoomResponse createRoom(CreateRoomRequest request) {
        // 1. Kiểm tra tính duy nhất của mã phòng học
        if (roomRepository.existsByCode(request.getCode())) {
            log.warn("Thêm phòng học thất bại: mã phòng {} đã tồn tại", request.getCode());
            throw new BusinessException(ErrorCode.ADM_ROOM_CODE_EXISTS,
                    "Mã phòng học " + request.getCode() + " đã tồn tại trong hệ thống");
        }

        // 2. Khởi tạo đối tượng Room
        Room room = new Room();
        room.setCode(request.getCode().trim().toUpperCase());
        room.setName(request.getName().trim());
        room.setCapacity(request.getCapacity());
        room.setBranchName(request.getBranchName() != null ? request.getBranchName().trim() : "Cơ sở chính");
        room.setIsActive(true);

        Room saved = roomRepository.save(room);
        log.info("Đã tạo mới phòng học thành công: ID={}, code={}, capacity={}",
                saved.getId(), saved.getCode(), saved.getCapacity());

        return RoomResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public RoomResponse updateRoom(Integer id, UpdateRoomRequest request) {
        // Cùng khóa với create/update lớp: không cho capacity đổi giữa lúc lớp kiểm tra và commit.
        roomRepository.lockScheduling();
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_ROOM_NOT_FOUND,
                        "Không tìm thấy phòng học với ID: " + id));

        int assignedMax = roomRepository.maxAssignedClassSize(id);
        if (request.getCapacity() < assignedMax) {
            throw new BusinessException(ErrorCode.ADM_ROOM_CAPACITY_EXCEEDED,
                    "Không thể giảm sức chứa phòng dưới sĩ số trần của lớp đang gán (" + assignedMax + ")");
        }

        room.setName(request.getName().trim());
        room.setCapacity(request.getCapacity());
        if (request.getBranchName() != null) {
            room.setBranchName(request.getBranchName().trim());
        }
        if (request.getIsActive() != null) {
            room.setIsActive(request.getIsActive());
        }

        Room updated = roomRepository.save(room);
        log.info("Cập nhật thông tin phòng học thành công: ID={}, code={}", updated.getId(), updated.getCode());

        return RoomResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public RoomResponse updateRoomStatus(Integer id, Boolean isActive) {
        // Hibernate có thể ghi cả capacity: phải đọc entity sau cùng khóa để không phục hồi capacity cũ.
        roomRepository.lockScheduling();
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_ROOM_NOT_FOUND,
                        "Không tìm thấy phòng học với ID: " + id));

        room.setIsActive(isActive);
        Room updated = roomRepository.save(room);
        log.info("Đã cập nhật trạng thái phòng học ID={}, is_active={}", id, isActive);

        return RoomResponse.fromEntity(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponse getRoomById(Integer id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_ROOM_NOT_FOUND,
                        "Không tìm thấy phòng học với ID: " + id));

        return RoomResponse.fromEntity(room);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getRooms(String branchName, Boolean isActive, Integer minCapacity) {
        return roomRepository.searchRooms(branchName, isActive, minCapacity)
                .stream()
                .map(RoomResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
