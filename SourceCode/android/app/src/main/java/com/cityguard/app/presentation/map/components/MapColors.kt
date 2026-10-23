package com.cityguard.app.presentation.map.components

import androidx.compose.ui.graphics.Color
import com.cityguard.app.domain.model.IncidentCategory

// Tạm lấy màu gần đúng từ ảnh Figma. Khi leader chốt token, chuyển về theme chung.
internal object MapColors {
    val HeaderYellow = Color(0xFFFFC638)
    val BottomYellow = Color(0xFFFFDD83)
    val Sand = Color(0xFFF9F3E4)
    val Road = Color(0xFFD5DEE1)
    val MainRoad = Color(0xFFA9B9C7)
    val Water = Color(0xFF9ADBEA)
    val Park = Color(0xFFBCE9BA)
    val Dark = Color(0xFF252525)
    val Red = Color(0xFFED3349)
    val Blue = Color(0xFF229ADE)
    val Green = Color(0xFF28A65F)
    val Amber = Color(0xFFF0B520)
}

internal fun IncidentCategory.markerColor(): Color = when (this) {
    IncidentCategory.TRAFFIC_JAM -> MapColors.Red
    IncidentCategory.ROAD_BLOCK -> MapColors.Amber
    IncidentCategory.ROAD_DAMAGE -> Color(0xFF8A687F)
    IncidentCategory.BROKEN_TRAFFIC_LIGHT -> Color(0xFFB56DCE)
    IncidentCategory.FALLEN_TREE -> MapColors.Green
    IncidentCategory.FLOODING -> MapColors.Blue
}

internal fun IncidentCategory.markerSymbol(): String = when (this) {
    IncidentCategory.TRAFFIC_JAM -> "🚗"
    IncidentCategory.ROAD_BLOCK -> "⛔"
    IncidentCategory.ROAD_DAMAGE -> "⚠"
    IncidentCategory.BROKEN_TRAFFIC_LIGHT -> "🚦"
    IncidentCategory.FALLEN_TREE -> "🌳"
    IncidentCategory.FLOODING -> "💧"
}
