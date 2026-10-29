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
import androidx.compose.material.icons.filled.Person
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hoangpha.cityguard.R
import com.hoangpha.cityguard.ui.theme.*

@Composable
fun RegisterScreen(navController: NavController, authViewModel: AuthViewModel = viewModel()) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val authState by authViewModel.authState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Success -> {
                // chuyển về đăng nhập thì chuyển main thành login
                //navController.navigate("main") { popUpTo("register") { inclusive = true } }
                snackbarHostState.showSnackbar("Đăng ký thành công!")
                authViewModel.resetState()
            }
            is AuthState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                authViewModel.resetState()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CityGuardYellowLight)
    ) {
        // ẢNH NỀN NHÀ RÕ HƠN
        Image(
            painter = painterResource(id = R.drawable.bg_city_silhouette),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(450.dp)
                //.padding(bottom = 60.dp),
                .offset(y = 80.dp),
            contentScale = ContentScale.Crop,
            alpha = 0.6f, // Tăng độ rõ nét
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

            // LOGO
            Image(
                painter = painterResource(id = R.drawable.logo_cityguard),
                contentDescription = "Logo",
                modifier = Modifier
                    .width(130.dp)
                    .height(130.dp)
            )

            // CHỮ
            Text("CITYGUARD", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = CityGuardBlack)
            Text("An toàn, kết nối", fontSize = 18.sp, color = CityGuardBlack, modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))

            // KHUNG BỌC CÁC Ô NHẬP LIỆU
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CityGuardWhite.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                // Tên đăng nhập
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                    Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = CityGuardBlack)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tên đăng nhập", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CityGuardBlack)
                }
                OutlinedTextField(
                    value = username, onValueChange = { username = it },
                    placeholder = { Text("Nhập tên đăng nhập", color = Color.Gray, fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CityGuardWhite, unfocusedContainerColor = CityGuardWhite,
                        focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Email
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                    Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(16.dp), tint = CityGuardBlack)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Email", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CityGuardBlack)
                }
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    placeholder = { Text("Nhập email", color = Color.Gray, fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CityGuardWhite, unfocusedContainerColor = CityGuardWhite,
                        focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Mật khẩu
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                    Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp), tint = CityGuardBlack)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Password", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CityGuardBlack)
                }
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    placeholder = { Text("Nhập mật khẩu", color = Color.Gray, fontSize = 14.sp) },
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
                Spacer(modifier = Modifier.height(10.dp))

                // Confirm
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                    Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp), tint = CityGuardBlack)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Confirm", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CityGuardBlack)
                }
                OutlinedTextField(
                    value = confirmPass, onValueChange = { confirmPass = it },
                    placeholder = { Text("Nhập lại mật khẩu", color = Color.Gray, fontSize = 14.sp) },
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

            // Nút Đăng ký
            if (authState is AuthState.Loading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CityGuardBlack)
                }
            } else {
                Button(
                    onClick = { authViewModel.register(username, email, password, confirmPass) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CityGuardBlack),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("ĐĂNG KÝ", color = CityGuardWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Đã có tài khoản? ", color = CityGuardBlack, fontSize = 14.sp)
                Text(
                    text = "Đăng nhập",
                    modifier = Modifier.clickable { navController.popBackStack() },
                    color = CityGuardBlack, fontSize = 14.sp, fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}