package com.englishcenter.admission.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Thực thể biểu diễn đơn đăng ký tuyển sinh trong phân hệ Admission (bảng admission.enrollments).
 * Quản lý cơ chế giữ chỗ 48h (hold_expires_at), bảo vệ sĩ số trần (INV-ADM-01), và đồng bộ hồ sơ học vụ.
 */
@Entity
@Table(name = "enrollments", schema = "admission", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"student_id", "class_id"})
})
public class Enrollment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", insertable = false, updatable = false)
    private User student;

    @Column(name = "class_id", nullable = false)
    private Integer classId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", insertable = false, updatable = false)
    private ClassEntity clazz;

    @Column(nullable = false, length = 50)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED, EXPIRED, CANCELLED, CANCELLED_REFUND_PENDING

    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;

    @Column(name = "invoice_code", length = 50)
    private String invoiceCode;

    @Column(length = 50)
    private String source = "ONLINE"; // ONLINE, WALK_IN, HOTLINE, FANPAGE, REFERRAL

    @Column(name = "approval_method", length = 50)
    private String approvalMethod; // ONLINE_PAYMENT, MANUAL_CASH, STAFF_OVERRIDE

    @Column(name = "approved_by_user_id")
    private Integer approvedByUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id", insertable = false, updatable = false)
    private User approvedByUser;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "enrolled_at", nullable = false)
    private LocalDateTime enrolledAt = LocalDateTime.now();

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "enrollment_channel", nullable = false, length = 20)
    private String enrollmentChannel = "ONLINE"; // ONLINE, AT_COUNTER

    @Column(name = "hold_extended_by_user_id")
    private Integer holdExtendedByUserId;

    @Column(name = "hold_extension_reason", columnDefinition = "TEXT")
    private String holdExtensionReason;

    @Column(name = "student_sync_status", nullable = false, length = 20)
    private String studentSyncStatus = "NOT_STARTED"; // NOT_STARTED, PENDING, SYNCED, FAILED

    @Column(name = "is_synced_to_student", nullable = false)
    private Boolean isSyncedToStudent = false;

    public Enrollment() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public Integer getClassId() {
        return classId;
    }

    public void setClassId(Integer classId) {
        this.classId = classId;
    }

    public ClassEntity getClazz() {
        return clazz;
    }

    public void setClazz(ClassEntity clazz) {
        this.clazz = clazz;
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

    public Integer getApprovedByUserId() {
        return approvedByUserId;
    }

    public void setApprovedByUserId(Integer approvedByUserId) {
        this.approvedByUserId = approvedByUserId;
    }

    public User getApprovedByUser() {
        return approvedByUser;
    }

    public void setApprovedByUser(User approvedByUser) {
        this.approvedByUser = approvedByUser;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
        if (this.notes == null) {
            this.notes = note;
        }
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
        if (this.note == null) {
            this.note = notes;
        }
    }

    public LocalDateTime getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(LocalDateTime enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getEnrollmentChannel() {
        return enrollmentChannel;
    }

    public void setEnrollmentChannel(String enrollmentChannel) {
        this.enrollmentChannel = enrollmentChannel;
    }

    public Integer getHoldExtendedByUserId() {
        return holdExtendedByUserId;
    }

    public void setHoldExtendedByUserId(Integer holdExtendedByUserId) {
        this.holdExtendedByUserId = holdExtendedByUserId;
    }

    public String getHoldExtensionReason() {
        return holdExtensionReason;
    }

    public void setHoldExtensionReason(String holdExtensionReason) {
        this.holdExtensionReason = holdExtensionReason;
    }

    public String getStudentSyncStatus() {
        return studentSyncStatus;
    }

    public void setStudentSyncStatus(String studentSyncStatus) {
        this.studentSyncStatus = studentSyncStatus;
        if ("SYNCED".equals(studentSyncStatus)) {
            this.isSyncedToStudent = true;
        }
    }

    public Boolean getIsSyncedToStudent() {
        return isSyncedToStudent;
    }

    public void setIsSyncedToStudent(Boolean isSyncedToStudent) {
        this.isSyncedToStudent = isSyncedToStudent;
        if (Boolean.TRUE.equals(isSyncedToStudent) && "NOT_STARTED".equals(this.studentSyncStatus)) {
            this.studentSyncStatus = "SYNCED";
        }
    }
}
