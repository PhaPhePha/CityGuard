package com.hoangpha.cityguard.ui.profile

// TV2 - Trần Hữu Thịnh: đổi bố cục My Reports theo prototype; giữ API phân trang theo JWT.
// Không thêm report mẫu, không sửa nghiệp vụ tạo report/vote/comment của TV4.
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hoangpha.cityguard.data.api.ReportDto
import com.hoangpha.cityguard.ui.theme.*
import java.time.Instant

private val ReportsCanvas = Color(0xFFFFF8D7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReportsScreen(navController: NavController, viewModel: MyReportsViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // TV2: tự làm mới khi quay về từ detail/mở app lại; bỏ nút tải lại thường trực.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val current = viewModel.state.value
        if (!current.loading && !current.loadingMore) viewModel.refresh()
    }
    val tabs = listOf("Tất cả", "Đang hoạt động", "Đã hết hạn")
    val statuses = listOf(null, "ACTIVE", "EXPIRED")

    Column(Modifier.fillMaxSize().background(CityGuardYellow)) {
        TopAppBar(
            title = { Text("Báo cáo của tôi", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại hồ sơ")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = CityGuardYellow)
        )

        // TV2: panel kem có mép bo tròn đè nhẹ lên nền vàng như prototype My Reports.
        Column(
            Modifier.fillMaxWidth().weight(1f).offset(y = (-12).dp)
                // TV2: bo cả hai góc dưới của panel kem, đồng bộ với màn Hồ sơ.
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                .background(ReportsCanvas)
        ) {
            // TV2: bộ lọc nằm trong panel kem, nhưng vẫn lọc theo status trả về từ API.
            Row(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    FilterChip(
                        selected = state.status == statuses[index],
                        onClick = { viewModel.selectStatus(statuses[index]) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        label = {
                            Text(title, Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontSize = 12.sp, fontWeight = if (state.status == statuses[index]) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        colors = FilterChipDefaults.filterChipColors(containerColor = CityGuardWhite, selectedContainerColor = CityGuardWhite,
                            labelColor = Color.Gray, selectedLabelColor = CityGuardBlack),
                        border = BorderStroke(1.dp, if (state.status == statuses[index]) CityGuardDarkYellow else Color.Transparent)
                    )
                }
            }
            when {
                state.loading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CityGuardBlack)
                }
                state.reports.isEmpty() && state.error == null -> {
                    Column(
                        Modifier.fillMaxWidth().weight(1f).padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null, tint = CityGuardDarkYellow, modifier = Modifier.size(72.dp))
                        Spacer(Modifier.height(20.dp))
                        Text(when (state.status) {
                            "ACTIVE" -> "Chưa có báo cáo đang hoạt động"
                            "EXPIRED" -> "Chưa có báo cáo đã hết hạn"
                            else -> "Bạn chưa có báo cáo nào"
                        }, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = CityGuardBlack, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Text("Báo cáo của tài khoản này sẽ xuất hiện tại đây.", color = Color.Gray, fontSize = 15.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
                else -> LazyColumn(
                    Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.reports, key = { it.id }) { report ->
                        MyReportCard(report) {
                            navController.navigate("report_detail/${Uri.encode(report.id)}") { launchSingleTop = true }
                        }
                    }
                    state.error?.let { message -> item {
                        Surface(shape = RoundedCornerShape(12.dp), color = CityGuardWhite, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(message, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                                TextButton(onClick = viewModel::retry) { Text("Thử lại", color = CityGuardBlack) }
                            }
                        }
                    } }
                    if (state.loadingMore) item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = CityGuardBlack) } }
                    else if (state.hasNext && state.error == null) item {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            TextButton(onClick = viewModel::loadMore) { Text("Xem thêm báo cáo", color = CityGuardBlack) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MyReportCard(report: ReportDto, onClick: () -> Unit) {
    val incidentColor = if (report.type == "FLOOD") Color(0xFF29B6F6) else CityGuardRed
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp), color = CityGuardWhite,
        border = BorderStroke(1.dp, Color(0xFFF0F0F0))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Surface(shape = CircleShape, color = incidentColor, modifier = Modifier.size(34.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(when (report.type) { "FLOOD" -> Icons.Default.WaterDrop; "TRAFFIC_JAM" -> Icons.Default.DirectionsCar; else -> Icons.Default.Warning },
                        contentDescription = null, tint = CityGuardWhite, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(report.type.categoryLabel(), fontWeight = FontWeight.Bold, fontSize = 17.sp,
                        modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(6.dp))
                    ReportBadge(report.status.statusLabel(), if (report.status == "EXPIRED") Color(0xFFEEEEEE) else Color(0xFFE4F5DF),
                        if (report.status == "EXPIRED") Color.DarkGray else Color(0xFF388E3C))
                }
                // TV2: địa chỉ/thời gian/trạng thái/mô tả đều từ report của API, không phải dữ liệu mẫu.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, Modifier.size(13.dp), tint = Color.Gray)
                    Text(report.addressSnapshot.orEmpty().ifBlank { "Chưa có địa chỉ" }, fontSize = 13.sp, color = Color.DarkGray,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, Modifier.size(13.dp), tint = Color.Gray)
                    Text(report.createdAt.reportTimeLabel(), fontSize = 12.sp, color = Color.Gray)
                }
                if (report.description.orEmpty().isNotBlank()) Text(report.description.orEmpty(), fontSize = 13.sp, color = Color.DarkGray, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val trust = report.trustLabel
                    ReportBadge("Độ tin cậy: ${trust.trustLabel()}", if (trust == "HIGH") Color(0xFFE4F5DF) else Color(0xFFFFF0BA),
                        if (trust == "HIGH") Color(0xFF388E3C) else Color(0xFF8C6C00))
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ThumbUp, contentDescription = "Lượt xác nhận", Modifier.size(12.dp), tint = CityGuardGreen)
                    Text(" ${report.confirmCount}  ", fontSize = 11.sp, color = Color.Gray)
                    Icon(Icons.Default.ThumbDown, contentDescription = "Lượt báo không còn", Modifier.size(12.dp), tint = CityGuardRed)
                    Text(" ${report.noLongerCount}", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun ReportBadge(text: String, background: Color, foreground: Color) {
    Surface(color = background, shape = RoundedCornerShape(5.dp)) {
        Text(text, color = foreground, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp), maxLines = 1)
    }
}

private fun String.categoryLabel(): String = when (this) {
    "TRAFFIC_JAM" -> "Kẹt xe"; "FLOOD" -> "Ngập nước"; "ROAD_DAMAGE" -> "Đường hư hỏng"
    "ROAD_BLOCK" -> "Vật cản"; "FALLEN_TREE" -> "Cây đổ"; "BROKEN_TRAFFIC_LIGHT" -> "Đèn giao thông hỏng"
    else -> this
}
private fun String.statusLabel(): String = when (this) {
    "ACTIVE" -> "Đang hoạt động"; "EXPIRED" -> "Đã hết hạn"; "PENDING_VERIFY" -> "Đang xác minh"
    "HIDDEN" -> "Đã ẩn"; else -> this
}
private fun String?.trustLabel(): String = when (this) {
    "HIGH" -> "Cao"; "MEDIUM" -> "Trung bình"; "LOW" -> "Thấp"; "PENDING_VERIFY", null -> "Đang xác minh"; else -> this
}
private fun String.reportTimeLabel(): String = runCatching {
    val minutes = ((System.currentTimeMillis() - Instant.parse(this).toEpochMilli()) / 60_000).coerceAtLeast(0)
    when { minutes < 1 -> "Vừa xong"; minutes < 60 -> "$minutes phút trước"; minutes < 1_440 -> "${minutes / 60} giờ trước"; else -> "${minutes / 1_440} ngày trước" }
}.getOrDefault(this)
