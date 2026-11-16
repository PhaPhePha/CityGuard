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

class ApiReportRepository(private val api: ReportApi) : ReportRepository {
    override val reports: Flow<List<Report>> = flow { emit(requireReports(api.list()).map(::toDomain)) }

    override fun observeReport(id: String): Flow<Report?> = flow {
        val response = api.detail(id)
        emit(if (response.isSuccessful) response.body()?.data?.let(::toDomain) else null)
    }

    override suspend fun createReport(newReport: NewReport): Report = toDomain(requireReport(api.create(CreateReportDto(
        newReport.requestId, newReport.category.toApiType(), newReport.description, newReport.address,
        newReport.latitude, newReport.longitude, newReport.imageUri
    ))))

    override suspend fun vote(reportId: String, type: VoteType) { requireReport(api.vote(reportId, VoteDto(type.name))) }
    override suspend fun addComment(reportId: String, content: String) { requireReport(api.comment(reportId, CommentRequestDto(content))) }
    override suspend fun myReports(): List<Report> = requireReports(api.mine()).map(::toDomain)

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
    private fun String?.toTrustLabel() = when (this) { "HIGH" -> "Cao"; "MEDIUM" -> "Trung bình"; "PENDING_VERIFY" -> "Đang xác minh"; else -> this ?: "Đang xác minh" }
    private fun String.toCategory() = when (this) { "FLOOD" -> IncidentCategory.FLOODING; "ROAD_DAMAGE", "BROKEN_TRAFFIC_LIGHT" -> IncidentCategory.ROAD_DAMAGE; "ROAD_BLOCK", "FALLEN_TREE" -> IncidentCategory.ROAD_OBSTRUCTION; else -> IncidentCategory.TRAFFIC_JAM }
    private fun IncidentCategory.toApiType() = when (this) { IncidentCategory.FLOODING -> "FLOOD"; IncidentCategory.ROAD_DAMAGE -> "ROAD_DAMAGE"; IncidentCategory.ROAD_OBSTRUCTION -> "ROAD_BLOCK"; IncidentCategory.TRAFFIC_JAM -> "TRAFFIC_JAM" }
    private fun String.toRelativeLabel(): String = runCatching {
        val minutes = ((System.currentTimeMillis() - Instant.parse(this).toEpochMilli()) / 60_000).coerceAtLeast(0)
        when { minutes < 1 -> "Vừa xong"; minutes < 60 -> "$minutes phút trước"; else -> "${minutes / 60} giờ trước" }
    }.getOrDefault("Vừa xong")
}
