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
import androidx.compose.ui.text.style.TextAlign
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 四个状态卡片
                        StatusCards(
                            volume     = viewModel.volumePercent,
                            brightness = viewModel.brightnessPercent,
                            speed      = viewModel.playbackSpeed,
                            onSpeedClick = viewModel::cycleSpeed,
                            onPiPClick   = {
                                (context as Activity).enterPictureInPictureMode(
                                    PictureInPictureParams.Builder()
                                        .setAspectRatio(Rational(16, 9))
                                        .build()
                                )
                            }
                        )

                        // 进度条 + 时间
                        LiquidGlassContainer(
                            modifier     = Modifier.fillMaxWidth(),
                            cornerRadius = 20.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                // 进度条（加粗 3dp）
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
                                        thumbColor          = AccentWhite,
                                        activeTrackColor    = AccentWhite,
                                        inactiveTrackColor  = Color.White.copy(alpha = 0.25f)
                                    )
                                )

                                // 时间
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text  = formatTime(viewModel.currentPosition),
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text  = formatTime(viewModel.duration),
                                        color = Color.White.copy(alpha = 0.45f),
                                        fontSize = 11.sp
                                    )
                                }

                                Spacer(Modifier.height(6.dp))

                                // 五键播放控制行
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 上一个
                                    IconButton(onClick = { viewModel.seekTo(0L) }) {
                                        Icon(
                                            Icons.Default.SkipPrevious,
                                            contentDescription = "上一个",
                                            tint  = Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    // 快退 10s
                                    IconButton(onClick = { viewModel.seekBy(-10_000L) }) {
                                        Icon(
                                            Icons.Default.Replay10,
                                            contentDescription = "快退10秒",
                                            tint  = Color.White,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    // 播放/暂停大按钮
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.15f))
                                            .border(0.5.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                                            .clickable { viewModel.togglePlayPause() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (viewModel.isPlaying)
                                                Icons.Default.Pause
                                            else
                                                Icons.Default.PlayArrow,
                                            contentDescription = if (viewModel.isPlaying) "暂停" else "播放",
                                            tint  = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    // 快进 30s
                                    IconButton(onClick = { viewModel.seekBy(30_000L) }) {
                                        Icon(
                                            Icons.Default.Forward30,
                                            contentDescription = "快进30秒",
                                            tint  = Color.White,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    // 下一个（暂跳到结尾）
                                    IconButton(onClick = {
                                        if (viewModel.duration > 0) viewModel.seekTo(viewModel.duration - 500)
                                    }) {
                                        Icon(
                                            Icons.Default.SkipNext,
                                            contentDescription = "下一个",
                                            tint  = Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(24.dp)
                                        )
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

// ── 四个状态卡片 ──────────────────────────────────────────────────────────────

@Composable
private fun StatusCards(
    volume: Int,
    brightness: Int,
    speed: Float,
    onSpeedClick: () -> Unit,
    onPiPClick: () -> Unit
) {
    val speedText = when (speed) {
        0.5f  -> "0.5×"
        1.0f  -> "1.0×"
        1.25f -> "1.25×"
        1.5f  -> "1.5×"
        2.0f  -> "2.0×"
        else  -> "${speed}×"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusCard(modifier = Modifier.fillMaxWidth(), label = "音量",  value = "${volume}%")
            StatusCard(modifier = Modifier.fillMaxWidth(), label = "亮度",  value = "${brightness}%")
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusCard(
                modifier  = Modifier.fillMaxWidth(),
                label     = "速度",
                value     = speedText,
                clickable = true,
                onClick   = onSpeedClick
            )
            StatusCard(
                modifier  = Modifier.fillMaxWidth(),
                label     = "画中画",
                value     = "进入",
                clickable = true,
                onClick   = onPiPClick
            )
        }
    }
}

@Composable
private fun StatusCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    clickable: Boolean = false,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .border(0.5.dp, CardBorder, RoundedCornerShape(12.dp))
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text      = value,
                color     = Color.White,
                fontSize  = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign  = TextAlign.Center
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text     = label,
                color    = Color.White.copy(alpha = 0.45f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
