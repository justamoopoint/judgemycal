package com.judgemycal.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand palette: violet + mint.
val Violet = Color(0xFF6C4DF6)
val VioletDeep = Color(0xFF4A32B8)
val Mint = Color(0xFF7BE8C4)
val MintDeep = Color(0xFF2FA07C)
val Ink = Color(0xFF1C1830)
val Paper = Color(0xFFFAF8FF)

// Confidence band colours (never red/alarming — uncertainty is honest, not scary).
val ConfidenceHigh = MintDeep
val ConfidenceMedium = Color(0xFFE8B93B)
val ConfidenceLow = Color(0xFF9B8AF0)

private val LightColors = lightColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7E0FF),
    onPrimaryContainer = VioletDeep,
    secondary = MintDeep,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCF5E5),
    onSecondaryContainer = Color(0xFF0F5138),
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEDE9F8),
    onSurfaceVariant = Color(0xFF55506B),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB7A6FF),
    onPrimary = Color(0xFF2A1D6E),
    primaryContainer = VioletDeep,
    onPrimaryContainer = Color(0xFFE7E0FF),
    secondary = Mint,
    onSecondary = Color(0xFF00382A),
    secondaryContainer = Color(0xFF0F5138),
    onSecondaryContainer = Color(0xFFCCF5E5),
    background = Color(0xFF141120),
    onBackground = Color(0xFFEAE6F5),
    surface = Color(0xFF1C1830),
    onSurface = Color(0xFFEAE6F5),
    surfaceVariant = Color(0xFF2A2540),
    onSurfaceVariant = Color(0xFFBDB6D4),
)

@Composable
fun JudgeMyCalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
