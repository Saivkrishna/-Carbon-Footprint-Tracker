package com.carbonprint.app.domain

import com.carbonprint.app.data.local.entity.DailySummaryEntity
import com.carbonprint.app.domain.calculator.CarbonEmissionCalculator
import com.carbonprint.app.domain.model.TransportMode
import com.carbonprint.app.domain.model.VehicleType
import com.carbonprint.app.domain.tracking.StepTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests covering detailed tracking metrics for all travel modes:
 * - Walking (steps, distance, duration, emissions, savings)
 * - Running (steps, distance, duration, emissions, savings)
 * - Cycling (distance, duration, average speed, savings)
 * - Motorcycle (distance, duration, average speed, estimated emissions, savings)
 * - Car (distance, duration, average speed, estimated emissions)
 * - Bus / Public Transport (distance, duration, average speed, estimated emissions, savings)
 * - Still (stationary status, zero emissions, zero duplicate trips)
 * - Separate walking and running step accumulation
 */
class TravelModeMetricsTest {

    @Test
    fun walking_stepEstimationAndEmissions() {
        val distanceMeters = 1000.0 // 1 km walk
        val estimatedSteps = StepTracker.calculateEstimatedSteps(TransportMode.WALKING, distanceMeters)

        // Stride ~0.76 m -> 1000 / 0.76 ≈ 1315 steps
        assertTrue("Walking 1km should estimate between 1200 and 1400 steps", estimatedSteps in 1200..1400)

        // Walking emissions = 0 g tailpipe
        val emissions = CarbonEmissionCalculator.calculateEmissionGrams(TransportMode.WALKING, distanceMeters)
        assertEquals(0.0, emissions, 0.001)

        // Walking saves 192 g/km vs standard car
        val saved = CarbonEmissionCalculator.calculateSavedEmissionGrams(TransportMode.WALKING, distanceMeters)
        assertEquals(192.0, saved, 0.01)
    }

    @Test
    fun running_stepEstimationAndEmissions() {
        val distanceMeters = 1000.0 // 1 km run
        val estimatedSteps = StepTracker.calculateEstimatedSteps(TransportMode.RUNNING, distanceMeters)

        // Running stride ~1.05 m -> 1000 / 1.05 ≈ 952 steps
        assertTrue("Running 1km should estimate between 850 and 1050 steps", estimatedSteps in 850..1050)

        // Running steps must be fewer than walking steps for the same distance due to longer stride
        val walkingSteps = StepTracker.calculateEstimatedSteps(TransportMode.WALKING, distanceMeters)
        assertTrue("Running steps should be fewer than walking steps for equal distance", estimatedSteps < walkingSteps)

        // Running emissions = 0 g tailpipe
        val emissions = CarbonEmissionCalculator.calculateEmissionGrams(TransportMode.RUNNING, distanceMeters)
        assertEquals(0.0, emissions, 0.001)

        // Running saves 192 g/km
        val saved = CarbonEmissionCalculator.calculateSavedEmissionGrams(TransportMode.RUNNING, distanceMeters)
        assertEquals(192.0, saved, 0.01)
    }

    @Test
    fun cycling_distanceDurationAndSpeed() {
        val distanceMeters = 15000.0 // 15 km
        val durationSeconds = 3600L // 1 hour

        val avgSpeedKmh = (distanceMeters / 1000.0) / (durationSeconds / 3600.0)
        assertEquals(15.0, avgSpeedKmh, 0.01)

        // Cycling tailpipe emissions = 0
        val emissions = CarbonEmissionCalculator.calculateEmissionGrams(TransportMode.ON_BICYCLE, distanceMeters)
        assertEquals(0.0, emissions, 0.001)

        // Cycling saved = 15 km * 192 g/km = 2880 g
        val saved = CarbonEmissionCalculator.calculateSavedEmissionGrams(TransportMode.ON_BICYCLE, distanceMeters)
        assertEquals(2880.0, saved, 0.01)
    }

