package com.cityguard.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO yêu cầu đăng ký tài khoản (khớp kiểm tra ràng buộc Schema V3).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Pattern(
            regexp = "^[A-Za-z0-9_]{3,50}$",
            message = "Tên đăng nhập từ 3 đến 50 ký tự, chỉ chứa chữ cái, chữ số và dấu gạch dưới"
    )
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Định dạng email không hợp lệ")
    @Size(max = 254, message = "Email không được vượt quá 254 ký tự")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, max = 100, message = "Mật khẩu phải có độ dài từ 6 đến 100 ký tự")
    private String password;

    @NotBlank(message = "Tên hiển thị không được để trống")
    @Size(min = 1, max = 100, message = "Tên hiển thị từ 1 đến 100 ký tự")
    private String displayName;
}

