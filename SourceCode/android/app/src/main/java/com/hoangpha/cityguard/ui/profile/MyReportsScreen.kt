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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hoangpha.cityguard.domain.model.Report

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReportsScreen(navController: NavController, viewModel: MyReportsViewModel = viewModel()) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Tất cả", "Đang hoạt động", "Đã hết hạn")

    val reports by viewModel.reports.collectAsStateWithLifecycle()
    val visibleReports = reports.filter { selectedTab == 0 || (selectedTab == 1 && it.status == "ACTIVE") || (selectedTab == 2 && it.status == "EXPIRED") }

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
            items(visibleReports) { report ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = report.category.displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                            Surface(
                                color = Color(0xFFD6F3D8),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = report.trustLabel,
                                    color = Color(0xFF43A047),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = report.address, fontSize = 14.sp, color = Color.Gray)
                        Text(text = report.createdAtLabel, fontSize = 12.sp, color = Color.Gray)
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
