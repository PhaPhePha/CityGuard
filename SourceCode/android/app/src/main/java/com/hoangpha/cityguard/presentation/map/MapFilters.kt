package com.hoangpha.cityguard.presentation.map

import com.hoangpha.cityguard.domain.model.IncidentCategory

enum class MapReportStatus(
    val displayName: String
) {
    PENDING_VERIFY("Đang xác minh"),
    ACTIVE("Đang diễn ra")
}

data class MapFilters(
    val category: IncidentCategory? = null,
    val status: MapReportStatus? = null
) {

    fun toApiType(): String? {
        return when (category) {
            IncidentCategory.TRAFFIC_JAM -> "TRAFFIC_JAM"
            IncidentCategory.FLOODING -> "FLOOD"
            IncidentCategory.ROAD_DAMAGE -> "ROAD_DAMAGE"
            IncidentCategory.ROAD_OBSTRUCTION -> "ROAD_BLOCK"
            null -> null
        }
    }

    fun toApiStatus(): String? {
        return status?.name
    }
}