package com.cityguard.backend.service;

import com.cityguard.backend.dto.auth.AuthResponse;
import com.cityguard.backend.dto.auth.LoginRequest;

public interface AdminAuthService {
    AuthResponse adminLogin(LoginRequest request);
}
