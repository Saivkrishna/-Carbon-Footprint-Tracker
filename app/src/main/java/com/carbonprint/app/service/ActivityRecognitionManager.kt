package com.carbonprint.app.service

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.carbonprint.app.domain.model.TransportMode
import com.carbonprint.app.receiver.ActivityTransitionReceiver
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.tasks.Task

/**
 * Manages Google Play Services Activity Recognition Transition registration and activity mapping.
 */
object ActivityRecognitionManager {

    private const val TAG = "ActivityRecognitionMgr"
    private const val TRANSITION_PENDING_INTENT_REQUEST_CODE = 4001

    /**
     * Builds and registers activity transition requests with Google Play Services.
     */
    fun registerActivityTransitions(context: Context): Task<Void>? {
        if (!hasActivityRecognitionPermission(context)) {
            Log.w(TAG, "Cannot register activity transitions: ACTIVITY_RECOGNITION permission missing.")
            return null
        }

        val transitions = mutableListOf<ActivityTransition>()

        val trackedActivities = intArrayOf(
            DetectedActivity.IN_VEHICLE,
            DetectedActivity.ON_BICYCLE,
            DetectedActivity.WALKING,
            DetectedActivity.RUNNING,
            DetectedActivity.STILL
        )

        for (activity in trackedActivities) {
            transitions.add(
                ActivityTransition.Builder()
                    .setActivityType(activity)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()
            )
            transitions.add(
                ActivityTransition.Builder()
                    .setActivityType(activity)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build()
            )
        }

        val request = ActivityTransitionRequest(transitions)
        val pendingIntent = getTransitionPendingIntent(context)

        return try {
            val client = ActivityRecognition.getClient(context)
            // Register periodic confidence updates as well for richer sensor fusion
            try {
                client.requestActivityUpdates(10_000L, pendingIntent)
            } catch (e: Exception) {
                Log.w(TAG, "Could not register periodic activity updates: ${e.message}")
            }

            client.requestActivityTransitionUpdates(request, pendingIntent)
                .addOnSuccessListener {
                    Log.i(TAG, "Successfully registered Google Play Activity Transitions.")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to register activity transitions: ${e.message}", e)
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException registering activity transitions: ${e.message}", e)
            null
        }
    }

    /**
     * Unregisters activity transitions when tracking is disabled by the user.
     */
    fun unregisterActivityTransitions(context: Context): Task<Void>? {
        if (!hasActivityRecognitionPermission(context)) return null
        val pendingIntent = getTransitionPendingIntent(context)
        return try {
            val client = ActivityRecognition.getClient(context)
            try {
                client.removeActivityUpdates(pendingIntent)
            } catch (_: Exception) {}

            client.removeActivityTransitionUpdates(pendingIntent)
                .addOnSuccessListener {
                    Log.i(TAG, "Successfully unregistered activity transitions.")
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException unregistering activity transitions", e)
            null
        }
    }

    /**
     * Maps DetectedActivity integers to domain [TransportMode].
     */
    fun toTransportMode(detectedActivityType: Int): TransportMode {
        return when (detectedActivityType) {
            DetectedActivity.IN_VEHICLE -> TransportMode.IN_VEHICLE
            DetectedActivity.ON_BICYCLE -> TransportMode.ON_BICYCLE
            DetectedActivity.WALKING -> TransportMode.WALKING
            DetectedActivity.RUNNING -> TransportMode.RUNNING
            DetectedActivity.STILL -> TransportMode.STILL
            else -> TransportMode.UNKNOWN
        }
    }

    fun hasActivityRecognitionPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun getTransitionPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ActivityTransitionReceiver::class.java).apply {
            action = ActivityTransitionReceiver.ACTION_PROCESS_ACTIVITY_TRANSITIONS
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(
            context,
            TRANSITION_PENDING_INTENT_REQUEST_CODE,
            intent,
            flags
        )
    }
}
