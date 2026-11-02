
package com.cityguard.app.domain.model

enum class IncidentCategory(
    val displayName: String
) {
    TRAFFIC_JAM("Kẹt xe"),
    FLOODING("Ngập nước"),
    ROAD_DAMAGE("Đường hư"),
    ROAD_OBSTRUCTION("Có vật cản")
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
