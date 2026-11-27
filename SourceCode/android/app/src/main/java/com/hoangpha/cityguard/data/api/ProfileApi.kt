package com.hoangpha.cityguard.data.api
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

data class ProfileSummaryDto(
    val id: Long, val username: String, val email: String, val displayName: String,
    val avatarUrl: String?, val role: String, val reportCount: Long, val confirmedReports: Long
)
data class PageDto<T>(
    val items: List<T>, val page: Int, val size: Int,
    val totalElements: Long, val totalPages: Int, val hasNext: Boolean
)
interface ProfileApi {
    // [TV2-08] /me xác định user từ JWT phía server, không nhận userId do Android tự khai.
    // Summary phẳng: username/email/displayName/avatarUrl/role/reportCount/confirmedReports.
    @GET("api/v1/me/summary")
    suspend fun summary(): Response<ApiResponse<ProfileSummaryDto>>
    @GET("api/v1/me/reports")
    suspend fun reports(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("status") status: String? = null
    ): Response<ApiResponse<PageDto<ReportDto>>>
}
