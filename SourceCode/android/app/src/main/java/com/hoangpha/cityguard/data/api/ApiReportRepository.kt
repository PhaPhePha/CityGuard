package com.hoangpha.cityguard.data.api

import com.hoangpha.cityguard.domain.model.Comment
import com.hoangpha.cityguard.domain.model.IncidentCategory
import com.hoangpha.cityguard.domain.model.Report
import com.hoangpha.cityguard.domain.model.VoteType
import com.hoangpha.cityguard.domain.repository.NewReport
import com.hoangpha.cityguard.domain.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import com.hoangpha.cityguard.domain.repository.MapReportRepository
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.hoangpha.cityguard.domain.model.CommunityReportPage
class ApiReportRepository(
    private val api: ReportApi,
    private val context: Context
) : ReportRepository, MapReportRepository {
    private val supportedMapTypes = setOf(
        "TRAFFIC_JAM",
        "FLOOD",
        "ROAD_DAMAGE",
        "ROAD_BLOCK"
    )
    override suspend fun getReportsForMap(
        bbox: String?,
        type: String?,
        status: String?
    ): List<Report> {

        val response = api.list(
            bbox = bbox,
            type = type,
            status = status
        )

        if (!response.isSuccessful) {
            error("Report API error: ${response.code()}")
        }

        val body = response.body()
            ?: error("Report API returned empty body")

        if (!body.success) {
            error("Report API request failed")
        }

        val reports = body.data
            ?: error("Report API returned missing data")

        return reports
            .filter { it.type in supportedMapTypes }
            .map(::toDomain)
    }

    override val reports: Flow<List<Report>> = flow { emit(requireReports(api.list()).map(::toDomain)) }
    private suspend fun uploadImage(imageUri: String): String {
        val uri = Uri.parse(imageUri)

        val mimeType = context.contentResolver.getType(uri)
            ?: "image/jpeg"

        val fileName = context.contentResolver.query(
            uri, null, null, null, null
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                cursor.getString(nameIndex)
            } else {
                "image.jpg"
            }
        } ?: "image.jpg"

        val imageBytes = context.contentResolver.openInputStream(uri)?.use {
            it.readBytes()
        } ?: error("Không thể đọc ảnh đã chọn")

        val requestBody = imageBytes.toRequestBody(
            mimeType.toMediaTypeOrNull()
        )

        val part = MultipartBody.Part.createFormData(
            "file", fileName, requestBody
        )

        val response = api.uploadImage(part)

        if (!response.isSuccessful) {
            error("Upload ảnh thất bại: HTTP ${response.code()}")
        }

        return response.body()?.data?.imageUrl
            ?: error("Backend không trả về URL ảnh")
    }
    override fun observeReport(id: String): Flow<Report?> = flow {
        val response = api.detail(id)
        val dto = if (response.isSuccessful) response.body()?.data else null
        if (dto == null) {
            emit(null)
            return@flow
        }
        val commentsResponse = api.comments(id)
        val comments = if (commentsResponse.isSuccessful) {
            commentsResponse.body()?.data?.items.orEmpty()
        } else {
            emptyList()
        }
        emit(toDomain(dto.copy(comments = comments)))
    }

    override suspend fun createReport(newReport: NewReport): Report {
        val uploadedImageUrl = newReport.imageUri?.let { uri ->
            uploadImage(uri)
        }

        val request = CreateReportDto(
            newReport.requestId,
            newReport.category.toApiType(),
            newReport.description,
            newReport.address,
            newReport.latitude,
            newReport.longitude,
            uploadedImageUrl
        )

        return toDomain(requireReport(api.create(request)))
    }

    override suspend fun vote(reportId: String, type: VoteType) { requireReport(api.vote(reportId, VoteDto(type.name))) }
    override suspend fun addComment(reportId: String, content: String) { requireReport(api.comment(reportId, CommentRequestDto(content))) }
    override suspend fun myReports(): List<Report> = requireReports(api.mine()).map(::toDomain)
    override suspend fun getCommunityReports(
        page: Int,
        size: Int,
        recent: Boolean
    ): CommunityReportPage {
        val response = api.community(
            page = page,
            size = size,
            recent = recent
        )

        if (!response.isSuccessful) {
            error("Community API error: ${response.code()}")
        }

        val data = response.body()?.data
            ?: error("Community API returned empty data")

        return CommunityReportPage(
            items = data.items.map(::toDomain),
            page = data.page,
            size = data.size,
            totalElements = data.totalElements,
            totalPages = data.totalPages,
            hasNext = data.hasNext
        )
    }
    private fun requireReport(response: retrofit2.Response<ApiResponse<ReportDto>>): ReportDto {
        if (!response.isSuccessful) error("API error ${response.code()}")
        return response.body()?.data ?: error("API returned empty data")
    }
    private fun requireReports(response: retrofit2.Response<ApiResponse<List<ReportDto>>>): List<ReportDto> {
        if (!response.isSuccessful) error("API error ${response.code()}")
        return response.body()?.data ?: error("API returned empty data")
    }
    private fun toDomain(dto: ReportDto) = Report(
        id = dto.id, category = dto.type.toCategory(), description = dto.description, address = dto.addressSnapshot,
        createdAtLabel = dto.createdAt.toRelativeLabel(), trustLabel = dto.trustLabel.toTrustLabel(), confirmCount = dto.confirmCount,
        noLongerCount = dto.noLongerCount, myVote = dto.userVote?.let { runCatching { VoteType.valueOf(it) }.getOrNull() },
        imageUri = dto.imageUrl, comments = dto.comments.map { Comment(it.id, it.authorName, it.content, it.createdAt.toRelativeLabel()) },
        createdAtMillis = runCatching { Instant.parse(dto.createdAt).toEpochMilli() }.getOrDefault(0L), latitude = dto.latitude,
        longitude = dto.longitude, creatorId = dto.creatorId, status = dto.status
    )
    private fun String?.toTrustLabel() = when (this) { "HIGH" -> "Cao"; "MEDIUM" -> "Trung bình"; "LOW" -> "Thấp"; "PENDING_VERIFY" -> "Đang xác minh"; else -> this ?: "Đang xác minh" }
    private fun String.toCategory() = when (this) { "FLOOD" -> IncidentCategory.FLOODING; "ROAD_DAMAGE", "BROKEN_TRAFFIC_LIGHT" -> IncidentCategory.ROAD_DAMAGE; "ROAD_BLOCK", "FALLEN_TREE" -> IncidentCategory.ROAD_OBSTRUCTION; else -> IncidentCategory.TRAFFIC_JAM }
    private fun IncidentCategory.toApiType() = when (this) { IncidentCategory.FLOODING -> "FLOOD"; IncidentCategory.ROAD_DAMAGE -> "ROAD_DAMAGE"; IncidentCategory.ROAD_OBSTRUCTION -> "ROAD_BLOCK"; IncidentCategory.TRAFFIC_JAM -> "TRAFFIC_JAM" }
    private fun String.toRelativeLabel(): String = runCatching {
        val minutes = ((System.currentTimeMillis() - Instant.parse(this).toEpochMilli()) / 60_000).coerceAtLeast(0)
        when { minutes < 1 -> "Vừa xong"; minutes < 60 -> "$minutes phút trước"; else -> "${minutes / 60} giờ trước" }
    }.getOrDefault("Vừa xong")

}
