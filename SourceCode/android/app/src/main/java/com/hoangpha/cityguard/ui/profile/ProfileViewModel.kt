package com.hoangpha.cityguard.ui.profile

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

data class ProfileUiState(val loading: Boolean = true, val profile: ProfileSummaryDto? = null, val error: String? = null)
class ProfileViewModel @JvmOverloads constructor(
    //Runtime dùng ProfileApi thật; fake chỉ được truyền từ test.
    // Loading/error/retry giữ dữ liệu đã tải; chưa thay thế kiểm thử backend tắt trên thiết bị thật.
    application: Application,
    private val api: ProfileApi = ApiClient.profile(SessionManager(application))
) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(ProfileUiState())
    val state = _state.asStateFlow()
    private var job: Job? = null
    init { load() }
    fun load() {
        if (job?.isActive == true) return
        // làm mới ngầm giữ hồ sơ đã có, tránh tên/email và các hàng nhảy vị trí khi quay lại màn hình.
        _state.value = _state.value.copy(loading = true, error = null)
        job = viewModelScope.launch {
            try { _state.value = ProfileUiState(loading = false, profile = requireApiData(api.summary())) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _state.value = _state.value.copy(loading = false, error = e.userMessage()) }
        }
    }
}
