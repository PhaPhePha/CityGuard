package com.hoangpha.cityguard.domain.repository

import com.hoangpha.cityguard.domain.model.IncidentCategory
import com.hoangpha.cityguard.domain.model.Report
import com.hoangpha.cityguard.domain.model.VoteType
import kotlinx.coroutines.flow.Flow

// Dữ liệu người dùng nhập khi tạo report (chưa có id, thời gian, số vote)
data class NewReport(
    val requestId: String,          // mã chống gửi trùng: tạo một lần khi mở form, thử lại thì gửi lại đúng mã này
    val category: IncidentCategory,
    val description: String,
    val address: String,
    val imageUri: String?
)

interface ReportRepository {
    // Danh sách report; tự cập nhật khi dữ liệu đổi
    val reports: Flow<List<Report>>

    // Một report theo id; null nghĩa là không tìm thấy
    fun observeReport(id: String): Flow<Report?>

    suspend fun createReport(newReport: NewReport): Report
    suspend fun vote(reportId: String, type: VoteType)
    suspend fun addComment(reportId: String, content: String)
}