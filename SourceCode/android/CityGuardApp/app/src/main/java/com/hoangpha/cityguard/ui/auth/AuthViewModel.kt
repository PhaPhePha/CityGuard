package com.hoangpha.cityguard.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hoangpha.cityguard.data.api.ApiClient
import com.hoangpha.cityguard.data.api.LoginRequest
import com.hoangpha.cityguard.data.api.RegisterRequest
import com.hoangpha.cityguard.data.local.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Định nghĩa các trạng thái của màn hình Đăng nhập / Đăng ký
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionManager = SessionManager(application.applicationContext)
    private val authApi = ApiClient.create(sessionManager)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Vui lòng nhập đầy đủ thông tin")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = authApi.login(LoginRequest(username, password))
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    sessionManager.saveSession(body.token, body.userId)
                    _authState.value = AuthState.Success
                } else {
                    _authState.value = AuthState.Error("Sai tên đăng nhập hoặc mật khẩu")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Lỗi kết nối mạng. Vui lòng kiểm tra lại!")
            }
        }
    }

    fun register(username: String, email: String, password: String, confirmPass: String) {
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Vui lòng nhập đầy đủ thông tin")
            return
        }
        if (password != confirmPass) {
            _authState.value = AuthState.Error("Mật khẩu xác nhận không khớp")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authState.value = AuthState.Error("Email không hợp lệ")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = authApi.register(RegisterRequest(username, email, password))
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    sessionManager.saveSession(body.token, body.userId)
                    _authState.value = AuthState.Success
                } else {
                    _authState.value = AuthState.Error("Email hoặc tên đăng nhập đã tồn tại")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Lỗi kết nối mạng. Vui lòng thử lại!")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            sessionManager.clearSession()
            _authState.value = AuthState.Idle
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}