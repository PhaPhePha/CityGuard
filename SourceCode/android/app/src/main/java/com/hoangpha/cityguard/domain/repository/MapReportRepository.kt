package com.hoangpha.cityguard.domain.repository

import com.hoangpha.cityguard.domain.model.Report

interface MapReportRepository {

    suspend fun getReportsForMap(
        bbox: String? = null,
        type: String? = null,
        status: String? = null
    ): List<Report>
}