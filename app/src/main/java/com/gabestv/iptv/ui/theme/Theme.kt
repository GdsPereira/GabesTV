package com.gabestv.iptv.ui.theme

import androidx.compose.material3.darkColorScheme as standardDarkColorScheme
import androidx.compose.material3.MaterialTheme as StandardMaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.darkColorScheme as tvDarkColorScheme
import androidx.tv.material3.MaterialTheme as TvMaterialTheme
import com.gabestv.iptv.ui.util.LocalDeviceType

// Core Design System 2.0 Palette
val CyberPurple = Color(0xFF7C4DFF)
val CyberPurpleLight = Color(0xFF9E77FF)
val ElectricCyan = Color(0xFF00E5FF)
val NeonRed = Color(0xFFFF2A55)
val GoldAccent = Color(0xFFFFB300)

val OledBlack = Color(0xFF08070E)
val DeepDarkBackground = Color(0xFF0E0D18)
val SurfaceDark = Color(0xFF161526)
val SurfaceVariantDark = Color(0xFF232238)
val SurfaceElevated = Color(0xFF2E2D4A)

val OnSurfaceLight = Color(0xFFFBFBFE)
val TextMuted = Color(0xFFA7A9BE)
val TextDim = Color(0xFF6B6E8C)

private val StandardM3ColorScheme = standardDarkColorScheme(
    primary = CyberPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3B208C),
    onPrimaryContainer = Color.White,
    secondary = ElectricCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF004D54),
    onSecondaryContainer = Color.White,
    tertiary = NeonRed,
    background = DeepDarkBackground,
    onBackground = OnSurfaceLight,
    surface = SurfaceDark,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextMuted
)

private val TvM3ColorScheme = tvDarkColorScheme(
    primary = CyberPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3B208C),
    onPrimaryContainer = Color.White,
    secondary = ElectricCyan,
    onSecondary = Color.Black,
    background = DeepDarkBackground,
    onBackground = OnSurfaceLight,
    surface = SurfaceDark,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextMuted
)

/**
 * Dual-Theme Provider for GabesTV.
 * Provides Android TV Material3 theme for TV devices and Mobile Material3 theme for Phone/Tablet,
 * ensuring flawless component compatibility and preventing runtime layout crashes.
 */
@Composable
fun GabesTVTheme(
    content: @Composable () -> Unit
) {
    val deviceType = LocalDeviceType.current
    if (deviceType.isTv) {
        TvMaterialTheme(
            colorScheme = TvM3ColorScheme,
            content = content
        )
    } else {
        StandardMaterialTheme(
            colorScheme = StandardM3ColorScheme,
            content = content
        )
    }
}
