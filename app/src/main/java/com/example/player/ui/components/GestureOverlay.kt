package com.example.player.ui.components

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import com.example.player.ui.theme.AppSpring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class DragDirection { HORIZONTAL, VERTICAL, UNDECIDED }

/**
 * 手势提示类型：区分 Volume / Brightness（显示图标+进度条）和 Seek（纯文字）
 */
private sealed class GestureHint {
    data class Volume(val percent: Int) : GestureHint()
    data class Brightness(val percent: Int) : GestureHint()
    data class Seek(val label: String) : GestureHint()
}

/**
 * 全屏透明手势层：
 * - 左 1/3 + 垂直  → 屏幕亮度
 * - 中 1/3 + 水平  → 快进/快退（每 dp 0.5s）
 * - 右 1/3 + 垂直  → 媒体音量
 * - 单击           → 切换控制栏
 * - 双击           → 播放/暂停
 *
 * @param enabled         false = 锁定模式，仅保留单击切换控制栏，拖拽/双击禁用
 * @param onVolumeChanged   音量变化回调（0-100）
 * @param onBrightnessChanged 亮度变化回调（0-100）
 */
@Composable
fun GestureOverlay(
    onToggleControls: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekBy: (Long) -> Unit,
    onVolumeChanged: (Int) -> Unit = {},
    onBrightnessChanged: (Int) -> Unit = {},
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var hint by remember { mutableStateOf<GestureHint?>(null) }
    var hideHintJob by remember { mutableStateOf<Job?>(null) }

    fun showHint(newHint: GestureHint) {
        hint = newHint
        hideHintJob?.cancel()
        hideHintJob = scope.launch {
            delay(1500)
            hint = null
        }
    }

    fun adjustVolume(normalizedDelta: Float) {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val next = (cur - normalizedDelta * max).roundToInt().coerceIn(0, max)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, next, 0)
        val percent = if (max > 0) (next * 100f / max).roundToInt() else 0
        showHint(GestureHint.Volume(percent))
        onVolumeChanged(percent)
    }

    fun adjustBrightness(normalizedDelta: Float) {
        val activity = context as? Activity ?: return
        val lp: WindowManager.LayoutParams = activity.window.attributes
        val current = if (lp.screenBrightness < 0f) 0.5f else lp.screenBrightness
        val next = (current - normalizedDelta).coerceIn(0.01f, 1.0f)
        lp.screenBrightness = next
        activity.window.attributes = lp
        val percent = (next * 100).roundToInt()
        showHint(GestureHint.Brightness(percent))
        onBrightnessChanged(percent)
    }

    var dragDir by remember { mutableStateOf(DragDirection.UNDECIDED) }

    // 拖拽修饰符（锁定时禁用）
    val dragModifier = if (enabled) {
        Modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = {
                    dragDir = DragDirection.UNDECIDED
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                onDragEnd = { dragDir = DragDirection.UNDECIDED },
                onDrag = { change, delta ->
                    change.consume()
                    val xFrac = change.position.x / size.width.toFloat()

                    if (dragDir == DragDirection.UNDECIDED) {
                        dragDir = if (abs(delta.x) > abs(delta.y) * 1.5f) {
                            DragDirection.HORIZONTAL
                        } else if (abs(delta.y) > abs(delta.x) * 1.5f) {
                            DragDirection.VERTICAL
                        } else {
                            DragDirection.UNDECIDED
                        }
                    }

                    when {
                        xFrac in 0.33f..0.67f && dragDir == DragDirection.HORIZONTAL -> {
                            val ms = (delta.x * 500).toLong()
                            onSeekBy(ms)
                            val secs = ms / 1000
                            showHint(GestureHint.Seek(if (secs >= 0) "+${secs}s" else "${secs}s"))
                        }
                        xFrac > 0.67f && dragDir == DragDirection.VERTICAL -> {
                            adjustVolume(delta.y / size.height.toFloat())
                        }
                        xFrac < 0.33f && dragDir == DragDirection.VERTICAL -> {
                            adjustBrightness(delta.y / size.height.toFloat())
                        }
                    }
                }
            )
        }
    } else Modifier

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(enabled) {
                detectTapGestures(
                    onTap        = { onToggleControls() },
                    onDoubleTap  = if (enabled) { { onTogglePlayPause() } } else null
                )
            }
            .then(dragModifier)
    ) {
        // iOS 26：手势提示弹窗以 scale+fade 弹出（类 iOS Toast 质感），消失时柔和收缩
        AnimatedVisibility(
            visible  = hint != null,
            enter    = scaleIn(AppSpring.press(), initialScale = 0.72f) + fadeIn(AppSpring.standard()),
            exit     = scaleOut(AppSpring.gentle(), targetScale = 0.88f) + fadeOut(AppSpring.gentle()),
            modifier = Modifier.align(Alignment.Center)
        ) {
            hint?.let { currentHint ->
                LiquidGlassContainer(
                    modifier     = Modifier.width(120.dp),
                    cornerRadius = 12.dp,
                    blurRadius   = 15f
                ) {
                    when (currentHint) {
                        is GestureHint.Volume -> {
                            Column(
                                modifier              = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalAlignment   = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector        = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    tint               = Color.White,
                                    modifier           = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress           = { currentHint.percent / 100f },
                                    modifier           = Modifier.width(80.dp).height(3.dp),
                                    color              = Color.White,
                                    trackColor         = Color.White.copy(alpha = 0.25f),
                                    strokeCap          = StrokeCap.Round
                                )
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    text       = "${currentHint.percent}%",
                                    color      = Color.White,
                                    fontSize   = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        is GestureHint.Brightness -> {
                            Column(
                                modifier              = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalAlignment   = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector        = Icons.Default.BrightnessHigh,
                                    contentDescription = null,
                                    tint               = Color.White,
                                    modifier           = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress           = { currentHint.percent / 100f },
                                    modifier           = Modifier.width(80.dp).height(3.dp),
                                    color              = Color.White,
                                    trackColor         = Color.White.copy(alpha = 0.25f),
                                    strokeCap          = StrokeCap.Round
                                )
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    text       = "${currentHint.percent}%",
                                    color      = Color.White,
                                    fontSize   = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        is GestureHint.Seek -> {
                            Text(
                                text       = currentHint.label,
                                color      = Color.White,
                                fontSize   = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier   = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
