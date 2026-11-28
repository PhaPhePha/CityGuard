package com.hoangpha.cityguard.domain.repository

import com.hoangpha.cityguard.domain.model.IncidentCategory
import com.hoangpha.cityguard.domain.model.Report
import com.hoangpha.cityguard.domain.model.VoteType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import com.hoangpha.cityguard.domain.model.CommunityReportPage
// Dữ liệu người dùng nhập khi tạo report (chưa có id, thời gian, số vote)
data class NewReport(
    val requestId: String,          // mã chống gửi trùng: tạo một lần khi mở form, thử lại thì gửi lại đúng mã này
    val category: IncidentCategory,
    val description: String,
    val address: String,
    val imageUri: String?,
    // Tọa độ thật lấy từ thiết bị; mặc định chỉ để fake repository/test cũ còn biên dịch.
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

interface ReportRepository {
    // Danh sách report; tự cập nhật khi dữ liệu đổi
    val reports: Flow<List<Report>>

    // Một report theo id; null nghĩa là không tìm thấy
    fun observeReport(id: String): Flow<Report?>

    suspend fun createReport(newReport: NewReport): Report
    suspend fun vote(reportId: String, type: VoteType)
    suspend fun addComment(reportId: String, content: String)
    suspend fun getCommunityReports(
        page: Int,
        size: Int,
        recent: Boolean
    ): CommunityReportPage
    suspend fun myReports(): List<Report> = reports.first()
}
