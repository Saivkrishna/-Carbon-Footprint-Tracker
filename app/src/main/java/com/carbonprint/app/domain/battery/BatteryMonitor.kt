package com.carbonprint.app.domain.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.carbonprint.app.data.local.entity.TripEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages battery efficiency monitoring during GPS and activity tracking sessions.
 *
 * Uses Android's sticky ACTION_BATTERY_CHANGED intent to read battery level instantly
 * without background polling, thread wakeups, or unnecessary battery consumption.
 */
object BatteryMonitor {

    /**
     * Reads current battery percentage (0-100) using sticky intent broadcast.
     * Returns null if battery status is unavailable.
     */
    fun getBatteryPercentage(context: Context): Int? {
        return try {
            val intent: Intent? = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            if (intent == null) return null

            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

            if (level >= 0 && scale > 0) {
                ((level.toFloat() / scale.toFloat()) * 100).toInt().coerceIn(0, 100)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Computes the battery percentage decrease between trip start and stop.
     * Prevents negative drops caused by charging during tracking.
     */
    fun calculateBatteryDrop(startPct: Int?, endPct: Int?): Int? {
        if (startPct == null || endPct == null) return null
        return maxOf(0, startPct - endPct)
    }

    /**
     * Estimates hourly battery drain rate percentage (e.g. 3.2% per hour).
     */
    fun calculateDrainRatePerHour(dropPct: Int?, durationSeconds: Long): Double? {
        if (dropPct == null || durationSeconds <= 0L) return null
        val hours = durationSeconds / 3600.0
        if (hours <= 0.0) return null
        val rate = dropPct / hours
        return Math.round(rate * 10.0) / 10.0
    }

    /**
     * Aggregates battery metrics across completed trips for daily and weekly analytics.
     */
    fun computeBatterySummary(trips: List<TripEntity>): BatteryUsageSummary {
        val completedTrips = trips.filter { it.isCompleted }
        val latestTrip = completedTrips.maxByOrNull { it.startTime }

        val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val sevenDaysAgoMillis = System.currentTimeMillis() - (7L * 24L * 3600L * 1000L)

        val todayTrips = completedTrips.filter {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it.startTime)) == todayDateStr
        }

        val weeklyTrips = completedTrips.filter { it.startTime >= sevenDaysAgoMillis }

        val todayDrop = todayTrips.mapNotNull { it.batteryDropPct }.sum()
        val todayDuration = todayTrips.sumOf { it.durationSeconds }
        val todayBatteryTripCount = todayTrips.count { it.batteryDropPct != null }

        val weeklyDrop = weeklyTrips.mapNotNull { it.batteryDropPct }.sum()
        val weeklyDuration = weeklyTrips.sumOf { it.durationSeconds }
        val weeklyBatteryTripCount = weeklyTrips.count { it.batteryDropPct != null }

        val weeklyDrainRate = if (weeklyDuration >= 300L && weeklyDrop >= 0) {
            calculateDrainRatePerHour(weeklyDrop, weeklyDuration)
        } else {
            null
        }

        return BatteryUsageSummary(
            latestStartPct = latestTrip?.startBatteryPct,
            latestEndPct = latestTrip?.endBatteryPct,
            latestDropPct = latestTrip?.batteryDropPct,
            latestDurationSeconds = latestTrip?.durationSeconds ?: 0L,
            todayTotalDropPct = todayDrop,
            todayTrackedDurationSeconds = todayDuration,
            todayTripCountWithBattery = todayBatteryTripCount,
            weeklyTotalDropPct = weeklyDrop,
            weeklyTrackedDurationSeconds = weeklyDuration,
            weeklyTripCountWithBattery = weeklyBatteryTripCount,
            weeklyAverageDrainRatePerHour = weeklyDrainRate
        )
    }
}

/**
 * Data model representing aggregated battery efficiency statistics.
 */
data class BatteryUsageSummary(
    val latestStartPct: Int? = null,
    val latestEndPct: Int? = null,
    val latestDropPct: Int? = null,
    val latestDurationSeconds: Long = 0L,
    val todayTotalDropPct: Int = 0,
    val todayTrackedDurationSeconds: Long = 0L,
    val todayTripCountWithBattery: Int = 0,
    val weeklyTotalDropPct: Int = 0,
    val weeklyTrackedDurationSeconds: Long = 0L,
    val weeklyTripCountWithBattery: Int = 0,
    val weeklyAverageDrainRatePerHour: Double? = null
)
