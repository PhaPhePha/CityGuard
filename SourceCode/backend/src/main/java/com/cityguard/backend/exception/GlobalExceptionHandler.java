package com.cityguard.backend.exception;

import com.cityguard.backend.dto.response.ErrorResponse;
import com.cityguard.backend.enums.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Xử lý ngoại lệ tập trung toàn ứng dụng (theo DacTaKyThuat Trang 9, Mục 6.3).
 * Đảm bảo che giấu hoàn toàn stack trace khỏi response client.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Bắt ngoại lệ nghiệp vụ AppException được ném có chủ đích.
     */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        log.warn("Lỗi nghiệp vụ [{}]: {}", errorCode.name(), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(errorCode, ex.getMessage());
        return ResponseEntity.status(errorCode.getStatus()).body(errorResponse);
    }

    /**
     * Bắt toàn bộ ngoại lệ chưa được phân loại khác (Fallback 500).
     * Tuyệt đối không hiển thị thông tin nhạy cảm hoặc stack trace ra bên ngoài.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUncaughtException(Exception ex) {
        log.error("Lỗi hệ thống ngoài dự kiến: ", ex);

        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);
        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus()).body(errorResponse);
    }
}
