package com.hoangpha.cityguard.presentation.report

import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.hoangpha.cityguard.data.mock.FakeReportRepository
import com.hoangpha.cityguard.domain.model.IncidentCategory
import com.hoangpha.cityguard.domain.model.Report
import com.hoangpha.cityguard.domain.repository.NewReport
import com.hoangpha.cityguard.domain.repository.ReportRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class CreateReportViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    // Repository đếm số lần createReport được gọi
    private class CountingRepository : ReportRepository by FakeReportRepository {
        var createCalls = 0
        override suspend fun createReport(newReport: NewReport): Report {
            createCalls++
            return FakeReportRepository.createReport(newReport)
        }
    }

    // Repository lỗi ở lần đầu (giả lập mất mạng) và ghi lại requestId mỗi lần gọi
    private class FlakyRepository : ReportRepository by FakeReportRepository {
        val requestIds = mutableListOf<String>()
        private var shouldFail = true
        override suspend fun createReport(newReport: NewReport): Report {
            requestIds += newReport.requestId
            if (shouldFail) {
                shouldFail = false
                throw RuntimeException("mất mạng")
            }
            return FakeReportRepository.createReport(newReport)
        }
    }

    private fun CreateReportViewModel.fillValidForm() {
        onCategorySelected(IncidentCategory.TRAFFIC_JAM)
        onDescriptionChanged("Kẹt xe kéo dài")
        onAddressChanged("Võ Oanh, Bình Thạnh")
    }

    @Test
    fun submitWithoutCategoryShowsErrorAndDoesNotCallRepository() = runTest {
        val repo = CountingRepository()
        val vm = CreateReportViewModel(repo)
        vm.onDescriptionChanged("mô tả")
        vm.onAddressChanged("địa chỉ")

        vm.submit()
        advanceUntilIdle()

        assertEquals("Vui lòng chọn loại sự cố", vm.uiState.value.errorMessage)
        assertEquals(0, repo.createCalls)
    }

    @Test
    fun descriptionLongerThan150IsRejected() = runTest {
        val vm = CreateReportViewModel(CountingRepository())
        vm.onDescriptionChanged("a".repeat(150))
        vm.onDescriptionChanged("a".repeat(151))
        assertEquals(150, vm.uiState.value.description.length)
    }

    @Test
    fun doubleSubmitCreatesOnlyOneReport() = runTest {
        val repo = CountingRepository()
        val vm = CreateReportViewModel(repo)
        vm.fillValidForm()

        vm.submit()
        vm.submit()          // bấm lần hai khi lần một còn đang gửi
        advanceUntilIdle()

        assertEquals(1, repo.createCalls)
        assertNotNull(vm.uiState.value.createdReportId)
    }

    @Test
    fun retryAfterFailureReusesSameRequestId() = runTest {
        val repo = FlakyRepository()
        val vm = CreateReportViewModel(repo)
        vm.fillValidForm()

        vm.submit()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isSubmitting)
        assertNull(vm.uiState.value.createdReportId)

        vm.submit()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.createdReportId)
        assertEquals(2, repo.requestIds.size)
        assertEquals(repo.requestIds[0], repo.requestIds[1])
    }

    @Test
    fun sameRequestIdCreatesOnlyOneReportInRepository() = runTest {
        val request = NewReport(
            requestId = UUID.randomUUID().toString(),
            category = IncidentCategory.FLOODING,
            description = "ngập",
            address = "Quận 1",
            imageUri = null
        )
        val first = FakeReportRepository.createReport(request)
        val second = FakeReportRepository.createReport(request)

        assertEquals(first.id, second.id)
        val count = FakeReportRepository.reports.first().count { it.id == first.id }
        assertEquals(1, count)
    }
}