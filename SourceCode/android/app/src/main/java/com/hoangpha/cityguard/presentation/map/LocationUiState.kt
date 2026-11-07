package com.hoangpha.cityguard.presentation.map

import com.hoangpha.cityguard.domain.location.LocationResult

sealed interface LocationUiState {
    data object Idle : LocationUiState
    data object Loading : LocationUiState

    data class Finished(
        val result: LocationResult,
        val requestId: Int
    ) : LocationUiState
}
