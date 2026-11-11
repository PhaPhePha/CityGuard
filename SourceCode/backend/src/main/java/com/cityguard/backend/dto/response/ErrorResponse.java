package com.cityguard.backend.dto.response;

import java.time.Instant;

import com.cityguard.backend.enums.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Định dạng phản hồi lỗi chuẩn (theo DacTaKyThuat Trang 9, Mục 6.3):
 * { boolean success, String errorCode, String message, Instant timestamp }.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    @Builder.Default
    private boolean success = false; // Luôn là false
    private String errorCode;        // Mã lỗi (VD: REPORT_NOT_FOUND)
    private String message;          // Thông điệp rõ ràng cho client
    private Instant timestamp;

    public static ErrorResponse of(ErrorCode errorCode) {
        return ErrorResponse.builder()
                .success(false)
                .errorCode(errorCode.name())
                .message(errorCode.getMessage())
                .timestamp(Instant.now())
                .build();
    }

    public static ErrorResponse of(ErrorCode errorCode, String customMessage) {
        return ErrorResponse.builder()
                .success(false)
                .errorCode(errorCode.name())
                .message(customMessage != null ? customMessage : errorCode.getMessage())
                .timestamp(Instant.now())
                .build();
    }
}
