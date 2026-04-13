package com.example.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppColorScheme = darkColorScheme(
    primary          = PrimaryBlue,
    onPrimary        = Color.White,
    background       = DarkBackground,
    onBackground     = TextPrimary,
    surface          = DarkSurface,
    onSurface        = TextPrimary,
    surfaceVariant   = DarkCard,
    onSurfaceVariant = TextSecondary,
    outline          = DarkBorder
)

@Composable
fun PlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography  = Typography,
        content     = content
    )
}
