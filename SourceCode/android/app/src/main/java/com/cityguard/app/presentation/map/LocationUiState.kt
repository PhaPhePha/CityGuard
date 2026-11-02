package com.cityguard.app.presentation.map

import com.cityguard.app.domain.location.LocationResult

sealed interface LocationUiState {
    data object Idle : LocationUiState
    data object Loading : LocationUiState

    data class Finished(
        val result: LocationResult,
        val requestId: Int
    ) : LocationUiState
}
