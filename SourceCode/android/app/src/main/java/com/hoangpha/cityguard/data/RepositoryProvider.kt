package com.hoangpha.cityguard.data

import com.hoangpha.cityguard.data.api.ApiClient
import com.hoangpha.cityguard.data.api.ApiReportRepository
import com.hoangpha.cityguard.data.local.SessionManager
import com.hoangpha.cityguard.data.mock.FakeReportRepository
import com.hoangpha.cityguard.domain.repository.ReportRepository
import com.hoangpha.cityguard.domain.repository.MapReportRepository

object RepositoryProvider {
    @Volatile private var current: ReportRepository = FakeReportRepository
    val reportRepository: ReportRepository get() = current
    fun initialize(sessionManager: SessionManager) { current = ApiReportRepository(ApiClient.reports(sessionManager)) }

    val mapReportRepository: MapReportRepository
        get() = checkNotNull(current as? MapReportRepository) {
            "MapReportRepository has not been initialized"
        }
}
