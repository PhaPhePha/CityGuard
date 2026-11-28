package com.cityguard.backend.controller;

import com.cityguard.backend.dto.admin.auth.AdminAuthResponse;
import com.cityguard.backend.dto.admin.auth.AdminLoginRequest;
import com.cityguard.backend.dto.auth.AuthResponse;
import com.cityguard.backend.dto.common.ApiResponse;
import com.cityguard.backend.service.AdminAuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {
    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AdminAuthResponse>> login(
            @Valid @RequestBody AdminLoginRequest request){
        AuthResponse authResponse = adminAuthService.adminLogin(request.toLoginRequest());
        AdminAuthResponse response = AdminAuthResponse.from(authResponse);

        return ResponseEntity.ok(
                ApiResponse.success("Đăng nhập thành công", response)
        );
    }
}