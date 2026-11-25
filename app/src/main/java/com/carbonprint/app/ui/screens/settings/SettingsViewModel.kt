package com.carbonprint.app.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carbonprint.app.data.preferences.UserPreferencesManager
import com.carbonprint.app.domain.model.VehicleType
import com.carbonprint.app.service.ActivityRecognitionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesManager: UserPreferencesManager
) : ViewModel() {

    private val _vehicleType = MutableStateFlow(preferencesManager.selectedVehicleType)
    val vehicleType: StateFlow<VehicleType> = _vehicleType.asStateFlow()

    private val _customFactor = MutableStateFlow(preferencesManager.customCarEmissionFactor)
    val customFactor: StateFlow<Double> = _customFactor.asStateFlow()

    private val _weeklyGoalKg = MutableStateFlow(preferencesManager.weeklyGoalTargetKg)
    val weeklyGoalKg: StateFlow<Double> = _weeklyGoalKg.asStateFlow()

    private val _userWeightKg = MutableStateFlow(preferencesManager.userWeightKg)
    val userWeightKg: StateFlow<Double> = _userWeightKg.asStateFlow()

    private val _isActivityRecognitionEnabled = MutableStateFlow(preferencesManager.isActivityRecognitionEnabled)
    val isActivityRecognitionEnabled: StateFlow<Boolean> = _isActivityRecognitionEnabled.asStateFlow()

    fun setVehicleType(type: VehicleType) {
        preferencesManager.selectedVehicleType = type
        preferencesManager.customCarEmissionFactor = type.emissionFactorGramsPerKm
        _vehicleType.value = type
        _customFactor.value = type.emissionFactorGramsPerKm
    }

    fun setCustomFactor(factor: Double) {
        preferencesManager.customCarEmissionFactor = factor
        _customFactor.value = factor
    }

    fun setWeeklyGoal(goalKg: Double) {
        preferencesManager.weeklyGoalTargetKg = goalKg
        _weeklyGoalKg.value = goalKg
    }

    fun setUserWeight(weight: Double) {
        preferencesManager.userWeightKg = weight
        _userWeightKg.value = weight
    }

    fun setActivityRecognitionEnabled(context: Context? = null, enabled: Boolean) {
        preferencesManager.isActivityRecognitionEnabled = enabled
        _isActivityRecognitionEnabled.value = enabled

        if (context != null) {
            if (enabled) {
                if (ActivityRecognitionManager.hasActivityRecognitionPermission(context)) {
                    ActivityRecognitionManager.registerActivityTransitions(context)
                }
            } else {
                ActivityRecognitionManager.unregisterActivityTransitions(context)
            }
        }
    }
}
