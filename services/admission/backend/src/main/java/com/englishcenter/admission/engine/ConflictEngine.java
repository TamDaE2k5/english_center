package com.englishcenter.admission.engine;

import com.englishcenter.admission.common.constant.DayPattern;
import com.englishcenter.admission.common.exception.BusinessException;
import com.englishcenter.admission.common.exception.ErrorCode;
import com.englishcenter.admission.engine.dto.ConflictCheckRequest;
import com.englishcenter.admission.engine.dto.ConflictCheckResult;
import com.englishcenter.admission.entity.ClassEntity;
import com.englishcenter.admission.repository.ClassRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Động cơ Phân tích Xung đột Thời khóa biểu (Conflict Engine - FR-ACA-03).
 * Ngăn ngừa xung đột phòng học (ADM_ROOM_CONFLICT) và xung đột giảng viên (ADM_TEACHER_CONFLICT).
 */
@Component
public class ConflictEngine {

    private static final Logger log = LoggerFactory.getLogger(ConflictEngine.class);

    private final ClassRepository classRepository;

    public ConflictEngine(ClassRepository classRepository) {
        this.classRepository = classRepository;
    }

    /**
     * Kiểm tra xung đột lịch học của phòng học và giảng viên.
     * Ném ngoại lệ BusinessException nếu phát hiện bất kỳ xung đột nào.
     *
     * @param request Thông tin ca học, ngày học, phòng và giảng viên cần kiểm tra
     * @throws BusinessException nếu xảy ra xung đột
     */
    public void validateScheduleConflict(ConflictCheckRequest request) {
        ConflictCheckResult result = checkConflict(request);
        if (result.isHasConflict()) {
            if ("ROOM_CONFLICT".equals(result.getConflictType())) {
                log.warn("Phát hiện xung đột phòng học: {}", result.getConflictDescription());
                throw new BusinessException(ErrorCode.ADM_ROOM_CONFLICT, result.getConflictDescription());
            } else if ("TEACHER_CONFLICT".equals(result.getConflictType())) {
                log.warn("Phát hiện xung đột giảng viên: {}", result.getConflictDescription());
                throw new BusinessException(ErrorCode.ADM_TEACHER_CONFLICT, result.getConflictDescription());
            }
        }
    }

    /**
     * Phân tích và trả về đối tượng kết quả xung đột (không ném ngoại lệ).
     *
     * @param request Thông tin kiểm tra
     * @return ConflictCheckResult chứa thông tin chi tiết xung đột (nếu có)
     */
    public ConflictCheckResult checkConflict(ConflictCheckRequest request) {
        if (request.getStartDate() == null || request.getEndDate() == null
                || request.getStartTime() == null || request.getEndTime() == null
                || request.getDayPattern() == null) {
            return ConflictCheckResult.noConflict();
        }

        // 1. Kiểm tra xung đột phòng học (nếu có chỉ định roomId)
        if (request.getRoomId() != null) {
            List<ClassEntity> candidateRooms = classRepository.findConflictingRoomClasses(
                    request.getRoomId(),
                    request.getClassId(),
                    request.getStartDate(),
                    request.getEndDate(),
                    request.getStartTime(),
                    request.getEndTime()
            );

            for (ClassEntity candidate : candidateRooms) {
                if (isDayPatternOverlapping(request.getDayPattern(), candidate.getDayPattern())) {
                    String desc = String.format(
                            "Phòng học bị trùng lịch với lớp '%s' (Mã: %s, Mẫu ngày: %s, Ca: %s - %s)",
                            candidate.getName(), candidate.getCode(), candidate.getDayPattern(),
                            candidate.getStartTime(), candidate.getEndTime()
                    );
                    return ConflictCheckResult.roomConflict(
                            candidate.getId(), candidate.getCode(), candidate.getName(), desc
                    );
                }
            }
        }

        // 2. Kiểm tra xung đột giảng viên (nếu có chỉ định teacherId)
        if (request.getTeacherId() != null) {
            List<ClassEntity> candidateTeachers = classRepository.findConflictingTeacherClasses(
                    request.getTeacherId(),
                    request.getClassId(),
                    request.getStartDate(),
                    request.getEndDate(),
                    request.getStartTime(),
                    request.getEndTime()
            );

            for (ClassEntity candidate : candidateTeachers) {
                if (isDayPatternOverlapping(request.getDayPattern(), candidate.getDayPattern())) {
                    String desc = String.format(
                            "Giảng viên bị trùng lịch dạy với lớp '%s' (Mã: %s, Mẫu ngày: %s, Ca: %s - %s)",
                            candidate.getName(), candidate.getCode(), candidate.getDayPattern(),
                            candidate.getStartTime(), candidate.getEndTime()
                    );
                    return ConflictCheckResult.teacherConflict(
                            candidate.getId(), candidate.getCode(), candidate.getName(), desc
                    );
                }
            }
        }

        return ConflictCheckResult.noConflict();
    }

    /**
     * Xác định xem hai chuỗi mẫu ngày học có bị trùng lặp ngày nào trong tuần không.
     *
     * @param p1 Mẫu ngày thứ nhất
     * @param p2 Mẫu ngày thứ hai
     * @return true nếu có ít nhất 1 ngày học giao nhau
     */
    public boolean isDayPatternOverlapping(String p1, String p2) {
        if (p1 == null || p2 == null) {
            return false;
        }

        try {
            DayPattern dp1 = DayPattern.valueOf(p1.trim().toUpperCase());
            DayPattern dp2 = DayPattern.valueOf(p2.trim().toUpperCase());
            return dp1.overlapsWith(dp2);
        } catch (IllegalArgumentException e) {
            // Trường hợp mẫu ngày tùy chỉnh hoặc không nhận dạng được: so sánh trực tiếp
            return p1.trim().equalsIgnoreCase(p2.trim());
        }
    }
}
