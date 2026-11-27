package com.hoangpha.cityguard.ui.profile

// TV2 - Trần Hữu Thịnh: Nối My Reports với /me/reports, phân trang và lọc status trên server; không tải toàn bộ report rồi lọc user ở client.

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hoangpha.cityguard.data.api.*
import com.hoangpha.cityguard.data.local.SessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MyReportsUiState(
    val loading: Boolean = true, val loadingMore: Boolean = false,
    val reports: List<ReportDto> = emptyList(), val status: String? = null,
    val nextPage: Int = 0, val hasNext: Boolean = false, val error: String? = null
)
class MyReportsViewModel @JvmOverloads constructor(
    // Android chỉ ghép trang/distinct id; bảo vệ quyền sở hữu phải được test bằng hai JWT thật.
    application: Application,
    private val api: ProfileApi = ApiClient.profile(SessionManager(application))
) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(MyReportsUiState())
    val state = _state.asStateFlow()
    private var job: Job? = null
    init { refresh() }
    fun selectStatus(status: String?) {
        if (status == _state.value.status) return
        refresh(status)
    }
    fun refresh(status: String? = _state.value.status) {
        job?.cancel()
        _state.value = MyReportsUiState(status = status)
        fetch(0, status, append = false)
    }
    // dùng nextPage/hasNext từ state, chặn bấm tải thêm liên tục tạo request trùng.
    fun loadMore() {
        val current = _state.value
        if (job?.isActive == true || !current.hasNext) return
        fetch(current.nextPage, current.status, append = true)
    }
    fun retry() {
        if (_state.value.reports.isEmpty()) refresh() else loadMore()
    }
    // response của /me/reports có data.items; distinctBy(id) ngăn card bị lặp khi dữ liệu giữa hai trang thay đổi.
    private fun fetch(page: Int, status: String?, append: Boolean) {
        _state.value = _state.value.copy(loading = !append, loadingMore = append, error = null)
        job = viewModelScope.launch {
            try {
                val result = requireApiData(api.reports(page = page, status = status))
                val list = if (append) _state.value.reports + result.items else result.items
                _state.value = _state.value.copy(
                    loading = false, loadingMore = false, reports = list.distinctBy { it.id },
                    nextPage = page + 1, hasNext = result.hasNext, error = null
                )
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, loadingMore = false, error = e.userMessage())
            }
        }
    }
}
