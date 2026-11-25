package com.carbonprint.app.domain.tracking

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.carbonprint.app.domain.model.TransportMode

/**
 * Manages high-accuracy hardware step counting for walking and running activities.
 *
 * Implements Android hardware sensor integration:
 * 1. Prefers [Sensor.TYPE_STEP_COUNTER] for continuous, low-power hardware step accumulation.
 * 2. Falls back to [Sensor.TYPE_STEP_DETECTOR] when step counter is unavailable.
 * 3. Classifies steps strictly between walking and running according to the active activity recognition mode.
 * 4. Isolates trip-specific steps from historical steps using hardware baseline recording.
 * 5. Handles sensor counter resets, device reboots, and transitions between walking and running safely.
 */
class StepTracker(context: Context? = null) : SensorEventListener {

    private val sensorManager: SensorManager? =
        context?.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    val stepCounterSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    val stepDetectorSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    // Hardware preference: prefer TYPE_STEP_COUNTER, fallback to TYPE_STEP_DETECTOR
    val activeSensor: Sensor? = stepCounterSensor ?: stepDetectorSensor
    val isSensorAvailable: Boolean = activeSensor != null
    val isUsingStepCounter: Boolean = stepCounterSensor != null

    private var isSensorRegistered: Boolean = false

    private var currentMode: TransportMode = TransportMode.UNKNOWN

    // Trip-specific step accumulators
    private var walkingSteps: Int = 0
    private var runningSteps: Int = 0

    // Sensor baseline tracking for TYPE_STEP_COUNTER
    private var baselineStepCounter: Long = -1L
    private var lastRawStepCounter: Long = -1L

    // Sliding window of recent step timestamps for live cadence computation
    private val recentStepTimestamps = LongArray(60)
    private var stepTimestampIndex = 0
    private var stepTimestampCount = 0

    private fun recordStepTimestamps(count: Int, timestampMillis: Long) {
        val stepInterval = if (count > 1) 500L else 0L
        for (i in 0 until count) {
            val ts = timestampMillis - (count - 1 - i) * stepInterval
            recentStepTimestamps[stepTimestampIndex] = ts
            stepTimestampIndex = (stepTimestampIndex + 1) % recentStepTimestamps.size
            if (stepTimestampCount < recentStepTimestamps.size) stepTimestampCount++
        }
    }

    /**
     * Computes current step cadence in steps per second over a 5-second observation window.
     */
    fun getRecentStepCadence(nowMillis: Long = System.currentTimeMillis(), windowSeconds: Double = 5.0): Double {
        if (stepTimestampCount == 0 || windowSeconds <= 0.0) return 0.0
        val cutoff = nowMillis - (windowSeconds * 1000L).toLong()
        var count = 0
        val total = minOf(stepTimestampCount, recentStepTimestamps.size)
        for (i in 0 until total) {
            if (recentStepTimestamps[i] >= cutoff) {
                count++
            }
        }
        return count / windowSeconds
    }

    /**
     * Listener callback invoked whenever step counts update.
     */
    var onStepsChanged: ((walkingSteps: Int, runningSteps: Int) -> Unit)? = null

    private fun logDebug(tag: String, msg: String) {
        try {
            Log.d(tag, msg)
        } catch (_: Throwable) {
            // Safe fallback for local JVM unit test runners
        }
    }

    private fun logWarn(tag: String, msg: String) {
        try {
            Log.w(tag, msg)
        } catch (_: Throwable) {
            // Safe fallback for local JVM unit test runners
        }
    }

    /**
     * Starts listening to the preferred hardware step sensor.
     */
    fun startListening() {
        if (sensorManager != null && activeSensor != null && !isSensorRegistered) {
            try {
                isSensorRegistered = sensorManager.registerListener(
                    this,
                    activeSensor,
                    SensorManager.SENSOR_DELAY_UI
                )
                logDebug(TAG, "Step sensor registered: ${activeSensor.name} (type=${activeSensor.type})")
            } catch (e: Exception) {
                logWarn(TAG, "Failed to register step sensor: ${e.message}")
            }
        }
    }

    /**
     * Stops listening to hardware step sensor events.
     */
    fun stopListening() {
        if (sensorManager != null && isSensorRegistered) {
            try {
                sensorManager.unregisterListener(this)
                isSensorRegistered = false
                logDebug(TAG, "Step sensor unregistered")
            } catch (e: Exception) {
                logWarn(TAG, "Failed to unregister step sensor: ${e.message}")
            }
        }
    }

    /**
     * Resets accumulators and establishes a fresh baseline for a new trip session.
     */
    fun reset(initialMode: TransportMode = TransportMode.UNKNOWN) {
        currentMode = initialMode
        walkingSteps = 0
        runningSteps = 0
        baselineStepCounter = -1L
        lastRawStepCounter = -1L
        stepTimestampIndex = 0
        stepTimestampCount = 0
        onStepsChanged?.invoke(0, 0)
    }

