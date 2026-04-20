package com.example.player.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/**
 * iOS 26 "Liquid Glass" 动画弹簧规格系统
 *
 * iOS 26 的设计语言中所有状态转换均使用弹簧物理，而非线性 tween。
 * 这里按照用途分为 4 个预设，对应 iOS UISpringTimingParameters 的常用配置：
 *
 * | 预设       | dampingRatio | stiffness           | iOS 等效              |
 * |-----------|-----------------|---------------------|-----------------------|
 * | press     | 0.65            | StiffnessMedium     | response=0.22 bounce=0.35 |
 * | standard  | 0.85            | StiffnessMediumLow  | response=0.30 bounce=0.10 |
 * | spatial   | 0.90            | StiffnessLow        | response=0.45 bounce=0.05 |
 * | gentle    | 1.00            | StiffnessVeryLow    | response=0.55 bounce=0.00 |
 */
object AppSpring {

    /**
     * 按压 / 图标切换：快速响应，轻微弹跳。
     * 用于：按压 scale 反馈、收藏图标弹出、播放/暂停 crossfade。
     */
    fun <T> press() = spring<T>(
        dampingRatio = 0.65f,
        stiffness       = Spring.StiffnessMedium
    )

    /**
     * 标准 UI 过渡：流畅，轻微弹性。
     * 用于：Tab/Chip 选中颜色、AnimatedVisibility 入场、列表条目动画。
     */
    fun <T> standard() = spring<T>(
        dampingRatio = 0.85f,
        stiffness       = Spring.StiffnessMediumLow
    )

    /**
     * 大空间移动：稳重，几乎无回弹。
     * 用于：NavHost 页面切换、大面积布局动画。
     */
    fun <T> spatial() = spring<T>(
        dampingRatio = 0.90f,
        stiffness       = Spring.StiffnessLow
    )

    /**
     * 柔和消失：纯衰减，无弹跳。
     * 用于：所有 exit 动画、提示弹窗消失。
     */
    fun <T> gentle() = spring<T>(
        dampingRatio = 1.00f,
        stiffness       = Spring.StiffnessVeryLow
    )
}
