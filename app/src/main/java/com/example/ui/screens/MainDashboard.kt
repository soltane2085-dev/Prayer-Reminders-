package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.ClipboardManager
import android.content.ClipData
import android.os.Build
import android.widget.Toast
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import java.util.Calendar
import java.util.Locale
import java.text.SimpleDateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.example.calculator.QiblaHelper
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.example.calculator.AsrJuristicMethod
import com.example.calculator.CalculationMethod
import com.example.calculator.CityPreset
import com.example.calculator.HijriCalendarHelper
import com.example.calculator.HijriDate
import com.example.data.PrayerRecord
import com.example.data.SavedQuote
import com.example.data.StaticQuote
import com.example.viewmodel.PrayerViewModel
import com.example.ui.translation.Translation
import com.example.ui.theme.*
import com.example.ui.components.HijriDateConverterDialog
import com.example.ui.components.HijriDateConverterCardContent
import androidx.compose.ui.geometry.center
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainDashboard(
    viewModel: PrayerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val appLang by viewModel.appLang.collectAsStateWithLifecycle()
    val t = { key: String -> Translation.get(key, appLang) }

    // Request POST_NOTIFICATIONS permission for Android 13+
    var hasNotifyPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotifyPermission = isGranted
        if (!isGranted) {
            val permMsg = if (appLang == "AR") "لن تظهر تنبيهات الأذان بدون منح الإذن."
            else if (appLang == "FR") "Les alertes de l'Adhan ne s'afficheront pas sans autorisation."
            else "Notification alerts won't display without permissions."
            Toast.makeText(context, permMsg, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            hasNotifyPermission = true
        }
    }

    val showMosqueLocator by viewModel.showMosqueLocator.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(TwilightBackground),
        bottomBar = {
            NavigationBar(
                containerColor = TwilightBackground,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.AccessTime, contentDescription = "Pray List") },
                    label = { Text(t("tab_times"), fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TwilightBackground,
                        selectedTextColor = GoldenSun,
                        indicatorColor = GoldenSun,
                        unselectedIconColor = CelestialGrey,
                        unselectedTextColor = CelestialGrey
                    ),
                    modifier = Modifier.testTag("nav_tab_times")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Book, contentDescription = "Quran and Azkar") },
                    label = { Text(t("tab_quran"), fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TwilightBackground,
                        selectedTextColor = GoldenSun,
                        indicatorColor = GoldenSun,
                        unselectedIconColor = CelestialGrey,
                        unselectedTextColor = CelestialGrey
                    ),
                    modifier = Modifier.testTag("nav_tab_quran_azkar")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Explore, contentDescription = "Qibla Compass") },
                    label = { Text(t("tab_qibla"), fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TwilightBackground,
                        selectedTextColor = GoldenSun,
                        indicatorColor = GoldenSun,
                        unselectedIconColor = CelestialGrey,
                        unselectedTextColor = CelestialGrey
                    ),
                    modifier = Modifier.testTag("nav_tab_qibla")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Daily Reflections") },
                    label = { Text(t("tab_reflections"), fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TwilightBackground,
                        selectedTextColor = GoldenSun,
                        indicatorColor = GoldenSun,
                        unselectedIconColor = CelestialGrey,
                        unselectedTextColor = CelestialGrey
                    ),
                    modifier = Modifier.testTag("nav_tab_quotes")
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings Parameters") },
                    label = { Text(t("tab_configure"), fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TwilightBackground,
                        selectedTextColor = GoldenSun,
                        indicatorColor = GoldenSun,
                        unselectedIconColor = CelestialGrey,
                        unselectedTextColor = CelestialGrey
                    ),
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        val isIslamicArt = LocalIsIslamicArtTheme.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                    )
                )
        ) {
            if (isIslamicArt) {
                IslamicGeometricBackground(
                    patternColor = MaterialTheme.colorScheme.primary,
                    patternAlpha = 0.08f,
                    gridSizeDp = 48.dp
                )
            }
            Box(modifier = Modifier.padding(innerPadding)) {
                when (selectedTab) {
                    0 -> TimesTab(viewModel = viewModel, context = context)
                    1 -> QuranAzkarTab(viewModel = viewModel, context = context)
                    2 -> QiblaCompassScreen(viewModel = viewModel, context = context)
                    3 -> ReflectionsTab(viewModel = viewModel, context = context)
                    4 -> ConfigureTab(viewModel = viewModel, context = context)
                }
            }

            val showDateConverterDialog by viewModel.showDateConverterDialog.collectAsStateWithLifecycle()
            if (showDateConverterDialog) {
                HijriDateConverterDialog(
                    viewModel = viewModel,
                    onDismiss = { viewModel.setShowDateConverterDialog(false) }
                )
            }
        }
    }

        if (showMosqueLocator) {
            MosqueLocatorScreen(
                viewModel = viewModel,
                onClose = { viewModel.setShowMosqueLocator(false) }
            )
        }
    }
}

