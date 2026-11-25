package com.carbonprint.app.domain

import com.carbonprint.app.domain.model.TransportMode
import com.carbonprint.app.domain.tracking.StepTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Validates step counting accuracy, activity classification, and sensor edge cases.
 * Covers all 10 required test cases using simulated hardware sensor readings.
 */
class StepCountAccuracyTest {

    private lateinit var stepTracker: StepTracker

    @Before
    fun setUp() {
        // StepTracker with null context runs in simulated sensor mode
        stepTracker = StepTracker(context = null)
    }

    /**
     * Test Case 1: Walking with 100 actual steps.
     * Simulates cumulative hardware step counter baseline at 5000, then advancing by 100 to 5100.
     */
    @Test
    fun testCase1_walking100ActualSteps() {
        stepTracker.reset(TransportMode.WALKING)

        // Initial sensor reading establishes baseline
        stepTracker.processStepCounterReading(rawCount = 5000L)
        assertEquals(0, stepTracker.getWalkingSteps())
        assertEquals(0, stepTracker.getRunningSteps())

        // User walks 100 steps
        stepTracker.processStepCounterReading(rawCount = 5100L)
        assertEquals(100, stepTracker.getWalkingSteps())
        assertEquals(0, stepTracker.getRunningSteps())
        assertEquals(100, stepTracker.getTotalSteps())
    }

    /**
     * Test Case 2: Walking with 500 actual steps.
     * Simulates walking with multiple intermediate sensor batches totaling 500 steps.
     */
    @Test
    fun testCase2_walking500ActualSteps() {
        stepTracker.reset(TransportMode.WALKING)

        stepTracker.processStepCounterReading(rawCount = 10000L) // Baseline
        stepTracker.processStepCounterReading(rawCount = 10200L) // +200 steps
        stepTracker.processStepCounterReading(rawCount = 10350L) // +150 steps
        stepTracker.processStepCounterReading(rawCount = 10500L) // +150 steps (Total 500)

        assertEquals(500, stepTracker.getWalkingSteps())
        assertEquals(0, stepTracker.getRunningSteps())
        assertEquals(500, stepTracker.getTotalSteps())
    }

    /**
     * Test Case 3: Running with 100 actual steps.
     * Verifies that when classified as RUNNING, steps accumulate exclusively into runningSteps.
     */
    @Test
    fun testCase3_running100ActualSteps() {
        stepTracker.reset(TransportMode.RUNNING)

        stepTracker.processStepCounterReading(rawCount = 20000L) // Baseline
        stepTracker.processStepCounterReading(rawCount = 20100L) // +100 running steps

        assertEquals(0, stepTracker.getWalkingSteps())
        assertEquals(100, stepTracker.getRunningSteps())
        assertEquals(100, stepTracker.getTotalSteps())
    }

    /**
     * Test Case 4: Running with 500 actual steps.
     * Verifies continuous running step count accumulation over time.
     */
    @Test
    fun testCase4_running500ActualSteps() {
        stepTracker.reset(TransportMode.RUNNING)

        stepTracker.processStepCounterReading(rawCount = 30000L) // Baseline
        stepTracker.processStepCounterReading(rawCount = 30250L) // +250 running steps
        stepTracker.processStepCounterReading(rawCount = 30500L) // +250 running steps (Total 500)

        assertEquals(0, stepTracker.getWalkingSteps())
        assertEquals(500, stepTracker.getRunningSteps())
        assertEquals(500, stepTracker.getTotalSteps())
    }

    /**
     * Test Case 5: Walking-to-running transition.
     * User walks 150 steps, activity recognition triggers transition to RUNNING, user runs 250 steps.
     * Verifies no lost steps and no duplicate counting.
     */
    @Test
    fun testCase5_walkingToRunningTransition() {
        stepTracker.reset(TransportMode.WALKING)

        stepTracker.processStepCounterReading(rawCount = 40000L) // Baseline
        stepTracker.processStepCounterReading(rawCount = 40150L) // 150 walking steps

        assertEquals(150, stepTracker.getWalkingSteps())
        assertEquals(0, stepTracker.getRunningSteps())

        // Activity recognition detects transition: WALKING -> RUNNING
        stepTracker.onActivityModeChanged(TransportMode.RUNNING)

        // 250 running steps taken
        stepTracker.processStepCounterReading(rawCount = 40400L) // +250 steps while running

        assertEquals(150, stepTracker.getWalkingSteps())
        assertEquals(250, stepTracker.getRunningSteps())
        assertEquals(400, stepTracker.getTotalSteps())
    }

