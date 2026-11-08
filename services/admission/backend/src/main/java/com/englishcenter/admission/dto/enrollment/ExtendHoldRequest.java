package com.englishcenter.admission.dto.enrollment;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO yêu cầu gia hạn thời gian giữ chỗ tuyển sinh (FR-ENR-05, TASK-ADM-44).
 */
public class ExtendHoldRequest {

    @NotNull(message = "Số giờ gia hạn không được để trống")
    @Min(value = 1, message = "Số giờ gia hạn tối thiểu là 1 giờ")
    @Max(value = 72, message = "Số giờ gia hạn tối đa là 72 giờ (3 ngày)")
    private Integer extendedHours;

    @NotBlank(message = "Lý do gia hạn không được để trống")
    @Size(min = 5, message = "Lý do gia hạn phải có tối thiểu 5 ký tự để đối soát học vụ")
    private String reason;

    public ExtendHoldRequest() {
    }

    public ExtendHoldRequest(Integer extendedHours, String reason) {
        this.extendedHours = extendedHours;
        this.reason = reason;
    }

    public Integer getExtendedHours() {
        return extendedHours;
    }

    public void setExtendedHours(Integer extendedHours) {
        this.extendedHours = extendedHours;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
