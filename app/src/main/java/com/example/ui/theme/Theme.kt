package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

// CompositionLocals to allow components to easily query the active theme and Islamic Art mode
val LocalAppTheme = compositionLocalOf { "islamic_art" }
val LocalIsIslamicArtTheme = compositionLocalOf { false }

// 1. Light Theme (Soft Dawn & Luminous Sun)
val LightColorScheme = lightColorScheme(
    primary = Color(0xFFB45309),         // Rich warm amber gold
    onPrimary = Color.White,
    secondary = Color(0xFF0284C7),       // Sky azure blue highlight
    onSecondary = Color.White,
    tertiary = Color(0xFFC2410C),        // Terracotta sunset bronze
    onTertiary = Color.White,
    background = Color(0xFFFAF8F5),      // Warm soft cream canvas
    onBackground = Color(0xFF1E293B),    // Crisp dark slate text
    surface = Color(0xFFFFFFFF),         // Clean elevated white card
    onSurface = Color(0xFF0F172A),       // Deep slate charcoal text
    primaryContainer = Color(0xFFFEF3C7),// Soft amber container
    onPrimaryContainer = Color(0xFF92400E),
    surfaceVariant = Color(0xFFF1ECE1),  // Soft neutral warm container
    onSurfaceVariant = Color(0xFF64748B),// Muted slate
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2D9CC),  // Warm subtle card border
    error = Color(0xFFEF4444)
)

// 2. Dark Theme (Midnight Space & Twilight Serenity)
val DarkColorScheme = darkColorScheme(
    primary = StaticGoldenSun,           // Saturated golden dawn sun
    onPrimary = Color(0xFF0B0D1B),
    secondary = StaticDeepSkyBlue,       // Shimmering cyan highlight
    onSecondary = Color(0xFF0B0D1B),
    tertiary = StaticWarmBronze,         // Warm bronze tone
    onTertiary = Color.White,
    background = Color(0xFF0B0D1B),      // Midnight space indigo
    onBackground = StaticPureDawnLight,  // Starry white text
    surface = Color(0xFF161832),         // Deep navy twilight card
    onSurface = StaticPureDawnLight,
    primaryContainer = Color(0xFF22254B),// Cozy translucent container
    onPrimaryContainer = StaticGoldenSun,
    surfaceVariant = Color(0xFF1E2145),
    onSurfaceVariant = StaticCelestialGrey,
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF282B52),  // Crisp dark card border
    error = StaticCoralAlert
)

// 3. Custom 'Islamic Art' Theme (Warm Andalusian Ochre, Royal Gold & Lapis with Ornate Filigree)
val IslamicArtColorScheme = darkColorScheme(
    primary = Color(0xFFD4AF37),         // Royal Islamic Gold (Khatim gilding)
    onPrimary = Color(0xFF18110D),       // Deep Moroccan Mahogany
    secondary = Color(0xFF3B82F6),       // Lapis Lazuli Blue
    onSecondary = Color.White,
    tertiary = Color(0xFFC2410C),        // Terracotta / Andalusian Ochre
    onTertiary = Color.White,
    background = Color(0xFF18110D),      // Deep Moroccan Espresso & Sandstone canvas
    onBackground = Color(0xFFFDF8F0),    // Illuminated Gold-tinted cream text
    surface = Color(0xFF281D15),         // Warm Carved Wood / Ornate Parchment card
    onSurface = Color(0xFFFDF8F0),
    primaryContainer = Color(0xFF3E2C1E),// Antique gold/mahogany container
    onPrimaryContainer = Color(0xFFFDE047),
    surfaceVariant = Color(0xFF35251B),  // Warm secondary container
    onSurfaceVariant = Color(0xFFD7C4B0),// Sandstone beige
    outline = Color(0xFFB45309),
    outlineVariant = Color(0xFF6B4524),  // Warm antique gold/copper filigree border
    error = Color(0xFFF87171)
)

// 4. Light Sky Blue Theme (Airy Azure & Serene Sky)
val SkyBlueColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),         // Vibrant sky azure blue
    onPrimary = Color.White,
    secondary = Color(0xFF0EA5E9),       // Luminous sky blue accent
    onSecondary = Color.White,
    tertiary = Color(0xFF0369A1),        // Deep cerulean blue
    onTertiary = Color.White,
    background = Color(0xFFF0F9FF),      // Airy pastel sky-blue canvas
    onBackground = Color(0xFF0C2D48),    // Deep ocean navy text
    surface = Color(0xFFFFFFFF),         // Crisp luminous white card
    onSurface = Color(0xFF0C2D48),
    primaryContainer = Color(0xFFE0F2FE),// Soft sky-blue container
    onPrimaryContainer = Color(0xFF0369A1),
    surfaceVariant = Color(0xFFE0F2FE),  // Soft sky-blue secondary surface
    onSurfaceVariant = Color(0xFF475569),// Slate grey secondary text
    outline = Color(0xFF7DD3FC),
    outlineVariant = Color(0xFFBAE6FD),  // Subtle light sky-blue card border
    error = Color(0xFFEF4444)
)

@Composable
fun MyApplicationTheme(
    themeMode: String = "islamic_art",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = isSystemInDarkTheme()
    val effectiveTheme = when (themeMode) {
        "light", "dawn" -> "light"
        "sky_blue", "bleu_ciel" -> "sky_blue"
        "dark", "twilight", "shadow" -> "dark"
        "islamic_art" -> "islamic_art"
        "system" -> if (darkTheme) "dark" else "light"
        else -> "islamic_art"
    }

    val colorScheme = when (effectiveTheme) {
        "light" -> LightColorScheme
        "sky_blue" -> SkyBlueColorScheme
        "dark" -> DarkColorScheme
        "islamic_art" -> IslamicArtColorScheme
        else -> IslamicArtColorScheme
    }

    val isIslamicArt = effectiveTheme == "islamic_art"

    CompositionLocalProvider(
        LocalAppTheme provides effectiveTheme,
        LocalIsIslamicArtTheme provides isIslamicArt
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

