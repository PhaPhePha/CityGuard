package com.cityguard.backend.dto.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO yêu cầu đăng nhập (hỗ trợ nhập username hoặc email).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Tài khoản (username hoặc email) không được để trống")
    @JsonAlias({"username", "email", "login"})
    private String account;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
}

