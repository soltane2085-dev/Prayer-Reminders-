package com.example.calculator

import java.util.Calendar
import kotlin.math.*

enum class CalculationMethod(val displayName: String, val fajrAngle: Double, val ishaAngle: Double) {
    MWL("Muslim World League", 18.0, 17.0),
    ISNA("Islamic Society of North America", 15.0, 15.0),
    EGYPT("Egyptian General Authority of Survey", 19.5, 17.5),
    UMM_AL_QURA("Mecca / Umm Al-Qura", 18.5, 0.0), // Special calculation for Isha (Maghrib + 90min)
    KARACHI("University of Islamic Sciences, Karachi", 18.0, 18.0)
}

enum class AsrJuristicMethod(val displayName: String, val shadowFactor: Double) {
    STANDARD("Standard (Shafi'i, Maliki, Hanbali)", 1.0),
    HANAFI("Hanafi Shadow", 2.0)
}

data class CityPreset(
    val cityName: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: Double
) {
    companion object {
        val presets = listOf(
            CityPreset("Mecca, Saudi Arabia", 21.3891, 39.8579, 3.0),
            CityPreset("Cairo, Egypt", 30.0444, 31.2357, 2.0),
            CityPreset("Kuala Lumpur, Malaysia", 3.1390, 101.6869, 8.0),
            CityPreset("Jakarta, Indonesia", -6.2088, 106.8456, 7.0),
            CityPreset("Karachi, Pakistan", 24.8607, 67.0011, 5.0),
            CityPreset("London, United Kingdom", 51.5074, -0.1278, 1.0),
            CityPreset("New York, United States", 40.7128, -74.0060, -4.0),
            CityPreset("Paris, France", 48.8566, 2.3522, 2.0)
        )
    }
}

class PrayerTimeCalculator {

    companion object {
        private fun dsin(d: Double) = sin(Math.toRadians(d))
        private fun dcos(d: Double) = cos(Math.toRadians(d))
        private fun dtan(d: Double) = tan(Math.toRadians(d))
        private fun dasin(r: Double) = Math.toDegrees(asin(r))
        private fun dacos(r: Double) = Math.toDegrees(acos(r))
        private fun datan(r: Double) = Math.toDegrees(atan(r))
        private fun datan2(y: Double, x: Double) = Math.toDegrees(atan2(y, x))

        private fun normalize(value: Double, max: Double): Double {
            var v = value
            while (v < 0) v += max
            while (v >= max) v -= max
            return v
        }

        private fun getJulianDate(year: Int, month: Int, day: Int): Double {
            var y = year
            var m = month
            if (m <= 2) {
                y -= 1
                m += 12
            }
            val a = (y / 100.0).toInt()
            val b = 2 - a + (a / 4.0).toInt()
            return (365.25 * (y + 4716)).toInt() + (30.6001 * (m + 1)).toInt() + day + b - 1524.5
        }
    }

    fun calculateTimes(
        year: Int,
        month: Int,
        day: Int,
        latitude: Double,
        longitude: Double,
        timezoneOffset: Double,
        method: CalculationMethod,
        asrMethod: AsrJuristicMethod
    ): Map<String, String> {
        val jd = getJulianDate(year, month, day)
        val d = jd - 2451545.0

        // Step 1: Calculate sun coordinates
        val g = normalize(357.529 + 0.98560028 * d, 360.0)
        val q = normalize(280.459 + 0.98564736 * d, 360.0)
        val l = normalize(q + 1.915 * dsin(g) + 0.020 * dsin(2.0 * g), 360.0)
        val obliquity = 23.439 - 0.00000036 * d

        // Declination
        val declination = dasin(dsin(obliquity) * dsin(l))

        // Right Ascension
        var ra = datan2(dcos(obliquity) * dsin(l), dcos(l))
        ra = normalize(ra, 360.0)

        // Equation of Time in hours
        val diff = q - ra
        var normDiff = diff
        while (normDiff < -180.0) normDiff += 360.0
        while (normDiff > 180.0) normDiff -= 360.0
        val eqT = normDiff / 15.0

        // Transit time (Noon)
        val transit = 12.0 - eqT
        val localTransit = transit - (longitude / 15.0) + timezoneOffset

        // Dhuhr is noon + short buffer
        val dhuhrVal = localTransit + (1.5 / 60.0)

        // Hour angle helper
        fun getHourAngle(angle: Double, isMorning: Boolean): Double? {
            val num = dsin(-angle) - dsin(latitude) * dsin(declination)
            val den = dcos(latitude) * dcos(declination)
            val cosH = num / den
            if (cosH < -1.0 || cosH > 1.0) return null // Polar midnight/day
            val h = dacos(cosH)
            return if (isMorning) -h else h
        }

        // Sunrise (sun altitude at -0.833 degrees)
        val hSunrise = getHourAngle(0.833, true)
        val sunriseVal = hSunrise?.let { localTransit + it / 15.0 }

        // Sunset (sun altitude at -0.833 degrees)
        val hSunset = getHourAngle(0.833, false)
        val sunsetVal = hSunset?.let { localTransit + it / 15.0 }

        // Fajr (sun altitude at calculation method's angle)
        val hFajr = getHourAngle(method.fajrAngle, true)
        val fajrVal = hFajr?.let { localTransit + it / 15.0 }

        // Asr calculation
        val asrDiff = abs(latitude - declination)
        val asrAngle = datan(1.0 / (asrMethod.shadowFactor + dtan(asrDiff)))
        val hAsr = getHourAngle(-asrAngle, false)
        val asrVal = hAsr?.let { localTransit + it / 15.0 }

        // Isha calculation
        val ishaVal = if (method == CalculationMethod.UMM_AL_QURA) {
            sunsetVal?.let { it + 1.5 } // 90 minutes after Maghrib
        } else {
            val hIsha = getHourAngle(method.ishaAngle, false)
            hIsha?.let { localTransit + it / 15.0 }
        }

        val results = mutableMapOf<String, String>()
        results["Fajr"] = formatTime(fajrVal)
        results["Sunrise"] = formatTime(sunriseVal)
        results["Dhuhr"] = formatTime(dhuhrVal)
        results["Asr"] = formatTime(asrVal)
        results["Maghrib"] = formatTime(sunsetVal)
        results["Isha"] = formatTime(ishaVal)

        return results
    }

    private fun formatTime(timeVal: Double?): String {
        if (timeVal == null) return "N/A"
        var tv = normalize(timeVal, 24.0)
        val hours = tv.toInt()
        val minutes = ((tv - hours) * 60.0 + 0.5).toInt()
        var finalHours = hours
        var finalMinutes = minutes
        if (finalMinutes >= 60) {
            finalHours += 1
            finalMinutes -= 60
        }
        finalHours = finalHours % 24

        return String.format("%02d:%02d", finalHours, finalMinutes)
    }
}
