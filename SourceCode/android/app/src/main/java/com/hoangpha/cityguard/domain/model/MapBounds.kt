package com.hoangpha.cityguard.domain.model

data class MapBounds(
    val south: Double,
    val west: Double,
    val north: Double,
    val east: Double
) {
    init {
        require(south in -90.0..90.0)
        require(north in -90.0..90.0)
        require(west in -180.0..180.0)
        require(east in -180.0..180.0)

        require(south <= north)
        require(west <= east)
    }

    fun contains(
        latitude: Double,
        longitude: Double
    ): Boolean {
        return latitude in south..north &&
                longitude in west..east
    }

    fun toApiQuery(): String {
        return "$west,$south,$east,$north"
    }
}