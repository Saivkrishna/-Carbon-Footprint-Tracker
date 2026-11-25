package com.carbonprint.app.domain.tracking

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.sqrt

/**
 * Lightweight low-power sensor tracker sampling accelerometer and gyroscope
 * at [SensorManager.SENSOR_DELAY_NORMAL] to evaluate movement dynamics,
 * vibration variance, and torso/wheel angular velocity.
 *
 * Implements safe fallbacks when sensors are unavailable on hardware or in JVM unit test runners.
 */
class MotionSensorTracker(context: Context? = null) : SensorEventListener {

    companion object {
        private const val TAG = "MotionSensorTracker"
        private const val WINDOW_SIZE = 20 // ~4-5 seconds at SENSOR_DELAY_NORMAL (5 Hz)
    }

    private val sensorManager: SensorManager? =
        context?.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    val isAccelerometerAvailable: Boolean = accelerometer != null
    val isGyroscopeAvailable: Boolean = gyroscope != null

    private var isListening: Boolean = false

    // Circular buffers for statistical calculations
    private val accelMagnitudes = DoubleArray(WINDOW_SIZE)
    private var accelIndex = 0
    private var accelCount = 0

    private val gyroMagnitudes = DoubleArray(WINDOW_SIZE)
    private var gyroIndex = 0
    private var gyroCount = 0

    private fun logDebug(msg: String) {
        try {
            Log.d(TAG, msg)
        } catch (_: Throwable) {
            // Safe fallback for local JVM test runners
        }
    }

    private fun logWarn(msg: String) {
        try {
            Log.w(TAG, msg)
        } catch (_: Throwable) {
            // Safe fallback for local JVM test runners
        }
    }

    /**
     * Starts listening to accelerometer and gyroscope at low-power normal delay.
     */
    fun startListening() {
        if (sensorManager != null && !isListening) {
            try {
                accelerometer?.let {
                    sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
                }
                gyroscope?.let {
                    sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
                }
                isListening = true
                logDebug("Registered motion sensors (accel=$isAccelerometerAvailable, gyro=$isGyroscopeAvailable)")
            } catch (e: Exception) {
                logWarn("Failed to register motion sensors: ${e.message}")
            }
        }
    }

    /**
     * Stops listening to unregister hardware sensors and consume zero battery.
     */
    fun stopListening() {
        if (sensorManager != null && isListening) {
            try {
                sensorManager.unregisterListener(this)
                isListening = false
                logDebug("Unregistered motion sensors")
            } catch (e: Exception) {
                logWarn("Failed to unregister motion sensors: ${e.message}")
            }
        }
    }

    /**
     * Computes the dynamic variance of acceleration magnitude over the sliding window.
     * Returns null if no sensor data is available.
     */
    fun getCurrentVariance(): Double? {
        if (accelCount < 5) return null
        val count = minOf(accelCount, WINDOW_SIZE)
        var sum = 0.0
        for (i in 0 until count) {
            sum += accelMagnitudes[i]
        }
        val mean = sum / count
        var varianceSum = 0.0
        for (i in 0 until count) {
            val diff = accelMagnitudes[i] - mean
            varianceSum += diff * diff
        }
        return varianceSum / count
    }

    /**
     * Computes the average angular velocity magnitude over the sliding window.
     * Returns null if gyroscope is absent.
     */
    fun getCurrentGyroMagnitude(): Double? {
        if (gyroCount < 3) return null
        val count = minOf(gyroCount, WINDOW_SIZE)
        var sum = 0.0
        for (i in 0 until count) {
            sum += gyroMagnitudes[i]
        }
        return sum / count
    }

    /**
     * Direct injection method for unit testing and manual simulation.
     */
    fun feedRawSensorData(accelMag: Double, gyroMag: Double? = null) {
        accelMagnitudes[accelIndex] = accelMag
        accelIndex = (accelIndex + 1) % WINDOW_SIZE
        if (accelCount < WINDOW_SIZE) accelCount++

        if (gyroMag != null) {
            gyroMagnitudes[gyroIndex] = gyroMag
            gyroIndex = (gyroIndex + 1) % WINDOW_SIZE
            if (gyroCount < WINDOW_SIZE) gyroCount++
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.values.isEmpty()) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0].toDouble()
                val y = event.values[1].toDouble()
                val z = event.values[2].toDouble()
                val mag = sqrt(x * x + y * y + z * z)
                feedRawSensorData(mag, null)
            }
            Sensor.TYPE_GYROSCOPE -> {
                val gx = event.values[0].toDouble()
                val gy = event.values[1].toDouble()
                val gz = event.values[2].toDouble()
                val gmag = sqrt(gx * gx + gy * gy + gz * gz)
                gyroMagnitudes[gyroIndex] = gmag
                gyroIndex = (gyroIndex + 1) % WINDOW_SIZE
                if (gyroCount < WINDOW_SIZE) gyroCount++
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
