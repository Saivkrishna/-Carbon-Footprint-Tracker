package com.carbonprint.app.data.repository

import com.carbonprint.app.data.local.dao.DailySummaryDao
import com.carbonprint.app.data.local.dao.GoalDao
import com.carbonprint.app.data.local.dao.TripDao
import com.carbonprint.app.data.local.dao.WaypointDao
import com.carbonprint.app.data.local.entity.DailySummaryEntity
import com.carbonprint.app.data.local.entity.TripEntity
import com.carbonprint.app.data.local.entity.WaypointEntity
import com.carbonprint.app.data.preferences.UserPreferencesManager
import com.carbonprint.app.domain.calculator.CalorieCalculator
import com.carbonprint.app.domain.calculator.CarbonEmissionCalculator
import com.carbonprint.app.domain.model.TransportMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repository coordinating trip lifecycle, GPS waypoint trails, and real-time metric computations.
 */
class TripRepository(
    private val tripDao: TripDao,
    private val waypointDao: WaypointDao,
    private val dailySummaryDao: DailySummaryDao,
    private val goalDao: GoalDao,
    private val preferencesManager: UserPreferencesManager
) {

    val allTripsFlow: Flow<List<TripEntity>> = tripDao.getAllTripsFlow()

    fun getActiveTripFlow(): Flow<TripEntity?> = tripDao.getActiveTripFlow()

    suspend fun getActiveTrip(): TripEntity? = withContext(Dispatchers.IO) {
        tripDao.getActiveTrip()
    }

    suspend fun getTripById(id: Long): TripEntity? = withContext(Dispatchers.IO) {
        tripDao.getTripById(id)
    }

    fun getTripByIdFlow(id: Long): Flow<TripEntity?> = tripDao.getTripByIdFlow(id)

    fun getWaypointsForTripFlow(tripId: Long): Flow<List<WaypointEntity>> =
        waypointDao.getWaypointsForTripFlow(tripId)

    suspend fun getWaypointsForTrip(tripId: Long): List<WaypointEntity> = withContext(Dispatchers.IO) {
        waypointDao.getWaypointsForTrip(tripId)
    }

    /**
     * Initializes and persists a new active trip, ensuring duplicate trips are prevented.
     */
    suspend fun startTrip(
        mode: TransportMode,
        subMode: String = "AUTO",
        startLat: Double? = null,
        startLng: Double? = null,
        stepCount: Int = 0,
        walkingSteps: Int = 0,
        runningSteps: Int = 0,
        walkingDurationSeconds: Long = 0L,
        runningDurationSeconds: Long = 0L,
        startBatteryPct: Int? = null
    ): Long = withContext(Dispatchers.IO) {
        val existingActive = tripDao.getActiveTrip()
        if (existingActive != null) {
            if (existingActive.mode == mode && !existingActive.isCompleted) {
                // Prevent duplicate trip: reuse existing active trip
                preferencesManager.currentActiveTripId = existingActive.id
                preferencesManager.isTrackingActive = true
                return@withContext existingActive.id
            } else {
                // Finalize previous orphaned trip before starting new one
                completeTrip(existingActive.id, null, null)
            }
        }

        val now = System.currentTimeMillis()
        val newTrip = TripEntity(
            startTime = now,
            mode = mode,
            subMode = subMode,
            startLatitude = startLat,
            startLongitude = startLng,
            stepCount = if (stepCount > 0) stepCount else (walkingSteps + runningSteps),
            walkingSteps = walkingSteps,
            runningSteps = runningSteps,
            walkingDurationSeconds = walkingDurationSeconds,
            runningDurationSeconds = runningDurationSeconds,
            startBatteryPct = startBatteryPct,
            isCompleted = false
        )
        val id = tripDao.insertTrip(newTrip)
        preferencesManager.currentActiveTripId = id
        preferencesManager.isTrackingActive = true
        id
    }

    /**
     * Records a GPS breadcrumb waypoint for an active trip.
     */
    suspend fun addWaypoint(
        tripId: Long,
        latitude: Double,
        longitude: Double,
        altitude: Double,
        speedMps: Float,
        accuracyMeters: Float,
        timestamp: Long
    ): WaypointEntity = withContext(Dispatchers.IO) {
        val waypoint = WaypointEntity(
            tripId = tripId,
            latitude = latitude,
            longitude = longitude,
            altitude = altitude,
            speedMps = speedMps,
            accuracyMeters = accuracyMeters,
            timestamp = timestamp
        )
        waypointDao.insertWaypoint(waypoint)
        waypoint
    }

    /**
     * Updates the travel mode and sub-mode of an active trip without creating a duplicate trip.
     * Preserves all accumulated distance, waypoints, steps, duration, and battery metrics.
     */
    suspend fun updateTripMode(
        tripId: Long,
        mode: TransportMode,
        subMode: String
    ): TripEntity? = withContext(Dispatchers.IO) {
        val existing = tripDao.getTripById(tripId) ?: return@withContext null
        if (existing.isCompleted) return@withContext existing
        val updated = existing.copy(
            mode = mode,
            subMode = subMode
        )
        tripDao.updateTrip(updated)
        updated
    }

    /**
     * Periodically updates live metrics (distance, emissions, duration) during ongoing tracking.
     */
    suspend fun updateTripProgress(
        tripId: Long,
        distanceMeters: Double,
        durationSeconds: Long,
        currentSpeedKmh: Double,
        stepCount: Int = 0,
        walkingSteps: Int? = null,
        runningSteps: Int? = null,
        walkingDurationSeconds: Long? = null,
        runningDurationSeconds: Long? = null,
        overrideCo2Grams: Double? = null,
        overrideCo2SavedGrams: Double? = null,
        overrideCalories: Double? = null
    ): TripEntity? = withContext(Dispatchers.IO) {
        val existing = tripDao.getTripById(tripId) ?: return@withContext null
        if (existing.isCompleted) return@withContext existing

        val vehicleType = preferencesManager.selectedVehicleType
        val userWeight = preferencesManager.userWeightKg

        val co2Emitted = overrideCo2Grams ?: CarbonEmissionCalculator.calculateEmissionGrams(
            mode = existing.mode,
            distanceMeters = distanceMeters,
            vehicleType = vehicleType
        )
        val co2Saved = overrideCo2SavedGrams ?: CarbonEmissionCalculator.calculateSavedEmissionGrams(
            mode = existing.mode,
            distanceMeters = distanceMeters,
            vehicleType = vehicleType
        )
        val calories = overrideCalories ?: CalorieCalculator.calculateCaloriesBurned(
            mode = existing.mode,
            distanceMeters = distanceMeters,
            userWeightKg = userWeight
        )

        val wSteps = walkingSteps ?: existing.walkingSteps
        val rSteps = runningSteps ?: existing.runningSteps
        val finalSteps = if (stepCount > 0) stepCount else (wSteps + rSteps)

        val updated = existing.copy(
            distanceMeters = distanceMeters,
            durationSeconds = durationSeconds,
            co2Grams = co2Emitted,
            co2SavedGrams = co2Saved,
            caloriesBurned = calories,
            stepCount = finalSteps,
            walkingSteps = wSteps,
            runningSteps = rSteps,
            walkingDurationSeconds = walkingDurationSeconds ?: existing.walkingDurationSeconds,
            runningDurationSeconds = runningDurationSeconds ?: existing.runningDurationSeconds,
            averageSpeedKmh = if (durationSeconds > 0) (distanceMeters / 1000.0) / (durationSeconds / 3600.0) else 0.0,
            maxSpeedKmh = maxOf(existing.maxSpeedKmh, currentSpeedKmh)
        )
        tripDao.updateTrip(updated)
        updated
    }

    /**
     * Completes and finalizes an ongoing trip record.
     */
    suspend fun completeTrip(
        tripId: Long,
        endLat: Double? = null,
        endLng: Double? = null,
        endAddress: String? = null,
        stepCount: Int = 0,
        walkingSteps: Int? = null,
        runningSteps: Int? = null,
        walkingDurationSeconds: Long? = null,
        runningDurationSeconds: Long? = null,
        averageSpeedKmh: Double? = null,
        maxSpeedKmh: Double? = null,
        endBatteryPct: Int? = null,
        batteryDropPct: Int? = null
    ): TripEntity? = withContext(Dispatchers.IO) {
        val existing = tripDao.getTripById(tripId) ?: return@withContext null
        val now = System.currentTimeMillis()

        val finalAvgSpeed = averageSpeedKmh ?: existing.averageSpeedKmh
        val finalMaxSpeed = maxSpeedKmh ?: existing.maxSpeedKmh
        val wSteps = walkingSteps ?: existing.walkingSteps
        val rSteps = runningSteps ?: existing.runningSteps
        val finalSteps = if (stepCount > 0) stepCount else (wSteps + rSteps)

        val finalBatteryDrop = batteryDropPct ?: (
            if (existing.startBatteryPct != null && endBatteryPct != null) {
                maxOf(0, existing.startBatteryPct - endBatteryPct)
            } else {
                existing.batteryDropPct
            }
        )

        val updated = existing.copy(
            endTime = now,
            endLatitude = endLat,
            endLongitude = endLng,
            endAddress = endAddress,
            stepCount = finalSteps,
            walkingSteps = wSteps,
            runningSteps = rSteps,
            walkingDurationSeconds = walkingDurationSeconds ?: existing.walkingDurationSeconds,
            runningDurationSeconds = runningDurationSeconds ?: existing.runningDurationSeconds,
            averageSpeedKmh = finalAvgSpeed,
            maxSpeedKmh = finalMaxSpeed,
            endBatteryPct = endBatteryPct ?: existing.endBatteryPct,
            batteryDropPct = finalBatteryDrop,
            isCompleted = true
        )
        tripDao.updateTrip(updated)
        preferencesManager.currentActiveTripId = -1L
        preferencesManager.isTrackingActive = false
        updated
    }

    suspend fun getTotalWalkingSteps(): Int = withContext(Dispatchers.IO) {
        tripDao.getTotalWalkingSteps() ?: 0
    }

    suspend fun getTotalRunningSteps(): Int = withContext(Dispatchers.IO) {
        tripDao.getTotalRunningSteps() ?: 0
    }

    suspend fun getTotalSteps(): Int = withContext(Dispatchers.IO) {
        tripDao.getTotalSteps() ?: 0
    }

    /**
     * Discards an accidental or invalid trip.
     */
    suspend fun cancelTrip(tripId: Long) = withContext(Dispatchers.IO) {
        val trip = tripDao.getTripById(tripId)
        if (trip != null) {
            waypointDao.deleteWaypointsForTrip(tripId)
            tripDao.deleteTrip(trip)
        }
        preferencesManager.currentActiveTripId = -1L
        preferencesManager.isTrackingActive = false
    }

    /**
     * Deletes a trip, its GPS coordinates, and adjusts daily summary and goal records.
     */
    suspend fun deleteTrip(trip: TripEntity) = withContext(Dispatchers.IO) {
        waypointDao.deleteWaypointsForTrip(trip.id)
        tripDao.deleteTrip(trip)

        if (trip.isCompleted) {
            // 1. Decrement daily summary
            val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(trip.startTime))
            val existingSummary = dailySummaryDao.getDailySummary(dateString)
            if (existingSummary != null) {
                val updatedSummary = existingSummary.copy(
                    totalCo2Grams = (existingSummary.totalCo2Grams - trip.co2Grams).coerceAtLeast(0.0),
                    totalCo2SavedGrams = (existingSummary.totalCo2SavedGrams - trip.co2SavedGrams).coerceAtLeast(0.0),
                    totalDistanceMeters = (existingSummary.totalDistanceMeters - trip.distanceMeters).coerceAtLeast(0.0),
                    totalCaloriesBurned = (existingSummary.totalCaloriesBurned - trip.caloriesBurned).coerceAtLeast(0.0),
                    tripCount = (existingSummary.tripCount - 1).coerceAtLeast(0),
                    walkDistanceMeters = if (trip.mode == TransportMode.WALKING || trip.mode == TransportMode.RUNNING)
                        (existingSummary.walkDistanceMeters - trip.distanceMeters).coerceAtLeast(0.0) else existingSummary.walkDistanceMeters,
                    bikeDistanceMeters = if (trip.mode == TransportMode.ON_BICYCLE)
                        (existingSummary.bikeDistanceMeters - trip.distanceMeters).coerceAtLeast(0.0) else existingSummary.bikeDistanceMeters,
                    vehicleDistanceMeters = if (trip.mode == TransportMode.IN_VEHICLE)
                        (existingSummary.vehicleDistanceMeters - trip.distanceMeters).coerceAtLeast(0.0) else existingSummary.vehicleDistanceMeters,
                    transitDistanceMeters = if (trip.mode == TransportMode.TRANSIT)
                        (existingSummary.transitDistanceMeters - trip.distanceMeters).coerceAtLeast(0.0) else existingSummary.transitDistanceMeters,
                    updatedTimestamp = System.currentTimeMillis()
                )
                dailySummaryDao.upsertDailySummary(updatedSummary)
            }

            // 2. Re-synchronize current week goal
            val (weekStart, weekEnd) = GoalRepository.getCurrentWeekBounds()
            val activeGoal = goalDao.getActiveGoal()
            if (activeGoal != null && activeGoal.weekStartDate == weekStart) {
                val totalCo2Grams = tripDao.getTotalCo2Between(weekStart, weekEnd) ?: 0.0
                val totalCo2Kg = CarbonEmissionCalculator.gramsToKg(totalCo2Grams)
                goalDao.updateGoal(
                    activeGoal.copy(
                        currentCo2Kg = totalCo2Kg,
                        isAchieved = totalCo2Kg <= activeGoal.targetCo2Kg
                    )
                )
            }
        }
    }
}
