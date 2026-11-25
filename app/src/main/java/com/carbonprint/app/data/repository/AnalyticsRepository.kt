package com.carbonprint.app.data.repository

import com.carbonprint.app.data.local.dao.DailySummaryDao
import com.carbonprint.app.data.local.dao.TripDao
import com.carbonprint.app.data.local.entity.DailySummaryEntity
import com.carbonprint.app.data.local.entity.TripEntity
import com.carbonprint.app.domain.model.TransportMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repository aggregating daily travel metrics, mode distributions, and historical trends for analytics.
 */
class AnalyticsRepository(
    private val dailySummaryDao: DailySummaryDao,
    private val tripDao: TripDao
) {

    fun getRecentSummariesFlow(limit: Int = 7): Flow<List<DailySummaryEntity>> =
        dailySummaryDao.getRecentSummariesFlow(limit)

    fun getDailySummaryFlow(dateString: String): Flow<DailySummaryEntity?> =
        dailySummaryDao.getDailySummaryFlow(dateString)

    fun getSummariesBetweenFlow(startDate: String, endDate: String): Flow<List<DailySummaryEntity>> =
        dailySummaryDao.getSummariesBetweenFlow(startDate, endDate)

    /**
     * Synchronizes a newly completed trip with the corresponding day's aggregated record.
     */
    suspend fun recordTripToDailySummary(trip: TripEntity) = withContext(Dispatchers.IO) {
        if (!trip.isCompleted) return@withContext
        val dateString = formatDate(trip.startTime)
        val existing = dailySummaryDao.getDailySummary(dateString) ?: DailySummaryEntity(dateString = dateString)

        val addWalkSteps = if (trip.walkingSteps > 0) trip.walkingSteps else (if (trip.mode == TransportMode.WALKING) trip.stepCount else 0)
        val addRunSteps = if (trip.runningSteps > 0) trip.runningSteps else (if (trip.mode == TransportMode.RUNNING) trip.stepCount else 0)

        val updated = existing.copy(
            totalCo2Grams = existing.totalCo2Grams + trip.co2Grams,
            totalCo2SavedGrams = existing.totalCo2SavedGrams + trip.co2SavedGrams,
            totalDistanceMeters = existing.totalDistanceMeters + trip.distanceMeters,
            totalCaloriesBurned = existing.totalCaloriesBurned + trip.caloriesBurned,
            tripCount = existing.tripCount + 1,
            walkDistanceMeters = if (trip.mode == TransportMode.WALKING)
                existing.walkDistanceMeters + trip.distanceMeters else existing.walkDistanceMeters,
            runDistanceMeters = if (trip.mode == TransportMode.RUNNING)
                existing.runDistanceMeters + trip.distanceMeters else existing.runDistanceMeters,
            bikeDistanceMeters = if (trip.mode == TransportMode.ON_BICYCLE)
                existing.bikeDistanceMeters + trip.distanceMeters else existing.bikeDistanceMeters,
            vehicleDistanceMeters = if (trip.mode == TransportMode.IN_VEHICLE)
                existing.vehicleDistanceMeters + trip.distanceMeters else existing.vehicleDistanceMeters,
            motorcycleDistanceMeters = if (trip.mode == TransportMode.IN_VEHICLE && trip.subMode.contains("MOTORCYCLE", ignoreCase = true))
                existing.motorcycleDistanceMeters + trip.distanceMeters else existing.motorcycleDistanceMeters,
            carDistanceMeters = if (trip.mode == TransportMode.IN_VEHICLE && !trip.subMode.contains("MOTORCYCLE", ignoreCase = true))
                existing.carDistanceMeters + trip.distanceMeters else existing.carDistanceMeters,
            transitDistanceMeters = if (trip.mode == TransportMode.TRANSIT)
                existing.transitDistanceMeters + trip.distanceMeters else existing.transitDistanceMeters,
            walkingSteps = existing.walkingSteps + addWalkSteps,
            runningSteps = existing.runningSteps + addRunSteps,
            updatedTimestamp = System.currentTimeMillis()
        )
        dailySummaryDao.upsertDailySummary(updated)
    }

    /**
     * Reverts a deleted trip's contributions from the day's aggregated record.
     */
    suspend fun removeTripFromDailySummary(trip: TripEntity) = withContext(Dispatchers.IO) {
        if (!trip.isCompleted) return@withContext
        val dateString = formatDate(trip.startTime)
        val existing = dailySummaryDao.getDailySummary(dateString) ?: return@withContext

        val remWalkSteps = if (trip.walkingSteps > 0) trip.walkingSteps else (if (trip.mode == TransportMode.WALKING) trip.stepCount else 0)
        val remRunSteps = if (trip.runningSteps > 0) trip.runningSteps else (if (trip.mode == TransportMode.RUNNING) trip.stepCount else 0)

        val updated = existing.copy(
            totalCo2Grams = (existing.totalCo2Grams - trip.co2Grams).coerceAtLeast(0.0),
            totalCo2SavedGrams = (existing.totalCo2SavedGrams - trip.co2SavedGrams).coerceAtLeast(0.0),
            totalDistanceMeters = (existing.totalDistanceMeters - trip.distanceMeters).coerceAtLeast(0.0),
            totalCaloriesBurned = (existing.totalCaloriesBurned - trip.caloriesBurned).coerceAtLeast(0.0),
            tripCount = (existing.tripCount - 1).coerceAtLeast(0),
            walkDistanceMeters = if (trip.mode == TransportMode.WALKING)
                (existing.walkDistanceMeters - trip.distanceMeters).coerceAtLeast(0.0) else existing.walkDistanceMeters,
            runDistanceMeters = if (trip.mode == TransportMode.RUNNING)
                (existing.runDistanceMeters - trip.distanceMeters).coerceAtLeast(0.0) else existing.runDistanceMeters,
            bikeDistanceMeters = if (trip.mode == TransportMode.ON_BICYCLE)
                (existing.bikeDistanceMeters - trip.distanceMeters).coerceAtLeast(0.0) else existing.bikeDistanceMeters,
            vehicleDistanceMeters = if (trip.mode == TransportMode.IN_VEHICLE)
                (existing.vehicleDistanceMeters - trip.distanceMeters).coerceAtLeast(0.0) else existing.vehicleDistanceMeters,
            motorcycleDistanceMeters = if (trip.mode == TransportMode.IN_VEHICLE && trip.subMode.contains("MOTORCYCLE", ignoreCase = true))
                (existing.motorcycleDistanceMeters - trip.distanceMeters).coerceAtLeast(0.0) else existing.motorcycleDistanceMeters,
            carDistanceMeters = if (trip.mode == TransportMode.IN_VEHICLE && !trip.subMode.contains("MOTORCYCLE", ignoreCase = true))
                (existing.carDistanceMeters - trip.distanceMeters).coerceAtLeast(0.0) else existing.carDistanceMeters,
            transitDistanceMeters = if (trip.mode == TransportMode.TRANSIT)
                (existing.transitDistanceMeters - trip.distanceMeters).coerceAtLeast(0.0) else existing.transitDistanceMeters,
            walkingSteps = (existing.walkingSteps - remWalkSteps).coerceAtLeast(0),
            runningSteps = (existing.runningSteps - remRunSteps).coerceAtLeast(0),
            updatedTimestamp = System.currentTimeMillis()
        )
        dailySummaryDao.upsertDailySummary(updated)
    }

    /**
     * Computes the modal split (percentage or distance per TransportMode) across a list of trips.
     */
    fun calculateModeBreakdown(trips: List<TripEntity>): Map<TransportMode, Double> {
        val breakdown = mutableMapOf<TransportMode, Double>()
        for (trip in trips) {
            val current = breakdown.getOrDefault(trip.mode, 0.0)
            breakdown[trip.mode] = current + trip.distanceMeters
        }
        return breakdown
    }

    fun getTodayDateString(): String = formatDate(System.currentTimeMillis())

    private fun formatDate(epochMillis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date(epochMillis))
    }
}
