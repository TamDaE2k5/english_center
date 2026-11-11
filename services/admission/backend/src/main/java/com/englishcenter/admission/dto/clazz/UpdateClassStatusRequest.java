package com.englishcenter.admission.dto.clazz;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO yêu cầu cập nhật trạng thái vòng đời của lớp học (FR-ACA-04).
 * Trạng thái hợp lệ: OPEN, IN_PROGRESS, COMPLETED, CANCELLED.
 */
public class UpdateClassStatusRequest {

    @NotBlank(message = "Trạng thái lớp học không được để trống")
    private String status;

    public UpdateClassStatusRequest() {
    }

    public UpdateClassStatusRequest(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
