package com.hoangpha.cityguard.ui.profile

// TV2 - Trần Hữu Thịnh: bảo vệ việc làm mới ngầm, giữ hồ sơ khi mạng lỗi và retry thành công.
import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.hoangpha.cityguard.data.api.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var api: FakeProfileApi
    private lateinit var vm: ProfileViewModel
    private lateinit var store: ViewModelStore
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        api = FakeProfileApi()
        vm = ProfileViewModel(ApplicationProvider.getApplicationContext<Application>(), api)
        store = ViewModelStore().apply { put("profile", vm) }
    }
    @After fun tearDown() { store.clear(); Dispatchers.resetMain() }
    @Test fun backgroundRefreshRetainsProfileDuringLoadingAndFailure() = runTest(dispatcher) {
        runCurrent()
        assertEquals("Thịnh", vm.state.value.profile?.displayName)
        api.pending = CompletableDeferred()
        vm.load()
        runCurrent()
        assertTrue(vm.state.value.loading)
        assertEquals("Thịnh", vm.state.value.profile?.displayName)
        api.pending!!.completeExceptionally(java.io.IOException("offline"))
        runCurrent()
        assertFalse(vm.state.value.loading)
        assertNotNull(vm.state.value.error)
        assertEquals("Thịnh", vm.state.value.profile?.displayName)
    }
    @Test fun firstLoadFailureCanBeRetried() = runTest(dispatcher) {
        // TV2: đặt offline trước khi dựng ViewModel mới, không phụ thuộc thời điểm init của fixture chạy.
        store.clear()
        api.offline = true
        vm = ProfileViewModel(ApplicationProvider.getApplicationContext<Application>(), api)
        store = ViewModelStore().apply { put("profile", vm) }
        runCurrent()
        assertNotNull(vm.state.value.error)
        assertNull(vm.state.value.profile)
        api.offline = false
        vm.load()
        runCurrent()
        assertNull(vm.state.value.error)
        assertEquals("Thịnh", vm.state.value.profile?.displayName)
    }
    @Test fun refreshUsesUpdatedNameAndAvatarFromBackend() = runTest(dispatcher) {
        runCurrent()
        assertEquals("Thịnh", vm.state.value.profile?.displayName)
        assertNull(vm.state.value.profile?.avatarUrl)
        api.profile = api.profile.copy(displayName = "Tên từ Google", avatarUrl = "https://example.com/avatar-new.jpg")
        vm.load()
        runCurrent()
        assertEquals("Tên từ Google", vm.state.value.profile?.displayName)
        assertEquals("https://example.com/avatar-new.jpg", vm.state.value.profile?.avatarUrl)
        assertEquals(7L, vm.state.value.profile?.id)
    }

    private class FakeProfileApi : ProfileApi {
        var profile = ProfileSummaryDto(7, "thinh", "thinh@example.com", "Thịnh", null, "USER", 2, 11)
        var offline = false
        var pending: CompletableDeferred<Response<ApiResponse<ProfileSummaryDto>>>? = null
        override suspend fun summary(): Response<ApiResponse<ProfileSummaryDto>> {
            pending?.let { return it.await() }
            if (offline) throw java.io.IOException("offline")
            return Response.success(ApiResponse(true, null, profile))
        }
        override suspend fun reports(page: Int, size: Int, status: String?): Response<ApiResponse<PageDto<ReportDto>>> = error("Not used")
    }
}
