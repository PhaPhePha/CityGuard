package com.cityguard.backend.dto.common;

import com.cityguard.backend.enums.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Định dạng phản hồi lỗi chuẩn (theo DacTaKyThuat Trang 9, Mục 6.3).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    @Builder.Default
    private boolean success = false;
    private String errorCode;
    private String message;
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

