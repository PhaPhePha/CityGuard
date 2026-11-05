package com.hoangpha.cityguard.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.hoangpha.cityguard.R
import com.hoangpha.cityguard.data.local.SessionManager
import com.hoangpha.cityguard.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController, sessionManager: SessionManager) {
    LaunchedEffect(Unit) {
        delay(1500)
        val token = sessionManager.getToken()
        if (token != null) {
            //navController.navigate("main") { popUpTo("splash") { inclusive = true } }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CityGuardYellowLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp)) // ko để nội dung bị đẩy xuống thấp

            // 1. CHÈN LOGO VÀO ĐÂY (Phía trên chữ CITYGUARD)
            Image(
                painter = painterResource(id = R.drawable.logo_cityguard),
                contentDescription = "Logo",
                modifier = Modifier
                    .width(180.dp) // Chỉnh kích thước logo
                    .height(180.dp)
            )

            // 2. CHỮ CITYGUARD
            Text("CITYGUARD", fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = CityGuardBlack)
            Text("An toàn hơn, kết nối hơn", fontSize = 18.sp, color = CityGuardBlack, modifier = Modifier.padding(top = 4.dp, bottom = 32.dp))

            // 3. ẢNH NHÀ CAO TẦNG (TRÀN 2 BÊN MÀN HÌNH, TO RÕ)
            Image(
                painter = painterResource(id = R.drawable.bg_city_silhouette),
                contentDescription = "City Background",
                modifier = Modifier
                    .fillMaxWidth() // Luôn tràn 2 bên
                    .height(250.dp), // Tăng chiều cao lên 350dp cho thật to
                contentScale = ContentScale.Crop, // THAY ĐỔI QUAN TRỌNG: Ép phóng to, lấp đầy khung
                alpha = 1f, // Tăng độ đậm (rõ nét hơn)
                colorFilter = ColorFilter.tint(BuildingTint)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4. CÁC NÚT BẤM
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp) // Nút bấm thụt vào 2 bên
            ) {
                Button(
                    onClick = { navController.navigate("login") },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CityGuardBlack),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Đăng Nhập", color = CityGuardWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { navController.navigate("register") },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CityGuardWhite),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Đăng Ký", color = CityGuardBlack, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.weight(1f)) // Cân bằng khoảng trống phía dưới
        }
    }
}