    /**
     * Test Case 6: Running-to-walking transition.
     * User runs 300 steps, slows down to walking, takes 200 walking steps.
     */
    @Test
    fun testCase6_runningToWalkingTransition() {
        stepTracker.reset(TransportMode.RUNNING)

        stepTracker.processStepCounterReading(rawCount = 50000L) // Baseline
        stepTracker.processStepCounterReading(rawCount = 50300L) // 300 running steps

        assertEquals(0, stepTracker.getWalkingSteps())
        assertEquals(300, stepTracker.getRunningSteps())

        // Activity transition: RUNNING -> WALKING
        stepTracker.onActivityModeChanged(TransportMode.WALKING)

        // 200 walking steps taken
        stepTracker.processStepCounterReading(rawCount = 50500L) // +200 steps while walking

        assertEquals(200, stepTracker.getWalkingSteps())
        assertEquals(300, stepTracker.getRunningSteps())
        assertEquals(500, stepTracker.getTotalSteps())
    }

    /**
     * Test Case 7: Tracking stop and restart.
     * Trip 1 records 120 walking steps and completes.
     * Trip 2 starts with fresh baseline and records 80 walking steps.
     * Verifies Trip 2 does not include Trip 1 steps.
     */
    @Test
    fun testCase7_trackingStopAndRestart() {
        // --- Trip 1 ---
        stepTracker.reset(TransportMode.WALKING)
        stepTracker.processStepCounterReading(rawCount = 60000L) // Baseline 1
        stepTracker.processStepCounterReading(rawCount = 60120L) // +120 steps

        assertEquals(120, stepTracker.getWalkingSteps())
        assertEquals(120, stepTracker.getTotalSteps())

        // --- Stop and Restart for Trip 2 ---
        stepTracker.reset(TransportMode.WALKING) // New trip starts
        assertEquals(0, stepTracker.getWalkingSteps())
        assertEquals(0, stepTracker.getTotalSteps())

        // Trip 2 baseline established at current sensor value (60120 or higher)
        stepTracker.processStepCounterReading(rawCount = 60150L) // Baseline 2 (after 30 untracked steps)
        stepTracker.processStepCounterReading(rawCount = 60230L) // +80 steps during Trip 2

        assertEquals(80, stepTracker.getWalkingSteps())
        assertEquals(80, stepTracker.getTotalSteps())
    }

    /**
     * Test Case 8: Sensor unavailable.
     * Verifies that when no hardware sensor exists, steps remain 0 and no fabricated counts are produced.
     */
    @Test
    fun testCase8_sensorUnavailable() {
        val nullTracker = StepTracker(context = null)
        assertFalse(nullTracker.isSensorAvailable)

        nullTracker.reset(TransportMode.WALKING)
        assertEquals(0, nullTracker.getWalkingSteps())
        assertEquals(0, nullTracker.getRunningSteps())
        assertEquals(0, nullTracker.getTotalSteps())
    }

    /**
     * Test Case 9: Sensor counter reset.
     * Simulates device reboot or hardware counter overflow where rawCount drops below baseline.
     */
    @Test
    fun testCase9_sensorCounterReset() {
        stepTracker.reset(TransportMode.WALKING)

        stepTracker.processStepCounterReading(rawCount = 999900L) // Baseline
        stepTracker.processStepCounterReading(rawCount = 999950L) // +50 steps
        assertEquals(50, stepTracker.getWalkingSteps())

        // Device restarts! Step counter resets back to 10L
        stepTracker.processStepCounterReading(rawCount = 10L) // Reset detected, re-baselined
        assertEquals(50, stepTracker.getWalkingSteps()) // Preserved!

        // Subsequent steps after reboot
        stepTracker.processStepCounterReading(rawCount = 70L) // +60 steps
        assertEquals(110, stepTracker.getWalkingSteps()) // 50 + 60 = 110
    }

    /**
     * Test Case 10: Background tracking step detection.
     * Verifies that StepTracker listener callback triggers as sensor batches arrive in background.
     */
    @Test
    fun testCase10_backgroundTrackingListenerUpdates() {
        var callbackWalkingSteps = -1
        var callbackRunningSteps = -1

        stepTracker.onStepsChanged = { w, r ->
            callbackWalkingSteps = w
            callbackRunningSteps = r
        }

        stepTracker.reset(TransportMode.WALKING)
        stepTracker.processStepCounterReading(rawCount = 70000L) // Baseline
        stepTracker.processStepCounterReading(rawCount = 70042L) // +42 steps

        assertEquals(42, callbackWalkingSteps)
        assertEquals(0, callbackRunningSteps)
        assertEquals(42, stepTracker.getWalkingSteps())
    }

    /**
     * Error percentage calculation formula verification:
     * Error % = |CarbonPrint steps - Reference steps| / Reference steps * 100
     */
    @Test
    fun testStepCountErrorFormula() {
        val referenceSteps = 500.0
        val measuredSteps = 495.0
        val errorPct = Math.abs(measuredSteps - referenceSteps) / referenceSteps * 100.0
        assertEquals(1.0, errorPct, 0.001) // 1.0% error
    }
}
