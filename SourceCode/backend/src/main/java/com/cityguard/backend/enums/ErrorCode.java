package com.cityguard.backend.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Danh mục mã lỗi hệ thống (theo DacTaKyThuat Trang 9, Mục 6.3).
 */
@Getter
public enum ErrorCode {
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "Dữ liệu đầu vào không hợp lệ"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Yêu cầu đăng nhập hoặc token không hợp lệ"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này"),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy báo cáo sự cố"),
    RESOURCE_CONFLICT(HttpStatus.CONFLICT, "Dữ liệu bị xung đột"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống, vui lòng thử lại sau");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
