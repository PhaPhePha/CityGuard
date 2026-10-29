package com.cityguard.app.presentation.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.cityguard.app.data.mock.FakeReportData
import com.cityguard.app.domain.model.ReportSummary

class MapViewModel : ViewModel() {

    var uiState: MapUiState by mutableStateOf(
        createInitialState()
    )
        private set

    fun selectReport(reportId: String) {
        val currentState =
            uiState as? MapUiState.Success ?: return

        val reportExists = currentState.reports.any { report ->
            report.id == reportId
        }

        if (!reportExists) {
            return
        }

        uiState = currentState.copy(
            selectedReportId = reportId
        )
    }

    fun clearSelectedReport() {
        val currentState =
            uiState as? MapUiState.Success ?: return

        uiState = currentState.copy(
            selectedReportId = null
        )
    }

    private fun createInitialState(): MapUiState {
        // Nguồn dữ liệu tạm thời của M1.
        // Khi tích hợp API, thay bằng repository.
        val reports: List<ReportSummary> =
            FakeReportData.reports

        return if (reports.isEmpty()) {
            MapUiState.Empty
        } else {
            MapUiState.Success(reports = reports)
        }
    }
}