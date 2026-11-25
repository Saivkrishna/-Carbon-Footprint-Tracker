package com.carbonprint.app.domain.model

/**
 * Enumeration of detected and tracked transportation modes with default emission factors
 * and calorie burn indices based on IPCC and DEFRA transport averages.
 */
enum class TransportMode(
    val displayName: String,
    val emissionFactorGramsPerKm: Double, // Grams of CO2 emitted per kilometer
    val isZeroEmission: Boolean,
    val caloriesBurnedPerKm: Double       // Estimated kcal burned per km
) {
    STILL("Stationary", 0.0, true, 0.0),
    WALKING("Walking", 0.0, true, 55.0),
    RUNNING("Running", 0.0, true, 75.0),
    ON_BICYCLE("Cycling", 0.0, true, 35.0),
    IN_VEHICLE("Motor Vehicle", 170.0, false, 0.0), // Baseline avg for combustion cars
    TRANSIT("Public Transit", 68.0, false, 5.0),    // Bus / Train composite
    UNKNOWN("Unknown Activity", 0.0, true, 0.0);

    companion object {
        fun fromString(value: String?): TransportMode {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
}
