package com.carbonprint.app.domain.manager

import com.carbonprint.app.data.local.entity.BadgeEntity

/**
 * Gamification engine evaluating unlock thresholds and incremental progress for eco-achievement badges.
 */
object BadgeEvaluator {

    const val BADGE_FIRST_GREEN_MILE = "first_green_mile"
    const val BADGE_ZERO_EMISSION_DAY = "zero_emission_day"
    const val BADGE_CYCLING_CHAMPION = "cycling_champion"
    const val BADGE_CENTURY_WALKER = "century_walker"
    const val BADGE_CARBON_CUTTER = "carbon_cutter"
    const val BADGE_GOAL_CRUSHER = "goal_crusher"

    /**
     * Evaluates a list of badges against current user cumulative metrics and returns updated entities.
     *
     * @param existingBadges List of badges currently in the database.
     * @param totalWalkMeters Cumulative walking & running distance.
     * @param totalBikeMeters Cumulative cycling distance.
     * @param totalSavedCo2Grams Cumulative CO2 saved in grams compared to combustion car.
     * @param isGoalAchieved Whether current weekly carbon budget goal was satisfied.
     * @param isZeroEmissionDay Whether the user achieved at least one complete zero-emission day.
     * @return Pair of (all updated badges, newly unlocked badges).
     */
    fun evaluateBadges(
        existingBadges: List<BadgeEntity>,
        totalWalkMeters: Double,
        totalBikeMeters: Double,
        totalSavedCo2Grams: Double,
        isGoalAchieved: Boolean,
        isZeroEmissionDay: Boolean
    ): Pair<List<BadgeEntity>, List<BadgeEntity>> {
        val now = System.currentTimeMillis()
        val updatedList = mutableListOf<BadgeEntity>()
        val newlyUnlocked = mutableListOf<BadgeEntity>()

        for (badge in existingBadges) {
            var progress = badge.currentProgress
            var shouldUnlock = badge.isUnlocked

            when (badge.id) {
                BADGE_FIRST_GREEN_MILE -> {
                    val greenMeters = totalWalkMeters + totalBikeMeters
                    progress = minOf(badge.targetProgress, maxOf(progress, greenMeters))
                    if (progress >= badge.targetProgress) shouldUnlock = true
                }
                BADGE_ZERO_EMISSION_DAY -> {
                    if (isZeroEmissionDay) {
                        progress = badge.targetProgress
                        shouldUnlock = true
                    }
                }
                BADGE_CYCLING_CHAMPION -> {
                    progress = minOf(badge.targetProgress, maxOf(progress, totalBikeMeters))
                    if (progress >= badge.targetProgress) shouldUnlock = true
                }
                BADGE_CENTURY_WALKER -> {
                    progress = minOf(badge.targetProgress, maxOf(progress, totalWalkMeters))
                    if (progress >= badge.targetProgress) shouldUnlock = true
                }
                BADGE_CARBON_CUTTER -> {
                    progress = minOf(badge.targetProgress, maxOf(progress, totalSavedCo2Grams))
                    if (progress >= badge.targetProgress) shouldUnlock = true
                }
                BADGE_GOAL_CRUSHER -> {
                    if (isGoalAchieved) {
                        progress = badge.targetProgress
                        shouldUnlock = true
                    }
                }
            }

            val wasUnlocked = badge.isUnlocked
            val isNowUnlocked = shouldUnlock
            val unlockedTimestamp = if (!wasUnlocked && isNowUnlocked) now else badge.unlockedAt

            val updated = badge.copy(
                currentProgress = progress,
                isUnlocked = isNowUnlocked,
                unlockedAt = unlockedTimestamp
            )

            if (!wasUnlocked && isNowUnlocked) {
                newlyUnlocked.add(updated)
            }
            updatedList.add(updated)
        }

        return Pair(updatedList, newlyUnlocked)
    }
}
