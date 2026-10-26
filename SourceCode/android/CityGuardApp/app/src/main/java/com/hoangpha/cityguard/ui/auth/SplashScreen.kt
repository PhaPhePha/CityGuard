package com.hoangpha.cityguard.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.hoangpha.cityguard.R
import com.hoangpha.cityguard.data.local.SessionManager
import com.hoangpha.cityguard.ui.theme.CityGuardBlack
import com.hoangpha.cityguard.ui.theme.CityGuardWhite
import com.hoangpha.cityguard.ui.theme.CityGuardYellow
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController, sessionManager: SessionManager) {
    // Kiểm tra token sau 1.5 giây (giả lập thời gian loading)
    LaunchedEffect(Unit) {
        delay(1500)
        val token = sessionManager.getToken()
        if (token != null) {
            // Nếu đã đăng nhập, vào thẳng màn hình chính
            navController.navigate("main") { popUpTo("splash") { inclusive = true } }
        }
        // Nếu chưa đăng nhập, giữ nguyên ở màn hình Splash để user chọn Đăng nhập/Đăng ký
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CityGuardYellow)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Phần Logo và Slogan
            // (Bạn có thể thay Text bằng Image nếu có file ảnh logo)
            Text("CITYGUARD", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = CityGuardBlack)
            Text("An toàn, kết nối", fontSize = 16.sp, color = CityGuardBlack, modifier = Modifier.padding(bottom = 48.dp))

            // Nút Đăng Nhập (Màu đen)
            Button(
                onClick = { navController.navigate("login") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CityGuardBlack),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Đăng Nhập", color = CityGuardWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nút Đăng Ký (Màu trắng)
            Button(
                onClick = { navController.navigate("register") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CityGuardWhite),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Đăng Ký", color = CityGuardBlack, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}