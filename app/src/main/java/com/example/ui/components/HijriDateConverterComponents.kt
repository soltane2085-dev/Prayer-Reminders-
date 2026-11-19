package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.calculator.HijriCalendarHelper
import com.example.calculator.HijriDate
import com.example.ui.theme.CelestialGrey
import com.example.ui.theme.DeepSkyBlue
import com.example.ui.theme.GoldenSun
import com.example.ui.theme.GreenEmerald
import com.example.ui.theme.LocalIsIslamicArtTheme
import com.example.ui.theme.PureDawnLight
import com.example.ui.theme.RubElHizbStarIcon
import com.example.ui.theme.TwilightCard
import com.example.ui.translation.Translation
import com.example.viewmodel.PrayerViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Dashboard Dual Date Header Card:
 * Displays current Hijri Date & Gregorian Date side-by-side with rich ornamentation,
 * active occasion indicator, and instant "Date Converter" trigger button.
 */
@Composable
fun DualDateDashboardCard(
    viewModel: PrayerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isIslamicArt = LocalIsIslamicArtTheme.current
    val appLang by viewModel.appLang.collectAsStateWithLifecycle()
    val hijriOffset by viewModel.hijriOffset.collectAsStateWithLifecycle()
    val currentHijriDate by viewModel.currentHijriDate.collectAsStateWithLifecycle()
    val t = { key: String -> Translation.get(key, appLang) }

    val todayCal = remember { Calendar.getInstance() }
    val gregDateFormatted = remember(appLang) {
        val locale = when (appLang) {
            "AR" -> Locale("ar")
            "FR" -> Locale.FRANCE
            else -> Locale.US
        }
        val sdf = SimpleDateFormat("EEEE, d MMMM yyyy", locale)
        sdf.format(todayCal.time)
    }

    // Check if today has a registered Islamic occasion
    val todayOccasion = remember(currentHijriDate) {
        HijriCalendarHelper.occurrences.find {
            it.hijriMonth == currentHijriDate.month && it.hijriDay == currentHijriDate.day
        }
    }

    val cardBg = if (isIslamicArt) {
        Color(0xFF1E1710)
    } else {
        TwilightCard
    }

    val borderColor = if (isIslamicArt) {
        Color(0xFF8B6508).copy(alpha = 0.5f)
    } else {
        Color(0xFF2A2E55)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dual_date_dashboard_card"),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Subtle Islamic geometric accent watermark on background if islamic art theme
            if (isIslamicArt) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp)
                ) {
                    RubElHizbStarIcon(
                        modifier = Modifier.size(90.dp),
                        color = GoldenSun.copy(alpha = 0.08f),
                        strokeColor = GoldenSun.copy(alpha = 0.12f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                // Top Row: Title Badge & Date Converter Launch Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    if (isIslamicArt) Color(0xFF382613) else Color(0xFF222448),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isIslamicArt) {
                                RubElHizbStarIcon(
                                    modifier = Modifier.size(18.dp),
                                    color = GoldenSun,
                                    strokeColor = GoldenSun
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Hijri Calendar",
                                    tint = GoldenSun,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Text(
                            text = t("dual_date_title"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isIslamicArt) GoldenSun else DeepSkyBlue,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Interactive "Date Converter" Quick Action Pill Button
                    Surface(
                        onClick = { viewModel.setShowDateConverterDialog(true) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isIslamicArt) Color(0xFF3E2D17) else Color(0xFF26294F),
                        border = BorderStroke(1.dp, if (isIslamicArt) GoldenSun.copy(alpha = 0.5f) else DeepSkyBlue.copy(alpha = 0.4f)),
                        modifier = Modifier.testTag("btn_dashboard_open_converter")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Open Date Converter",
                                tint = GoldenSun,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = t("btn_open_converter"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureDawnLight
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Prominent Hijri Date Display
                val hijriDisplay = when (appLang) {
                    "AR" -> currentHijriDate.formatAr()
                    "FR" -> currentHijriDate.formatFr()
                    else -> currentHijriDate.formatEn()
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = hijriDisplay,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = PureDawnLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = null,
                                tint = CelestialGrey,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = gregDateFormatted,
                                fontSize = 13.sp,
                                color = CelestialGrey,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // If today is a special occasion, display elegant badge
                if (todayOccasion != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        GoldenSun.copy(alpha = 0.15f),
                                        Color(0xFF10B981).copy(alpha = 0.12f)
                                    )
                                ),
                                RoundedCornerShape(10.dp)
                            )
                            .border(1.dp, GoldenSun.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = GoldenSun,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (appLang == "AR") todayOccasion.titleAr else todayOccasion.titleEn,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldenSun
                            )
                            Text(
                                text = "• " + (if (appLang == "AR") todayOccasion.descAr else todayOccasion.descEn),
                                fontSize = 11.sp,
                                color = CelestialGrey,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interactive Full Modal Dialog for Bidirectional Hijri <-> Gregorian Date Conversion
 */
@Composable
fun HijriDateConverterDialog(
    viewModel: PrayerViewModel,
    onDismiss: () -> Unit
) {
    val appLang by viewModel.appLang.collectAsStateWithLifecycle()
    val isIslamicArt = LocalIsIslamicArtTheme.current
    val t = { key: String -> Translation.get(key, appLang) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .testTag("dialog_hijri_date_converter"),
            shape = RoundedCornerShape(28.dp),
            color = if (isIslamicArt) Color(0xFF1B140D) else Color(0xFF15162B),
            border = BorderStroke(1.5.dp, if (isIslamicArt) GoldenSun.copy(alpha = 0.6f) else Color(0xFF383C6E)),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (isIslamicArt) Color(0xFF352310) else Color(0xFF222549),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isIslamicArt) {
                                RubElHizbStarIcon(
                                    modifier = Modifier.size(22.dp),
                                    color = GoldenSun,
                                    strokeColor = GoldenSun
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = null,
                                    tint = GoldenSun,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = t("hijri_converter_title"),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureDawnLight
                            )
                            Text(
                                text = t("hijri_converter_subtitle"),
                                fontSize = 11.sp,
                                color = CelestialGrey,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF2B2D50).copy(alpha = 0.5f), CircleShape)
                            .testTag("btn_close_converter_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Dialog",
                            tint = PureDawnLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = if (isIslamicArt) Color(0xFF4A341B) else Color(0xFF2C2F58))
                Spacer(modifier = Modifier.height(16.dp))

                // Interactive Converter Component
                HijriDateConverterCardContent(
                    viewModel = viewModel,
                    isInsideDialog = true
                )
            }
        }
    }
}

/**
 * Reusable Body Component of the Hijri <-> Gregorian Date Converter:
 * Features tabs for:
 *  1) Gregorian -> Hijri Conversion
 *  2) Hijri -> Gregorian Conversion
 */
@Composable
fun HijriDateConverterCardContent(
    viewModel: PrayerViewModel,
    isInsideDialog: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appLang by viewModel.appLang.collectAsStateWithLifecycle()
    val hijriOffset by viewModel.hijriOffset.collectAsStateWithLifecycle()
    val isIslamicArt = LocalIsIslamicArtTheme.current
    val t = { key: String -> Translation.get(key, appLang) }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Gregorian -> Hijri, 1: Hijri -> Gregorian

    // Current date initializers
    val todayCal = remember { Calendar.getInstance() }
    val initialGregYear = remember { todayCal.get(Calendar.YEAR) }
    val initialGregMonth = remember { todayCal.get(Calendar.MONTH) + 1 }
    val initialGregDay = remember { todayCal.get(Calendar.DAY_OF_MONTH) }

    val initialTodayHijri = remember(hijriOffset) {
        HijriCalendarHelper.convertGregorianToHijri(
            initialGregYear,
            initialGregMonth,
            initialGregDay,
            hijriOffset
        )
    }

    // State for Gregorian -> Hijri conversion
    var gregYear by remember { mutableIntStateOf(initialGregYear) }
    var gregMonth by remember { mutableIntStateOf(initialGregMonth) }
    var gregDay by remember { mutableIntStateOf(initialGregDay) }

    // State for Hijri -> Gregorian conversion
    var hijriYear by remember { mutableIntStateOf(initialTodayHijri.year) }
    var hijriMonth by remember { mutableIntStateOf(initialTodayHijri.month) }
    var hijriDay by remember { mutableIntStateOf(initialTodayHijri.day) }

    // Tab Selector Row
    TabRow(
        selectedTabIndex = selectedTab,
        containerColor = if (isIslamicArt) Color(0xFF261A10) else Color(0xFF1E2042),
        contentColor = GoldenSun,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                color = GoldenSun,
                height = 3.dp
            )
        },
        divider = { HorizontalDivider(color = Color(0xFF2C2F58)) },
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .testTag("tab_converter_mode")
    ) {
        Tab(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            modifier = Modifier.testTag("tab_greg_to_hijri")
        ) {
            Row(
                modifier = Modifier.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = if (selectedTab == 0) GoldenSun else CelestialGrey,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = t("greg_to_hijri"),
                    fontSize = 13.sp,
                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                    color = if (selectedTab == 0) GoldenSun else CelestialGrey
                )
            }
        }

        Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            modifier = Modifier.testTag("tab_hijri_to_greg")
        ) {
            Row(
                modifier = Modifier.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isIslamicArt) {
                    RubElHizbStarIcon(
                        modifier = Modifier.size(16.dp),
                        color = if (selectedTab == 1) GoldenSun else CelestialGrey,
                        strokeColor = if (selectedTab == 1) GoldenSun else CelestialGrey
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = if (selectedTab == 1) GoldenSun else CelestialGrey,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = t("hijri_to_greg"),
                    fontSize = 13.sp,
                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                    color = if (selectedTab == 1) GoldenSun else CelestialGrey
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (selectedTab == 0) {
        // === MODE 1: Gregorian -> Hijri Conversion ===
        GregorianToHijriSection(
            gregYear = gregYear,
            onYearChange = { gregYear = it },
            gregMonth = gregMonth,
            onMonthChange = { gregMonth = it },
            gregDay = gregDay,
            onDayChange = { gregDay = it },
            onResetToday = {
                val now = Calendar.getInstance()
                gregYear = now.get(Calendar.YEAR)
                gregMonth = now.get(Calendar.MONTH) + 1
                gregDay = now.get(Calendar.DAY_OF_MONTH)
            },
            hijriOffset = hijriOffset,
            appLang = appLang,
            isIslamicArt = isIslamicArt,
            t = t
        )
    } else {
        // === MODE 2: Hijri -> Gregorian Conversion ===
        HijriToGregorianSection(
            hijriYear = hijriYear,
            onYearChange = { hijriYear = it },
            hijriMonth = hijriMonth,
            onMonthChange = { hijriMonth = it },
            hijriDay = hijriDay,
            onDayChange = { hijriDay = it },
            onResetToday = {
                val now = Calendar.getInstance()
                val todayH = HijriCalendarHelper.convertGregorianToHijri(
                    now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH) + 1,
                    now.get(Calendar.DAY_OF_MONTH),
                    hijriOffset
                )
                hijriYear = todayH.year
                hijriMonth = todayH.month
                hijriDay = todayH.day
            },
            appLang = appLang,
            isIslamicArt = isIslamicArt,
            t = t
        )
    }
}

/**
 * Gregorian -> Hijri Conversion UI Section
 */
@Composable
private fun GregorianToHijriSection(
    gregYear: Int,
    onYearChange: (Int) -> Unit,
    gregMonth: Int,
    onMonthChange: (Int) -> Unit,
    gregDay: Int,
    onDayChange: (Int) -> Unit,
    onResetToday: () -> Unit,
    hijriOffset: Int,
    appLang: String,
    isIslamicArt: Boolean,
    t: (String) -> String
) {
    val context = LocalContext.current

    // Convert to Hijri
    val convertedHijri = remember(gregYear, gregMonth, gregDay, hijriOffset) {
        HijriCalendarHelper.convertGregorianToHijri(gregYear, gregMonth, gregDay, hijriOffset)
    }

    // Occasions on the converted Hijri date
    val occasion = remember(convertedHijri) {
        HijriCalendarHelper.occurrences.find {
            it.hijriMonth == convertedHijri.month && it.hijriDay == convertedHijri.day
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = t("select_greg_date"),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = GoldenSun
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Date Pickers Row (Day, Month, Year)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Day selector
            DateNumberPicker(
                label = t("day"),
                value = gregDay,
                range = 1..31,
                onValueChange = onDayChange,
                modifier = Modifier.weight(1f),
                testTagPrefix = "greg_day"
            )

            // Month selector
            DateMonthPicker(
                label = t("month"),
                selectedMonth = gregMonth,
                monthNames = when (appLang) {
                    "AR" -> HijriCalendarHelper.gregorianMonthNamesAr
                    "FR" -> HijriCalendarHelper.gregorianMonthNamesFr
                    else -> HijriCalendarHelper.gregorianMonthNamesEn
                },
                onMonthChange = onMonthChange,
                modifier = Modifier.weight(1.5f),
                testTagPrefix = "greg_month"
            )

            // Year selector
            DateNumberPicker(
                label = t("year"),
                value = gregYear,
                range = 1900..2100,
                onValueChange = onYearChange,
                modifier = Modifier.weight(1.3f),
                testTagPrefix = "greg_year"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Steppers & Jump to Today Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SmallStepButton(text = "-1d", onClick = {
                    val cal = Calendar.getInstance().apply {
                        set(gregYear, gregMonth - 1, gregDay)
                        add(Calendar.DAY_OF_MONTH, -1)
                    }
                    onYearChange(cal.get(Calendar.YEAR))
                    onMonthChange(cal.get(Calendar.MONTH) + 1)
                    onDayChange(cal.get(Calendar.DAY_OF_MONTH))
                }, tag = "btn_greg_prev_day")

                SmallStepButton(text = "+1d", onClick = {
                    val cal = Calendar.getInstance().apply {
                        set(gregYear, gregMonth - 1, gregDay)
                        add(Calendar.DAY_OF_MONTH, 1)
                    }
                    onYearChange(cal.get(Calendar.YEAR))
                    onMonthChange(cal.get(Calendar.MONTH) + 1)
                    onDayChange(cal.get(Calendar.DAY_OF_MONTH))
                }, tag = "btn_greg_next_day")

                SmallStepButton(text = "-1m", onClick = {
                    val cal = Calendar.getInstance().apply {
                        set(gregYear, gregMonth - 1, gregDay)
                        add(Calendar.MONTH, -1)
                    }
                    onYearChange(cal.get(Calendar.YEAR))
                    onMonthChange(cal.get(Calendar.MONTH) + 1)
                    onDayChange(cal.get(Calendar.DAY_OF_MONTH))
                }, tag = "btn_greg_prev_month")

                SmallStepButton(text = "+1m", onClick = {
                    val cal = Calendar.getInstance().apply {
                        set(gregYear, gregMonth - 1, gregDay)
                        add(Calendar.MONTH, 1)
                    }
                    onYearChange(cal.get(Calendar.YEAR))
                    onMonthChange(cal.get(Calendar.MONTH) + 1)
                    onDayChange(cal.get(Calendar.DAY_OF_MONTH))
                }, tag = "btn_greg_next_month")
            }

            TextButton(
                onClick = onResetToday,
                modifier = Modifier.testTag("btn_greg_jump_today")
            ) {
                Icon(
                    imageVector = Icons.Default.Today,
                    contentDescription = null,
                    tint = DeepSkyBlue,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = t("jump_to_today"),
                    fontSize = 11.sp,
                    color = DeepSkyBlue,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Conversion Result Display Card
        val hijriResultFormatted = when (appLang) {
            "AR" -> convertedHijri.formatAr()
            "FR" -> convertedHijri.formatFr()
            else -> convertedHijri.formatEn()
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("result_greg_to_hijri_card"),
            colors = CardDefaults.cardColors(
                containerColor = if (isIslamicArt) Color(0xFF2C1D10) else Color(0xFF1E2248)
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (isIslamicArt) GoldenSun.copy(alpha = 0.5f) else Color(0xFF373B6F))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = t("islamic_date"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isIslamicArt) GoldenSun else DeepSkyBlue
                    )

                    // Copy button
                    IconButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Hijri Date", hijriResultFormatted)
                            cm.setPrimaryClip(clip)
                            Toast.makeText(context, t("date_copied"), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("btn_copy_hijri_result")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy result",
                            tint = GoldenSun,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = hijriResultFormatted,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = GoldenSun
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Occasion badge or standard day label
                if (occasion != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GreenEmerald.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, GreenEmerald.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = GreenEmerald,
                            modifier = Modifier.size(14.dp)
                        )
                        Column {
                            Text(
                                text = if (appLang == "AR") occasion.titleAr else occasion.titleEn,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenEmerald
                            )
                            Text(
                                text = if (appLang == "AR") occasion.descAr else occasion.descEn,
                                fontSize = 10.sp,
                                color = CelestialGrey
                            )
                        }
                    }
                } else {
                    Text(
                        text = t("no_occasion_on_date"),
                        fontSize = 11.sp,
                        color = CelestialGrey
                    )
                }
            }
        }
    }
}

/**
 * Hijri -> Gregorian Conversion UI Section
 */
@Composable
private fun HijriToGregorianSection(
    hijriYear: Int,
    onYearChange: (Int) -> Unit,
    hijriMonth: Int,
    onMonthChange: (Int) -> Unit,
    hijriDay: Int,
    onDayChange: (Int) -> Unit,
    onResetToday: () -> Unit,
    appLang: String,
    isIslamicArt: Boolean,
    t: (String) -> String
) {
    val context = LocalContext.current

    // Convert to Gregorian
    val convertedGreg = remember(hijriYear, hijriMonth, hijriDay) {
        HijriCalendarHelper.convertHijriToGregorian(hijriYear, hijriMonth, hijriDay)
    }

    // Occasion on that Hijri date
    val occasion = remember(hijriMonth, hijriDay) {
        HijriCalendarHelper.occurrences.find {
            it.hijriMonth == hijriMonth && it.hijriDay == hijriDay
        }
    }

    val daysInThisHijriMonth = remember(hijriYear, hijriMonth) {
        HijriCalendarHelper.getDaysInHijriMonth(hijriYear, hijriMonth)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = t("select_hijri_date"),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = GoldenSun
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Date Pickers Row (Day, Month, Year)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Day selector (1 to 29 or 30)
            DateNumberPicker(
                label = t("day"),
                value = hijriDay.coerceIn(1, daysInThisHijriMonth),
                range = 1..daysInThisHijriMonth,
                onValueChange = onDayChange,
                modifier = Modifier.weight(1f),
                testTagPrefix = "hijri_day"
            )

            // Month selector (1 to 12)
            DateMonthPicker(
                label = t("month"),
                selectedMonth = hijriMonth,
                monthNames = if (appLang == "AR") HijriCalendarHelper.arabicMonthNames else HijriCalendarHelper.englishMonthNames,
                onMonthChange = onMonthChange,
                modifier = Modifier.weight(1.5f),
                testTagPrefix = "hijri_month"
            )

            // Year selector (1300 to 1500 AH)
            DateNumberPicker(
                label = t("year"),
                value = hijriYear,
                range = 1300..1500,
                onValueChange = onYearChange,
                modifier = Modifier.weight(1.3f),
                testTagPrefix = "hijri_year"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Steppers & Jump to Today Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SmallStepButton(text = "-1d", onClick = {
                    if (hijriDay > 1) {
                        onDayChange(hijriDay - 1)
                    } else {
                        val prevMonth = if (hijriMonth == 1) 12 else hijriMonth - 1
                        val prevYear = if (hijriMonth == 1) hijriYear - 1 else hijriYear
                        val days = HijriCalendarHelper.getDaysInHijriMonth(prevYear, prevMonth)
                        onYearChange(prevYear)
                        onMonthChange(prevMonth)
                        onDayChange(days)
                    }
                }, tag = "btn_hijri_prev_day")

                SmallStepButton(text = "+1d", onClick = {
                    if (hijriDay < daysInThisHijriMonth) {
                        onDayChange(hijriDay + 1)
                    } else {
                        val nextMonth = if (hijriMonth == 12) 1 else hijriMonth + 1
                        val nextYear = if (hijriMonth == 12) hijriYear + 1 else hijriYear
                        onYearChange(nextYear)
                        onMonthChange(nextMonth)
                        onDayChange(1)
                    }
                }, tag = "btn_hijri_next_day")

                SmallStepButton(text = "-1m", onClick = {
                    if (hijriMonth == 1) {
                        onYearChange(hijriYear - 1)
                        onMonthChange(12)
                    } else {
                        onMonthChange(hijriMonth - 1)
                    }
                }, tag = "btn_hijri_prev_month")

                SmallStepButton(text = "+1m", onClick = {
                    if (hijriMonth == 12) {
                        onYearChange(hijriYear + 1)
                        onMonthChange(1)
                    } else {
                        onMonthChange(hijriMonth + 1)
                    }
                }, tag = "btn_hijri_next_month")
            }

            TextButton(
                onClick = onResetToday,
                modifier = Modifier.testTag("btn_hijri_jump_today")
            ) {
                Icon(
                    imageVector = Icons.Default.Today,
                    contentDescription = null,
                    tint = DeepSkyBlue,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = t("jump_to_today"),
                    fontSize = 11.sp,
                    color = DeepSkyBlue,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Conversion Result Display Card
        val gregResultFormatted = when (appLang) {
            "AR" -> convertedGreg.formatAr()
            "FR" -> convertedGreg.formatFr()
            else -> convertedGreg.formatEn()
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("result_hijri_to_greg_card"),
            colors = CardDefaults.cardColors(
                containerColor = if (isIslamicArt) Color(0xFF2C1D10) else Color(0xFF1E2248)
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (isIslamicArt) GoldenSun.copy(alpha = 0.5f) else Color(0xFF373B6F))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = t("gregorian_date"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isIslamicArt) GoldenSun else DeepSkyBlue
                    )

                    // Copy button
                    IconButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Gregorian Date", gregResultFormatted)
                            cm.setPrimaryClip(clip)
                            Toast.makeText(context, t("date_copied"), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("btn_copy_greg_result")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy result",
                            tint = GoldenSun,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = gregResultFormatted,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = PureDawnLight
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Occasion badge
                if (occasion != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GoldenSun.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, GoldenSun.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = GoldenSun,
                            modifier = Modifier.size(14.dp)
                        )
                        Column {
                            Text(
                                text = if (appLang == "AR") occasion.titleAr else occasion.titleEn,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldenSun
                            )
                            Text(
                                text = if (appLang == "AR") occasion.descAr else occasion.descEn,
                                fontSize = 10.sp,
                                color = CelestialGrey
                            )
                        }
                    }
                } else {
                    Text(
                        text = t("no_occasion_on_date"),
                        fontSize = 11.sp,
                        color = CelestialGrey
                    )
                }
            }
        }
    }
}

/**
 * Reusable Number Picker for Day/Year with +/- quick step buttons
 */
@Composable
private fun DateNumberPicker(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "picker"
) {
    Column(
        modifier = modifier
            .background(Color(0xFF22244B), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF33366A), RoundedCornerShape(12.dp))
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = CelestialGrey
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = { if (value > range.first) onValueChange(value - 1) },
                modifier = Modifier
                    .size(24.dp)
                    .testTag("${testTagPrefix}_dec")
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Decrement",
                    tint = PureDawnLight,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = value.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PureDawnLight,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .testTag("${testTagPrefix}_val")
            )

            IconButton(
                onClick = { if (value < range.last) onValueChange(value + 1) },
                modifier = Modifier
                    .size(24.dp)
                    .testTag("${testTagPrefix}_inc")
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Increment",
                    tint = PureDawnLight,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Reusable Dropdown Picker for Months with localized names
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateMonthPicker(
    label: String,
    selectedMonth: Int,
    monthNames: List<String>,
    onMonthChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "month_picker"
) {
    var expanded by remember { mutableStateOf(false) }
    val currentMonthName = monthNames.getOrElse(selectedMonth - 1) { "$selectedMonth" }

    Column(
        modifier = modifier
            .background(Color(0xFF22244B), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF33366A), RoundedCornerShape(12.dp))
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = CelestialGrey
        )
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = {
                    val prev = if (selectedMonth == 1) 12 else selectedMonth - 1
                    onMonthChange(prev)
                },
                modifier = Modifier
                    .size(24.dp)
                    .testTag("${testTagPrefix}_dec")
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous Month",
                    tint = PureDawnLight,
                    modifier = Modifier.size(16.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { expanded = true }
                    .padding(horizontal = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentMonthName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldenSun,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("${testTagPrefix}_val")
                )

                androidx.compose.material3.DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(Color(0xFF1B1D3D))
                ) {
                    monthNames.forEachIndexed { index, name ->
                        val mNum = index + 1
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "$mNum. $name",
                                    color = if (mNum == selectedMonth) GoldenSun else PureDawnLight,
                                    fontWeight = if (mNum == selectedMonth) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                onMonthChange(mNum)
                                expanded = false
                            }
                        )
                    }
                }
            }

            IconButton(
                onClick = {
                    val next = if (selectedMonth == 12) 1 else selectedMonth + 1
                    onMonthChange(next)
                },
                modifier = Modifier
                    .size(24.dp)
                    .testTag("${testTagPrefix}_inc")
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next Month",
                    tint = PureDawnLight,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Small quick-step button pill
 */
@Composable
private fun SmallStepButton(
    text: String,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF26284E),
        border = BorderStroke(0.5.dp, Color(0xFF393D78)),
        modifier = Modifier.testTag(tag)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = PureDawnLight,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
