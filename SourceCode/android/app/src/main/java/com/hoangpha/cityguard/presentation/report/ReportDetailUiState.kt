package com.hoangpha.cityguard.presentation.report

import com.hoangpha.cityguard.domain.model.Report

data class ReportDetailUiState(
    val isLoading: Boolean = true,        // true cho tới khi có dữ liệu đầu tiên
    val report: Report? = null,
    val notFound: Boolean = false,        // đã tải xong nhưng không có report với ID này
    val commentText: String = "",
    val commentError: String? = null,
    val isVoting: Boolean = false,
    val isSendingComment: Boolean = false,
    val errorMessage: String? = null
) {
    companion object {
        const val MAX_COMMENT_LENGTH = 300
    }
}