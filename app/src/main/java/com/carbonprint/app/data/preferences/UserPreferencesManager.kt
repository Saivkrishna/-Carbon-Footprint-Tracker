package com.carbonprint.app.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.carbonprint.app.domain.calculator.CarbonEmissionCalculator
import com.carbonprint.app.domain.model.VehicleType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Manages persistent user configuration and tracking preferences using SharedPreferences.
 */
class UserPreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "carbonprint_preferences"

        const val KEY_VEHICLE_TYPE = "pref_vehicle_type"
        const val KEY_CUSTOM_CAR_FACTOR = "pref_custom_car_factor"
        const val KEY_WEEKLY_GOAL_KG = "pref_weekly_goal_kg"
        const val KEY_USER_WEIGHT_KG = "pref_user_weight_kg"
        const val KEY_ACTIVITY_RECOGNITION_ENABLED = "pref_activity_recognition_enabled"
        const val KEY_TRACKING_ACTIVE = "pref_tracking_active"
        const val KEY_CURRENT_TRIP_ID = "pref_current_trip_id"

        const val DEFAULT_WEEKLY_GOAL_KG = 25.0
        const val DEFAULT_WEIGHT_KG = 70.0
    }

    var selectedVehicleType: VehicleType
        get() {
            val name = prefs.getString(KEY_VEHICLE_TYPE, VehicleType.PETROL_CAR.name)
            return VehicleType.fromString(name)
        }
        set(value) {
            prefs.edit().putString(KEY_VEHICLE_TYPE, value.name).apply()
        }

    var customCarEmissionFactor: Double
        get() = prefs.getFloat(KEY_CUSTOM_CAR_FACTOR, CarbonEmissionCalculator.BASELINE_CAR_EMISSION_FACTOR.toFloat()).toDouble()
        set(value) {
            prefs.edit().putFloat(KEY_CUSTOM_CAR_FACTOR, value.toFloat()).apply()
        }

    var weeklyGoalTargetKg: Double
        get() = prefs.getFloat(KEY_WEEKLY_GOAL_KG, DEFAULT_WEEKLY_GOAL_KG.toFloat()).toDouble()
        set(value) {
            prefs.edit().putFloat(KEY_WEEKLY_GOAL_KG, value.toFloat()).apply()
        }

    var userWeightKg: Double
        get() = prefs.getFloat(KEY_USER_WEIGHT_KG, DEFAULT_WEIGHT_KG.toFloat()).toDouble()
        set(value) {
            prefs.edit().putFloat(KEY_USER_WEIGHT_KG, value.toFloat()).apply()
        }

    var isActivityRecognitionEnabled: Boolean
        get() = prefs.getBoolean(KEY_ACTIVITY_RECOGNITION_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_ACTIVITY_RECOGNITION_ENABLED, value).apply()
        }

    var isTrackingActive: Boolean
        get() = prefs.getBoolean(KEY_TRACKING_ACTIVE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_TRACKING_ACTIVE, value).apply()
        }

    var currentActiveTripId: Long
        get() = prefs.getLong(KEY_CURRENT_TRIP_ID, -1L)
        set(value) {
            prefs.edit().putLong(KEY_CURRENT_TRIP_ID, value).apply()
        }

    /**
     * Emits the selected vehicle type reactively when changed.
     */
    val vehicleTypeFlow: Flow<VehicleType> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_VEHICLE_TYPE) {
                trySend(selectedVehicleType)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(selectedVehicleType)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    /**
     * Emits tracking active state reactively.
     */
    val isTrackingActiveFlow: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_TRACKING_ACTIVE) {
                trySend(isTrackingActive)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(isTrackingActive)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}
