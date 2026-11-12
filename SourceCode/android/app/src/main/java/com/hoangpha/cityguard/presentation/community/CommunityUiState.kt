package com.hoangpha.cityguard.presentation.community

import com.hoangpha.cityguard.domain.model.Report

data class CommunityUiState(
    val isLoading: Boolean = true,
    val reports: List<Report> = emptyList(),
    val errorMessage: String? = null      // có giá trị = tải thất bại, màn hình hiện nút Thử lại
)