package com.example.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * 极简深渊背景容器（Abyss 风格）
 *
 * 接近纯黑，仅顶部一抹若隐若现的蓝紫光晕，无星星。
 * OLED 友好，完全让玻璃卡片与内容成为视觉主角。
 *
 * 使用方法：
 *   StarryBackground(modifier = Modifier.fillMaxSize()) {
 *       // 你的 UI 内容
 *   }
 */
@Composable
fun StarryBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {

            // ── 底色：接近纯黑，顶部极淡蓝调 ─────────────────────────────────
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF08090F),   // 顶部：近纯黑带蓝
                        Color(0xFF040407)    // 底部：接近纯黑
                    )
                )
            )

            // ── 顶部唯一光晕：极淡蓝紫，若隐若现 ────────────────────────────
            // alpha = 0x0D ≈ 5%，半径覆盖整个屏幕宽度，营造 OLED 屏微光氛围
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x0D1020A0),   // 5% 蓝紫
                        Color(0x00000000)
                    ),
                    center = Offset(size.width * 0.50f, 0f),
                    radius = size.width * 0.85f
                ),
                radius = size.width * 0.85f,
                center = Offset(size.width * 0.50f, 0f)
            )
        }

        // ── 内容层 ─────────────────────────────────────────────────────────────
        content()
    }
}
