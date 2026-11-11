package com.englishcenter.admission.dto.room;

import jakarta.validation.constraints.NotNull;

/**
 * DTO yêu cầu cập nhật trạng thái hoạt động của phòng học.
 */
public class UpdateRoomStatusRequest {

    @NotNull(message = "Trạng thái hoạt động không được để trống")
    private Boolean isActive;

    public UpdateRoomStatusRequest() {
    }

    public UpdateRoomStatusRequest(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
