package com.gabestv.iptv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6C5CE7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF341F97),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF00CEC9),
    onSecondary = Color.Black,
    background = Color(0xFF0F0E17),
    onBackground = Color(0xFFFFFFFE),
    surface = Color(0xFF1B1A28),
    onSurface = Color(0xFFFFFFFE),
    surfaceVariant = Color(0xFF2E2D44),
    onSurfaceVariant = Color(0xFFA7A9BE),
    outline = Color(0xFF6C5CE7)
)

@Composable
fun GabesTVTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
