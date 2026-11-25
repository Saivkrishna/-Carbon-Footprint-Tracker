package com.carbonprint.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity storing pre-aggregated daily travel metrics for instant analytics and charts.
 */
@Entity(tableName = "daily_summaries")
data class DailySummaryEntity(
    @PrimaryKey
    val dateString: String, // Format: YYYY-MM-DD
    val totalCo2Grams: Double = 0.0,
    val totalCo2SavedGrams: Double = 0.0,
    val totalDistanceMeters: Double = 0.0,
    val totalCaloriesBurned: Double = 0.0,
    val tripCount: Int = 0,
    val walkDistanceMeters: Double = 0.0,
    val bikeDistanceMeters: Double = 0.0,
    val vehicleDistanceMeters: Double = 0.0,
    val transitDistanceMeters: Double = 0.0,
    val runDistanceMeters: Double = 0.0,
    val motorcycleDistanceMeters: Double = 0.0,
    val carDistanceMeters: Double = 0.0,
    val walkingSteps: Int = 0,
    val runningSteps: Int = 0,
    val updatedTimestamp: Long = System.currentTimeMillis()
)
