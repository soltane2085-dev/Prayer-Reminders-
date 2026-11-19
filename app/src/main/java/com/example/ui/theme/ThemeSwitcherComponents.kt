package com.example.ui.theme

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.translation.Translation
import com.example.viewmodel.PrayerViewModel

data class ThemeModel(
    val id: String,
    val titleKey: String,
    val descKey: String,
    val icon: ImageVector,
    val bgGradient: List<Color>,
    val surfaceColor: Color,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color,
    val isIslamicPattern: Boolean = false
)

val AvailableThemes = listOf(
    ThemeModel(
        id = "islamic_art",
        titleKey = "theme_islamic_art",
        descKey = "theme_islamic_art_desc",
        icon = Icons.Default.AutoAwesome,
        bgGradient = listOf(Color(0xFF261A13), Color(0xFF18110D)),
        surfaceColor = Color(0xFF35251B),
        primaryColor = Color(0xFFD4AF37),
        secondaryColor = Color(0xFF3B82F6),
        accentColor = Color(0xFFC2410C),
        isIslamicPattern = true
    ),
    ThemeModel(
        id = "light",
        titleKey = "theme_light",
        descKey = "theme_light_desc",
        icon = Icons.Default.WbSunny,
        bgGradient = listOf(Color(0xFFFAF8F5), Color(0xFFF1ECE1)),
        surfaceColor = Color(0xFFFFFFFF),
        primaryColor = Color(0xFFB45309),
        secondaryColor = Color(0xFF0284C7),
        accentColor = Color(0xFFD97706),
        isIslamicPattern = false
    ),
    ThemeModel(
        id = "sky_blue",
        titleKey = "theme_sky_blue",
        descKey = "theme_sky_blue_desc",
        icon = Icons.Default.Cloud,
        bgGradient = listOf(Color(0xFFE0F2FE), Color(0xFFF0F9FF)),
        surfaceColor = Color(0xFFFFFFFF),
        primaryColor = Color(0xFF0284C7),
        secondaryColor = Color(0xFF38BDF8),
        accentColor = Color(0xFF0369A1),
        isIslamicPattern = false
    ),
    ThemeModel(
        id = "dark",
        titleKey = "theme_dark",
        descKey = "theme_dark_desc",
        icon = Icons.Default.DarkMode,
        bgGradient = listOf(Color(0xFF141733), Color(0xFF0B0D1B)),
        surfaceColor = Color(0xFF1C1F42),
        primaryColor = Color(0xFFF59E0B),
        secondaryColor = Color(0xFF38BDF8),
        accentColor = Color(0xFFD97706),
        isIslamicPattern = false
    )
)

/**
 * Full Theme Switcher Card for ConfigureTab (Settings).
 */
