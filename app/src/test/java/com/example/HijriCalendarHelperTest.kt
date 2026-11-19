package com.example

import com.example.calculator.HijriCalendarHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HijriCalendarHelperTest {

    @Test
    fun testGregorianToHijriConversion() {
        // Test known Gregorian date: August 24, 2026
        val hijriDate = HijriCalendarHelper.convertGregorianToHijri(2026, 8, 24, 0)
        assertNotNull(hijriDate)
        assertEquals(1448, hijriDate.year)
        // In AH 1448, August 2026 falls in Safar (month 2)
        assertEquals(2, hijriDate.month)
        assertTrue(hijriDate.day in 1..30)
        assertTrue(hijriDate.formatAr().contains("١٤٤٨") || hijriDate.formatAr().contains("1448") || hijriDate.formatAr().contains("صفر"))
        assertTrue(hijriDate.formatEn().contains("1448"))
    }

    @Test
    fun testBidirectionalConversionRoundTrip() {
        // 1 Ramadan 1445 -> Gregorian -> Hijri
        val greg = HijriCalendarHelper.convertHijriToGregorian(1445, 9, 1)
        assertNotNull(greg)
        assertEquals(2024, greg.year)
        assertEquals(3, greg.month) // March 2024

        val backToHijri = HijriCalendarHelper.convertGregorianToHijri(greg.year, greg.month, greg.day, 0)
        assertEquals(1445, backToHijri.year)
        assertEquals(9, backToHijri.month)
        assertEquals(1, backToHijri.day)
    }

    @Test
    fun testMonthNamesLocalization() {
        assertEquals(12, HijriCalendarHelper.arabicMonthNames.size)
        assertEquals(12, HijriCalendarHelper.englishMonthNames.size)
        assertEquals(12, HijriCalendarHelper.gregorianMonthNamesAr.size)
        assertEquals(12, HijriCalendarHelper.gregorianMonthNamesEn.size)
        assertEquals(12, HijriCalendarHelper.gregorianMonthNamesFr.size)

        assertEquals("رمضان", HijriCalendarHelper.getMonthNameAr(9))
        assertEquals("Ramadan", HijriCalendarHelper.getMonthNameEn(9))
        assertEquals("يناير", HijriCalendarHelper.gregorianMonthNamesAr[0])
        assertEquals("January", HijriCalendarHelper.gregorianMonthNamesEn[0])
        assertEquals("Janvier", HijriCalendarHelper.gregorianMonthNamesFr[0])
    }

    @Test
    fun testOccasionsDetection() {
        val occurrences = HijriCalendarHelper.occurrences
        assertTrue("Occasions list should contain major Islamic events", occurrences.isNotEmpty())
        val ramadanOccasion = occurrences.find { it.hijriMonth == 9 && it.hijriDay == 1 }
        assertNotNull("1st of Ramadan must be registered", ramadanOccasion)
    }
}
