package com.github.abusalam.gps_coordinates

import com.github.abusalam.gps_coordinates.data.Coordinate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class DistanceCalculationTest {

    @Test
    fun testCalculatedDistanceCardinalDirection() {
        // Test cardinal directions
        val north = CalculatedDistance(1000f, 0f, 0f)
        assertEquals("N", north.cardinalDirection)

        val northEast = CalculatedDistance(1000f, 45f, 45f)
        assertEquals("NE", northEast.cardinalDirection)

        val east = CalculatedDistance(1000f, 90f, 90f)
        assertEquals("E", east.cardinalDirection)

        val south = CalculatedDistance(1000f, 180f, 180f)
        assertEquals("S", south.cardinalDirection)

        val west = CalculatedDistance(1000f, 270f, 270f)
        assertEquals("W", west.cardinalDirection)

        val northWest = CalculatedDistance(1000f, 315f, 315f)
        assertEquals("NW", northWest.cardinalDirection)
    }

    @Test
    fun testTravelTimeEstimation() {
        val distKm = 10.0
        val walkingHours = distKm / 5.0 // 2 hours
        val walkingMinutes = (walkingHours * 60).roundToInt()
        assertEquals(120, walkingMinutes)

        val drivingHours = distKm / 60.0 // 10 minutes
        val drivingMinutes = (drivingHours * 60).roundToInt()
        assertEquals(10, drivingMinutes)
    }

    @Test
    fun testNearbyWaypointDistanceCheck() {
        val threshold = 25.0f
        val distanceA = 8.5f
        val distanceB = 30.0f
        val isNearA = distanceA <= threshold
        val isNearB = distanceB <= threshold

        assertEquals(true, isNearA)
        assertEquals(false, isNearB)
    }

    @Test
    fun testOverwriteCoordinatePreservesId() {
        val original = Coordinate(
            id = 42,
            label = "Old Waypoint",
            latitude = 37.7749,
            longitude = -122.4194,
            isPinned = true,
            altitude = 10.0,
            tag = "hiking"
        )
        val updated = original.copy(
            label = "Updated Waypoint",
            latitude = 37.7750,
            longitude = -122.4195,
            altitude = 12.0,
            tag = "camp",
            timestamp = System.currentTimeMillis()
        )
        assertEquals(42, updated.id)
        assertEquals("Updated Waypoint", updated.label)
        assertEquals(37.7750, updated.latitude, 0.0001)
        assertEquals(true, updated.isPinned) // retains pinned state
    }

    @Test
    fun testCompassAngleUnwrapDelta() {
        // When transitioning from 355° to 5°, angle difference should be +10°, not -350°
        val current = 355f
        val target = 5f
        val delta = ((target - current + 540f) % 360f) - 180f
        assertEquals(10f, delta, 0.001f)

        // When transitioning from 5° to 355°, angle difference should be -10°, not +350°
        val deltaReverse = ((current - target + 540f) % 360f) - 180f
        assertEquals(-10f, deltaReverse, 0.001f)
    }

    @Test
    fun testSatelliteRadialProjection() {
        val maxRadius = 100f

        // Satellite directly overhead (90° elevation) should be at center (r = 0)
        val elevationZenith = 90f
        val rZenith = maxRadius * ((90f - elevationZenith.coerceIn(0f, 90f)) / 90f)
        assertEquals(0f, rZenith, 0.001f)

        // Satellite at horizon (0° elevation) should be at edge (r = maxRadius)
        val elevationHorizon = 0f
        val rHorizon = maxRadius * ((90f - elevationHorizon.coerceIn(0f, 90f)) / 90f)
        assertEquals(maxRadius, rHorizon, 0.001f)

        // Satellite at 45° elevation should be halfway (r = 50)
        val elevationMid = 45f
        val rMid = maxRadius * ((90f - elevationMid.coerceIn(0f, 90f)) / 90f)
        assertEquals(50f, rMid, 0.001f)
    }

    @Test
    fun testGnssSatelliteInfoAttributes() {
        val sat = GnssSatelliteInfo(
            id = 14,
            constellation = "GPS",
            azimuth = 210f,
            elevation = 65f,
            snr = 38.5f,
            usedInFix = true
        )
        assertEquals(14, sat.id)
        assertEquals("GPS", sat.constellation)
        assertEquals(210f, sat.azimuth, 0.001f)
        assertEquals(65f, sat.elevation, 0.001f)
        assertTrue(sat.usedInFix)
    }
}
