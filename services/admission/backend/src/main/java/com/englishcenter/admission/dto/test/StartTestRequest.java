package com.englishcenter.admission.dto.test;

/**
 * DTO yêu cầu bắt đầu lượt thi khảo thí trực tuyến (FR-TST-03, INV-TST-02).
 */
public class StartTestRequest {

    private Integer userId;
    private String email;
    private String fullName;
    private String phone;

    public StartTestRequest() {
    }

    public StartTestRequest(Integer userId, String email, String fullName, String phone) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
