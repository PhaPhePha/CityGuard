package com.hoangpha.cityguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hoangpha.cityguard.data.local.SessionManager
import com.hoangpha.cityguard.ui.auth.AuthViewModel
import com.hoangpha.cityguard.ui.auth.LoginScreen
import com.hoangpha.cityguard.ui.auth.RegisterScreen
import com.hoangpha.cityguard.ui.auth.SplashScreen
import com.hoangpha.cityguard.ui.navigation.MainScreen
import com.hoangpha.cityguard.ui.theme.CityGuardAppTheme
import com.hoangpha.cityguard.ui.auth.ForgotPasswordScreen
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Khởi tạo SessionManager
        val sessionManager = SessionManager(applicationContext)

        setContent {
            CityGuardAppTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = viewModel()

                NavHost(navController = navController, startDestination = "splash") {
                    // Màn hình Splash
                    composable("splash") { SplashScreen(navController, sessionManager) }
                    // Màn hình Đăng nhập
                    composable("login") { LoginScreen(navController, authViewModel) }
                    // Màn hình Đăng ký
                    composable("register") { RegisterScreen(navController, authViewModel) }
                    // Màn hình quên mật khẩu
                    composable("forgot_password") { ForgotPasswordScreen(navController) }
                    // Màn hình chính
                    //composable("main") { MainScreen(authViewModel) }

                }
            }
        }
    }
}