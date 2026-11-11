package com.englishcenter.admission.entity;

import jakarta.persistence.*;

/**
 * Thực thể biểu diễn phòng học trong phân hệ Admission (bảng admission.rooms).
 * Chứa thông tin mã phòng, sức chứa, chi nhánh cơ sở.
 */
@Entity
@Table(name = "rooms", schema = "admission")
public class Room extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "branch_name", length = 100)
    private String branchName = "Cơ sở chính";

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public Room() {
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
}
