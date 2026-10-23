package com.cityguard.app.presentation.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cityguard.app.data.mock.FakeReportData
import com.cityguard.app.domain.model.IncidentCategory
import com.cityguard.app.presentation.map.components.*
import kotlinx.coroutines.launch
import com.cityguard.app.presentation.map.components.ReportSummaryCard
/** M1: UI mock. Navigation thật thuộc Main Navigation của nhóm. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    uiState: MapUiState,
    modifier: Modifier = Modifier,
    onOpenProfile: (() -> Unit)? = null,
    onOpenReportDetails: ((String) -> Unit)? = null
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedReportId by rememberSaveable { mutableStateOf<String?>(null) }
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val reports = (uiState as? MapUiState.Success)?.reports.orEmpty()
    val selectedReport = reports.firstOrNull { it.id == selectedReportId }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .background(MapColors.Sand)
        ) {
            MapHeader(
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                onProfileClick = {
                    if (onOpenProfile != null) onOpenProfile()
                    else scope.launch { snackbarHostState.showSnackbar("Hồ sơ: sẽ tích hợp Navigation của nhóm") }
                }
            )

            Box(Modifier.fillMaxSize()) {
                when (uiState) {
                    MapUiState.Loading -> Box(
                        Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                    MapUiState.Empty -> CenterMessage("Chưa có sự cố nào")
                    is MapUiState.Error -> CenterMessage(uiState.message)
                    is MapUiState.Success -> {
                        if (reports.isEmpty()) CenterMessage("Chưa có sự cố nào")
                        else FakeMap(
                            reports = reports,
                            selectedReportId = selectedReportId,
                            onMarkerClick = { reportId ->
                                selectedReportId = reportId
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Nút lọc và GPS chỉ mở UI / thông báo ở milestone này.
                FilledTonalButton(
                    onClick = { showFilterSheet = true },
                    modifier = Modifier.align(Alignment.TopEnd)
                        .padding(top = 10.dp, end = 12.dp).testTag("filter_open"),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White, contentColor = MapColors.Dark
                    )
                ) { Text("☷ Lọc") }

                Column(
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    FilledTonalButton(
                        onClick = { scope.launch { snackbarHostState.showSnackbar("GPS đang phát triển (M2)") } },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White, contentColor = MapColors.Blue
                        ),
                        modifier = Modifier.testTag("gps_placeholder")
                    ) { Text("◎ Vị trí (demo)") }

                    if (selectedReport != null) {
                        Spacer(Modifier.height(6.dp))
                        ReportSummaryCard(
                            report = selectedReport,
                            onOpenDetails = {
                                if (onOpenReportDetails != null) onOpenReportDetails(selectedReport.id)
                                else scope.launch {
                                    snackbarHostState.showSnackbar("Chi tiết báo cáo đang phát triển")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(onDismissRequest = { showFilterSheet = false }) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Bộ lọc sự cố", style = MaterialTheme.typography.titleLarge)
                Text("M1: Chỉ hiển thị các loại sự cố, chưa áp dụng lọc marker.")
                IncidentCategory.entries.forEach { category ->
                    Text("• ${category.displayName}")
                }
                Button(onClick = { showFilterSheet = false }) { Text("Đóng") }
            }
        }
    }
}

@Composable
private fun CenterMessage(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}

@Preview(name = "Home Map - M1", showBackground = true, widthDp = 390, heightDp = 780)
@Composable
private fun HomeMapFigmaPreview() {
    MaterialTheme {
        Column(Modifier.fillMaxSize()) {
            MapScreen(
                uiState = MapUiState.Success(FakeReportData.reports),
                modifier = Modifier.weight(1f)
            )
            // Demo riêng cho Preview, KHÔNG thay thế bottom navigation thật.
            Row(
                Modifier.fillMaxWidth().height(68.dp).background(MapColors.BottomYellow),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("▥\nBản đồ", "⊕\nTạo báo cáo", "♟\nCộng đồng", "♙\nHồ sơ").forEach { label ->
                    Text(label, fontWeight = FontWeight.SemiBold, color = MapColors.Dark)
                }
            }
        }
    }
}

@Preview(name = "Loading", showBackground = true)
@Composable
private fun LoadingPreview() { MaterialTheme { MapScreen(MapUiState.Loading) } }

@Preview(name = "Empty", showBackground = true)
@Composable
private fun EmptyPreview() { MaterialTheme { MapScreen(MapUiState.Empty) } }

@Preview(name = "Error", showBackground = true)
@Composable
private fun ErrorPreview() { MaterialTheme { MapScreen(MapUiState.Error("Không thể tải dữ liệu")) } }
