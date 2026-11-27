package com.hoangpha.cityguard.ui.profile

// TV2 - Trần Hữu Thịnh: Nối Profile với /me/summary; giữ state loading/error/data và cho thử lại khi backend tắt.

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
    application: Application,
    private val api: ProfileApi = ApiClient.profile(SessionManager(application))
) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(ProfileUiState())
    val state = _state.asStateFlow()
    private var job: Job? = null
    init { load() }
    // TV2: không gửi thêm request khi đang tải; lỗi backend chuyển sang error để Profile hiện nút Thử lại.
    fun load() {
        if (job?.isActive == true) return
        // TV2: làm mới ngầm giữ hồ sơ đã có, tránh tên/email và các hàng nhảy vị trí khi quay lại màn hình.
        _state.value = _state.value.copy(loading = true, error = null)
        job = viewModelScope.launch {
            try { _state.value = ProfileUiState(loading = false, profile = requireApiData(api.summary())) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _state.value = _state.value.copy(loading = false, error = e.userMessage()) }
        }
    }
}
