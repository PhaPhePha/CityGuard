package com.hoangpha.cityguard.data.api
// provider chỉ GOOGLE/FACEBOOK; response giữ ApiResponse<AuthResponse>, expiresIn tính bằng ms.
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class LoginRequest(val account: String, val password: String)
enum class FirebaseProvider { GOOGLE, FACEBOOK }
data class FirebaseLoginRequest(val idToken: String, val provider: FirebaseProvider) {
    override fun toString() = "FirebaseLoginRequest(idToken=[REDACTED], provider=$provider)"
}
data class RegisterRequest(val username: String, val email: String, val password: String, val displayName: String)
data class RefreshTokenRequest(val refreshToken: String)
data class AuthResponse(
    val accessToken: String, val refreshToken: String, val tokenType: String,
    val expiresIn: Long, val userInfo: UserInfo
)
data class UserInfo(val id: Long, val username: String, val email: String, val displayName: String, val role: String)
data class ApiResponse<T>(val success: Boolean, val message: String?, val data: T?)

interface AuthApi {
    @POST("api/v1/auth/firebase")
    suspend fun loginFirebase(@Body request: FirebaseLoginRequest): Response<ApiResponse<AuthResponse>>
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<AuthResponse>>
    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<AuthResponse>>
    @POST("api/v1/auth/logout")
    suspend fun logout(@Body request: RefreshTokenRequest): Response<ApiResponse<Unit>>
}
