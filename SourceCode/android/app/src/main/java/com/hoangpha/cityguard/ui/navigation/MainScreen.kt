package com.hoangpha.cityguard.ui.navigation

// TV2 - Trần Hữu Thịnh: Giữ route các module; Hồ sơ/My Reports dùng thanh tab theo prototype, Detail dùng toàn màn hình.

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
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
import com.hoangpha.cityguard.presentation.community.CommunityRoute
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

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    // TV2: My Reports là màn con của Hồ sơ; giữ thanh tab như prototype, không đổi route các module khác.
    val showBottomBar = currentRoute in items.map { it.route } || currentRoute == "my_reports"
    // TV2: chỉ đổi đáy vùng nội dung Profile/My Reports; thanh điều hướng vẫn trắng như các màn khác.
    // TV2: chừa cả vùng điều hướng hệ thống cho hai màn này, tránh thẻ/nút nằm dưới thanh tab.
    val bottomPadding = when {
        !showBottomBar -> 0.dp
        currentRoute == "profile" || currentRoute == "my_reports" -> 80.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        else -> 80.dp
    }
    // Giữ thanh tab ở màn chính; màn con dùng toàn bộ chiều cao.
    Box(modifier = Modifier.fillMaxSize()) {
        // Nội dung các màn hình
        NavHost(
            navController = navController,
            startDestination = "map",
            modifier = Modifier.padding(bottom = bottomPadding)
        ) {
            composable("map") {
                MapRoute(
                    modifier = Modifier.testTag("map_route"),
                    onOpenProfile = { navController.navigate("profile") },
                    onOpenReportDetails = { id -> navController.navigate("report_detail/$id") }
                )
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
            composable("community") {
                CommunityRoute(
                    onReportClick = { reportId ->
                        navController.navigate("report_detail/$reportId")
                    }
                )
            }
            composable("profile") { ProfileScreen(navController, authViewModel, onLogout) }
            composable("my_reports") { MyReportsScreen(navController) }
        }

        // Thanh điều hướng dưới cùng
        if (showBottomBar) NavigationBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            // TV2: thanh dưới trắng theo yêu cầu đồng bộ; nội dung hai màn vẫn giữ nền vàng/kem riêng.
            containerColor = CityGuardWhite
        ) {
            items.forEach { item ->
                NavigationBarItem(
                    icon = { Icon(item.icon, contentDescription = item.title) },
                    label = { Text(item.title) },
                    selected = currentRoute == item.route || (currentRoute == "my_reports" && item.route == "profile"),
                    onClick = {
                        // TV2: đang ở My Reports mà bấm Hồ sơ thì trở về hồ sơ, không restore lại chính màn con.
                        if (currentRoute == "my_reports" && item.route == "profile") {
                            navController.popBackStack("profile", inclusive = false)
                        } else {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
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
