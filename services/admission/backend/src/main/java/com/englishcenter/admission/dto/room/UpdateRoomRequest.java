package com.englishcenter.admission.dto.room;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO yêu cầu cập nhật thông tin phòng học.
 */
public class UpdateRoomRequest {

    @NotBlank(message = "Tên phòng không được để trống")
    @Size(max = 100, message = "Tên phòng không vượt quá 100 ký tự")
    private String name;

    @NotNull(message = "Sức chứa tối đa không được để trống")
    @Min(value = 1, message = "Sức chứa phòng học phải là số nguyên dương lớn hơn hoặc bằng 1")
    private Integer capacity;

    @Size(max = 100, message = "Tên chi nhánh không vượt quá 100 ký tự")
    private String branchName;

    private Boolean isActive;

    public UpdateRoomRequest() {
    }

    public UpdateRoomRequest(String name, Integer capacity, String branchName, Boolean isActive) {
        this.name = name;
        this.capacity = capacity;
        this.branchName = branchName;
        this.isActive = isActive;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
