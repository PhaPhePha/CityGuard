package com.cityguard.backend.dto.admin.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUpdateUserStatusRequest {
    @NotBlank(message = "Trạng thái tài khoản không được để trống")
    @Pattern(regexp = "ACTIVE|LOCKED", message = "Trạng thái phải là ACTIVE hoặc LOCKED")
    private String status;

    @NotBlank(message = "Lý do thay đổi trạng thái không được để trống")
    @Size(max = 1000, message = "Lý do không được vượt quá 1000 ký tự")
    private String reason;
}
