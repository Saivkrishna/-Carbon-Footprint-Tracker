package com.carbonprint.app.domain

import com.carbonprint.app.domain.calculator.CalorieCalculator
import com.carbonprint.app.domain.model.TransportMode
import org.junit.Assert.assertEquals
import org.junit.Test

class CalorieCalculatorTest {

    @Test
    fun walkingCalories_forDefaultWeight() {
        // 70kg adult walking 5 km -> 5 km * 55.0 kcal/km = 275.0 kcal
        val kcal = CalorieCalculator.calculateCaloriesBurned(TransportMode.WALKING, 5000.0)
        assertEquals(275.0, kcal, 0.1)
    }

    @Test
    fun runningCalories_forDefaultWeight() {
        // 70kg adult running 5 km -> 5 km * 75.0 kcal/km = 375.0 kcal
        val kcal = CalorieCalculator.calculateCaloriesBurned(TransportMode.RUNNING, 5000.0)
        assertEquals(375.0, kcal, 0.1)
    }

    @Test
    fun cyclingCalories_forDefaultWeight() {
        // 70kg adult cycling 10 km -> 10 km * 35.0 kcal/km = 350.0 kcal
        val kcal = CalorieCalculator.calculateCaloriesBurned(TransportMode.ON_BICYCLE, 10000.0)
        assertEquals(350.0, kcal, 0.1)
    }

    @Test
    fun motorizedAndStillModes_burnZeroCalories() {
        assertEquals(0.0, CalorieCalculator.calculateCaloriesBurned(TransportMode.IN_VEHICLE, 10000.0), 0.01)
        assertEquals(0.0, CalorieCalculator.calculateCaloriesBurned(TransportMode.TRANSIT, 10000.0), 0.01)
        assertEquals(0.0, CalorieCalculator.calculateCaloriesBurned(TransportMode.STILL, 1000.0), 0.01)
    }

    @Test
    fun customWeight_scalesCaloriesProportionally() {
        // 84kg adult (1.2x of 70kg) walking 5 km -> 275.0 * 1.2 = 330.0 kcal
        val kcal = CalorieCalculator.calculateCaloriesBurned(
            TransportMode.WALKING,
            5000.0,
            userWeightKg = 84.0
        )
        assertEquals(330.0, kcal, 0.1)
    }

    @Test
    fun outOfRangeWeights_fallbackToDefaultWeight() {
        // Weight 10kg (<30kg) should fallback to 70kg
        val lowWeightKcal = CalorieCalculator.calculateCaloriesBurned(
            TransportMode.WALKING,
            5000.0,
            userWeightKg = 10.0
        )
        assertEquals(275.0, lowWeightKcal, 0.1)

        // Weight 350kg (>250kg) should fallback to 70kg
        val highWeightKcal = CalorieCalculator.calculateCaloriesBurned(
            TransportMode.WALKING,
            5000.0,
            userWeightKg = 350.0
        )
        assertEquals(275.0, highWeightKcal, 0.1)
    }

    @Test
    fun edgeCases_returnZeroSafely() {
        assertEquals(0.0, CalorieCalculator.calculateCaloriesBurned(TransportMode.WALKING, 0.0), 0.01)
        assertEquals(0.0, CalorieCalculator.calculateCaloriesBurned(TransportMode.WALKING, -500.0), 0.01)
        assertEquals(0.0, CalorieCalculator.calculateCaloriesBurned(TransportMode.WALKING, Double.NaN), 0.01)
    }
}
