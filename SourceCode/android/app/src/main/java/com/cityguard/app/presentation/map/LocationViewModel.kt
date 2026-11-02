package com.cityguard.app.presentation.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cityguard.app.domain.location.LocationProvider
import com.cityguard.app.domain.location.LocationResult
import kotlinx.coroutines.launch

class LocationViewModel(
    private val locationProvider: LocationProvider
) : ViewModel() {
// mutableStateOf() làm Compose nhận biết khi dữ liệu thay đổi để cập nhật UI.
    var uiState: LocationUiState by mutableStateOf(
        LocationUiState.Idle
    )
    // private set giúp những lớp khác đọc uiState nhưng không tự do thay đổi trạng thái.
        private set

    private var nextRequestId = 0

    fun locate() {
        if (uiState == LocationUiState.Loading) return
// viewModelScope.launch {} khởi chạy coroutine gắn với vòng đời ViewModel, giúp tác vụ không bị giữ lại vô hạn khi ViewModel bị hủy.
        viewModelScope.launch {
            uiState = LocationUiState.Loading

            val result = locationProvider.getCurrentLocation()

            nextRequestId++
            uiState = LocationUiState.Finished(
                result = result,
                requestId = nextRequestId
            )
        }
    }

    fun onPermissionDenied() {
        nextRequestId++
        uiState = LocationUiState.Finished(
            result = LocationResult.PermissionDenied,
            requestId = nextRequestId
        )
    }

    companion object {
        // factory() tạo ViewModel với LocationProvider được truyền từ ngoài vào. Đây là một hình thức Dependency Injection đơn giản, chưa cần dùng Hilt.
        fun factory(
            provider: LocationProvider
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T {
                    require(
                        modelClass.isAssignableFrom(
                            LocationViewModel::class.java
                        )
                    )
                    return LocationViewModel(provider) as T
                }
            }
        }
    }
}
