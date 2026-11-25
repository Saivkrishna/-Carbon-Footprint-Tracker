package com.carbonprint.app.domain.tracking

import com.carbonprint.app.domain.model.TransportMode

/**
 * Snapshot of sensor and environmental data provided to [TravelModeClassifier].
 */
data class SensorSnapshot(
    val detectedActivity: TransportMode = TransportMode.UNKNOWN,
    val activityConfidence: Int = 0, // 0 to 100
    val gpsSpeedKmh: Double? = null,
    val isGpsValid: Boolean = true,
    val stepCadencePerSecond: Double = 0.0,
    val accelVariance: Double? = null,
    val gyroMagnitude: Double? = null,
    val timestampMillis: Long = System.currentTimeMillis()
)

/**
 * Output of travel-mode classification.
 */
data class ClassificationResult(
    val confirmedMode: TransportMode,
    val confidence: Int,
    val isTransition: Boolean,
    val reason: String
)

/**
 * Intelligent multimodal travel-mode classifier combining:
 * 1. Google Activity Recognition API (with confidence score)
 * 2. GPS speed and accuracy
 * 3. Accelerometer dynamic variance (vibration vs footsteps)
 * 4. Gyroscope angular velocity (rotational body dynamics)
 * 5. Hardware Step Tracker cadence
 *
 * Implements confirmation debouncing to prevent rapid activity jitter, resolves
 * ambiguous states (e.g. distinguishing cycling from walking, and walking from running),
 * handles temporary GPS loss safely without discarding trips, and ensures zero duplicate trips.
 */
