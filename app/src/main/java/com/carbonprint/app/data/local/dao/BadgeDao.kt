package com.carbonprint.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.carbonprint.app.data.local.entity.BadgeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BadgeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBadges(badges: List<BadgeEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBadgeIfNotExists(badge: BadgeEntity)

    @Update
    suspend fun updateBadge(badge: BadgeEntity)

    @Query("SELECT * FROM badges ORDER BY isUnlocked DESC, id ASC")
    fun getAllBadgesFlow(): Flow<List<BadgeEntity>>

    @Query("SELECT * FROM badges WHERE isUnlocked = 1 ORDER BY unlockedAt DESC")
    fun getUnlockedBadgesFlow(): Flow<List<BadgeEntity>>

    @Query("SELECT * FROM badges WHERE id = :id")
    suspend fun getBadgeById(id: String): BadgeEntity?

    @Query("SELECT COUNT(*) FROM badges WHERE isUnlocked = 1")
    suspend fun getUnlockedBadgeCount(): Int
}
