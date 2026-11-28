package com.hoangpha.cityguard.domain.model

data class CommunityReportPage(
    val items: List<Report>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean
) {
    companion object {
        const val RECENT_WINDOW_MILLIS = 24L * 60 * 60 * 1000
    }
}