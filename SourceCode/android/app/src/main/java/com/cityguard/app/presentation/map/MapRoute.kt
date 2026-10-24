package com.cityguard.app.presentation.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MapRoute(
    modifier: Modifier = Modifier,
    onOpenProfile: (() -> Unit)? = null,
    onOpenReportDetails: ((String) -> Unit)? = null,
    mapViewModel: MapViewModel = viewModel()
) {
    MapScreen(
        uiState = mapViewModel.uiState,
        modifier = modifier,
        onOpenProfile = onOpenProfile,
        onOpenReportDetails = onOpenReportDetails,
        onSelectReport = mapViewModel::selectReport
    )
}