    /**
     * Smoothly updates the active travel mode (e.g. from WALKING to RUNNING)
     * preserving accumulated counts and classifying subsequent steps accurately.
     */
    fun onActivityModeChanged(newMode: TransportMode) {
        if (currentMode != newMode) {
            logDebug(TAG, "Activity mode transition: $currentMode -> $newMode (walkingSteps=$walkingSteps, runningSteps=$runningSteps)")
            currentMode = newMode
        }
    }

    /**
     * Returns total walking steps accumulated for the active trip.
     */
    fun getWalkingSteps(): Int = walkingSteps

    /**
     * Returns total running steps accumulated for the active trip.
     */
    fun getRunningSteps(): Int = runningSteps

    /**
     * Returns combined total steps for the active trip.
     */
    fun getTotalSteps(): Int = walkingSteps + runningSteps

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.values.isEmpty()) return

        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val rawCount = event.values[0].toLong()
                processStepCounterReading(rawCount)
            }
            Sensor.TYPE_STEP_DETECTOR -> {
                if (event.values[0] > 0f) {
                    processStepDetectorReading()
                }
            }
        }
    }

    /**
     * Processes a cumulative hardware step counter reading.
     * Can be invoked directly with simulated readings for unit testing.
     */
    fun processStepCounterReading(rawCount: Long, timestampMillis: Long = System.currentTimeMillis()) {
        if (rawCount < 0L) return

        // 1. First reading: record the trip baseline
        if (baselineStepCounter < 0L) {
            baselineStepCounter = rawCount
            lastRawStepCounter = rawCount
            logDebug(TAG, "Established step counter baseline: $baselineStepCounter")
            return
        }

        // 2. Hardware counter reset or device reboot detection
        if (rawCount < lastRawStepCounter) {
            logWarn(TAG, "Step counter reset detected: rawCount=$rawCount < lastRaw=$lastRawStepCounter. Resetting baseline.")
            baselineStepCounter = rawCount
            lastRawStepCounter = rawCount
            return
        }

        // 3. Calculate steps taken since last sensor update
        val delta = (rawCount - lastRawStepCounter).toInt()
        if (delta <= 0) {
            return // No new steps or duplicate event
        }

        // 4. Classify steps strictly into walking or running
        recordStepTimestamps(delta, timestampMillis)
        when (currentMode) {
            TransportMode.WALKING -> {
                walkingSteps += delta
            }
            TransportMode.RUNNING -> {
                runningSteps += delta
            }
            else -> {
                // STILL, IN_VEHICLE, ON_BICYCLE, or UNCERTAIN
                // "If activity classification is uncertain, do not silently assign steps to the wrong category."
                logDebug(TAG, "Ignored $delta step(s) during non-pedestrian mode: $currentMode")
            }
        }

        lastRawStepCounter = rawCount
        onStepsChanged?.invoke(walkingSteps, runningSteps)
    }

    /**
     * Processes an individual step detected event from TYPE_STEP_DETECTOR.
     * Can be invoked directly with simulated readings for unit testing.
     */
    fun processStepDetectorReading(timestampMillis: Long = System.currentTimeMillis()) {
        recordStepTimestamps(1, timestampMillis)
        when (currentMode) {
            TransportMode.WALKING -> {
                walkingSteps++
                onStepsChanged?.invoke(walkingSteps, runningSteps)
            }
            TransportMode.RUNNING -> {
                runningSteps++
                onStepsChanged?.invoke(walkingSteps, runningSteps)
            }
            else -> {
                // Not walking or running; ignore step event
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op for step sensors
    }

    /**
     * Legacy compatibility method returning total steps for walking/running.
     */
    fun getSteps(mode: TransportMode, distanceMeters: Double = 0.0): Int {
        return when (mode) {
            TransportMode.WALKING -> walkingSteps
            TransportMode.RUNNING -> runningSteps
            else -> getTotalSteps()
        }
    }

    companion object {
        private const val TAG = "StepTracker"

        // Biomechanical reference strides for validation and estimation:
        const val WALKING_STRIDE_METERS = 0.76
        const val RUNNING_STRIDE_METERS = 1.05

        /**
         * Calculates steps based on GPS distance and mode-specific average stride length.
         */
        fun calculateEstimatedSteps(mode: TransportMode, distanceMeters: Double): Int {
            if (distanceMeters <= 0.0 || !distanceMeters.isFinite()) return 0
            val stride = when (mode) {
                TransportMode.WALKING -> WALKING_STRIDE_METERS
                TransportMode.RUNNING -> RUNNING_STRIDE_METERS
                else -> return 0
            }
            return (distanceMeters / stride).toInt().coerceAtLeast(0)
        }
    }
}
