package com.englishcenter.admission.common.exception;

import com.englishcenter.admission.common.dto.ApiResponse;
import com.englishcenter.admission.common.dto.ErrorDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Bộ xử lý ngoại lệ toàn cục tập trung (Global Exception Handler).
 * Đảm bảo 100% phản hồi lỗi đều được bọc trong ApiResponse chuẩn,
 * không bao giờ làm lộ Java StackTrace 500 ra phía client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Bắt các ngoại lệ nghiệp vụ do Service chủ động ném ra
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("Lỗi nghiệp vụ phát sinh: [{}] - {}", ex.getErrorCode().name(), ex.getMessage());
        ErrorDetail errorDetail = new ErrorDetail(ex.getErrorCode().name(), ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getHttpStatus()).body(ApiResponse.error(errorDetail));
    }

    /**
     * Bắt lỗi kiểm tra tính hợp lệ dữ liệu đầu vào (@Valid DTO)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("Dữ liệu đầu vào không hợp lệ: {}", errorMessage);
        ErrorDetail errorDetail = new ErrorDetail(ErrorCode.ADM_VALIDATION_ERROR.name(), errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(errorDetail));
    }

    /**
     * Bắt lỗi định dạng JSON payload không đọc được (malformed JSON)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMalformedJsonException(HttpMessageNotReadableException ex) {
        log.warn("Định dạng dữ liệu JSON không hợp lệ");
        ErrorDetail errorDetail = new ErrorDetail(ErrorCode.ADM_VALIDATION_ERROR.name(), "Định dạng JSON gửi lên không hợp lệ");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(errorDetail));
    }

    /**
     * Bắt lỗi không đủ thẩm quyền truy cập tài nguyên (403 Forbidden)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Truy cập bị từ chối: {}", ex.getMessage());
        ErrorDetail errorDetail = new ErrorDetail(ErrorCode.AUTH_FORBIDDEN.name(), ErrorCode.AUTH_FORBIDDEN.getDefaultMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(errorDetail));
    }

    /**
     * Bắt tất cả các ngoại lệ hệ thống không mong muốn còn lại (500 Internal Server Error)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        log.error("Lỗi hệ thống nghiêm trọng chưa được phân loại:", ex);
        ErrorDetail errorDetail = new ErrorDetail(ErrorCode.INTERNAL_SERVER_ERROR.name(), ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(errorDetail));
    }
}
