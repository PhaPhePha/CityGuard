package com.hoangpha.cityguard.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hoangpha.cityguard.ui.theme.CityGuardBlack
import com.hoangpha.cityguard.ui.theme.CityGuardWhite
import com.hoangpha.cityguard.ui.theme.CityGuardYellow

@Composable
fun RegisterScreen(navController: NavController, authViewModel: AuthViewModel = viewModel()) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }

    val authState by authViewModel.authState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Success -> {
                navController.navigate("main") { popUpTo("register") { inclusive = true } }
                authViewModel.resetState()
            }
            is AuthState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                authViewModel.resetState()
            }
            else -> {}
        }
    }

    // Dùng Box thay vì Scaffold để bỏ thanh menu mặc định
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CityGuardYellow)
            .padding(24.dp)
    ) {
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo / Tiêu đề
            Text("CITYGUARD", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = CityGuardBlack)
            Text("An toàn, kết nối", fontSize = 16.sp, color = CityGuardBlack, modifier = Modifier.padding(bottom = 24.dp))

            // Ô nhập Tên đăng nhập
            TextField(
                value = username,
                onValueChange = { username = it },
                placeholder = { Text("Nhập tên đăng nhập", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = CityGuardWhite,
                    unfocusedContainerColor = CityGuardWhite,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Ô nhập Email
            TextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("Nhập email", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = CityGuardWhite,
                    unfocusedContainerColor = CityGuardWhite,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Ô nhập Mật khẩu
            TextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("Nhập mật khẩu", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = CityGuardWhite,
                    unfocusedContainerColor = CityGuardWhite,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Ô nhập Xác nhận mật khẩu
            TextField(
                value = confirmPass,
                onValueChange = { confirmPass = it },
                placeholder = { Text("Nhập lại mật khẩu", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = CityGuardWhite,
                    unfocusedContainerColor = CityGuardWhite,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Nút Đăng ký
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(color = CityGuardBlack)
            } else {
                Button(
                    onClick = { authViewModel.register(username, email, password, confirmPass) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CityGuardBlack),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("ĐĂNG KÝ", color = CityGuardWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chuyển về Đăng nhập
            Text(
                text = "Đã có tài khoản? Đăng nhập",
                modifier = Modifier.clickable { navController.popBackStack() },
                color = CityGuardBlack,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}