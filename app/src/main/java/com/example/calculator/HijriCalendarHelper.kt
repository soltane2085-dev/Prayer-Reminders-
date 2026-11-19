package com.example.calculator

import android.os.Build
import java.time.LocalDate
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahEra
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.TimeZone

data class HijriDate(
    val day: Int,
    val month: Int, // 1-based (1 to 12)
    val year: Int,
    val monthNameAr: String,
    val monthNameEn: String,
    val dayOfWeekAr: String = "",
    val dayOfWeekEn: String = ""
) {
    fun formatAr(): String = if (dayOfWeekAr.isNotEmpty()) "$dayOfWeekAr $day $monthNameAr $year هـ" else "$day $monthNameAr $year هـ"
    fun formatEn(): String = if (dayOfWeekEn.isNotEmpty()) "$dayOfWeekEn, $day $monthNameEn $year AH" else "$day $monthNameEn $year AH"
    fun formatFr(): String = if (dayOfWeekEn.isNotEmpty()) "$day $monthNameEn $year H" else "$day $monthNameEn $year H"
}

data class GregorianDateResult(
    val day: Int,
    val month: Int, // 1-based (1-12)
    val year: Int,
    val monthNameAr: String,
    val monthNameEn: String,
    val monthNameFr: String,
    val dayOfWeekAr: String = "",
    val dayOfWeekEn: String = "",
    val dayOfWeekFr: String = ""
) {
    fun formatAr(): String = if (dayOfWeekAr.isNotEmpty()) "$dayOfWeekAr، $day $monthNameAr $year م" else "$day $monthNameAr $year م"
    fun formatEn(): String = if (dayOfWeekEn.isNotEmpty()) "$dayOfWeekEn, $monthNameEn $day, $year" else "$monthNameEn $day, $year"
    fun formatFr(): String = if (dayOfWeekFr.isNotEmpty()) "$dayOfWeekFr $day $monthNameFr $year" else "$day $monthNameFr $year"
}

object HijriCalendarHelper {
    val arabicMonthNames = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
        "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )

    val englishMonthNames = listOf(
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani", "Jumada al-Awwal", "Jumada al-Thani",
        "Rajab", "Sha'ban", "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
    )

    val arabicDayNames = listOf(
        "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"
    )

    val englishDayNames = listOf(
        "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    )

    val frenchDayNames = listOf(
        "Dimanche", "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi"
    )

    val gregorianMonthNamesAr = listOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )

    val gregorianMonthNamesEn = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    val gregorianMonthNamesFr = listOf(
        "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
        "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
    )

    fun getMonthNameAr(month: Int): String {
        val m = month.coerceIn(1, 12)
        return arabicMonthNames[m - 1]
    }

    fun getMonthNameEn(month: Int): String {
        val m = month.coerceIn(1, 12)
        return englishMonthNames[m - 1]
    }

    /**
     * Converts Julian Day Number to Hijri Date.
     */
    fun julianToHijri(jd: Int): HijriDate {
        return julianToHijriFallback(jd)
    }

