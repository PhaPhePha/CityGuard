package com.hoangpha.cityguard.ui.auth

// Google/Facebook đổi Firebase ID Token lấy JWT CityGuard

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.hoangpha.cityguard.data.api.FirebaseProvider
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
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
import androidx.compose.ui.tooling.preview.Preview
import com.hoangpha.cityguard.ui.preview.PreviewAuthController
import com.hoangpha.cityguard.ui.preview.Tv2PreviewFrame

@Composable
fun LoginScreen(
    navController: NavController,
    authViewModel: AuthController = viewModel<AuthViewModel>(),
    socialLoginLauncher: ((FirebaseProvider) -> Unit)? = null
) {
    val loginEntry = remember { navController.currentBackStackEntry }
    var username by rememberSaveable { mutableStateOf(loginEntry?.savedStateHandle?.get<String>("registered_account").orEmpty()) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val authState by authViewModel.authState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val isLoading = authState is AuthState.Loading
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    // Back hủy request đang chờ, tránh một form đã đóng vẫn nhận kết quả và tự chuyển màn.
    fun goBack() {
        authViewModel.cancelPending()
        navController.popBackStack()
    }
    BackHandler(enabled = isLoading) { goBack() }
    val context = LocalContext.current
    val socialLogin = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK) {
            val provider = runCatching {
                FirebaseProvider.valueOf(data?.getStringExtra(SocialLoginActivity.EXTRA_PROVIDER).orEmpty())
            }.getOrNull()
            if (provider != null) authViewModel.finishSocialLogin(
                data?.getStringExtra(SocialLoginActivity.EXTRA_ID_TOKEN).orEmpty(), provider
            ) else authViewModel.socialLoginFailed("Không nhận được kết quả đăng nhập hợp lệ.")
        } else authViewModel.socialLoginFailed(data?.getStringExtra(SocialLoginActivity.EXTRA_ERROR))
    }
    fun startSocialLogin(provider: FirebaseProvider) {
        if (authViewModel.beginSocialLogin(provider)) {
            try {
                if (socialLoginLauncher != null) socialLoginLauncher(provider)
                else socialLogin.launch(Intent(context, SocialLoginActivity::class.java)
                    .putExtra(SocialLoginActivity.EXTRA_PROVIDER, provider.name))
            } catch (_: Exception) {
                authViewModel.socialLoginFailed("Không mở được đăng nhập. Vui lòng thử lại.")
            }
        }
    }

    LaunchedEffect(Unit) {
        loginEntry?.savedStateHandle?.remove<String>("registered_account")
        loginEntry?.savedStateHandle?.remove<String>("registration_message")?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Success -> {
                authViewModel.resetState()
                navController.navigate("main") {
                    popUpTo(navController.graph.id) { inclusive = false }
                    launchSingleTop = true
                }
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
            onClick = { goBack() },
            modifier = Modifier.align(Alignment.TopStart).zIndex(1f).systemBarsPadding().padding(start = 8.dp, top = 4.dp)
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Quay lại", tint = CityGuardBlack)
        }
        Image(
            painter = painterResource(id = R.drawable.bg_cityguard_auth),
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
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .padding(top = 48.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(if (compact) 0.dp else 12.dp))

            // 1. LOGO
            Image(
                painter = painterResource(id = R.drawable.logo_cityguard),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(if (compact) 130.dp else 112.dp)
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
                    Text("Tên đăng nhập hoặc email", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CityGuardBlack)
                }
                OutlinedTextField(
                    value = username,
                    readOnly = isLoading,
                    onValueChange = { username = it },
                    placeholder = { Text("Nhập tên đăng nhập hoặc email", color = Color.Gray, fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("login_account"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
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
                    readOnly = isLoading,
                    onValueChange = { password = it },
                    placeholder = { Text("Nhập mật khẩu", color = Color.Gray, fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("login_password"),
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CityGuardWhite, unfocusedContainerColor = CityGuardWhite,
                        focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent
                    ),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (!isLoading) { keyboard?.hide(); authViewModel.login(username, password) }
                    }),
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
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable(enabled = !isLoading) {
                        // TV2: khôi phục điều hướng theo yêu cầu; API đổi mật khẩu là phần tích hợp riêng với TV5.
                        authViewModel.resetState()
                        navController.navigate("forgot_password") { launchSingleTop = true }
                    },
                    color = CityGuardBlack, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            AuthSubmitButton("ĐĂNG NHẬP", "Đang đăng nhập…", isLoading, "login_submit") {
                keyboard?.hide()
                snackbarHostState.currentSnackbarData?.dismiss()
                authViewModel.login(username, password)
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
                    onClick = { scope.launch { snackbarHostState.showSnackbar("Tính năng đăng nhập bằng Google đang phát triển. Vui lòng quay lại sau!") } }, 
                    enabled = true,
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
                    onClick = { scope.launch { snackbarHostState.showSnackbar("Tính năng đăng nhập bằng Facebook đang phát triển. Vui lòng quay lại sau!") } }, 
                    enabled = true,
                    modifier = Modifier.size(48.dp).semantics { contentDescription = "Đăng nhập bằng Facebook" },
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color(0xFFDADCE0)),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = CityGuardWhite, disabledContainerColor = CityGuardWhite)
                ) {
                    Image(painterResource(R.drawable.ic_facebook_signin), contentDescription = null, modifier = Modifier.size(28.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Chưa có tài khoản? ", color = CityGuardBlack, fontSize = 15.sp)
                Text(
                    text = "Đăng ký",
                    modifier = Modifier.clickable(enabled = !isLoading) { authViewModel.resetState(); navController.navigate("register") { launchSingleTop = true } },
                    color = CityGuardBlack, fontSize = 15.sp, fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().imePadding()
                .padding(horizontal = 12.dp, vertical = 8.dp).zIndex(2f)
        )
    }

}

// Chỉ dựng giao diện trong Android Studio, không đăng nhập/gọi backend.
@Preview(name = "Login - Bình thường", group = "TV2 Login", widthDp = 393, heightDp = 852, showBackground = true, showSystemUi = true)
@Composable
private fun LoginIdlePreview() {
    Tv2PreviewFrame { nav -> LoginScreen(nav, remember { PreviewAuthController() }, socialLoginLauncher = {}) }
}

@Preview(name = "Login - Đang đăng nhập", group = "TV2 Login", widthDp = 393, heightDp = 852, showBackground = true, showSystemUi = true)
@Composable
private fun LoginLoadingPreview() {
    Tv2PreviewFrame { nav -> LoginScreen(nav, remember { PreviewAuthController(AuthState.Loading) }, socialLoginLauncher = {}) }
}
