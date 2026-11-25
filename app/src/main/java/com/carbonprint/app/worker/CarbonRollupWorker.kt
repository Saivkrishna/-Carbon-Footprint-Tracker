package com.carbonprint.app.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.carbonprint.app.CarbonPrintApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Periodic WorkManager worker aggregating completed trip data into daily summaries,
 * recalculating weekly goal progress, and evaluating eco-achievement badges.
 */
class CarbonRollupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "CarbonRollupWorker"
        const val UNIQUE_WORK_NAME = "CarbonRollupWork"

        /**
         * Enqueues unique periodic background rollup to run every 12 hours.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<CarbonRollupWorker>(
                12, TimeUnit.HOURS,
                30, TimeUnit.MINUTES // 30-minute flex interval
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            Log.i(TAG, "Scheduled unique periodic CarbonRollupWorker.")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            val app = applicationContext as? CarbonPrintApp ?: return@withContext Result.failure()

            Log.i(TAG, "Executing periodic carbon rollup...")

            // 1. Refresh weekly goal progress
            app.goalRepository.updateCurrentWeekGoalProgress()

            // 2. Perform badge check based on aggregated cumulative totals
            val goal = app.goalRepository.getActiveGoal()
            app.badgeRepository.evaluateAllBadgesFromDatabase(
                isGoalAchieved = goal?.isAchieved ?: false
            )

            Log.i(TAG, "Carbon rollup completed successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error executing CarbonRollupWorker: ${e.message}", e)
            Result.retry()
        }
    }
}
