package com.carbonprint.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing an eco-achievement badge awarded to incentivize green travel habits.
 */
@Entity(tableName = "badges")
data class BadgeEntity(
    @PrimaryKey
    val id: String, // e.g. "first_green_mile", "zero_emission_day"
    val title: String,
    val description: String,
    val category: String, // e.g. "DISTANCE", "EMISSION", "HABIT", "STREAK"
    val iconName: String,
    val currentProgress: Double = 0.0,
    val targetProgress: Double = 1.0,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null
)
