package com.englishcenter.admission.dto.clazz;

/**
 * DTO phản hồi thông tin tính toán sĩ số và slot khả dụng thời gian thực của lớp học (FR-ACA-05, INV-ADM-01).
 * Tuân thủ quy ước snake_case khi tuần tự hóa sang JSON.
 */
public class ClassCapacityResponse {

    private Integer classId;
    private String className;
    private Integer maxStudents;
    private Long approvedCount;
    private Long activeHeldCount;
    private Long availableSlots;
    private Boolean isFull;

    public ClassCapacityResponse() {
    }

    public ClassCapacityResponse(Integer classId, String className, Integer maxStudents,
                                 Long approvedCount, Long activeHeldCount, Long availableSlots, Boolean isFull) {
        this.classId = classId;
        this.className = className;
        this.maxStudents = maxStudents;
        this.approvedCount = approvedCount;
        this.activeHeldCount = activeHeldCount;
        this.availableSlots = availableSlots;
        this.isFull = isFull;
    }

    public Integer getClassId() {
        return classId;
    }

    public void setClassId(Integer classId) {
        this.classId = classId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Integer getMaxStudents() {
        return maxStudents;
    }

    public void setMaxStudents(Integer maxStudents) {
        this.maxStudents = maxStudents;
    }

    public Long getApprovedCount() {
        return approvedCount;
    }

    public void setApprovedCount(Long approvedCount) {
        this.approvedCount = approvedCount;
    }

    public Long getActiveHeldCount() {
        return activeHeldCount;
    }

    public void setActiveHeldCount(Long activeHeldCount) {
        this.activeHeldCount = activeHeldCount;
    }

    public Long getAvailableSlots() {
        return availableSlots;
    }

    public void setAvailableSlots(Long availableSlots) {
        this.availableSlots = availableSlots;
    }

    public Boolean getIsFull() {
        return isFull;
    }

    public void setIsFull(Boolean isFull) {
        this.isFull = isFull;
    }
}
