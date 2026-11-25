package com.carbonprint.app.domain

import com.carbonprint.app.domain.model.TransportMode
import com.carbonprint.app.domain.tracking.StateAction
import com.carbonprint.app.domain.tracking.TrackingState
import com.carbonprint.app.domain.tracking.TrackingStateMachine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TrackingStateMachineTest {

    private lateinit var stateMachine: TrackingStateMachine

    @Before
    fun setUp() {
        stateMachine = TrackingStateMachine(stillTimeoutSeconds = 120L)
    }

    @Test
    fun initialState_isIdle() {
        assertTrue(stateMachine.currentState is TrackingState.Idle)
    }

    @Test
    fun onUserStart_transitionsIdleToActive() {
        val action = stateMachine.onUserStart(TransportMode.WALKING, tripId = 101L, nowMillis = 1000L)
        assertTrue(action is StateAction.StartNewTrip)
        val startAction = action as StateAction.StartNewTrip
        assertEquals(TransportMode.WALKING, startAction.mode)
        assertFalse(startAction.isAuto)

        assertTrue(stateMachine.currentState is TrackingState.Active)
        val active = stateMachine.currentState as TrackingState.Active
        assertEquals(TransportMode.WALKING, active.mode)
        assertFalse(active.isAuto)
        assertEquals(101L, active.tripId)
    }

    @Test
    fun onUserStart_whenAlreadyActiveInSameMode_preventsDuplicateTrip() {
        stateMachine.onUserStart(TransportMode.WALKING, tripId = 101L, nowMillis = 1000L)
        // Redundant click on start button
        val secondAction = stateMachine.onUserStart(TransportMode.WALKING, tripId = 102L, nowMillis = 2000L)
        assertEquals(StateAction.None, secondAction)

        val active = stateMachine.currentState as TrackingState.Active
        assertEquals(101L, active.tripId) // Initial trip preserved without duplication
    }

    @Test
    fun onUserStop_finalizesActiveTrip() {
        stateMachine.onUserStart(TransportMode.WALKING, tripId = 101L, nowMillis = 1000L)
        val stopAction = stateMachine.onUserStop()

        assertTrue(stopAction is StateAction.FinalizeTrip)
        assertTrue(stateMachine.currentState is TrackingState.Idle)
    }

    @Test
    fun onUserStop_whenIdle_doesNothing() {
        val stopAction = stateMachine.onUserStop()
        assertEquals(StateAction.None, stopAction)
        assertTrue(stateMachine.currentState is TrackingState.Idle)
    }

    @Test
    fun autoTracking_beginsStillDebounceOnRedLight() {
        // Auto-detected walking trip starts
        stateMachine.onActivityTransition(TransportMode.WALKING, tripId = 201L, nowMillis = 1000L)
        assertTrue(stateMachine.currentState is TrackingState.Active)

        // User stops at a traffic signal / pedestrian crossing (STILL detected)
        val action = stateMachine.onActivityTransition(TransportMode.STILL, tripId = 201L, nowMillis = 5000L)
        assertTrue(action is StateAction.BeginStillDebounce)
        assertTrue(stateMachine.currentState is TrackingState.DebouncingStill)

        val debouncing = stateMachine.currentState as TrackingState.DebouncingStill
        assertEquals(TransportMode.WALKING, debouncing.mode)
        assertEquals(201L, debouncing.tripId)
    }

    @Test
    fun autoTracking_resumesTripWhenMovementDetectedWithinDebounceWindow() {
        // Start auto trip, then pause at signal
        stateMachine.onActivityTransition(TransportMode.WALKING, tripId = 201L, nowMillis = 1000L)
        stateMachine.onActivityTransition(TransportMode.STILL, tripId = 201L, nowMillis = 5000L)
        assertTrue(stateMachine.currentState is TrackingState.DebouncingStill)

        // Traffic light turns green 30 seconds later -> WALKING resumes!
        val resumeAction = stateMachine.onActivityTransition(TransportMode.WALKING, tripId = 201L, nowMillis = 35000L)
        assertTrue(resumeAction is StateAction.ResumeActiveTrip)
        val resumed = resumeAction as StateAction.ResumeActiveTrip
        assertEquals(TransportMode.WALKING, resumed.mode)

        // Trip is back to active, NOT duplicated or finalized!
        assertTrue(stateMachine.currentState is TrackingState.Active)
        val active = stateMachine.currentState as TrackingState.Active
        assertEquals(201L, active.tripId)
    }

    @Test
    fun autoTracking_finalizesTripWhenStillTimeoutExpires() {
        // Start auto trip, then stationary
        stateMachine.onActivityTransition(TransportMode.WALKING, tripId = 201L, nowMillis = 1000L)
        stateMachine.onActivityTransition(TransportMode.STILL, tripId = 201L, nowMillis = 5000L)

        // 120-second timeout expires without resumption
        val timeoutAction = stateMachine.onStillTimeoutExpired()
        assertTrue(timeoutAction is StateAction.FinalizeTrip)
        assertTrue(stateMachine.currentState is TrackingState.Idle)
    }

    @Test
    fun manualTracking_ignoresStillTransitions() {
        // User manually pressed "START TRACKING" from screen
        stateMachine.onUserStart(TransportMode.RUNNING, tripId = 301L, nowMillis = 1000L)

        // User stops to catch breath (STILL detected)
        val stillAction = stateMachine.onActivityTransition(TransportMode.STILL, tripId = 301L, nowMillis = 10000L)
        assertEquals(StateAction.None, stillAction)

        // Must remain Active: manual workouts should not auto-terminate at red lights!
        assertTrue(stateMachine.currentState is TrackingState.Active)
        val active = stateMachine.currentState as TrackingState.Active
        assertEquals(TransportMode.RUNNING, active.mode)
        assertFalse(active.isAuto)
    }

    @Test
    fun autoTracking_switchesModeSmoothly() {
        // User walking
        stateMachine.onActivityTransition(TransportMode.WALKING, tripId = 401L, nowMillis = 1000L)
        // User boards vehicle
        val switchAction = stateMachine.onActivityTransition(TransportMode.IN_VEHICLE, tripId = 402L, nowMillis = 60000L)
        assertTrue(switchAction is StateAction.ChangeTripMode)
        val changeAction = switchAction as StateAction.ChangeTripMode
        assertEquals(TransportMode.IN_VEHICLE, changeAction.newMode)
        assertEquals(401L, changeAction.tripId) // Active trip ID preserved without creating duplicate trip!

        val active = stateMachine.currentState as TrackingState.Active
        assertEquals(TransportMode.IN_VEHICLE, active.mode)
        assertEquals(401L, active.tripId)
        assertTrue(active.isAuto)
    }

    @Test
    fun autoTracking_stillDebounce_resumesInDifferentMode_preservesTrip() {
        // Auto walking trip starts
        stateMachine.onActivityTransition(TransportMode.WALKING, tripId = 501L, nowMillis = 1000L)
        // User stops at bus stop (STILL)
        stateMachine.onActivityTransition(TransportMode.STILL, tripId = 501L, nowMillis = 5000L)
        assertTrue(stateMachine.currentState is TrackingState.DebouncingStill)

        // Bus arrives and moves (IN_VEHICLE) within debounce window
        val action = stateMachine.onActivityTransition(TransportMode.IN_VEHICLE, tripId = 502L, nowMillis = 30000L)
        assertTrue(action is StateAction.ChangeTripMode)
        val changeAction = action as StateAction.ChangeTripMode
        assertEquals(TransportMode.IN_VEHICLE, changeAction.newMode)
        assertEquals(501L, changeAction.tripId) // Initial trip preserved

        assertTrue(stateMachine.currentState is TrackingState.Active)
        val active = stateMachine.currentState as TrackingState.Active
        assertEquals(TransportMode.IN_VEHICLE, active.mode)
        assertEquals(501L, active.tripId)
    }
}
