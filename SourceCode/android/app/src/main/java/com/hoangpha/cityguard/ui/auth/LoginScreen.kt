package com.hoangpha.cityguard.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavController
import com.hoangpha.cityguard.R
import com.hoangpha.cityguard.ui.theme.*

@Composable
fun LoginScreen(
    navController: NavController,
    authViewModel: AuthController = viewModel<AuthViewModel>()
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val authState by authViewModel.authState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Success -> {
                navController.navigate("main") {
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    launchSingleTop = true
                }
                snackbarHostState.showSnackbar("Đăng nhập thành công!")
                authViewModel.resetState()
            }
            is AuthState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                authViewModel.resetState()
            }
            else -> {}
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(CityGuardYellowLight)
    ) {
        val compact = maxHeight < 740.dp
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.align(Alignment.TopStart).zIndex(1f).systemBarsPadding().padding(start = 8.dp, top = 4.dp)
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Quay lại", tint = CityGuardBlack)
        }
        // ẢNH NỀN NHÀ RÕ HƠN MỘT CHÚT (Tăng alpha lên 0.55f)
        // Giữ tỷ lệ ảnh; neo đáy để thành phố và con đường không bị lệch giữa các màn.
        Image(
            painter = painterResource(id = R.drawable.bg_cityguard_auth),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomCenter
        )
        // Vàng rõ hơn ở vùng thương hiệu, trong dần xuống thành phố để giữ chi tiết ảnh.
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    0f to CityGuardAuthYellow.copy(alpha = 0.72f),
                    0.42f to CityGuardAuthYellow.copy(alpha = 0.45f),
                    1f to CityGuardAuthYellow.copy(alpha = 0.08f)
                )
            )
        )

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .padding(top = 48.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(if (compact) 0.dp else 12.dp))

            // 1. LOGO (CHÈN TRÊN CHỮ)
            Image(
                painter = painterResource(id = R.drawable.logo_cityguard),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(if (compact) 130.dp else 120.dp)
            )

            // 2. CHỮ
            Text("CITYGUARD", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = CityGuardBlack)
            Text("An toàn hơn, kết nối hơn", fontSize = 17.sp, color = CityGuardBlack, modifier = Modifier.padding(top = 4.dp, bottom = if (compact) 24.dp else 32.dp))
            Text(
                text = "Welcome back",
                fontSize = 26.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
                color = CityGuardBlack,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                textAlign = TextAlign.Start
            )

            // KHUNG BỌC CÁC Ô NHẬP LIỆU
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CityGuardWhite.copy(alpha = 0.78f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                // Ô Tài khoản
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                    Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = CityGuardBlack)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tên đăng nhập", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CityGuardBlack)
                }
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    placeholder = { Text("Nhập tên đăng nhập", color = Color.Gray, fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CityGuardWhite, unfocusedContainerColor = CityGuardWhite,
                        focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Ô Mật khẩu
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                    Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp), tint = CityGuardBlack)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mật khẩu", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CityGuardBlack)
                }
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("Nhập mật khẩu", color = Color.Gray, fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    shape = RoundedCornerShape(18.dp),
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

                Text(
                    text = "Quên mật khẩu?",
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable {
                        navController.navigate("forgot_password")
                    },
                    color = CityGuardBlack, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Nút Đăng nhập
            if (authState is AuthState.Loading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CityGuardBlack)
                }
            } else {
                Button(
                    onClick = { authViewModel.login(username, password) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CityGuardBlack),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("ĐĂNG NHẬP", color = CityGuardWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(Modifier.weight(1f), thickness = 1.dp, color = CityGuardBlack.copy(alpha = 0.15f))
                Text("hoặc đăng nhập bằng", modifier = Modifier.padding(horizontal = 12.dp), fontSize = 13.sp, color = CityGuardBlack.copy(alpha = 0.75f))
                HorizontalDivider(Modifier.weight(1f), thickness = 1.dp, color = CityGuardBlack.copy(alpha = 0.15f))
            }
            // nút Google
            Spacer(modifier = Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally)) {
                OutlinedButton(
                    onClick = {}, enabled = false,
                    modifier = Modifier.size(48.dp).semantics { contentDescription = "Đăng nhập bằng Google" },
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color(0xFFDADCE0)),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = CityGuardWhite, disabledContainerColor = CityGuardWhite)
                ) {
                    Image(painterResource(R.drawable.ic_google_signin), contentDescription = null, modifier = Modifier.size(26.dp))
                }
                // nút Facebook
                OutlinedButton(
                    onClick = {}, enabled = false,
                    modifier = Modifier.size(48.dp).semantics { contentDescription = "Đăng nhập bằng Facebook" },
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color(0xFFDADCE0)),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = CityGuardWhite, disabledContainerColor = CityGuardWhite)
                ) {
                    Image(painterResource(R.drawable.ic_facebook_signin), contentDescription = null, modifier = Modifier.size(28.dp))
                }
                // nút Ig
                OutlinedButton(
                    onClick = {}, enabled = false,
                    modifier = Modifier.size(48.dp).semantics { contentDescription = "Đăng nhập bằng Instagram" },
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color(0xFFDADCE0)),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = CityGuardWhite, disabledContainerColor = CityGuardWhite)
                ) {
                    Image(painterResource(R.drawable.ic_instagram_signin), contentDescription = null, modifier = Modifier.size(28.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Chưa có tài khoản? ", color = CityGuardBlack, fontSize = 15.sp)
                Text(
                    text = "Đăng ký",
                    modifier = Modifier.clickable { navController.navigate("register") },
                    color = CityGuardBlack, fontSize = 15.sp, fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
