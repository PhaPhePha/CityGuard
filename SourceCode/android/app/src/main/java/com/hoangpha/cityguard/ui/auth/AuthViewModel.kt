package com.hoangpha.cityguard.ui.auth

// TV2 - Trần Hữu Thịnh: Quản lý Auth + trạng thái phiên trong ViewModel hiện có; hết hạn yêu cầu Login, không refresh ngầm.

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hoangpha.cityguard.data.api.*
import com.hoangpha.cityguard.data.local.SessionManager
import com.hoangpha.cityguard.data.local.SessionStore
import com.hoangpha.cityguard.data.local.StoredSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.withTimeoutOrNull

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    // TV2 - Thịnh: tạo tài khoản xong vẫn chưa có phiên; UI chuyển về Login thay vì vào main.
    data class Registered(val account: String) : AuthState()
    data class Error(val message: String) : AuthState()
}
sealed class SessionStatus {
    object Checking : SessionStatus()
    object SignedOut : SessionStatus()
    data class SignedIn(val session: StoredSession) : SessionStatus()
}

class AuthViewModel @JvmOverloads constructor(
    application: Application,
    private val sessionManager: SessionStore = SessionManager(application),
    private val authApi: AuthApi = ApiClient.auth(sessionManager)
) : AndroidViewModel(application), AuthController {
    private var authJob: Job? = null // TV2: request Auth có thể hủy khi user rời form.
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()
    // TV2: Checking ngăn app vội mở main trước khi DataStore đọc xong; SignedIn chỉ dành cho phiên còn hạn.
    val sessionStatus: StateFlow<SessionStatus> = sessionManager.session.map { session ->
        if (session != null && session.expiresAt > System.currentTimeMillis()) SessionStatus.SignedIn(session)
        else SessionStatus.SignedOut
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SessionStatus.Checking)

    // TV2: timer theo vòng đời ViewModel; đổi token/logout sẽ hủy timer cũ qua collectLatest.
    init {
        viewModelScope.launch {
            sessionManager.session.collectLatest { session ->
                if (session != null) {
                    delay((session.expiresAt - System.currentTimeMillis()).coerceAtLeast(0))
                    sessionManager.clearIfTokenMatches(session.accessToken)
                }
            }
        }
    }

    // TV2: trim tài khoản nhưng giữ nguyên password; chặn gửi trùng khi request đang chạy.
    override fun login(username: String, password: String) {
        if (_authState.value is AuthState.Loading) return
        if (username.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Vui lòng nhập đầy đủ thông tin")
            return
        }
        authenticate { requireApiData(authApi.login(LoginRequest(username.trim(), password))) }
    }

    // TV2: ràng buộc username/email/password khớp RegisterRequest backend; vẫn nhận message validation phía server.
    override fun register(username: String, email: String, password: String, confirmPass: String) {
        if (_authState.value is AuthState.Loading) return
        val name = username.trim()
        val mail = email.trim()
        val error = when {
            name.isBlank() || mail.isBlank() || password.isBlank() -> "Vui lòng nhập đầy đủ thông tin"
            !Regex("^[A-Za-z0-9_]{3,50}$").matches(name) -> "Tên đăng nhập từ 3–50 ký tự, chỉ dùng chữ, số và dấu gạch dưới"
            password.length !in 6..100 -> "Mật khẩu phải có từ 6 đến 100 ký tự"
            password != confirmPass -> "Mật khẩu xác nhận không khớp"
            mail.length > 254 || !Patterns.EMAIL_ADDRESS.matcher(mail).matches() -> "Email không hợp lệ"
            else -> null
        }
        if (error != null) { _authState.value = AuthState.Error(error); return }
        // Form hiện có chưa nhập tên hiển thị riêng: dùng username làm giá trị ban đầu.
        authenticate(registration = true) { requireApiData(authApi.register(RegisterRequest(name, mail, password, name))) }
    }

    // TV2: chỉ lưu phiên sau khi response có token/user hợp lệ; lỗi không điều hướng vào main.
    private fun authenticate(registration: Boolean = false, request: suspend () -> AuthResponse) {
        _authState.value = AuthState.Loading
        authJob = viewModelScope.launch {
            try {
                val body = request()
                require(body.userInfo.id > 0 && body.tokenType.equals("Bearer", ignoreCase = true))
                if (registration) {
                    // TV2: API register có trả JWT nhưng flow yêu cầu user tự đăng nhập.
                    // Không lưu bất kỳ token/user ID nào ở bước này, nên chốt phiên không tự mở main.
                    _authState.value = AuthState.Registered(body.userInfo.username)
                } else {
                    sessionManager.saveSession(body.accessToken, body.userInfo.id.toString(), body.refreshToken, body.expiresIn)
                    _authState.value = AuthState.Success
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                // TV2: timeout khi đăng ký chưa chứng minh server chưa tạo tài khoản; không tự gửi lại POST.
                val message = if (registration && e is java.io.InterruptedIOException)
                    "Chưa xác nhận được kết quả đăng ký. Hãy thử đăng nhập trước khi đăng ký lại."
                else e.userMessage()
                _authState.value = AuthState.Error(message)
            }
        }
    }

    // TV2: logout local có hiệu lực ngay cả khi offline; API logout thu hồi refresh token theo best effort.
    override fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            val refresh = sessionManager.session.first()?.refreshToken
            sessionManager.clearSession()
            _authState.value = AuthState.Idle
            onComplete()
            // Xóa phiên local ngay cả khi server tắt; thu hồi refresh token theo best effort.
            if (!refresh.isNullOrBlank()) {
                try { withTimeoutOrNull(3_000) { authApi.logout(RefreshTokenRequest(refresh)) } }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { /* Phiên local đã được xóa. */ }
            }
        }
    }
    // TV2: chỉ hủy việc đang chờ, không giả lập rollback tài khoản đã được server tạo.
    override fun cancelPending() {
        authJob?.cancel()
        authJob = null
        _authState.value = AuthState.Idle
    }
    override fun resetState() { _authState.value = AuthState.Idle }
}
