package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import com.example.viewmodel.PrayerViewModel

@Composable
fun QuranAzkarTab(
    viewModel: PrayerViewModel,
    context: Context
) {
    var currentSubTab by remember { mutableIntStateOf(0) } // 0 = Quran, 1 = Azkar
    val sharedPrefs = remember { context.getSharedPreferences("quran_azkar_prefs", Context.MODE_PRIVATE) }

    // Navigation and state inside Quran reader
    var selectedSurah by remember { mutableStateOf<Surah?>(null) }
    var bookmarkSurahId by remember { mutableIntStateOf(sharedPrefs.getInt("bookmarked_surah_id", -1)) }
    var bookmarkSurahName by remember { mutableStateOf(sharedPrefs.getString("bookmarked_surah_name", "") ?: "") }

    // Active Category for Azkar
    var activeAzkarCategory by remember { mutableStateOf<AzkarCategory?>(null) }

    AnimatedContent(
        targetState = selectedSurah != null || activeAzkarCategory != null,
        transitionSpec = {
            slideInHorizontally(initialOffsetX = { it }) + fadeIn() togetherWith
                    slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
        },
        label = "tab_navigation"
    ) { isSubViewActive ->
        if (selectedSurah != null) {
            // Quran Reader View
            val surah = selectedSurah!!
            QuranReaderView(
                surah = surah,
                onBack = { selectedSurah = null },
                isBookmarked = bookmarkSurahId == surah.id,
                onToggleBookmark = {
                    if (bookmarkSurahId == surah.id) {
                        bookmarkSurahId = -1
                        bookmarkSurahName = ""
                        sharedPrefs.edit().remove("bookmarked_surah_id").remove("bookmarked_surah_name").apply()
                        Toast.makeText(context, "تم إزالة العلامة المرجعية", Toast.LENGTH_SHORT).show()
                    } else {
                        bookmarkSurahId = surah.id
                        bookmarkSurahName = surah.name
                        sharedPrefs.edit()
                            .putInt("bookmarked_surah_id", surah.id)
                            .putString("bookmarked_surah_name", surah.name)
                            .apply()
                        Toast.makeText(context, "تم حفظ العلامة المرجعية (سورة ${surah.name})", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        } else if (activeAzkarCategory != null) {
            // Detailed Azkar List View
            AzkarCategoryDetailView(
                category = activeAzkarCategory!!,
                onBack = { activeAzkarCategory = null }
            )
        } else {
            // Main Dashboard View (Tabs: Quran & Azkar list)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Large Header Title
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "القرآن الكريم والأذكار",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldenSun,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Normal,
                            modifier = Modifier.testTag("quran_azkar_header")
                        )
                        Text(
                            text = "قرآن يتلى، وأذكار تحفظك طوال يومك",
                            fontSize = 12.sp,
                            color = CelestialGrey
                        )
                    }
                }

                // Sub-Tab Switcher (Custom Beautiful Pill Switcher)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TwilightCard, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (currentSubTab == 0) DeepSkyBlue else Color.Transparent)
                                .clickable { currentSubTab = 0 }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "القرآن الكريم",
                                color = if (currentSubTab == 0) TwilightBackground else PureDawnLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (currentSubTab == 1) DeepSkyBlue else Color.Transparent)
                                .clickable { currentSubTab = 1 }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "حصن المسلم والأذكار",
                                color = if (currentSubTab == 1) TwilightBackground else PureDawnLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                if (currentSubTab == 0) {
                    // --- QURAN TAB ---

                    // Highlight Bookmark is Saved
                    if (bookmarkSurahId != -1) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val match = QuranProvider.surahs.firstOrNull { it.id == bookmarkSurahId }
                                        if (match != null) {
                                            selectedSurah = match
                                        }
                                    }
                                    .testTag("bookmark_resume_card"),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2417)),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, GoldenSun)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(Color(0xFF3E3019), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bookmark,
                                            contentDescription = "Bookmark Logo",
                                            tint = GoldenSun,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "متابعة القراءة المرجعية",
                                            fontSize = 11.sp,
                                            color = GoldenSun,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "سورة $bookmarkSurahName",
                                            fontSize = 15.sp,
                                            color = PureDawnLight,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "Go",
                                        tint = GoldenSun,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "قائمة السور المتاحة".uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = CelestialGrey,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                        )
                    }

                    // Surah List
                    items(QuranProvider.surahs) { surah ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSurah = surah }
                                .testTag("surah_card_${surah.id}"),
                            colors = CardDefaults.cardColors(containerColor = TwilightCard),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFF25274A))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Surah Number circular badge
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFF1E2145), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = surah.id.toString(),
                                        fontSize = 12.sp,
                                        color = DeepSkyBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                // Informational Details
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = surah.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PureDawnLight
                                    )
                                    Text(
                                        text = "${surah.englishName} • ${surah.revelationType}",
                                        fontSize = 12.sp,
                                        color = CelestialGrey
                                    )
                                }

                                // Verses Count and visual arrow
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "${surah.verses.size} آيات",
                                        fontSize = 11.sp,
                                        color = GoldenSun,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (bookmarkSurahId == surah.id) {
                                            Icon(
                                                imageVector = Icons.Default.Bookmark,
                                                contentDescription = "Bookmarked",
                                                tint = GoldenSun,
                                                modifier = Modifier.size(16.dp).padding(end = 4.dp)
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Read",
                                            tint = CelestialGrey,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // --- AZKAR TAB ---
                    item {
                        Text(
                            text = "تصنيفات الأذكار اليومية".uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = CelestialGrey,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                        )
                    }

                    items(AzkarProvider.categories) { category ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { activeAzkarCategory = category }
                                .testTag("azkar_cat_${category.id}"),
                            colors = CardDefaults.cardColors(containerColor = TwilightCard),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFF25274A))
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Category Icon
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(
                                            when (category.id) {
                                                "sabah" -> Color(0xFF332A15)
                                                "masaa" -> Color(0xFF15193B)
                                                "post_prayer" -> Color(0xFF122C24)
                                                else -> Color(0xFF241530)
                                            },
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (category.id) {
                                            "sabah" -> Icons.Default.WbSunny
                                            "masaa" -> Icons.Default.NightsStay
                                            "post_prayer" -> Icons.Default.CheckCircle
                                            else -> Icons.Default.Bedtime
                                        },
                                        contentDescription = category.titleAr,
                                        tint = when (category.id) {
                                            "sabah" -> GoldenSun
                                            "masaa" -> DeepSkyBlue
                                            "post_prayer" -> GreenEmerald
                                            else -> Color(0xFFBD93F9)
                                        },
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = category.titleAr,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PureDawnLight
                                    )
                                    Text(
                                        text = "${category.titleEn} • ${category.items.size} ذِكْر",
                                        fontSize = 12.sp,
                                        color = CelestialGrey
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Open",
                                    tint = CelestialGrey,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun QuranReaderView(
    surah: Surah,
    onBack: () -> Unit,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit
) {
    var arabicFontSize by remember { mutableFloatStateOf(22f) }
    var showEnglishTranslation by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TwilightBackground)
    ) {
        // Styled Reading App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(TwilightCard, CircleShape)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = PureDawnLight
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "سورة ${surah.name}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureDawnLight
                )
                Text(
                    text = surah.englishName,
                    fontSize = 11.sp,
                    color = CelestialGrey
                )
            }

            // Top Quick settings (Bookmark & translation toggle)
            IconButton(
                onClick = onToggleBookmark,
                modifier = Modifier
                    .background(
                        if (isBookmarked) Color(0xFF332A15) else TwilightCard,
                        CircleShape
                    )
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = if (isBookmarked) GoldenSun else PureDawnLight,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Adjustable Controls Panel (Card at the top for reading tools)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = TwilightCard),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF202242))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Translation toggle switcher text
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.clickable { showEnglishTranslation = !showEnglishTranslation }
                ) {
                    Checkbox(
                        checked = showEnglishTranslation,
                        onCheckedChange = { showEnglishTranslation = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = DeepSkyBlue,
                            uncheckedColor = CelestialGrey,
                            checkmarkColor = TwilightBackground
                        )
                    )
                    Text("الترجمة الإنجليزية", color = PureDawnLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Font Resizer controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("حجم الخط", color = CelestialGrey, fontSize = 11.sp)
                    FilledIconButton(
                        onClick = { if (arabicFontSize > 16) arabicFontSize -= 3 },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF282A4C)),
                        modifier = Modifier.size(28.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("A-", color = PureDawnLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "${arabicFontSize.toInt()}sp",
                        fontSize = 11.sp,
                        color = GoldenSun,
                        fontWeight = FontWeight.Bold
                    )
                    FilledIconButton(
                        onClick = { if (arabicFontSize < 40) arabicFontSize += 3 },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF282A4C)),
                        modifier = Modifier.size(28.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("A+", color = PureDawnLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Quran Verses Feed
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sura Banner Bismillah (except for Al-Fatihah which already contains bismillah as verse 1)
            if (surah.id != 1) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldenSun,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            items(surah.verses.size) { index ->
                val arText = surah.verses[index]
                val enText = if (index < surah.englishVerses.size) surah.englishVerses[index] else ""

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1023)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF1B1D38))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        // Verse number badge layout
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF23254A), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "آية ${index + 1}",
                                    fontSize = 10.sp,
                                    color = DeepSkyBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // High diacritic Arabic verse
                        Text(
                            text = arText,
                            fontSize = arabicFontSize.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureDawnLight,
                            textAlign = TextAlign.Right,
                            lineHeight = (arabicFontSize * 1.5).sp,
                            modifier = Modifier.fillMaxWidth().testTag("quran_verse_${surah.id}_$index")
                        )

                        // English translation toggle
                        if (showEnglishTranslation && enText.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color(0xFF202242), thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = enText,
                                fontSize = 13.sp,
                                color = CelestialGrey,
                                textAlign = TextAlign.Left,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
                Text(
                    text = "صَدَقَ اللهُ الْعَظِيمُ",
                    fontSize = 18.sp,
                    color = GoldenSun,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

@Composable
fun AzkarCategoryDetailView(
    category: AzkarCategory,
    onBack: () -> Unit
) {
    // Dynamic mutable counts tracking state. We initialize a map holding item index to its current remaining count
    val remainingCounts = remember(category) {
        mutableStateMapOf<Int, Int>().apply {
            category.items.forEachIndexed { index, item ->
                put(index, item.count)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TwilightBackground)
    ) {
        // Header Category Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(TwilightCard, CircleShape)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = PureDawnLight
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.titleAr,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureDawnLight
                )
                Text(
                    text = category.titleEn,
                    fontSize = 11.sp,
                    color = CelestialGrey
                )
            }

            // Global Reset category counts button
            IconButton(
                onClick = {
                    category.items.forEachIndexed { i, item ->
                        remainingCounts[i] = item.count
                    }
                },
                modifier = Modifier
                    .background(TwilightCard, CircleShape)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset All",
                    tint = GoldenSun,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Scrollable Lists of Azkar Item Cards
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(category.items.size) { index ->
                val item = category.items[index]
                val left = remainingCounts[index] ?: item.count
                val isCompleted = left <= 0

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            if (left > 0) {
                                remainingCounts[index] = left - 1
                            }
                        }
                        .testTag("azkar_card_${category.id}_$index"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCompleted) Color(0xFF0F1E19) else TwilightCard
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isCompleted) GreenEmerald else Color(0xFF25274A)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Badge at top of the Azkar card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isCompleted) GreenEmerald else Color(0xFF282A4C),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isCompleted) "مكتمل ✅" else "التكرار: $left / ${item.count}",
                                    fontSize = 10.sp,
                                    color = if (isCompleted) TwilightBackground else GoldenSun,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Single item local reset button
                            if (left != item.count) {
                                IconButton(
                                    onClick = { remainingCounts[index] = item.count },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Undo,
                                        contentDescription = "Reset Item",
                                        tint = CelestialGrey,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Beautiful large Arabic content text
                        Text(
                            text = item.text,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureDawnLight,
                            textAlign = TextAlign.Right,
                            lineHeight = 28.sp,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // English phonetic/meaning
                        if (item.translation.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = item.translation,
                                fontSize = 12.sp,
                                color = CelestialGrey,
                                textAlign = TextAlign.Left,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Benefit/Rewards of saying the Azkar
                        if (item.benefit.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isCompleted) Color(0xFF132F23) else Color(0xFF1A1F3D),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Benefit Info",
                                        tint = if (isCompleted) GreenEmerald else DeepSkyBlue,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = item.benefit,
                                        fontSize = 11.sp,
                                        color = if (isCompleted) GreenEmerald else CelestialGrey,
                                        lineHeight = 16.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Large satisfaction-driven countdown Tapping Button inside card
                        Button(
                            onClick = {
                                if (left > 0) {
                                    remainingCounts[index] = left - 1
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCompleted) GreenEmerald.copy(alpha = 0.1f) else Color(0xFF1E2145)
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isCompleted) GreenEmerald else DeepSkyBlue
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Done Check",
                                    tint = GreenEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تقبل الله منك", color = GreenEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Text(
                                    text = "اضغط للتكرار الحالي (متبقي: $left)",
                                    color = DeepSkyBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}
