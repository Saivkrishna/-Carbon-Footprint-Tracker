package com.carbonprint.app.data.repository

import com.carbonprint.app.data.local.dao.GoalDao
import com.carbonprint.app.data.local.dao.TripDao
import com.carbonprint.app.data.local.entity.GoalEntity
import com.carbonprint.app.data.preferences.UserPreferencesManager
import com.carbonprint.app.domain.calculator.CarbonEmissionCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Repository managing weekly carbon reduction goals, budgets, and achievement status.
 */
class GoalRepository(
    private val goalDao: GoalDao,
    private val tripDao: TripDao,
    private val preferencesManager: UserPreferencesManager
) {

    val activeGoalFlow: Flow<GoalEntity?> = goalDao.getActiveGoalFlow()
    val allGoalsFlow: Flow<List<GoalEntity>> = goalDao.getAllGoalsFlow()

    suspend fun getActiveGoal(): GoalEntity? = withContext(Dispatchers.IO) {
        goalDao.getActiveGoal()
    }

    /**
     * Retrieves the goal for the current calendar week, automatically creating a new one if a new week started.
     */
    suspend fun getOrCreateCurrentWeekGoal(): GoalEntity = withContext(Dispatchers.IO) {
        val (weekStart, weekEnd) = getCurrentWeekBounds()
        val existing = goalDao.getActiveGoal()

        if (existing != null && existing.weekStartDate == weekStart) {
            return@withContext existing
        }

        // Archive prior goals if any
        if (existing != null && existing.weekStartDate < weekStart) {
            goalDao.updateGoal(existing.copy(isArchived = true))
        }

        val target = preferencesManager.weeklyGoalTargetKg
        val newGoal = GoalEntity(
            weekStartDate = weekStart,
            weekEndDate = weekEnd,
            targetCo2Kg = target,
            currentCo2Kg = 0.0,
            isAchieved = false
        )
        val id = goalDao.insertOrUpdateGoal(newGoal)
        newGoal.copy(id = id)
    }

    /**
     * Recomputes cumulative emissions for the current week from actual completed trips and updates the goal.
     */
    suspend fun updateCurrentWeekGoalProgress(): GoalEntity = withContext(Dispatchers.IO) {
        val goal = getOrCreateCurrentWeekGoal()
        val totalCo2Grams = tripDao.getTotalCo2Between(goal.weekStartDate, goal.weekEndDate) ?: 0.0
        val totalCo2Kg = CarbonEmissionCalculator.gramsToKg(totalCo2Grams)

        val isAchieved = totalCo2Kg <= goal.targetCo2Kg
        val updated = goal.copy(
            currentCo2Kg = totalCo2Kg,
            isAchieved = isAchieved
        )
        goalDao.insertOrUpdateGoal(updated)
        updated
    }

    /**
     * Sets a custom weekly CO2 target for the user.
     */
    suspend fun setCustomWeeklyGoal(targetCo2Kg: Double): GoalEntity = withContext(Dispatchers.IO) {
        preferencesManager.weeklyGoalTargetKg = targetCo2Kg
        val currentGoal = getOrCreateCurrentWeekGoal()
        val updated = currentGoal.copy(
            targetCo2Kg = targetCo2Kg,
            isAchieved = currentGoal.currentCo2Kg <= targetCo2Kg
        )
        goalDao.insertOrUpdateGoal(updated)
        updated
    }

    companion object {
        /**
         * Calculates epoch millis boundaries for Monday 00:00:00.000 to Sunday 23:59:59.999 of the week containing [nowMillis].
         * Deterministic across all locales and timezones.
         */
        fun getCurrentWeekBounds(nowMillis: Long = System.currentTimeMillis()): Pair<Long, Long> {
            val cal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                firstDayOfWeek = Calendar.MONDAY
                val dayOfWeek = get(Calendar.DAY_OF_WEEK)
                val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
                add(Calendar.DAY_OF_YEAR, -daysFromMonday)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startMillis = cal.timeInMillis

            cal.add(Calendar.DAY_OF_YEAR, 6)
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val endMillis = cal.timeInMillis

            return Pair(startMillis, endMillis)
        }
    }
}
