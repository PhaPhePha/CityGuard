package com.hoangpha.cityguard.presentation.community

import com.hoangpha.cityguard.domain.model.Report

enum class CommunityTab { RECENT, NEWEST }

data class CommunityUiState(
    val isLoading: Boolean = true,
    val reports: List<Report> = emptyList(),
    val currentPage: Int = 0,
    val hasNextPage: Boolean = false,
    val isLoadingMore: Boolean = false,
    val selectedTab: CommunityTab = CommunityTab.RECENT,
    val errorMessage: String? = null
) {
    // Danh sách thực sự hiển thị theo tab đang chọn. Mới nhất luôn ở trên cùng.
    // Tham số nowMillis để test được mà không phụ thuộc đồng hồ thật.
    fun visibleReports(): List<Report> {
        return reports.sortedByDescending { it.createdAtMillis }
    }
    companion object {
        const val RECENT_WINDOW_MILLIS = 24L * 60 * 60 * 1000   // 24 giờ
    }
}