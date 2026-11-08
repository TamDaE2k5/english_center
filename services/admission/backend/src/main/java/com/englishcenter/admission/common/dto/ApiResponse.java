package com.englishcenter.admission.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Lớp bọc phản hồi API chuẩn toàn hệ thống (Standard API Response Wrapper).
 * Toàn bộ các API của Admission Service đều trả về định dạng này để đảm bảo tính nhất quán.
 * 
 * @param <T> Kiểu dữ liệu của phần thân payload data trả về cho client
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /** Trạng thái thành công hay thất bại của request */
    private boolean success;

    /** Dữ liệu trả về khi thành công */
    private T data;

    /** Thông điệp thông báo thân thiện cho người dùng */
    private String message;

    /** Chi tiết mã lỗi và thông báo lỗi khi thất bại */
    private ErrorDetail error;

    public ApiResponse() {
    }

    public ApiResponse(boolean success, T data, String message, ErrorDetail error) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.error = error;
    }

    /**
     * Tạo phản hồi thành công có kèm dữ liệu và thông báo
     *
     * @param data Dữ liệu kết quả nghiệp vụ
     * @param message Thông điệp thành công
     * @param <T> Kiểu dữ liệu
     * @return Đối tượng ApiResponse thành công
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, null);
    }

    /**
     * Tạo phản hồi thành công chỉ có dữ liệu
     *
     * @param data Dữ liệu kết quả nghiệp vụ
     * @param <T> Kiểu dữ liệu
     * @return Đối tượng ApiResponse thành công
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, "Thao tác thành công", null);
    }

    /**
     * Tạo phản hồi thất bại kèm mã lỗi và thông báo lỗi chi tiết
     *
     * @param error Chi tiết lỗi nghiệp vụ
     * @param <T> Kiểu dữ liệu
     * @return Đối tượng ApiResponse thất bại
     */
    public static <T> ApiResponse<T> error(ErrorDetail error) {
        return new ApiResponse<>(false, null, null, error);
    }

    /**
     * Tạo phản hồi thất bại từ chuỗi mã lỗi và thông điệp.
     *
     * @param code    Mã lỗi nghiệp vụ
     * @param message Thông điệp giải thích
     * @param <T>     Kiểu dữ liệu
     * @return Đối tượng ApiResponse thất bại
     */
    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, null, new ErrorDetail(code, message));
    }

    /**
     * Tạo phản hồi thất bại từ mã lỗi ErrorCode chuẩn.
     *
     * @param errorCode Mã lỗi Enum ErrorCode
     * @param <T>       Kiểu dữ liệu
     * @return Đối tượng ApiResponse thất bại
     */
    public static <T> ApiResponse<T> error(com.englishcenter.admission.common.exception.ErrorCode errorCode) {
        return new ApiResponse<>(false, null, null, new ErrorDetail(errorCode.name(), errorCode.getDefaultMessage()));
    }

    /**
     * Tạo phản hồi thất bại từ mã lỗi ErrorCode và thông điệp tùy biến.
     *
     * @param errorCode     Mã lỗi Enum ErrorCode
     * @param customMessage Thông điệp lỗi chi tiết
     * @param <T>           Kiểu dữ liệu
     * @return Đối tượng ApiResponse thất bại
     */
    public static <T> ApiResponse<T> error(com.englishcenter.admission.common.exception.ErrorCode errorCode, String customMessage) {
        return new ApiResponse<>(false, null, null, new ErrorDetail(errorCode.name(), customMessage));
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ErrorDetail getError() {
        return error;
    }

    public void setError(ErrorDetail error) {
        this.error = error;
    }
}
