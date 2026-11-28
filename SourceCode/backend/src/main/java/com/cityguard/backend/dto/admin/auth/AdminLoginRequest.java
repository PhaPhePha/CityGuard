package com.cityguard.backend.dto.admin.auth;

import com.cityguard.backend.dto.auth.LoginRequest;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO yêu cầu đăng nhập dành cho Admin Web (theo ADMIN_API_CONTRACT.md mục 3.1).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginRequest {

    @JsonAlias("account")
    @NotBlank(message = "Tài khoản (username hoặc email) không được để trống")
    private String identifier;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;

    public LoginRequest toLoginRequest() {
        return LoginRequest.builder()
                .account(this.identifier)
                .password(this.password)
                .build();
    }
}
