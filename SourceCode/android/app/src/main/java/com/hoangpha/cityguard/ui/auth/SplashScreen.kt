package com.hoangpha.cityguard.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Brush
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CityGuardYellowLight)
    ) {
        Image(
            painter = painterResource(R.drawable.bg_cityguard_auth),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomCenter
        )
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    0f to CityGuardAuthYellow.copy(alpha = 0.72f),
                    0.42f to CityGuardAuthYellow.copy(alpha = 0.45f),
                    1f to CityGuardAuthYellow.copy(alpha = 0.08f)
                )
            )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp)) // ko để nội dung bị đẩy xuống thấp

            // 1. CHÈN LOGO
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

            // Giữ vị trí cụm nút; ảnh thành phố giờ nằm ở lớp nền toàn màn hình.
            Spacer(Modifier.height(250.dp))

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
