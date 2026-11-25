package com.carbonprint.app.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.carbonprint.app.CarbonPrintApp
import com.carbonprint.app.domain.model.TransportMode
import com.carbonprint.app.service.ActivityRecognitionManager
import com.carbonprint.app.service.TripTrackingService
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult

/**
 * BroadcastReceiver triggered by Google Play Services Activity Recognition Transition and periodic updates.
 * Automatically initiates, adjusts, or concludes GPS tracking when physical movement begins, transitions, or stops.
 */
class ActivityTransitionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ActivityTransitionRcvr"
        const val ACTION_PROCESS_ACTIVITY_TRANSITIONS =
            "com.carbonprint.app.ACTION_PROCESS_ACTIVITY_TRANSITIONS"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val app = context.applicationContext as? CarbonPrintApp ?: return

        // Respect user background sensing toggle
        if (!app.preferencesManager.isActivityRecognitionEnabled) {
            Log.d(TAG, "Activity recognition is disabled by user preference. Skipping.")
            return
        }

        // 1. Process Google Play Activity Transition events
        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent) ?: return
            for (event in result.transitionEvents) {
                val mode = ActivityRecognitionManager.toTransportMode(event.activityType)
                val isEnter = event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER

                Log.i(TAG, "Activity Transition: mode=$mode, transition=${if (isEnter) "ENTER" else "EXIT"}")

                if (isEnter) {
                    handleDetectedActivity(context, mode, confidence = 100)
                }
            }
            return
        }

        // 2. Process Google Play Periodic Activity Recognition results (with confidence scores)
        if (ActivityRecognitionResult.hasResult(intent)) {
            val result = ActivityRecognitionResult.extractResult(intent) ?: return
            val mostProbable = result.mostProbableActivity ?: return
            val mode = ActivityRecognitionManager.toTransportMode(mostProbable.type)
            val confidence = mostProbable.confidence

            Log.d(TAG, "Periodic Activity Recognition: mode=$mode, confidence=$confidence%")
            handleDetectedActivity(context, mode, confidence)
        }
    }

    private fun handleDetectedActivity(context: Context, mode: TransportMode, confidence: Int) {
        when (mode) {
            TransportMode.WALKING,
            TransportMode.RUNNING,
            TransportMode.ON_BICYCLE,
            TransportMode.IN_VEHICLE -> {
                val hasLocationPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (!hasLocationPermission) {
                    Log.w(TAG, "Location permission missing. Cannot auto-start tracking for $mode.")
                    return
                }

                if (!TripTrackingService.isTracking.value) {
                    // Only auto-start if confidence is solid (>= 60%)
                    if (confidence >= 60) {
                        Log.i(TAG, "Auto-starting trip tracking for $mode (confidence=$confidence%)")
                        TripTrackingService.startTracking(context, mode, isAuto = true)
                    }
                } else {
                    // Active tracking: inform service of movement resumption or mode change with confidence
                    Log.i(TAG, "Active tracking continues with movement: $mode ($confidence%)")
                    TripTrackingService.notifyActivityDetected(context, mode, confidence)
                }
            }
            TransportMode.STILL -> {
                // User stationary: delegate to service for debounced stationary handling
                if (TripTrackingService.isTracking.value && confidence >= 50) {
                    Log.i(TAG, "Stationary detected ($confidence%). Notifying service for debounce window.")
                    TripTrackingService.notifyStillDetected(context)
                }
            }
            else -> {}
        }
    }
}
