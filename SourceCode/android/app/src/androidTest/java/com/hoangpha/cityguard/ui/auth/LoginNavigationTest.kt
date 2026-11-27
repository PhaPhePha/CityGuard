package com.hoangpha.cityguard.ui.auth

// TV2 - Trần Hữu Thịnh: kiểm tra tương tác Auth trên emulator bằng controller giả, không gọi API thật/tạo tài khoản thật.
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.hoangpha.cityguard.data.api.FirebaseProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginNavigationTest {
    @get:Rule val composeTestRule = createComposeRule()
    private lateinit var navController: TestNavHostController
    private lateinit var auth: FakeAuthController
    @Before fun setup() {
        navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        auth = FakeAuthController()
        navController.navigatorProvider.addNavigator(ComposeNavigator())
        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                NavHost(navController, startDestination = "login") {
                    composable("login") { LoginScreen(navController, auth, socialLoginLauncher = { auth.launchedProvider = it }) }
                    composable("register") { RegisterScreen(navController, auth) }
                    composable("forgot_password") { ForgotPasswordScreen(navController) }
                    composable("main") { Box(Modifier.testTag("map_route")) }
                }
            }
        }
    }
    @Test fun forgotPasswordOpensOriginalRecoveryScreen() {
        composeTestRule.onNodeWithText("Quên mật khẩu?").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals("forgot_password", navController.currentDestination?.route)
        composeTestRule.onNodeWithText("Khôi phục mật khẩu").assertIsDisplayed()
    }
    @Test fun googleButtonStartsGoogleFlowWithoutEnteringMain() {
        composeTestRule.onNodeWithContentDescription("Đăng nhập bằng Google").performScrollTo().performClick()
        composeTestRule.runOnIdle {
            assertEquals(FirebaseProvider.GOOGLE, auth.launchedProvider)
            assertEquals("login", navController.currentDestination?.route)
        }
    }
    @Test fun facebookButtonStartsFacebookFlowWithoutEnteringMain() {
        composeTestRule.onNodeWithContentDescription("Đăng nhập bằng Facebook").performScrollTo().performClick()
        composeTestRule.runOnIdle {
            assertEquals(FirebaseProvider.FACEBOOK, auth.launchedProvider)
            assertEquals("login", navController.currentDestination?.route)
        }
    }
    @Test fun loginOffersOnlyGoogleAndFacebookWithoutInstagram() {
        composeTestRule.onNodeWithContentDescription("Đăng nhập bằng Google").assertExists()
        composeTestRule.onNodeWithContentDescription("Đăng nhập bằng Facebook").assertExists()
        composeTestRule.onNodeWithContentDescription("Đăng nhập bằng Instagram").assertDoesNotExist()
    }

    @Test fun loginButtonRemainsVisibleAndDisabledWhileSending() {
        composeTestRule.runOnIdle { auth.setState(AuthState.Loading) }
        composeTestRule.onNodeWithTag("login_submit").performScrollTo().assertIsDisplayed().assertIsNotEnabled()
        composeTestRule.onNodeWithText("Đang đăng nhập…").assertIsDisplayed()
        composeTestRule.runOnIdle { auth.setState(AuthState.Error("Lỗi kết nối")) }
        composeTestRule.onNodeWithTag("login_submit").assertIsEnabled()
    }
    private fun openRegister() {
        composeTestRule.onNodeWithText("Đăng ký").performScrollTo().performClick()
        composeTestRule.waitForIdle()
    }
    @Test fun registerButtonRemainsVisibleAndDisabledWhileSending() {
        openRegister()
        composeTestRule.onNodeWithTag("register_username").performTextInput("thinh")
        composeTestRule.onNodeWithTag("register_email").performTextInput("thinh@example.com")
        composeTestRule.onNodeWithTag("register_password").performTextInput("secret123")
        composeTestRule.onNodeWithTag("register_confirm").performTextInput("secret123")
        composeTestRule.onNodeWithTag("register_submit").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("register_submit").assertIsDisplayed().assertIsNotEnabled()
        composeTestRule.onNodeWithText("Đang đăng ký…").assertIsDisplayed()
        assertEquals(1, auth.registerCalls)
        composeTestRule.runOnIdle { auth.setState(AuthState.Error("Lỗi kết nối")) }
        composeTestRule.onNodeWithTag("register_submit").assertIsEnabled()
    }
    @Test fun backFromPendingRegisterCancelsBeforeReturningToLogin() {
        openRegister()
        composeTestRule.runOnIdle { auth.setState(AuthState.Loading) }
        composeTestRule.onNodeWithContentDescription("Quay lại").performClick()
        composeTestRule.waitForIdle()
        assertEquals("login", navController.currentDestination?.route)
        assertEquals(1, auth.cancelCalls)
    }
    @Test fun existingAccountLinkReturnsToLoginWithoutDuplicateEntry() {
        openRegister()
        composeTestRule.onNodeWithText("Đăng nhập").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals("login", navController.currentDestination?.route)
        assertNull(navController.previousBackStackEntry)
    }
    // TV2: Register thành công phải về Login, chỉ điền account; không vào main hoặc giữ form cũ.
    @Test fun registrationSuccessReturnsToLoginWithAccountAndNoPassword() {
        openRegister()
        composeTestRule.runOnIdle { auth.setState(AuthState.Registered("thinh")) }
        composeTestRule.waitForIdle()
        assertEquals("login", navController.currentDestination?.route)
        composeTestRule.onNodeWithTag("login_account").assertTextEquals("thinh")
        assertEquals("", composeTestRule.onNodeWithTag("login_password").fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        composeTestRule.onNodeWithTag("map_route").assertDoesNotExist()
        assertNull(navController.previousBackStackEntry)
    }
    @Test fun recoverySubmitWithValidFieldsNeverClaimsPasswordWasChanged() {
        composeTestRule.onNodeWithText("Quên mật khẩu?").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("recovery_email").performTextInput("thinh@example.com")
        composeTestRule.onNodeWithTag("recovery_password").performTextInput("secret123")
        composeTestRule.onNodeWithTag("recovery_confirm").performTextInput("secret123")
        composeTestRule.onNodeWithTag("recovery_submit").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Chưa thể khôi phục mật khẩu").assertIsDisplayed()
    }
    @Test fun authSuccessClearsAuthBackStack() {
        composeTestRule.runOnIdle { auth.setState(AuthState.Success) }
        composeTestRule.waitForIdle()
        assertEquals("main", navController.currentDestination?.route)
        composeTestRule.onNodeWithTag("map_route").assertExists()
        assertNull(navController.previousBackStackEntry)
    }
    private class FakeAuthController : AuthController {
        var launchedProvider: FirebaseProvider? = null
        override fun beginSocialLogin(provider: FirebaseProvider): Boolean {
            state.value = AuthState.Loading
            return true
        }
        private val state = MutableStateFlow<AuthState>(AuthState.Idle)
        override val authState: StateFlow<AuthState> = state
        var registerCalls = 0
        var cancelCalls = 0
        fun setState(value: AuthState) { state.value = value }
        override fun login(username: String, password: String) { state.value = AuthState.Loading }
        override fun register(username: String, email: String, password: String, confirmPass: String) {
            registerCalls++; state.value = AuthState.Loading
        }
        override fun cancelPending() { cancelCalls++; state.value = AuthState.Idle }
        override fun logout(onComplete: () -> Unit) = onComplete()
        override fun resetState() { state.value = AuthState.Idle }
    }
}
