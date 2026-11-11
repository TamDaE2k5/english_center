package com.englishcenter.admission.dto.clazz;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO yêu cầu mở lớp học mới (FR-ACA-02, FR-FAC-03).
 */
public class CreateClassRequest {

    @Size(max = 50, message = "Mã lớp học không vượt quá 50 ký tự")
    private String code;

    @NotBlank(message = "Tên lớp học không được để trống")
    @Size(max = 255, message = "Tên lớp học không vượt quá 255 ký tự")
    private String name;

    @NotNull(message = "ID khóa học không được để trống")
    private Integer courseId;

    @NotNull(message = "ID phòng học không được để trống")
    private Integer roomId;

    private Integer teacherId;

    @NotBlank(message = "Mẫu ngày học không được để trống")
    private String dayPattern = "MON_WED_FRI"; // MON_WED_FRI, TUE_THU_SAT, SAT_SUN, DAILY, CUSTOM

    @NotNull(message = "Giờ bắt đầu ca học không được để trống")
    private LocalTime startTime;

    @NotNull(message = "Giờ kết thúc ca học không được để trống")
    private LocalTime endTime;

    @NotNull(message = "Ngày khai giảng không được để trống")
    private LocalDate startDate;

    @NotNull(message = "Ngày bế giảng dự kiến không được để trống")
    private LocalDate endDate;

    @Min(value = 1, message = "Sĩ số tối thiểu phải >= 1")
    private Integer minStudents = 10;

    @NotNull(message = "Sĩ số tối đa không được để trống")
    @Min(value = 1, message = "Sĩ số tối đa phải >= 1")
    private Integer maxStudents = 25;

    public CreateClassRequest() {
    }

    public CreateClassRequest(String code, String name, Integer courseId, Integer roomId, Integer teacherId,
                              String dayPattern, LocalTime startTime, LocalTime endTime,
                              LocalDate startDate, LocalDate endDate, Integer minStudents, Integer maxStudents) {
        this.code = code;
        this.name = name;
        this.courseId = courseId;
        this.roomId = roomId;
        this.teacherId = teacherId;
        this.dayPattern = dayPattern;
        this.startTime = startTime;
        this.endTime = endTime;
        this.startDate = startDate;
        this.endDate = endDate;
        this.minStudents = minStudents != null ? minStudents : 10;
        this.maxStudents = maxStudents != null ? maxStudents : 25;
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
