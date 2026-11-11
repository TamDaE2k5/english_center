package com.englishcenter.admission.dto.room;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO yêu cầu tạo mới phòng học.
 */
public class CreateRoomRequest {

    @NotBlank(message = "Mã phòng không được để trống")
    @Size(max = 50, message = "Mã phòng không vượt quá 50 ký tự")
    private String code;

    @NotBlank(message = "Tên phòng không được để trống")
    @Size(max = 100, message = "Tên phòng không vượt quá 100 ký tự")
    private String name;

    @NotNull(message = "Sức chứa tối đa không được để trống")
    @Min(value = 1, message = "Sức chứa phòng học phải là số nguyên dương lớn hơn hoặc bằng 1")
    private Integer capacity;

    @Size(max = 100, message = "Tên chi nhánh không vượt quá 100 ký tự")
    private String branchName = "Cơ sở chính";

    public CreateRoomRequest() {
    }

    public CreateRoomRequest(String code, String name, Integer capacity, String branchName) {
        this.code = code;
        this.name = name;
        this.capacity = capacity;
        this.branchName = branchName != null ? branchName : "Cơ sở chính";
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
}
