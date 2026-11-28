
package com.hoangpha.cityguard.presentation.community

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.hoangpha.cityguard.domain.model.IncidentCategory
import com.hoangpha.cityguard.domain.model.Report
import com.hoangpha.cityguard.presentation.map.components.markerIconRes
import com.hoangpha.cityguard.ui.theme.CityGuardAppTheme
import com.hoangpha.cityguard.ui.theme.CityGuardDarkYellow
import com.hoangpha.cityguard.ui.theme.CityGuardGreen
import com.hoangpha.cityguard.ui.theme.CityGuardRed
import com.hoangpha.cityguard.ui.theme.CityGuardYellow

// Kết nối ViewModel với giao diện Community
@Composable
fun CommunityRoute(
    onReportClick: (String) -> Unit,
    viewModel: CommunityViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    CommunityScreen(
        state = state,
        onReportClick = onReportClick,
        onTabSelected = viewModel::onTabSelected,
        onRetry = viewModel::load,
        onLoadMore = viewModel::loadNextPage
    )
}

// Màu nền của phần nội dung
private val SheetBackground = Color(0xFFFFF8E1)

// Giao diện Community
@Composable
fun CommunityScreen(
    state: CommunityUiState,
    onReportClick: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onTabSelected: (CommunityTab) -> Unit = {},
    onLoadMore: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CityGuardYellow)
    ) {
        // Phần đầu nền vàng
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(
                    start = 20.dp,
                    top = 16.dp,
                    bottom = 16.dp
                )
        ) {
            Text(
                text = "Cộng đồng",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // Phần nội dung nền kem
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    SheetBackground,
                    RoundedCornerShape(
                        topStart = 24.dp,
                        topEnd = 24.dp
                    )
                )
                .padding(horizontal = 12.dp)
        ) {
            // Hai tab Gần đây và Mới nhất
            CommunityTabs(
                selected = state.selectedTab.ordinal,
                onSelect = {
                    onTabSelected(CommunityTab.entries[it])
                }
            )

            val visibleReports = state.visibleReports()
            val listState = rememberLazyListState()

            // Theo dõi vị trí cuộn để tự động tải trang tiếp theo
            LaunchedEffect(
                listState,
                visibleReports.size,
                state.hasNextPage,
                state.isLoading,
                state.isLoadingMore
            ) {
                snapshotFlow {
                    listState.layoutInfo
                        .visibleItemsInfo
                        .lastOrNull()
                        ?.index
                }.collect { lastVisibleIndex ->

                    if (
                        visibleReports.isNotEmpty() &&
                        lastVisibleIndex != null &&
                        lastVisibleIndex >= visibleReports.size - 3 &&
                        state.hasNextPage &&
                        !state.isLoading &&
                        !state.isLoadingMore
                    ) {
                        onLoadMore()
                    }
                }
            }

            // Xử lý các trạng thái của danh sách
            when {
                // Đang tải trang đầu tiên
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                // Có lỗi khi tải dữ liệu
                state.errorMessage != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.errorMessage,
                            color = MaterialTheme.colorScheme.error
                        )

                        Button(onClick = onRetry) {
                            Text("Thử lại")
                        }
                    }
                }

                // Không có báo cáo
                visibleReports.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Chưa có báo cáo nào")
                    }
                }

                // Có dữ liệu
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 4.dp),
                        contentPadding = PaddingValues(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Hiển thị danh sách Report Card
                        items(
                            items = visibleReports,
                            key = { it.id }
                        ) { report ->
                            ReportCard(
                                report = report,
                                onClick = {
                                    onReportClick(report.id)
                                }
                            )
                        }

                        // Hiển thị vòng xoay khi tải trang tiếp theo
                        if (state.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Hai tab của Community
@Composable
private fun CommunityTabs(
    selected: Int,
    onSelect: (Int) -> Unit
) {
    val titles = listOf("Gần đây", "Mới nhất")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        titles.forEachIndexed { index, title ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        onSelect(index)
                    }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontWeight = if (selected == index) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    },
                    fontSize = 14.sp
                )

                Spacer(Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(2.dp)
                        .background(
                            if (selected == index) {
                                CityGuardDarkYellow
                            } else {
                                Color.Transparent
                            }
                        )
                )
            }
        }
    }
}

