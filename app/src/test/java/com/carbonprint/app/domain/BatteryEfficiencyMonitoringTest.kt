package com.carbonprint.app.domain

import com.carbonprint.app.data.local.entity.TripEntity
import com.carbonprint.app.domain.battery.BatteryMonitor
import com.carbonprint.app.domain.model.TransportMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests validating battery efficiency calculations, percentage decreases,
 * drain rates, and daily/weekly aggregations.
 */
class BatteryEfficiencyMonitoringTest {

    @Test
    fun calculateBatteryDrop_validReadings_computesCorrectDecrease() {
        val startPct = 85
        val endPct = 82
        val drop = BatteryMonitor.calculateBatteryDrop(startPct, endPct)
        assertEquals(3, drop)
    }

    @Test
    fun calculateBatteryDrop_chargingDuringTrip_clampsDropToZero() {
        val startPct = 50
        val endPct = 65 // Plugged in during tracking
        val drop = BatteryMonitor.calculateBatteryDrop(startPct, endPct)
        assertEquals(0, drop)
    }

    @Test
    fun calculateBatteryDrop_nullReadings_returnsNullSafely() {
        assertNull(BatteryMonitor.calculateBatteryDrop(null, 80))
        assertNull(BatteryMonitor.calculateBatteryDrop(80, null))
        assertNull(BatteryMonitor.calculateBatteryDrop(null, null))
    }

    @Test
    fun calculateDrainRatePerHour_computesAccurateHourlyDischarge() {
        // 3% drop over 1800 seconds (0.5 hour) -> 6.0% per hour
        val rate = BatteryMonitor.calculateDrainRatePerHour(dropPct = 3, durationSeconds = 1800L)
        assertNotNull(rate)
        assertEquals(6.0, rate!!, 0.01)

        // 2% drop over 3600 seconds (1 hour) -> 2.0% per hour
        val rate1Hour = BatteryMonitor.calculateDrainRatePerHour(dropPct = 2, durationSeconds = 3600L)
        assertEquals(2.0, rate1Hour!!, 0.01)

        // Invalid duration returns null
        assertNull(BatteryMonitor.calculateDrainRatePerHour(dropPct = 2, durationSeconds = 0L))
        assertNull(BatteryMonitor.calculateDrainRatePerHour(dropPct = null, durationSeconds = 3600L))
    }

    @Test
    fun computeBatterySummary_aggregatesLatestDailyAndWeeklyStats() {
        val now = System.currentTimeMillis()
        val trips = listOf(
            TripEntity(
                id = 1L,
                startTime = now - 3600000L, // 1 hour ago
                endTime = now - 1800000L,
                mode = TransportMode.WALKING,
                durationSeconds = 1800L,
                isCompleted = true,
                startBatteryPct = 90,
                endBatteryPct = 88,
                batteryDropPct = 2
            ),
            TripEntity(
                id = 2L,
                startTime = now - 1200000L, // 20 mins ago
                endTime = now - 600000L,
                mode = TransportMode.RUNNING,
                durationSeconds = 600L,
                isCompleted = true,
                startBatteryPct = 88,
                endBatteryPct = 87,
                batteryDropPct = 1
            )
        )

        val summary = BatteryMonitor.computeBatterySummary(trips)

        // Latest trip (Trip 2)
        assertEquals(88, summary.latestStartPct)
        assertEquals(87, summary.latestEndPct)
        assertEquals(1, summary.latestDropPct)

        // Today's aggregate: 2 + 1 = 3% drop across 2 trips (2400 seconds)
        assertEquals(3, summary.todayTotalDropPct)
        assertEquals(2400L, summary.todayTrackedDurationSeconds)
        assertEquals(2, summary.todayTripCountWithBattery)

        // Weekly aggregate
        assertEquals(3, summary.weeklyTotalDropPct)
        assertEquals(2400L, summary.weeklyTrackedDurationSeconds)
        assertEquals(2, summary.weeklyTripCountWithBattery)

        // Average drain rate: 3% over 2400s (0.6667h) -> 4.5% / hr
        assertNotNull(summary.weeklyAverageDrainRatePerHour)
        assertEquals(4.5, summary.weeklyAverageDrainRatePerHour!!, 0.1)
    }
}
