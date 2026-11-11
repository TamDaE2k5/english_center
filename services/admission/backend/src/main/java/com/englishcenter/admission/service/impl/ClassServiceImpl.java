package com.englishcenter.admission.service.impl;

import com.englishcenter.admission.common.exception.BusinessException;
import com.englishcenter.admission.common.exception.ErrorCode;
import com.englishcenter.admission.dto.clazz.*;
import com.englishcenter.admission.engine.ConflictEngine;
import com.englishcenter.admission.engine.dto.ConflictCheckRequest;
import com.englishcenter.admission.entity.ClassEntity;
import com.englishcenter.admission.entity.Course;
import com.englishcenter.admission.entity.Room;
import com.englishcenter.admission.entity.User;
import com.englishcenter.admission.entity.Enrollment;
import com.englishcenter.admission.security.SecurityUtils;
import com.englishcenter.admission.repository.*;
import com.englishcenter.admission.service.ClassService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Hiện thực nghiệp vụ quản lý lớp học, kiểm soát sức chứa và vòng đời (FR-ACA-02, FR-ACA-04, FR-FAC-03, FR-ACA-05).
 */
@Service
public class ClassServiceImpl implements ClassService {

    private static final Logger log = LoggerFactory.getLogger(ClassServiceImpl.class);

    private final ClassRepository classRepository;
    private final CourseRepository courseRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ConflictEngine conflictEngine;

    public ClassServiceImpl(ClassRepository classRepository,
                            CourseRepository courseRepository,
                            RoomRepository roomRepository,
                            UserRepository userRepository,
                            EnrollmentRepository enrollmentRepository,
                            ConflictEngine conflictEngine) {
        this.classRepository = classRepository;
        this.courseRepository = courseRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.conflictEngine = conflictEngine;
    }

