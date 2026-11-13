package com.hoangpha.cityguard.presentation.report

import androidx.lifecycle.SavedStateHandle
import com.hoangpha.cityguard.data.mock.FakeReportRepository
import com.hoangpha.cityguard.domain.model.IncidentCategory
import com.hoangpha.cityguard.domain.model.VoteType
import com.hoangpha.cityguard.domain.repository.NewReport
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class ReportDetailViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private fun detailViewModel(reportId: String) =
        ReportDetailViewModel(SavedStateHandle(mapOf("reportId" to reportId)))

    // Mỗi test tự tạo một report riêng để không bị ảnh hưởng bởi test khác
    private suspend fun newReportId(): String =
        FakeReportRepository.createReport(
            NewReport(UUID.randomUUID().toString(), IncidentCategory.ROAD_DAMAGE, "ổ gà", "Quận 3", null)
        ).id

    @Test
    fun unknownIdShowsNotFound() = runTest {
        val vm = detailViewModel("KHONG_TON_TAI")
        assertTrue(vm.uiState.value.notFound)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun votingTwiceWithSameChoiceCountsOnce() = runTest {
        val vm = detailViewModel(newReportId())
        vm.vote(VoteType.CONFIRMED)
        vm.vote(VoteType.CONFIRMED)
        assertEquals(1, vm.uiState.value.report!!.confirmCount)
    }

    @Test
    fun changingVoteMovesCountToOtherSide() = runTest {
        val vm = detailViewModel(newReportId())
        vm.vote(VoteType.CONFIRMED)
        vm.vote(VoteType.NO_LONGER)

        val report = vm.uiState.value.report!!
        assertEquals(0, report.confirmCount)
        assertEquals(1, report.noLongerCount)
        assertEquals(VoteType.NO_LONGER, report.myVote)
    }

    @Test
    fun blankCommentShowsErrorAndIsNotAdded() = runTest {
        val vm = detailViewModel(newReportId())
        vm.onCommentChanged("   ")
        vm.sendComment()

        assertEquals("Bình luận không được để trống", vm.uiState.value.commentError)
        assertTrue(vm.uiState.value.report!!.comments.isEmpty())
    }

    @Test
    fun validCommentIsAddedAndInputCleared() = runTest {
        val vm = detailViewModel(newReportId())
        vm.onCommentChanged("  Vẫn còn kẹt  ")
        vm.sendComment()

        assertEquals("Vẫn còn kẹt", vm.uiState.value.report!!.comments.last().content)
        assertEquals("", vm.uiState.value.commentText)
    }
}