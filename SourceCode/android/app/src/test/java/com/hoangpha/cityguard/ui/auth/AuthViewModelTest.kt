package com.hoangpha.cityguard.ui.auth

// TV2 - Trần Hữu Thịnh: Kiểm thử phần TV2 và các điểm tích hợp; fake chỉ dùng trong test, không thay nguồn dữ liệu runtime của app.

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.hoangpha.cityguard.data.api.*
import com.hoangpha.cityguard.data.local.SessionManager
import com.hoangpha.cityguard.data.local.SessionStore
import com.hoangpha.cityguard.data.local.StoredSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var manager: MemorySessionStore
    private lateinit var api: FakeAuthApi
    private lateinit var vm: AuthViewModel
    private lateinit var store: ViewModelStore
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        val app = ApplicationProvider.getApplicationContext<Application>()
        manager = MemorySessionStore()
        runBlocking { manager.clearSession() }
        api = FakeAuthApi()
        vm = AuthViewModel(app, manager, api)
        store = ViewModelStore().apply { put("auth", vm) }
    }
    @After fun tearDown() {
        store.clear()
        runBlocking { manager.clearSession() }
        Dispatchers.resetMain()
    }
    @Test fun emptyLoginDoesNotCallServer() = runTest(dispatcher) {
        vm.login("", "")
        assertTrue(vm.authState.value is AuthState.Error)
        assertEquals(0, api.loginCalls)
    }
    @Test fun passwordMismatchDoesNotRegister() = runTest(dispatcher) {
        vm.register("thinh", "thinh@example.com", "secret123", "other123")
        assertEquals("Mật khẩu xác nhận không khớp", (vm.authState.value as AuthState.Error).message)
        assertEquals(0, api.registerCalls)
    }
    // TV2: hồi quy lỗi register tự vào app; 201 chỉ xác nhận đã tạo user, không lưu phiên.
    @Test fun registration201RequiresSeparateLoginWithoutSavingTokens() = runTest(dispatcher) {
        vm.register("thinh", "thinh@example.com", "secret123", "secret123")
        vm.authState.first { it is AuthState.Registered || it is AuthState.Error }
        assertEquals(AuthState.Registered("thinh"), vm.authState.value)
        assertNull(manager.getToken())
        assertNull(manager.getUserId())
        assertNull(manager.session.first())
    }
    @Test fun login200SavesTokensAndUserId() = runTest(dispatcher) {
        vm.login("thinh", "secret123")
        vm.authState.first { it is AuthState.Success || it is AuthState.Error }
        assertEquals(AuthState.Success, vm.authState.value)
        assertEquals("access", manager.getToken())
        assertEquals("7", manager.getUserId())
        assertEquals("refresh", manager.session.first()?.refreshToken)
    }
    @Test fun login401ShowsServerMessageWithoutSavingSession() = runTest(dispatcher) {
        api.loginResponse = { failure(401, "Sai mật khẩu") }
        vm.login("thinh", "wrong")
        runCurrent()
        assertEquals("Sai mật khẩu", (vm.authState.value as AuthState.Error).message)
        assertNull(manager.session.first())
    }
    @Test fun registration409ShowsConflict() = runTest(dispatcher) {
        api.registerResponse = { failure(409, "Email đã được sử dụng") }
        vm.register("thinh", "thinh@example.com", "secret123", "secret123")
        runCurrent()
        assertEquals("Email đã được sử dụng", (vm.authState.value as AuthState.Error).message)
    }
    @Test fun networkFailureShowsRetryMessage() = runTest(dispatcher) {
        api.loginResponse = { throw java.io.IOException("offline") }
        vm.login("thinh", "secret123")
        runCurrent()
        assertTrue((vm.authState.value as AuthState.Error).message.contains("Kiểm tra mạng"))
    }
    @Test fun logoutClearsLocalSessionEvenWhenServerIsOffline() = runTest(dispatcher) {
        manager.saveSession("access", "7", "refresh", 30_000)
        var complete = false
        vm.logout { complete = true }
        manager.session.first { it == null }
        runCurrent()
        assertTrue(complete)
        assertNull(manager.getToken())
    }

    // TV2: message validation từ server phải được giữ, không gán thành lỗi trùng tài khoản.
    @Test fun registration400ShowsValidationMessage() = runTest(dispatcher) {
        api.registerResponse = { failure(400, "Email không hợp lệ từ server") }
        vm.register("thinh", "thinh@example.com", "secret123", "secret123")
        runCurrent()
        assertEquals("Email không hợp lệ từ server", (vm.authState.value as AuthState.Error).message)
        assertNull(manager.session.first())
    }
    // TV2: mô phỏng mở lại app với token đã hết hạn bằng clock cố định, không sleep/advanceTimeBy.
    // Dừng ViewModel fixture để timer nền không can thiệp bài test đọc/xóa phiên đã lưu.
    @Test fun expiredPersistedSessionDoesNotProvideAccessToken() = runTest(dispatcher) {
        store.clear()
        var now = System.currentTimeMillis()
        val clockedSession = SessionManager(ApplicationProvider.getApplicationContext<Application>()) { now }
        clockedSession.saveSession("access", "7", "refresh", 60_000)
        assertEquals("access", clockedSession.getToken())
        now += 60_001
        assertNull(clockedSession.getToken())
        assertNull(clockedSession.session.first())
    }

    // TV2: timeout là lỗi hết thời gian chờ, không được báo nhầm sai mật khẩu hay server chắc chắn xử lý chậm.
    @Test fun timeoutShowsAccurateConnectionMessage() = runTest(dispatcher) {
        api.loginResponse = { throw java.net.SocketTimeoutException("connect timed out") }
        vm.login("thinh", "secret123")
        runCurrent()
        assertTrue((vm.authState.value as AuthState.Error).message.contains("quá thời gian chờ"))
    }
    // TV2: bấm Back hủy request; kết quả trả về sau đó không được tự lưu phiên hoặc đổi màn.
    @Test fun pendingAuthCanBeCancelledWithoutSavingLateResponse() = runTest(dispatcher) {
        api.pendingLogin = CompletableDeferred()
        vm.login("thinh", "secret123")
        vm.login("thinh", "secret123")
        runCurrent()
        assertEquals(1, api.loginCalls)
        assertEquals(AuthState.Loading, vm.authState.value)
        vm.cancelPending()
        runCurrent()
        api.pendingLogin!!.complete(api.loginResponse())
        runCurrent()
        assertEquals(AuthState.Idle, vm.authState.value)
        assertNull(manager.session.first())
    }

    @Test fun registerTimeoutDoesNotClaimAccountWasNeverCreated() = runTest(dispatcher) {
        api.registerResponse = { throw java.net.SocketTimeoutException("read timed out") }
        vm.register("thinh", "thinh@example.com", "secret123", "secret123")
        runCurrent()
        val message = (vm.authState.value as AuthState.Error).message
        assertTrue(message.contains("Chưa xác nhận được kết quả đăng ký"))
        assertTrue(message.contains("thử đăng nhập"))
        assertEquals(1, api.registerCalls)
    }
    private fun failure(code: Int, message: String): Response<ApiResponse<AuthResponse>> = Response.error(code,
        """{"success":false,"message":"$message"}""".toResponseBody("application/json".toMediaType()))


    // TV2: fake chỉ lưu giá trị trong RAM; ba SessionManagerTest vẫn kiểm tra DataStore thật.
    private class MemorySessionStore : SessionStore {
        private val value = MutableStateFlow<StoredSession?>(null)
        override val session: Flow<StoredSession?> = value
        override suspend fun saveSession(token: String, userId: String, refreshToken: String, expiresIn: Long) {
            value.value = StoredSession(token, refreshToken, userId, System.currentTimeMillis() + expiresIn)
        }
        override suspend fun getToken(): String? = value.value?.accessToken
        override suspend fun getUserId(): String? = value.value?.userId
        override suspend fun clearSession() { value.value = null }
        override suspend fun clearIfTokenMatches(token: String) {
            if (value.value?.accessToken == token) value.value = null
        }
    }
    private class FakeAuthApi : AuthApi {
        private val auth = AuthResponse("access", "refresh", "Bearer", 1_800_000,
            UserInfo(7, "thinh", "thinh@example.com", "Thịnh", "USER"))
        var pendingLogin: CompletableDeferred<Response<ApiResponse<AuthResponse>>>? = null
        var loginCalls = 0
        var registerCalls = 0
        var loginResponse: () -> Response<ApiResponse<AuthResponse>> = { Response.success(ApiResponse(true, null, auth)) }
        var registerResponse: () -> Response<ApiResponse<AuthResponse>> = { Response.success(201, ApiResponse(true, null, auth)) }
        override suspend fun login(request: LoginRequest): Response<ApiResponse<AuthResponse>> {
            loginCalls++; return pendingLogin?.await() ?: loginResponse()
        }
        override suspend fun register(request: RegisterRequest): Response<ApiResponse<AuthResponse>> {
            registerCalls++; return registerResponse()
        }
        override suspend fun logout(request: RefreshTokenRequest): Response<ApiResponse<Unit>> {
            throw java.io.IOException("offline")
        }
    }
}
