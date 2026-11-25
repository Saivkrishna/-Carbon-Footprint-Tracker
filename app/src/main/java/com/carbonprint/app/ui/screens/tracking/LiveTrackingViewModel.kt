package com.carbonprint.app.ui.screens.tracking

import android.content.Context
import androidx.lifecycle.ViewModel
import com.carbonprint.app.domain.model.TransportMode
import com.carbonprint.app.domain.model.VehicleType
import com.carbonprint.app.service.TripTrackingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LiveTrackingViewModel : ViewModel() {

    val isTracking: StateFlow<Boolean> = TripTrackingService.isTracking
    val currentMode: StateFlow<TransportMode> = TripTrackingService.currentModeState
    val currentSubMode: StateFlow<String> = TripTrackingService.subModeState
    val distanceMeters: StateFlow<Double> = TripTrackingService.distanceMetersState
    val durationSeconds: StateFlow<Long> = TripTrackingService.durationSecondsState
    val co2Grams: StateFlow<Double> = TripTrackingService.co2GramsState
    val co2SavedGrams: StateFlow<Double> = TripTrackingService.co2SavedGramsState
    val calories: StateFlow<Double> = TripTrackingService.caloriesState
    val speedKmh: StateFlow<Double> = TripTrackingService.speedKmhState
    val stepCount: StateFlow<Int> = TripTrackingService.stepCountState
    val walkingSteps: StateFlow<Int> = TripTrackingService.walkingStepCountState
    val runningSteps: StateFlow<Int> = TripTrackingService.runningStepCountState
    val walkingDurationSeconds: StateFlow<Long> = TripTrackingService.walkingDurationSecondsState
    val runningDurationSeconds: StateFlow<Long> = TripTrackingService.runningDurationSecondsState
    val startBatteryPct: StateFlow<Int?> = TripTrackingService.startBatteryPctState
    val batteryDropPct: StateFlow<Int?> = TripTrackingService.batteryDropPctState

    private val _selectedMode = MutableStateFlow(TransportMode.WALKING)
    val selectedMode: StateFlow<TransportMode> = _selectedMode.asStateFlow()

    private val _selectedSubMode = MutableStateFlow("WALK")
    val selectedSubMode: StateFlow<String> = _selectedSubMode.asStateFlow()

    fun selectMode(mode: TransportMode, subMode: String = mode.name) {
        _selectedMode.value = mode
        _selectedSubMode.value = subMode
    }

    fun startTracking(context: Context) {
        if (_selectedMode.value == TransportMode.STILL) {
            // Still mode is stationary; do not create unnecessary trips
            return
        }
        val vehicleType = if (_selectedSubMode.value.equals("MOTORCYCLE", ignoreCase = true)) {
            VehicleType.MOTORCYCLE
        } else null

        TripTrackingService.startTracking(
            context = context,
            mode = _selectedMode.value,
            isAuto = false,
            subMode = _selectedSubMode.value,
            vehicleType = vehicleType
        )
    }

    fun stopTracking(context: Context) {
        TripTrackingService.stopTracking(context)
    }
}