// Thẻ hiển thị thông tin báo cáo
@Composable
fun ReportCard(
    report: Report,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Cột trái: loại sự cố, vị trí, thời gian và độ tin cậy
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(
                            id = report.category.markerIconRes()
                        ),
                        contentDescription = report.category.displayName,
                        modifier = Modifier.size(32.dp)
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = report.category.displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "📍 ${report.address}",
                    fontSize = 12.sp,
                    maxLines = 2
                )

                Text(
                    text = "🕒 ${report.createdAtLabel}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                Spacer(Modifier.height(6.dp))

                val (badgeBackground, badgeText) =
                    trustColors(report.trustLabel)

                Text(
                    text = "Độ tin cậy: ${report.trustLabel}",
                    modifier = Modifier
                        .background(
                            badgeBackground,
                            RoundedCornerShape(5.dp)
                        )
                        .padding(
                            horizontal = 6.dp,
                            vertical = 3.dp
                        ),
                    color = badgeText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Cột phải: ảnh và số lượt vote
            Column(
                horizontalAlignment = Alignment.End
            ) {
                val uri = report.imageUri

                if (uri != null) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Ảnh sự cố",
                        modifier = Modifier
                            .size(
                                width = 92.dp,
                                height = 104.dp
                            )
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(
                                width = 92.dp,
                                height = 104.dp
                            )
                            .background(
                                Color(0xFFE8E2D7),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chưa có ảnh",
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "👍 ${report.confirmCount}   👎 ${report.noLongerCount}",
                    fontSize = 12.sp
                )
            }
        }
    }
}

// Màu huy hiệu độ tin cậy
private fun trustColors(label: String): Pair<Color, Color> =
    when (label) {
        "Cao" -> Pair(
            Color(0xFFD6F3D8),
            CityGuardGreen
        )

        "Thấp" -> Pair(
            Color(0xFFFFD9D9),
            CityGuardRed
        )

        else -> Pair(
            Color(0xFFFFF3C4),
            CityGuardDarkYellow
        )
    }

// Dữ liệu mẫu chỉ phục vụ Android Studio Preview
private val sampleReports = listOf(
    Report(
        "R001",
        IncidentCategory.TRAFFIC_JAM,
        "Các phương tiện di chuyển chậm.",
        "Khu vực A",
        "5 phút trước",
        "Cao",
        9,
        1,
        null
    ),
    Report(
        "R003",
        IncidentCategory.FLOODING,
        "Nước ngập gây khó khăn di chuyển.",
        "Khu vực C",
        "15 phút trước",
        "Đang xác minh",
        0,
        0,
        null
    )
)

// Preview: Có dữ liệu
@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 780,
    name = "Có dữ liệu"
)
@Composable
private fun CommunityContentPreview() {
    CityGuardAppTheme {
        CommunityScreen(
            state = CommunityUiState(
                isLoading = false,
                reports = sampleReports
            ),
            onReportClick = {},
            onRetry = {}
        )
    }
}

// Preview: Đang tải
@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 780,
    name = "Đang tải"
)
@Composable
private fun CommunityLoadingPreview() {
    CityGuardAppTheme {
        CommunityScreen(
            state = CommunityUiState(isLoading = true),
            onReportClick = {},
            onRetry = {}
        )
    }
}

// Preview: Danh sách rỗng
@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 780,
    name = "Rỗng"
)
@Composable
private fun CommunityEmptyPreview() {
    CityGuardAppTheme {
        CommunityScreen(
            state = CommunityUiState(isLoading = false),
            onReportClick = {},
            onRetry = {}
        )
    }
}

// Preview: Có lỗi
@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 780,
    name = "Lỗi"
)
@Composable
private fun CommunityErrorPreview() {
    CityGuardAppTheme {
        CommunityScreen(
            state = CommunityUiState(
                isLoading = false,
                errorMessage = "Không tải được danh sách. Vui lòng thử lại."
            ),
            onReportClick = {},
            onRetry = {}
        )
    }
}
