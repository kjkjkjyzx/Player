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
                elevation    = if (isLight) 3.dp else 2.dp,
                shape        = shape,
                clip         = false,
                spotColor    = Color.Black.copy(alpha = if (isLight) 0.10f else 0.12f),
                ambientColor = Color.Black.copy(alpha = if (isLight) 0.04f else 0.05f)
            )
            .clip(shape)
            .then(clickMod)
    ) {

        // ── Layer 1: 磨砂基底（极薄白色，模拟磨砂感） ───────────────────────
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Color.White.copy(alpha = if (isLight) 0.12f else 0.06f)
                )
        )

        // ── Layer 2: 顶部淡高光 ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = if (isLight) arrayOf(
                            0.00f to Color.White.copy(alpha = 0.10f),
                            0.12f to Color.White.copy(alpha = 0.04f),
                            0.28f to Color.Transparent
                        ) else arrayOf(
                            0.00f to Color.White.copy(alpha = 0.13f),
                            0.10f to Color.White.copy(alpha = 0.05f),
                            0.25f to Color.Transparent
                        )
                    )
                )
        )

        // ── Layer 3: 左上角极淡散射 ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colorStops = arrayOf(
                            0.00f to Color.White.copy(alpha = if (isLight) 0.06f else 0.07f),
                            0.35f to Color.Transparent
                        ),
                        start = Offset(0f, 0f),
                        end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
        )

        // ── Layer 4: 细边框（单色，极淡） ────────────────────────────────────
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = if (isLight) 0.22f else 0.18f),
                    shape = shape
                )
        )

        // ── Layer 5: 内容层 ───────────────────────────────────────────────────
        content()
    }
}
