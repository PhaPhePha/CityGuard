package com.hoangpha.cityguard.presentation.community

import com.hoangpha.cityguard.domain.model.IncidentCategory
import com.hoangpha.cityguard.domain.model.Report
import org.junit.Assert.assertEquals
import org.junit.Test

class CommunityUiStateTest {

    private val now = 1_000_000_000L
    private val minute = 60_000L
    private val hour = 60 * minute

    private fun report(id: String, ageMillis: Long) = Report(
        id = id,
        category = IncidentCategory.TRAFFIC_JAM,
        description = "test",
        address = "Quận 1",
        createdAtLabel = "",
        trustLabel = "Cao",
        confirmCount = 0,
        noLongerCount = 0,
        myVote = null,
        createdAtMillis = now - ageMillis
    )

    private val reports = listOf(
        report("cu", 30 * hour),          // 30 giờ trước
        report("moi", 5 * minute),        // 5 phút trước
        report("giua", 2 * hour)          // 2 giờ trước
    )

    @Test
    fun newestTabShowsAllReportsNewestFirst() {
        val state = CommunityUiState(isLoading = false, reports = reports, selectedTab = CommunityTab.NEWEST)
        assertEquals(listOf("moi", "giua", "cu"), state.visibleReports(now).map { it.id })
    }

    @Test
    fun recentTabHidesReportsOlderThan24HoursAndSortsNewestFirst() {
        val state = CommunityUiState(isLoading = false, reports = reports, selectedTab = CommunityTab.RECENT)
        assertEquals(listOf("moi", "giua"), state.visibleReports(now).map { it.id })
    }
}