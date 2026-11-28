package com.cityguard.backend.service;

import com.cityguard.backend.dto.auth.AuthResponse;
import com.cityguard.backend.dto.auth.FirebaseLoginRequest;
import com.cityguard.backend.dto.auth.FirebaseLoginRequest;
import com.cityguard.backend.dto.auth.LoginRequest;
import com.cityguard.backend.dto.auth.RefreshTokenRequest;
import com.cityguard.backend.dto.auth.RegisterRequest;

/**
 * Interface nghiệp vụ xử lý xác thực và quản lý phiên người dùng (theo SRS Trang 7 Mục 3.1).
 */
public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse loginWithFirebase(FirebaseLoginRequest request);
    AuthResponse refreshToken(RefreshTokenRequest request);
    void logout(RefreshTokenRequest request);
}

