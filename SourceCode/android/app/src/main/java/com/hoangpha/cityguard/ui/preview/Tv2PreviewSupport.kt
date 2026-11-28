package com.hoangpha.cityguard.ui.preview

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.hoangpha.cityguard.data.api.ProfileSummaryDto
import com.hoangpha.cityguard.data.api.ReportDto
import com.hoangpha.cityguard.ui.auth.AuthController
import com.hoangpha.cityguard.ui.auth.AuthState
import com.hoangpha.cityguard.ui.theme.CityGuardAppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Chỉ các hàm @Preview sử dụng helper/dữ liệu dưới đây. Không gọi API, Firebase hoặc SessionManager.
internal class PreviewAuthController(initialState: AuthState = AuthState.Idle) : AuthController {
    override val authState: StateFlow<AuthState> = MutableStateFlow(initialState)
    override fun login(username: String, password: String) = Unit
    override fun register(username: String, email: String, password: String, confirmPass: String) = Unit
    override fun logout(onComplete: () -> Unit) = Unit
    override fun resetState() = Unit
}

@Composable
internal fun Tv2PreviewFrame(content: @Composable (NavHostController) -> Unit) {
    val previewLifecycle = LocalLifecycleOwner.current.lifecycle
    val backOwner = remember(previewLifecycle) {
        object : OnBackPressedDispatcherOwner {
            override val lifecycle = previewLifecycle
            override val onBackPressedDispatcher = OnBackPressedDispatcher()
        }
    }
    val resultOwner = remember {
        object : ActivityResultRegistryOwner {
            override val activityResultRegistry = object : ActivityResultRegistry() {
                override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) = Unit
            }
        }
    }
    CompositionLocalProvider(
        LocalOnBackPressedDispatcherOwner provides backOwner,
        LocalActivityResultRegistryOwner provides resultOwner
    ) {
        val navController = rememberNavController()
        CityGuardAppTheme { content(navController) }
    }
}

internal fun previewProfile() = ProfileSummaryDto(
    id = 1, username = "preview_user", email = "preview@example.com", displayName = "Tên xem trước",
    avatarUrl = null, role = "USER", reportCount = 5, confirmedReports = 3
)

internal fun previewReports() = listOf(
    ReportDto(
        id = "preview-1", type = "TRAFFIC_JAM", status = "ACTIVE", trustScore = 70,
        trustLabel = "LIKELY", confirmCount = 3, noLongerCount = 0, userVote = null,
        latitude = 10.77, longitude = 106.69, addressSnapshot = "Địa chỉ xem trước",
        description = "Nội dung báo cáo dùng riêng cho Preview.", imageUrl = null,
        createdAt = "2026-11-27T08:00:00Z", expiresAt = null, creatorId = "1"
    )
)
