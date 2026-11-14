package com.hoangpha.cityguard.data.mock

import com.hoangpha.cityguard.domain.model.Comment
import com.hoangpha.cityguard.domain.model.IncidentCategory
import com.hoangpha.cityguard.domain.model.Report
import com.hoangpha.cityguard.domain.model.VoteType
import com.hoangpha.cityguard.domain.repository.NewReport
import com.hoangpha.cityguard.domain.repository.ReportRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID

// DỮ LIỆU GIẢ: chỉ để làm giao diện khi chưa có API. Tắt app là mất.
object FakeReportRepository : ReportRepository {

    private val state = MutableStateFlow(sampleReports())

    // Mô phỏng việc server nhớ mã requestId để không tạo trùng
    private val createdByRequestId = mutableMapOf<String, Report>()
    private var nextNumber = 7

    override val reports: Flow<List<Report>> = state.asStateFlow()

    override fun observeReport(id: String): Flow<Report?> =
        state.map { list -> list.find { it.id == id } }

    override suspend fun createReport(newReport: NewReport): Report {
        delay(1000)  // giả lập thời gian chờ mạng

        // Cùng requestId gửi lại -> trả về report đã tạo, không tạo thêm
        createdByRequestId[newReport.requestId]?.let { return it }

        val report = Report(
            id = "R" + nextNumber.toString().padStart(3, '0'),
            category = newReport.category,
            description = newReport.description,
            address = newReport.address,
            createdAtLabel = "Vừa xong",
            trustLabel = "Đang xác minh",
            confirmCount = 0,
            noLongerCount = 0,
            myVote = null,
            imageUri = newReport.imageUri,
            createdAtMillis = System.currentTimeMillis()
        )
        nextNumber++
        createdByRequestId[newReport.requestId] = report
        state.update { list -> listOf(report) + list }   // report mới lên đầu
        return report
    }

    override suspend fun vote(reportId: String, type: VoteType) {
        state.update { list ->
            list.map { r ->
                if (r.id != reportId || r.myVote == type) r   // cùng lựa chọn -> không làm gì
                else r.copy(
                    myVote = type,
                    confirmCount = r.confirmCount +
                            (if (type == VoteType.CONFIRMED) 1 else 0) -
                            (if (r.myVote == VoteType.CONFIRMED) 1 else 0),
                    noLongerCount = r.noLongerCount +
                            (if (type == VoteType.NO_LONGER) 1 else 0) -
                            (if (r.myVote == VoteType.NO_LONGER) 1 else 0)
                )
            }
        }
    }

    override suspend fun addComment(reportId: String, content: String) {
        val comment = Comment(
            id = UUID.randomUUID().toString(),
            authorName = "Bạn",
            content = content,
            createdAtLabel = "Vừa xong"
        )
        state.update { list ->
            list.map { r ->
                if (r.id == reportId) r.copy(comments = r.comments + comment) else r
            }
        }
    }

    private fun sampleReports(): List<Report> {
        val now = System.currentTimeMillis()
        val minute = 60_000L
        return listOf(
            Report(
                id = "R001", category = IncidentCategory.TRAFFIC_JAM,
                description = "Các phương tiện đang di chuyển chậm.",
                address = "Khu vực A", createdAtLabel = "5 phút trước", trustLabel = "Cao",
                confirmCount = 9, noLongerCount = 1, myVote = null,
                comments = listOf(
                    Comment(
                        "C001",
                        "Nguyễn Văn A",
                        "Đang kẹt xe tại đây, vẫn còn đông lắm.",
                        "1 phút trước"
                    )
                ),
                createdAtMillis = now - 5 * minute
            ),
            Report(
                id = "R002", category = IncidentCategory.ROAD_DAMAGE,
                description = "Mặt đường xuống cấp.",
                address = "Khu vực B", createdAtLabel = "20 phút trước", trustLabel = "Thấp",
                confirmCount = 0, noLongerCount = 2, myVote = null,
                createdAtMillis = now - 20 * minute
            ),
            Report(
                id = "R003",
                category = IncidentCategory.FLOODING,
                description = "Nước ngập gây khó khăn di chuyển.",
                address = "Khu vực C",
                createdAtLabel = "15 phút trước",
                trustLabel = "Đang xác minh",
                confirmCount = 0,
                noLongerCount = 0,
                myVote = null,
                createdAtMillis = now - 15 * minute
            ),
            Report(
                id = "R004", category = IncidentCategory.TRAFFIC_JAM,
                description = "Lưu lượng xe tăng cao.",
                address = "Khu vực D", createdAtLabel = "30 phút trước", trustLabel = "Trung bình",
                confirmCount = 4, noLongerCount = 2, myVote = null,
                createdAtMillis = now - 30 * minute
            ),
            Report(
                id = "R005", category = IncidentCategory.ROAD_DAMAGE,
                description = "Công trình sửa chữa mặt đường.",
                address = "Khu vực E", createdAtLabel = "1 giờ trước", trustLabel = "Trung bình",
                confirmCount = 2, noLongerCount = 0, myVote = null,
                createdAtMillis = now - 60 * minute
            ),
            Report(
                id = "R006", category = IncidentCategory.ROAD_OBSTRUCTION,
                description = "Cây lớn đổ gây cản trở giao thông.",
                address = "Khu vực H", createdAtLabel = "2 giờ trước", trustLabel = "Cao",
                confirmCount = 6, noLongerCount = 0, myVote = null,
                createdAtMillis = now - 120 * minute
            )
        )
    }
}