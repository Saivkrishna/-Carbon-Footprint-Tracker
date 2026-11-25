package com.carbonprint.app.domain.tracking

import com.carbonprint.app.domain.model.TransportMode

/**
 * Lifecycle states of trip tracking.
 */
sealed class TrackingState {
    object Idle : TrackingState()

    data class Active(
        val mode: TransportMode,
        val isAuto: Boolean,
        val tripId: Long,
        val startedAtMillis: Long
    ) : TrackingState()

    data class DebouncingStill(
        val mode: TransportMode,
        val tripId: Long,
        val startedAtMillis: Long,
        val stationarySinceMillis: Long,
        val timeoutSeconds: Long = DEFAULT_STILL_TIMEOUT_SECONDS
    ) : TrackingState()

    companion object {
        const val DEFAULT_STILL_TIMEOUT_SECONDS = 120L // 2-minute stationary confirmation delay
    }
}

/**
 * Action outcomes emitted by [TrackingStateMachine] when processing events.
 */
sealed class StateAction {
    object None : StateAction()
    data class StartNewTrip(val mode: TransportMode, val isAuto: Boolean) : StateAction()
    data class ResumeActiveTrip(val mode: TransportMode) : StateAction()
    data class ChangeTripMode(val newMode: TransportMode, val tripId: Long) : StateAction()
    object BeginStillDebounce : StateAction()
    object FinalizeTrip : StateAction()
}

/**
 * Deterministic finite state machine governing trip lifecycles.
 * Prevents duplicate trips, handles temporary traffic light pauses, and coordinates transitions.
 */
class TrackingStateMachine(
    private val stillTimeoutSeconds: Long = TrackingState.DEFAULT_STILL_TIMEOUT_SECONDS
) {
    var currentState: TrackingState = TrackingState.Idle
        private set

    /**
     * Handles explicit user-requested tracking start from the visible UI screen.
     */
    fun onUserStart(mode: TransportMode, tripId: Long, nowMillis: Long = System.currentTimeMillis()): StateAction {
        return when (val state = currentState) {
            is TrackingState.Idle -> {
                currentState = TrackingState.Active(mode, isAuto = false, tripId, nowMillis)
                StateAction.StartNewTrip(mode, isAuto = false)
            }
            is TrackingState.Active -> {
                if (state.mode == mode) {
                    // Already tracking same mode; ignore duplicate start
                    StateAction.None
                } else {
                    // Finalize current trip and start new mode
                    currentState = TrackingState.Active(mode, isAuto = false, tripId, nowMillis)
                    StateAction.StartNewTrip(mode, isAuto = false)
                }
            }
            is TrackingState.DebouncingStill -> {
                // Cancel stationary debounce and transition to active manual tracking
                currentState = TrackingState.Active(mode, isAuto = false, tripId, nowMillis)
                StateAction.ResumeActiveTrip(mode)
            }
        }
    }

    /**
     * Handles background activity recognition transition events.
     */
    fun onActivityTransition(
        detectedMode: TransportMode,
        tripId: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): StateAction {
        return when (val state = currentState) {
            is TrackingState.Idle -> {
                if (detectedMode.isMoving) {
                    currentState = TrackingState.Active(detectedMode, isAuto = true, tripId, nowMillis)
                    StateAction.StartNewTrip(detectedMode, isAuto = true)
                } else {
                    StateAction.None
                }
            }
            is TrackingState.Active -> {
                if (detectedMode == TransportMode.STILL) {
                    if (state.isAuto) {
                        // Begin stationary confirmation debounce window (e.g. traffic light / brief pause)
                        currentState = TrackingState.DebouncingStill(
                            mode = state.mode,
                            tripId = state.tripId,
                            startedAtMillis = state.startedAtMillis,
                            stationarySinceMillis = nowMillis,
                            timeoutSeconds = stillTimeoutSeconds
                        )
                        StateAction.BeginStillDebounce
                    } else {
                        // User explicitly started tracking: keep manual trip active through stops
                        StateAction.None
                    }
                } else if (detectedMode.isMoving) {
                    if (detectedMode == state.mode) {
                        // Redundant transition; ignore duplicate
                        StateAction.None
                    } else if (state.isAuto) {
                        // Auto mode changed (e.g. walking -> boarding vehicle): update trip mode without creating duplicate trip
                        currentState = TrackingState.Active(
                            mode = detectedMode,
                            isAuto = true,
                            tripId = state.tripId,
                            startedAtMillis = state.startedAtMillis
                        )
                        StateAction.ChangeTripMode(detectedMode, state.tripId)
                    } else {
                        StateAction.None
                    }
                } else {
                    StateAction.None
                }
            }
            is TrackingState.DebouncingStill -> {
                if (detectedMode.isMoving) {
                    if (detectedMode == state.mode) {
                        // Movement resumed in same mode within debounce window! Cancel debounce, resume trip!
                        currentState = TrackingState.Active(
                            mode = state.mode,
                            isAuto = true,
                            tripId = state.tripId,
                            startedAtMillis = state.startedAtMillis
                        )
                        StateAction.ResumeActiveTrip(state.mode)
                    } else {
                        // Movement resumed in a different mode: update trip mode and preserve trip
                        currentState = TrackingState.Active(
                            mode = detectedMode,
                            isAuto = true,
                            tripId = state.tripId,
                            startedAtMillis = state.startedAtMillis
                        )
                        StateAction.ChangeTripMode(detectedMode, state.tripId)
                    }
                } else {
                    // Still stationary; continue debouncing
                    StateAction.None
                }
            }
        }
    }

    /**
     * Called when the stationary debounce timer expires without resumption of movement.
     */
    fun onStillTimeoutExpired(): StateAction {
        return when (currentState) {
            is TrackingState.DebouncingStill -> {
                currentState = TrackingState.Idle
                StateAction.FinalizeTrip
            }
            else -> StateAction.None
        }
    }

    /**
     * Handles explicit stop command from user (UI button or notification action).
     */
    fun onUserStop(): StateAction {
        return if (currentState !is TrackingState.Idle) {
            currentState = TrackingState.Idle
            StateAction.FinalizeTrip
        } else {
            StateAction.None
        }
    }

    private val TransportMode.isMoving: Boolean
        get() = this == TransportMode.WALKING ||
                this == TransportMode.RUNNING ||
                this == TransportMode.ON_BICYCLE ||
                this == TransportMode.IN_VEHICLE
}
