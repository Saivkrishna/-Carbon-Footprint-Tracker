package com.carbonprint.app.data.repository

import com.carbonprint.app.data.local.dao.BadgeDao
import com.carbonprint.app.data.local.dao.DailySummaryDao
import com.carbonprint.app.data.local.dao.TripDao
import com.carbonprint.app.data.local.entity.BadgeEntity
import com.carbonprint.app.domain.manager.BadgeEvaluator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Repository for eco-achievement badges, progression evaluation, and unlocks.
 */
class BadgeRepository(
    private val badgeDao: BadgeDao,
    private val tripDao: TripDao,
    private val dailySummaryDao: DailySummaryDao
) {

    val allBadgesFlow: Flow<List<BadgeEntity>> = badgeDao.getAllBadgesFlow()
    val unlockedBadgesFlow: Flow<List<BadgeEntity>> = badgeDao.getUnlockedBadgesFlow()

    suspend fun getUnlockedBadgeCount(): Int = withContext(Dispatchers.IO) {
        badgeDao.getUnlockedBadgeCount()
    }

    suspend fun getBadgeById(id: String): BadgeEntity? = withContext(Dispatchers.IO) {
        badgeDao.getBadgeById(id)
    }

    /**
     * Evaluates all badges using true cumulative statistics fetched directly from the database.
     *
     * @param isGoalAchieved Current week's goal achievement status.
     * @return List of newly unlocked badges in this evaluation cycle.
     */
    suspend fun evaluateAllBadgesFromDatabase(isGoalAchieved: Boolean): List<BadgeEntity> = withContext(Dispatchers.IO) {
        val totalWalk = tripDao.getTotalWalkingDistance() ?: 0.0
        val totalBike = tripDao.getTotalCyclingDistance() ?: 0.0
        val totalSaved = tripDao.getTotalCo2Saved() ?: 0.0
        val hasZeroEmissionDay = dailySummaryDao.getZeroEmissionDaysCount() > 0

        evaluateAndPersistBadges(
            totalWalkMeters = totalWalk,
            totalBikeMeters = totalBike,
            totalSavedCo2Grams = totalSaved,
            isGoalAchieved = isGoalAchieved,
            isZeroEmissionDay = hasZeroEmissionDay
        )
    }

    /**
     * Evaluates all badges against provided metrics and persists updates.
     *
     * @return List of newly unlocked badges in this evaluation cycle.
     */
    suspend fun evaluateAndPersistBadges(
        totalWalkMeters: Double,
        totalBikeMeters: Double,
        totalSavedCo2Grams: Double,
        isGoalAchieved: Boolean,
        isZeroEmissionDay: Boolean
    ): List<BadgeEntity> = withContext(Dispatchers.IO) {
        val existingBadges = badgeDao.getAllBadgesFlow().first()
        if (existingBadges.isEmpty()) return@withContext emptyList()

        val (updatedList, newlyUnlocked) = BadgeEvaluator.evaluateBadges(
            existingBadges = existingBadges,
            totalWalkMeters = totalWalkMeters,
            totalBikeMeters = totalBikeMeters,
            totalSavedCo2Grams = totalSavedCo2Grams,
            isGoalAchieved = isGoalAchieved,
            isZeroEmissionDay = isZeroEmissionDay
        )

        badgeDao.insertBadges(updatedList)
        newlyUnlocked
    }
}
