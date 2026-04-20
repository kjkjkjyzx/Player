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
     * 磨砂渐变填充：顶部亮（光线入射区）→ 底部暗（背光区），4段渐变。
     * 替代原先的平面 Color.White.copy(alpha = 0.06f)，模拟真实玻璃曲面的光照变化。
     */
    val backgroundBrush: Brush = Brush.verticalGradient(
        0.00f to Color.White.copy(alpha = 0.18f),
        0.25f to Color.White.copy(alpha = 0.09f),
        0.65f to Color.White.copy(alpha = 0.05f),
        1.00f to Color.White.copy(alpha = 0.02f)
    )

    /**
     * 渐变描边：顶部镜面高光 → 侧面环境光 → 底部消隐。
     * 替代原先的均匀 Color.White.copy(alpha = 0.18f)，赋予玻璃明确的受光方向。
     */
    val borderBrush: Brush = Brush.verticalGradient(
        0.00f to Color.White.copy(alpha = 0.15f),
        0.35f to Color.White.copy(alpha = 0.07f),
        1.00f to Color.White.copy(alpha = 0.02f)
    )

    /**
     * 顶部镜面高光条：中心亮白（0.65f），两端水平渐隐为透明。
     * 适配任意圆角形状（Canvas 在 clip 之后绘制，自动裁剪圆角）。
     * 这是 iOS 26 Liquid Glass 最标志性的折射元素。
     */
    val highlightBrush: Brush = Brush.horizontalGradient(
        0.00f to Color.Transparent,
        0.20f to Color.White.copy(alpha = 0.60f),
        0.50f to Color.White.copy(alpha = 0.65f),
        0.80f to Color.White.copy(alpha = 0.60f),
        1.00f to Color.Transparent
    )

    /** 阴影高度 */
    val elevation: Dp = 5.dp

    /** 描边宽度（全局统一） */
    val borderWidth: Dp = 0.5.dp

    /** 顶部高光条高度 */
    val highlightHeight: Dp = 1.5.dp
}
