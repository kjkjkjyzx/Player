package com.example.player.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.util.Rational
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.PlayerView
import com.example.player.ui.components.GestureOverlay
import com.example.player.ui.components.LiquidGlassContainer
import com.example.player.viewmodel.PlayerViewModel

private val AccentWhite = Color.White
private val CardBg      = Color(0x59000000)   // 35% black
private val CardBorder  = Color(0x1AFFFFFF)   // 10% white

@Composable
fun PlayerScreen(
    videoUri: Uri,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(videoUri) { viewModel.initializePlayer(videoUri) }

    // 沉浸式全屏
    DisposableEffect(Unit) {
        val window = (context as Activity).window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val ctrl = WindowInsetsControllerCompat(window, window.decorView)
        ctrl.hide(WindowInsetsCompat.Type.systemBars())
        ctrl.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            ctrl.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    DisposableEffect(Unit) { onDispose { viewModel.onPause() } }

    // 自动旋转
    val isLandscape   = viewModel.isLandscapeVideo
    val videoSizeKnown = viewModel.videoSizeKnown
    LaunchedEffect(videoSizeKnown, isLandscape) {
        if (!videoSizeKnown) return@LaunchedEffect
        (context as Activity).requestedOrientation =
            if (isLandscape) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            else             ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
    }
    DisposableEffect(Unit) {
        onDispose {
            (context as Activity).requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // 画中画 - 按 Home 自动进入
    LaunchedEffect(Unit) {
        (context as Activity).setPictureInPictureParams(
            PictureInPictureParams.Builder().setAutoEnterEnabled(true).build()
        )
    }

    // 检测是否处于画中画模式
    var isInPiP by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_PAUSE) {
                isInPiP = (context as Activity).isInPictureInPictureMode
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 视频画面
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = viewModel.player
                    useController = false
                }
            },
            update = { it.player = viewModel.player },
            modifier = Modifier.fillMaxSize()
        )

        // 缓冲指示器
        if (viewModel.isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
                strokeWidth = 2.dp
            )
        }

        // 画中画模式下隐藏所有控件
        if (!isInPiP) {
            GestureOverlay(
                onToggleControls   = viewModel::toggleControls,
                onTogglePlayPause  = viewModel::togglePlayPause,
                onSeekBy           = viewModel::seekBy,
                onVolumeChanged    = viewModel::updateVolume,
                onBrightnessChanged = viewModel::updateBrightness,
                modifier           = Modifier.fillMaxSize()
            )

            AnimatedVisibility(
                visible = viewModel.controlsVisible,
                enter   = fadeIn(),
                exit    = fadeOut()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {

                    // ── 顶栏 ──────────────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 圆形返回按钮（毛玻璃胶囊）
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.4f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                .clickable(onClick = onBack),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "正在播放",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp,
                                letterSpacing = 0.3.sp
                            )
                            Text(
                                text = viewModel.videoTitle,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }

                        // 画中画按钮
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.4f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                .clickable {
                                    (context as Activity).enterPictureInPictureMode(
                                        PictureInPictureParams.Builder()
                                            .setAspectRatio(Rational(16, 9))
                                            .setAutoEnterEnabled(true)
                                            .build()
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PictureInPicture,
                                contentDescription = "画中画",
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    // ── 底部区域 ──────────────────────────────────────────────
                    // ── 底部控制栏（单层玻璃容器，三列布局）────────────────────
                    val speedText = when (viewModel.playbackSpeed) {
                        0.5f  -> "0.5×"
                        1.0f  -> "1.0×"
                        1.25f -> "1.25×"
                        1.5f  -> "1.5×"
                        2.0f  -> "2.0×"
                        else  -> "${viewModel.playbackSpeed}×"
                    }
                    val enterPiP = {
                        (context as Activity).enterPictureInPictureMode(
                            PictureInPictureParams.Builder()
                                .setAspectRatio(Rational(16, 9))
                                .build()
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        LiquidGlassContainer(
                            modifier     = Modifier.fillMaxWidth(),
                            cornerRadius = 20.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // ── 左：播放控制按钮 ──────────────────────────────
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    // 上一个
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .clickable { viewModel.seekTo(0L) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.SkipPrevious, "上一个",
                                            tint = Color.White.copy(alpha = 0.55f),
                                            modifier = Modifier.size(18.dp))
                                    }
                                    // 快退 10s
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .clickable { viewModel.seekBy(-10_000L) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Replay10, "快退10秒",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp))
                                    }
                                    // 播放/暂停
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.15f))
                                            .border(0.5.dp, Color.White.copy(alpha = 0.30f), CircleShape)
                                            .clickable { viewModel.togglePlayPause() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (viewModel.isPlaying) Icons.Default.Pause
                                                          else Icons.Default.PlayArrow,
                                            contentDescription = if (viewModel.isPlaying) "暂停" else "播放",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    // 快进 30s
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .clickable { viewModel.seekBy(30_000L) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Forward30, "快进30秒",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp))
                                    }
                                    // 下一个
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                if (viewModel.duration > 0)
                                                    viewModel.seekTo(viewModel.duration - 500)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.SkipNext, "下一个",
                                            tint = Color.White.copy(alpha = 0.55f),
                                            modifier = Modifier.size(18.dp))
                                    }
                                }

                                Spacer(Modifier.width(10.dp))

                                // ── 中：进度条 + 时间 ─────────────────────────────
                                Column(modifier = Modifier.weight(1f)) {
                                    Slider(
                                        value = if (viewModel.duration > 0)
                                            viewModel.currentPosition.toFloat() / viewModel.duration
                                        else 0f,
                                        onValueChange = { frac ->
                                            if (viewModel.duration > 0)
                                                viewModel.seekTo((frac * viewModel.duration).toLong())
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = SliderDefaults.colors(
                                            thumbColor         = AccentWhite,
                                            activeTrackColor   = AccentWhite,
                                            inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                                        )
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(formatTime(viewModel.currentPosition),
                                            color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp)
                                        Text(formatTime(viewModel.duration),
                                            color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp)
                                    }
                                }

                                Spacer(Modifier.width(10.dp))

                                // ── 右：速度 + 画中画 ─────────────────────────────
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // 速度切换
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(CardBg)
                                            .border(0.5.dp, CardBorder, RoundedCornerShape(8.dp))
                                            .clickable { viewModel.cycleSpeed() }
                                            .padding(horizontal = 10.dp, vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(speedText,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold)
                                            Text("速度",
                                                color = Color.White.copy(alpha = 0.45f),
                                                fontSize = 9.sp)
                                        }
                                    }
                                    // 画中画
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(CardBg)
                                            .border(0.5.dp, CardBorder, CircleShape)
                                            .clickable { enterPiP() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.PictureInPicture, "画中画",
                                            tint = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