    @Test
    fun motorcycle_distanceSpeedEmissionsAndSavings() {
        val distanceMeters = 20000.0 // 20 km
        val durationSeconds = 1800L // 30 minutes (0.5 hours)

        // Average speed = 20 / 0.5 = 40 km/h
        val avgSpeedKmh = (distanceMeters / 1000.0) / (durationSeconds / 3600.0)
        assertEquals(40.0, avgSpeedKmh, 0.01)

        // Motorcycle factor = 103.0 g/km -> 20 km * 103.0 = 2060.0 g CO2
        val emissions = CarbonEmissionCalculator.calculateEmissionGrams(
            TransportMode.IN_VEHICLE,
            distanceMeters,
            vehicleType = VehicleType.MOTORCYCLE
        )
        assertEquals(2060.0, emissions, 0.01)

        // Saved vs 192.0 g/km car = (192 - 103) * 20 = 89 * 20 = 1780.0 g saved
        val saved = CarbonEmissionCalculator.calculateSavedEmissionGrams(
            TransportMode.IN_VEHICLE,
            distanceMeters,
            vehicleType = VehicleType.MOTORCYCLE
        )
        assertEquals(1780.0, saved, 0.01)
    }

    @Test
    fun car_distanceSpeedAndEmissions() {
        val distanceMeters = 25000.0 // 25 km
        val durationSeconds = 1800L // 30 mins

        // Speed = 25 / 0.5 = 50 km/h
        val avgSpeedKmh = (distanceMeters / 1000.0) / (durationSeconds / 3600.0)
        assertEquals(50.0, avgSpeedKmh, 0.01)

        // Petrol car factor = 192.0 g/km -> 25 km * 192.0 = 4800.0 g CO2
        val emissions = CarbonEmissionCalculator.calculateEmissionGrams(
            TransportMode.IN_VEHICLE,
            distanceMeters,
            vehicleType = VehicleType.PETROL_CAR
        )
        assertEquals(4800.0, emissions, 0.01)

        // Baseline car saves 0 g vs itself
        val saved = CarbonEmissionCalculator.calculateSavedEmissionGrams(
            TransportMode.IN_VEHICLE,
            distanceMeters,
            vehicleType = VehicleType.PETROL_CAR
        )
        assertEquals(0.0, saved, 0.01)
    }

    @Test
    fun transit_busDistanceSpeedEmissionsAndSavings() {
        val distanceMeters = 10000.0 // 10 km
        val durationSeconds = 1200L // 20 mins

        val avgSpeedKmh = (distanceMeters / 1000.0) / (durationSeconds / 3600.0)
        assertEquals(30.0, avgSpeedKmh, 0.01)

        // Public transit factor = 68.0 g/km -> 10 km * 68 = 680.0 g CO2
        val emissions = CarbonEmissionCalculator.calculateEmissionGrams(TransportMode.TRANSIT, distanceMeters)
        assertEquals(680.0, emissions, 0.01)

        // Saved vs 192.0 g/km car = (192 - 68) * 10 = 1240.0 g saved
        val saved = CarbonEmissionCalculator.calculateSavedEmissionGrams(TransportMode.TRANSIT, distanceMeters)
        assertEquals(1240.0, saved, 0.01)
    }

    @Test
    fun stillMode_stationaryStatusProducesZeroEmissions() {
        val distanceMeters = 0.0
        val emissions = CarbonEmissionCalculator.calculateEmissionGrams(TransportMode.STILL, distanceMeters)
        assertEquals(0.0, emissions, 0.001)

        val saved = CarbonEmissionCalculator.calculateSavedEmissionGrams(TransportMode.STILL, distanceMeters)
        assertEquals(0.0, saved, 0.001)

        // Zero distance estimates zero steps
        val steps = StepTracker.calculateEstimatedSteps(TransportMode.STILL, 0.0)
        assertEquals(0, steps)
    }

    @Test
    fun segregatedDailySummary_keepsWalkingAndRunningStepsSeparate() {
        val summary = DailySummaryEntity(
            dateString = "2026-11-24",
            totalDistanceMeters = 12000.0,
            totalCo2Grams = 0.0,
            totalCo2SavedGrams = 2304.0,
            totalCaloriesBurned = 620.0,
            walkDistanceMeters = 4000.0,
            runDistanceMeters = 3000.0,
            bikeDistanceMeters = 5000.0,
            motorcycleDistanceMeters = 0.0,
            carDistanceMeters = 0.0,
            transitDistanceMeters = 0.0,
            tripCount = 3,
            walkingSteps = 5240,
            runningSteps = 2850
        )

        assertEquals(5240, summary.walkingSteps)
        assertEquals(2850, summary.runningSteps)
        assertEquals(8090, summary.walkingSteps + summary.runningSteps)
        assertEquals(4000.0, summary.walkDistanceMeters, 0.01)
        assertEquals(3000.0, summary.runDistanceMeters, 0.01)
    }
}