@Composable
fun ThemeSwitcherCard(
    currentThemeId: String,
    onThemeSelect: (String) -> Unit,
    appLang: String,
    modifier: Modifier = Modifier
) {
    val t = { key: String -> Translation.get(key, appLang) }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val isIslamicArt = LocalIsIslamicArtTheme.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("theme_switcher_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(
            1.dp,
            if (isIslamicArt) Color(0xFFD4AF37).copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isIslamicArt) {
                            RubElHizbStarIcon(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeColor = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = t("theme_section_title"),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = t("theme_switcher_desc"),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            if (isIslamicArt) {
                IslamicDecorativeDivider(
                    modifier = Modifier.padding(vertical = 2.dp),
                    color = Color(0xFFD4AF37).copy(alpha = 0.5f)
                )
            } else {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            // Theme Options Tiles
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AvailableThemes.forEach { theme ->
                    val isSelected = currentThemeId == theme.id ||
                            (currentThemeId == "twilight" && theme.id == "dark") ||
                            (currentThemeId == "dawn" && theme.id == "light") ||
                            (currentThemeId == "bleu_ciel" && theme.id == "sky_blue") ||
                            (currentThemeId == "shadow" && theme.id == "dark")

                    ThemeOptionTile(
                        theme = theme,
                        isSelected = isSelected,
                        title = t(theme.titleKey),
                        desc = t(theme.descKey),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onThemeSelect(theme.id)
                            val themeName = t(theme.titleKey)
                            Toast.makeText(
                                context,
                                Translation.get("theme_applied_toast", appLang).replace("{name}", themeName),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }

            // System Default Option
            val isSystemSelected = currentThemeId == "system"
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onThemeSelect("system")
                        Toast.makeText(
                            context,
                            Translation.get("theme_applied_toast", appLang).replace("{name}", t("theme_system")),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .testTag("theme_option_system"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSystemSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    1.dp,
                    if (isSystemSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SettingsBrightness,
                            contentDescription = null,
                            tint = if (isSystemSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = t("theme_system"),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = t("theme_system_desc"),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    RadioButton(
                        selected = isSystemSelected,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onThemeSelect("system")
                        },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    }
}

/**
 * Individual visual theme tile with live mini-canvas swatch and pattern preview.
 */
@Composable
private fun ThemeOptionTile(
    theme: ThemeModel,
    isSelected: Boolean,
    title: String,
    desc: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("theme_option_${theme.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) theme.primaryColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Visual Palette & Pattern Preview Box
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.verticalGradient(theme.bgGradient))
                    .border(1.dp, theme.primaryColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Background pattern for Islamic Art
                if (theme.isIslamicPattern) {
                    IslamicGeometricBackground(
                        patternColor = theme.primaryColor,
                        patternAlpha = 0.35f,
                        gridSizeDp = 20.dp
                    )
                }

                // Inner badge / icon
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(theme.surfaceColor)
                        .border(1.dp, theme.primaryColor.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (theme.isIslamicPattern) {
                        RubElHizbStarIcon(
                            modifier = Modifier.size(16.dp),
                            color = theme.primaryColor,
                            strokeColor = theme.primaryColor
                        )
                    } else {
                        Icon(
                            imageVector = theme.icon,
                            contentDescription = null,
                            tint = theme.primaryColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Text Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    if (theme.isIslamicPattern) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = theme.primaryColor.copy(alpha = 0.2f),
                            border = BorderStroke(0.5.dp, theme.primaryColor.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "★ Featured",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.primaryColor,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 14.sp
                )

                // Swatch Dots
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(theme.primaryColor))
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(theme.secondaryColor))
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(theme.accentColor))
                }
            }

            // Radio Button / Active check
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = theme.primaryColor
                )
            )
        }
    }
}

/**
 * Compact Quick Theme Toggle Pill for Top Bars / Headers.
 * Allows quick cycling: Light -> Dark -> Islamic Art on tap, or opens full dialog on long press.
 */
@Composable
fun QuickThemeTogglePill(
    currentThemeId: String,
    onThemeSelect: (String) -> Unit,
    appLang: String,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val isIslamicArt = LocalIsIslamicArtTheme.current
    val t = { key: String -> Translation.get(key, appLang) }

    if (showDialog) {
        ThemeSelectionDialog(
            currentThemeId = currentThemeId,
            onThemeSelect = {
                onThemeSelect(it)
                showDialog = false
            },
            onDismiss = { showDialog = false },
            appLang = appLang
        )
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable {
                // Fast cycle on tap: Islamic Art -> Light -> Sky Blue -> Dark -> Islamic Art
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val nextTheme = when (currentThemeId) {
                    "islamic_art" -> "light"
                    "light", "dawn" -> "sky_blue"
                    "sky_blue", "bleu_ciel" -> "dark"
                    "dark", "twilight", "shadow" -> "islamic_art"
                    else -> "islamic_art"
                }
                onThemeSelect(nextTheme)
            }
            .testTag("quick_theme_toggle_btn"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        border = BorderStroke(
            1.dp,
            if (isIslamicArt) Color(0xFFD4AF37).copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            when {
                isIslamicArt -> {
                    RubElHizbStarIcon(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeColor = MaterialTheme.colorScheme.primary
                    )
                }
                currentThemeId == "sky_blue" || currentThemeId == "bleu_ciel" -> {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = "Light Sky Blue Theme",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                currentThemeId == "light" || currentThemeId == "dawn" -> {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = "Light Theme",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.DarkMode,
                        contentDescription = "Dark Theme",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = when {
                    isIslamicArt -> if (appLang == "AR") "فن إسلامي" else if (appLang == "FR") "Art Islamique" else "Islamic Art"
                    currentThemeId == "sky_blue" || currentThemeId == "bleu_ciel" -> if (appLang == "AR") "سماوي فاتح" else if (appLang == "FR") "Bleu Ciel" else "Sky Blue"
                    currentThemeId == "light" || currentThemeId == "dawn" -> if (appLang == "AR") "نهاري" else if (appLang == "FR") "Clair" else "Light"
                    else -> if (appLang == "AR") "ليلي" else if (appLang == "FR") "Sombre" else "Dark"
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Modal Theme Picker Dialog.
 */
@Composable
fun ThemeSelectionDialog(
    currentThemeId: String,
    onThemeSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    appLang: String
) {
    val t = { key: String -> Translation.get(key, appLang) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = t("theme_section_title"),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                AvailableThemes.forEach { theme ->
                    val isSelected = currentThemeId == theme.id
                    ThemeOptionTile(
                        theme = theme,
                        isSelected = isSelected,
                        title = t(theme.titleKey),
                        desc = t(theme.descKey),
                        onClick = {
                            onThemeSelect(theme.id)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (appLang == "AR") "إغلاق" else if (appLang == "FR") "Fermer" else "Close",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
