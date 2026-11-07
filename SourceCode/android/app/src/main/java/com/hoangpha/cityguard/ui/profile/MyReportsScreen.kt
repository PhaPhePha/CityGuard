package com.hoangpha.cityguard.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.hoangpha.cityguard.ui.theme.CityGuardBlack
import com.hoangpha.cityguard.ui.theme.CityGuardYellow
import com.hoangpha.cityguard.utils.ReportMock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReportsScreen(navController: NavController) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Tất cả", "Đang hoạt động", "Đã hết hạn")

    val mockReports = listOf(
        ReportMock("1", "Kẹt xe", "Xô Viết Nghệ Tĩnh, Bình Thạnh", "5 phút trước", "Đang hoạt động", Color(0xFF43A047), 9, 1),
        ReportMock("2", "Ngập nước", "Lý Chính Thắng, Q.3", "2 giờ trước", "Đang xác minh", Color(0xFFF9A825), 0, 0)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Báo cáo của tôi", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = CityGuardYellow)
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = CityGuardYellow,
            contentColor = CityGuardBlack
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(mockReports) { report ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = report.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                            Surface(
                                color = report.statusColor.copy(alpha = 0.2f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = report.status,
                                    color = report.statusColor,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = report.location, fontSize = 14.sp, color = Color.Gray)
                        Text(text = report.time, fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ThumbUp, contentDescription = "Confirm", tint = Color(0xFF43A047), modifier = Modifier.size(16.dp))
                            Text(text = " ${report.confirmCount}  ", fontSize = 12.sp)
                            Icon(Icons.Default.ThumbDown, contentDescription = "No longer", tint = Color(0xFFE53935), modifier = Modifier.size(16.dp))
                            Text(text = " ${report.noLongerCount}", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}