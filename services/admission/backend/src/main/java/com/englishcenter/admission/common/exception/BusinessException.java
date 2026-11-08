package com.englishcenter.admission.common.exception;

/**
 * Ngoại lệ nghiệp vụ cơ sở của phân hệ Admission & Identity Service.
 * Được ném ra từ tầng Service khi xảy ra vi phạm các quy tắc nghiệp vụ hoặc dữ liệu không hợp lệ.
 */
public class BusinessException extends RuntimeException {

    /** Mã lỗi nghiệp vụ chuẩn định danh */
    private final ErrorCode errorCode;

    /**
     * Khởi tạo ngoại lệ với mã lỗi chuẩn và thông điệp mặc định của mã lỗi
     *
     * @param errorCode Mã lỗi nghiệp vụ
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    /**
     * Khởi tạo ngoại lệ với mã lỗi chuẩn và thông điệp tùy chỉnh chi tiết
     *
     * @param errorCode Mã lỗi nghiệp vụ
     * @param customMessage Thông điệp mô tả lỗi chi tiết
     */
    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
