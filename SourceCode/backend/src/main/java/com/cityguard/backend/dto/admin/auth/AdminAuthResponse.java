package com.cityguard.backend.dto.admin.auth;

import com.cityguard.backend.dto.auth.AuthResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO phản hồi đăng nhập dành cho React Admin Web (theo ADMIN_API_CONTRACT.md mục 3.1 & auth.types.ts).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAuthResponse {

    private String token;

    @Builder.Default
    private String tokenType = "Bearer";

    private AdminUserDto user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminUserDto {
        private Long id;
        private String username;
        private String email;
        private String displayName;
        private String role;
        private String status;
    }

    public static AdminAuthResponse from(AuthResponse authResponse) {
        if (authResponse == null) {
            return null;
        }
        AuthResponse.UserInfo userInfo = authResponse.getUserInfo();
        AdminUserDto adminUser = userInfo == null ? null : AdminUserDto.builder()
                .id(userInfo.getId())
                .username(userInfo.getUsername())
                .email(userInfo.getEmail())
                .displayName(userInfo.getDisplayName())
                .role(userInfo.getRole())
                .status("ACTIVE")
                .build();

        return AdminAuthResponse.builder()
                .token(authResponse.getAccessToken())
                .tokenType(authResponse.getTokenType() != null ? authResponse.getTokenType() : "Bearer")
                .user(adminUser)
                .build();
    }
}
