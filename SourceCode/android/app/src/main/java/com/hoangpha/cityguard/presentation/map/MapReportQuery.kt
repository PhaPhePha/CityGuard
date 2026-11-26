package com.hoangpha.cityguard.presentation.map

import com.hoangpha.cityguard.domain.model.MapBounds

data class MapReportQuery(
    val bounds: MapBounds,
    val filters: MapFilters
)