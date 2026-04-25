package com.example.player.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * 极简深渊背景容器（Abyss 风格）
 *
 * 接近纯黑，顶部一抹若隐若现的蓝紫光晕，加 20 颗极暗微粒缓慢闪烁。
 * OLED 友好，完全让玻璃卡片与内容成为视觉主角。
 *
 * 生命周期管控：后台时停止粒子动画以节省电量。
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
    val density = LocalDensity.current.density
    val lifecycleOwner = LocalLifecycleOwner.current

    // ── 生命周期管控：后台时停止动画以节省电量 ──────────────────────────────
    var isAnimating by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isAnimating = when (event) {
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP,
                Lifecycle.Event.ON_DESTROY -> false
                Lifecycle.Event.ON_RESUME,
                Lifecycle.Event.ON_START -> true
                else -> isAnimating
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // ── 20 颗伪随机星点（黄金分割比均布，确保不聚堆） ───────────────────────
    val starData = remember {
        List(20) { i ->
            Triple(
                (i * 0.6180339f) % 1f,           // x 比例
                (i * 0.3819660f + 0.05f) % 1f,   // y 比例
                2000 + (i * 337) % 4000           // 动画周期 ms（2000–6000）
            )
        }
    }

    // ── 粒子 alpha 值：动画态用帧值，静止态用中值 0.06 ──────────────────────
    val alphas: List<Float> = if (isAnimating) {
        val infiniteTransition = rememberInfiniteTransition(label = "stars")
        starData.map { (_, _, period) ->
            infiniteTransition.animateFloat(
                initialValue  = 0.02f,
                targetValue   = 0.10f,
                animationSpec = infiniteRepeatable(
                    animation  = tween(period, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "starAlpha$period"
            ).value
        }
    } else {
        // 后台或已销毁：所有星点用静态中值，停止动画帧更新
        List(20) { 0.06f }
    }

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

            // ── 微粒星点：极暗，OLED 友好 ─────────────────────────────────────
            starData.forEachIndexed { i, (xFrac, yFrac, _) ->
                val radius = (0.8f + (i % 3) * 0.5f) * density   // 0.8–1.8 dp
                drawCircle(
                    color  = Color.White.copy(alpha = alphas[i]),
                    radius = radius,
                    center = Offset(size.width * xFrac, size.height * yFrac)
                )
            }
        }

        // ── 内容层 ─────────────────────────────────────────────────────────────
        content()
    }
}
