package com.carbonprint.app.domain

import com.carbonprint.app.domain.calculator.CarbonEmissionCalculator
import com.carbonprint.app.domain.model.TransportMode
import com.carbonprint.app.domain.model.VehicleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CarbonEmissionCalculatorTest {

    @Test
    fun zeroEmissionModes_produceZeroEmission() {
        val distance = 5000.0 // 5 km
        assertEquals(0.0, CarbonEmissionCalculator.calculateEmissionGrams(TransportMode.WALKING, distance), 0.001)
        assertEquals(0.0, CarbonEmissionCalculator.calculateEmissionGrams(TransportMode.RUNNING, distance), 0.001)
        assertEquals(0.0, CarbonEmissionCalculator.calculateEmissionGrams(TransportMode.ON_BICYCLE, distance), 0.001)
        assertEquals(0.0, CarbonEmissionCalculator.calculateEmissionGrams(TransportMode.STILL, distance), 0.001)
    }

    @Test
    fun zeroEmissionModes_producePositiveSavedEmissions() {
        val distance = 10000.0 // 10 km
        // Baseline = 192.0 g/km -> 10 * 192 = 1920.0 g saved
        val saved = CarbonEmissionCalculator.calculateSavedEmissionGrams(TransportMode.ON_BICYCLE, distance)
        assertEquals(1920.0, saved, 0.01)
    }

    @Test
    fun inVehicle_calculatesCorrectlyForDifferentVehicleTypes() {
        val distance = 10000.0 // 10 km

        // Petrol car: 192.0 g/km * 10 km = 1920.0 g
        val petrol = CarbonEmissionCalculator.calculateEmissionGrams(
            TransportMode.IN_VEHICLE,
            distance,
            VehicleType.PETROL_CAR
        )
        assertEquals(1920.0, petrol, 0.01)

        // Diesel car: 171.0 g/km * 10 km = 1710.0 g
        val diesel = CarbonEmissionCalculator.calculateEmissionGrams(
            TransportMode.IN_VEHICLE,
            distance,
            VehicleType.DIESEL_CAR
        )
        assertEquals(1710.0, diesel, 0.01)

        // Hybrid car: 105.0 g/km * 10 km = 1050.0 g
        val hybrid = CarbonEmissionCalculator.calculateEmissionGrams(
            TransportMode.IN_VEHICLE,
            distance,
            VehicleType.HYBRID_CAR
        )
        assertEquals(1050.0, hybrid, 0.01)

        // Electric car: 45.0 g/km * 10 km = 450.0 g
        val ev = CarbonEmissionCalculator.calculateEmissionGrams(
            TransportMode.IN_VEHICLE,
            distance,
            VehicleType.ELECTRIC_CAR
        )
        assertEquals(450.0, ev, 0.01)

        // Motorcycle: 103.0 g/km * 10 km = 1030.0 g
        val motorcycle = CarbonEmissionCalculator.calculateEmissionGrams(
            TransportMode.IN_VEHICLE,
            distance,
            VehicleType.MOTORCYCLE
        )
        assertEquals(1030.0, motorcycle, 0.01)
    }

    @Test
    fun electricVehicle_producesPositiveSavedEmissionsAgainstBaseline() {
        val distance = 10000.0 // 10 km
        // Baseline = 1920.0 g, EV = 450.0 g -> Saved = 1470.0 g
        val saved = CarbonEmissionCalculator.calculateSavedEmissionGrams(
            TransportMode.IN_VEHICLE,
            distance,
            vehicleType = VehicleType.ELECTRIC_CAR
        )
        assertEquals(1470.0, saved, 0.01)
    }

    @Test
    fun customFactor_overridesDefaultCalculation() {
        val distance = 10000.0 // 10 km
        val customFactor = 85.5 // g/km

        val emission = CarbonEmissionCalculator.calculateEmissionGrams(
            TransportMode.IN_VEHICLE,
            distance,
            vehicleType = VehicleType.PETROL_CAR,
            customFactor = customFactor
        )
        // 10 km * 85.5 g/km = 855.0 g
        assertEquals(855.0, emission, 0.01)
    }

    @Test
    fun transitMode_usesCompositeTransitFactor() {
        val distance = 15000.0 // 15 km
        // Transit factor = 68.0 g/km -> 15 * 68 = 1020.0 g
        val emission = CarbonEmissionCalculator.calculateEmissionGrams(TransportMode.TRANSIT, distance)
        assertEquals(1020.0, emission, 0.01)
    }

    @Test
    fun edgeCases_returnZeroSafely() {
        assertEquals(0.0, CarbonEmissionCalculator.calculateEmissionGrams(TransportMode.IN_VEHICLE, 0.0), 0.001)
        assertEquals(0.0, CarbonEmissionCalculator.calculateEmissionGrams(TransportMode.IN_VEHICLE, -500.0), 0.001)
        assertEquals(0.0, CarbonEmissionCalculator.calculateEmissionGrams(TransportMode.IN_VEHICLE, Double.NaN), 0.001)
        assertEquals(0.0, CarbonEmissionCalculator.calculateEmissionGrams(TransportMode.IN_VEHICLE, Double.POSITIVE_INFINITY), 0.001)

        assertEquals(0.0, CarbonEmissionCalculator.calculateSavedEmissionGrams(TransportMode.WALKING, 0.0), 0.001)
        assertEquals(0.0, CarbonEmissionCalculator.calculateSavedEmissionGrams(TransportMode.WALKING, -100.0), 0.001)
    }

    @Test
    fun unitConversions_areAccurate() {
        assertEquals(1.5, CarbonEmissionCalculator.gramsToKg(1500.0), 0.001)
        assertEquals(0.0, CarbonEmissionCalculator.gramsToKg(-10.0), 0.001)
        assertEquals(2500.0, CarbonEmissionCalculator.kgToGrams(2.5), 0.001)
        assertEquals(4.25, CarbonEmissionCalculator.metersToKm(4250.0), 0.001)
    }
}
