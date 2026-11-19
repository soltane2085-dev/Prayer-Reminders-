package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

// Static Twilight Serenity Color Palette for Theme definition
val StaticTwilightBackground = Color(0xFF0F1021)   // Solid rich midnight sky deep space
val StaticTwilightCard = Color(0xFF1A1C36)         // Cozy translucent dark cards
val StaticGoldenSun = Color(0xFFF59E0B)            // Saturated gold dawn sky accent
val StaticWarmBronze = Color(0xFFD97706)           // Supporting sunset orange tone
val StaticDeepSkyBlue = Color(0xFF38BDF8)          // Starry glowing cyan highlights
val StaticPureDawnLight = Color(0xFFF8FAFC)        // Off-white bright readable text
val StaticCelestialGrey = Color(0xFF94A3B8)        // Soft muted celestial grey
val StaticCoralAlert = Color(0xFFF87171)           // Red-pink soft alert for mute states
val StaticGreenEmerald = Color(0xFF10B981)         // Beautiful islamic green for prayed states
val StaticIslamicGold = Color(0xFFD4AF37)          // Royal Islamic Art Gold

// Dynamic, theme-aware getters used throughout the screens automatically
val TwilightBackground: Color
    @Composable
    get() = MaterialTheme.colorScheme.background

val TwilightCard: Color
    @Composable
    get() = MaterialTheme.colorScheme.surface

val CardBorderColor: Color
    @Composable
    get() = MaterialTheme.colorScheme.outlineVariant

val GoldenSun: Color
    @Composable
    get() = MaterialTheme.colorScheme.primary

val WarmBronze: Color
    @Composable
    get() = MaterialTheme.colorScheme.tertiary

val DeepSkyBlue: Color
    @Composable
    get() = MaterialTheme.colorScheme.secondary

val PureDawnLight: Color
    @Composable
    get() = MaterialTheme.colorScheme.onSurface

val CelestialGrey: Color
    @Composable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

val CoralAlert: Color
    @Composable
    get() = MaterialTheme.colorScheme.error

// Standard constant GreenEmerald
val GreenEmerald = StaticGreenEmerald

