package com.carbonprint.app.domain.calculator

import com.carbonprint.app.domain.model.TransportMode

/**
 * Estimates energy expenditure (calories burned) during physical activity trips.
 *
 * Scientific Basis:
 * - Metabolic Equivalent of Task (MET) values from the Compendium of Physical Activities:
 *   - Walking at moderate pace (4.8 km/h): MET ~3.8 (~0.79 kcal/kg/km -> ~55 kcal/km for 70kg adult)
 *   - Running at moderate pace (9.6 km/h): MET ~9.8 (~1.07 kcal/kg/km -> ~75 kcal/km for 70kg adult)
 *   - Cycling at moderate pace (16 km/h): MET ~6.0 (~0.49 kcal/kg/km -> ~35 kcal/km for 70kg adult)
 *   - Motorized transit / stationary: negligible exercise expenditure (0 kcal).
 */
object CalorieCalculator {

    const val DEFAULT_USER_WEIGHT_KG = 70.0

    /**
     * Calculates estimated calories burned (kcal) for a trip.
     *
     * @param mode Activity mode.
     * @param distanceMeters Distance covered in meters.
     * @param userWeightKg Weight of the user in kilograms (defaults to 70.0 kg).
     * @return Calories burned in kcal (rounded to 1 decimal place).
     */
    fun calculateCaloriesBurned(
        mode: TransportMode,
        distanceMeters: Double,
        userWeightKg: Double = DEFAULT_USER_WEIGHT_KG
    ): Double {
        if (distanceMeters <= 0.0 || !distanceMeters.isFinite()) return 0.0
        val safeWeight = if (userWeightKg in 30.0..250.0) userWeightKg else DEFAULT_USER_WEIGHT_KG
        val distanceKm = distanceMeters / 1000.0

        val weightMultiplier = safeWeight / DEFAULT_USER_WEIGHT_KG
        val baseKcalPerKm = when (mode) {
            TransportMode.WALKING -> 55.0
            TransportMode.RUNNING -> 75.0
            TransportMode.ON_BICYCLE -> 35.0
            else -> 0.0
        }

        val totalKcal = distanceKm * baseKcalPerKm * weightMultiplier
        return if (totalKcal.isFinite()) Math.round(totalKcal * 10.0) / 10.0 else 0.0
    }
}
