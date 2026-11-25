package com.carbonprint.app.domain.location

/**
 * Encapsulates raw GPS location coordinates and metadata for filtering and distance calculation.
 */
data class GpsPoint(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val timestampMillis: Long,
    val speedMps: Float? = null,
    val altitudeMeters: Double? = null
)

/**
 * Represents the outcome of filtering a candidate GPS point against tracking heuristics.
 */
sealed class FilterResult {
    data class Accepted(
        val distanceMeters: Double,
        val calculatedSpeedKmh: Double,
        val point: GpsPoint
    ) : FilterResult()

    data class RejectedInaccurate(
        val accuracyMeters: Float,
        val thresholdMeters: Float
    ) : FilterResult()

    data class RejectedTimeAnomaly(
        val deltaMillis: Long
    ) : FilterResult()

    data class RejectedJitter(
        val distanceMeters: Double,
        val thresholdMeters: Double
    ) : FilterResult()

    data class RejectedImplausibleSpeed(
        val calculatedSpeedKmh: Double,
        val maxSpeedKmh: Double,
        val distanceMeters: Double
    ) : FilterResult()
}

/**
 * Pure Kotlin GPS filtering and geodesic distance calculator.
 * Independent of Android framework classes, ensuring complete JVM unit-testability.
 */
object GpsLocationFilter {

    const val DEFAULT_MAX_ACCURACY_METERS = 30.0f
    const val DEFAULT_MAX_SPEED_KMH = 220.0
    const val DEFAULT_MIN_DISTANCE_METERS = 2.0
    const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Calculates the great-circle distance between two geographic coordinates using the Haversine formula.
     * Accurate within 0.5% worldwide across terrestrial distances.
     *
     * @return Distance in meters (non-negative).
     */
    fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        if (!lat1.isFinite() || !lon1.isFinite() || !lat2.isFinite() || !lon2.isFinite()) return 0.0

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0)
        val c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a))
        val distance = EARTH_RADIUS_METERS * c

        return if (distance.isFinite() && distance >= 0.0) distance else 0.0
    }

    /**
     * Evaluates a candidate GPS point against tracking heuristics.
     *
     * @param candidate Incoming raw GPS point.
     * @param previousValidPoint Last verified valid point in current trip, or null if first fix.
     * @param maxAccuracyMeters Accuracy threshold (readings with larger error circles are rejected).
     * @param maxSpeedKmh Speed threshold above which point is flagged as GPS bounce/teleportation.
     * @param minDistanceMeters Minimum movement threshold to suppress stationary jitter.
     * @return [FilterResult] indicating acceptance or specific rejection reason.
     */
    fun evaluate(
        candidate: GpsPoint,
        previousValidPoint: GpsPoint?,
        maxAccuracyMeters: Float = DEFAULT_MAX_ACCURACY_METERS,
        maxSpeedKmh: Double = DEFAULT_MAX_SPEED_KMH,
        minDistanceMeters: Double = DEFAULT_MIN_DISTANCE_METERS
    ): FilterResult {
        // 1. Accuracy Check: Reject missing, negative, or excessively inaccurate GPS fixes
        if (!candidate.accuracyMeters.isFinite() ||
            candidate.accuracyMeters <= 0f ||
            candidate.accuracyMeters > maxAccuracyMeters
        ) {
            return FilterResult.RejectedInaccurate(candidate.accuracyMeters, maxAccuracyMeters)
        }

        // 2. Coordinate range validation
        if (candidate.latitude !in -90.0..90.0 || candidate.longitude !in -180.0..180.0) {
            return FilterResult.RejectedInaccurate(candidate.accuracyMeters, maxAccuracyMeters)
        }

        // 3. First point in trip: Accept immediately with zero distance delta
        if (previousValidPoint == null) {
            val initialSpeed = candidate.speedMps?.let { if (it >= 0f) it * 3.6 else 0.0 } ?: 0.0
            return FilterResult.Accepted(0.0, initialSpeed, candidate)
        }

        // 4. Chronological Time Anomaly Check: Reject out-of-order or duplicate timestamps
        val timeDeltaMillis = candidate.timestampMillis - previousValidPoint.timestampMillis
        if (timeDeltaMillis <= 0) {
            return FilterResult.RejectedTimeAnomaly(timeDeltaMillis)
        }

        // 5. Geodesic Distance Calculation
        val distance = calculateDistance(
            previousValidPoint.latitude, previousValidPoint.longitude,
            candidate.latitude, candidate.longitude
        )

        // 6. Stationary Jitter Suppression: Ignore tiny drifts under threshold
        if (distance < minDistanceMeters) {
            return FilterResult.RejectedJitter(distance, minDistanceMeters)
        }

        // 7. Implausible Speed / GPS Teleportation Check
        val timeDeltaSeconds = timeDeltaMillis / 1000.0
        val speedMps = distance / timeDeltaSeconds
        val speedKmh = speedMps * 3.6

        if (speedKmh > maxSpeedKmh) {
            return FilterResult.RejectedImplausibleSpeed(speedKmh, maxSpeedKmh, distance)
        }

        return FilterResult.Accepted(distance, speedKmh, candidate)
    }
}
