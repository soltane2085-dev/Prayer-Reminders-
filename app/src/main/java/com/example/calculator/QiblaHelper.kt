package com.example.calculator

import kotlin.math.*

object QiblaHelper {
    // Kaaba coordinates (Makkah Al-Mukarramah, Saudi Arabia)
    const val KAABA_LAT = 21.422487
    const val KAABA_LON = 39.826206

    /**
     * Calculates the Qibla direction (azimuth angle) in degrees (0 to 360) 
     * clockwise from TRUE North for any given geographical latitude & longitude.
     */
    fun calculateQiblaDirection(latitude: Double, longitude: Double): Double {
        val latRad = Math.toRadians(latitude)
        val lonRad = Math.toRadians(longitude)
        val kaabaLatRad = Math.toRadians(KAABA_LAT)
        val kaabaLonRad = Math.toRadians(KAABA_LON)

        val deltaLon = kaabaLonRad - lonRad

        val numerator = sin(deltaLon)
        val denominator = cos(latRad) * tan(kaabaLatRad) - sin(latRad) * cos(deltaLon)

        val qiblaRad = atan2(numerator, denominator)
        var qiblaDeg = Math.toDegrees(qiblaRad)

        // Standardize to 0..360 deg
        qiblaDeg = (qiblaDeg + 360.0) % 360.0
        return qiblaDeg
    }

    /**
     * Calculates great-circle distance to the Holy Kaaba in kilometers using the Haversine formula.
     */
    fun calculateDistanceToKaaba(latitude: Double, longitude: Double): Double {
        val earthRadius = 6371.0 // kilometers
        val dLat = Math.toRadians(KAABA_LAT - latitude)
        val dLon = Math.toRadians(KAABA_LON - longitude)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(latitude)) * cos(Math.toRadians(KAABA_LAT)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    /**
     * Calculates distance to Kaaba in miles.
     */
    fun calculateDistanceToKaabaMiles(latitude: Double, longitude: Double): Double {
        return calculateDistanceToKaaba(latitude, longitude) * 0.621371
    }

    /**
     * Computes the shortest signed angular difference between target Qibla angle and current phone heading.
     * Result is in range [-180, +180]. Positive means Kaaba is to the right (turn clockwise),
     * negative means Kaaba is to the left (turn counter-clockwise).
     */
    fun calculateSignedAngleDiff(qiblaAngle: Double, phoneHeading: Float): Double {
        var diff = (qiblaAngle - phoneHeading) % 360.0
        if (diff > 180.0) diff -= 360.0
        if (diff < -180.0) diff += 360.0
        return diff
    }

    /**
     * Returns true if the phone is facing the Kaaba within the specified tolerance threshold.
     */
    fun isFacingKaaba(qiblaAngle: Double, phoneHeading: Float, toleranceDegrees: Float = 3.5f): Boolean {
        val diff = abs(calculateSignedAngleDiff(qiblaAngle, phoneHeading))
        return diff <= toleranceDegrees
    }

    /**
     * Returns cardinal / intercardinal abbreviation for any heading angle (0..360).
     */
    fun getCardinalDirection(heading: Float): String {
        val normalized = (heading % 360f + 360f) % 360f
        return when {
            normalized >= 337.5f || normalized < 22.5f -> "N"
            normalized < 67.5f -> "NE"
            normalized < 112.5f -> "E"
            normalized < 157.5f -> "SE"
            normalized < 202.5f -> "S"
            normalized < 247.5f -> "SW"
            normalized < 292.5f -> "W"
            else -> "NW"
        }
    }
}
