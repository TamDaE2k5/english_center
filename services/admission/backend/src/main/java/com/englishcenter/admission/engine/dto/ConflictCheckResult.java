package com.englishcenter.admission.engine.dto;

/**
 * DTO kết quả phân tích xung đột thời khóa biểu từ ConflictEngine.
 */
public class ConflictCheckResult {

    private boolean hasConflict;
    private String conflictType; // ROOM_CONFLICT, TEACHER_CONFLICT, NONE
    private String conflictDescription;
    private Integer conflictingClassId;
    private String conflictingClassCode;
    private String conflictingClassName;

    public ConflictCheckResult() {
        this.hasConflict = false;
        this.conflictType = "NONE";
    }

    public static ConflictCheckResult noConflict() {
        return new ConflictCheckResult();
    }

    public static ConflictCheckResult roomConflict(Integer classId, String classCode, String className, String description) {
        ConflictCheckResult result = new ConflictCheckResult();
        result.hasConflict = true;
        result.conflictType = "ROOM_CONFLICT";
        result.conflictingClassId = classId;
        result.conflictingClassCode = classCode;
        result.conflictingClassName = className;
        result.conflictDescription = description;
        return result;
    }

    public static ConflictCheckResult teacherConflict(Integer classId, String classCode, String className, String description) {
        ConflictCheckResult result = new ConflictCheckResult();
        result.hasConflict = true;
        result.conflictType = "TEACHER_CONFLICT";
        result.conflictingClassId = classId;
        result.conflictingClassCode = classCode;
        result.conflictingClassName = className;
        result.conflictDescription = description;
        return result;
    }

    public boolean isHasConflict() {
        return hasConflict;
    }

    public void setHasConflict(boolean hasConflict) {
        this.hasConflict = hasConflict;
    }

    public String getConflictType() {
        return conflictType;
    }

    public void setConflictType(String conflictType) {
        this.conflictType = conflictType;
    }

    public String getConflictDescription() {
        return conflictDescription;
    }

    public void setConflictDescription(String conflictDescription) {
        this.conflictDescription = conflictDescription;
    }

    public Integer getConflictingClassId() {
        return conflictingClassId;
    }

    public void setConflictingClassId(Integer conflictingClassId) {
        this.conflictingClassId = conflictingClassId;
    }

    public String getConflictingClassCode() {
        return conflictingClassCode;
    }

    public void setConflictingClassCode(String conflictingClassCode) {
        this.conflictingClassCode = conflictingClassCode;
    }

    public String getConflictingClassName() {
        return conflictingClassName;
    }

    public void setConflictingClassName(String conflictingClassName) {
        this.conflictingClassName = conflictingClassName;
    }
}
