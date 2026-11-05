package com.hoangpha.cityguard.ui.auth

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun appNavigatesToForgotPassword() {
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())

        composeTestRule.setContent {
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            // Trong thực tế, bạn sẽ bọc LoginScreen trong NavHost để test navigation graph hoàn chỉnh.
            // Đoạn test mẫu này đảm bảo file được tạo thành công để bạn có thể mở rộng sau này.
        }

        // TODO: Viết UI logic điều hướng click vào chữ "Quên mật khẩu?" và kiểm tra navController.currentDestination
    }
}