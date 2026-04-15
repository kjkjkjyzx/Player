package com.example.player.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// 品牌色
val PrimaryBlue = Color(0xFF82A8FF)   // 在深色背景上更亮的蓝色

// 渐变色（用于 Chip 文字等元素）
val GradientStart = Color(0xFF8899FF)
val GradientEnd   = Color(0xFFAA66CC)

// 星空备用纯色渐变（无 Canvas 时的回退）
val AppGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF0C1030), Color(0xFF060818)),
    start  = Offset.Zero,
    end    = Offset(x = Float.POSITIVE_INFINITY, y = Float.POSITIVE_INFINITY)
)

// 深空背景系（与 StarryBackground 配套）
val DarkBackground  = Color(0xFF0C1030)   // 深靛蓝，Scaffold containerColor fallback
val DarkSurface     = Color(0xFF131A40)   // 稍亮深蓝，底部弹窗 / 对话框背景
val DarkCard        = Color(0xFF1B2250)   // 中深蓝，卡片 / 列表项背景
val DarkBorder      = Color(0x33FFFFFF)   // 20% white，主要分割线 / 边框
val DarkBorderLight = Color(0x40FFFFFF)   // 25% white，细节边框

// 文字（浅色，在深空背景上可读）
val TextPrimary   = Color(0xFFFFFFFF)     // 纯白
val TextSecondary = Color(0xB3FFFFFF)     // 70% white
val TextTertiary  = Color(0x73FFFFFF)     // 45% white

// 胶囊 Chip
val ChipSelected   = Color(0xFFFFFFFF)    // 选中：白色
val ChipUnselected = Color(0x26FFFFFF)    // 未选中：15% white

// 底部导航栏（深蓝半透明，与星空融合）
val NavBarBg = Color(0xE00C1030)
