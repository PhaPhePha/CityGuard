package com.hoangpha.cityguard.ui.auth

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hoangpha.cityguard.ui.navigation.MainScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var navController: TestNavHostController
    private lateinit var authController: FakeAuthController

    @Before
    fun setUp() {
        navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        authController = FakeAuthController()
        navController.navigatorProvider.addNavigator(ComposeNavigator())

        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                NavHost(
                    navController = navController,
                    startDestination = "login"
                ) {
                    composable("login") { LoginScreen(navController, authController) }
                    composable("forgot_password") { ForgotPasswordScreen(navController) }
                    composable("main") { MainScreen(authController) {} }
                }
            }
        }
    }

    @Test
    fun forgotPasswordLinkNavigatesToForgotPasswordRoute() {
        composeTestRule.onNodeWithText("Quên mật khẩu?").performClick()
        composeTestRule.waitForIdle()

        assertEquals("forgot_password", navController.currentDestination?.route)
    }

    @Test
    fun authSuccessNavigatesToMainAndDisplaysMapRoute() {
        composeTestRule.runOnIdle {
            authController.setState(AuthState.Success)
        }
        composeTestRule.waitForIdle()

        assertEquals("main", navController.currentDestination?.route)
        composeTestRule.onNodeWithTag("map_route").assertIsDisplayed()
    }

    private class FakeAuthController : AuthController {
        private val mutableAuthState = MutableStateFlow<AuthState>(AuthState.Idle)
        override val authState: StateFlow<AuthState> = mutableAuthState

        fun setState(state: AuthState) {
            mutableAuthState.value = state
        }

        override fun login(username: String, password: String) = Unit
        override fun register(username: String, email: String, password: String, confirmPass: String) = Unit
        override fun logout(onComplete: () -> Unit) = onComplete()
        override fun resetState() {
            mutableAuthState.value = AuthState.Idle
        }
    }
}
