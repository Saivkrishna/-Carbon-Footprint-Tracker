package com.carbonprint.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.carbonprint.app.data.local.entity.TripEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity): Long

    @Update
    suspend fun updateTrip(trip: TripEntity)

    @Delete
    suspend fun deleteTrip(trip: TripEntity)

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getTripById(id: Long): TripEntity?

    @Query("SELECT * FROM trips WHERE id = :id")
    fun getTripByIdFlow(id: Long): Flow<TripEntity?>

    @Query("SELECT * FROM trips ORDER BY startTime DESC")
    fun getAllTripsFlow(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE isCompleted = 0 ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveTrip(): TripEntity?

    @Query("SELECT * FROM trips WHERE isCompleted = 0 ORDER BY startTime DESC LIMIT 1")
    fun getActiveTripFlow(): Flow<TripEntity?>

    @Query("SELECT * FROM trips WHERE startTime >= :startMillis AND startTime <= :endMillis ORDER BY startTime DESC")
    fun getTripsBetweenFlow(startMillis: Long, endMillis: Long): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE startTime >= :startMillis AND startTime <= :endMillis")
    suspend fun getTripsBetween(startMillis: Long, endMillis: Long): List<TripEntity>

    @Query("SELECT SUM(co2Grams) FROM trips WHERE isCompleted = 1 AND startTime >= :startMillis AND startTime <= :endMillis")
    suspend fun getTotalCo2Between(startMillis: Long, endMillis: Long): Double?

    @Query("SELECT SUM(distanceMeters) FROM trips WHERE isCompleted = 1 AND startTime >= :startMillis AND startTime <= :endMillis")
    suspend fun getTotalDistanceBetween(startMillis: Long, endMillis: Long): Double?

    @Query("SELECT SUM(distanceMeters) FROM trips WHERE isCompleted = 1 AND (mode = 'WALKING' OR mode = 'RUNNING')")
    suspend fun getTotalWalkingDistance(): Double?

    @Query("SELECT SUM(distanceMeters) FROM trips WHERE isCompleted = 1 AND mode = 'ON_BICYCLE'")
    suspend fun getTotalCyclingDistance(): Double?

    @Query("SELECT SUM(co2SavedGrams) FROM trips WHERE isCompleted = 1")
    suspend fun getTotalCo2Saved(): Double?

    @Query("SELECT SUM(co2Grams) FROM trips WHERE isCompleted = 1")
    suspend fun getTotalCo2Emitted(): Double?

    @Query("SELECT COUNT(*) FROM trips")
    suspend fun getTotalTripCount(): Int

    @Query("SELECT SUM(stepCount) FROM trips WHERE isCompleted = 1 AND mode = 'WALKING'")
    suspend fun getTotalWalkingSteps(): Int?

    @Query("SELECT SUM(stepCount) FROM trips WHERE isCompleted = 1 AND mode = 'RUNNING'")
    suspend fun getTotalRunningSteps(): Int?

    @Query("SELECT SUM(stepCount) FROM trips WHERE isCompleted = 1 AND (mode = 'WALKING' OR mode = 'RUNNING')")
    suspend fun getTotalSteps(): Int?

    @Query("SELECT SUM(walkingSteps) FROM trips WHERE isCompleted = 1")
    suspend fun getSumWalkingSteps(): Int?

    @Query("SELECT SUM(runningSteps) FROM trips WHERE isCompleted = 1")
    suspend fun getSumRunningSteps(): Int?

    @Query("SELECT SUM(batteryDropPct) FROM trips WHERE isCompleted = 1")
    suspend fun getTotalBatteryDropPct(): Int?
}
