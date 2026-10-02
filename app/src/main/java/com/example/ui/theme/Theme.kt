package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TerminalColorScheme = darkColorScheme(
    primary = PhosphorGreen,
    onPrimary = TerminalBlack,
    primaryContainer = Color(0xFF003816),
    onPrimaryContainer = PhosphorGreen,
    secondary = CyberCyan,
    onSecondary = TerminalBlack,
    secondaryContainer = Color(0xFF003B47),
    onSecondaryContainer = CyberCyan,
    tertiary = TerminalAmber,
    onTertiary = TerminalBlack,
    tertiaryContainer = Color(0xFF473100),
    onTertiaryContainer = TerminalAmber,
    error = TerminalCrimson,
    onError = TerminalBlack,
    background = TerminalBlack,
    onBackground = TerminalTextPrimary,
    surface = TerminalDarkSurface,
    onSurface = TerminalTextPrimary,
    surfaceVariant = TerminalCardBg,
    onSurfaceVariant = TerminalTextSecondary,
    outline = TerminalBorder,
    outlineVariant = TerminalBorderHighlight
)

@Composable
fun RPSBattleTerminalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // The battle terminal is intentionally an authentic black/dark cyber console
    MaterialTheme(
        colorScheme = TerminalColorScheme,
        typography = TerminalTypography,
        content = content
    )
}
