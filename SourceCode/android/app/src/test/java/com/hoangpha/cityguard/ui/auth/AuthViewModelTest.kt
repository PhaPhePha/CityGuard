package com.hoangpha.cityguard.ui.auth

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: AuthViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val app = ApplicationProvider.getApplicationContext<Application>()
        viewModel = AuthViewModel(app)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login with empty credentials should return Error state`() {
        // Arrange
        val username = ""
        val password = ""

        // Act
        viewModel.login(username, password)

        // Assert
        val state = viewModel.authState.value
        assertTrue(state is AuthState.Error)
        assertEquals("Vui lòng nhập đầy đủ thông tin", (state as AuthState.Error).message)
    }

    @Test
    fun `register with mismatched passwords should return Error state`() {
        // Act
        viewModel.register("user1", "user1@email.com", "pass123", "pass456")

        // Assert
        val state = viewModel.authState.value
        assertTrue(state is AuthState.Error)
        assertEquals("Mật khẩu xác nhận không khớp", (state as AuthState.Error).message)
    }
}