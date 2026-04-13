package com.example.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppColorScheme = lightColorScheme(
    primary          = PrimaryBlue,
    onPrimary        = Color.White,
    secondary        = TextSecondary,
    onSecondary      = Color.White,
    background       = BeigeBackground,
    onBackground     = TextPrimary,
    surface          = BeigeSurface,
    onSurface        = TextPrimary,
    surfaceVariant   = BeigeDivider,
    onSurfaceVariant = TextSecondary,
    outline          = BeigeDivider
)

@Composable
fun PlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography  = Typography,
        content     = content
    )
}
