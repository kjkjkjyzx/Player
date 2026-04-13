package com.example.player.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid glass container with layered visual effects.
 *
 * @param isLight  true = 浅色（米黄/白）背景模式；false = 深色（视频）背景模式（默认）
 * @param onClick  可选点击回调；设置后内部自动应用 clickable（已经过 clip 裁剪）
 *
 * 分层结构：
 *  Layer 1 – 半透明基底     : 遮挡背景，决定整体色调
 *  Layer 2 – 模糊霜化层     : RenderEffect Gaussian blur (API 31+)，低版本降级
 *  Layer 3 – 玻璃边缘高光   : 模拟玻璃折射的渐变边框
 *  Layer 4 – 内容层
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
    val shape = RoundedCornerShape(cornerRadius)
    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Box(modifier = modifier.clip(shape).then(clickMod)) {

        // ── Layer 1: 半透明基底 ───────────────────────────────────────────────
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    if (isLight) Color.White.copy(alpha = 0.55f)
                    else Color.Black.copy(alpha = 0.30f)
                )
        )

        // ── Layer 2: 模糊霜化层 ───────────────────────────────────────────────
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        renderEffect = RenderEffect
                            .createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP)
                            .asComposeRenderEffect()
                        alpha = if (isLight) 0.40f else 0.55f
                    }
                    .background(
                        Brush.verticalGradient(
                            colors = if (isLight) listOf(
                                Color.White.copy(alpha = 0.55f),
                                Color.White.copy(alpha = 0.20f)
                            ) else listOf(
                                Color.White.copy(alpha = 0.60f),
                                Color.White.copy(alpha = 0.20f)
                            )
                        )
                    )
            )
        } else {
            // API < 31 降级：纯半透明层
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        if (isLight) Color.White.copy(alpha = 0.45f)
                        else Color.White.copy(alpha = 0.12f)
                    )
            )
        }

        // ── Layer 3: 玻璃边缘高光 ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.85f),
                            Color.White.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.05f),
                            Color.White.copy(alpha = 0.45f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    ),
                    shape = shape
                )
        )

        // ── Layer 4: 内容层 ───────────────────────────────────────────────────
        content()
    }
}
