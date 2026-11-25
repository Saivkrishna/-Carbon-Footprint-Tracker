package com.carbonprint.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.carbonprint.app.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateGoal(goal: GoalEntity): Long

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Query("SELECT * FROM goals WHERE isArchived = 0 ORDER BY weekStartDate DESC LIMIT 1")
    fun getActiveGoalFlow(): Flow<GoalEntity?>

    @Query("SELECT * FROM goals WHERE isArchived = 0 ORDER BY weekStartDate DESC LIMIT 1")
    suspend fun getActiveGoal(): GoalEntity?

    @Query("SELECT * FROM goals ORDER BY weekStartDate DESC")
    fun getAllGoalsFlow(): Flow<List<GoalEntity>>

    @Query("UPDATE goals SET currentCo2Kg = :currentCo2Kg, isAchieved = :isAchieved WHERE id = :id")
    suspend fun updateGoalProgress(id: Long, currentCo2Kg: Double, isAchieved: Boolean)
}
