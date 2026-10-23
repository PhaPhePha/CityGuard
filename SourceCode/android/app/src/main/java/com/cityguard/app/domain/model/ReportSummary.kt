package com.cityguard.app.domain.model

enum class IncidentCategory(val displayName: String) {
    TRAFFIC_JAM("Ùn tắc giao thông"),
    ROAD_BLOCK("Đường bị chặn"),
    ROAD_DAMAGE("Đường hư hỏng"),
    BROKEN_TRAFFIC_LIGHT("Đèn giao thông hỏng"),
    FALLEN_TREE("Cây đổ"),
    FLOODING("Ngập nước")
}

data class ReportSummary(
    val id: String,
    val title: String,
    val description: String,
    val category: IncidentCategory,
    val address: String,
    val latitude: Double,
    val longitude: Double
)