package com.hoangpha.cityguard.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

data class ReportDto(
    val id: String, val type: String, val status: String, val trustScore: Int?, val trustLabel: String?,
    val confirmCount: Int, val noLongerCount: Int, val userVote: String?, val latitude: Double, val longitude: Double,
    val addressSnapshot: String, val description: String, val imageUrl: String?, val createdAt: String,
    val expiresAt: String?, val creatorId: String?, val comments: List<CommentDto> = emptyList()
)
data class CommentDto(val id: String, val authorName: String, val content: String, val createdAt: String)
data class CreateReportDto(val requestId: String, val type: String, val description: String, val addressSnapshot: String, val latitude: Double, val longitude: Double, val imageUrl: String?)
data class VoteDto(val type: String)
data class CommentRequestDto(val content: String)
data class UploadImageDto(val url: String?, val imageUrl: String?)

interface ReportApi {
    @Multipart
    @POST("api/v1/storage/upload")
    suspend fun uploadImage(@Part file: okhttp3.MultipartBody.Part): Response<ApiResponse<UploadImageDto>>
    @GET("api/v1/reports") suspend fun list(@Query("bbox") bbox: String? = null, @Query("type") type: String? = null,@Query("status") status: String? = null): Response<ApiResponse<List<ReportDto>>>
    @GET("api/v1/reports/{id}") suspend fun detail(@Path("id") id: String): Response<ApiResponse<ReportDto>>
    @GET("api/v1/me/reports") suspend fun mine(@Query("page") page: Int = 0, @Query("size") size: Int = 50): Response<ApiResponse<PageDto<ReportDto>>>
    @POST("api/v1/reports") suspend fun create(@Body request: CreateReportDto): Response<ApiResponse<ReportDto>>
    @DELETE("api/v1/reports/{id}") suspend fun delete(@Path("id") id: String): Response<ApiResponse<Unit>>
    @POST("api/v1/reports/{id}/vote") suspend fun vote(@Path("id") id: String, @Body request: VoteDto): Response<ApiResponse<ReportDto>>
    @POST("api/v1/reports/{id}/comments") suspend fun comment(@Path("id") id: String, @Body request: CommentRequestDto): Response<ApiResponse<ReportDto>>
}
