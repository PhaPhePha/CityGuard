package com.cityguard.app.presentation.map

import com.cityguard.app.domain.model.ReportSummary

sealed interface MapUiState {

    object Loading : MapUiState

    data class Success(
        val reports: List<ReportSummary>
    ) : MapUiState

    object Empty : MapUiState

    data class Error(
        val message: String
    ) : MapUiState
}