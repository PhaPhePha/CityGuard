package com.hoangpha.cityguard

// TV2 - Trần Hữu Thịnh: Quan sát phiên ở navigation gốc để chặn main khi chưa đăng nhập và xóa back stack lúc hết phiên/logout.

import android.os.Bundle
import android.annotation.SuppressLint
import android.os.Build
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.hoangpha.cityguard.data.RepositoryProvider
import com.hoangpha.cityguard.data.local.SessionManager
import com.hoangpha.cityguard.ui.auth.*
import com.hoangpha.cityguard.ui.navigation.MainScreen
import com.hoangpha.cityguard.ui.theme.CityGuardAppTheme
import java.net.URI

class MainActivity : ComponentActivity() {
    // TV2 - Trần Hữu Thịnh: xin quyền local của Android 17 qua hộp thoại hệ thống.
    // Không tự gửi lại Login/Register sau khi được cấp quyền, tránh tạo yêu cầu trùng.
    // TV2: lint cảnh báo do Fragment cũ trong dependency của nhóm; màn này kế thừa
    // ComponentActivity trực tiếp, không đi qua FragmentActivity nên không gặp lỗi chuyển tiếp quyền đó.
    @SuppressLint("InvalidFragmentVersionForActivityResult")
    private val localNetworkPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) Toast.makeText(
            this,
            "Bản thử nghiệm cần quyền mạng nội bộ để kết nối backend trên máy tính. Bạn có thể cấp lại trong Cài đặt ứng dụng.",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun requestLocalBackendPermissionIfNeeded() {
        // TV2: chỉ bản debug dùng localhost/LAN mới hỏi; không yêu cầu quyền này ở bản release.
        if (!BuildConfig.DEBUG || Build.VERSION.SDK_INT < 37) return
        val host = runCatching { URI(BuildConfig.BASE_URL).host.orEmpty().lowercase() }.getOrDefault("")
        val localHost = host == "localhost" || host.endsWith(".localhost") || host.endsWith(".local") ||
            host == "::1" || host == "[::1]" || host.startsWith("127.") || host.startsWith("10.") ||
            host.startsWith("192.168.") || Regex("172\\.(1[6-9]|2[0-9]|3[01])\\..*").matches(host)
        val permission = "android.permission.ACCESS_LOCAL_NETWORK"
        if (localHost && checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            localNetworkPermission.launch(permission)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestLocalBackendPermissionIfNeeded()
        val sessionManager = SessionManager(applicationContext)
        RepositoryProvider.initialize(sessionManager)
        setContent {
            CityGuardAppTheme {
                val auth: AuthViewModel = viewModel()
                val session by auth.sessionStatus.collectAsStateWithLifecycle()
                if (session == SessionStatus.Checking) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else {
                    val nav = rememberNavController()
                    val entry by nav.currentBackStackEntryAsState()
                    val route = entry?.destination?.route
                    // TV2: dùng cùng một chốt phiên cho mọi màn con; pop tới graph gốc để Back không mở lại dữ liệu user cũ.
                    LaunchedEffect(session, route) {
                        val target = when {
                            session is SessionStatus.SignedIn && route != "main" -> "main"
                            session == SessionStatus.SignedOut && route == "main" -> "login"
                            else -> null
                        }
                        if (target != null) nav.navigate(target) {
                            popUpTo(nav.graph.id) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                    NavHost(navController = nav, startDestination = "splash") {
                        composable("splash") { SplashScreen(nav, sessionManager) }
                        composable("login") { LoginScreen(nav, auth) }
                        composable("register") { RegisterScreen(nav, auth) }
                        // TV2: khôi phục route Quên mật khẩu theo yêu cầu; form báo rõ nếu chức năng khôi phục chưa khả dụng.
                        composable("forgot_password") { ForgotPasswordScreen(nav) }
                        composable("main") {
                            if (session is SessionStatus.SignedIn) MainScreen(auth, onLogout = {})
                        }
                    }
                }
            }
        }
    }
}
