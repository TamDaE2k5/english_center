package com.englishcenter.admission.service;

import com.englishcenter.admission.common.exception.BusinessException;
import com.englishcenter.admission.common.exception.ErrorCode;
import com.englishcenter.admission.dto.clazz.ClassCapacityResponse;
import com.englishcenter.admission.dto.clazz.ClassResponse;
import com.englishcenter.admission.dto.clazz.CreateClassRequest;
import com.englishcenter.admission.dto.clazz.UpdateClassStatusRequest;
import com.englishcenter.admission.engine.ConflictEngine;
import com.englishcenter.admission.entity.ClassEntity;
import com.englishcenter.admission.entity.Course;
import com.englishcenter.admission.entity.Room;
import com.englishcenter.admission.entity.User;
import com.englishcenter.admission.repository.*;
import com.englishcenter.admission.service.impl.ClassServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassServiceTest {

    @Mock
    private ClassRepository classRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private ConflictEngine conflictEngine;

    private ClassServiceImpl classService;

    @BeforeEach
    void setUp() {
        classService = new ClassServiceImpl(
                classRepository,
                courseRepository,
                roomRepository,
                userRepository,
                enrollmentRepository,
                conflictEngine
        );
    }

    @Test
    @DisplayName("Mở lớp học thành công khi sĩ số <= sức chứa phòng và không có xung đột lịch")
    void createClass_Success() {
        CreateClassRequest request = new CreateClassRequest(
                "CLS-202611-001", "Lớp IELTS 6.5 Sáng", 1, 2, 5,
                "MON_WED_FRI", LocalTime.of(8, 0), LocalTime.of(9, 30),
                LocalDate.of(2026, 11, 15), LocalDate.of(2027, 2, 15),
                10, 20
        );

        Course course = new Course();
        course.setId(1);
        course.setName("IELTS 6.5");

        Room room = new Room();
        room.setId(2);
        room.setName("Phòng 201");
        room.setCapacity(25); // Sức chứa 25 >= max_students (20) -> Hợp lệ

        User teacher = new User();
        teacher.setId(5);
        teacher.setFullName("Thầy Nam");
        teacher.setRole("TEACHER");

        when(courseRepository.findById(1)).thenReturn(Optional.of(course));
        when(roomRepository.findById(2)).thenReturn(Optional.of(room));
        when(userRepository.findById(5)).thenReturn(Optional.of(teacher));
        when(classRepository.existsByCode("CLS-202611-001")).thenReturn(false);

        ClassEntity saved = new ClassEntity();
        saved.setId(100);
        saved.setCode("CLS-202611-001");
        saved.setName("Lớp IELTS 6.5 Sáng");
        saved.setMaxStudents(20);
        saved.setStatus("OPEN");
        saved.setCourse(course);
        saved.setRoomEntity(room);
        saved.setTeacher(teacher);

        when(classRepository.save(any(ClassEntity.class))).thenReturn(saved);
        when(enrollmentRepository.countByClassIdAndStatus(eq(100), eq("APPROVED"))).thenReturn(0L);
        when(enrollmentRepository.countOccupiedSlots(eq(100), any())).thenReturn(0L);

        ClassResponse response = classService.createClass(request);

        assertNotNull(response);
        assertEquals("CLS-202611-001", response.getCode());
        assertEquals("OPEN", response.getStatus());
        assertEquals(20, response.getAvailableSlots());
        verify(conflictEngine, times(1)).validateScheduleConflict(any());
        verify(classRepository, times(1)).save(any(ClassEntity.class));
    }

    @Test
    @DisplayName("Mở lớp thất bại với lỗi 400 ADM_ROOM_CAPACITY_EXCEEDED khi max_students > room.capacity (FR-FAC-03)")
    void createClass_RoomCapacityExceeded_ThrowsException() {
        CreateClassRequest request = new CreateClassRequest(
                "CLS-OVERFLOW", "Lớp Quá Tải", 1, 3, null,
                "MON_WED_FRI", LocalTime.of(8, 0), LocalTime.of(9, 30),
                LocalDate.of(2026, 11, 15), LocalDate.of(2027, 2, 15),
                10, 30 // Sĩ số xin mở: 30
        );

        Course course = new Course();
        course.setId(1);

        Room smallRoom = new Room();
        smallRoom.setId(3);
        smallRoom.setName("Phòng Nhỏ");
        smallRoom.setCapacity(20); // Sức chứa chỉ có 20!

        when(courseRepository.findById(1)).thenReturn(Optional.of(course));
        when(roomRepository.findById(3)).thenReturn(Optional.of(smallRoom));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> classService.createClass(request));

        assertEquals(ErrorCode.ADM_ROOM_CAPACITY_EXCEEDED, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("vượt quá sức chứa tối đa"));
        verify(classRepository, never()).save(any(ClassEntity.class));
    }

    @Test
    @DisplayName("Mở lớp ném lỗi 400 khi ngày bế giảng <= ngày khai giảng")
    void createClass_InvalidDateRange_ThrowsValidation() {
        CreateClassRequest request = new CreateClassRequest(
                "CLS-ERR-DATE", "Lớp Lỗi Ngày", 1, 2, null,
                "MON_WED_FRI", LocalTime.of(8, 0), LocalTime.of(9, 30),
                LocalDate.of(2027, 2, 15), LocalDate.of(2026, 11, 15), // end < start!
                10, 20
        );

        BusinessException exception = assertThrows(BusinessException.class,
                () -> classService.createClass(request));

        assertEquals(ErrorCode.ADM_VALIDATION_ERROR, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("end_date"));
    }

    @Test
    @DisplayName("Chuyển trạng thái lớp hợp lệ: OPEN -> IN_PROGRESS -> COMPLETED")
    void updateClassStatus_ValidTransitions() {
        ClassEntity classEntity = new ClassEntity();
        classEntity.setId(20);
        classEntity.setStatus("OPEN");
        classEntity.setMaxStudents(25);

        when(classRepository.findById(20)).thenReturn(Optional.of(classEntity));
        when(classRepository.save(any(ClassEntity.class))).thenAnswer(i -> i.getArgument(0));

        // 1. OPEN -> IN_PROGRESS
        ClassResponse r1 = classService.updateClassStatus(20, "IN_PROGRESS");
        assertEquals("IN_PROGRESS", r1.getStatus());

        // 2. IN_PROGRESS -> COMPLETED
        ClassResponse r2 = classService.updateClassStatus(20, "COMPLETED");
        assertEquals("COMPLETED", r2.getStatus());
    }

    @Test
    @DisplayName("Chuyển trạng thái lớp không hợp lệ bị từ chối (COMPLETED -> OPEN)")
    void updateClassStatus_InvalidTransition_ThrowsException() {
        ClassEntity completedClass = new ClassEntity();
        completedClass.setId(21);
        completedClass.setStatus("COMPLETED");

        when(classRepository.findById(21)).thenReturn(Optional.of(completedClass));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> classService.updateClassStatus(21, "OPEN"));

        assertEquals(ErrorCode.ADM_INVALID_STATE_TRANSITION, exception.getErrorCode());
    }

    @Test
    @DisplayName("Tính toán sĩ số và slot khả dụng thời gian thực chính xác (TASK-ADM-24, FR-ACA-05)")
    void getClassCapacity_CalculatesCorrectly() {
        ClassEntity classEntity = new ClassEntity();
        classEntity.setId(50);
        classEntity.setName("Lớp IELTS Test Capacity");
        classEntity.setMaxStudents(20);

        when(classRepository.findById(50)).thenReturn(Optional.of(classEntity));
        when(enrollmentRepository.countByClassIdAndStatus(eq(50), eq("APPROVED"))).thenReturn(14L);
        when(enrollmentRepository.countActiveHeldSlots(eq(50), any())).thenReturn(3L);

        ClassCapacityResponse capacity = classService.getClassCapacity(50);

        assertNotNull(capacity);
        assertEquals(20, capacity.getMaxStudents());
        assertEquals(14L, capacity.getApprovedCount());
        assertEquals(3L, capacity.getActiveHeldCount());
        assertEquals(3L, capacity.getAvailableSlots()); // 20 - 14 - 3 = 3
        assertFalse(capacity.getIsFull());
    }

    @Test
    @DisplayName("Khi tổng số người học + giữ chỗ bằng max_students thì isFull = true và availableSlots = 0")
    void getClassCapacity_FullClass_ReturnsIsFullTrue() {
        ClassEntity classEntity = new ClassEntity();
        classEntity.setId(51);
        classEntity.setName("Lớp Đã Đầy");
        classEntity.setMaxStudents(20);

        when(classRepository.findById(51)).thenReturn(Optional.of(classEntity));
        when(enrollmentRepository.countByClassIdAndStatus(eq(51), eq("APPROVED"))).thenReturn(18L);
        when(enrollmentRepository.countActiveHeldSlots(eq(51), any())).thenReturn(2L);

        ClassCapacityResponse capacity = classService.getClassCapacity(51);

        assertNotNull(capacity);
        assertEquals(0L, capacity.getAvailableSlots()); // 20 - 18 - 2 = 0
        assertTrue(capacity.getIsFull());
    }
}
