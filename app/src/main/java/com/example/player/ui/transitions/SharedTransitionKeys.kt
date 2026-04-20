package com.example.player.ui.transitions

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.compositionLocalOf

/**
 * 全局 CompositionLocal，将 SharedTransitionLayout 的 scope 及当前路由的
 * AnimatedContentScope 传递给深层子树（VideoCard、PlayerScreen 等），
 * 避免逐级修改函数签名。
 *
 * 在 PlayerApp (MainActivity.kt) 的 NavHost 外层通过
 * CompositionLocalProvider 提供值；在不参与共享动画的页面中保持 null。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalAnimatedContentScope = compositionLocalOf<AnimatedContentScope?> { null }

/**
 * 横屏退出信号：true 时临时为横屏视频开启 SharedBounds。
 *
 * 横屏视频平时禁用 SharedBounds（进入时旋转＋导航同帧，坐标系冲突）。
 * 退出时：先旋转回竖屏 → 等旋转完成（380ms）→ 设此 flag 触发两侧注册 SharedBounds
 * → popBackStack()，SharedBounds 在竖屏坐标系下正常收缩回卡片缩略图。
 */
val LocalLandscapeExiting = compositionLocalOf { false }
