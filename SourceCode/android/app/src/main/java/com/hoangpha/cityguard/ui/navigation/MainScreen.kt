package com.hoangpha.cityguard.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hoangpha.cityguard.presentation.map.MapRoute
import com.hoangpha.cityguard.ui.auth.AuthController
import com.hoangpha.cityguard.ui.profile.ProfileScreen
import com.hoangpha.cityguard.ui.profile.MyReportsScreen
import com.hoangpha.cityguard.ui.theme.CityGuardBlack
import com.hoangpha.cityguard.ui.theme.CityGuardWhite
import com.hoangpha.cityguard.presentation.report.CreateReportRoute
import com.hoangpha.cityguard.ui.theme.CityGuardYellow
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.hoangpha.cityguard.presentation.report.ReportDetailRoute
sealed class BottomNavItem(var title: String, var icon: androidx.compose.ui.graphics.vector.ImageVector, var route: String) {
    object Map : BottomNavItem("Bản đồ", Icons.Default.Map, "map")
    object CreateReport : BottomNavItem("Tạo báo cáo", Icons.Default.AddCircle, "create_report")
    object Community : BottomNavItem("Cộng đồng", Icons.Default.People, "community")
    object Profile : BottomNavItem("Hồ sơ", Icons.Default.Person, "profile")
}

@Composable
fun MainScreen(authViewModel: AuthController, onLogout: () -> Unit) {
    val navController = rememberNavController()
    val items = listOf(BottomNavItem.Map, BottomNavItem.CreateReport, BottomNavItem.Community, BottomNavItem.Profile)

    // Sử dụng Box thay vì Scaffold để không hiện nút 3 gạch
    Box(modifier = Modifier.fillMaxSize()) {
        // Nội dung các màn hình
        NavHost(
            navController = navController,
            startDestination = "map",
            modifier = Modifier.padding(bottom = 80.dp)
        ) {
            composable("map") {
                MapRoute(modifier = Modifier.testTag("map_route"))
            }
            composable("create_report") {
                CreateReportRoute(
                    onCreated = { reportId ->
                        navController.navigate("report_detail/$reportId") {
                            popUpTo("create_report") { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "report_detail/{reportId}",
                arguments = listOf(navArgument("reportId") { type = NavType.StringType })
            ) {
                ReportDetailRoute(onBack = { navController.popBackStack() })
            }
            composable("community") { Text("Cộng đồng chưa triển khai") }
            composable("profile") { ProfileScreen(navController, authViewModel, onLogout) }
            composable("my_reports") { MyReportsScreen(navController) }
        }

        // Thanh điều hướng dưới cùng
        NavigationBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            containerColor = CityGuardWhite
        ) {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            items.forEach { item ->
                NavigationBarItem(
                    icon = { Icon(item.icon, contentDescription = item.title) },
                    label = { Text(item.title) },
                    selected = currentRoute == item.route,
                    onClick = {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CityGuardBlack,
                        selectedTextColor = CityGuardBlack,
                        indicatorColor = CityGuardYellow
                    )
                )
            }
        }
    }
}
