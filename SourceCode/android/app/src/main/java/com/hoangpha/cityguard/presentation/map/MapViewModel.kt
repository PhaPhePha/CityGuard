package com.hoangpha.cityguard.presentation.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoangpha.cityguard.data.RepositoryProvider
import com.hoangpha.cityguard.domain.model.Report
import com.hoangpha.cityguard.domain.model.ReportSummary
import com.hoangpha.cityguard.domain.repository.MapReportRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.io.IOException
import com.hoangpha.cityguard.domain.model.MapBounds
import kotlinx.coroutines.ensureActive
import android.util.Log
import kotlinx.coroutines.delay
class MapViewModel(
    private val repository: MapReportRepository =
        RepositoryProvider.mapReportRepository
) : ViewModel() {

    private val logTag = "CityGuardMap"
    var uiState: MapUiState by mutableStateOf<MapUiState>(
        MapUiState.Loading
    )
        private set

    private var loadJob: Job? = null
    private var activeQuery: MapReportQuery? = null
    private var latestRequestId: Long = 0L
// Method này nhận Bounding Box từ Google Maps, lưu lại
// vùng truy vấn và gọi Repository. Nếu có một request cũ đang chạy khi vùng khởi tạo thay đổi,
// nó sẽ yêu cầu hủy request đó.

    fun onViewportReady(bounds: MapBounds) {

        if (currentBounds == bounds) return

        currentBounds = bounds

        loadReports()
    }
    private var currentBounds: MapBounds? = null
    var searchQuery by mutableStateOf("")
        private set
    // Khai báo trạng thái bộ lọc
    var filters by mutableStateOf(MapFilters())
        private set

    fun applyFilters(newFilters: MapFilters) {

        if (filters == newFilters) return

        filters = newFilters

        uiState = MapUiState.Loading

        loadReports()
    }
    private fun loadReports() {

        val bounds = currentBounds ?: return

        val query = MapReportQuery(
            bounds = bounds,
            filters = filters
        )

        // Không gửi request trùng đang thực thi.
        if (loadJob?.isActive == true &&
            activeQuery == query
        ) {
            return
        }

        // Đánh dấu request mới nhất.
        val requestId = ++latestRequestId

        // Hủy request trước đó.
        loadJob?.cancel()

        activeQuery = query

        loadJob = viewModelScope.launch {

            val previousState =
                uiState as? MapUiState.Success

            uiState = previousState?.copy(
                isRefreshing = true
            ) ?: MapUiState.Loading
            Log.d(logTag, "START requestId=$requestId")
            try {

                val reports = repository.getReportsForMap(
                    bbox = query.bounds.toApiQuery(),
                    type = query.filters.toApiType(),
                    status = query.filters.toApiStatus()
                )

                // Kiểm tra Coroutine đã bị hủy chưa.
                ensureActive()

                Log.d(
                    logTag,
                    "SUCCESS requestId=$requestId, count=${reports.size}"
                )
                // Không cho request cũ cập nhật UI.

                if (requestId != latestRequestId) {
                    return@launch
                }

                val summaries = reports.map(::toSummary)

                val selectedId =
                    (uiState as? MapUiState.Success)
                        ?.selectedReportId

                uiState = if (summaries.isEmpty()) {

                    MapUiState.Empty

                } else {

                    MapUiState.Success(
                        reports = summaries,

                        selectedReportId = selectedId
                            ?.takeIf { id ->
                                summaries.any { it.id == id }
                            },

                        isRefreshing = false
                    )
                }

            } catch (e: CancellationException) {

                Log.d(
                    logTag,
                    "CANCELLED requestId=$requestId"
                )
                throw e

            } catch (e: IOException) {

                ensureActive()

                if (requestId == latestRequestId) {

                    uiState = MapUiState.Error(
                        "Không thể kết nối máy chủ. " +
                                "Vui lòng kiểm tra kết nối và thử lại."
                    )
                }

            } catch (e: Exception) {

                ensureActive()

                if (requestId == latestRequestId) {

                    uiState = MapUiState.Error(
                        "Không tải được dữ liệu bản đồ. " +
                                "Vui lòng thử lại."
                    )
                }
            }
        }
    }
    fun retry() {

        if (loadJob?.isActive == true){
            Log.d(
                logTag,
                "SKIPPED retry: request already running"
            )
            return
        }
        if (uiState is MapUiState.Error ||
            uiState == MapUiState.Empty
        ) {
            loadReports()
        }
    }

    fun selectReport(reportId: String) {
        val current = uiState as? MapUiState.Success
            ?: return

        if (current.reports.any { it.id == reportId }) {
            uiState = current.copy(
                selectedReportId = reportId
            )
        }
    }

    fun clearSelectedReport() {
        val current = uiState as? MapUiState.Success
            ?: return

        uiState = current.copy(
            selectedReportId = null
        )
    }

    private fun toSummary(report: Report): ReportSummary {
        return ReportSummary(
            id = report.id,
            title = report.category.displayName,
            description = report.description,
            category = report.category,
            address = report.address,
            latitude = report.latitude,
            longitude = report.longitude,
            createdAtLabel = report.createdAtLabel
                .takeIf { report.createdAtMillis > 0L },
            trustLabel = report.trustLabel,
            imageUri = report.imageUri
        )
    }
}