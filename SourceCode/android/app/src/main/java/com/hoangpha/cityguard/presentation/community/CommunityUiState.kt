package com.hoangpha.cityguard.presentation.community

import com.hoangpha.cityguard.domain.model.Report

enum class CommunityTab { NEARBY, NEWEST }

data class CommunityUiState(
    val isLoading: Boolean = true,
    val reports: List<Report> = emptyList(),
    val selectedTab: CommunityTab = CommunityTab.NEARBY,
    val errorMessage: String? = null
) {
    // Danh sách thực sự hiển thị theo tab đang chọn
    val visibleReports: List<Report>
        get() = when (selectedTab) {
            // "Gần đây" cần vị trí người dùng nên tạm giữ thứ tự repository trả về
            CommunityTab.NEARBY -> reports
            CommunityTab.NEWEST -> reports.sortedByDescending { it.createdAtMillis }
        }
}