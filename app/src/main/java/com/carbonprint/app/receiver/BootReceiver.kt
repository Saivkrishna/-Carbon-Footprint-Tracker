package com.carbonprint.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.carbonprint.app.CarbonPrintApp
import com.carbonprint.app.service.ActivityRecognitionManager
import com.carbonprint.app.worker.CarbonRollupWorker
import com.carbonprint.app.worker.GoalReminderWorker

/**
 * Re-arms activity transition monitoring and background WorkManager tasks following device restart.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        Log.i(TAG, "Device reboot completed. Re-initializing CarbonPrint background services.")

        val app = context.applicationContext as? CarbonPrintApp ?: return

        // Clear any stale tracking flags from before device shutdown
        app.preferencesManager.isTrackingActive = false
        app.preferencesManager.currentActiveTripId = -1L

        // Re-register activity recognition ONLY if enabled by the user AND runtime permission is granted
        if (app.preferencesManager.isActivityRecognitionEnabled &&
            ActivityRecognitionManager.hasActivityRecognitionPermission(app)
        ) {
            Log.i(TAG, "Re-registering activity transitions post-boot.")
            ActivityRecognitionManager.registerActivityTransitions(app)
        } else {
            Log.i(TAG, "Activity recognition post-boot registration skipped (disabled or permission missing).")
        }

        // Schedule periodic maintenance tasks without bypassing background constraints
        CarbonRollupWorker.schedule(app)
        GoalReminderWorker.schedule(app)
    }
}
