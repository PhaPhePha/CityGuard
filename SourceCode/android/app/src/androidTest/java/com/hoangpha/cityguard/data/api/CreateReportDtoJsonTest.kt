package com.hoangpha.cityguard.data.api

import com.google.gson.Gson
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateReportDtoJsonTest {

    @Test
    fun jsonUsesClientRequestIdKeyExpectedByBackend() {
        val dto = CreateReportDto(
            clientRequestId = "c3bfa499-45cf-4791-b9b4-0006622463a5",
            type = "TRAFFIC_JAM",
            description = "TEST",
            addressSnapshot = "Quận 1",
            latitude = 10.0,
            longitude = 106.0,
            imageUrl = null
        )

        val json = Gson().toJson(dto)

        // Backend (CreateReportRequest.java) chỉ đọc khóa "clientRequestId"
        assertTrue(json, json.contains("\"clientRequestId\":\"c3bfa499-45cf-4791-b9b4-0006622463a5\""))
        assertFalse(json, json.contains("\"requestId\""))
    }
}