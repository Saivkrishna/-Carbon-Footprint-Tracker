package com.carbonprint.app.domain

import com.carbonprint.app.domain.model.TransportMode
import com.carbonprint.app.domain.tracking.ClassificationResult
import com.carbonprint.app.domain.tracking.SensorSnapshot
import com.carbonprint.app.domain.tracking.StateAction
import com.carbonprint.app.domain.tracking.TrackingState
import com.carbonprint.app.domain.tracking.TrackingStateMachine
import com.carbonprint.app.domain.tracking.TravelModeClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests validating multimodal automatic travel-mode detection, activity transitions,
 * stationary debouncing, GPS loss handling, and duplicate trip prevention.
 */
class AutomaticTravelModeDetectionTest {

    private lateinit var classifier: TravelModeClassifier
    private lateinit var stateMachine: TrackingStateMachine

    @Before
    fun setUp() {
        classifier = TravelModeClassifier(confirmationThreshold = 2)
        stateMachine = TrackingStateMachine(stillTimeoutSeconds = 120L)
    }

    // 1. WALKING DETECTION
    @Test
    fun testWalkingDetection() {
        val snapshot = SensorSnapshot(
            detectedActivity = TransportMode.WALKING,
            activityConfidence = 85,
            gpsSpeedKmh = 4.5,
            isGpsValid = true,
            stepCadencePerSecond = 1.7,
            accelVariance = 2.4
        )
        // First sample establishes candidate
        classifier.classify(snapshot)
        // Second sample confirms WALKING
        val result = classifier.classify(snapshot)

        assertEquals(TransportMode.WALKING, result.confirmedMode)
        assertTrue(result.confidence >= 75)
    }

    // 2. RUNNING DETECTION
    @Test
    fun testRunningDetection() {
        val snapshot = SensorSnapshot(
            detectedActivity = TransportMode.RUNNING,
            activityConfidence = 90,
            gpsSpeedKmh = 10.8,
            isGpsValid = true,
            stepCadencePerSecond = 2.7,
            accelVariance = 11.5
        )
        classifier.classify(snapshot)
        val result = classifier.classify(snapshot)

        assertEquals(TransportMode.RUNNING, result.confirmedMode)
        assertTrue(result.confidence >= 80)
    }

    // 3. CYCLING DETECTION - Ensure cycling is not misclassified as walking
    @Test
    fun testCyclingDetection_notClassifiedAsWalking() {
        // Cycling at 18 km/h with 0 steps, even if noisy AR suggests WALKING (50%)
        val ambiguousSnapshot = SensorSnapshot(
            detectedActivity = TransportMode.WALKING,
            activityConfidence = 50,
            gpsSpeedKmh = 18.0, // Human walking at 18 km/h is physically impossible
            isGpsValid = true,
            stepCadencePerSecond = 0.0, // Zero footsteps
            accelVariance = 1.2
        )
        classifier.classify(ambiguousSnapshot)
        val result = classifier.classify(ambiguousSnapshot)

        // Must be classified as ON_BICYCLE, never WALKING!
        assertEquals(TransportMode.ON_BICYCLE, result.confirmedMode)

        // Clear cycling AR detection
        val directCycling = SensorSnapshot(
            detectedActivity = TransportMode.ON_BICYCLE,
            activityConfidence = 85,
            gpsSpeedKmh = 22.0,
            isGpsValid = true,
            stepCadencePerSecond = 0.0
        )
        classifier.classify(directCycling)
        val cyclingResult = classifier.classify(directCycling)
        assertEquals(TransportMode.ON_BICYCLE, cyclingResult.confirmedMode)
    }

    // 4. IN-VEHICLE DETECTION
    @Test
    fun testInVehicleDetection() {
        val snapshot = SensorSnapshot(
            detectedActivity = TransportMode.IN_VEHICLE,
            activityConfidence = 92,
            gpsSpeedKmh = 55.0,
            isGpsValid = true,
            stepCadencePerSecond = 0.0,
            accelVariance = 0.4
        )
        // High speed vehicle (> 45 km/h) triggers immediate confirmed transition
        val result = classifier.classify(snapshot)

        assertEquals(TransportMode.IN_VEHICLE, result.confirmedMode)
        assertTrue(result.confidence >= 90)
    }

    // 5. STILL DETECTION
    @Test
    fun testStillDetection() {
        val snapshot = SensorSnapshot(
            detectedActivity = TransportMode.STILL,
            activityConfidence = 88,
            gpsSpeedKmh = 0.0,
            isGpsValid = true,
            stepCadencePerSecond = 0.0,
            accelVariance = 0.04
        )
        classifier.classify(snapshot)
        val result = classifier.classify(snapshot)

        assertEquals(TransportMode.STILL, result.confirmedMode)
        assertTrue(result.confidence >= 80)
    }

    // 6. WALKING-TO-VEHICLE TRANSITION
    @Test
    fun testWalkingToVehicleTransition() {
        classifier.reset(TransportMode.WALKING)
        assertEquals(TransportMode.WALKING, classifier.currentConfirmedMode)

        // User enters vehicle: speed accelerates to 50 km/h with 0 steps
        val drivingSnapshot = SensorSnapshot(
            detectedActivity = TransportMode.IN_VEHICLE,
            activityConfidence = 90,
            gpsSpeedKmh = 50.0,
            isGpsValid = true,
            stepCadencePerSecond = 0.0,
            accelVariance = 0.5
        )

        val transitionResult = classifier.classify(drivingSnapshot)
        assertEquals(TransportMode.IN_VEHICLE, transitionResult.confirmedMode)
        assertTrue(transitionResult.isTransition)
    }