@Composable
fun TimesTab(viewModel: PrayerViewModel, context: Context) {
    val appLang by viewModel.appLang.collectAsStateWithLifecycle()
    val t = { key: String -> Translation.get(key, appLang) }
    val isIslamicArt = LocalIsIslamicArtTheme.current
    val currentThemeId by viewModel.themeMode.collectAsStateWithLifecycle()

    val prayerTimes by viewModel.prayerTimes.collectAsStateWithLifecycle()
    val nextName by viewModel.nextPrayerName.collectAsStateWithLifecycle()
    val nextCountdown by viewModel.nextPrayerCountdown.collectAsStateWithLifecycle()
    val todayRecord by viewModel.todayRecord.collectAsStateWithLifecycle()
    val cityIdx by viewModel.cityPresetIdx.collectAsStateWithLifecycle()
    val customEnabled by viewModel.customLocEnabled.collectAsStateWithLifecycle()
    val customName by viewModel.customCityName.collectAsStateWithLifecycle()
    val todayQuote by viewModel.todayQuote.collectAsStateWithLifecycle()
    val dailyQuote by viewModel.dailyQuote.collectAsStateWithLifecycle()
    val isDailyQuoteLoading by viewModel.isDailyQuoteLoading.collectAsStateWithLifecycle()
    val isQuoteSaved by viewModel.isDailyQuoteSaved.collectAsStateWithLifecycle()

    val notifyFajr by viewModel.notifyFajr.collectAsStateWithLifecycle()
    val notifyDhuhr by viewModel.notifyDhuhr.collectAsStateWithLifecycle()
    val notifyAsr by viewModel.notifyAsr.collectAsStateWithLifecycle()
    val notifyMaghrib by viewModel.notifyMaghrib.collectAsStateWithLifecycle()
    val notifyIsha by viewModel.notifyIsha.collectAsStateWithLifecycle()

    val mosqueSearchQuery by viewModel.mosqueSearchQuery.collectAsStateWithLifecycle()
    val mosqueSearchResult by viewModel.mosqueSearchResult.collectAsStateWithLifecycle()
    val mosqueIsSearching by viewModel.mosqueIsSearching.collectAsStateWithLifecycle()
    val mosqueSearchErrorMsg by viewModel.mosqueSearchErrorMsg.collectAsStateWithLifecycle()

    val linkedMosqueEnabled by viewModel.linkedMosqueEnabled.collectAsStateWithLifecycle()
    val linkedMosqueName by viewModel.linkedMosqueName.collectAsStateWithLifecycle()
    val linkedMosqueAddress by viewModel.linkedMosqueAddress.collectAsStateWithLifecycle()
    val linkedMosqueSource by viewModel.linkedMosqueSource.collectAsStateWithLifecycle()

    val iqamahOffsetFajr by viewModel.iqamahOffsetFajr.collectAsStateWithLifecycle()
    val iqamahOffsetDhuhr by viewModel.iqamahOffsetDhuhr.collectAsStateWithLifecycle()
    val iqamahOffsetAsr by viewModel.iqamahOffsetAsr.collectAsStateWithLifecycle()
    val iqamahOffsetMaghrib by viewModel.iqamahOffsetMaghrib.collectAsStateWithLifecycle()
    val iqamahOffsetIsha by viewModel.iqamahOffsetIsha.collectAsStateWithLifecycle()

    val iqamahTimes by viewModel.iqamahTimes.collectAsStateWithLifecycle()

    val displayCity = if (customEnabled) customName else CityPreset.presets[cityIdx].cityName

    val showAboutDialogState = viewModel.showAboutDialog.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // App Custom Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(
                    1.dp,
                    if (isIslamicArt) Color(0xFFD4AF37).copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = t("peaceful_pray"),
                            fontSize = 12.sp,
                            color = DeepSkyBlue,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = displayCity,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Quick Theme Toggle Pill
                    QuickThemeTogglePill(
                        currentThemeId = currentThemeId,
                        onThemeSelect = { viewModel.updateThemeMode(it) },
                        appLang = appLang,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isIslamicArt) {
                            RubElHizbStarIcon(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeColor = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageIconsForNextPrayer(nextName),
                                contentDescription = "Prayer state icon",
                                tint = GoldenSun,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        // Live Countdown visualization Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TwilightCard),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF2A2E55))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (nextName.isNotEmpty()) {
                                val translatedNextName = t(nextName)
                                t("next_prayer").replace("{name}", translatedNextName).uppercase()
                            } else {
                                t("calculating").uppercase()
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.6.sp,
                            color = CelestialGrey
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // High fidelity dynamic time text
                        Text(
                            text = nextCountdown,
                            fontSize = 42.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            color = GoldenSun,
                            letterSpacing = 2.sp,
                            modifier = Modifier.testTag("id_countdown_timer")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = t("stay_centered"),
                            fontSize = 13.sp,
                            color = CelestialGrey,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // --- Hijri and Gregorian Calendar Section ---
        item {
            HijriCalendarCard(viewModel = viewModel)
        }

        // --- Daily Prayer Self-Tracking Card ---
        item {
            PrayerTrackerCard(viewModel = viewModel)
        }

        // Checklist Checklist + timing headers
        item {
            Text(
                text = t("todays_prayers").uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = CelestialGrey,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )
        }

        // List of the five prayers with alert toggles and completion checkmarks
        val prayerListKeys = listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha")
        items(prayerListKeys) { prayer ->
            val time = prayerTimes[prayer] ?: "--:--"
            val alertEnabled = when (prayer) {
                "Fajr" -> notifyFajr
                "Dhuhr" -> notifyDhuhr
                "Asr" -> notifyAsr
                "Maghrib" -> notifyMaghrib
                "Isha" -> notifyIsha
                else -> false
            }

            val isChecked = when (prayer) {
                "Fajr" -> todayRecord.fajrPrayed
                "Dhuhr" -> todayRecord.dhuhrPrayed
                "Asr" -> todayRecord.asrPrayed
                "Maghrib" -> todayRecord.maghribPrayed
                "Isha" -> todayRecord.ishaPrayed
                else -> false
            }

            val hasAlertSupport = prayer != "Sunrise"
            val hasCheckSupport = prayer != "Sunrise"

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("prayer_item_$prayer"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isChecked) Color(0xFF13202E) else TwilightCard
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp,
                    if (isChecked) Color(0xFF10B981).copy(alpha = 0.4f) else Color(0xFF232549)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Small aesthetic indicator representing the solar angle
                    Icon(
                        imageVector = imageIconsForNextPrayer(prayer),
                        contentDescription = "$prayer icon",
                        tint = if (isChecked) GreenEmerald else DeepSkyBlue,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Title and time
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = t(prayer),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureDawnLight
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = time,
                                fontSize = 14.sp,
                                color = if (isChecked) GreenEmerald else CelestialGrey,
                                fontWeight = FontWeight.Medium
                            )
                            val iqamahTime = iqamahTimes[prayer]
                            if (iqamahTime != null) {
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF3B2F1D), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = GoldenSun,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = "${t("iqamah")}: $iqamahTime",
                                            fontSize = 10.sp,
                                            color = GoldenSun,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Interactive Alarm Toggle
                    if (hasAlertSupport) {
                        IconButton(
                            onClick = {
                                viewModel.toggleNotification(prayer)
                                val statusMsg = if (!alertEnabled) {
                                    if (appLang == "EN") "Reminders enabled for ${t(prayer)}"
                                    else if (appLang == "FR") "Rappels activés pour ${t(prayer)}"
                                    else "تم تفعيل التنبيه لـ ${t(prayer)}"
                                } else {
                                    if (appLang == "EN") "Reminders muted for ${t(prayer)}"
                                    else if (appLang == "FR") "Rappels désactivés pour ${t(prayer)}"
                                    else "تم كتم التنبيه لـ ${t(prayer)}"
                                }
                                Toast.makeText(
                                    context,
                                    statusMsg,
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("toggle_notify_$prayer")
                        ) {
                            Icon(
                                imageVector = if (alertEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                contentDescription = "Toggle alert for $prayer",
                                tint = if (alertEnabled) GoldenSun else CelestialGrey.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Habit Checklist Checkbox
                    if (hasCheckSupport) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { viewModel.togglePrayerChecked(prayer) },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("check_habit_$prayer")
                        ) {
                            Icon(
                                imageVector = if (isChecked) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                                contentDescription = "Mark $prayer as prayed",
                                tint = if (isChecked) GreenEmerald else CelestialGrey.copy(alpha = 0.35f),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- Linked Mosque Platform Option ---
        item {
            Text(
                text = t("mosque_title").uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = CelestialGrey,
                modifier = Modifier.padding(start = 4.dp, top = 20.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("linked_mosque_card"),
                colors = CardDefaults.cardColors(containerColor = TwilightCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF25274A))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    if (!linkedMosqueEnabled) {
                        // Empty / Unlinked state
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFF1E293B), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = "Mosque Location",
                                    tint = DeepSkyBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = t("mosque_title"),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureDawnLight
                                )
                                Text(
                                    text = t("mosque_desc"),
                                    fontSize = 11.sp,
                                    color = CelestialGrey
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { viewModel.setShowMosqueLocator(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenEmerald),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("btn_open_mosque_map"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                tint = PureDawnLight,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = t("open_mosque_locator"),
                                color = PureDawnLight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Search Bar
                        OutlinedTextField(
                            value = mosqueSearchQuery,
                            onValueChange = { viewModel.updateMosqueSearchQuery(it) },
                            placeholder = { Text(t("search_mosque_placeholder"), color = CelestialGrey, fontSize = 12.sp) },
                            label = { Text(t("search"), color = DeepSkyBlue, fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("mosque_search_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DeepSkyBlue,
                                unfocusedBorderColor = Color(0xFF282A4C),
                                focusedLabelColor = DeepSkyBlue,
                                cursorColor = GoldenSun,
                                focusedTextColor = PureDawnLight,
                                unfocusedTextColor = PureDawnLight
                             ),
                            trailingIcon = {
                                if (mosqueSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.clearMosqueSearch() }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = CelestialGrey, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                imeAction = androidx.compose.ui.text.input.ImeAction.Search
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onSearch = { viewModel.searchMosque() }
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { viewModel.searchMosque() },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepSkyBlue),
                            modifier = Modifier.fillMaxWidth().height(42.dp).testTag("btn_search_mosque"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (mosqueIsSearching) {
                                CircularProgressIndicator(color = TwilightBackground, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = TwilightBackground, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(t("search"), color = TwilightBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (mosqueSearchErrorMsg.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = mosqueSearchErrorMsg,
                                color = CoralAlert,
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Showcase search results
                        val result = mosqueSearchResult
                        if (result != null && result.success) {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = Color(0xFF282A4C))
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(t("found_mosques"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldenSun)
                            Spacer(modifier = Modifier.height(8.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1F3C)),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF2A2E55))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(result.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PureDawnLight)
                                    Text(result.address, fontSize = 11.sp, color = CelestialGrey)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "${t("iqamah")} (Default):",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DeepSkyBlue
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${t("Fajr")}: ${result.fajrOffset} ${t("mins")} • ${t("Dhuhr")}: ${result.dhuhrOffset} ${t("mins")} • ${t("Asr")}: ${result.asrOffset} ${t("mins")} • ${t("Maghrib")}: ${result.maghribOffset} ${t("mins")} • ${t("Isha")}: ${result.ishaOffset} ${t("mins")}",
                                        fontSize = 11.sp,
                                        color = PureDawnLight
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            viewModel.linkMosque(result)
                                            Toast.makeText(context, t("linked_mosque_success").replace("{name}", result.name), Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GreenEmerald),
                                        modifier = Modifier.fillMaxWidth().height(36.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(t("link_mosque"), color = PureDawnLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        // Persisted Linked State
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFF13202E), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Linked Mosque Connected",
                                    tint = GreenEmerald,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = linkedMosqueName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PureDawnLight,
                                        modifier = Modifier.weight(1f, fill = false),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF1E3A8A), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(linkedMosqueSource, fontSize = 8.sp, color = DeepSkyBlue, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(
                                    text = linkedMosqueAddress,
                                    fontSize = 11.sp,
                                    color = CelestialGrey,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFF282A4C))
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "${t("iqamah")} (Minutes):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldenSun
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val prayers = listOf(
                            Triple("Fajr", t("Fajr"), iqamahOffsetFajr),
                            Triple("Dhuhr", t("Dhuhr"), iqamahOffsetDhuhr),
                            Triple("Asr", t("Asr"), iqamahOffsetAsr),
                            Triple("Maghrib", t("Maghrib"), iqamahOffsetMaghrib),
                            Triple("Isha", t("Isha"), iqamahOffsetIsha)
                        )

                        prayers.forEach { (key, labelName, offsetVal) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(labelName, fontSize = 12.sp, color = PureDawnLight, modifier = Modifier.weight(1f))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.updateIqamahOffset(key, offsetVal - 5) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF282A4C)),
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.size(28.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("-", color = PureDawnLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = "$offsetVal ${t("mins")}",
                                        fontSize = 12.sp,
                                        color = GoldenSun,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.widthIn(min = 50.dp),
                                        textAlign = TextAlign.Center
                                    )
                                    Button(
                                        onClick = { viewModel.updateIqamahOffset(key, offsetVal + 5) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF282A4C)),
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.size(28.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("+", color = PureDawnLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.unlinkMosque()
                                Toast.makeText(context, t("unlink_mosque"), Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CoralAlert.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, CoralAlert)
                        ) {
                            Text(t("unlink_mosque"), color = CoralAlert, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Inspirational quote section Card (Room-cached & Gemini API powered)
        item {
            val activeQuoteText = dailyQuote?.arabicText ?: todayQuote.text
            val activeQuoteSource = dailyQuote?.source ?: todayQuote.author
            val activeTranslation = dailyQuote?.translation.orEmpty()
            val activeReflection = dailyQuote?.reflection.orEmpty()
            val isAiPowered = dailyQuote?.isAiGenerated == true

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = (if (appLang == "AR") {
                        com.example.data.QuoteProvider.getQuoteTitle(activeQuoteText, activeQuoteSource, appLang)
                    } else {
                        t("today_spirit")
                    }).uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = CelestialGrey
                )

                Surface(
                    color = if (isAiPowered) GoldenSun.copy(alpha = 0.14f) else DeepSkyBlue.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isAiPowered) {
                            if (appLang == "AR") "حكمة اليوم • Gemini" else if (appLang == "FR") "Citation du jour • Gemini" else "Daily Quote • Gemini"
                        } else {
                            if (appLang == "AR") "محفوظة محلياً" else if (appLang == "FR") "Enregistrée localement" else "Saved Locally"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isAiPowered) GoldenSun else DeepSkyBlue,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        item {
            val activeQuoteText = dailyQuote?.arabicText ?: todayQuote.text
            val activeQuoteSource = dailyQuote?.source ?: todayQuote.author
            val activeTranslation = dailyQuote?.translation.orEmpty()
            val activeReflection = dailyQuote?.reflection.orEmpty()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("daily_quote_card"),
                colors = CardDefaults.cardColors(containerColor = TwilightCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF25274A))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatQuote,
                        contentDescription = "Quote Mark",
                        tint = DeepSkyBlue,
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = activeQuoteText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = PureDawnLight,
                        lineHeight = 24.sp
                    )

                    if (appLang != "AR" && activeTranslation.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = activeTranslation,
                            fontSize = 13.sp,
                            color = CelestialGrey,
                            lineHeight = 19.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }

                    if (activeReflection.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = Color(0xFF1A1D3A),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = activeReflection,
                                fontSize = 12.sp,
                                color = PureDawnLight.copy(alpha = 0.85f),
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "— $activeQuoteSource",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldenSun,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(color = Color(0xFF25274A))

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.rollNewTodayQuote() },
                            enabled = !isDailyQuoteLoading,
                            modifier = Modifier.testTag("btn_refresh_daily_quote")
                        ) {
                            if (isDailyQuoteLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = GoldenSun
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Roll new quote",
                                    tint = CelestialGrey
                                )
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.toggleFavoriteDailyQuote()
                                val itemType = com.example.data.QuoteProvider.getQuoteLabel(activeQuoteText, activeQuoteSource, appLang)
                                val qMsg = if (!isQuoteSaved) {
                                    if (appLang == "EN") "Added reflection to Favorites Starred List!"
                                    else if (appLang == "FR") "Réflexion ajoutée aux Favoris !"
                                    else "تم حفظ $itemType في المفضلة"
                                } else {
                                    if (appLang == "EN") "Removed reflection from Starred List."
                                    else if (appLang == "FR") "Réflexion retirée des Favoris."
                                    else "تم إزالة $itemType من المفضلة"
                                }
                                Toast.makeText(
                                    context,
                                    qMsg,
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("btn_favorite_daily_quote")
                        ) {
                            Icon(
                                imageVector = if (isQuoteSaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite this quote",
                                tint = if (isQuoteSaved) CoralAlert else CelestialGrey,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isQuoteSaved) t("fav_quotes") else t("save_quote"),
                                color = if (isQuoteSaved) CoralAlert else CelestialGrey,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        item {
            if (showAboutDialogState.value) {
                AlertDialog(
                    onDismissRequest = { viewModel.setShowAboutDialog(false) },
                    containerColor = Color(0xFF13152F),
                    shape = RoundedCornerShape(24.dp),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = GoldenSun,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = if (appLang == "AR") "معلومات التطبيق" else if (appLang == "FR") "Détails de l'app" else "About Application",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureDawnLight
                            )
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (appLang == "AR") "آذان ومواقيت الصلاة" else if (appLang == "FR") "Rappels de Prières" else "Prayer Reminders",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = PureDawnLight
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (appLang == "AR") {
                                    "برنامج إسلامي متكامل لمتابعة مواقيت الصلوات بدقة عالية من MAWAQIT، وقراءة كتاب الله والأذكار وحفظ مواضيع التدبر الإيماني والدعاء عبر مساعد الذكاء الاصطناعي Gemini AI وصياغة أحاديث وحكم ملهمة."
                                } else if (appLang == "FR") {
                                    "Compagnon spirituel complet pour les horaires de prière (MAWAQIT), la lecture du Saint Coran, des Azkars et les réflexions spirituelles via Gemini AI."
                                } else {
                                    "An elegant prayer companion with accurate Adhan times from MAWAQIT, Quran & Azkar readings, Qibla compass, and live AI reflections via Gemini AI."
                                },
                                fontSize = 12.sp,
                                color = CelestialGrey,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = Color(0xFF282A4C), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "SOLTANE Mourad by AI Creative",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldenSun,
                                textAlign = TextAlign.Center,
                                letterSpacing = 0.5.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Version 1.2.0 • Created with Serenity",
                                fontSize = 10.sp,
                                color = CelestialGrey.copy(alpha = 0.5f)
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { viewModel.setShowAboutDialog(false) }
                        ) {
                            Text(
                                text = if (appLang == "AR") "إغلاق" else if (appLang == "FR") "Fermer" else "Close",
                                color = GoldenSun,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ReflectionsTab(viewModel: PrayerViewModel, context: Context) {
    val appLang by viewModel.appLang.collectAsStateWithLifecycle()
    val t = { key: String -> Translation.get(key, appLang) }

    val aiGenerating by viewModel.aiIsGenerating.collectAsStateWithLifecycle()
    val aiErrorMsg by viewModel.aiErrorMsg.collectAsStateWithLifecycle()
    val generatedQuote by viewModel.generatedQuote.collectAsStateWithLifecycle()
    val isGenSaved by viewModel.isGeneratedQuoteSaved.collectAsStateWithLifecycle()
    val topicInput by viewModel.aiTopicInput.collectAsStateWithLifecycle()
    val savedQuotes by viewModel.savedQuotes.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // Topic search Input card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TwilightCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF282A4C))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = (if (appLang == "AR") "مستشار الحكم والأحاديث الذكي" else if (appLang == "FR") "Conseiller Spirituel IA" else "AI Spiritual Counselor").uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = DeepSkyBlue
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (appLang == "AR") "اطلب من Gemini صياغة حكم وأحاديث ملهمة حول موضوع معين (مثل: الصبر، الإحسان، التوكل)." else if (appLang == "FR") "Demandez à Gemini de rédiger des réflexions sereines sur un sujet (ex: patience, bonté, foi)." else "Ask Gemini to compose custom serene reflections on a topic (e.g. patience, kindness, faith).",
                        fontSize = 13.sp,
                        color = CelestialGrey,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = topicInput,
                        onValueChange = { viewModel.updateTopicInput(it) },
                        placeholder = { Text(if (appLang == "AR") "اكتب موضوعاً (مثل: الأمل في الشدائد)" else if (appLang == "FR") "Entrez un sujet (ex: l'espoir dans l'épreuve)" else "Enter a topic (e.g. hope in hardship)", color = CelestialGrey) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_topic_field"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldenSun,
                            unfocusedBorderColor = Color(0xFF282A4C),
                            focusedContainerColor = Color(0xFF13152C),
                            unfocusedContainerColor = Color(0xFF13152C),
                            focusedTextColor = PureDawnLight,
                            unfocusedTextColor = PureDawnLight
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.generateAIQuote() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_trigger_ai"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldenSun),
                        enabled = !aiGenerating
                    ) {
                        if (aiGenerating) {
                            CircularProgressIndicator(
                                color = TwilightBackground,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (appLang == "AR") "جاري جلب الفائدة الروحية..." else if (appLang == "FR") "Formulation en cours..." else "Formulating Serenity...", color = TwilightBackground, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Gemini AI Magic")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (appLang == "AR") "توليد حكمة/حديث عبر Gemini" else if (appLang == "FR") "Générer une réflexion Gemini" else "Generate Gemini Reflection", color = TwilightBackground, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (aiErrorMsg.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = aiErrorMsg,
                            color = CoralAlert,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 16.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Output of AI Generator Card
        if (generatedQuote != null || aiGenerating) {
            item {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + expandVertically()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2349)),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, GoldenSun.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = "Gemini output icon",
                                    tint = GoldenSun,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                generatedQuote?.let { quote ->
                                    val itemLabel = com.example.data.QuoteProvider.getQuoteLabel(quote.text, quote.author, appLang)
                                    Text(
                                        text = if (appLang == "AR") "$itemLabel ذكي مباشر" else if (appLang == "FR") "RÉFLEXION IA EN DIRECT" else "LIVE AI REFLECTION",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.5.sp,
                                        color = GoldenSun
                                    )
                                } ?: Text(
                                    text = if (appLang == "AR") "جاري التدبر..." else if (appLang == "FR") "RÉFLEXION IA EN DIRECT" else "LIVE AI REFLECTION",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp,
                                    color = GoldenSun
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (aiGenerating) {
                                LinearProgressIndicator(
                                    color = GoldenSun,
                                    trackColor = TwilightBackground,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                generatedQuote?.let { quote ->
                                    Text(
                                        text = quote.text,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = PureDawnLight,
                                        lineHeight = 22.sp,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "— ${quote.author}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldenSun,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            viewModel.toggleFavoriteQuote(quote)
                                            val itemType = com.example.data.QuoteProvider.getQuoteLabel(quote.text, quote.author, appLang)
                                            val qMsg = if (!isGenSaved) {
                                                if (appLang == "EN") "Reflection added to Starred library!"
                                                else if (appLang == "FR") "Réflexion ajoutée à la bibliothèque !"
                                                else "تم حفظ $itemType في المكتبة المفضلة"
                                            } else {
                                                if (appLang == "EN") "Removed from Starred library."
                                                else if (appLang == "FR") "Retiré de la bibliothèque."
                                                else "تم إزالة $itemType من المكتبة"
                                            }
                                            Toast.makeText(
                                                context,
                                                qMsg,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (isGenSaved) Color.Transparent else GoldenSun),
                                        modifier = Modifier.align(Alignment.End),
                                        shape = RoundedCornerShape(10.dp),
                                        border = if (isGenSaved) BorderStroke(1.dp, CelestialGrey) else null
                                    ) {
                                        Icon(
                                            imageVector = if (isGenSaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                            contentDescription = "Save quote to library",
                                            tint = if (isGenSaved) CoralAlert else TwilightBackground,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isGenSaved) (if (appLang == "AR") "تم الحفظ" else if (appLang == "FR") "Enregistré" else "Saved") else t("save_quote"),
                                            color = if (isGenSaved) PureDawnLight else TwilightBackground,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Saved Starred custom collection headers
        item {
            Text(
                text = t("fav_quotes").uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = CelestialGrey,
                modifier = Modifier.padding(start = 4.dp, top = 16.dp)
            )
        }

        if (savedQuotes.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TwilightCard),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF232549))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.FavoriteBorder,
                            contentDescription = "Empty collection icon",
                            tint = CelestialGrey.copy(alpha = 0.4f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = t("no_favs"),
                            fontSize = 14.sp,
                            color = CelestialGrey,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (appLang == "AR") "احفظ الأحاديث والحكم الملهمة أو قم بطلب المزيد عبر الذكاء الاصطناعي لملء سجلك الروحي اليومي." else if (appLang == "FR") "Enregistrez des citations ou générez-en de nouvelles avec Gemini pour enrichir votre journal spirituel." else "Favorite quotes or generate new ones using Gemini to fill your spiritual journal.",
                            fontSize = 12.sp,
                            color = CelestialGrey.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        } else {
            items(savedQuotes) { quote ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("saved_quote_item_${quote.id}"),
                    colors = CardDefaults.cardColors(containerColor = TwilightCard),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF232549))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatQuote,
                                contentDescription = "Sajda quote marker",
                                tint = DeepSkyBlue.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )

                            IconButton(
                                onClick = { viewModel.removeStarredQuoteById(quote.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete starred quote",
                                    tint = CoralAlert.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = quote.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = PureDawnLight,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "— ${quote.author}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldenSun,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConfigureTab(viewModel: PrayerViewModel, context: Context) {
    val appLang by viewModel.appLang.collectAsStateWithLifecycle()
    val t = { key: String -> Translation.get(key, appLang) }

    val cityIdx by viewModel.cityPresetIdx.collectAsStateWithLifecycle()
    val customEnabled by viewModel.customLocEnabled.collectAsStateWithLifecycle()
    val customLat by viewModel.customLat.collectAsStateWithLifecycle()
    val customLon by viewModel.customLon.collectAsStateWithLifecycle()
    val customTz by viewModel.customTz.collectAsStateWithLifecycle()
    val customName by viewModel.customCityName.collectAsStateWithLifecycle()
    val calcMethod by viewModel.calcMethod.collectAsStateWithLifecycle()
    val asrMethod by viewModel.asrMethod.collectAsStateWithLifecycle()
    val recentRecords by viewModel.recentRecords.collectAsStateWithLifecycle()
    val alertSoundType by viewModel.alertSoundType.collectAsStateWithLifecycle()
    val customAdhanFileName by viewModel.customAdhanFileName.collectAsStateWithLifecycle()

    val isFullAdhanDownloaded by viewModel.isFullAdhanDownloaded.collectAsStateWithLifecycle()
    val isShortAdhanDownloaded by viewModel.isShortAdhanDownloaded.collectAsStateWithLifecycle()
    val isDownloadingFullAdhan by viewModel.isDownloadingFullAdhan.collectAsStateWithLifecycle()
    val isDownloadingShortAdhan by viewModel.isDownloadingShortAdhan.collectAsStateWithLifecycle()

    val searchQuery by viewModel.locationSearchQuery.collectAsStateWithLifecycle()
    val searchResult by viewModel.locationSearchResult.collectAsStateWithLifecycle()
    val searchIsSearching by viewModel.locationIsSearching.collectAsStateWithLifecycle()
    val searchErrorMsg by viewModel.locationSearchErrorMsg.collectAsStateWithLifecycle()

    val gpsLoading by viewModel.gpsLoading.collectAsStateWithLifecycle()
    val gpsError by viewModel.gpsError.collectAsStateWithLifecycle()

    val customAdhanLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = getFileName(context, uri) ?: "custom_adhan_audio.mp3"
            val success = viewModel.importCustomAdhan(uri, fileName)
            if (success) {
                val sMsg = if (appLang == "AR") "تم تحميل وإضافة الأذان الخاص بك بنجاح: $fileName"
                else if (appLang == "FR") "Adhan personnalisé avec succès : $fileName"
                else "Successfully imported and added your custom Adhan: $fileName"
                Toast.makeText(context, sMsg, Toast.LENGTH_LONG).show()
            } else {
                val fMsg = if (appLang == "AR") "فشل في تحميل أو نسخ ملف الصوت."
                else if (appLang == "FR") "Échec du chargement du fichier audio."
                else "Failed to load or copy audio file."
                Toast.makeText(context, fMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            viewModel.detectGPSLocation(context)
        } else {
            val pMsg = if (appLang == "AR") "تم رفض صلاحية تحديد الموقع الجغرافي." else if (appLang == "FR") "Autorisation de localisation refusée." else "Location permission denied."
            Toast.makeText(context, pMsg, Toast.LENGTH_LONG).show()
        }
    }

    var editingLat by remember(customLat, customLon, customTz, customName) { mutableStateOf(customLat.toString()) }
    var editingLon by remember(customLat, customLon, customTz, customName) { mutableStateOf(customLon.toString()) }
    var editingTz by remember(customLat, customLon, customTz, customName) { mutableStateOf(customTz.toString()) }
    var editingName by remember(customLat, customLon, customTz, customName) { mutableStateOf(customName) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // --- Multi Language Selection Section ---
        item {
            Text(
                text = (if (appLang == "AR") "اللغة (Language / Langue)" else if (appLang == "FR") "Langue (Language)" else "Language (Langue)").uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = CelestialGrey,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("language_selector_card"),
                colors = CardDefaults.cardColors(containerColor = TwilightCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF282A4C))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val languages = listOf("AR" to "العربية", "EN" to "English", "FR" to "Français")
                    languages.forEach { (code, name) ->
                        val isSelected = appLang == code
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .background(
                                    if (isSelected) GoldenSun else Color(0xFF1B2349).copy(alpha = 0.5f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    viewModel.updateAppLanguage(code)
                                    Toast.makeText(context, if (code == "AR") "تم تغيير لغة التطبيق إلى العربية بنجاح." else if (code == "FR") "Langue modifiée avec succès." else "Language updated to English successfully.", Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) TwilightBackground else PureDawnLight
                            )
                        }
                    }
                }
            }
        }

        // --- Theme Selection Section ---
        item {
            val activeTheme by viewModel.themeMode.collectAsStateWithLifecycle()
            ThemeSwitcherCard(
                currentThemeId = activeTheme,
                onThemeSelect = { viewModel.updateThemeMode(it) },
                appLang = appLang
            )
        }

        // --- About Application Section ---
        item {
            val aboutTitle = if (appLang == "AR") "نبذة عن التطبيق" else if (appLang == "FR") "À propos de l'application" else "About Application"
            Text(
                text = aboutTitle.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = CelestialGrey,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("about_app_settings_card"),
                colors = CardDefaults.cardColors(containerColor = TwilightCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF282A4C))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0xFF232549), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About App Icon",
                            tint = GoldenSun,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (appLang == "AR") "آذان ومواقيت الصلاة" else if (appLang == "FR") "Rappels de Prières" else "Prayer Reminders",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = PureDawnLight
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (appLang == "AR") {
                            "مرافقك الروحي اليومي للحصول على أدق مواقيت الصلاة من MAWAQIT، قراءة القرآن والأذكار بحرية وبون قيود، تحديد اتجاه القبلة، والتدبر عبر المساعد الذكي Gemini AI وصياغة الحكم والأوراد الإيمانية."
                        } else if (appLang == "FR") {
                            "Votre compagnon spirituel quotidien proposant des horaires de prière précis (MAWAQIT), la lecture du Coran et des Azkars, une boussole de la Qibla et des réflexions inspirantes par Gemini AI."
                        } else {
                            "Your daily spiritual companion offering accurate prayer times from MAWAQIT, Quran and Azkar reading, a precise Qibla compass, and personalized reflections generated via Gemini AI."
                        },
                        fontSize = 12.sp,
                        color = CelestialGrey,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFF282A4C), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "SOLTANE Mourad by AI Creative",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldenSun,
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Version 1.2.0 • Created with Serenity",
                        fontSize = 10.sp,
                        color = CelestialGrey.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Streak / Habit Stats Tracker Section
        item {
            Text(
                text = (if (appLang == "AR") "تحليل العادات والمواظبة (٧ أيام)" else if (appLang == "FR") "Cohérence des Prières (7 jours)" else "Habit Analytics (7 Days)").uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = CelestialGrey,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TwilightCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF282A4C))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = if (appLang == "AR") "مؤشر المواظبة على الصلوات بناءً على علامات الإتمام المدخلة." else if (appLang == "FR") "Suivi de la régularité des prières basé sur vos cases cochées." else "Prayer Consistency tracker based on your marked checkboxes.",
                        fontSize = 12.sp,
                        color = CelestialGrey,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Draw a beautiful custom 7 day calendar checklist cells
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Gather last 7 days calendar times
                        val calendar = Calendar.getInstance()
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val sDayFmt = SimpleDateFormat("EEE", Locale.getDefault())

                        val daysList = mutableListOf<Pair<String, String>>()
                        for (i in 0..6) {
                            val cur = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                            daysList.add(Pair(sdf.format(cur.time), sDayFmt.format(cur.time)))
                        }
                        daysList.reverse()

                        daysList.forEach { dayPair ->
                            val matchRecord = recentRecords.firstOrNull { it.date == dayPair.first }
                            val totalPrayed = if (matchRecord != null) {
                                (if (matchRecord.fajrPrayed) 1 else 0) +
                                (if (matchRecord.dhuhrPrayed) 1 else 0) +
                                (if (matchRecord.asrPrayed) 1 else 0) +
                                (if (matchRecord.maghribPrayed) 1 else 0) +
                                (if (matchRecord.ishaPrayed) 1 else 0)
                            } else 0

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 2.dp)
                            ) {
                                Text(
                                    text = dayPair.second,
                                    fontSize = 12.sp,
                                    color = CelestialGrey,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(
                                            if (totalPrayed == 5) Color(0xFF10B981)
                                            else if (totalPrayed > 0) Color(0xFF047857).copy(alpha = 0.5f)
                                            else Color(0xFF222645),
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$totalPrayed",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (totalPrayed > 0) PureDawnLight else CelestialGrey.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Notification Test Card
        item {
            Text(
                text = (if (appLang == "AR") "تشخيص واختبار إشارات التنبيه" else if (appLang == "FR") "Diagnostic du Signal de Rappel" else "Reminder Signal Diagnostic").uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = CelestialGrey,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TwilightCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF282A4C))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = if (appLang == "AR") "اختبار جرس التنبيه المحلي" else if (appLang == "FR") "Tester la notification de rappel" else "Test Local Notification Alarm",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureDawnLight
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (appLang == "AR") "يتيح لك إرسال إشعار محلي مع الصوت لتجربته في غضون ٥ ثوانٍ." else if (appLang == "FR") "Planifie une alarme de test locale avec son dans exactement 5 secondes." else "Triggers a local test alarm with volume to fire in exactly 5 seconds.",
                            fontSize = 12.sp,
                            color = CelestialGrey,
                            lineHeight = 16.sp
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.triggerTestNotification()
                            val noticeTxt = if (appLang == "AR") "تمت جدولة إشعار الاختبار! يرجى الانتظار ٥ ثوانٍ."
                            else if (appLang == "FR") "Notification de test planifiée ! Attendez 5 secondes."
                            else "Test notice scheduled! Wait 5s."
                            Toast.makeText(context, noticeTxt, Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepSkyBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_trigger_test")
                    ) {
                        Text(
                            text = if (appLang == "AR") "اختبار (٥ث)" else "Test (5s)",
                            color = TwilightBackground,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Presets choosing card
        item {
            Text(
                text = (if (appLang == "AR") "إعدادات الموقع الجغرافي" else if (appLang == "FR") "Paramètres de Localisation" else "Location Parameters").uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = CelestialGrey,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gps_location_updater_card"),
                colors = CardDefaults.cardColors(containerColor = TwilightCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF2E3162))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF222448), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "GPS Location Support",
                                tint = GoldenSun,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (appLang == "AR") "تحديد الموقع تلقائياً عبر GPS" else if (appLang == "FR") "Localisation GPS Automatique" else "Auto-Detect Location (GPS)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureDawnLight
                            )
                            Text(
                                text = if (appLang == "AR") "استخدم الـ GPS لتحديد موقعك تلقائياً وبدقة لتحديث مواقيت الصلاة والقبلة فورياً." else if (appLang == "FR") "Mettez automatiquement à jour vos coordonnées géographiques pour des calculs d'horaires et de Qibla impeccables." else "Instantly find your precision coordinates dynamically for perfect prayer timetable and Qibla alignments.",
                                fontSize = 11.sp,
                                color = CelestialGrey
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (fineGranted || coarseGranted) {
                                viewModel.detectGPSLocation(context)
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DeepSkyBlue,
                            disabledContainerColor = DeepSkyBlue.copy(alpha = 0.4f)
                        ),
                        enabled = !gpsLoading,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("btn_detect_gps_location")
                    ) {
                        if (gpsLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = TwilightBackground,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (appLang == "AR") "جاري الاتصال بالأقمار الصناعية..." else if (appLang == "FR") "Recherche satellite..." else "Locating satellite...", color = TwilightBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = "Detect",
                                tint = TwilightBackground,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (appLang == "AR") "تحديد الموقع عبر GPS الآن" else if (appLang == "FR") "Localiser via GPS" else "Detect location via GPS", color = TwilightBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    gpsError?.let { err ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF381B2B)),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, CoralAlert.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Error",
                                    tint = CoralAlert,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = err,
                                    color = CoralAlert,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    if (customEnabled && customName.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF24445A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Active Location",
                                    tint = GreenEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (appLang == "AR") "الموقع النشط حالياً: $customName" else if (appLang == "FR") "Position active : $customName" else "Currently active: $customName",
                                        color = PureDawnLight,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (appLang == "AR") "الإحداثيات: عرض: ${String.format("%.4f", customLat)} | طول: ${String.format("%.4f", customLon)} | النطاق الزمني: UTC${if (customTz >= 0) "+" else ""}$customTz" else if (appLang == "FR") "Coordonnées : Lat: ${String.format("%.4f", customLat)} | Lon: ${String.format("%.4f", customLon)} | TZ : UTC${if (customTz >= 0) "+" else ""}$customTz" else "Coordinates: Lat: ${String.format("%.4f", customLat)} | Lon: ${String.format("%.4f", customLon)} | TZ: UTC${if (customTz >= 0) "+" else ""}$customTz",
                                        color = CelestialGrey,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TwilightCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF282A4C))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = if (appLang == "AR") "أ. اختيار مدينة جاهزة" else if (appLang == "FR") "A. Villes Prédéfinies" else "A. Preset Cities",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldenSun
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(CityPreset.presets.size) { index ->
                            val preset = CityPreset.presets[index]
                            val isSelected = !customEnabled && cityIdx == index
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color(0xFF2D3169) else Color(0xFF13152C))
                                    .border(
                                        1.dp,
                                        if (isSelected) GoldenSun else Color(0xFF282A4C),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { viewModel.selectPresetCity(index) }
                                    .padding(8.dp)
                                    .testTag("preset_city_$index"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = preset.cityName.split(",")[0],
                                    color = if (isSelected) GoldenSun else PureDawnLight,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = Color(0xFF282A4C))
                    Spacer(modifier = Modifier.height(16.dp))

                    // B. AI Place Search
                    Text(
                        text = if (appLang == "AR") "ب. بحث ذكي عن مدينة أو معلم" else if (appLang == "FR") "B. Recherche de Ville (Propulsé par l'IA)" else "B. Global Place Search (AI-Powered)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldenSun
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateLocationSearchQuery(it) },
                        label = { Text(if (appLang == "AR") "ابحث عن مكان (مثل: مكة، الدار البيضاء، القاهرة)" else if (appLang == "FR") "Rechercher un lieu (ex: Makkah, Casablanca, Paris)" else "Search Place (e.g. Makkah, Casablanca, Boston)", color = CelestialGrey) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PureDawnLight,
                            unfocusedTextColor = PureDawnLight,
                            focusedBorderColor = GoldenSun,
                            unfocusedBorderColor = Color(0xFF282A4C)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.clearLocationSearch() }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        tint = CelestialGrey
                                    )
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { viewModel.searchLocation() },
                        enabled = !searchIsSearching && searchQuery.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DeepSkyBlue,
                            disabledContainerColor = DeepSkyBlue.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("btn_search_place"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (searchIsSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = TwilightBackground,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (appLang == "AR") "جاري البحث عبر Gemini..." else if (appLang == "FR") "Recherche par l'IA..." else "Searching with Gemini...", color = TwilightBackground, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = TwilightBackground,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (appLang == "AR") "استعلام الإحداثيات وخريطة الصلاة" else if (appLang == "FR") "Obtenir fuseau & coordonnées" else "Query Coordinates & Timezone", color = TwilightBackground, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (searchErrorMsg.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = searchErrorMsg,
                            color = CoralAlert,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    searchResult?.let { result ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF13152C)),
                            border = BorderStroke(1.dp, GoldenSun.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (appLang == "AR") "تم العثور على مطابقة:" else if (appLang == "FR") "Correspondance trouvée :" else "Match Found:",
                                    fontSize = 11.sp,
                                    color = GoldenSun,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = result.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureDawnLight
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "${if (appLang == "AR") "العرض" else "Lat"}: ${String.format("%.4f", result.latitude)}",
                                        fontSize = 12.sp,
                                        color = CelestialGrey
                                    )
                                    Text(
                                        text = "${if (appLang == "AR") "الطول" else "Lon"}: ${String.format("%.4f", result.longitude)}",
                                        fontSize = 12.sp,
                                        color = CelestialGrey
                                    )
                                    Text(
                                        text = "${if (appLang == "AR") "فارق UTC" else if (appLang == "FR") "Décalage" else "UTC Offset"}: ${if (result.timezoneOffset >= 0) "+" else ""}${result.timezoneOffset}",
                                        fontSize = 12.sp,
                                        color = CelestialGrey
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        editingName = result.name
                                        editingLat = result.latitude.toString()
                                        editingLon = result.longitude.toString()
                                        editingTz = result.timezoneOffset.toString()
                                        viewModel.enableCustomLocation(result.latitude, result.longitude, result.timezoneOffset, result.name)
                                        val actMsg = if (appLang == "AR") "تم تفعيل الموقع الجديد بنجاح: ${result.name}"
                                        else if (appLang == "FR") "Position personnalisée activée : ${result.name}"
                                        else "Activated Custom Location: ${result.name}"
                                        Toast.makeText(context, actMsg, Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp)
                                        .testTag("apply_search_result"),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = if (appLang == "AR") "تطبيق وتفعيل هذا الموقع" else if (appLang == "FR") "Appliquer et Activer" else "Apply & Enable This Location",
                                        fontSize = 11.sp,
                                        color = PureDawnLight,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = Color(0xFF282A4C))
                    Spacer(modifier = Modifier.height(16.dp))

                    // C. Custom Coordinates editor
                    Text(
                        text = if (appLang == "AR") "ج. إدخال وتعديل الإحداثيات يدوياً" else if (appLang == "FR") "C. Coordonnées Manuelles Précises" else "C. Manual Coordinates Fine-tuning",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldenSun
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editingName,
                        onValueChange = { editingName = it },
                        label = { Text(if (appLang == "AR") "اسم المدينة/الموقع" else if (appLang == "FR") "Nom ou étiquette de la position" else "City Name Label", color = CelestialGrey) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PureDawnLight,
                            unfocusedTextColor = PureDawnLight,
                            focusedBorderColor = GoldenSun,
                            unfocusedBorderColor = Color(0xFF282A4C)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editingLat,
                            onValueChange = { editingLat = it },
                            label = { Text(if (appLang == "AR") "دائرة العرض" else if (appLang == "FR") "Latitude" else "Latitude", color = CelestialGrey) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = PureDawnLight,
                                unfocusedTextColor = PureDawnLight,
                                focusedBorderColor = GoldenSun,
                                unfocusedBorderColor = Color(0xFF282A4C)
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = editingLon,
                            onValueChange = { editingLon = it },
                            label = { Text(if (appLang == "AR") "خط الطول" else if (appLang == "FR") "Longitude" else "Longitude", color = CelestialGrey) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = PureDawnLight,
                                unfocusedTextColor = PureDawnLight,
                                focusedBorderColor = GoldenSun,
                                unfocusedBorderColor = Color(0xFF282A4C)
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = editingTz,
                        onValueChange = { editingTz = it },
                        label = { Text(if (appLang == "AR") "فارق الساعة UTC (مثل: +3.0، -5.0)" else if (appLang == "FR") "Fuseau horaire UTC (ex: +3.0, -5.0)" else "Timezone UTC Offset (e.g. +3.0, -5.0)", color = CelestialGrey) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PureDawnLight,
                            unfocusedTextColor = PureDawnLight,
                            focusedBorderColor = GoldenSun,
                            unfocusedBorderColor = Color(0xFF282A4C)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val parsedLat = editingLat.toDoubleOrNull()
                            val parsedLon = editingLon.toDoubleOrNull()
                            val parsedTz = editingTz.toDoubleOrNull()

                            if (parsedLat == null || parsedLon == null || parsedTz == null) {
                                val errTxt = if (appLang == "AR") "خطأ: قيم الإحداثيات الرقمية المدخلة غير صالحة."
                                else if (appLang == "FR") "Erreur : les valeurs numériques fournies sont invalides."
                                else "Error: Standard coordinates numeric parameters invalid."
                                Toast.makeText(context, errTxt, Toast.LENGTH_SHORT).show()
                            } else {
                                val name = editingName.trim().ifEmpty { if (appLang == "AR") "موقع مخصص" else if (appLang == "FR") "Position custom" else "Custom Coordinates" }
                                viewModel.enableCustomLocation(parsedLat, parsedLon, parsedTz, name)
                                val succTxt = if (appLang == "AR") "تم حفظ الموقع المخصص: $name بنجاح!"
                                else if (appLang == "FR") "Position personnalisée : $name enregistrée !"
                                else "Custom Location: $name saved successfully!"
                                Toast.makeText(context, succTxt, Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (customEnabled) Color(0xFF10B981) else GoldenSun),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("btn_save_custom_loc"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (customEnabled) (if (appLang == "AR") "✓ الموقع المخصص نشط حالياً" else if (appLang == "FR") "✓ Position personnalisée active" else "✓ Custom Location Active") else (if (appLang == "AR") "تطبيق وتفعيل الإحداثيات المخصصة" else if (appLang == "FR") "Appliquer et Activer les coordonées" else "Apply Custom Coordinates"),
                            color = if (customEnabled) PureDawnLight else TwilightBackground,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Calculation methods choosing card
        item {
            Text(
                text = (if (appLang == "AR") "معايير الحساب الفقهية" else if (appLang == "FR") "Conventions de Calcul" else "Mathematical Conventions").uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = CelestialGrey,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TwilightCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF282A4C))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = if (appLang == "AR") "١. طريقة حساب مواقيت الصلاة" else if (appLang == "FR") "1. Méthode de calcul" else "1. Calculation Method",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldenSun
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    CalculationMethod.values().forEach { method ->
                        val isSelected = method == calcMethod
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.updateCalculationMethod(method) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.updateCalculationMethod(method) },
                                colors = RadioButtonDefaults.colors(selectedColor = GoldenSun)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (appLang == "AR") {
                                        if (method == CalculationMethod.UMM_AL_QURA) "تقويم أم القرى (السعودية)"
                                        else if (method == CalculationMethod.EGYPT) "الهيئة العامة المصرية للمساحة"
                                        else if (method == CalculationMethod.MWL) "رابطة العالم الإسلامي"
                                        else if (method == CalculationMethod.ISNA) "الجمعية الإسلامية لأمريكا الشمالية"
                                        else if (method == CalculationMethod.KARACHI) "جامعة العلوم الإسلامية بكراتشي"
                                        else method.displayName
                                    } else method.displayName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PureDawnLight
                                )
                                Text(
                                    text = if (method == CalculationMethod.UMM_AL_QURA) {
                                        if (appLang == "AR") "الفجر: ١٨.٥° / العشاء: بعد المغرب بـ ٩٠ دقيقة"
                                        else if (appLang == "FR") "Fajr 18.5° / Isha 90 min après Maghrib"
                                        else "18.5° Fajr / Isha starts 90 min after Maghrib"
                                    } else {
                                        if (appLang == "AR") "الفجر: ${method.fajrAngle}° / العشاء: ${method.ishaAngle}°"
                                        else if (appLang == "FR") "Fajr ${method.fajrAngle}° / Isha ${method.ishaAngle}°"
                                        else "${method.fajrAngle}° Fajr / ${method.ishaAngle}° Isha"
                                    },
                                    fontSize = 12.sp,
                                    color = CelestialGrey
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFF282A4C))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Asr methods
                    Text(
                        text = if (appLang == "AR") "٢. المذهب الفقهي لصلاة العصر (طول الظل)" else if (appLang == "FR") "2. Jurisprudence de la prière de l'Asr" else "2. Asr Juristic Shadow Method",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldenSun
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    AsrJuristicMethod.values().forEach { method ->
                        val isSelected = method == asrMethod
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.updateAsrMethod(method) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.updateAsrMethod(method) },
                                colors = RadioButtonDefaults.colors(selectedColor = GoldenSun)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = method.displayName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PureDawnLight
                                )
                                Text(
                                    text = if (appLang == "AR") "مضاعف عامل الظل: x${method.shadowFactor}" else if (appLang == "FR") "Multiplicateur de facteur d'ombre : x${method.shadowFactor}" else "Shadow factor multiplier: x${method.shadowFactor}",
                                    fontSize = 12.sp,
                                    color = CelestialGrey
                                )
                            }
                        }
                    }
                }
            }
        }

        // Alert sound section card
        item {
            Text(
                text = (if (appLang == "AR") "أصوات وإشعارات الأذان" else if (appLang == "FR") "Sons de l'Adhan et d'Alerte" else "Adhan & Alert Sounds").uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = CelestialGrey,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("card_alert_sounds"),
                colors = CardDefaults.cardColors(containerColor = TwilightCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF282A4C))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = if (appLang == "AR") "تخصيص رنات وصوت الأذان" else if (appLang == "FR") "Configuration d'Alerte et d'Adhan" else "Adhan & Alert Sound Option",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldenSun
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val soundOptions = when (appLang) {
                        "AR" -> listOf(
                            Triple("ADHAN_FULL", "الأذان الكامل (مكة المكرمة)", "أذان بصوت جميل كامل عند دخول وقت الصلاة"),
                            Triple("ADHAN_SHORT", "تكبيرات الأذان فقط", "تكبيرتان فقط لتنبيه سريع وموجز"),
                            Triple(
                                "CUSTOM", 
                                if (customAdhanFileName != null) "أذان مخصص: $customAdhanFileName" else "تحميل أذان مخصص من جهازك (الملف)", 
                                if (customAdhanFileName != null) "تم تحميل الملف بنجاح كصوت للأذان" else "اختر ملفاً صوتياً (MP3 أو WAV) ليكون صوت الأذان الخاص بك"
                            ),
                            Triple("SYSTEM_DEFAULT", "نغمة النظام الافتراضية", "نغمة التنبيه الافتراضية الخاصة بهاتفك"),
                            Triple("SILENT", "بدون صوت (صامت)", "إشعار مرئي فقط بدون أي أصوات")
                        )
                        "FR" -> listOf(
                            Triple("ADHAN_FULL", "Adhan Complet (Makkah)", "Adhan complet et serein avec de magnifiques voix"),
                            Triple("ADHAN_SHORT", "Takbeer Uniquement", "Seulement deux Takbeer pour un rappel rapide"),
                            Triple(
                                "CUSTOM", 
                                if (customAdhanFileName != null) "Adhan personnalisé : $customAdhanFileName" else "Importer un Adhan personnalisé (Fichier)", 
                                if (customAdhanFileName != null) "Fichier audio chargé avec succès" else "Sélectionnez votre propre fichier audio (MP3, WAV) pour l'Adhan"
                            ),
                            Triple("SYSTEM_DEFAULT", "Notification du Système", "Sonnerie par défaut programmée de l'appareil"),
                            Triple("SILENT", "Mode Silencieux", "Affiche uniquement une notification sans aucun son")
                        )
                        else -> listOf(
                            Triple("ADHAN_FULL", "Full Adhan (Makkah)", "Magnificent full call to prayer sound when time starts"),
                            Triple("ADHAN_SHORT", "Adhan Takbeer Only", "First two Takbeers for a lightweight notification"),
                            Triple(
                                "CUSTOM", 
                                if (customAdhanFileName != null) "Custom Adhan: $customAdhanFileName" else "Import Custom Adhan File", 
                                if (customAdhanFileName != null) "Audio file successfully loaded" else "Choose your own audio recording (MP3, WAV) for the Adhan alert"
                            ),
                            Triple("SYSTEM_DEFAULT", "System Default Tone", "Your device's default push notification chime"),
                            Triple("SILENT", "Silent Mode", "Banner and icon vibration alerts only under zero volume")
                        )
                    }

                    soundOptions.forEach { (type, title, desc) ->
                        val isSelected = type == alertSoundType
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    if (type == "CUSTOM" && customAdhanFileName == null) {
                                        customAdhanLauncher.launch("audio/*")
                                    } else {
                                        viewModel.updateAlertSoundType(type)
                                    }
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { 
                                    if (type == "CUSTOM" && customAdhanFileName == null) {
                                        customAdhanLauncher.launch("audio/*")
                                    } else {
                                        viewModel.updateAlertSoundType(type)
                                    }
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = GoldenSun)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PureDawnLight
                                )
                                Text(
                                    text = desc,
                                    fontSize = 12.sp,
                                    color = CelestialGrey
                                )
                                if (type == "ADHAN_FULL") {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isDownloadingFullAdhan) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(12.dp),
                                                strokeWidth = 1.5.dp,
                                                color = GoldenSun
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (appLang == "AR") "جاري التحميل للعمل بدون إنترنت..."
                                                       else if (appLang == "FR") "Téléchargement pour le mode hors-ligne..."
                                                       else "Downloading for offline use...",
                                                fontSize = 11.sp,
                                                color = GoldenSun
                                            )
                                        } else if (isFullAdhanDownloaded) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Downloaded",
                                                tint = Color(0xFF4CAF50),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (appLang == "AR") "جاهز ومحمّل للعمل بدون إنترنت ✅"
                                                       else if (appLang == "FR") "Prêt pour le mode hors-ligne ✅"
                                                       else "Ready for offline use ✅",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF4CAF50)
                                            )
                                        } else {
                                            Text(
                                                text = if (appLang == "AR") "⚠️ غير محمّل! اضغط هنا للتنزيل للعمل بدون إنترنت"
                                                       else if (appLang == "FR") "⚠️ Non téléchargé ! Appuyez pour charger"
                                                       else "⚠️ Not loaded! Tap to download for offline use",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFF9800),
                                                modifier = Modifier
                                                    .clickable { viewModel.triggerAdhanDownload("ADHAN_FULL") }
                                                    .padding(vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                if (type == "ADHAN_SHORT") {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isDownloadingShortAdhan) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(12.dp),
                                                strokeWidth = 1.5.dp,
                                                color = GoldenSun
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (appLang == "AR") "جاري التحميل للعمل بدون إنترنت..."
                                                       else if (appLang == "FR") "Téléchargement pour le mode hors-ligne..."
                                                       else "Downloading for offline use...",
                                                fontSize = 11.sp,
                                                color = GoldenSun
                                            )
                                        } else if (isShortAdhanDownloaded) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Downloaded",
                                                tint = Color(0xFF4CAF50),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (appLang == "AR") "جاهز ومحمّل للعمل بدون إنترنت ✅"
                                                       else if (appLang == "FR") "Prêt pour le mode hors-ligne ✅"
                                                       else "Ready for offline use ✅",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF4CAF50)
                                            )
                                        } else {
                                            Text(
                                                text = if (appLang == "AR") "⚠️ غير محمّل! اضغط هنا للتنزيل للعمل بدون إنترنت"
                                                       else if (appLang == "FR") "⚠️ Non téléchargé ! Appuyez pour charger"
                                                       else "⚠️ Not loaded! Tap to download for offline use",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFF9800),
                                                modifier = Modifier
                                                    .clickable { viewModel.triggerAdhanDownload("ADHAN_SHORT") }
                                                    .padding(vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                if (type == "CUSTOM") {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (appLang == "AR") "📥 اضغط هنا لاختيار ملف صوتي من هاتفك"
                                               else if (appLang == "FR") "📥 Cliquez ici pour sélectionner un fichier audio"
                                               else "📥 Tap here to choose an audio file from device",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepSkyBlue,
                                        modifier = Modifier
                                            .clickable { customAdhanLauncher.launch("audio/*") }
                                            .padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFF282A4C))
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (appLang == "AR") "تجربة واختبار الصوت المختار:" else if (appLang == "FR") "Test de Lecture Audio :" else "Audio Playback Test:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureDawnLight
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.playTestSound() },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepSkyBlue),
                            modifier = Modifier.weight(1f).height(40.dp).testTag("btn_play_test_sound"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Test Audio",
                                tint = TwilightBackground,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (appLang == "AR") "استماع" else if (appLang == "FR") "Tester" else "Test Play", color = TwilightBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.stopTestSound() },
                            colors = ButtonDefaults.buttonColors(containerColor = CoralAlert),
                            modifier = Modifier.weight(1f).height(40.dp).testTag("btn_stop_test_sound"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop Test Audio",
                                tint = PureDawnLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (appLang == "AR") "إيقاف" else if (appLang == "FR") "Arrêter" else "Stop", color = PureDawnLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Map corresponding prayer modes to spiritual astronomical vector symbols
@Composable
fun imageIconsForNextPrayer(prayer: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (prayer) {
        "Fajr" -> Icons.Outlined.Brightness3       // Waxing moon (dawn twilight)
        "Sunrise" -> Icons.Default.WbTwilight       // Rising sun
        "Dhuhr" -> Icons.Default.WbSunny         // Shining noon sun
        "Asr" -> Icons.Outlined.WbSunny          // Afternoon sun
        "Maghrib" -> Icons.Default.FilterDrama       // Sunset clouds drama
        "Isha" -> Icons.Default.Star             // Stars glowing in the night
        else -> Icons.Default.NotificationAdd
    }
}

@Composable
fun HijriCalendarCard(viewModel: PrayerViewModel) {
    val context = LocalContext.current
    val hijriOffset by viewModel.hijriOffset.collectAsStateWithLifecycle()
    val appLang by viewModel.appLang.collectAsStateWithLifecycle()
    
    val todayCal = remember { Calendar.getInstance() }
    val activeTodayHijri = remember(hijriOffset) {
        HijriCalendarHelper.convertGregorianToHijri(
            todayCal.get(Calendar.YEAR),
            todayCal.get(Calendar.MONTH) + 1,
            todayCal.get(Calendar.DAY_OF_MONTH),
            hijriOffset
        )
    }

    var viewMonth by remember { mutableStateOf(activeTodayHijri.month) }
    var viewYear by remember { mutableStateOf(activeTodayHijri.year) }
    var selectedDay by remember { mutableStateOf<Int?>(activeTodayHijri.day) }
    var isExpanded by remember { mutableStateOf(false) }

    // Sync visible calendar when today date calculations change from offset updates
    LaunchedEffect(activeTodayHijri) {
        if (!isExpanded) {
            viewMonth = activeTodayHijri.month
            viewYear = activeTodayHijri.year
            selectedDay = activeTodayHijri.day
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hijri_calendar_card"),
        colors = CardDefaults.cardColors(containerColor = TwilightCard),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFF2E3162))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header display of today
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF222448), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Hijri Calendar",
                        tint = GoldenSun,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (appLang == "AR") "التقويم الهجري اليومي" else if (appLang == "FR") "Calendrier Islamique" else "Daily Islamic Calendar",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepSkyBlue,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (appLang == "AR") activeTodayHijri.formatAr() else activeTodayHijri.formatEn(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = PureDawnLight
                    )
                    Text(
                        text = SimpleDateFormat("EEEE, d MMMM yyyy", if (appLang == "AR") Locale("ar") else if (appLang == "FR") Locale.FRANCE else Locale.US).format(todayCal.time),
                        fontSize = 12.sp,
                        color = CelestialGrey
                    )
                }

                IconButton(
                    onClick = { viewModel.setShowDateConverterDialog(true) },
                    modifier = Modifier.testTag("btn_header_open_converter")
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Open Date Converter",
                        tint = GoldenSun,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.testTag("btn_expand_hijri_cal")
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand Calendar",
                        tint = CelestialGrey
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    HorizontalDivider(color = Color(0xFF2E3162))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Month Navigation Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (viewMonth == 1) {
                                    viewMonth = 12
                                    viewYear -= 1
                                } else {
                                    viewMonth -= 1
                                }
                                selectedDay = null
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF222448), RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Previous Month",
                                tint = PureDawnLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${HijriCalendarHelper.getMonthNameAr(viewMonth)} $viewYear هـ",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldenSun
                            )
                            Text(
                                text = HijriCalendarHelper.getMonthNameEn(viewMonth) + " $viewYear AH",
                                fontSize = 11.sp,
                                color = CelestialGrey
                            )
                        }

                        IconButton(
                            onClick = {
                                if (viewMonth == 12) {
                                    viewMonth = 1
                                    viewYear += 1
                                } else {
                                    viewMonth += 1
                                }
                                selectedDay = null
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF222448), RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Next Month",
                                tint = PureDawnLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Grid Layout
                    val firstDayOfWeek = HijriCalendarHelper.getHijriDayOfWeek(viewYear, viewMonth, 1)
                    val daysInMonth = HijriCalendarHelper.getDaysInHijriMonth(viewYear, viewMonth)
                    val totalCells = (firstDayOfWeek - 1) + daysInMonth
                    val rowsCount = Math.ceil(totalCells / 7.0).toInt()

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Weekday labels
                        Row(modifier = Modifier.fillMaxWidth()) {
                            val daysLabel = when (appLang) {
                                "AR" -> listOf("أحد", "إثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة", "سبت")
                                "FR" -> listOf("Dim", "Lun", "Mar", "Mer", "Jeu", "Ven", "Sam")
                                else -> listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                            }
                            daysLabel.forEach { dayLabel ->
                                Text(
                                    text = dayLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CelestialGrey,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        for (r in 0 until rowsCount) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                for (c in 0 until 7) {
                                    val cellIndex = r * 7 + c
                                    val dayNum = cellIndex - (firstDayOfWeek - 1) + 1

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                    ) {
                                        if (dayNum in 1..daysInMonth) {
                                            val isToday = (dayNum == activeTodayHijri.day && viewMonth == activeTodayHijri.month && viewYear == activeTodayHijri.year)
                                            val isSelected = (dayNum == selectedDay)

                                            val bgColor = when {
                                                isSelected -> GreenEmerald.copy(alpha = 0.25f)
                                                isToday -> GoldenSun.copy(alpha = 0.2f)
                                                else -> Color(0xFF1E1F3C)
                                            }
                                            val borderColor = when {
                                                isSelected -> GreenEmerald
                                                isToday -> GoldenSun
                                                else -> Color(0xFF2C2F5E)
                                            }

                                            val hasOccasion = remember(viewMonth, dayNum) {
                                                HijriCalendarHelper.occurrences.any { it.hijriMonth == viewMonth && it.hijriDay == dayNum }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(bgColor)
                                                    .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        selectedDay = if (selectedDay == dayNum) null else dayNum
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Text(
                                                        text = dayNum.toString(),
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = when {
                                                            isSelected -> GreenEmerald
                                                            isToday -> GoldenSun
                                                            else -> PureDawnLight
                                                        }
                                                    )
                                                    if (hasOccasion) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(4.dp)
                                                                .background(DeepSkyBlue, CircleShape)
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.fillMaxSize())
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Selected Day Details panel
                    if (selectedDay != null) {
                        val dayOccasions = HijriCalendarHelper.occurrences.filter { it.hijriMonth == viewMonth && it.hijriDay == selectedDay }
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("day_details_card"),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF181530)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF33315B))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (appLang == "AR") {
                                        "تفاصيل يوم $selectedDay ${HijriCalendarHelper.getMonthNameAr(viewMonth)}:"
                                    } else if (appLang == "FR") {
                                        "Détails du $selectedDay ${HijriCalendarHelper.getMonthNameEn(viewMonth)} :"
                                    } else {
                                        "Details for $selectedDay ${HijriCalendarHelper.getMonthNameEn(viewMonth)}:"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepSkyBlue
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                if (dayOccasions.isNotEmpty()) {
                                    dayOccasions.forEach { occ ->
                                        Row(
                                            modifier = Modifier.padding(bottom = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = GoldenSun,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    text = if (appLang == "AR") occ.titleAr else occ.titleEn,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PureDawnLight
                                                )
                                                Text(
                                                    text = if (appLang == "AR") occ.descAr else occ.descEn,
                                                    fontSize = 11.sp,
                                                    color = CelestialGrey
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = if (appLang == "AR") "لا توجد مناسبات دينية رئيسية مسجلة في هذا اليوم."
                                        else if (appLang == "FR") "Aucun événement spirituel majeur pour ce jour."
                                        else "No major spiritual events found on this day.",
                                        fontSize = 12.sp,
                                        color = CelestialGrey,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = Color(0xFF2E3162))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Sighting Adjuster Controls
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF222448), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = if (appLang == "AR") "تعديل تقويم رؤية الهلال (Hijri Calibration)"
                            else if (appLang == "FR") "Calibrage du Calendrier Hijri"
                            else "Sighting Adjuster (Hijri Calibration)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldenSun
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (appLang == "AR") "للتوافق مع الرؤية المحلية في بلدك في حال اختلاف مطالع الأهلة."
                            else if (appLang == "FR") "Ajustez pour correspondre aux observations de la lune dans votre région."
                            else "Align with local moonsightings in your country if crescent reports vary.",
                            fontSize = 10.sp,
                            color = CelestialGrey
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { viewModel.updateHijriOffset(hijriOffset - 1) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E3162)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(32.dp).testTag("btn_sh_offset_dec")
                            ) {
                                Text("-", fontSize = 16.sp, color = PureDawnLight, fontWeight = FontWeight.Bold)
                            }

                            val offsetLabel = when {
                                hijriOffset > 0 -> if (appLang == "AR") "+$hijriOffset يوم (تقديم)" else if (appLang == "FR") "+$hijriOffset jour(s) (avance)" else "+$hijriOffset day(s) (forward)"
                                hijriOffset < 0 -> if (appLang == "AR") "$hijriOffset يوم (تأخير)" else if (appLang == "FR") "$hijriOffset jour(s) (retard)" else "$hijriOffset day(s) (delayed)"
                                else -> if (appLang == "AR") "تطابق قياسي (0 يوم)" else if (appLang == "FR") "Standard (0 jour)" else "Standard Match (0 days)"
                            }
                            Text(
                                text = offsetLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureDawnLight,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )

                            Button(
                                onClick = { viewModel.updateHijriOffset(hijriOffset + 1) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E3162)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(32.dp).testTag("btn_sh_offset_inc")
                            ) {
                                Text("+", fontSize = 16.sp, color = PureDawnLight, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (hijriOffset != 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            TextButton(
                                onClick = { viewModel.updateHijriOffset(0) },
                                modifier = Modifier.align(Alignment.CenterHorizontally).testTag("btn_sh_offset_reset")
                            ) {
                                Text(
                                    text = if (appLang == "AR") "إعادة الضبط للتاريخ الافتراضي" else if (appLang == "FR") "Réinitialiser" else "Reset to Default Date",
                                    fontSize = 11.sp,
                                    color = DeepSkyBlue
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = Color(0xFF2E3162))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Embedded Interactive Hijri & Gregorian Converter Card
                    Text(
                        text = if (appLang == "AR") "محوّل التاريخ الهجري والميلادي" else if (appLang == "FR") "Convertisseur de Dates Hijri / Grégorien" else "Hijri & Gregorian Date Converter",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureDawnLight
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (appLang == "AR") "تحويل فوري ودقيق بين التقويمين باليوم والشهر والسنة مع كشف المناسبات:" else if (appLang == "FR") "Convertissez instantanément n'importe quelle date entre les deux calendriers :" else "Bidirectional conversion between Islamic and Gregorian calendars with occasion detection:",
                        fontSize = 11.sp,
                        color = CelestialGrey
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    HijriDateConverterCardContent(viewModel = viewModel)

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = Color(0xFF2E3162))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Islamic Occasions Countdown Section
                    Text(
                        text = if (appLang == "AR") "المناسبات الإسلامية القادمة" else if (appLang == "FR") "Événements Islamiques" else "Upcoming Islamic Events",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureDawnLight
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (appLang == "AR") "جدول المواعيد السنوية للعبادات والمناسبات الكبرى مع عد تنازلي دقيق باليوم:" else if (appLang == "FR") "Les dates sacrées et jours restants avant les fêtes majeures :" else "Annual important dates and precision daily countdowns for major occasions:",
                        fontSize = 11.sp,
                        color = CelestialGrey
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val sortedOccasions = remember(activeTodayHijri) {
                        HijriCalendarHelper.occurrences.map { occ ->
                            val daysLeft = HijriCalendarHelper.calculateDaysUntilOccasion(activeTodayHijri, occ, hijriOffset)
                            occ to daysLeft
                        }.sortedBy { it.second }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        sortedOccasions.forEach { (occ, daysLeft) ->
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("occasion_row_${occ.id}"),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E203E)),
                                border = BorderStroke(0.5.dp, Color(0xFF2C2F5E)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                if (daysLeft == 0) GreenEmerald.copy(alpha = 0.15f) else Color(0xFF262850),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (daysLeft == 0) Icons.Default.EventAvailable else Icons.Default.Event,
                                            contentDescription = null,
                                            tint = if (daysLeft == 0) GreenEmerald else DeepSkyBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (appLang == "AR") occ.titleAr else occ.titleEn,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PureDawnLight
                                        )
                                        Text(
                                            text = if (appLang == "AR") "${occ.hijriDay} ${HijriCalendarHelper.getMonthNameAr(occ.hijriMonth)}" else "${occ.hijriDay} ${HijriCalendarHelper.getMonthNameEn(occ.hijriMonth)}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = GoldenSun
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (daysLeft == 0) GreenEmerald.copy(alpha = 0.2f) else Color(0xFF2E3162),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when {
                                                daysLeft == 0 -> if (appLang == "AR") "اليوم!" else if (appLang == "FR") "Aujourd'hui !" else "Today!"
                                                daysLeft == 1 -> if (appLang == "AR") "غداً" else if (appLang == "FR") "Demain" else "Tomorrow"
                                                else -> if (appLang == "AR") "متبقي $daysLeft يوم" else if (appLang == "FR") "Dans $daysLeft j" else "$daysLeft days left"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (daysLeft == 0) GreenEmerald else PureDawnLight
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrayerTrackerCard(viewModel: PrayerViewModel) {
    val context = LocalContext.current
    val todayRecord by viewModel.todayRecord.collectAsStateWithLifecycle()
    val perfectDaysCount by viewModel.perfectDaysCount.collectAsStateWithLifecycle()
    val recentRecords by viewModel.recentRecords.collectAsStateWithLifecycle()
    val appLang by viewModel.appLang.collectAsStateWithLifecycle()
    val t = { key: String -> Translation.get(key, appLang) }

    var showSunnah by remember { mutableStateOf(false) }

    val completedCount = todayRecord.completedCount
    val isAllDone = todayRecord.isAllCompleted
    val progressFraction = completedCount / 5f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("prayer_tracker_card"),
        colors = CardDefaults.cardColors(
            containerColor = if (isAllDone) Color(0xFF102626) else TwilightCard
        ),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            1.dp,
            if (isAllDone) GreenEmerald.copy(alpha = 0.6f) else Color(0xFF2E3162)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                if (isAllDone) GreenEmerald.copy(alpha = 0.2f) else Color(0xFF222448),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAllDone) Icons.Filled.Verified else Icons.Default.TaskAlt,
                            contentDescription = "Prayer Self Tracking",
                            tint = if (isAllDone) GreenEmerald else GoldenSun,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = t("prayer_tracker_title"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureDawnLight
                        )
                        Text(
                            text = t("prayers_completed_stat").replace("{count}", "$completedCount"),
                            fontSize = 12.sp,
                            color = if (isAllDone) GreenEmerald else DeepSkyBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Quick Toggle All Button
                TextButton(
                    onClick = {
                        val markAll = !isAllDone
                        viewModel.markAllPrayers(completed = markAll)
                        val msg = if (markAll) {
                            if (appLang == "AR") "تم تحديد جميع الصلوات كمؤداة ✓"
                            else if (appLang == "FR") "Toutes les prières marquées comme accomplies ✓"
                            else "All prayers marked as performed ✓"
                        } else {
                            if (appLang == "AR") "تمت إعادة تعيين الصلوات"
                            else if (appLang == "FR") "Prières réinitialisées"
                            else "Prayers reset"
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (isAllDone) CoralAlert else GoldenSun
                    ),
                    modifier = Modifier.testTag("btn_toggle_all_prayers")
                ) {
                    Text(
                        text = if (isAllDone) t("unmark_all") else t("mark_all_done"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Animated Progress Bar
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .testTag("prayer_progress_indicator"),
                color = if (isAllDone) GreenEmerald else GoldenSun,
                trackColor = Color(0xFF1E2140),
            )

            // Celebratory Banner when 5/5 completed
            AnimatedVisibility(
                visible = isAllDone,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F3A2E)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GreenEmerald.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = "Completed Badge",
                            tint = GoldenSun,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = t("all_prayers_completed"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureDawnLight,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5 Obligatory Prayer Chips Row
            val obligatoryPrayers = listOf(
                Triple("Fajr", t("Fajr"), todayRecord.fajrPrayed),
                Triple("Dhuhr", t("Dhuhr"), todayRecord.dhuhrPrayed),
                Triple("Asr", t("Asr"), todayRecord.asrPrayed),
                Triple("Maghrib", t("Maghrib"), todayRecord.maghribPrayed),
                Triple("Isha", t("Isha"), todayRecord.ishaPrayed)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                obligatoryPrayers.forEach { (prayerKey, label, isChecked) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .background(
                                if (isChecked) Color(0xFF0F4E38) else Color(0xFF1D2042),
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                if (isChecked) GreenEmerald else Color(0xFF2C3058),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                viewModel.togglePrayerChecked(prayerKey)
                            }
                            .testTag("tracker_chip_$prayerKey"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isChecked) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                                contentDescription = "$label Status",
                                tint = if (isChecked) GreenEmerald else CelestialGrey.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
                                color = if (isChecked) PureDawnLight else CelestialGrey
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Expandable Voluntary / Sunnah Section Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSunnah = !showSunnah }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = DeepSkyBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = t("sunnah_prayers"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepSkyBlue
                    )
                }

                Icon(
                    imageVector = if (showSunnah) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand Sunnah",
                    tint = CelestialGrey,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(
                visible = showSunnah,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val sunnahList = listOf(
                        Triple("duha", t("duha"), todayRecord.duhaPrayed),
                        Triple("qiyam", t("qiyam"), todayRecord.qiyamPrayed),
                        Triple("witr", t("witr"), todayRecord.witrPrayed)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sunnahList.forEach { (sunnahKey, label, isChecked) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .background(
                                        if (isChecked) Color(0xFF1B3A4B) else Color(0xFF171A38),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isChecked) DeepSkyBlue else Color(0xFF282B50),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        viewModel.togglePrayerChecked(sunnahKey)
                                    }
                                    .testTag("tracker_sunnah_$sunnahKey"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isChecked) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                                        contentDescription = label,
                                        tint = if (isChecked) DeepSkyBlue else CelestialGrey.copy(alpha = 0.5f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isChecked) PureDawnLight else CelestialGrey,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFF282B50))
            Spacer(modifier = Modifier.height(10.dp))

            // Consistency & Perfect Days Counter Footnote
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Perfect Days",
                        tint = GoldenSun,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = t("perfect_days_count").replace("{count}", "$perfectDaysCount"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PureDawnLight
                    )
                }

                // Show 7-day mini progress dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val last7Days = (0..6).map { offset ->
                        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }
                        sdf.format(cal.time)
                    }.reversed()

                    last7Days.forEach { dateStr ->
                        val record = recentRecords.firstOrNull { it.date == dateStr }
                        val isDayPerfect = record?.isAllCompleted == true
                        val hasSome = (record?.completedCount ?: 0) > 0

                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    when {
                                        isDayPerfect -> GreenEmerald
                                        hasSome -> GoldenSun
                                        else -> Color(0xFF2C3058)
                                    },
                                    CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QiblaTab(viewModel: PrayerViewModel, context: Context) {
    val qiblaCoralAlert = CoralAlert
    val qiblaCelestialGrey = CelestialGrey
    val qiblaGoldenSun = GoldenSun
    val qiblaGreenEmerald = GreenEmerald

    val customLat by viewModel.customLat.collectAsStateWithLifecycle()
    val customLon by viewModel.customLon.collectAsStateWithLifecycle()
    val customName by viewModel.customCityName.collectAsStateWithLifecycle()
    val customEnabled by viewModel.customLocEnabled.collectAsStateWithLifecycle()

    val currentCityIdx by viewModel.cityPresetIdx.collectAsStateWithLifecycle()
    val activeLat = if (customEnabled) customLat.toDouble() else CityPreset.presets[currentCityIdx].latitude
    val activeLon = if (customEnabled) customLon.toDouble() else CityPreset.presets[currentCityIdx].longitude
    val activeName = if (customEnabled) customName else CityPreset.presets[currentCityIdx].cityName

    val qiblaAngle = remember(activeLat, activeLon) {
        QiblaHelper.calculateQiblaDirection(activeLat, activeLon)
    }
    val distance = remember(activeLat, activeLon) {
        QiblaHelper.calculateDistanceToKaaba(activeLat, activeLon)
    }

    var sensorHeading by remember { mutableStateOf(0f) }
    var useManualDrag by remember { mutableStateOf(false) }
    var manualHeading by remember { mutableStateOf(0f) }

    val displayHeading = if (useManualDrag) manualHeading else sensorHeading

    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val hapticFeedback = LocalHapticFeedback.current

    DisposableEffect(useManualDrag) {
        if (useManualDrag) {
            return@DisposableEffect onDispose {}
        }
        var gravity = FloatArray(3)
        var geomagnetic = FloatArray(3)
        var hasGravity = false
        var hasGeomagnetic = false

        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                try {
                    if (event == null) return
                    if (useManualDrag) return

                    if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                        val rotationMatrix = FloatArray(9)
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(rotationMatrix, orientation)
                        val azimuthRad = orientation[0]
                        var azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
                        azimuthDeg = (azimuthDeg + 360f) % 360f
                        sensorHeading = azimuthDeg
                    } else {
                        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                            val copyLen = kotlin.math.min(event.values.size, gravity.size)
                            System.arraycopy(event.values, 0, gravity, 0, copyLen)
                            hasGravity = true
                        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                            val copyLen = kotlin.math.min(event.values.size, geomagnetic.size)
                            System.arraycopy(event.values, 0, geomagnetic, 0, copyLen)
                            hasGeomagnetic = true
                        }

                        if (hasGravity && hasGeomagnetic) {
                            val R = FloatArray(9)
                            val I = FloatArray(9)
                            if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                                val orientation = FloatArray(3)
                                SensorManager.getOrientation(R, orientation)
                                val azimuthRad = orientation[0]
                                var azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
                                azimuthDeg = (azimuthDeg + 360f) % 360f
                                sensorHeading = azimuthDeg
                            }
                        }
                    }
                } catch (t: Throwable) {
                    Log.e("QiblaTab", "Error handling sensor update", t)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotationVectorSensor != null) {
            sensorManager.registerListener(sensorListener, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val mag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            if (accel != null) {
                sensorManager.registerListener(sensorListener, accel, SensorManager.SENSOR_DELAY_UI)
            }
            if (mag != null) {
                sensorManager.registerListener(sensorListener, mag, SensorManager.SENSOR_DELAY_UI)
            }
        }

        onDispose {
            sensorManager.unregisterListener(sensorListener)
        }
    }

    val angleDiff = remember(qiblaAngle, displayHeading) {
        val diff = (qiblaAngle - displayHeading + 180.0) % 360.0 - 180.0
        kotlin.math.abs(diff)
    }
    val isAligned = angleDiff < 3.5f

    LaunchedEffect(isAligned) {
        if (isAligned) {
            try {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            } catch (e: Exception) {
                // Ignore missing hardware components gracefully
            }
        }
    }

    var compassCenter by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // Qibla Indicator card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("qibla_compass_card"),
                colors = CardDefaults.cardColors(containerColor = TwilightCard),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF2E3162))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "اتجاه القبلة الشريفة (Qibla Direction)".uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepSkyBlue,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = activeName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = PureDawnLight
                    )
                    Text(
                        text = "خط عرض: ${String.format("%.4f", activeLat)} | خط طول: ${String.format("%.4f", activeLon)}",
                        fontSize = 11.sp,
                        color = CelestialGrey
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = Color(0xFF2E3162))
                    Spacer(modifier = Modifier.height(18.dp))

                    // Compass Dial
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .align(Alignment.CenterHorizontally),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .onGloballyPositioned { coordinates ->
                                    val size = coordinates.size
                                    compassCenter = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                                }
                                .pointerInput(useManualDrag) {
                                    if (useManualDrag) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            val startPoint = change.position - dragAmount - compassCenter
                                            val endPoint = change.position - compassCenter
                                            val startAngle = kotlin.math.atan2(startPoint.y, startPoint.x)
                                            val endAngle = kotlin.math.atan2(endPoint.y, endPoint.x)
                                            val deltaAngle = Math.toDegrees((endAngle - startAngle).toDouble())
                                            manualHeading = ((manualHeading - deltaAngle.toFloat()) + 360f) % 360f
                                        }
                                    }
                                }
                                .testTag("qibla_compass_canvas")
                        ) {
                            val radius = size.minDimension / 2f
                            val center = size.center

                            // Outer main circle border
                            drawCircle(
                                color = if (isAligned) GreenEmerald else Color(0xFF2E3162),
                                radius = radius,
                                style = Stroke(width = 4.dp.toPx())
                            )

                            // Alignment glow highlight
                            if (isAligned) {
                                drawCircle(
                                    color = GreenEmerald.copy(alpha = 0.12f),
                                    radius = radius - 8.dp.toPx(),
                                    style = Fill
                                )
                            }

                            // Rotating compass dial elements
                            rotate(-displayHeading, center) {
                                // Compass Ticks every 30 degrees
                                for (angle in 0 until 360 step 30) {
                                    val angleRad = Math.toRadians(angle.toDouble())
                                    val tickLength = if (angle % 90 == 0) 14.dp.toPx() else 8.dp.toPx()
                                    val tickWidth = if (angle % 90 == 0) 3.dp.toPx() else 1.5.dp.toPx()
                                    val tickColor = if (angle == 0) qiblaCoralAlert else qiblaCelestialGrey

                                    val startX = (center.x + (radius - 12.dp.toPx() - tickLength) * kotlin.math.sin(angleRad)).toFloat()
                                    val startY = (center.y - (radius - 12.dp.toPx() - tickLength) * kotlin.math.cos(angleRad)).toFloat()
                                    val endX = (center.x + (radius - 12.dp.toPx()) * kotlin.math.sin(angleRad)).toFloat()
                                    val endY = (center.y - (radius - 12.dp.toPx()) * kotlin.math.cos(angleRad)).toFloat()

                                    drawLine(
                                        color = tickColor,
                                        start = androidx.compose.ui.geometry.Offset(startX, startY),
                                        end = androidx.compose.ui.geometry.Offset(endX, endY),
                                        strokeWidth = tickWidth
                                    )
                                }

                                // Draw dial cardinal letters N, S, E, W
                                val textPaint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.WHITE
                                    textSize = 14.sp.toPx()
                                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                                    textAlign = android.graphics.Paint.Align.CENTER
                                }

                                drawIntoCanvas { canvas ->
                                    val nativeCanvas = canvas.nativeCanvas

                                    // North
                                    textPaint.color = android.graphics.Color.parseColor("#EF4444")
                                    nativeCanvas.drawText("N", center.x, center.y - radius + 36.dp.toPx(), textPaint)

                                    // South
                                    textPaint.color = android.graphics.Color.WHITE
                                    nativeCanvas.drawText("S", center.x, center.y + radius - 24.dp.toPx(), textPaint)

                                    // East
                                    nativeCanvas.drawText("E", center.x + radius - 24.dp.toPx(), center.y + 5.dp.toPx(), textPaint)

                                    // West
                                    nativeCanvas.drawText("W", center.x - radius + 24.dp.toPx(), center.y + 5.dp.toPx(), textPaint)
                                }

                                // Rotate and draw golden Qibla indicator inside rotating dial (pointing at qiblaAngle relative to True North)
                                rotate(qiblaAngle.toFloat(), center) {
                                    val qiblaLineLength = radius * 0.72f

                                    // Pointer line
                                    drawLine(
                                        color = if (isAligned) qiblaGreenEmerald else qiblaGoldenSun,
                                        start = center,
                                        end = androidx.compose.ui.geometry.Offset(center.x, center.y - qiblaLineLength),
                                        strokeWidth = 4.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )

                                    // Pointed triangle arrow
                                    val path = Path().apply {
                                        moveTo(center.x, center.y - qiblaLineLength - 6.dp.toPx())
                                        lineTo(center.x - 10.dp.toPx(), center.y - qiblaLineLength + 14.dp.toPx())
                                        lineTo(center.x + 10.dp.toPx(), center.y - qiblaLineLength + 14.dp.toPx())
                                        close()
                                    }
                                    drawPath(
                                        path = path,
                                        color = if (isAligned) qiblaGreenEmerald else qiblaGoldenSun,
                                        style = Fill
                                    )

                                    // Draw Mini Kaaba cube at 60% distance
                                    val cubeSize = 22.dp.toPx()
                                    val cubeY = center.y - qiblaLineLength * 0.6f

                                    // Draw Kaaba shadow
                                    drawRect(
                                        color = Color.Black.copy(alpha = 0.3f),
                                        topLeft = androidx.compose.ui.geometry.Offset(center.x - cubeSize / 2f + 2.dp.toPx(), cubeY - cubeSize / 2f + 2.dp.toPx()),
                                        size = androidx.compose.ui.geometry.Size(cubeSize, cubeSize)
                                    )
                                    // Black cube body
                                    drawRect(
                                        color = Color(0xFF101115),
                                        topLeft = androidx.compose.ui.geometry.Offset(center.x - cubeSize / 2f, cubeY - cubeSize / 2f),
                                        size = androidx.compose.ui.geometry.Size(cubeSize, cubeSize)
                                    )
                                    // Kiswah status line
                                    drawRect(
                                        color = qiblaGoldenSun,
                                        topLeft = androidx.compose.ui.geometry.Offset(center.x - cubeSize / 2f, cubeY - cubeSize * 0.15f),
                                        size = androidx.compose.ui.geometry.Size(cubeSize, cubeSize * 0.15f)
                                    )
                                }
                            }

                            // 4. Draw static forward-pointing overlay (phone top target)
                            val targetPath = Path().apply {
                                moveTo(center.x, 6.dp.toPx())
                                lineTo(center.x - 10.dp.toPx(), 20.dp.toPx())
                                lineTo(center.x + 10.dp.toPx(), 20.dp.toPx())
                                close()
                            }
                            drawPath(
                                path = targetPath,
                                color = if (isAligned) qiblaGreenEmerald else qiblaCoralAlert,
                                style = Fill
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Alignment State Panel
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAligned) GreenEmerald.copy(alpha = 0.15f) else Color(0xFF222448)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isAligned) GreenEmerald else Color(0xFF2E3162))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (isAligned) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Aligned",
                                        tint = GreenEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "متطابق مع القبلة! (Facing Kaaba)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GreenEmerald
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "هاتفكم موجه الآن باتجاه القبلة المشرفة بدقة.",
                                    fontSize = 11.sp,
                                    color = PureDawnLight
                                )
                            } else {
                                val directionText = if ((qiblaAngle - displayHeading) >= 0) "اليمين (Right)" else "اليسار (Left)"
                                Text(
                                    text = "ضع الهاتف مستوياً، ثم أدره حتى تتطابق الإشارة",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = CelestialGrey
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "أدر هاتفك باتجاه: $directionText بمقدار ${String.format("%.1f", angleDiff)}°",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldenSun
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stat Indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("زاوية القبلة (Azimuth)", fontSize = 10.sp, color = CelestialGrey)
                            Text("${String.format("%.1f", qiblaAngle)}°", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PureDawnLight)
                        }
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF2E3162)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("انحراف الهاتف (Heading)", fontSize = 10.sp, color = CelestialGrey)
                            Text("${String.format("%.1f", displayHeading)}°", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PureDawnLight)
                        }
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF2E3162)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("المسافة للكعبة (Distance)", fontSize = 10.sp, color = CelestialGrey)
                            Text("${String.format("%,.0f", distance)} كم", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PureDawnLight)
                        }
                    }
                }
            }
        }

        // Adjustments & Simulation Options
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("qibla_compass_simulator_card"),
                colors = CardDefaults.cardColors(containerColor = TwilightCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF2E3162))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CompassCalibration,
                            contentDescription = "Calibration Settings",
                            tint = GoldenSun,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "نمط المحاكاة التفاعلية (Interactive Simulator)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureDawnLight
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "تفادياً لعدم وجود مستشعرات البوصلة في المحاكيات أو بعض الأجهزة، قم بتفعيل الدوران اليدوي لتدوير البوصلة بيدك واختبار مدى تطابقها ميكانيكياً.",
                        fontSize = 11.sp,
                        color = CelestialGrey
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تفعيل الدوران اليدوي باللمس:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureDawnLight
                        )
                        Switch(
                            checked = useManualDrag,
                            onCheckedChange = { useManualDrag = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = GoldenSun,
                                checkedTrackColor = GoldenSun.copy(alpha = 0.4f),
                                uncheckedThumbColor = CelestialGrey,
                                uncheckedTrackColor = Color(0xFF222448)
                            ),
                            modifier = Modifier.testTag("switch_manual_compass_drag")
                        )
                    }

                    if (useManualDrag) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "يمكنك سحب/تدوير البوصلة مباشرة بإصبعك على شاشة البوصلة بالأعلى، أو تدويرها باستخدام شريط السحب أدناه:",
                            fontSize = 11.sp,
                            color = CelestialGrey
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Slider(
                            value = manualHeading,
                            onValueChange = { manualHeading = it },
                            valueRange = 0f..360f,
                            colors = SliderDefaults.colors(
                                thumbColor = GoldenSun,
                                activeTrackColor = GoldenSun,
                                inactiveTrackColor = Color(0xFF222448)
                            ),
                            modifier = Modifier.testTag("slider_manual_compass_heading")
                        )
                    }
                }
            }
        }
    }
}

fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        var cursor: android.database.Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, null, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } catch (e: Throwable) {
            // ignore
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}
