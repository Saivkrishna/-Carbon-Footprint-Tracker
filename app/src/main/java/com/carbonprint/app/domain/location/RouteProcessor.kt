package com.carbonprint.app.domain.location

import com.carbonprint.app.data.local.entity.WaypointEntity

/**
 * Representation of a validated geographic coordinate along a journey route.
 */
data class RoutePoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val speedMps: Float = 0f,
    val timestamp: Long = 0L
)

/**
 * Geographic rectangular bounding box encompassing all points of a route.
 */
data class RouteBoundingBox(
    val minLatitude: Double,
    val maxLatitude: Double,
    val minLongitude: Double,
    val maxLongitude: Double
) {
    val centerLatitude: Double get() = (minLatitude + maxLatitude) / 2.0
    val centerLongitude: Double get() = (minLongitude + maxLongitude) / 2.0
}

/**
 * Processed and validated route data ready for map rendering and metric display.
 */
data class ProcessedRoute(
    val points: List<RoutePoint>,
    val startPoint: RoutePoint?,
    val destinationPoint: RoutePoint?,
    val boundingBox: RouteBoundingBox?,
    val totalWaypointsCount: Int
) {
    val hasValidRoute: Boolean get() = points.isNotEmpty()
    val isMultiPointRoute: Boolean get() = points.size >= 2
}

/**
 * Processes, validates, and prepares recorded GPS waypoints for polyline map rendering.
 */
object RouteProcessor {

    /**
     * Filters invalid coordinates, sorts chronologically, and calculates route boundaries.
     */
    fun processWaypoints(rawWaypoints: List<WaypointEntity>?): ProcessedRoute {
        if (rawWaypoints.isNullOrEmpty()) {
            return ProcessedRoute(
                points = emptyList(),
                startPoint = null,
                destinationPoint = null,
                boundingBox = null,
                totalWaypointsCount = 0
            )
        }

        // 1. Filter out invalid/NaN/out-of-range and (0,0) sentinel coordinates
        val validPoints = rawWaypoints
            .filter { isValidCoordinate(it.latitude, it.longitude) }
            .sortedBy { it.timestamp } // 2. Enforce strict chronological order
            .map {
                RoutePoint(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    altitude = it.altitude,
                    speedMps = it.speedMps,
                    timestamp = it.timestamp
                )
            }

        if (validPoints.isEmpty()) {
            return ProcessedRoute(
                points = emptyList(),
                startPoint = null,
                destinationPoint = null,
                boundingBox = null,
                totalWaypointsCount = rawWaypoints.size
            )
        }

        val start = validPoints.first()
        val dest = validPoints.last()

        var minLat = Double.MAX_VALUE
        var maxLat = -Double.MAX_VALUE
        var minLon = Double.MAX_VALUE
        var maxLon = -Double.MAX_VALUE

        for (p in validPoints) {
            if (p.latitude < minLat) minLat = p.latitude
            if (p.latitude > maxLat) maxLat = p.latitude
            if (p.longitude < minLon) minLon = p.longitude
            if (p.longitude > maxLon) maxLon = p.longitude
        }

        val bbox = RouteBoundingBox(
            minLatitude = minLat,
            maxLatitude = maxLat,
            minLongitude = minLon,
            maxLongitude = maxLon
        )

        return ProcessedRoute(
            points = validPoints,
            startPoint = start,
            destinationPoint = dest,
            boundingBox = bbox,
            totalWaypointsCount = rawWaypoints.size
        )
    }

    /**
     * Validates that coordinates fall within valid terrestrial latitudes and longitudes.
     * Rejects NaN, Infinite, and (0.0, 0.0) null-island artifact coordinates.
     */
    fun isValidCoordinate(latitude: Double, longitude: Double): Boolean {
        return latitude.isFinite() && longitude.isFinite() &&
                latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
                !(Math.abs(latitude) < 0.000001 && Math.abs(longitude) < 0.000001)
    }
}
