package com.cityguard.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO yêu cầu đăng nhập mạng xã hội qua Firebase Authentication.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FirebaseLoginRequest {

    @NotBlank(message = "Firebase ID token không được để trống")
    private String idToken;

    @NotBlank(message = "Provider không được để trống")
    @Pattern(regexp = "^(?i)(GOOGLE|FACEBOOK)$", message = "Provider chỉ chấp nhận GOOGLE hoặc FACEBOOK")
    private String provider;
}
