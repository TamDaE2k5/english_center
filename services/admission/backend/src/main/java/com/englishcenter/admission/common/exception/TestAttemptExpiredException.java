package com.englishcenter.admission.common.exception;

/** Báo hết hạn cho client và cho phép transaction ghi nhận trạng thái EXPIRED. */
public class TestAttemptExpiredException extends BusinessException {
    public TestAttemptExpiredException() {
        super(ErrorCode.ADM_ATTEMPT_EXPIRED);
    }
}