    // 7. VEHICLE-TO-WALKING TRANSITION
    @Test
    fun testVehicleToWalkingTransition() {
        classifier.reset(TransportMode.IN_VEHICLE)
        assertEquals(TransportMode.IN_VEHICLE, classifier.currentConfirmedMode)

        // User exits vehicle and begins walking
        val walkingSnapshot = SensorSnapshot(
            detectedActivity = TransportMode.WALKING,
            activityConfidence = 85,
            gpsSpeedKmh = 3.8,
            isGpsValid = true,
            stepCadencePerSecond = 1.6,
            accelVariance = 2.2
        )

        val transitionResult = classifier.classify(walkingSnapshot)
        assertEquals(TransportMode.WALKING, transitionResult.confirmedMode)
        assertTrue(transitionResult.isTransition)
    }

    // 8. WALKING-TO-RUNNING TRANSITION
    @Test
    fun testWalkingToRunningTransition() {
        classifier.reset(TransportMode.WALKING)

        // User accelerates from walking to running: cadence > 2.4/s, speed > 9 km/h, variance > 8
        val runningSnapshot = SensorSnapshot(
            detectedActivity = TransportMode.RUNNING,
            activityConfidence = 88,
            gpsSpeedKmh = 10.2,
            isGpsValid = true,
            stepCadencePerSecond = 2.7,
            accelVariance = 12.0
        )

        classifier.classify(runningSnapshot)
        val transitionResult = classifier.classify(runningSnapshot)
        assertEquals(TransportMode.RUNNING, transitionResult.confirmedMode)
        assertTrue(transitionResult.isTransition)
    }

    // 9. TEMPORARY GPS LOSS - Retains active mode without crashing or terminating
    @Test
    fun testTemporaryGpsLoss() {
        classifier.reset(TransportMode.IN_VEHICLE)

        // Driving into an underground road tunnel (GPS fix completely lost)
        val tunnelSnapshot = SensorSnapshot(
            detectedActivity = TransportMode.IN_VEHICLE,
            activityConfidence = 75,
            gpsSpeedKmh = null,
            isGpsValid = false,
            stepCadencePerSecond = 0.0,
            accelVariance = 0.6
        )

        val result = classifier.classify(tunnelSnapshot)
        // Must maintain IN_VEHICLE mode rather than falling back to UNKNOWN or terminating
        assertEquals(TransportMode.IN_VEHICLE, result.confirmedMode)
        assertFalse(result.isTransition)

        // Walking inside a shopping mall (GPS unavailable, but step sensor active)
        classifier.reset(TransportMode.WALKING)
        val mallSnapshot = SensorSnapshot(
            detectedActivity = TransportMode.WALKING,
            activityConfidence = 80,
            gpsSpeedKmh = null,
            isGpsValid = false,
            stepCadencePerSecond = 1.8,
            accelVariance = 2.5
        )
        val mallResult = classifier.classify(mallSnapshot)
        assertEquals(TransportMode.WALKING, mallResult.confirmedMode)
    }

    // 10. UNCERTAIN ACTIVITY RECOGNITION - Safely maintains established mode
    @Test
    fun testUncertainActivityRecognition() {
        classifier.reset(TransportMode.WALKING)

        // Low-confidence / noisy AR reading
        val noisySnapshot = SensorSnapshot(
            detectedActivity = TransportMode.UNKNOWN,
            activityConfidence = 15,
            gpsSpeedKmh = 4.0,
            isGpsValid = true,
            stepCadencePerSecond = 0.0,
            accelVariance = null
        )

        val result = classifier.classify(noisySnapshot)
        // Must hold established WALKING mode without making unsupported guesses
        assertEquals(TransportMode.WALKING, result.confirmedMode)
        assertFalse(result.isTransition)
    }

    // 11. DUPLICATE TRIP PREVENTION
    @Test
    fun testDuplicateTripPrevention() {
        // Start active auto trip
        val startAction = stateMachine.onActivityTransition(TransportMode.WALKING, tripId = 1001L, nowMillis = 1000L)
        assertTrue(startAction is StateAction.StartNewTrip)
        val activeTripId = (stateMachine.currentState as TrackingState.Active).tripId
        assertEquals(1001L, activeTripId)

        // 1. Redundant start call in same mode does NOT create new trip
        val redundantAction = stateMachine.onUserStart(TransportMode.WALKING, tripId = 1002L, nowMillis = 2000L)
        assertEquals(StateAction.None, redundantAction)
        assertEquals(1001L, (stateMachine.currentState as TrackingState.Active).tripId)

        // 2. Mode transition (Walking -> Vehicle) updates trip mode preserving active tripId
        val modeChangeAction = stateMachine.onActivityTransition(TransportMode.IN_VEHICLE, tripId = 1003L, nowMillis = 60000L)
        assertTrue(modeChangeAction is StateAction.ChangeTripMode)
        val changeAction = modeChangeAction as StateAction.ChangeTripMode
        assertEquals(TransportMode.IN_VEHICLE, changeAction.newMode)
        assertEquals(1001L, changeAction.tripId) // Initial trip preserved without duplication!

        // 3. Traffic light pause (STILL) enters debounce window without terminating trip
        val stillAction = stateMachine.onActivityTransition(TransportMode.STILL, tripId = 1001L, nowMillis = 70000L)
        assertTrue(stillAction is StateAction.BeginStillDebounce)
        assertTrue(stateMachine.currentState is TrackingState.DebouncingStill)

        // 4. Resuming movement cancels debounce and preserves initial tripId
        val resumeAction = stateMachine.onActivityTransition(TransportMode.IN_VEHICLE, tripId = 1004L, nowMillis = 85000L)
        assertTrue(resumeAction is StateAction.ResumeActiveTrip)
        assertEquals(1001L, (stateMachine.currentState as TrackingState.Active).tripId)
    }
}
