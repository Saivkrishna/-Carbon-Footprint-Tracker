package com.carbonprint.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.carbonprint.app.domain.model.TransportMode

/**
 * Entity representing an individual recorded trip or travel segment.
 */
@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long = 0,
    val mode: TransportMode = TransportMode.UNKNOWN,
    val subMode: String = "AUTO", // e.g. CAR, BUS, TRAIN, BICYCLE, WALK
    val distanceMeters: Double = 0.0,
    val durationSeconds: Long = 0,
    val co2Grams: Double = 0.0,
    val co2SavedGrams: Double = 0.0, // Compared to standard ICE car (192g/km)
    val caloriesBurned: Double = 0.0,
    val averageSpeedKmh: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val stepCount: Int = 0,
    val walkingSteps: Int = 0,
    val runningSteps: Int = 0,
    val walkingDurationSeconds: Long = 0,
    val runningDurationSeconds: Long = 0,
    val startBatteryPct: Int? = null,
    val endBatteryPct: Int? = null,
    val batteryDropPct: Int? = null,
    val startLatitude: Double? = null,
    val startLongitude: Double? = null,
    val endLatitude: Double? = null,
    val endLongitude: Double? = null,
    val startAddress: String? = null,
    val endAddress: String? = null,
    val isCompleted: Boolean = false,
    val notes: String? = null
)
