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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
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
import android.content.res.Configuration
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
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
            (context as Activity).requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    // 画中画 - 按 Home 自动进入
    LaunchedEffect(Unit) {
        (context as Activity).setPictureInPictureParams(
            PictureInPictureParams.Builder().setAutoEnterEnabled(true).build()
        )
    }

    // 锁定状态
    var isLocked by remember { mutableStateOf(false) }

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

    val isLandscapeMode = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── 视频画面 ───────────────────────────────────────────────────────────
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

        // ── 缓冲指示器 ─────────────────────────────────────────────────────────
        if (viewModel.isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
                strokeWidth = 2.dp
            )
        }

        // ── 画中画模式下隐藏所有控件 ───────────────────────────────────────────
        if (!isInPiP) {
            GestureOverlay(
                onToggleControls    = viewModel::toggleControls,
                onTogglePlayPause   = viewModel::togglePlayPause,
                onSeekBy            = viewModel::seekBy,
                onVolumeChanged     = viewModel::updateVolume,
                onBrightnessChanged = viewModel::updateBrightness,
                enabled             = !isLocked,
                modifier            = Modifier.fillMaxSize()
            )

            // ── 锁定按钮（锁定中或控制栏可见时显示）──────────────────────────
            AnimatedVisibility(
                visible = viewModel.controlsVisible || isLocked,
                enter   = fadeIn(),
                exit    = fadeOut()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 20.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = if (isLocked) 0.40f else 0.25f))
                            .border(0.5.dp, Color.White.copy(alpha = if (isLocked) 0.35f else 0.18f), CircleShape)
                            .clickable { isLocked = !isLocked },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = if (isLocked) "解锁" else "锁定",
                            tint = Color.White.copy(alpha = if (isLocked) 1f else 0.70f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // ── 主控制层（锁定时隐藏）─────────────────────────────────────────
            AnimatedVisibility(
                visible = viewModel.controlsVisible && !isLocked,
                enter   = fadeIn(),
                exit    = fadeOut()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {

                    // ── 顶部渐变遮罩 ───────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isLandscapeMode) 90.dp else 130.dp)
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent)
                                )
                            )
                    )

                    // ── 底部渐变遮罩 ───────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isLandscapeMode) 120.dp else 180.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                                )
                            )
                    )

                    // ── 顶部栏 ─────────────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(
                                horizontal = 16.dp,
                                vertical   = if (isLandscapeMode) 10.dp else 14.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 返回按钮
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.28f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.18f), CircleShape)
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

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "正在播放",
                                color = Color.White.copy(alpha = 0.45f),
                                fontSize = 10.sp,
                                letterSpacing = 0.4.sp
                            )
                            Text(
                                text = viewModel.videoTitle,
                                color = Color.White.copy(alpha = 0.92f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        // 画中画按钮
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.28f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.18f), CircleShape)
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

                    // ── 中央播放控制 ────────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(bottom = if (isLandscapeMode) 0.dp else 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(
                            if (isLandscapeMode) 14.dp else 22.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 跳到开头
                        IconButton(
                            onClick  = { viewModel.seekTo(0L) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.SkipPrevious, "跳到开头",
                                tint = Color.White.copy(alpha = 0.50f),
                                modifier = Modifier.size(26.dp))
                        }
                        // 快退 10s
                        IconButton(
                            onClick  = { viewModel.seekBy(-10_000L) },
                            modifier = Modifier.size(46.dp)
                        ) {
                            Icon(Icons.Default.Replay10, "快退10秒",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp))
                        }
                        // 播放 / 暂停（主按钮）
                        Box(
                            modifier = Modifier
                                .size(if (isLandscapeMode) 56.dp else 64.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.18f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                                .clickable { viewModel.togglePlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (viewModel.isPlaying) Icons.Default.Pause
                                              else Icons.Default.PlayArrow,
                                contentDescription = if (viewModel.isPlaying) "暂停" else "播放",
                                tint = Color.White,
                                modifier = Modifier.size(if (isLandscapeMode) 28.dp else 32.dp)
                            )
                        }
                        // 快进 30s
                        IconButton(
                            onClick  = { viewModel.seekBy(30_000L) },
                            modifier = Modifier.size(46.dp)
                        ) {
                            Icon(Icons.Default.Forward30, "快进30秒",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp))
                        }
                        // 跳到末尾
                        IconButton(
                            onClick  = {
                                if (viewModel.duration > 0)
                                    viewModel.seekTo(viewModel.duration - 500)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.SkipNext, "跳到末尾",
                                tint = Color.White.copy(alpha = 0.50f),
                                modifier = Modifier.size(26.dp))
                        }
                    }

                    // ── 底部：进度条 + 时间 + 速度 ────────────────────────────
                    val speedText = when (viewModel.playbackSpeed) {
                        0.5f  -> "0.5×"
                        1.0f  -> "1×"
                        1.25f -> "1.25×"
                        1.5f  -> "1.5×"
                        2.0f  -> "2×"
                        else  -> "${viewModel.playbackSpeed}×"
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(
                                horizontal = 20.dp,
                                vertical   = if (isLandscapeMode) 6.dp else 18.dp
                            ),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        // 进度条
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
                                thumbColor         = Color.White,
                                activeTrackColor   = Color.White,
                                inactiveTrackColor = Color.White.copy(alpha = 0.28f)
                            )
                        )
                        // 时间 + 速度切换
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${formatTime(viewModel.currentPosition)} / ${formatTime(viewModel.duration)}",
                                color = Color.White.copy(alpha = 0.72f),
                                fontSize = 11.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.10f))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                                    .clickable { viewModel.cycleSpeed() }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = speedText,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

