package com.cityguard.backend.exception;

import com.cityguard.backend.enums.ErrorCode;
import lombok.Getter;

/**
 * Ngoại lệ ứng dụng mang mã lỗi ErrorCode theo chuẩn hệ thống.
 */
@Getter
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public AppException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
