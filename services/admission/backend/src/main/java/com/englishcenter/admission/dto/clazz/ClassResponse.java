package com.englishcenter.admission.dto.clazz;

import com.englishcenter.admission.entity.ClassEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * DTO phản hồi thông tin chi tiết lớp học.
 * Tuân thủ quy ước snake_case khi tuần tự hóa sang JSON.
 */
public class ClassResponse {

    private Integer id;
    private String code;
    private String name;
    private Integer courseId;
    private String courseName;
    private String courseCode;
    private Integer roomId;
    private String roomName;
    private String roomCode;
    private Integer roomCapacity;
    private Integer teacherId;
    private String teacherName;
    private String teacherEmail;
    private String dayPattern;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer minStudents;
    private Integer maxStudents;
    private String status;
    private Integer enrolledStudents;
    private Integer availableSlots;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ClassResponse() {
    }

    public static ClassResponse fromEntity(ClassEntity entity, int enrolledCount, int availableSlots) {
        if (entity == null) {
            return null;
        }

        ClassResponse res = new ClassResponse();
        res.id = entity.getId();
        res.code = entity.getCode();
        res.name = entity.getName();
        res.courseId = entity.getCourseId();
        if (entity.getCourse() != null) {
            res.courseName = entity.getCourse().getName();
            res.courseCode = entity.getCourse().getCode();
        }

        res.roomId = entity.getRoomId();
        if (entity.getRoomEntity() != null) {
            res.roomName = entity.getRoomEntity().getName();
            res.roomCode = entity.getRoomEntity().getCode();
            res.roomCapacity = entity.getRoomEntity().getCapacity();
        } else if (entity.getRoom() != null) {
            res.roomName = entity.getRoom();
        }

        res.teacherId = entity.getTeacherId();
        if (entity.getTeacher() != null) {
            res.teacherName = entity.getTeacher().getFullName();
            res.teacherEmail = entity.getTeacher().getEmail();
        } else if (entity.getTeacherName() != null) {
            res.teacherName = entity.getTeacherName();
        }

        res.dayPattern = entity.getDayPattern();
        res.startTime = entity.getStartTime();
        res.endTime = entity.getEndTime();
        res.startDate = entity.getStartDate();
        res.endDate = entity.getEndDate();
        res.minStudents = entity.getMinStudents();
        res.maxStudents = entity.getMaxStudents();
        res.status = entity.getStatus();
        res.enrolledStudents = enrolledCount;
        res.availableSlots = Math.max(0, availableSlots);
        res.createdAt = entity.getCreatedAt();
        res.updatedAt = entity.getUpdatedAt();

        return res;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public Integer getRoomId() {
        return roomId;
    }

    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public Integer getRoomCapacity() {
        return roomCapacity;
    }

    public void setRoomCapacity(Integer roomCapacity) {
        this.roomCapacity = roomCapacity;
    }

    public Integer getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Integer teacherId) {
        this.teacherId = teacherId;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getTeacherEmail() {
        return teacherEmail;
    }

    public void setTeacherEmail(String teacherEmail) {
        this.teacherEmail = teacherEmail;
    }

    public String getDayPattern() {
        return dayPattern;
    }

    public void setDayPattern(String dayPattern) {
        this.dayPattern = dayPattern;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Integer getMinStudents() {
        return minStudents;
    }

    public void setMinStudents(Integer minStudents) {
        this.minStudents = minStudents;
    }

    public Integer getMaxStudents() {
        return maxStudents;
    }

    public void setMaxStudents(Integer maxStudents) {
        this.maxStudents = maxStudents;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getEnrolledStudents() {
        return enrolledStudents;
    }

    public void setEnrolledStudents(Integer enrolledStudents) {
        this.enrolledStudents = enrolledStudents;
    }

    public Integer getAvailableSlots() {
        return availableSlots;
    }

    public void setAvailableSlots(Integer availableSlots) {
        this.availableSlots = availableSlots;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
