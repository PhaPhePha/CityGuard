package com.cityguard.backend.dto.response;

public record AuthResponse(String token, String userId, String username) {}
