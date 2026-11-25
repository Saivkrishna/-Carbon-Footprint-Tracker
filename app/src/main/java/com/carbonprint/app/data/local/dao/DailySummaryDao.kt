package com.carbonprint.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.carbonprint.app.data.local.entity.DailySummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailySummaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailySummary(summary: DailySummaryEntity)

    @Query("SELECT * FROM daily_summaries WHERE dateString = :dateString")
    suspend fun getDailySummary(dateString: String): DailySummaryEntity?

    @Query("SELECT * FROM daily_summaries WHERE dateString = :dateString")
    fun getDailySummaryFlow(dateString: String): Flow<DailySummaryEntity?>

    @Query("SELECT * FROM daily_summaries ORDER BY dateString DESC LIMIT :limit")
    fun getRecentSummariesFlow(limit: Int = 7): Flow<List<DailySummaryEntity>>

    @Query("SELECT * FROM daily_summaries WHERE dateString >= :startDate AND dateString <= :endDate ORDER BY dateString ASC")
    fun getSummariesBetweenFlow(startDate: String, endDate: String): Flow<List<DailySummaryEntity>>

    @Query("SELECT COUNT(*) FROM daily_summaries WHERE totalCo2Grams = 0.0 AND totalDistanceMeters > 0.0")
    suspend fun getZeroEmissionDaysCount(): Int
}
