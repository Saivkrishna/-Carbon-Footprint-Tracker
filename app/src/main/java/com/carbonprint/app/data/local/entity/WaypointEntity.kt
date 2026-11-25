package com.carbonprint.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a GPS waypoint/breadcrumb recorded during an active trip.
 */
@Entity(
    tableName = "waypoints",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["tripId"])]
)
data class WaypointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tripId: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val speedMps: Float = 0f,
    val accuracyMeters: Float = 0f,
    val timestamp: Long
)
