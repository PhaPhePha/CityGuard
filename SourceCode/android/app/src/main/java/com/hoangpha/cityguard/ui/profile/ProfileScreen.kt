package com.hoangpha.cityguard.ui.profile

// TV2 - Trần Hữu Thịnh: bố cục Row/Column và thẻ trắng theo prototype của nhóm.
// Hồ sơ/summary vẫn lấy API thật; username và role giữ trong DTO nhưng không hiển thị theo yêu cầu.
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hoangpha.cityguard.ui.auth.AuthController
import com.hoangpha.cityguard.ui.theme.*

private val ProfileCanvas = Color(0xFFFFF8D7)

@Composable
fun ProfileScreen(
    navController: NavController,
    authViewModel: AuthController,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val profile = state.profile
    val context = LocalContext.current
    var menuInfo by remember { mutableStateOf<Pair<String, String>?>(null) }
    var signingOut by remember { mutableStateOf(false) }

    // TV2: tự cập nhật khi quay về hoặc mở lại app, không cần nút Tải lại trên giao diện bình thường.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (!viewModel.state.value.loading) viewModel.load()
    }

    // TV2: vàng đậm làm nền khung; panel kem được đặt phía trước như prototype của nhóm.
    Column(Modifier.fillMaxSize().background(CityGuardYellow)) {
        // TV2: header chỉ chứa tiêu đề và thông tin; ba ô thống kê thuộc panel kem bên dưới.
        Box(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().background(CityGuardYellow).statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp)
            ) {
                Text("Hồ sơ", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = CityGuardBlack)
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(80.dp).clip(CircleShape).background(CityGuardWhite),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "Ảnh đại diện", Modifier.size(48.dp), tint = Color(0xFF6689A3))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            profile?.displayName ?: if (state.loading) "Đang tải hồ sơ…" else "Hồ sơ của bạn",
                            fontWeight = FontWeight.Bold, fontSize = 20.sp, color = CityGuardBlack,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(profile?.email.orEmpty(), fontSize = 13.sp, color = CityGuardBlack.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        // TV2: panel kem bo góc phủ nền vàng; thống kê nằm trong luồng bố cục, không offset/zIndex gây chồng lấn.
        Column(
            Modifier.fillMaxWidth().weight(1f).padding(bottom = 12.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                .background(ProfileCanvas).verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp).padding(top = 12.dp, bottom = 24.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileStatCard("Tổng báo cáo", profile?.reportCount?.toString() ?: "—", Modifier.weight(1f))
                ProfileStatCard("Đã xác nhận", profile?.confirmedReports?.toString() ?: "—", Modifier.weight(1f))
                ProfileStatCard("Bị phản hồi", "—", Modifier.weight(1f))
            }
            Spacer(Modifier.height(24.dp))

            // TV2: chỉ hiện loading/error khi cần; bỏ cụm chưa có báo cáo/chưa có số liệu/nút tải lại thường trực.
            if (state.loading && profile == null) {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(bottom = 12.dp), color = CityGuardBlack)
            }
            state.error?.let { message ->
                Surface(color = CityGuardWhite, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(message, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                        TextButton(onClick = viewModel::load) { Text("Thử lại", color = CityGuardBlack) }
                    }
                }
            }

            ProfileMenuItem("My Reports", "Xem báo cáo của tôi", Icons.AutoMirrored.Filled.ListAlt) {
                navController.navigate("my_reports") { launchSingleTop = true }
            }
            ProfileMenuItem("Xác nhận của tôi", "Các báo cáo tôi đã xác nhận", Icons.Default.CheckCircleOutline) {
                // TV2: API hiện chỉ có tổng lượt xác nhận; không bịa danh sách mà backend chưa cung cấp.
                menuInfo = "Xác nhận của tôi" to (profile?.let {
                    "Bạn đã gửi ${it.confirmedReports} lượt xác nhận. Phiên bản hiện tại chưa hỗ trợ xem danh sách báo cáo đã xác nhận."
                } ?: "Chưa tải được số lượt xác nhận. Hãy kiểm tra kết nối rồi thử lại.")
            }
            ProfileMenuItem("Cài đặt", "Thông báo, quyền riêng tư", Icons.Default.Settings) {
                // TV2: mở cài đặt ứng dụng của Android, không thêm API/cài đặt giả vào backend.
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()))
            }
            ProfileMenuItem("Trợ giúp", "Hướng dẫn sử dụng", Icons.Default.HelpOutline) {
                menuInfo = "Trợ giúp" to "• Đăng ký tài khoản, sau đó đăng nhập để sử dụng.\n• Bản đồ: xem sự cố và mở chi tiết.\n• My Reports: xem các báo cáo của tài khoản đang đăng nhập.\n• Khi tải dữ liệu lỗi: kiểm tra mạng rồi chọn Thử lại.\n• Đăng xuất: kết thúc phiên trên thiết bị."
            }
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !signingOut) {
                        if (!signingOut) {
                            signingOut = true
                            authViewModel.logout(onLogout)
                        }
                    }
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = CityGuardRed, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text(if (signingOut) "Đang đăng xuất…" else "Đăng xuất", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CityGuardRed)
            }
        }
    }
    menuInfo?.let { (title, message) ->
        AlertDialog(
            onDismissRequest = { menuInfo = null },
            title = { Text(title) }, text = { Text(message) },
            confirmButton = { TextButton(onClick = { menuInfo = null }) { Text("Đóng") } }
        )
    }
}

// TV2: Row chia đều ba thẻ, số liệu và nhãn xếp bằng Column như prototype.
@Composable
private fun ProfileStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier, 
        colors = CardDefaults.cardColors(containerColor = CityGuardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = CityGuardBlack)
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 12.sp, color = Color.Gray, maxLines = 1)
        }
    }
}

@Composable
fun ProfileMenuItem(title: String, subtitle: String, icon: ImageVector, textColor: Color = CityGuardBlack, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(bottom = 9.dp),
        shape = RoundedCornerShape(12.dp), 
        colors = CardDefaults.cardColors(containerColor = CityGuardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor)
                if (subtitle.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, fontSize = 12.sp, color = Color.Gray)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
        }
    }
}
