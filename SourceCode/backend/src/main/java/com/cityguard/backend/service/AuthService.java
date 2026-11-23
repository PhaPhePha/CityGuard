package com.cityguard.backend.service;

import com.cityguard.backend.dto.request.LoginRequest;
import com.cityguard.backend.dto.request.RefreshTokenRequest;
import com.cityguard.backend.dto.request.RegisterRequest;
import com.cityguard.backend.dto.response.AuthResponse;

/**
 * Interface nghiệp vụ xử lý xác thực và quản lý phiên người dùng (theo SRS Trang 7 Mục 3.1).
 */
public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(RefreshTokenRequest request);
    void logout(RefreshTokenRequest request);
}

