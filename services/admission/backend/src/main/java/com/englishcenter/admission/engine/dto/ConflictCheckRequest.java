package com.englishcenter.admission.engine.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO yêu cầu kiểm tra xung đột thời khóa biểu giữa phòng học và giảng viên.
 */
public class ConflictCheckRequest {

    private Integer classId;
    private Integer roomId;
    private Integer teacherId;
    private String dayPattern;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDate startDate;
    private LocalDate endDate;

    public ConflictCheckRequest() {
    }

    public ConflictCheckRequest(Integer classId, Integer roomId, Integer teacherId, String dayPattern,
                                LocalTime startTime, LocalTime endTime, LocalDate startDate, LocalDate endDate) {
        this.classId = classId;
        this.roomId = roomId;
        this.teacherId = teacherId;
        this.dayPattern = dayPattern;
        this.startTime = startTime;
        this.endTime = endTime;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Integer getClassId() {
        return classId;
    }

    public void setClassId(Integer classId) {
        this.classId = classId;
    }

    public Integer getRoomId() {
        return roomId;
    }

    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }

    public Integer getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Integer teacherId) {
        this.teacherId = teacherId;
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
}
