package com.example

import com.example.calculator.QiblaHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QiblaHelperTest {

    @Test
    fun testQiblaAngleForKnownLocations() {
        // Cairo: Lat 30.0444, Lon 31.2357 -> Qibla should be around 135-137 degrees (South-East)
        val cairoQibla = QiblaHelper.calculateQiblaDirection(30.0444, 31.2357)
        assertTrue("Cairo Qibla should be in range 130..140", cairoQibla in 130.0..140.0)

        // London: Lat 51.5074, Lon -0.1278 -> Qibla should be around 118-120 degrees (East-South-East)
        val londonQibla = QiblaHelper.calculateQiblaDirection(51.5074, -0.1278)
        assertTrue("London Qibla should be in range 115..125", londonQibla in 115.0..125.0)

        // Jakarta: Lat -6.2088, Lon 106.8456 -> Qibla should be around 294-296 degrees (West-North-West)
        val jakartaQibla = QiblaHelper.calculateQiblaDirection(-6.2088, 106.8456)
        assertTrue("Jakarta Qibla should be in range 290..300", jakartaQibla in 290.0..300.0)
    }

    @Test
    fun testDistanceToKaaba() {
        // Distance from Mecca itself (21.4225, 39.8262) should be close to 0 km
        val distanceMecca = QiblaHelper.calculateDistanceToKaaba(21.4225, 39.8262)
        assertTrue("Distance at Mecca should be < 5 km", distanceMecca < 5.0)

        // Distance from Cairo (approx 1280 km)
        val distanceCairo = QiblaHelper.calculateDistanceToKaaba(30.0444, 31.2357)
        assertTrue("Distance from Cairo should be ~1200-1400 km", distanceCairo in 1200.0..1400.0)
    }

    @Test
    fun testSignedAngleDiffAndAlignment() {
        // Exact alignment
        val diffZero = QiblaHelper.calculateSignedAngleDiff(135.0, 135.0f)
        assertEquals(0.0, diffZero, 0.001)
        assertTrue(QiblaHelper.isFacingKaaba(135.0, 135.0f, 3.5f))

        // Within tolerance
        assertTrue(QiblaHelper.isFacingKaaba(135.0, 137.0f, 3.5f))
        assertTrue(QiblaHelper.isFacingKaaba(135.0, 132.5f, 3.5f))

        // Outside tolerance
        assertFalse(QiblaHelper.isFacingKaaba(135.0, 140.0f, 3.5f))

        // Wrap-around across 0° / 360°
        val diffWrap = QiblaHelper.calculateSignedAngleDiff(5.0, 355.0f)
        assertEquals(10.0, diffWrap, 0.001) // Target is 10 deg clockwise (turn right)
        assertTrue(QiblaHelper.isFacingKaaba(2.0, 359.0f, 3.5f))
    }

    @Test
    fun testCardinalDirections() {
        assertEquals("N", QiblaHelper.getCardinalDirection(0f))
        assertEquals("NNE", QiblaHelper.getCardinalDirection(22f))
        assertEquals("NE", QiblaHelper.getCardinalDirection(45f))
        assertEquals("E", QiblaHelper.getCardinalDirection(90f))
        assertEquals("SE", QiblaHelper.getCardinalDirection(135f))
        assertEquals("S", QiblaHelper.getCardinalDirection(180f))
        assertEquals("SW", QiblaHelper.getCardinalDirection(225f))
        assertEquals("W", QiblaHelper.getCardinalDirection(270f))
        assertEquals("NW", QiblaHelper.getCardinalDirection(315f))
    }
}
