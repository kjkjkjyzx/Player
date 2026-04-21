package com.example.player.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * iOS 26 Liquid Glass 统一设计 Token
 *
 * 所有玻璃容器（LiquidGlassContainer、VideoCard、PlayerScreen 按钮等）
 * 从此对象取值，确保全局视觉一致，避免魔法数字散落各处。
 */
object GlassDefaults {

    /**
     * 磨砂渐变填充：顶部亮（光线入射区）→ 中部暗 → 底部轻微回升（环境反射光）。
     * 5段渐变模拟真实玻璃曲面的光照变化，底部回升模拟玻璃下方的环境光反射。
     */
    val backgroundBrush: Brush = Brush.verticalGradient(
        0.00f to Color.White.copy(alpha = 0.20f),
        0.22f to Color.White.copy(alpha = 0.10f),
        0.55f to Color.White.copy(alpha = 0.05f),
        0.85f to Color.White.copy(alpha = 0.04f),
        1.00f to Color.White.copy(alpha = 0.09f)
    )

    /**
     * 渐变描边：顶部镜面高光 → 侧面环境光 → 底部镜面反光。
     * 顶部和底部同时有亮度，模拟玻璃受到上下双向光源照射的镜面边缘。
     */
    val borderBrush: Brush = Brush.verticalGradient(
        0.00f to Color.White.copy(alpha = 0.22f),
        0.40f to Color.White.copy(alpha = 0.07f),
        0.75f to Color.White.copy(alpha = 0.05f),
        1.00f to Color.White.copy(alpha = 0.18f)
    )

    /**
     * 顶部镜面高光条：中心亮白（0.72f），两端水平渐隐为透明。
     * 适配任意圆角形状（Canvas 在 clip 之后绘制，自动裁剪圆角）。
     * 这是 iOS 26 Liquid Glass 最标志性的折射元素。
     */
    val highlightBrush: Brush = Brush.horizontalGradient(
        0.00f to Color.Transparent,
        0.12f to Color.White.copy(alpha = 0.62f),
        0.50f to Color.White.copy(alpha = 0.72f),
        0.88f to Color.White.copy(alpha = 0.62f),
        1.00f to Color.Transparent
    )

    /**
     * 底部反射高光条：模拟玻璃放置在发光平面上时底部的内反射。
     * 与顶部 highlightBrush 同构，但亮度更低（0.28f vs 0.72f），营造立体感。
     */
    val bottomHighlightBrush: Brush = Brush.horizontalGradient(
        0.00f to Color.Transparent,
        0.20f to Color.White.copy(alpha = 0.22f),
        0.50f to Color.White.copy(alpha = 0.28f),
        0.80f to Color.White.copy(alpha = 0.22f),
        1.00f to Color.Transparent
    )

    /** 阴影高度 */
    val elevation: Dp = 6.dp

    /** 描边宽度（全局统一） */
    val borderWidth: Dp = 0.6.dp

    /** 顶部高光条高度 */
    val highlightHeight: Dp = 2.dp

    /** 底部反射高光条高度 */
    val bottomHighlightHeight: Dp = 1.2.dp
}
