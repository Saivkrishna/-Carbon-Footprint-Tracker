package com.carbonprint.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.carbonprint.app.data.local.entity.WaypointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WaypointDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaypoint(waypoint: WaypointEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaypoints(waypoints: List<WaypointEntity>)

    @Query("SELECT * FROM waypoints WHERE tripId = :tripId ORDER BY timestamp ASC")
    suspend fun getWaypointsForTrip(tripId: Long): List<WaypointEntity>

    @Query("SELECT * FROM waypoints WHERE tripId = :tripId ORDER BY timestamp ASC")
    fun getWaypointsForTripFlow(tripId: Long): Flow<List<WaypointEntity>>

    @Query("DELETE FROM waypoints WHERE tripId = :tripId")
    suspend fun deleteWaypointsForTrip(tripId: Long)
}
