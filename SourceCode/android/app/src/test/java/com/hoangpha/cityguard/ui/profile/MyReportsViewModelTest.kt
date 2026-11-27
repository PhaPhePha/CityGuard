package com.hoangpha.cityguard.ui.profile

// TV2 - Trần Hữu Thịnh: Kiểm thử phần TV2 và các điểm tích hợp; fake chỉ dùng trong test, không thay nguồn dữ liệu runtime của app.

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.hoangpha.cityguard.data.api.*
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
class MyReportsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var api: FakeProfileApi
    private lateinit var vm: MyReportsViewModel
    private lateinit var store: ViewModelStore
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        api = FakeProfileApi()
        vm = MyReportsViewModel(ApplicationProvider.getApplicationContext<Application>(), api)
        store = ViewModelStore().apply { put("reports", vm) }
    }
    @After fun tearDown() { store.clear(); Dispatchers.resetMain() }
    @Test fun emptyResponseStopsLoading() = runTest(dispatcher) {
        runCurrent()
        assertFalse(vm.state.value.loading)
        assertTrue(vm.state.value.reports.isEmpty())
        assertNull(vm.state.value.error)
    }
    @Test fun statusIsSentToBackend() = runTest(dispatcher) {
        runCurrent()
        vm.selectStatus("ACTIVE")
        runCurrent()
        assertEquals("ACTIVE", api.lastStatus)
        assertEquals(0, api.lastPage)
    }
    @Test fun networkErrorCanBeRetried() = runTest(dispatcher) {
        runCurrent()
        api.offline = true
        vm.refresh()
        runCurrent()
        assertNotNull(vm.state.value.error)
        api.offline = false
        vm.retry()
        runCurrent()
        assertNull(vm.state.value.error)
    }
    private class FakeProfileApi : ProfileApi {
        var offline = false
        var lastStatus: String? = null
        var lastPage = -1
        override suspend fun summary(): Response<ApiResponse<ProfileSummaryDto>> = error("Not used")
        override suspend fun reports(page: Int, size: Int, status: String?): Response<ApiResponse<PageDto<ReportDto>>> {
            if (offline) throw java.io.IOException("offline")
            lastStatus = status; lastPage = page
            return Response.success(ApiResponse(true, null, PageDto(emptyList(), page, size, 0, 0, false)))
        }
    }
}
