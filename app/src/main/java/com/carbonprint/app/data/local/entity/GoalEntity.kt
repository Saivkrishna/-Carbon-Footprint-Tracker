package com.carbonprint.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity tracking a user's weekly carbon reduction goal and budget.
 */
@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val weekStartDate: Long, // Epoch timestamp for Monday 00:00:00
    val weekEndDate: Long,   // Epoch timestamp for Sunday 23:59:59
    val targetCo2Kg: Double = 25.0, // Weekly target limit in kg
    val currentCo2Kg: Double = 0.0, // Actual cumulative CO2 emitted this week
    val targetReductionPercentage: Double = 15.0,
    val isAchieved: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
