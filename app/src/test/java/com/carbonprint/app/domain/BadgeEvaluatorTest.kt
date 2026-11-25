package com.carbonprint.app.domain

import com.carbonprint.app.data.local.entity.BadgeEntity
import com.carbonprint.app.domain.manager.BadgeEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BadgeEvaluatorTest {

    private fun createInitialBadges(): List<BadgeEntity> {
        return listOf(
            BadgeEntity(
                id = BadgeEvaluator.BADGE_FIRST_GREEN_MILE,
                title = "First Green Mile",
                description = "Complete your first mile (1.6 km) of active travel.",
                category = "DISTANCE",
                iconName = "ic_badge_green_mile",
                targetProgress = 1609.34,
                currentProgress = 0.0,
                isUnlocked = false
            ),
            BadgeEntity(
                id = BadgeEvaluator.BADGE_ZERO_EMISSION_DAY,
                title = "Zero Emission Day",
                description = "Record a day with 100% eco-friendly transport.",
                category = "HABIT",
                iconName = "ic_badge_zero_day",
                targetProgress = 1.0,
                currentProgress = 0.0,
                isUnlocked = false
            ),
            BadgeEntity(
                id = BadgeEvaluator.BADGE_CYCLING_CHAMPION,
                title = "Cycling Champion",
                description = "Ride 50 km on bicycle.",
                category = "DISTANCE",
                iconName = "ic_badge_cyclist",
                targetProgress = 50000.0,
                currentProgress = 0.0,
                isUnlocked = false
            ),
            BadgeEntity(
                id = BadgeEvaluator.BADGE_CENTURY_WALKER,
                title = "Century Walker",
                description = "Walk or run 100 km.",
                category = "DISTANCE",
                iconName = "ic_badge_walker",
                targetProgress = 100000.0,
                currentProgress = 0.0,
                isUnlocked = false
            ),
            BadgeEntity(
                id = BadgeEvaluator.BADGE_CARBON_CUTTER,
                title = "Carbon Cutter",
                description = "Save 10 kg of CO2 emissions.",
                category = "EMISSION",
                iconName = "ic_badge_leaf",
                targetProgress = 10000.0,
                currentProgress = 0.0,
                isUnlocked = false
            ),
            BadgeEntity(
                id = BadgeEvaluator.BADGE_GOAL_CRUSHER,
                title = "Goal Crusher",
                description = "Meet or beat your weekly carbon reduction goal.",
                category = "HABIT",
                iconName = "ic_badge_trophy",
                targetProgress = 1.0,
                currentProgress = 0.0,
                isUnlocked = false
            )
        )
    }

    @Test
    fun evaluateBadges_unlocksFirstGreenMile() {
        val badges = createInitialBadges()
        // Walk 1000m + Bike 800m = 1800m (> 1609.34m)
        val (updated, newlyUnlocked) = BadgeEvaluator.evaluateBadges(
            existingBadges = badges,
            totalWalkMeters = 1000.0,
            totalBikeMeters = 800.0,
            totalSavedCo2Grams = 200.0,
            isGoalAchieved = false,
            isZeroEmissionDay = false
        )

        val greenMile = updated.first { it.id == BadgeEvaluator.BADGE_FIRST_GREEN_MILE }
        assertTrue(greenMile.isUnlocked)
        assertEquals(greenMile.targetProgress, greenMile.currentProgress, 0.01) // Capped at target
        assertEquals(1, newlyUnlocked.size)
        assertEquals(BadgeEvaluator.BADGE_FIRST_GREEN_MILE, newlyUnlocked[0].id)
    }

    @Test
    fun evaluateBadges_unlocksCyclingChampionAndCarbonCutter() {
        val badges = createInitialBadges()
        val (updated, newlyUnlocked) = BadgeEvaluator.evaluateBadges(
            existingBadges = badges,
            totalWalkMeters = 5000.0,
            totalBikeMeters = 55000.0, // > 50,000m target
            totalSavedCo2Grams = 12000.0, // > 10,000g target
            isGoalAchieved = false,
            isZeroEmissionDay = false
        )

        val bikeBadge = updated.first { it.id == BadgeEvaluator.BADGE_CYCLING_CHAMPION }
        assertTrue(bikeBadge.isUnlocked)

        val carbonBadge = updated.first { it.id == BadgeEvaluator.BADGE_CARBON_CUTTER }
        assertTrue(carbonBadge.isUnlocked)

        val unlockedIds = newlyUnlocked.map { it.id }
        assertTrue(unlockedIds.contains(BadgeEvaluator.BADGE_FIRST_GREEN_MILE))
        assertTrue(unlockedIds.contains(BadgeEvaluator.BADGE_CYCLING_CHAMPION))
        assertTrue(unlockedIds.contains(BadgeEvaluator.BADGE_CARBON_CUTTER))
    }

    @Test
    fun evaluateBadges_unlocksZeroEmissionDayAndGoalCrusher() {
        val badges = createInitialBadges()
        val (updated, newlyUnlocked) = BadgeEvaluator.evaluateBadges(
            existingBadges = badges,
            totalWalkMeters = 0.0,
            totalBikeMeters = 0.0,
            totalSavedCo2Grams = 0.0,
            isGoalAchieved = true,
            isZeroEmissionDay = true
        )

        val zeroDay = updated.first { it.id == BadgeEvaluator.BADGE_ZERO_EMISSION_DAY }
        assertTrue(zeroDay.isUnlocked)

        val goalCrusher = updated.first { it.id == BadgeEvaluator.BADGE_GOAL_CRUSHER }
        assertTrue(goalCrusher.isUnlocked)

        assertEquals(2, newlyUnlocked.size)
    }

    @Test
    fun evaluateBadges_isIdempotentForAlreadyUnlockedBadges() {
        val initialTimestamp = 1700000000000L
        val badges = createInitialBadges().map {
            if (it.id == BadgeEvaluator.BADGE_FIRST_GREEN_MILE) {
                it.copy(isUnlocked = true, unlockedAt = initialTimestamp, currentProgress = it.targetProgress)
            } else it
        }

        val (updated, newlyUnlocked) = BadgeEvaluator.evaluateBadges(
            existingBadges = badges,
            totalWalkMeters = 5000.0,
            totalBikeMeters = 5000.0,
            totalSavedCo2Grams = 0.0,
            isGoalAchieved = false,
            isZeroEmissionDay = false
        )

        val greenMile = updated.first { it.id == BadgeEvaluator.BADGE_FIRST_GREEN_MILE }
        assertTrue(greenMile.isUnlocked)
        assertEquals(initialTimestamp, greenMile.unlockedAt) // Timestamp preserved
        assertFalse(newlyUnlocked.any { it.id == BadgeEvaluator.BADGE_FIRST_GREEN_MILE }) // Not re-added
    }
}
