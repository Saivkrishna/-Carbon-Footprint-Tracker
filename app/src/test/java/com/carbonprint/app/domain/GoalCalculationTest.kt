package com.carbonprint.app.domain

import com.carbonprint.app.data.repository.GoalRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class GoalCalculationTest {

    @Test
    fun weekBounds_exactDurationIsSevenDays() {
        val now = System.currentTimeMillis()
        val (startMillis, endMillis) = GoalRepository.getCurrentWeekBounds(now)

        assertTrue("End must be after start", endMillis > startMillis)
        // 7 days = 7 * 24 * 60 * 60 * 1000 = 604,800,000 ms.
        // Bounds are Monday 00:00:00.000 to Sunday 23:59:59.999 -> delta = 604,799,999 ms
        val delta = endMillis - startMillis
        assertEquals(604799999L, delta)
    }

    @Test
    fun weekBounds_startIsMonday00_00_endIsSunday23_59() {
        val now = System.currentTimeMillis()
        val (startMillis, endMillis) = GoalRepository.getCurrentWeekBounds(now)

        val calStart = Calendar.getInstance().apply { timeInMillis = startMillis }
        assertEquals(Calendar.MONDAY, calStart.get(Calendar.DAY_OF_WEEK))
        assertEquals(0, calStart.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calStart.get(Calendar.MINUTE))
        assertEquals(0, calStart.get(Calendar.SECOND))
        assertEquals(0, calStart.get(Calendar.MILLISECOND))

        val calEnd = Calendar.getInstance().apply { timeInMillis = endMillis }
        assertEquals(Calendar.SUNDAY, calEnd.get(Calendar.DAY_OF_WEEK))
        assertEquals(23, calEnd.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, calEnd.get(Calendar.MINUTE))
        assertEquals(59, calEnd.get(Calendar.SECOND))
        assertEquals(999, calEnd.get(Calendar.MILLISECOND))
    }

    @Test
    fun weekBounds_consistentAcrossAllDaysOfSameWeek() {
        // Create an arbitrary Monday noon
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 12, 12, 0, 0) // October 12, 2026 is a Monday
        }
        val mondayNoon = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, 2) // Wednesday
        val wednesdayNoon = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, 3) // Saturday
        val saturdayNoon = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, 1) // Sunday
        val sundayNight = cal.apply {
            set(Calendar.HOUR_OF_DAY, 22)
            set(Calendar.MINUTE, 30)
        }.timeInMillis

        val boundsMonday = GoalRepository.getCurrentWeekBounds(mondayNoon)
        val boundsWednesday = GoalRepository.getCurrentWeekBounds(wednesdayNoon)
        val boundsSaturday = GoalRepository.getCurrentWeekBounds(saturdayNoon)
        val boundsSunday = GoalRepository.getCurrentWeekBounds(sundayNight)

        assertEquals("Monday and Wednesday must share exact start", boundsMonday.first, boundsWednesday.first)
        assertEquals("Monday and Saturday must share exact start", boundsMonday.first, boundsSaturday.first)
        assertEquals("Monday and Sunday must share exact start", boundsMonday.first, boundsSunday.first)

        assertEquals("Monday and Sunday must share exact end", boundsMonday.second, boundsSunday.second)
    }
}