    @Override
    @Transactional
    public ClassResponse createClass(CreateClassRequest request) {
        // Serialize toàn bộ check -> save, kể cả khi hai request chưa có chung bản ghi lớp để khóa.
        roomRepository.lockScheduling();
        // 1. Kiểm tra tính hợp lệ của ngày học và giờ học
        validateDatesAndTimes(request.getStartDate(), request.getEndDate(),
                request.getStartTime(), request.getEndTime());

        // 2. Kiểm tra quan hệ sĩ số tối đa >= sĩ số tối thiểu
        if (request.getMaxStudents() < request.getMinStudents()) {
            throw new BusinessException(ErrorCode.ADM_VALIDATION_ERROR,
                    "Sĩ số tối đa (max_students) phải lớn hơn hoặc bằng sĩ số tối thiểu (min_students)");
        }

        // 3. Kiểm tra khóa học tồn tại
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_COURSE_NOT_FOUND,
                        "Không tìm thấy khóa học với ID: " + request.getCourseId()));

        // 4. Kiểm tra phòng học tồn tại
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_ROOM_NOT_FOUND,
                        "Không tìm thấy phòng học với ID: " + request.getRoomId()));

        // 5. RÀNG BUỘC SỨC CHỨA PHÒNG (FR-FAC-03): max_students <= room.capacity
        if (request.getMaxStudents() > room.getCapacity()) {
            log.warn("Mở lớp thất bại: sĩ số lớp ({}) vượt quá sức chứa phòng {} ({})",
                    request.getMaxStudents(), room.getCode(), room.getCapacity());
            throw new BusinessException(ErrorCode.ADM_ROOM_CAPACITY_EXCEEDED,
                    String.format("Sĩ số lớp (%d) vượt quá sức chứa tối đa của phòng học %s (%d)",
                            request.getMaxStudents(), room.getName(), room.getCapacity()));
        }

        // 6. Kiểm tra giảng viên phụ trách (nếu có chỉ định)
        User teacher = null;
        if (request.getTeacherId() != null) {
            teacher = userRepository.findById(request.getTeacherId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ADM_USER_NOT_FOUND,
                            "Không tìm thấy giảng viên với ID: " + request.getTeacherId()));
            if (!"TEACHER".equalsIgnoreCase(teacher.getRole())) {
                throw new BusinessException(ErrorCode.ADM_VALIDATION_ERROR,
                        "Người dùng được phân công giảng dạy phải có vai trò TEACHER");
            }
        }

        // 7. ĐỘNG CƠ KIỂM TRA XUNG ĐỘT THỜI KHÓA BIỂU (FR-ACA-03, TASK-ADM-23)
        conflictEngine.validateScheduleConflict(new ConflictCheckRequest(
                null,
                room.getId(),
                teacher != null ? teacher.getId() : null,
                request.getDayPattern(),
                request.getStartTime(),
                request.getEndTime(),
                request.getStartDate(),
                request.getEndDate()
        ));

        // 8. Tự động sinh mã lớp nếu chưa có hoặc kiểm tra trùng mã
        String code = request.getCode();
        if (code == null || code.isBlank()) {
            code = String.format("CLS-%s-%04d",
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")),
                    (int) (classRepository.count() + 1));
        } else {
            code = code.trim().toUpperCase();
            if (classRepository.existsByCode(code)) {
                throw new BusinessException(ErrorCode.ADM_VALIDATION_ERROR,
                        "Mã lớp học " + code + " đã tồn tại trong hệ thống");
            }
        }

        // 9. Khởi tạo thực thể ClassEntity
        ClassEntity classEntity = new ClassEntity();
        classEntity.setCourseId(course.getId());
        classEntity.setRoomId(room.getId());
        classEntity.setTeacherId(teacher != null ? teacher.getId() : null);
        classEntity.setCode(code);
        classEntity.setName(request.getName().trim());
        classEntity.setDayPattern(request.getDayPattern().trim().toUpperCase());
        classEntity.setStartTime(request.getStartTime());
        classEntity.setEndTime(request.getEndTime());
        classEntity.setStartDate(request.getStartDate());
        classEntity.setEndDate(request.getEndDate());
        classEntity.setMinStudents(request.getMinStudents());
        classEntity.setMaxStudents(request.getMaxStudents());
        classEntity.setStatus("OPEN");

        // Các trường snapshot tương thích dữ liệu
        classEntity.setRoom(room.getName());
        classEntity.setTeacherName(teacher != null ? teacher.getFullName() : null);
        classEntity.setSchedule(request.getDayPattern() + " " + request.getStartTime() + " - " + request.getEndTime());

        ClassEntity saved = classRepository.save(classEntity);
        log.info("Mở lớp học mới thành công: ID={}, code={}, max_students={}, status=OPEN",
                saved.getId(), saved.getCode(), saved.getMaxStudents());

        return toClassResponse(saved);
    }

    @Override
    @Transactional
    public ClassResponse updateClass(Integer id, UpdateClassRequest request) {
        // Luôn lấy khóa lịch trước khóa lớp, tránh đảo thứ tự giữa các thao tác sửa lịch.
        roomRepository.lockScheduling();
        ClassEntity classEntity = classRepository.findByIdWithLock(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_CLASS_NOT_FOUND,
                        "Không tìm thấy lớp học với ID: " + id));

        // Không cho phép chỉnh sửa thông tin lớp đã COMPLETED hoặc CANCELLED
        if ("COMPLETED".equalsIgnoreCase(classEntity.getStatus()) || "CANCELLED".equalsIgnoreCase(classEntity.getStatus())) {
            log.warn("Không thể cập nhật lớp ID={} vì trạng thái hiện tại là {}", id, classEntity.getStatus());
            throw new BusinessException(ErrorCode.ADM_INVALID_STATE_TRANSITION,
                    "Không thể chỉnh sửa lớp học đã hoàn thành (COMPLETED) hoặc đã hủy (CANCELLED)");
        }

        // Xác định giá trị mới hoặc giữ nguyên giá trị cũ
        Integer newRoomId = request.getRoomId() != null ? request.getRoomId() : classEntity.getRoomId();
        Integer newTeacherId = request.getTeacherId() != null ? request.getTeacherId() : classEntity.getTeacherId();
        String newDayPattern = request.getDayPattern() != null ? request.getDayPattern().trim().toUpperCase() : classEntity.getDayPattern();
        LocalDate newStartDate = request.getStartDate() != null ? request.getStartDate() : classEntity.getStartDate();
        LocalDate newEndDate = request.getEndDate() != null ? request.getEndDate() : classEntity.getEndDate();
        java.time.LocalTime newStartTime = request.getStartTime() != null ? request.getStartTime() : classEntity.getStartTime();
        java.time.LocalTime newEndTime = request.getEndTime() != null ? request.getEndTime() : classEntity.getEndTime();
        Integer newMin = request.getMinStudents() != null ? request.getMinStudents() : classEntity.getMinStudents();
        Integer newMax = request.getMaxStudents() != null ? request.getMaxStudents() : classEntity.getMaxStudents();

        validateDatesAndTimes(newStartDate, newEndDate, newStartTime, newEndTime);

        if (newMax < newMin) {
            throw new BusinessException(ErrorCode.ADM_VALIDATION_ERROR,
                    "Sĩ số tối đa (max_students) phải lớn hơn hoặc bằng sĩ số tối thiểu (min_students)");
        }

        // Kiểm tra sức chứa phòng học mới
        Room room = roomRepository.findById(newRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_ROOM_NOT_FOUND,
                        "Không tìm thấy phòng học với ID: " + newRoomId));

        if (newMax > room.getCapacity()) {
            throw new BusinessException(ErrorCode.ADM_ROOM_CAPACITY_EXCEEDED,
                    String.format("Sĩ số lớp (%d) vượt quá sức chứa tối đa của phòng học %s (%d)",
                            newMax, room.getName(), room.getCapacity()));
        }

        // Kiểm tra giảng viên mới (nếu có)
        User teacher = null;
        if (newTeacherId != null) {
            teacher = userRepository.findById(newTeacherId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ADM_USER_NOT_FOUND,
                            "Không tìm thấy giảng viên với ID: " + newTeacherId));
            if (!"TEACHER".equalsIgnoreCase(teacher.getRole())) {
                throw new BusinessException(ErrorCode.ADM_VALIDATION_ERROR,
                        "Người dùng được phân công giảng dạy phải có vai trò TEACHER");
            }
        }

        // Kiểm tra xung đột lịch (loại trừ chính lớp hiện tại)
        conflictEngine.validateScheduleConflict(new ConflictCheckRequest(
                id,
                room.getId(),
                teacher != null ? teacher.getId() : null,
                newDayPattern,
                newStartTime,
                newEndTime,
                newStartDate,
                newEndDate
        ));

        // Cập nhật các trường
        if (request.getName() != null && !request.getName().isBlank()) {
            classEntity.setName(request.getName().trim());
        }
        classEntity.setRoomId(room.getId());
        classEntity.setTeacherId(teacher != null ? teacher.getId() : null);
        classEntity.setDayPattern(newDayPattern);
        classEntity.setStartDate(newStartDate);
        classEntity.setEndDate(newEndDate);
        classEntity.setStartTime(newStartTime);
        classEntity.setEndTime(newEndTime);
        classEntity.setMinStudents(newMin);
        classEntity.setMaxStudents(newMax);

        // Cập nhật snapshot
        classEntity.setRoom(room.getName());
        classEntity.setTeacherName(teacher != null ? teacher.getFullName() : null);
        classEntity.setSchedule(newDayPattern + " " + newStartTime + " - " + newEndTime);

        ClassEntity updated = classRepository.save(classEntity);
        log.info("Cập nhật thông tin lớp học ID={} thành công", updated.getId());

        return toClassResponse(updated);
    }

    @Override
    @Transactional
    public ClassResponse updateClassStatus(Integer id, String newStatus) {
        if (newStatus != null && "CANCELLED".equalsIgnoreCase(newStatus.trim())) return cancelClass(id, "Hủy lớp thiếu sĩ số");
        ClassEntity classEntity = classRepository.findByIdWithLock(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_CLASS_NOT_FOUND,
                        "Không tìm thấy lớp học với ID: " + id));

        String currentStatus = classEntity.getStatus().toUpperCase();
        String targetStatus = newStatus.trim().toUpperCase();

        if (currentStatus.equals(targetStatus)) {
            return toClassResponse(classEntity);
        }

        // Kiểm tra quy tắc chuyển trạng thái hợp lệ (FR-ACA-04)
        boolean isValid = false;
        if ("OPEN".equals(currentStatus)) {
            isValid = "IN_PROGRESS".equals(targetStatus) || "CANCELLED".equals(targetStatus);
        } else if ("IN_PROGRESS".equals(currentStatus)) {
            isValid = "COMPLETED".equals(targetStatus) || "CANCELLED".equals(targetStatus);
        }

        if (!isValid) {
            log.warn("Chuyển trạng thái lớp ID={} bất hợp lệ: từ {} sang {}", id, currentStatus, targetStatus);
            throw new BusinessException(ErrorCode.ADM_INVALID_STATE_TRANSITION,
                    String.format("Không thể chuyển trạng thái lớp từ %s sang %s", currentStatus, targetStatus));
        }

        classEntity.setStatus(targetStatus);
        ClassEntity saved = classRepository.save(classEntity);
        log.info("Đã chuyển trạng thái lớp ID={} từ {} sang {}", id, currentStatus, targetStatus);

        return toClassResponse(saved);
    }

    /** Khóa lớp trước đơn như luồng giữ chỗ; rollback toàn bộ nếu ghi yêu cầu Finance thất bại. */
    @Override @Transactional
    public ClassResponse cancelClass(Integer id, String reason) {
        if (reason == null || reason.isBlank() || reason.length() > 2000)
            throw new BusinessException(ErrorCode.ADM_VALIDATION_ERROR, "Cần lý do hủy lớp tối đa 2000 ký tự");
        ClassEntity clazz = classRepository.findByIdWithLock(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_CLASS_NOT_FOUND));
        if ("CANCELLED".equals(clazz.getStatus())) return toClassResponse(clazz);
        long approved = enrollmentRepository.countByClassIdAndStatus(id, "APPROVED");
        if (!"OPEN".equals(clazz.getStatus()) || clazz.getStartDate() == null
                || !clazz.getStartDate().isAfter(LocalDate.now()) || approved >= clazz.getMinStudents())
            throw new BusinessException(ErrorCode.ADM_INVALID_STATE_TRANSITION, "Chỉ hủy lớp OPEN chưa khai giảng và thiếu sĩ số tối thiểu");
        for (Enrollment e : enrollmentRepository.findByClassIdOrderByCreatedAtAsc(id)) {
            enrollmentRepository.refreshForMutation(e);
            if ("PENDING".equals(e.getStatus())) {
                e.setStatus("CANCELLED"); e.setHoldExpiresAt(LocalDateTime.now());
            } else if ("APPROVED".equals(e.getStatus())) {
                enrollmentRepository.enqueueClassRefund(e.getId(), id, e.getInvoiceCode(), reason.trim(), SecurityUtils.getCurrentUserId());
                e.setStatus("CANCELLED_REFUND_PENDING");
            } else continue;
            String notes = (e.getNotes() == null ? "" : e.getNotes() + " | ") + "[Hủy lớp]: " + reason.trim();
            e.setNotes(notes); e.setNote(notes); enrollmentRepository.save(e);
        }
        clazz.setStatus("CANCELLED"); classRepository.save(clazz);
        return toClassResponse(clazz);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassResponse getClassById(Integer id) {
        ClassEntity classEntity = classRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_CLASS_NOT_FOUND,
                        "Không tìm thấy lớp học với ID: " + id));

        return toClassResponse(classEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassResponse> getClasses(Integer courseId, String status) {
        return classRepository.searchClasses(courseId, status)
                .stream()
                .map(this::toClassResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ClassCapacityResponse getClassCapacity(Integer classId) {
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_CLASS_NOT_FOUND,
                        "Không tìm thấy lớp học với ID: " + classId));

        LocalDateTime now = LocalDateTime.now();
        long approvedCount = enrollmentRepository.countByClassIdAndStatus(classId, "APPROVED");
        long activeHeldCount = enrollmentRepository.countActiveHeldSlots(classId, now);

        long occupied = approvedCount + activeHeldCount;
        long availableSlots = Math.max(0, classEntity.getMaxStudents() - occupied);
        boolean isFull = (availableSlots <= 0);

        return new ClassCapacityResponse(
                classEntity.getId(),
                classEntity.getName(),
                classEntity.getMaxStudents(),
                approvedCount,
                activeHeldCount,
                availableSlots,
                isFull
        );
    }

    /**
     * Chuyển đổi ClassEntity sang ClassResponse kèm tính toán slot khả dụng.
     */
    private ClassResponse toClassResponse(ClassEntity entity) {
        long approvedCount = enrollmentRepository.countByClassIdAndStatus(entity.getId(), "APPROVED");
        long occupiedSlots = enrollmentRepository.countOccupiedSlots(entity.getId(), LocalDateTime.now());
        int available = (int) Math.max(0, entity.getMaxStudents() - occupiedSlots);

        return ClassResponse.fromEntity(entity, (int) approvedCount, available);
    }

    /**
     * Xác thực khoảng ngày và khung giờ hợp lệ.
     */
    private void validateDatesAndTimes(LocalDate startDate, LocalDate endDate,
                                       java.time.LocalTime startTime, java.time.LocalTime endTime) {
        if (startDate == null || endDate == null || !endDate.isAfter(startDate)) {
            throw new BusinessException(ErrorCode.ADM_VALIDATION_ERROR,
                    "Ngày bế giảng (end_date) phải diễn ra sau ngày khai giảng (start_date)");
        }
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new BusinessException(ErrorCode.ADM_VALIDATION_ERROR,
                    "Giờ kết thúc ca học (end_time) phải sau giờ bắt đầu (start_time)");
        }
    }
}