    private fun julianToHijriFallback(jd: Int): HijriDate {
        // Calibrated epoch to align accurately with modern Umm al-Qura standard
        val jdZero = 1948439
        val daysIn30Years = 10631
        val jdDiff = jd - jdZero
        
        var cycle = jdDiff / daysIn30Years
        var rem = jdDiff % daysIn30Years
        if (rem < 0) {
            rem += daysIn30Years
            cycle -= 1
        }
        
        val leapYears = intArrayOf(2, 5, 7, 10, 13, 16, 18, 21, 24, 26, 29)
        var yearInCycle = 1
        var daysLeft = rem
        
        while (yearInCycle <= 30) {
            val isLeap = leapYears.contains(yearInCycle)
            val daysInYear = if (isLeap) 355 else 354
            if (daysLeft < daysInYear) {
                break
            }
            daysLeft -= daysInYear
            yearInCycle++
        }
        
        val hijriYear = cycle * 30 + yearInCycle
        
        var hijriMonth = 1
        while (hijriMonth <= 12) {
            var monthDays = if (hijriMonth % 2 == 1) 30 else 29
            if (hijriMonth == 12) {
                val isLeap = leapYears.contains(yearInCycle)
                monthDays = if (isLeap) 30 else 29
            }
            
            if (daysLeft < monthDays) {
                break
            }
            daysLeft -= monthDays
            hijriMonth++
        }
        
        val hijriDay = daysLeft + 1
        val validMonth = hijriMonth.coerceIn(1, 12)
        
        val dowIndex = (((jd + 1) % 7) + 7) % 7
        val dayAr = arabicDayNames.getOrElse(dowIndex) { "" }
        val dayEn = englishDayNames.getOrElse(dowIndex) { "" }

        return HijriDate(
            day = hijriDay,
            month = validMonth,
            year = hijriYear,
            monthNameAr = getMonthNameAr(validMonth),
            monthNameEn = getMonthNameEn(validMonth),
            dayOfWeekAr = dayAr,
            dayOfWeekEn = dayEn
        )
    }

    /**
     * Converts a Gregorian Date to Julian Day Number.
     */
    fun gregorianToJulian(year: Int, month: Int, day: Int): Int {
        var m = month
        var y = year
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = (y / 100.0).toInt()
        val b = 2 - a + (a / 4.0).toInt()
        val jd = (365.25 * (y + 4716)).toInt() + (30.6001 * (m + 1)).toInt() + day + b - 1524
        return jd
    }

