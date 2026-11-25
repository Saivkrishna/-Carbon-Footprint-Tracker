package com.carbonprint.app.domain.calculator

import com.carbonprint.app.domain.model.TransportMode
import com.carbonprint.app.domain.model.VehicleType

/**
 * Calculator for carbon dioxide (CO2) emissions and savings.
 *
 * Assumptions & Emission Factor Baselines:
 * - Baseline ICE Passenger Car: 192.0 g CO2/km (UK DEFRA / IPCC global average for passenger vehicles).
 *   Used as the benchmark against which green travel savings are computed.
 * - Walking, Running, Cycling: 0.0 g CO2/km (Zero direct tailpipe emissions).
 * - Public Transit (Bus/Metro/Train composite): ~68.0 g CO2/passenger-km.
 * - Motor Vehicle: Dynamically calculated based on user's selected [VehicleType] or custom factor.
 */
object CarbonEmissionCalculator {

    const val BASELINE_CAR_EMISSION_FACTOR = 192.0 // g CO2/km

    /**
     * Calculates the total CO2 emitted in grams for a given trip.
     *
     * @param mode Transport mode detected or selected.
     * @param distanceMeters Traveled distance in meters.
     * @param vehicleType Optional vehicle category for motorized vehicle trips.
     * @param customFactor Optional user-defined emission factor in g/km.
     * @return CO2 emitted in grams (rounded to 2 decimal places, non-negative).
     */
    fun calculateEmissionGrams(
        mode: TransportMode,
        distanceMeters: Double,
        vehicleType: VehicleType? = null,
        customFactor: Double? = null
    ): Double {
        if (distanceMeters <= 0.0 || !distanceMeters.isFinite()) return 0.0
        val distanceKm = distanceMeters / 1000.0

        val factorGramsPerKm: Double = when {
            customFactor != null && customFactor >= 0.0 -> customFactor
            mode == TransportMode.IN_VEHICLE -> vehicleType?.emissionFactorGramsPerKm ?: VehicleType.PETROL_CAR.emissionFactorGramsPerKm
            mode.isZeroEmission -> 0.0
            else -> mode.emissionFactorGramsPerKm
        }

        val emitted = distanceKm * factorGramsPerKm
        return if (emitted.isFinite()) Math.round(emitted * 100.0) / 100.0 else 0.0
    }

    /**
     * Calculates CO2 emissions saved compared to driving a standard internal combustion engine car.
     *
     * @param mode Transport mode used.
     * @param distanceMeters Traveled distance in meters.
     * @param baselineFactor Comparison baseline factor in g CO2/km (defaults to standard car ~192g/km).
     * @param vehicleType User's vehicle type if traveling in vehicle (e.g. EV still saves compared to ICE).
     * @return CO2 saved in grams.
     */
    fun calculateSavedEmissionGrams(
        mode: TransportMode,
        distanceMeters: Double,
        baselineFactor: Double = BASELINE_CAR_EMISSION_FACTOR,
        vehicleType: VehicleType? = null
    ): Double {
        if (distanceMeters <= 0.0 || !distanceMeters.isFinite()) return 0.0
        val distanceKm = distanceMeters / 1000.0
        val baselineEmission = distanceKm * baselineFactor

        val actualEmission = calculateEmissionGrams(mode, distanceMeters, vehicleType)
        val saved = baselineEmission - actualEmission
        return if (saved > 0.0 && saved.isFinite()) Math.round(saved * 100.0) / 100.0 else 0.0
    }

    /**
     * Converts grams of CO2 to kilograms.
     */
    fun gramsToKg(grams: Double): Double {
        if (grams <= 0.0 || !grams.isFinite()) return 0.0
        return Math.round((grams / 1000.0) * 100.0) / 100.0
    }

    /**
     * Converts kilograms of CO2 to grams.
     */
    fun kgToGrams(kg: Double): Double {
        if (kg <= 0.0 || !kg.isFinite()) return 0.0
        return kg * 1000.0
    }

    /**
     * Converts meters to kilometers.
     */
    fun metersToKm(meters: Double): Double {
        if (meters <= 0.0 || !meters.isFinite()) return 0.0
        return Math.round((meters / 1000.0) * 100.0) / 100.0
    }
}
