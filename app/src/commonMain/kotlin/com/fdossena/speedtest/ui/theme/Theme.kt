package com.fdossena.speedtest.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF), // Cyan
    secondary = Color(0xFF5E5CE6), // Indigo
    tertiary = Color(0xFFFF9F0A), // Amber
    onPrimary = Color(0xFF08080E),
    onSecondary = Color.White,
    onTertiary = Color(0xFF08080E),
    surface = Color(0xFF08080E),
    onSurface = Color.White,
    background = Color(0xFF08080E),
    onBackground = Color.White,
    error = Color(0xFFFE27A9)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFFF9F0A), // Amber
    secondary = Color(0xFF5E5CE6), // Indigo
    tertiary = Color(0xFF00E5FF), // Cyan
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    surface = Color(0xFFF0F2F8),
    onSurface = Color.Black,
    background = Color(0xFFF0F2F8),
    onBackground = Color.Black,
    error = Color(0xFF00FF64)
)

@Composable
fun SpeedtestTheme(
    themeMode: Int = 0, // 0: System, 1: Light, 2: Dark
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
