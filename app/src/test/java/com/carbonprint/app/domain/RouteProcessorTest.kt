package com.carbonprint.app.domain

import com.carbonprint.app.data.local.entity.WaypointEntity
import com.carbonprint.app.domain.location.RouteProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteProcessorTest {

    @Test
    fun processWaypoints_emptyList_returnsEmptyProcessedRoute() {
        val result = RouteProcessor.processWaypoints(emptyList())
        assertFalse(result.hasValidRoute)
        assertFalse(result.isMultiPointRoute)
        assertTrue(result.points.isEmpty())
        assertNull(result.startPoint)
        assertNull(result.destinationPoint)
        assertNull(result.boundingBox)
        assertEquals(0, result.totalWaypointsCount)
    }

    @Test
    fun processWaypoints_nullList_returnsEmptyProcessedRouteSafely() {
        val result = RouteProcessor.processWaypoints(null)
        assertFalse(result.hasValidRoute)
        assertEquals(0, result.totalWaypointsCount)
    }

    @Test
    fun processWaypoints_invalidCoordinates_areFilteredOut() {
        val raw = listOf(
            WaypointEntity(id = 1, tripId = 1, latitude = 37.7749, longitude = -122.4194, timestamp = 1000L),
            WaypointEntity(id = 2, tripId = 1, latitude = Double.NaN, longitude = -122.4194, timestamp = 2000L), // NaN
            WaypointEntity(id = 3, tripId = 1, latitude = 95.0, longitude = -122.4194, timestamp = 3000L), // Out of range lat
            WaypointEntity(id = 4, tripId = 1, latitude = 0.0, longitude = 0.0, timestamp = 4000L), // (0,0) Null Island
            WaypointEntity(id = 5, tripId = 1, latitude = 37.7755, longitude = -122.4180, timestamp = 5000L)
        )

        val result = RouteProcessor.processWaypoints(raw)
        assertTrue(result.hasValidRoute)
        assertEquals(2, result.points.size)
        assertEquals(5, result.totalWaypointsCount)
        assertEquals(37.7749, result.points[0].latitude, 0.0001)
        assertEquals(37.7755, result.points[1].latitude, 0.0001)
    }

    @Test
    fun processWaypoints_unsortedPoints_areSortedChronologically() {
        val raw = listOf(
            WaypointEntity(id = 3, tripId = 1, latitude = 37.7760, longitude = -122.4170, timestamp = 3000L),
            WaypointEntity(id = 1, tripId = 1, latitude = 37.7740, longitude = -122.4190, timestamp = 1000L),
            WaypointEntity(id = 2, tripId = 1, latitude = 37.7750, longitude = -122.4180, timestamp = 2000L)
        )

        val result = RouteProcessor.processWaypoints(raw)
        assertEquals(3, result.points.size)
        // Verify chronological order
        assertEquals(1000L, result.points[0].timestamp)
        assertEquals(2000L, result.points[1].timestamp)
        assertEquals(3000L, result.points[2].timestamp)

        assertNotNull(result.startPoint)
        assertNotNull(result.destinationPoint)
        assertEquals(1000L, result.startPoint!!.timestamp)
        assertEquals(3000L, result.destinationPoint!!.timestamp)
    }

    @Test
    fun processWaypoints_singlePoint_returnsValidSinglePointRoute() {
        val raw = listOf(
            WaypointEntity(id = 1, tripId = 1, latitude = 37.7749, longitude = -122.4194, timestamp = 1000L)
        )

        val result = RouteProcessor.processWaypoints(raw)
        assertTrue(result.hasValidRoute)
        assertFalse(result.isMultiPointRoute)
        assertEquals(1, result.points.size)
        assertEquals(result.startPoint, result.destinationPoint)
    }

    @Test
    fun processWaypoints_boundingBox_calculatesExactExtremes() {
        val raw = listOf(
            WaypointEntity(id = 1, tripId = 1, latitude = 37.7700, longitude = -122.4100, timestamp = 1000L),
            WaypointEntity(id = 2, tripId = 1, latitude = 37.7800, longitude = -122.4200, timestamp = 2000L),
            WaypointEntity(id = 3, tripId = 1, latitude = 37.7750, longitude = -122.4050, timestamp = 3000L)
        )

        val result = RouteProcessor.processWaypoints(raw)
        val bbox = result.boundingBox
        assertNotNull(bbox)
        assertEquals(37.7700, bbox!!.minLatitude, 0.0001)
        assertEquals(37.7800, bbox.maxLatitude, 0.0001)
        assertEquals(-122.4200, bbox.minLongitude, 0.0001)
        assertEquals(-122.4050, bbox.maxLongitude, 0.0001)

        assertEquals((37.7700 + 37.7800) / 2.0, bbox.centerLatitude, 0.0001)
        assertEquals((-122.4200 + -122.4050) / 2.0, bbox.centerLongitude, 0.0001)
    }
}
