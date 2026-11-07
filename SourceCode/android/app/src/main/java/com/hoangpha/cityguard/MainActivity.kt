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
import com.hoangpha.cityguard.ui.auth.ForgotPasswordScreen
import com.hoangpha.cityguard.ui.auth.LoginScreen
import com.hoangpha.cityguard.ui.auth.RegisterScreen
import com.hoangpha.cityguard.ui.auth.SplashScreen
import com.hoangpha.cityguard.ui.navigation.MainScreen
import com.hoangpha.cityguard.ui.theme.CityGuardAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Khởi tạo SessionManager
        val sessionManager = SessionManager(applicationContext)

        setContent {
            CityGuardAppTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = viewModel()

                NavHost(navController = navController, startDestination = "splash") {
                    composable("splash") { SplashScreen(navController, sessionManager) }
                    composable("login") { LoginScreen(navController, authViewModel) }
                    composable("register") { RegisterScreen(navController, authViewModel) }
                    composable("forgot_password") { ForgotPasswordScreen(navController) }
                    composable("main") {
                        MainScreen(authViewModel) {
                            navController.navigate("login") {
                                popUpTo("main") {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    }
                }
            }
        }
    }
}
