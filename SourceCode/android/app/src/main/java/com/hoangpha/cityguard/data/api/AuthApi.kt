package com.hoangpha.cityguard.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

// Dữ liệu gửi lên
data class LoginRequest(val username: String, val password: String)
data class RegisterRequest(val username: String, val email: String, val password: String)

// Dữ liệu trả về
data class AuthResponse(val token: String, val userId: String, val message: String)

interface AuthApi {
    @POST("/api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("/api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>
}