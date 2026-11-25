package com.carbonprint.app.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.carbonprint.app.CarbonPrintApp
import com.carbonprint.app.service.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Periodic WorkManager task checking user's weekly carbon reduction budget
 * and issuing proactive reminders when the user nears their target threshold.
 */
class GoalReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "GoalReminderWorker"
        const val UNIQUE_WORK_NAME = "GoalReminderWork"

        /**
         * Enqueues daily reminder check.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<GoalReminderWorker>(
                24, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            Log.i(TAG, "Scheduled unique periodic GoalReminderWorker.")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            val app = applicationContext as? CarbonPrintApp ?: return@withContext Result.failure()

            val currentGoal = app.goalRepository.getActiveGoal() ?: return@withContext Result.success()
            val ratio = if (currentGoal.targetCo2Kg > 0.0) {
                currentGoal.currentCo2Kg / currentGoal.targetCo2Kg
            } else 0.0

            if (ratio >= 0.85 && ratio < 1.0) {
                NotificationHelper.showGoalAlertNotification(
                    context = applicationContext,
                    title = "Weekly Carbon Alert 🌿",
                    message = "You've used ${String.format("%.0f", ratio * 100)}% of your weekly carbon budget (${String.format("%.1f", currentGoal.currentCo2Kg)} / ${String.format("%.1f", currentGoal.targetCo2Kg)} kg). Consider cycling or walking for your next trip!"
                )
            } else if (ratio >= 1.0) {
                NotificationHelper.showGoalAlertNotification(
                    context = applicationContext,
                    title = "Weekly Carbon Budget Exceeded ⚠️",
                    message = "You've emitted ${String.format("%.1f", currentGoal.currentCo2Kg)} kg CO2 this week, exceeding your ${String.format("%.1f", currentGoal.targetCo2Kg)} kg target. Take green trips to offset!"
                )
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error running GoalReminderWorker: ${e.message}", e)
            Result.retry()
        }
    }
}
