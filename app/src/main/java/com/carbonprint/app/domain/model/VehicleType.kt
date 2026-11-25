package com.carbonprint.app.domain.model

/**
 * Passenger vehicle classifications with empirical emission factors (grams of CO2 per km).
 *
 * Sources and assumptions:
 * - UK DEFRA / BEIS Greenhouse Gas Reporting Conversion Factors (2023-2025 updates)
 * - European Environment Agency (EEA) passenger transport emission indicators
 * - Grid emission intensity factored into electric vehicle baseline (~45 g CO2/km)
 */
enum class VehicleType(
    val displayName: String,
    val emissionFactorGramsPerKm: Double
) {
    PETROL_CAR("Petrol / Gasoline Car", 192.0),
    DIESEL_CAR("Diesel Car", 171.0),
    HYBRID_CAR("Hybrid (HEV/PHEV)", 105.0),
    ELECTRIC_CAR("Electric Vehicle (EV)", 45.0),
    MOTORCYCLE("Motorcycle / Scooter", 103.0);

    companion object {
        fun fromString(value: String?): VehicleType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: PETROL_CAR
        }
    }
}
