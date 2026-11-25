package com.carbonprint.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.carbonprint.app.MainActivity
import com.carbonprint.app.R
import com.carbonprint.app.domain.model.TransportMode

/**
 * Manages notification channels, foreground tracking service notifications, and system alerts.
 */
object NotificationHelper {

    const val CHANNEL_ID_TRACKING = "carbonprint_tracking_channel"
    const val CHANNEL_ID_ALERTS = "carbonprint_alerts_channel"
    const val CHANNEL_ID_BADGES = "carbonprint_badges_channel"

    const val NOTIFICATION_ID_TRACKING = 1001
    const val NOTIFICATION_ID_GOAL_ALERT = 2001
    const val NOTIFICATION_ID_BADGE = 3001

    /**
     * Initializes notification channels required for Android 8.0 (API 26) and above.
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val trackingChannel = NotificationChannel(
                CHANNEL_ID_TRACKING,
                "Live Trip Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live GPS distance, duration and CO2 emission for active journeys"
                setShowBadge(false)
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_ID_ALERTS,
                "Carbon & Goal Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies when approaching weekly carbon budgets or low-carbon suggestions"
            }

            val badgesChannel = NotificationChannel(
                CHANNEL_ID_BADGES,
                "Eco Badges & Achievements",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Celebrates newly unlocked green travel milestone badges"
            }

            notificationManager.createNotificationChannels(
                listOf(trackingChannel, alertsChannel, badgesChannel)
            )
        }
    }

    /**
     * Builds ongoing notification for [TripTrackingService].
     */
    fun buildTrackingNotification(
        context: Context,
        mode: TransportMode,
        distanceMeters: Double,
        durationSeconds: Long,
        co2Grams: Double,
        isAutoDetected: Boolean,
        isStationaryPaused: Boolean = false,
        stepCount: Int = 0
    ): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            flags
        )

        val stopTripIntent = Intent(context, TripTrackingService::class.java).apply {
            action = TripTrackingService.ACTION_STOP_TRACKING
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            1,
            stopTripIntent,
            flags
        )

        val distanceKm = String.format("%.2f", distanceMeters / 1000.0)
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        val timeFormatted = String.format("%02d:%02d", minutes, seconds)
        val co2Formatted = if (co2Grams >= 1000.0) {
            String.format("%.2f kg", co2Grams / 1000.0)
        } else {
            String.format("%.0f g", co2Grams)
        }

        val modeLabel = when {
            isStationaryPaused -> "${mode.displayName} (Stationary)"
            isAutoDetected -> "${mode.displayName} (Auto)"
            else -> mode.displayName
        }
        val contentText = when {
            isStationaryPaused -> "$distanceKm km  •  Paused at stop/signal  •  Auto-resuming"
            (mode == TransportMode.WALKING || mode == TransportMode.RUNNING) && stepCount > 0 ->
                "$distanceKm km  •  $timeFormatted  •  $stepCount steps"
            else -> "$distanceKm km  •  $timeFormatted  •  $co2Formatted CO2"
        }

        return NotificationCompat.Builder(context, CHANNEL_ID_TRACKING)
            .setContentTitle("Tracking Trip: $modeLabel")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Trip",
                stopPendingIntent
            )
            .build()
    }

    /**
     * Sends a carbon budget alert notification if POST_NOTIFICATIONS is granted.
     */
    fun showGoalAlertNotification(context: Context, title: String, message: String) {
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_GOAL_ALERT,
            intent,
            flags
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_GOAL_ALERT, notification)
    }

    /**
     * Sends a celebration notification when an eco-badge is unlocked.
     */
    fun showBadgeUnlockedNotification(context: Context, badgeTitle: String, badgeDesc: String) {
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_BADGE,
            intent,
            flags
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_BADGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("New Eco Badge Unlocked! 🏅")
            .setContentText("$badgeTitle — $badgeDesc")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Congratulations! You've unlocked '$badgeTitle': $badgeDesc"))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BADGE, notification)
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }
}