    /**
     * Converts Julian Day Number to Gregorian Date (as java.util.Calendar).
     */
    fun julianToGregorian(jd: Int): Calendar {
        val w = ((jd - 1867216.25) / 36524.25).toInt()
        val x = (w / 4.0).toInt()
        val a = jd + 1 + w - x
        val b = a + 1524
        val c = ((b - 122.1) / 365.25).toInt()
        val d = (365.25 * c).toInt()
        val e = ((b - d) / 30.6001).toInt()
        
        val day = b - d - (30.6001 * e).toInt()
        val month = if (e < 14) e - 1 else e - 13
        val year = if (month > 2) c - 4716 else c - 4715
        
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.DAY_OF_MONTH, day)
        cal.set(Calendar.HOUR_OF_DAY, 12)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }

    /**
     * Converts Hijri Date to Julian Day Number.
     */
    fun hijriToJulian(year: Int, month: Int, day: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val hDate = HijrahChronology.INSTANCE.date(
                    HijrahEra.AH, year, month, day.coerceIn(1, 30)
                )
                val gDate = LocalDate.from(hDate)
                return gregorianToJulian(gDate.year, gDate.monthValue, gDate.dayOfMonth)
            } catch (e: Throwable) {
                // fallback
            }
        }
        return hijriToJulianFallback(year, month, day)
    }

    private fun hijriToJulianFallback(year: Int, month: Int, day: Int): Int {
        val jdZero = 1948439
        val daysIn30Years = 10631
        val cycle = (year - 1) / 30
        val yearInCycle = year - (cycle * 30)
        
        var jd = jdZero + cycle * daysIn30Years
        val leapYears = intArrayOf(2, 5, 7, 10, 13, 16, 18, 21, 24, 26, 29)
        for (y in 1 until yearInCycle) {
            val isLeap = leapYears.contains(y)
            jd += if (isLeap) 355 else 354
        }
        
        for (m in 1 until month) {
            val monthDays = if (m % 2 == 1) 30 else 29
            jd += monthDays
        }
        
        jd += (day - 1)
        return jd
    }

    /**
     * Converts Gregorian Date to Hijri Date with offset support.
     * Uses official Umm al-Qura standard calendar.
     */
    fun convertGregorianToHijri(year: Int, month: Int, day: Int, offsetDays: Int = 0): HijriDate {
        val cal = Calendar.getInstance().apply {
            set(year, month - 1, day)
            add(Calendar.DAY_OF_MONTH, offsetDays)
        }
        val dow = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon... 7=Sat
        val dayAr = arabicDayNames.getOrElse(dow - 1) { "" }
        val dayEn = englishDayNames.getOrElse(dow - 1) { "" }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val localDate = LocalDate.of(year, month, day).plusDays(offsetDays.toLong())
                val hijrahDate = HijrahChronology.INSTANCE.date(localDate)
                val hYear = hijrahDate.get(ChronoField.YEAR)
                val hMonth = hijrahDate.get(ChronoField.MONTH_OF_YEAR)
                val hDay = hijrahDate.get(ChronoField.DAY_OF_MONTH)

                return HijriDate(
                    day = hDay,
                    month = hMonth,
                    year = hYear,
                    monthNameAr = getMonthNameAr(hMonth),
                    monthNameEn = getMonthNameEn(hMonth),
                    dayOfWeekAr = dayAr,
                    dayOfWeekEn = dayEn
                )
            } catch (e: Throwable) {
                // Fallback below
            }
        }

        val jd = gregorianToJulian(year, month, day)
        val result = julianToHijri(jd + offsetDays)
        return result.copy(dayOfWeekAr = dayAr, dayOfWeekEn = dayEn)
    }

    /**
     * Converts Hijri Date to Gregorian Date.
     */
    fun convertHijriToGregorian(year: Int, month: Int, day: Int): GregorianDateResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val hDate = HijrahChronology.INSTANCE.date(
                    HijrahEra.AH, year, month.coerceIn(1, 12), day.coerceIn(1, 30)
                )
                val gDate = LocalDate.from(hDate)
                val gYear = gDate.year
                val gMonth = gDate.monthValue
                val gDay = gDate.dayOfMonth
                val dow = gDate.dayOfWeek.value % 7 // 0=Sunday, 1=Monday... 6=Saturday
                val dayAr = arabicDayNames.getOrElse(dow) { "" }
                val dayEn = englishDayNames.getOrElse(dow) { "" }
                val dayFr = frenchDayNames.getOrElse(dow) { "" }

                val mAr = gregorianMonthNamesAr.getOrElse(gMonth - 1) { "" }
                val mEn = gregorianMonthNamesEn.getOrElse(gMonth - 1) { "" }
                val mFr = gregorianMonthNamesFr.getOrElse(gMonth - 1) { "" }

                return GregorianDateResult(
                    day = gDay,
                    month = gMonth,
                    year = gYear,
                    monthNameAr = mAr,
                    monthNameEn = mEn,
                    monthNameFr = mFr,
                    dayOfWeekAr = dayAr,
                    dayOfWeekEn = dayEn,
                    dayOfWeekFr = dayFr
                )
            } catch (e: Throwable) {
                // fallback
            }
        }

        val jd = hijriToJulian(year, month, day)
        val cal = julianToGregorian(jd)
        val gYear = cal.get(Calendar.YEAR)
        val gMonth = cal.get(Calendar.MONTH) + 1
        val gDay = cal.get(Calendar.DAY_OF_MONTH)
        val dow = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon... 7=Sat
        val dayAr = arabicDayNames.getOrElse(dow - 1) { "" }
        val dayEn = englishDayNames.getOrElse(dow - 1) { "" }
        val dayFr = frenchDayNames.getOrElse(dow - 1) { "" }

        val mAr = gregorianMonthNamesAr.getOrElse(gMonth - 1) { "" }
        val mEn = gregorianMonthNamesEn.getOrElse(gMonth - 1) { "" }
        val mFr = gregorianMonthNamesFr.getOrElse(gMonth - 1) { "" }

        return GregorianDateResult(
            day = gDay,
            month = gMonth,
            year = gYear,
            monthNameAr = mAr,
            monthNameEn = mEn,
            monthNameFr = mFr,
            dayOfWeekAr = dayAr,
            dayOfWeekEn = dayEn,
            dayOfWeekFr = dayFr
        )
    }

    /**
     * Returns total days in a given Hijri month of a Hijri year.
     */
    fun getDaysInHijriMonth(year: Int, month: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val hDate = HijrahChronology.INSTANCE.date(
                    HijrahEra.AH, year, month, 1
                )
                return hDate.lengthOfMonth()
            } catch (e: Throwable) {
                // fallback
            }
        }
        val jd1 = hijriToJulian(year, month, 1)
        val nextMonth = if (month == 12) 1 else month + 1
        val nextYear = if (month == 12) year + 1 else year
        val jd2 = hijriToJulian(nextYear, nextMonth, 1)
        return (jd2 - jd1).coerceIn(29, 30)
    }

    /**
     * Get the Hijri day of the week (standard Islamic week: Sunday=1, Saturday=7).
     */
    fun getHijriDayOfWeek(year: Int, month: Int, day: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val hDate = HijrahChronology.INSTANCE.date(
                    HijrahEra.AH, year, month, day.coerceIn(1, 30)
                )
                val localDate = LocalDate.from(hDate)
                val dow = localDate.dayOfWeek.value // 1=Monday..7=Sunday
                return if (dow == 7) 1 else dow + 1
            } catch (e: Throwable) {
                // fallback
            }
        }
        val jd = hijriToJulian(year, month, day)
        return (((jd + 1) % 7) + 7) % 7 + 1
    }

    data class IslamicOccasion(
        val id: String,
        val hijriMonth: Int,
        val hijriDay: Int,
        val titleAr: String,
        val titleEn: String,
        val descAr: String,
        val descEn: String
    )

    val occurrences = listOf(
        IslamicOccasion("new_year", 1, 1, "رأس السنة الهجرية", "Islamic New Year", "بداية العام الهجري الجديد وتحول الهجرة النبوية المباركة", "Start of the new Islamic Hijri year"),
        IslamicOccasion("ashura", 1, 10, "يوم عاشوراء", "Day of Ashura", "يوم نجى الله فيه سيدنا موسى وقومه ويُستحب صيام يوم قبله ومعه", "The day Allah saved Moses and his people; fasting is highly recommended"),
        IslamicOccasion("mawlid", 3, 12, "المولد النبوي الشريف", "Prophet Muhammad's Birthday", "ذكرى مولد رسول الإنسانية والرحمة مهدى الله للعالمين صلى الله عليه وسلم", "Mawlid al-Nabi, commemorating birth of Prophet Muhammad (PBUH)"),
        IslamicOccasion("isra_mi_raj", 7, 27, "ليلة الإسراء والمعراج", "Isra' and Mi'raj", "الرحلة الإعجازية من المسجد الحرام للبيت المقدس والعروج للسماء وفرض الصلوات الخمس", "Night of miraculous journey and ascension, when 5 prayers were ordained"),
        IslamicOccasion("mid_shaban", 8, 15, "ليلة النصف من شعبان", "Mid-Sha'ban", "ليلة مباركة تغفر فيها الذنوب ويحوّل فيها اتجاه المسلمين للقبلة", "The night of salvation and dynamic Qibla shift towards Kaaba"),
        IslamicOccasion("ramadan_start", 9, 1, "بداية شهر رمضان المبارك", "First Day of Ramadan", "شهر الصيام، والقرآن، والعبادات، والبركة الكبرى وعتق الرقاب", "Month of fasting, elevated prayer, and Quranic revelation"),
        IslamicOccasion("laylat_al_qadr", 9, 27, "ليلة القدر المباركة (تقديرية)", "Laylat al-Qadr (Estimated)", "ليلة القدر المباركة خير من ألف شهر والتي نزل فيها القرآن الكريم", "The Night of Decree, better than a thousand months"),
        IslamicOccasion("eid_fitr", 10, 1, "عيد الفطر المبارك", "Eid al-Fitr", "جائزة الصائمين وفرحتهم بإتمام فريضة الصيام المباركة والتقرب لله", "Festival of breaking the fast, marking the completion of Ramadan"),
        IslamicOccasion("sacred_month", 11, 1, "الأشهر الحرم (ذو القعدة)", "Sacred Month (Dhu al-Qi'dah)", "أول شهور الحج والأشهر الحرم التي يُكره فيها القتال ويتعاظم الثواب", "Beginning of sacred months with heightened spiritual values"),
        IslamicOccasion("dhu_al_hijjah_10", 12, 1, "أيام العشر من ذي الحجة", "First 10 Days of Dhu al-Hijjah", "أعظم وأحب أيام العام إلى الله عز وجل ويُستحب الإكثار من العبادات والتكبير فيها", "The most beloved ten days of the year for righteous deeds"),
        IslamicOccasion("arafa", 12, 9, "يوم عرفة المبارك", "Day of Arafah", "يوم الحج الأكبر ومغفرة عامين من الذنوب وصيامه لغير الحاج فضيلة عظيمة", "Day of Arafah, fasting is highly recommended for non-pilgrims"),
        IslamicOccasion("eid_adha", 12, 10, "عيد الأضحى المبارك", "Eid al-Adha", "عيد التضحية والفرح وإتمام مناسك الحج الكبرى بالأقطار الإسلامية", "Festival of Sacrifice, marking the completion of major Hajj rituals")
    )

    /**
     * Calculates days remaining until a specific Hijri event from the current Hijri date (with offset).
     * Handles wrapping to the next Hijri year if the event has already passed in the current Hijri year.
     */
    fun calculateDaysUntilOccasion(currentHijri: HijriDate, occasion: IslamicOccasion, offsetDays: Int = 0): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val currentHDate = HijrahChronology.INSTANCE.date(
                    HijrahEra.AH, currentHijri.year, currentHijri.month, currentHijri.day.coerceIn(1, 30)
                )
                val currentLocalDate = LocalDate.from(currentHDate)

                var occHDate = try {
                    HijrahChronology.INSTANCE.date(
                        HijrahEra.AH, currentHijri.year, occasion.hijriMonth, occasion.hijriDay
                    )
                } catch (e: Throwable) {
                    HijrahChronology.INSTANCE.date(
                        HijrahEra.AH, currentHijri.year, occasion.hijriMonth, 29
                    )
                }

                var occLocalDate = LocalDate.from(occHDate)
                if (occLocalDate.isBefore(currentLocalDate)) {
                    val nextOccHDate = try {
                        HijrahChronology.INSTANCE.date(
                            HijrahEra.AH, currentHijri.year + 1, occasion.hijriMonth, occasion.hijriDay
                        )
                    } catch (e: Throwable) {
                        HijrahChronology.INSTANCE.date(
                            HijrahEra.AH, currentHijri.year + 1, occasion.hijriMonth, 29
                        )
                    }
                    occLocalDate = LocalDate.from(nextOccHDate)
                }
                return ChronoUnit.DAYS.between(currentLocalDate, occLocalDate).toInt()
            } catch (e: Throwable) {
                // fallback below
            }
        }

        val currentJulian = hijriToJulian(currentHijri.year, currentHijri.month, currentHijri.day)
        var occasionJulian = hijriToJulian(currentHijri.year, occasion.hijriMonth, occasion.hijriDay)
        
        if (occasionJulian < currentJulian) {
            occasionJulian = hijriToJulian(currentHijri.year + 1, occasion.hijriMonth, occasion.hijriDay)
        }
        
        return occasionJulian - currentJulian
    }
}