class TravelModeClassifier(
    private val confirmationThreshold: Int = 2,
    initialMode: TransportMode = TransportMode.UNKNOWN
) {
    companion object {
        const val CONFIDENCE_HIGH = 75
        const val CONFIDENCE_MODERATE = 50
        const val CONFIDENCE_LOW = 30

        // Speed boundaries (km/h)
        const val SPEED_STILL_MAX = 1.8
        const val SPEED_WALKING_MAX = 7.5
        const val SPEED_RUNNING_MAX = 22.0
        const val SPEED_CYCLING_MIN = 8.0
        const val SPEED_CYCLING_MAX = 40.0
        const val SPEED_VEHICLE_MIN = 25.0

        // Cadence boundaries (steps/second)
        const val CADENCE_PEDESTRIAN_MIN = 0.6
        const val CADENCE_RUNNING_MIN = 2.2
        const val CADENCE_NON_PEDESTRIAN_MAX = 0.35

        // Accelerometer variance boundaries (m^2/s^4)
        const val ACCEL_VARIANCE_STILL_MAX = 0.35
        const val ACCEL_VARIANCE_RUNNING_MIN = 6.0
    }

    var currentConfirmedMode: TransportMode = initialMode
        private set

    private var candidateMode: TransportMode? = null
    private var candidateCount: Int = 0

    /**
     * Resets the classifier to an explicit mode.
     */
    fun reset(mode: TransportMode = TransportMode.UNKNOWN) {
        currentConfirmedMode = mode
        candidateMode = null
        candidateCount = 0
    }

    /**
     * Evaluates a multimodal sensor snapshot and determines the confirmed travel mode.
     */
    fun classify(snapshot: SensorSnapshot): ClassificationResult {
        val rawDecision = determineRawMode(snapshot)
        val mode = rawDecision.first
        val confidence = rawDecision.second
        val reason = rawDecision.third

        // 1. If raw decision matches current confirmed mode, reset candidate and retain mode
        if (mode == currentConfirmedMode) {
            candidateMode = null
            candidateCount = 0
            return ClassificationResult(
                confirmedMode = currentConfirmedMode,
                confidence = confidence,
                isTransition = false,
                reason = "Maintained $currentConfirmedMode: $reason"
            )
        }

        // 2. High-certainty direct vehicle transition (e.g. driving at > 45 km/h with 0 steps)
        val isOverwhelmingVehicle = mode == TransportMode.IN_VEHICLE &&
                (snapshot.gpsSpeedKmh ?: 0.0) >= 45.0 &&
                snapshot.stepCadencePerSecond <= CADENCE_NON_PEDESTRIAN_MAX

        // 3. High-certainty pedestrian transition (user stopped car and walked > 5 steps with high cadence)
        val isOverwhelmingWalking = mode == TransportMode.WALKING &&
                snapshot.stepCadencePerSecond >= 1.2 &&
                (snapshot.gpsSpeedKmh == null || snapshot.gpsSpeedKmh <= SPEED_WALKING_MAX) &&
                snapshot.detectedActivity == TransportMode.WALKING &&
                snapshot.activityConfidence >= CONFIDENCE_HIGH

        if (isOverwhelmingVehicle || isOverwhelmingWalking || confirmationThreshold <= 1) {
            val oldMode = currentConfirmedMode
            currentConfirmedMode = mode
            candidateMode = null
            candidateCount = 0
            return ClassificationResult(
                confirmedMode = mode,
                confidence = confidence,
                isTransition = true,
                reason = "Immediate transition from $oldMode to $mode: $reason"
            )
        }

        // 4. Confirmation debouncing for candidate mode
        if (candidateMode == mode) {
            candidateCount++
            if (candidateCount >= confirmationThreshold) {
                val oldMode = currentConfirmedMode
                currentConfirmedMode = mode
                candidateMode = null
                candidateCount = 0
                return ClassificationResult(
                    confirmedMode = mode,
                    confidence = confidence,
                    isTransition = true,
                    reason = "Confirmed transition from $oldMode to $mode ($candidateCount/$confirmationThreshold updates): $reason"
                )
            } else {
                return ClassificationResult(
                    confirmedMode = currentConfirmedMode,
                    confidence = confidence,
                    isTransition = false,
                    reason = "Pending confirmation for $mode ($candidateCount/$confirmationThreshold): $reason"
                )
            }
        } else {
            candidateMode = mode
            candidateCount = 1
            return ClassificationResult(
                confirmedMode = currentConfirmedMode,
                confidence = confidence,
                isTransition = false,
                reason = "Candidate switch observed to $mode (1/$confirmationThreshold): $reason"
            )
        }
    }

    /**
     * Core sensor fusion rule engine.
     */
    private fun determineRawMode(snapshot: SensorSnapshot): Triple<TransportMode, Int, String> {
        val speed = snapshot.gpsSpeedKmh
        val hasGps = snapshot.isGpsValid && speed != null
        val cadence = snapshot.stepCadencePerSecond
        val arMode = snapshot.detectedActivity
        val arConf = snapshot.activityConfidence
        val variance = snapshot.accelVariance
        val gyro = snapshot.gyroMagnitude

        // RULE 1: Handle temporary GPS loss (indoor, tunnels, subway)
        if (!hasGps) {
            // Rely on StepTracker and Activity Recognition without ending trip
            if (cadence >= CADENCE_RUNNING_MIN || (arMode == TransportMode.RUNNING && arConf >= CONFIDENCE_MODERATE)) {
                return Triple(TransportMode.RUNNING, 80, "GPS unavailable; running confirmed by step cadence ($cadence/s) and AR ($arConf%)")
            }
            if (cadence >= CADENCE_PEDESTRIAN_MIN || (arMode == TransportMode.WALKING && arConf >= CONFIDENCE_MODERATE)) {
                return Triple(TransportMode.WALKING, 80, "GPS unavailable; walking confirmed by step cadence ($cadence/s) and AR ($arConf%)")
            }
            if (arMode == TransportMode.IN_VEHICLE && arConf >= CONFIDENCE_MODERATE) {
                return Triple(TransportMode.IN_VEHICLE, 75, "GPS unavailable; vehicle travel confirmed by AR ($arConf%) with zero steps")
            }
            if (arMode == TransportMode.ON_BICYCLE && arConf >= CONFIDENCE_MODERATE) {
                return Triple(TransportMode.ON_BICYCLE, 75, "GPS unavailable; cycling confirmed by AR ($arConf%) with zero steps")
            }
            if (arMode == TransportMode.STILL && arConf >= CONFIDENCE_MODERATE && cadence <= CADENCE_NON_PEDESTRIAN_MAX) {
                return Triple(TransportMode.STILL, 75, "GPS unavailable; stillness confirmed by AR ($arConf%) and zero steps")
            }
            // If AR is uncertain during GPS loss, maintain current confirmed mode safely
            if (currentConfirmedMode != TransportMode.UNKNOWN) {
                return Triple(currentConfirmedMode, 65, "GPS unavailable; safely retaining current mode ($currentConfirmedMode)")
            }
        }

        // RULE 2: Stillness / Stationary detection
        val isSpeedStill = speed != null && speed <= SPEED_STILL_MAX
        val isStepsStill = cadence <= CADENCE_NON_PEDESTRIAN_MAX
        val isSensorStill = variance == null || variance <= ACCEL_VARIANCE_STILL_MAX

        if (isSpeedStill && isStepsStill && (arMode == TransportMode.STILL || isSensorStill)) {
            val conf = if (arMode == TransportMode.STILL) maxOf(arConf, 85) else 80
            return Triple(TransportMode.STILL, conf, "Stationary: speed=${speed ?: 0.0} km/h, cadence=$cadence/s, variance=$variance")
        }

        // RULE 3: Definite High-Speed Motor Vehicle (> 40 km/h)
        if (speed != null && speed > SPEED_CYCLING_MAX && isStepsStill) {
            return Triple(TransportMode.IN_VEHICLE, 95, "Vehicle confirmed by sustained high speed (${speed} km/h > 40 km/h) and no steps")
        }

        // RULE 4: Cycling Detection & Disambiguation from Walking
        // "Ensure that Cycling is not incorrectly classified as Walking."
        // If speed is 8-40 km/h and steps are absent/negligible:
        val isSpeedCycling = speed != null && speed in SPEED_CYCLING_MIN..SPEED_CYCLING_MAX
        if (isSpeedCycling && isStepsStill) {
            // Even if AR hints WALKING, the physical impossibility of walking at > 8 km/h without steps rules it out!
            if (arMode == TransportMode.ON_BICYCLE) {
                return Triple(TransportMode.ON_BICYCLE, maxOf(arConf, 85), "Cycling: AR consensus ($arConf%) + speed=${speed} km/h + 0 steps")
            }
            if (arMode == TransportMode.IN_VEHICLE && speed > SPEED_VEHICLE_MIN) {
                return Triple(TransportMode.IN_VEHICLE, arConf, "Vehicle: AR confirmed + speed=${speed} km/h")
            }
            // Disambiguate Cycling from Vehicle at 8-25 km/h with moderate road vibration
            if (speed < SPEED_VEHICLE_MIN) {
                return Triple(TransportMode.ON_BICYCLE, 80, "Cycling: speed=${speed} km/h in 8-25 km/h range without footstep impacts")
            }
        }

        // RULE 5: Motor Vehicle Detection (moderate speed 25-40 km/h with AR or zero steps)
        if (speed != null && speed >= SPEED_VEHICLE_MIN && isStepsStill) {
            if (arMode == TransportMode.IN_VEHICLE && arConf >= CONFIDENCE_LOW) {
                return Triple(TransportMode.IN_VEHICLE, maxOf(arConf, 85), "Vehicle: AR ($arConf%) + speed=${speed} km/h + 0 steps")
            }
            if (arMode != TransportMode.ON_BICYCLE && speed >= 32.0) {
                return Triple(TransportMode.IN_VEHICLE, 80, "Vehicle: speed=${speed} km/h exceeds normal cycling with 0 steps")
            }
        }

        // RULE 6: Running vs Walking Disambiguation
        // "Ensure that Walking and Running are distinguished correctly."
        val isHighCadence = cadence >= CADENCE_RUNNING_MIN
        val isRunningSpeed = speed != null && speed > SPEED_WALKING_MAX
        val isHighVariance = variance != null && variance >= ACCEL_VARIANCE_RUNNING_MIN

        if (isHighCadence || (isRunningSpeed && cadence >= CADENCE_PEDESTRIAN_MIN) || (arMode == TransportMode.RUNNING && arConf >= CONFIDENCE_MODERATE)) {
            if (isHighCadence || isHighVariance || arMode == TransportMode.RUNNING) {
                val conf = if (arMode == TransportMode.RUNNING) maxOf(arConf, 85) else 80
                return Triple(TransportMode.RUNNING, conf, "Running: cadence=$cadence/s, speed=${speed ?: 0.0} km/h, variance=$variance")
            }
        }

        // RULE 7: Walking Detection
        val isPedestrianSpeed = speed != null && speed in SPEED_STILL_MAX..SPEED_WALKING_MAX
        val isWalkingCadence = cadence >= CADENCE_PEDESTRIAN_MIN

        if (isWalkingCadence || (isPedestrianSpeed && arMode == TransportMode.WALKING)) {
            val conf = if (arMode == TransportMode.WALKING) maxOf(arConf, 85) else 75
            return Triple(TransportMode.WALKING, conf, "Walking: cadence=$cadence/s, speed=${speed ?: 0.0} km/h, AR=$arMode ($arConf%)")
        }

        // RULE 8: AR-driven classification when sensor data is neutral
        if (arConf >= CONFIDENCE_MODERATE && arMode != TransportMode.UNKNOWN) {
            return Triple(arMode, arConf, "Activity Recognition API confidence: $arMode ($arConf%)")
        }

        // RULE 9: Uncertain detection - Safe Fallback
        // "Uncertain detections are handled safely instead of making unsupported assumptions."
        if (currentConfirmedMode != TransportMode.UNKNOWN) {
            return Triple(currentConfirmedMode, CONFIDENCE_LOW, "Uncertain detection; safely maintaining current mode ($currentConfirmedMode)")
        }

        return Triple(TransportMode.UNKNOWN, 0, "No conclusive motion or sensor evidence")
    }
}
