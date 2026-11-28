
package com.hoangpha.cityguard.presentation.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoangpha.cityguard.data.RepositoryProvider
import com.hoangpha.cityguard.domain.repository.ReportRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CommunityViewModel @JvmOverloads constructor(
    private val repository: ReportRepository =
        RepositoryProvider.reportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommunityUiState())
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var loadMoreJob: Job? = null

    // Xử lý khi người dùng chuyển tab
    fun onTabSelected(tab: CommunityTab) {
        if (_uiState.value.selectedTab == tab) return

        _uiState.update {
            it.copy(selectedTab = tab)
        }

        load()
    }

    // Tải trang đầu tiên hoặc tải lại danh sách
    fun load() {
        android.util.Log.d(
            "CommunityRefresh",
            "LOAD CALLED - ViewModel: ${this.hashCode()}"
        )

        // Hủy các request cũ
        loadJob?.cancel()
        loadMoreJob?.cancel()

        val recent =
            _uiState.value.selectedTab == CommunityTab.RECENT

        // Đặt lại trạng thái phân trang
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                currentPage = 0,
                hasNextPage = false,
                isLoadingMore = false,
                reports = emptyList()
            )
        }

        loadJob = viewModelScope.launch {
            try {
                val result = repository.getCommunityReports(
                    page = 0,
                    size = 20,
                    recent = recent
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        reports = result.items,
                        currentPage = result.page,
                        hasNextPage = result.hasNext,
                        errorMessage = null
                    )
                }

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage =
                            "Không tải được danh sách. Vui lòng thử lại."
                    )
                }
            }
        }
    }

    // Tải trang tiếp theo khi người dùng cuộn xuống
    fun loadNextPage() {
        val currentState = _uiState.value

        // Không tải thêm nếu đang có request
        if (currentState.isLoading || currentState.isLoadingMore) return

        // Không tải nếu đã hết dữ liệu
        if (!currentState.hasNextPage) return

        val nextPage = currentState.currentPage + 1
        val recent = currentState.selectedTab == CommunityTab.RECENT

        // Đánh dấu đang tải trang tiếp theo
        _uiState.update {
            it.copy(isLoadingMore = true)
        }

        loadMoreJob = viewModelScope.launch {
            try {
                val result = repository.getCommunityReports(
                    page = nextPage,
                    size = 20,
                    recent = recent
                )

                _uiState.update { state ->

                    // Không cập nhật dữ liệu nếu người dùng đã chuyển tab
                    if (state.selectedTab != currentState.selectedTab) {
                        state.copy(isLoadingMore = false)

                    } else {
                        // Nối dữ liệu mới và loại bỏ báo cáo trùng ID
                        val mergedReports =
                            (state.reports + result.items)
                                .distinctBy { it.id }

                        state.copy(
                            reports = mergedReports,
                            currentPage = result.page,
                            hasNextPage = result.hasNext,
                            isLoadingMore = false
                        )
                    }
                }

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoadingMore = false)
                }
            }
        }
    }
}
