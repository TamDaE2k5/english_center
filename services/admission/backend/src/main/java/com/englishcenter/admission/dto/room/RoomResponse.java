package com.englishcenter.admission.dto.room;

import com.englishcenter.admission.entity.Room;

import java.time.LocalDateTime;

/**
 * DTO phản hồi thông tin chi tiết phòng học.
 * Tuân thủ quy ước snake_case khi tuần tự hóa sang JSON.
 */
public class RoomResponse {

    private Integer id;
    private String code;
    private String name;
    private Integer capacity;
    private String branchName;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RoomResponse() {
    }

    public RoomResponse(Integer id, String code, String name, Integer capacity, String branchName,
                        Boolean isActive, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.capacity = capacity;
        this.branchName = branchName;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Chuyển đổi từ Room Entity sang RoomResponse DTO.
     *
     * @param room Thực thể Room
     * @return DTO RoomResponse
     */
    public static RoomResponse fromEntity(Room room) {
        if (room == null) {
            return null;
        }
        return new RoomResponse(
                room.getId(),
                room.getCode(),
                room.getName(),
                room.getCapacity(),
                room.getBranchName(),
                room.getIsActive(),
                room.getCreatedAt(),
                room.getUpdatedAt()
        );
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
