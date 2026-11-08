package com.englishcenter.admission.dto.enrollment;

import com.englishcenter.admission.entity.Enrollment;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * DTO phản hồi thông tin chi tiết đơn đăng ký tuyển sinh.
 * Tuân thủ quy ước snake_case khi tuần tự hóa sang JSON.
 */
public class EnrollmentResponse {

    private Integer enrollmentId;
    private String enrollmentCode;
    private Integer classId;
    private String className;
    private String classCode;
    private Integer studentId;
    private String studentName;
    private String studentEmail;
    private String studentPhone;
    private String status;
    private LocalDateTime holdExpiresAt;
    private Long remainingSeconds;
    private String invoiceCode;
    private String source;
    private String approvalMethod;
    private String notes;
    private Boolean isSyncedToStudent;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;

    public EnrollmentResponse() {
    }

    public static EnrollmentResponse fromEntity(Enrollment entity) {
        if (entity == null) {
            return null;
        }

        EnrollmentResponse res = new EnrollmentResponse();
        res.enrollmentId = entity.getId();
        res.enrollmentCode = String.format("ENR-%d", entity.getId());
        res.classId = entity.getClassId();
        if (entity.getClazz() != null) {
            res.className = entity.getClazz().getName();
            res.classCode = entity.getClazz().getCode();
        }

        res.studentId = entity.getStudentId();
        if (entity.getStudent() != null) {
            res.studentName = entity.getStudent().getFullName();
            res.studentEmail = entity.getStudent().getEmail();
            res.studentPhone = entity.getStudent().getPhone();
        }

        res.status = entity.getStatus();
        res.holdExpiresAt = entity.getHoldExpiresAt();

        // Tính thời gian đếm ngược còn lại (TASK-ADM-42)
        if ("PENDING".equalsIgnoreCase(entity.getStatus()) && entity.getHoldExpiresAt() != null) {
            long sec = Duration.between(LocalDateTime.now(), entity.getHoldExpiresAt()).getSeconds();
            res.remainingSeconds = Math.max(0L, sec);
        } else {
            res.remainingSeconds = 0L;
        }

        res.invoiceCode = entity.getInvoiceCode();
        res.source = entity.getSource();
        res.approvalMethod = entity.getApprovalMethod();
        res.notes = entity.getNotes() != null ? entity.getNotes() : entity.getNote();
        res.isSyncedToStudent = entity.getIsSyncedToStudent();
        res.createdAt = entity.getCreatedAt() != null ? entity.getCreatedAt() : entity.getEnrolledAt();
        res.approvedAt = entity.getApprovedAt();

        return res;
    }

    public Integer getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(Integer enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public String getEnrollmentCode() {
        return enrollmentCode;
    }

    public void setEnrollmentCode(String enrollmentCode) {
        this.enrollmentCode = enrollmentCode;
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

    public String getClassCode() {
        return classCode;
    }

    public void setClassCode(String classCode) {
        this.classCode = classCode;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public String getStudentPhone() {
        return studentPhone;
    }

    public void setStudentPhone(String studentPhone) {
        this.studentPhone = studentPhone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getHoldExpiresAt() {
        return holdExpiresAt;
    }

    public void setHoldExpiresAt(LocalDateTime holdExpiresAt) {
        this.holdExpiresAt = holdExpiresAt;
    }

    public Long getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(Long remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }

    public String getInvoiceCode() {
        return invoiceCode;
    }

    public void setInvoiceCode(String invoiceCode) {
        this.invoiceCode = invoiceCode;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getApprovalMethod() {
        return approvalMethod;
    }

    public void setApprovalMethod(String approvalMethod) {
        this.approvalMethod = approvalMethod;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Boolean getIsSyncedToStudent() {
        return isSyncedToStudent;
    }

    public void setIsSyncedToStudent(Boolean isSyncedToStudent) {
        this.isSyncedToStudent = isSyncedToStudent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }
}
