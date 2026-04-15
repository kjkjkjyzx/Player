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
import kotlin.random.Random

/**
 * 星空背景容器
 *
 * 用 Canvas 绘制：
 *  - 深蓝黑垂直渐变底色
 *  - 两处柔和星云光晕（紫色 / 蓝色）
 *  - 200 颗小星星（暗）
 *  - 60 颗中型星星
 *  - 15 颗亮星（带径向光晕）
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

            // ── 底色：深空垂直渐变 ────────────────────────────────────────────
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0C1030),   // 顶部：深靛蓝
                        Color(0xFF060818)    // 底部：近纯黑
                    )
                )
            )

            // ── 星云光晕 ─────────────────────────────────────────────────────
            // 左上角：紫色漫射
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x1A7050D0), Color(0x00000000)),
                    center = Offset(size.width * 0.18f, size.height * 0.12f),
                    radius = size.width * 0.55f
                ),
                radius = size.width * 0.55f,
                center = Offset(size.width * 0.18f, size.height * 0.12f)
            )
            // 右下角：蓝色漫射
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x152060B0), Color(0x00000000)),
                    center = Offset(size.width * 0.82f, size.height * 0.78f),
                    radius = size.width * 0.48f
                ),
                radius = size.width * 0.48f,
                center = Offset(size.width * 0.82f, size.height * 0.78f)
            )
            // 中间偏右：极淡蓝色云气
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x0C3070C0), Color(0x00000000)),
                    center = Offset(size.width * 0.65f, size.height * 0.40f),
                    radius = size.width * 0.35f
                ),
                radius = size.width * 0.35f,
                center = Offset(size.width * 0.65f, size.height * 0.40f)
            )

            val rng = Random(seed = 2024)

            // ── 小星星（200 颗，暗淡，密集）────────────────────────────────────
            repeat(200) {
                val x = rng.nextFloat() * size.width
                val y = rng.nextFloat() * size.height
                val r = rng.nextFloat() * 0.9f + 0.3f
                val a = rng.nextFloat() * 0.45f + 0.18f
                drawCircle(
                    color  = Color(1f, 1f, 1f, a),
                    radius = r,
                    center = Offset(x, y)
                )
            }

            // ── 中型星星（60 颗，较亮）──────────────────────────────────────────
            repeat(60) {
                val x = rng.nextFloat() * size.width
                val y = rng.nextFloat() * size.height
                val r = rng.nextFloat() * 0.9f + 1.1f
                val a = rng.nextFloat() * 0.35f + 0.55f
                // 轻微暖色/冷色随机偏移，增加真实感
                val tint = if (rng.nextBoolean())
                    Color(0.95f, 0.97f, 1.00f, a)   // 冷白
                else
                    Color(1.00f, 0.97f, 0.90f, a)   // 暖白
                drawCircle(color = tint, radius = r, center = Offset(x, y))
            }

            // ── 亮星（15 颗，带径向光晕）──────────────────────────────────────
            repeat(15) {
                val x   = rng.nextFloat() * size.width
                val y   = rng.nextFloat() * size.height
                val r   = rng.nextFloat() * 1.2f + 1.8f
                val cx  = Offset(x, y)
                // 外层光晕
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(1f, 1f, 1f, 0.22f),
                            Color(0f, 0f, 0f, 0f)
                        ),
                        center = cx,
                        radius = r * 5f
                    ),
                    radius = r * 5f,
                    center = cx
                )
                // 内层光晕
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(1f, 1f, 1f, 0.70f),
                            Color(0f, 0f, 0f, 0f)
                        ),
                        center = cx,
                        radius = r * 2f
                    ),
                    radius = r * 2f,
                    center = cx
                )
                // 星核
                drawCircle(
                    color  = Color.White,
                    radius = r,
                    center = cx
                )
            }
        }

        // ── 内容层 ─────────────────────────────────────────────────────────────
        content()
    }
}
