package com.englishcenter.admission.service;

import com.englishcenter.admission.common.exception.BusinessException;
import com.englishcenter.admission.common.exception.ErrorCode;
import com.englishcenter.admission.dto.room.CreateRoomRequest;
import com.englishcenter.admission.dto.room.RoomResponse;
import com.englishcenter.admission.dto.room.UpdateRoomRequest;
import com.englishcenter.admission.entity.Room;
import com.englishcenter.admission.repository.RoomRepository;
import com.englishcenter.admission.service.impl.RoomServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    private RoomServiceImpl roomService;

    @BeforeEach
    void setUp() {
        roomService = new RoomServiceImpl(roomRepository);
    }

    @Test
    @DisplayName("Tạo mới phòng học thành công khi mã phòng chưa tồn tại")
    void createRoom_Success() {
        CreateRoomRequest request = new CreateRoomRequest("ROOM-P101", "Phòng Lab 101", 30, "Cơ sở Cầu Giấy");
        when(roomRepository.existsByCode("ROOM-P101")).thenReturn(false);

        Room saved = new Room();
        saved.setId(1);
        saved.setCode("ROOM-P101");
        saved.setName("Phòng Lab 101");
        saved.setCapacity(30);
        saved.setBranchName("Cơ sở Cầu Giấy");
        saved.setIsActive(true);

        when(roomRepository.save(any(Room.class))).thenReturn(saved);

        RoomResponse response = roomService.createRoom(request);

        assertNotNull(response);
        assertEquals("ROOM-P101", response.getCode());
        assertEquals(30, response.getCapacity());
        assertTrue(response.getIsActive());
        verify(roomRepository, times(1)).save(any(Room.class));
    }

    @Test
    @DisplayName("Tạo phòng học thất bại và ném lỗi 409 ADM_ROOM_CODE_EXISTS khi mã phòng đã trùng")
    void createRoom_DuplicateCode_ThrowsConflict() {
        CreateRoomRequest request = new CreateRoomRequest("ROOM-P101", "Phòng Lab 101", 30, "Cơ sở Cầu Giấy");
        when(roomRepository.existsByCode("ROOM-P101")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> roomService.createRoom(request));

        assertEquals(ErrorCode.ADM_ROOM_CODE_EXISTS, exception.getErrorCode());
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    @DisplayName("Cập nhật phòng học thành công khi ID hợp lệ")
    void updateRoom_Success() {
        Room existing = new Room();
        existing.setId(2);
        existing.setCode("ROOM-P102");
        existing.setName("Phòng Cũ");
        existing.setCapacity(20);
        existing.setBranchName("Cơ sở Cũ");
        existing.setIsActive(true);

        when(roomRepository.findById(2)).thenReturn(Optional.of(existing));
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateRoomRequest updateRequest = new UpdateRoomRequest("Phòng Mới", 25, "Cơ sở Mới", true);
        RoomResponse response = roomService.updateRoom(2, updateRequest);

        assertNotNull(response);
        assertEquals("Phòng Mới", response.getName());
        assertEquals(25, response.getCapacity());
        assertEquals("Cơ sở Mới", response.getBranchName());
    }

    @Test
    @DisplayName("Cập nhật phòng học ném lỗi 404 ADM_ROOM_NOT_FOUND khi không tìm thấy ID")
    void updateRoom_NotFound_ThrowsException() {
        when(roomRepository.findById(999)).thenReturn(Optional.empty());
        UpdateRoomRequest updateRequest = new UpdateRoomRequest("Phòng Mới", 25, "Cơ sở Mới", true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> roomService.updateRoom(999, updateRequest));

        assertEquals(ErrorCode.ADM_ROOM_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("Cập nhật trạng thái phòng học (kích hoạt/vô hiệu hóa) thành công")
    void updateRoomStatus_Success() {
        Room existing = new Room();
        existing.setId(3);
        existing.setIsActive(true);

        when(roomRepository.findById(3)).thenReturn(Optional.of(existing));
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoomResponse response = roomService.updateRoomStatus(3, false);

        assertNotNull(response);
        assertFalse(response.getIsActive());
    }

    @Test
    @DisplayName("Tìm kiếm danh sách phòng học theo tiêu chí lọc")
    void getRooms_Filtered_Success() {
        Room r1 = new Room();
        r1.setId(1);
        r1.setCode("R-01");
        r1.setName("Room 1");
        r1.setCapacity(30);
        r1.setBranchName("Cầu Giấy");
        r1.setIsActive(true);

        when(roomRepository.searchRooms(eq("Cầu Giấy"), eq(true), eq(20))).thenReturn(List.of(r1));

        List<RoomResponse> result = roomService.getRooms("Cầu Giấy", true, 20);

        assertEquals(1, result.size());
        assertEquals("R-01", result.get(0).getCode());
    }
}
