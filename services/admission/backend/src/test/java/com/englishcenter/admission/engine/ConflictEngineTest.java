package com.englishcenter.admission.engine;

import com.englishcenter.admission.common.exception.BusinessException;
import com.englishcenter.admission.common.exception.ErrorCode;
import com.englishcenter.admission.engine.dto.ConflictCheckRequest;
import com.englishcenter.admission.engine.dto.ConflictCheckResult;
import com.englishcenter.admission.entity.ClassEntity;
import com.englishcenter.admission.repository.ClassRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConflictEngineTest {

    @Mock
    private ClassRepository classRepository;

    private ConflictEngine conflictEngine;

    @BeforeEach
    void setUp() {
        conflictEngine = new ConflictEngine(classRepository);
    }

    @Test
    @DisplayName("Không có xung đột khi phòng học và giảng viên đều chưa có lịch")
    void validateScheduleConflict_NoConflict_Passes() {
        ConflictCheckRequest request = new ConflictCheckRequest(
                null, 1, 2, "MON_WED_FRI",
                LocalTime.of(8, 0), LocalTime.of(9, 30),
                LocalDate.of(2026, 11, 15), LocalDate.of(2027, 2, 15)
        );

        when(classRepository.findConflictingRoomClasses(anyInt(), any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(classRepository.findConflictingTeacherClasses(anyInt(), any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> conflictEngine.validateScheduleConflict(request));
    }

    @Test
    @DisplayName("Cùng phòng và cùng khung giờ nhưng khác mẫu ngày (T2-4-6 vs T3-5-7) thì không xung đột")
    void validateScheduleConflict_DifferentDayPatterns_NoConflict() {
        ConflictCheckRequest request = new ConflictCheckRequest(
                null, 1, 2, "MON_WED_FRI",
                LocalTime.of(8, 0), LocalTime.of(9, 30),
                LocalDate.of(2026, 11, 15), LocalDate.of(2027, 2, 15)
        );

        ClassEntity existingClass = new ClassEntity();
        existingClass.setId(10);
        existingClass.setCode("CLS-TUE");
        existingClass.setName("Lớp Thứ 3-5-7");
        existingClass.setDayPattern("TUE_THU_SAT"); // Khác ngày trong tuần!
        existingClass.setStartTime(LocalTime.of(8, 0));
        existingClass.setEndTime(LocalTime.of(9, 30));

        when(classRepository.findConflictingRoomClasses(eq(1), any(), any(), any(), any(), any()))
                .thenReturn(List.of(existingClass));
        when(classRepository.findConflictingTeacherClasses(eq(2), any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        ConflictCheckResult result = conflictEngine.checkConflict(request);
        assertFalse(result.isHasConflict());
        assertEquals("NONE", result.getConflictType());
        assertDoesNotThrow(() -> conflictEngine.validateScheduleConflict(request));
    }

    @Test
    @DisplayName("Phát hiện xung đột phòng học và ném mã lỗi 409 ADM_ROOM_CONFLICT khi trùng ngày và giờ")
    void validateScheduleConflict_RoomConflict_ThrowsException() {
        ConflictCheckRequest request = new ConflictCheckRequest(
                null, 1, 2, "MON_WED_FRI",
                LocalTime.of(8, 0), LocalTime.of(9, 30),
                LocalDate.of(2026, 11, 15), LocalDate.of(2027, 2, 15)
        );

        ClassEntity conflictingClass = new ClassEntity();
        conflictingClass.setId(11);
        conflictingClass.setCode("CLS-EXISTING");
        conflictingClass.setName("Lớp Tiếng Anh Giao Tiếp");
        conflictingClass.setDayPattern("MON_WED_FRI"); // Trùng Thứ 2-4-6!
        conflictingClass.setStartTime(LocalTime.of(8, 0));
        conflictingClass.setEndTime(LocalTime.of(9, 30));

        when(classRepository.findConflictingRoomClasses(eq(1), any(), any(), any(), any(), any()))
                .thenReturn(List.of(conflictingClass));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> conflictEngine.validateScheduleConflict(request));

        assertEquals(ErrorCode.ADM_ROOM_CONFLICT, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("CLS-EXISTING"));
    }

    @Test
    @DisplayName("Phát hiện xung đột giảng viên và ném mã lỗi 409 ADM_TEACHER_CONFLICT khi giáo viên trùng lịch")
    void validateScheduleConflict_TeacherConflict_ThrowsException() {
        ConflictCheckRequest request = new ConflictCheckRequest(
                null, 1, 5, "SAT_SUN",
                LocalTime.of(14, 0), LocalTime.of(15, 30),
                LocalDate.of(2026, 11, 15), LocalDate.of(2027, 2, 15)
        );

        when(classRepository.findConflictingRoomClasses(eq(1), any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        ClassEntity teacherClass = new ClassEntity();
        teacherClass.setId(12);
        teacherClass.setCode("CLS-TEACHER-BUSY");
        teacherClass.setName("Lớp IELTS Thầy Nam");
        teacherClass.setDayPattern("SAT_SUN");
        teacherClass.setStartTime(LocalTime.of(14, 0));
        teacherClass.setEndTime(LocalTime.of(15, 30));

        when(classRepository.findConflictingTeacherClasses(eq(5), any(), any(), any(), any(), any()))
                .thenReturn(List.of(teacherClass));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> conflictEngine.validateScheduleConflict(request));

        assertEquals(ErrorCode.ADM_TEACHER_CONFLICT, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("CLS-TEACHER-BUSY"));
    }

    @Test
    @DisplayName("Mẫu ngày DAILY giao cắt với MON_WED_FRI phát hiện xung đột chính xác")
    void validateScheduleConflict_DailyPattern_ConflictsWithMonWedFri() {
        boolean overlaps = conflictEngine.isDayPatternOverlapping("DAILY", "MON_WED_FRI");
        assertTrue(overlaps);

        boolean reverseOverlaps = conflictEngine.isDayPatternOverlapping("TUE_THU_SAT", "DAILY");
        assertTrue(reverseOverlaps);

        boolean disjoint = conflictEngine.isDayPatternOverlapping("MON_WED_FRI", "SAT_SUN");
        assertFalse(disjoint);
    }
}
