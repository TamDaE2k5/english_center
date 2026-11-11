package com.englishcenter.admission.common.dto;

/**
 * Đối tượng biểu diễn chi tiết lỗi trả về cho client khi request thất bại.
 * Bắt buộc gồm mã lỗi nghiệp vụ chuẩn (code) và thông điệp giải thích thân thiện (message).
 */
public class ErrorDetail {

    /** Mã lỗi nghiệp vụ chuẩn (AUTH_..., ADM_...) */
    private String code;

    /** Thông điệp giải thích nguyên nhân lỗi */
    private String message;

    public ErrorDetail() {
    }

    public ErrorDetail(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
