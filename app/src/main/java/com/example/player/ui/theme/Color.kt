package com.example.player.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// 品牌色
val PrimaryBlue = Color(0xFF82A8FF)   // 在深色背景上更亮的蓝色

// 渐变色（用于 Chip 文字等元素）
val GradientStart = Color(0xFF8899FF)
val GradientEnd   = Color(0xFFAA66CC)

// 深渊极简备用渐变（无 Canvas 时的回退）
val AppGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF08090F), Color(0xFF040407)),
    start  = Offset.Zero,
    end    = Offset(x = Float.POSITIVE_INFINITY, y = Float.POSITIVE_INFINITY)
)

// 深渊背景系（Abyss 风格，与 StarryBackground 配套）
val DarkBackground  = Color(0xFF08090F)   // 近纯黑带蓝，Scaffold containerColor fallback
val DarkSurface     = Color(0xFF0E0F1A)   // 深黑略带紫，底部弹窗 / 对话框背景
val DarkCard        = Color(0xFF14151F)   // 深黑卡片，AlertDialog containerColor
val DarkBorder      = Color(0x33FFFFFF)   // 20% white，主要分割线 / 边框
val DarkBorderLight = Color(0x40FFFFFF)   // 25% white，细节边框

// 文字（浅色，在深空背景上可读）
val TextPrimary   = Color(0xFFFFFFFF)     // 纯白
val TextSecondary = Color(0xB3FFFFFF)     // 70% white
val TextTertiary  = Color(0x73FFFFFF)     // 45% white

// 胶囊 Chip
val ChipSelected   = Color(0xFFFFFFFF)    // 选中：白色
val ChipUnselected = Color(0x26FFFFFF)    // 未选中：15% white

// 底部导航栏（近纯黑半透明，与深渊背景融合）
val NavBarBg = Color(0xE008090F)
