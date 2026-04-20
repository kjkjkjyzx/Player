package com.example.player.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.player.ui.theme.GlassDefaults

/**
 * iOS 26 Liquid Glass container — 5 层结构
 *
 * [阴影↑] → [渐变磨砂填充] → [顶部镜面高光条] → [渐变描边] → [内容]
 *
 * 通过 GlassDefaults 取值，确保全局视觉一致。
 * 顶部高光条（highlightBrush）模拟玻璃折射的镜面反射，
 * 渐变填充（backgroundBrush）模拟光线从顶部入射后向底部衰减的磨砂感，
 * 渐变描边（borderBrush）赋予容器明确的受光方向（顶亮底暗）。
 *
 * @param isLight     保留参数（兼容旧调用方），当前由 GlassDefaults 统一处理，无实际作用
 * @param blurRadius  保留参数（供未来 RenderEffect 后台模糊接入），当前无效
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
                elevation    = GlassDefaults.elevation,
                shape        = shape,
                clip         = false,
                spotColor    = Color.Black.copy(alpha = 0.20f),
                ambientColor = Color.Black.copy(alpha = 0.08f)
            )
            .clip(shape)
            // ── Layer 1+2: 渐变磨砂填充 + 顶部镜面高光条（合并到 drawBehind，
            //    在绘制阶段执行，消除两个独立的组合树节点） ────────────────────
            .drawBehind {
                drawRect(brush = GlassDefaults.backgroundBrush)
                drawRect(
                    brush = GlassDefaults.highlightBrush,
                    size  = Size(size.width, GlassDefaults.highlightHeight.toPx())
                )
            }
            .then(clickMod)
    ) {
        // ── Layer 3: 渐变描边（顶部镜面高光 → 底部消隐） ──────────────────
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(GlassDefaults.borderWidth, GlassDefaults.borderBrush, shape)
        )

        // ── Layer 4: 内容层 ────────────────────────────────────────────────
        content()
    }
}
