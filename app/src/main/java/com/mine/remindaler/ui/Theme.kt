package com.mine.remindaler.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AppBackground = Color(0xFF070B10)
val AppSurface = Color(0xFF111923)
val AppSurfaceHigh = Color(0xFF1A2430)
val AppPrimary = Color(0xFF4ADE80)
val AppSecondary = Color(0xFF60A5FA)
val AppText = Color(0xFFE6EDF3)
val AppMuted = Color(0xFF8B98A5)
val AppDanger = Color(0xFFF87171)
val AppWarning = Color(0xFFFBBF24)

private val RemindalerColors = darkColorScheme(
    primary = AppPrimary,
    onPrimary = Color(0xFF04120A),
    secondary = AppSecondary,
    onSecondary = Color(0xFF06121F),
    background = AppBackground,
    onBackground = AppText,
    surface = AppSurface,
    onSurface = AppText,
    surfaceVariant = AppSurfaceHigh,
    onSurfaceVariant = AppMuted,
    error = AppDanger,
    onError = Color(0xFF1A0606)
)

@Composable
fun RemindalerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RemindalerColors,
        content = content
    )
}
