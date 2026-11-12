package com.hoangpha.cityguard.presentation.community

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.hoangpha.cityguard.domain.model.IncidentCategory
import com.hoangpha.cityguard.domain.model.Report
import com.hoangpha.cityguard.ui.theme.CityGuardAppTheme

// "Route": nối ViewModel với màn hình
@Composable
fun CommunityRoute(
    onReportClick: (String) -> Unit,      // truyền ID report được bấm
    viewModel: CommunityViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CommunityScreen(
        state = state,
        onReportClick = onReportClick,
        onRetry = viewModel::load
    )
}

// "Screen": chỉ vẽ theo state
@Composable
fun CommunityScreen(
    state: CommunityUiState,
    onReportClick: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)
    ) {
        Text("Cộng đồng", style = MaterialTheme.typography.headlineSmall)
        Text("(DỮ LIỆU MẪU)", style = MaterialTheme.typography.labelSmall)   // phân biệt dữ liệu mẫu

        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            state.errorMessage != null -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
                    Button(onClick = onRetry) { Text("Thử lại") }
                }
            }

            state.reports.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Chưa có báo cáo nào")
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.reports, key = { it.id }) { report ->
                        ReportCard(report = report, onClick = { onReportClick(report.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun ReportCard(report: Report, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            report.imageUri?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = "Ảnh sự cố",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 140.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Text(report.category.displayName, style = MaterialTheme.typography.titleMedium)
            Text(report.address)
            Text(report.createdAtLabel, style = MaterialTheme.typography.bodySmall)
            Text("Độ tin cậy: ${report.trustLabel}")
            Text("👍 ${report.confirmCount}   👎 ${report.noLongerCount}")
        }
    }
}

// ----- Xem thử trong Android Studio -----
private val sampleReports = listOf(
    Report("R001", IncidentCategory.TRAFFIC_JAM, "Các phương tiện di chuyển chậm.",
        "Khu vực A", "5 phút trước", "Cao", 9, 1, null),
    Report("R003", IncidentCategory.FLOODING, "Nước ngập gây khó khăn di chuyển.",
        "Khu vực C", "15 phút trước", "Đang xác minh", 0, 0, null)
)

@Preview(showBackground = true, widthDp = 360, heightDp = 780, name = "Có dữ liệu")
@Composable
private fun CommunityContentPreview() {
    CityGuardAppTheme {
        CommunityScreen(CommunityUiState(isLoading = false, reports = sampleReports), {}, {})
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780, name = "Đang tải")
@Composable
private fun CommunityLoadingPreview() {
    CityGuardAppTheme {
        CommunityScreen(CommunityUiState(isLoading = true), {}, {})
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780, name = "Rỗng")
@Composable
private fun CommunityEmptyPreview() {
    CityGuardAppTheme {
        CommunityScreen(CommunityUiState(isLoading = false), {}, {})
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780, name = "Lỗi")
@Composable
private fun CommunityErrorPreview() {
    CityGuardAppTheme {
        CommunityScreen(
            CommunityUiState(isLoading = false, errorMessage = "Không tải được danh sách. Vui lòng thử lại."),
            {}, {}
        )
    }
}