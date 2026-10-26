package com.hoangpha.cityguard.utils

import androidx.compose.ui.graphics.Color

data class ReportMock(
    val id: String,
    val title: String,
    val location: String,
    val time: String,
    val status: String,
    val statusColor: Color,
    val confirmCount: Int,
    val noLongerCount: Int
)