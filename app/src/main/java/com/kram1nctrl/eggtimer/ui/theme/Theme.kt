package com.kram1nctrl.eggtimer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Yolk = Color(0xFFF2A93B)
private val YolkDeep = Color(0xFFB4690E)
private val Shell = Color(0xFFFFF8E7)
private val ShellDark = Color(0xFF1B1712)
private val Ink = Color(0xFF221A0F)
private val InkSoft = Color(0xFF4F4536)

private val LightColors = lightColorScheme(
    primary = YolkDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDCA8),
    onPrimaryContainer = Color(0xFF3A2200),
    secondary = Color(0xFF6F5B3E),
    onSecondary = Color.White,
    background = Shell,
    onBackground = Ink,
    surface = Shell,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF0E0C8),
    onSurfaceVariant = InkSoft,
    outline = Color(0xFF857463),
)

private val DarkColors = darkColorScheme(
    primary = Yolk,
    onPrimary = Color(0xFF3A2200),
    primaryContainer = Color(0xFF5B3D00),
    onPrimaryContainer = Color(0xFFFFDCA8),
    secondary = Color(0xFFDCC5A0),
    onSecondary = Color(0xFF3C2F1A),
    background = ShellDark,
    onBackground = Color(0xFFF2E5D2),
    surface = ShellDark,
    onSurface = Color(0xFFF2E5D2),
    surfaceVariant = Color(0xFF3A3228),
    onSurfaceVariant = Color(0xFFD6C6B0),
    outline = Color(0xFF9C8C77),
)

@Composable
fun EggTimerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
