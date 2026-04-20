package com.example.player.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * 液态玻璃播放器色彩方案。
 *
 * 基调：极简深渊近纯黑背景 + 半透明玻璃层（`DarkBackground` / `DarkSurface` / `DarkCard`），
 * 这是整个视觉语言的核心，不随系统壁纸变化。
 *
 * 动态色（Android 12+）：仅替换 `primary` / `secondary` / `tertiary` 三类强调色，
 * 让主按钮色、滑块轨道、选中状态跟随用户壁纸 Monet 取色，带来轻度个性化。
 */
private val StaticColorScheme = lightColorScheme(
    primary          = PrimaryBlue,
    onPrimary        = Color.White,
    secondary        = PrimaryBlue,
    onSecondary      = Color.White,
    tertiary         = GradientEnd,
    onTertiary       = Color.White,
    background       = DarkBackground,
    onBackground     = TextPrimary,
    surface          = DarkSurface,
    onSurface        = TextPrimary,
    surfaceVariant   = DarkCard,
    onSurfaceVariant = TextSecondary,
    outline          = DarkBorder,
    inverseSurface   = Color(0xFF1A1A1A)
)

@Composable
fun PlayerTheme(
    /** true 时优先使用 Monet 动态色（仅 Android 12+ 生效）。 */
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        val dynamic = dynamicDarkColorScheme(context)
        // 壁纸强调色覆盖默认蓝色，但玻璃基调色（background / surface / surfaceVariant）
        // 继续沿用项目固定的深空蓝，避免 Monet 把背景染成浅色破坏液态玻璃氛围。
        StaticColorScheme.copy(
            primary     = dynamic.primary,
            onPrimary   = dynamic.onPrimary,
            secondary   = dynamic.secondary,
            onSecondary = dynamic.onSecondary,
            tertiary    = dynamic.tertiary,
            onTertiary  = dynamic.onTertiary
        )
    } else {
        StaticColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
