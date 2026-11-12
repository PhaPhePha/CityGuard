package com.hoangpha.cityguard.presentation.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoangpha.cityguard.data.mock.FakeReportRepository
import com.hoangpha.cityguard.domain.repository.ReportRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CommunityViewModel @JvmOverloads constructor(
    // Hôm nay là dữ liệu giả; khi có API chỉ đổi đối tượng này
    private val repository: ReportRepository = FakeReportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommunityUiState())
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    // Gọi lần đầu và khi bấm "Thử lại"
    fun load() {
        loadJob?.cancel()                                   // hủy lượt tải cũ, tránh hai luồng chạy song song
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        loadJob = viewModelScope.launch {
            repository.reports
                .catch {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Không tải được danh sách. Vui lòng thử lại.")
                    }
                }
                .collect { list ->
                    // Tạo report mới hay vote ở màn Chi tiết thì danh sách tự cập nhật
                    _uiState.update { it.copy(isLoading = false, reports = list, errorMessage = null) }
                }
        }
    }
}