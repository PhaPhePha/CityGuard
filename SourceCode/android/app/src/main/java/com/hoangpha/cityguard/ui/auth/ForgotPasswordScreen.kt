package com.hoangpha.cityguard.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.hoangpha.cityguard.R
import com.hoangpha.cityguard.ui.theme.*

@Composable
fun ForgotPasswordScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CityGuardYellowLight)
    ) {
        Image(
            painter = painterResource(id = R.drawable.bg_city_silhouette),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(400.dp)
                .offset(y = 80.dp),
            contentScale = ContentScale.Crop,
            alpha = 0.5f,
            colorFilter = ColorFilter.tint(BuildingTint)
        )

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Image(
                painter = painterResource(id = R.drawable.logo_cityguard),
                contentDescription = "Logo",
                modifier = Modifier.width(150.dp).height(150.dp)
            )

            Text("Khôi Phục Mật Khẩu", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = CityGuardBlack)
            Text("Nhập email và mật khẩu mới của bạn", fontSize = 14.sp, color = CityGuardBlack, modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CityGuardWhite.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                // Email xác thực
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                    Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(16.dp), tint = CityGuardBlack)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Email xác thực", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CityGuardBlack)
                }
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    placeholder = { Text("Nhập email của bạn", color = Color.Gray, fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CityGuardWhite, unfocusedContainerColor = CityGuardWhite,
                        focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Mật khẩu mới
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                    Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp), tint = CityGuardBlack)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mật khẩu mới", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CityGuardBlack)
                }
                OutlinedTextField(
                    value = newPassword, onValueChange = { newPassword = it },
                    placeholder = { Text("Nhập mật khẩu mới", color = Color.Gray, fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CityGuardWhite, unfocusedContainerColor = CityGuardWhite,
                        focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent
                    ),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = icon, contentDescription = "Hiện/Ẩn mật khẩu", tint = Color.Gray)
                        }
                    },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Nhập lại mật khẩu mới
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                    Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp), tint = CityGuardBlack)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Nhập lại mật khẩu mới", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CityGuardBlack)
                }
                OutlinedTextField(
                    value = confirmNewPassword, onValueChange = { confirmNewPassword = it },
                    placeholder = { Text("Nhập lại mật khẩu mới", color = Color.Gray, fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CityGuardWhite, unfocusedContainerColor = CityGuardWhite,
                        focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent
                    ),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Nút Xác nhận đổi mật khẩu
            Button(
                onClick = {
                    // Xử lý logic đổi mật khẩu ở đây
                    // Ví dụ: if(email.isBlank() || newPassword != confirmNewPassword) { ... }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CityGuardBlack),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("XÁC NHẬN ĐỔI MẬT KHẨU", color = CityGuardWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Quay lại Đăng nhập",
                modifier = Modifier.clickable { navController.popBackStack() },
                color = CityGuardBlack, fontSize = 14.sp, fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}