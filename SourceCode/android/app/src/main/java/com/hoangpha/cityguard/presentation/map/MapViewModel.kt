package com.hoangpha.cityguard.presentation.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoangpha.cityguard.data.RepositoryProvider
import com.hoangpha.cityguard.domain.model.Report
import com.hoangpha.cityguard.domain.model.ReportSummary
import com.hoangpha.cityguard.domain.repository.ReportRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MapViewModel(private val repository: ReportRepository = RepositoryProvider.reportRepository) : ViewModel() {
    var uiState: MapUiState by mutableStateOf<MapUiState>(MapUiState.Loading)
        private set

    init {
        viewModelScope.launch {
            repository.reports.catch { uiState = MapUiState.Error("Không tải được dữ liệu bản đồ") }
                .collect { reports -> uiState = if (reports.isEmpty()) MapUiState.Empty else MapUiState.Success(reports.map(::toSummary)) }
        }
    }

    fun selectReport(reportId: String) {
        val current = uiState as? MapUiState.Success ?: return
        if (current.reports.any { it.id == reportId }) uiState = current.copy(selectedReportId = reportId)
    }

    fun clearSelectedReport() { (uiState as? MapUiState.Success)?.let { uiState = it.copy(selectedReportId = null) } }

    private fun toSummary(report: Report) = ReportSummary(report.id, report.category.displayName, report.description, report.category, report.address, report.latitude, report.longitude)
}
