package com.carbonprint.app

import android.app.Application
import com.carbonprint.app.data.local.CarbonPrintDatabase
import com.carbonprint.app.data.preferences.UserPreferencesManager
import com.carbonprint.app.data.repository.AnalyticsRepository
import com.carbonprint.app.data.repository.BadgeRepository
import com.carbonprint.app.data.repository.GoalRepository
import com.carbonprint.app.data.repository.TripRepository
import com.carbonprint.app.service.ActivityRecognitionManager
import com.carbonprint.app.service.NotificationHelper
import com.carbonprint.app.worker.CarbonRollupWorker
import com.carbonprint.app.worker.GoalReminderWorker

class CarbonPrintApp : Application() {

    val database: CarbonPrintDatabase by lazy {
        CarbonPrintDatabase.getInstance(this)
    }

    val preferencesManager: UserPreferencesManager by lazy {
        UserPreferencesManager(this)
    }

    val tripRepository: TripRepository by lazy {
        TripRepository(
            tripDao = database.tripDao(),
            waypointDao = database.waypointDao(),
            dailySummaryDao = database.dailySummaryDao(),
            goalDao = database.goalDao(),
            preferencesManager = preferencesManager
        )
    }

    val goalRepository: GoalRepository by lazy {
        GoalRepository(
            goalDao = database.goalDao(),
            tripDao = database.tripDao(),
            preferencesManager = preferencesManager
        )
    }

    val badgeRepository: BadgeRepository by lazy {
        BadgeRepository(
            badgeDao = database.badgeDao(),
            tripDao = database.tripDao(),
            dailySummaryDao = database.dailySummaryDao()
        )
    }

    val analyticsRepository: AnalyticsRepository by lazy {
        AnalyticsRepository(
            dailySummaryDao = database.dailySummaryDao(),
            tripDao = database.tripDao()
        )
    }

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Android 8.0+ notification channels
        NotificationHelper.createNotificationChannels(this)

        // 2. Schedule background WorkManager tasks
        CarbonRollupWorker.schedule(this)
        GoalReminderWorker.schedule(this)

        // 3. Register activity transitions if enabled and permission exists
        if (preferencesManager.isActivityRecognitionEnabled &&
            ActivityRecognitionManager.hasActivityRecognitionPermission(this)
        ) {
            ActivityRecognitionManager.registerActivityTransitions(this)
        }
    }
}
