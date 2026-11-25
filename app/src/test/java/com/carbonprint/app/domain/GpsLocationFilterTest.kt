package com.carbonprint.app.domain

import com.carbonprint.app.domain.location.FilterResult
import com.carbonprint.app.domain.location.GpsLocationFilter
import com.carbonprint.app.domain.location.GpsPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GpsLocationFilterTest {

    @Test
    fun calculateDistance_identicalCoordinates_returnsZero() {
        val lat = 37.7749
        val lon = -122.4194
        val dist = GpsLocationFilter.calculateDistance(lat, lon, lat, lon)
        assertEquals(0.0, dist, 0.001)
    }

    @Test
    fun calculateDistance_knownTerrestrialCoordinates_isAccurate() {
        // London (51.5074, -0.1278) to Paris (48.8566, 2.3522) ≈ 343.5 km (343,500 m)
        val dist = GpsLocationFilter.calculateDistance(51.5074, -0.1278, 48.8566, 2.3522)
        assertEquals(343500.0, dist, 2000.0) // within 2km (0.5%)
    }

    @Test
    fun calculateDistance_invalidOrNaNCoordinates_returnsZeroSafely() {
        assertEquals(0.0, GpsLocationFilter.calculateDistance(Double.NaN, 0.0, 10.0, 10.0), 0.001)
        assertEquals(0.0, GpsLocationFilter.calculateDistance(10.0, 10.0, Double.POSITIVE_INFINITY, 0.0), 0.001)
    }

    @Test
    fun evaluate_firstPointInTrip_isAcceptedWithZeroDistance() {
        val firstPoint = GpsPoint(
            latitude = 37.7749,
            longitude = -122.4194,
            accuracyMeters = 10.0f,
            timestampMillis = 1000L,
            speedMps = 1.4f
        )
        val result = GpsLocationFilter.evaluate(firstPoint, previousValidPoint = null)
        assertTrue(result is FilterResult.Accepted)
        val accepted = result as FilterResult.Accepted
        assertEquals(0.0, accepted.distanceMeters, 0.001)
        assertEquals(1.4 * 3.6, accepted.calculatedSpeedKmh, 0.01)
    }

    @Test
    fun evaluate_inaccuratePoint_isRejected() {
        val previous = GpsPoint(37.7749, -122.4194, 10.0f, 1000L)
        val inaccuratePoint = GpsPoint(37.7750, -122.4194, 65.0f, 2000L) // accuracy 65m > 30m default

        val result = GpsLocationFilter.evaluate(inaccuratePoint, previous)
        assertTrue(result is FilterResult.RejectedInaccurate)
        val rejected = result as FilterResult.RejectedInaccurate
        assertEquals(65.0f, rejected.accuracyMeters, 0.01f)
    }

    @Test
    fun evaluate_negativeOrMissingAccuracy_isRejected() {
        val previous = GpsPoint(37.7749, -122.4194, 10.0f, 1000L)
        val negativePoint = GpsPoint(37.7750, -122.4194, -5.0f, 2000L)
        val nanPoint = GpsPoint(37.7750, -122.4194, Float.NaN, 2000L)

        assertTrue(GpsLocationFilter.evaluate(negativePoint, previous) is FilterResult.RejectedInaccurate)
        assertTrue(GpsLocationFilter.evaluate(nanPoint, previous) is FilterResult.RejectedInaccurate)
    }

    @Test
    fun evaluate_chronologicalTimeAnomaly_isRejected() {
        val previous = GpsPoint(37.7749, -122.4194, 10.0f, 5000L)
        val outOfOrderPoint = GpsPoint(37.7752, -122.4194, 10.0f, 4000L) // timestamp before previous!

        val result = GpsLocationFilter.evaluate(outOfOrderPoint, previous)
        assertTrue(result is FilterResult.RejectedTimeAnomaly)
    }

    @Test
    fun evaluate_stationaryGpsJitter_isSuppressed() {
        val previous = GpsPoint(37.774900, -122.419400, 5.0f, 1000L)
        // 0.000005 degrees latitude is ~0.5 meters (below 2.0m min distance threshold)
        val jitterPoint = GpsPoint(37.774905, -122.419400, 5.0f, 3000L)

        val result = GpsLocationFilter.evaluate(jitterPoint, previous)
        assertTrue(result is FilterResult.RejectedJitter)
    }

    @Test
    fun evaluate_implausibleSpeedTeleport_isRejected() {
        val previous = GpsPoint(37.7749, -122.4194, 10.0f, 1000L)
        // 1 second later, 1 kilometer away = 3600 km/h (teleportation bounce)
        val teleportPoint = GpsPoint(37.7839, -122.4194, 10.0f, 2000L)

        val result = GpsLocationFilter.evaluate(teleportPoint, previous)
        assertTrue(result is FilterResult.RejectedImplausibleSpeed)
        val rejected = result as FilterResult.RejectedImplausibleSpeed
        assertTrue(rejected.calculatedSpeedKmh > 220.0)
    }

    @Test
    fun evaluate_plausibleMovement_isAccepted() {
        val previous = GpsPoint(37.774900, -122.419400, 8.0f, 1000L)
        // Move ~15 meters in 5 seconds = 3.0 m/s = 10.8 km/h (normal jogging pace)
        val candidate = GpsPoint(37.775035, -122.419400, 8.0f, 6000L)

        val result = GpsLocationFilter.evaluate(candidate, previous)
        assertTrue(result is FilterResult.Accepted)
        val accepted = result as FilterResult.Accepted
        assertTrue("Distance should be ~15m", accepted.distanceMeters in 14.0..16.0)
        assertTrue("Speed should be ~10.8 km/h", accepted.calculatedSpeedKmh in 10.0..12.0)
    }
}
