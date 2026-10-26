package com.hoangpha.cityguard.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.hoangpha.cityguard.ui.auth.AuthViewModel
import com.hoangpha.cityguard.ui.theme.CityGuardBlack
import com.hoangpha.cityguard.ui.theme.CityGuardWhite
import com.hoangpha.cityguard.ui.theme.CityGuardYellow

@Composable
fun ProfileScreen(navController: NavController, authViewModel: AuthViewModel) {
    Column(modifier = Modifier.fillMaxSize().background(CityGuardWhite)) {
        // Header màu vàng
        Box(modifier = Modifier.fillMaxWidth().background(CityGuardYellow).padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(CityGuardWhite), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = "Avatar", tint = CityGuardBlack, modifier = Modifier.size(40.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = "Nguyễn Văn A", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = CityGuardBlack)
                    Text(text = "nguyenvana@gmail.com", fontSize = 14.sp, color = CityGuardBlack.copy(alpha = 0.7f))
                }
            }
        }

        // Stats Row
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatItem("Tổng báo cáo", "2")
            StatItem("Đã xác nhận", "11")
            StatItem("Bị phản hồi", "4")
        }

        Divider(thickness = 1.dp, color = Color.LightGray)

        ProfileMenuItem("My Reports", "Xem báo cáo của tôi", Icons.Default.List) {
            navController.navigate("my_reports")
        }
        ProfileMenuItem("Xác nhận của tôi", "Các báo cáo tôi đã xác nhận", Icons.Default.CheckCircle) { }
        ProfileMenuItem("Cài đặt", "Thông báo, quyền riêng tư", Icons.Default.Settings) { }
        ProfileMenuItem("Trợ giúp", "Hướng dẫn sử dụng", Icons.Default.Help) { }
        ProfileMenuItem("Đăng xuất", "", Icons.Default.ExitToApp, textColor = Color.Red) {
            authViewModel.logout()
            navController.navigate("login") { popUpTo(0) { inclusive = true } }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = CityGuardBlack)
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun ProfileMenuItem(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, textColor: Color = CityGuardBlack, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = textColor)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, color = textColor, fontWeight = FontWeight.Medium)
            if (subtitle.isNotEmpty()) {
                Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
            }
        }
        Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = "Arrow", tint = Color.Gray)
    }
    Divider(thickness = 0.5.dp, color = Color.LightGray, modifier = Modifier.padding(start = 16.dp))
}