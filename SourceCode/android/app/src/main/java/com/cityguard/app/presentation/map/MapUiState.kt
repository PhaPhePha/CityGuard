package com.cityguard.app.presentation.map

import com.cityguard.app.domain.model.ReportSummary

sealed interface MapUiState {

    object Loading : MapUiState

    data class Success(
        val reports: List<ReportSummary>,
        val selectedReportId: String? = null
    ) : MapUiState {
        val selectedReport: ReportSummary?
            get() = reports.firstOrNull{
                it.id == selectedReportId
            }
    }

    object Empty : MapUiState

    data class Error(
        val message: String
    ) : MapUiState
}