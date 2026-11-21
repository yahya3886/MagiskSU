package com.magisksu.manager.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * KernelSU-style green palette (Material 3 ton dengesi korunarak).
 * Used whenever dynamic color is disabled or unavailable.
 */

// Green ramp
val Green10 = Color(0xFF00210F)
val Green20 = Color(0xFF00391F)
val Green30 = Color(0xFF00522C)
val Green40 = Color(0xFF006B3B)
val Green80 = Color(0xFF6DDAA1)
val Green90 = Color(0xFF8DF5C0)

// Neutral ramp
val Neutral4 = Color(0xFF0F1511)
val Neutral6 = Color(0xFF171D19)
val Neutral12 = Color(0xFF222923)
val Neutral20 = Color(0xFF2B322C)
val Neutral24 = Color(0xFF31352F)
val Neutral87 = Color(0xFFDFE4DE)
val Neutral90 = Color(0xFFE5EAE4)
val Neutral94 = Color(0xFFEFF5EE)
val Neutral96 = Color(0xFFF6FBF4)

// Secondary / tertiary accents
val Secondary40 = Color(0xFF4E6355)
val Secondary80 = Color(0xFFB4CCBB)
val Secondary90 = Color(0xFFD0E8D6)

val Tertiary40 = Color(0xFF3B6470)
val Tertiary80 = Color(0xFFA0E8F7)
val Tertiary90 = Color(0xFFC0EBF8)

val Error40 = Color(0xFFBA1A1A)
val Error80 = Color(0xFFFFB4AB)
val Error90 = Color(0xFFFFDAD6)

val LightColors = lightColorScheme(
    primary = Green40,
    onPrimary = Color.White,
    primaryContainer = Green90,
    onPrimaryContainer = Green10,
    secondary = Secondary40,
    onSecondary = Color.White,
    secondaryContainer = Secondary90,
    onSecondaryContainer = Color(0xFF0B1F13),
    tertiary = Tertiary40,
    onTertiary = Color.White,
    tertiaryContainer = Tertiary90,
    onTertiaryContainer = Color(0xFF001F26),
    background = Neutral96,
    onBackground = Neutral6,
    surface = Neutral96,
    onSurface = Neutral6,
    surfaceVariant = Color(0xFFDDE5DC),
    onSurfaceVariant = Color(0xFF414941),
    surfaceContainer = Neutral94,
    surfaceContainerHigh = Neutral90,
    surfaceContainerHighest = Neutral87,
    surfaceContainerLow = Color(0xFFEFF5EE),
    surfaceContainerLowest = Color.White,
    outline = Color(0xFF717971),
    outlineVariant = Color(0xFFC1C9C1),
    error = Error40,
    onError = Color.White,
    errorContainer = Error90,
    onErrorContainer = Color(0xFF410002),
    scrim = Color.Black,
)

val DarkColors = darkColorScheme(
    primary = Green80,
    onPrimary = Green20,
    primaryContainer = Green30,
    onPrimaryContainer = Green90,
    secondary = Secondary80,
    onSecondary = Color(0xFF1B3428),
    secondaryContainer = Color(0xFF3B4B3E),
    onSecondaryContainer = Secondary90,
    tertiary = Tertiary80,
    onTertiary = Color(0xFF003541),
    tertiaryContainer = Color(0xFF1F4B56),
    onTertiaryContainer = Tertiary90,
    background = Neutral4,
    onBackground = Neutral87,
    surface = Neutral4,
    onSurface = Neutral87,
    surfaceVariant = Color(0xFF414941),
    onSurfaceVariant = Color(0xFFC1C9C0),
    surfaceContainer = Neutral12,
    surfaceContainerHigh = Neutral20,
    surfaceContainerHighest = Color(0xFF3B433C),
    surfaceContainerLow = Color(0xFF131A15),
    surfaceContainerLowest = Color.Black,
    outline = Color(0xFF8B938A),
    outlineVariant = Neutral24,
    error = Error80,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Error90,
    scrim = Color.Black,
)
