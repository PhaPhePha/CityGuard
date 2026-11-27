package com.hoangpha.cityguard.data.api

// TV2 - Trần Hữu Thịnh: Tích hợp TV2 vào repository chung: sửa myReports theo data.items/hasNext và phân biệt 404 với lỗi tải detail.

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

class ApiReportRepository(private val api: ReportApi) : ReportRepository,MapReportRepository {
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

    override fun observeReport(id: String): Flow<Report?> = flow {
        val response = api.detail(id)
        // TV2 tích hợp TV4: chỉ HTTP 404 mới là không tìm thấy; lỗi mạng/5xx đi vào trạng thái lỗi.
        emit(if (response.code() == 404) null else toDomain(requireApiData(response)))
    }

    override suspend fun createReport(newReport: NewReport): Report = toDomain(requireReport(api.create(CreateReportDto(
        newReport.requestId, newReport.category.toApiType(), newReport.description, newReport.address,
        newReport.latitude, newReport.longitude, newReport.imageUri
    ))))

    override suspend fun vote(reportId: String, type: VoteType) { requireReport(api.vote(reportId, VoteDto(type.name))) }
    override suspend fun addComment(reportId: String, content: String) { requireReport(api.comment(reportId, CommentRequestDto(content))) }
    // TV2 tích hợp: giữ interface chung của TV4, nhưng đọc đúng endpoint phân trang và gom các trang cho caller cũ.
    override suspend fun myReports(): List<Report> {
        val result = mutableListOf<Report>()
        var page = 0
        do {
            val data = requireApiData(api.mine(page = page++))
            result.addAll(data.items.map(::toDomain))
        } while (data.hasNext)
        return result.distinctBy { it.id }
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
    private fun String?.toTrustLabel() = when (this) { "HIGH" -> "Cao"; "MEDIUM" -> "Trung bình"; "PENDING_VERIFY" -> "Đang xác minh"; else -> this ?: "Đang xác minh" }
    private fun String.toCategory() = when (this) { "FLOOD" -> IncidentCategory.FLOODING; "ROAD_DAMAGE", "BROKEN_TRAFFIC_LIGHT" -> IncidentCategory.ROAD_DAMAGE; "ROAD_BLOCK", "FALLEN_TREE" -> IncidentCategory.ROAD_OBSTRUCTION; else -> IncidentCategory.TRAFFIC_JAM }
    private fun IncidentCategory.toApiType() = when (this) { IncidentCategory.FLOODING -> "FLOOD"; IncidentCategory.ROAD_DAMAGE -> "ROAD_DAMAGE"; IncidentCategory.ROAD_OBSTRUCTION -> "ROAD_BLOCK"; IncidentCategory.TRAFFIC_JAM -> "TRAFFIC_JAM" }
    private fun String.toRelativeLabel(): String = runCatching {
        val minutes = ((System.currentTimeMillis() - Instant.parse(this).toEpochMilli()) / 60_000).coerceAtLeast(0)
        when { minutes < 1 -> "Vừa xong"; minutes < 60 -> "$minutes phút trước"; else -> "${minutes / 60} giờ trước" }
    }.getOrDefault("Vừa xong")

}
