package com.example.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid glass container — 高透亮真实玻璃效果
 *
 * @param isLight  true = 浅色背景模式；false = 深色/星空背景模式
 */
@Composable
fun LiquidGlassContainer(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    blurRadius: Float = 22f,
    isLight: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape    = RoundedCornerShape(cornerRadius)
    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Box(
        modifier = modifier
            .shadow(
                elevation    = if (isLight) 6.dp else 10.dp,
                shape        = shape,
                clip         = false,
                spotColor    = Color.Black.copy(alpha = if (isLight) 0.18f else 0.45f),
                ambientColor = Color.Black.copy(alpha = if (isLight) 0.07f else 0.15f)
            )
            .clip(shape)
            .then(clickMod)
    ) {

        // ── Layer 1: 透明基底 ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    if (isLight) Color.White.copy(alpha = 0.10f)
                    else         Color.Transparent
                )
        )

        // ── Layer 2: 顶部强镜面高光 ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = if (isLight) arrayOf(
                            0.00f to Color.White.copy(alpha = 0.18f),
                            0.10f to Color.White.copy(alpha = 0.08f),
                            0.30f to Color.White.copy(alpha = 0.02f),
                            1.00f to Color.Transparent
                        ) else arrayOf(
                            0.00f to Color.White.copy(alpha = 0.65f),
                            0.06f to Color.White.copy(alpha = 0.38f),
                            0.15f to Color.White.copy(alpha = 0.12f),
                            0.28f to Color.White.copy(alpha = 0.02f),
                            1.00f to Color.Transparent
                        )
                    )
                )
        )

        // ── Layer 3: 左上角散射光斑 ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colorStops = arrayOf(
                            0.00f to Color.White.copy(alpha = if (isLight) 0.12f else 0.28f),
                            0.35f to Color.White.copy(alpha = if (isLight) 0.03f else 0.06f),
                            1.00f to Color.Transparent
                        ),
                        start = Offset(0f, 0f),
                        end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
        )

        // ── Layer 4: 棱边折射描边 ────────────────────────────────────────────
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isLight) 0.20f else 0.95f),
                            Color.White.copy(alpha = if (isLight) 0.10f else 0.45f),
                            Color.White.copy(alpha = if (isLight) 0.02f else 0.06f),
                            Color.White.copy(alpha = if (isLight) 0.12f else 0.55f)
                        ),
                        start = Offset(0f, 0f),
                        end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    ),
                    shape = shape
                )
        )

        // ── Layer 5: 内容层 ───────────────────────────────────────────────────
        content()
    }
}
