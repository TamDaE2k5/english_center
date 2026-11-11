package com.englishcenter.admission.dto.clazz;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO yêu cầu cập nhật thông tin lớp học (FR-ACA-02).
 */
public class UpdateClassRequest {

    @Size(max = 255, message = "Tên lớp học không vượt quá 255 ký tự")
    private String name;

    private Integer roomId;
    private Integer teacherId;
    private String dayPattern;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDate startDate;
    private LocalDate endDate;

    @Min(value = 1, message = "Sĩ số tối thiểu phải >= 1")
    private Integer minStudents;

    @Min(value = 1, message = "Sĩ số tối đa phải >= 1")
    private Integer maxStudents;

    public UpdateClassRequest() {
    }

    public UpdateClassRequest(String name, Integer roomId, Integer teacherId, String dayPattern,
                              LocalTime startTime, LocalTime endTime, LocalDate startDate,
                              LocalDate endDate, Integer minStudents, Integer maxStudents) {
        this.name = name;
        this.roomId = roomId;
        this.teacherId = teacherId;
        this.dayPattern = dayPattern;
        this.startTime = startTime;
        this.endTime = endTime;
        this.startDate = startDate;
        this.endDate = endDate;
        this.minStudents = minStudents;
        this.maxStudents = maxStudents;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
}
