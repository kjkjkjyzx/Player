package com.example.player.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.util.Rational
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import com.example.player.ui.theme.AppSpring
import com.example.player.ui.theme.GlassDefaults
import com.example.player.ui.transitions.LocalLandscapeExiting
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
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.collectAsState
import android.content.res.Configuration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.ui.PlayerView
import com.example.player.R
import com.example.player.ui.components.GestureOverlay
import com.example.player.ui.transitions.LocalAnimatedContentScope
import com.example.player.ui.transitions.LocalSharedTransitionScope
import com.example.player.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay

private val AccentWhite = Color.White
private val CardBg      = Color(0x59000000)   // 35% black
private val CardBorder  = Color(0x1AFFFFFF)   // 10% white

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PlayerScreen(
    videoUri: Uri,
    isLandscapeSource: Boolean = false,   // 来源是否为横屏视频，true 时禁用 SharedBounds
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(videoUri) { viewModel.initializePlayer(videoUri) }

    // ── 沉浸式全屏：立即设置 Window Flag，延迟 180ms 再隐藏系统栏 ──────────────
    // 把 hide(systemBars) 推迟到 sharedBounds 容器展开到全屏后，
    // 避免两个动画（容器展开 + 系统栏收起）叠加产生视觉混乱。
    DisposableEffect(Unit) {
        val window = (context as Activity).window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val ctrl = WindowInsetsControllerCompat(window, window.decorView)
        ctrl.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            ctrl.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    LaunchedEffect(Unit) {
        delay(180)   // 等 sharedBounds 展开到全屏后再缩回系统栏
        val ctrl = WindowInsetsControllerCompat(
            (context as Activity).window,
            (context as Activity).window.decorView
        )
        ctrl.hide(WindowInsetsCompat.Type.systemBars())
    }

    DisposableEffect(Unit) { onDispose { viewModel.onPause() } }

    // 自动旋转
    val isLandscape    = viewModel.isLandscapeVideo
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

    // 画中画 - 按 Home 自动进入；Rational 随视频尺寸更新，避免拉伸
    LaunchedEffect(viewModel.videoWidth, viewModel.videoHeight) {
        val activity = context as Activity
        val w = viewModel.videoWidth
        val h = viewModel.videoHeight
        val builder = PictureInPictureParams.Builder().setAutoEnterEnabled(true)
        if (w > 0 && h > 0) builder.setAspectRatio(clampPipRatio(w, h))
        activity.setPictureInPictureParams(builder.build())
    }

    // 锁定状态
    var isLocked by remember { mutableStateOf(false) }

    // iOS 26 spring stagger：三层控件独立 visible state，由 LaunchedEffect 在真正的弹簧时间轴上错开
    var visTop    by remember { mutableStateOf(false) }
    var visCenter by remember { mutableStateOf(false) }
    var visBottom by remember { mutableStateOf(false) }
    val wantControlsVisible = viewModel.controlsVisible && !isLocked && videoSizeKnown
    LaunchedEffect(wantControlsVisible) {
        if (wantControlsVisible) {
            // 入场：顶部 → 中央 → 底部，各错开 55ms（向下级联）
            visTop = true;     delay(55)
            visCenter = true;  delay(55)
            visBottom = true
        } else {
            // 退场：反向错开——底部 → 中央 → 顶部，各错开 40ms（向上收拢）
            visBottom = false;  delay(40)
            visCenter = false;  delay(40)
            visTop = false
        }
    }

    // 锁定图标颜色：弹簧过渡（锁定=纯白，解锁=70%白）
    val lockTint by animateColorAsState(
        targetValue   = if (isLocked) Color.White else Color.White.copy(alpha = 0.70f),
        animationSpec = AppSpring.standard(),
        label         = "lockTint"
    )

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
    val connectionError by viewModel.connectionError.collectAsState()

    // 返回路径由 MainActivity.navigateBackFromPlayer 同一帧内把方向切回 PORTRAIT 并 popBackStack，
    // 旋转动画与退场淡出同步进行（对称于进入时的 navigateToPlayer），不再先转屏再退出。
    val handleBack: () -> Unit = onBack

    // 拦截系统返回手势 / 物理返回键，确保与返回按钮行为一致
    BackHandler(onBack = handleBack)

    // ── SharedBounds：根 Box ↔ VideoCard 根 Box 做容器变换动画 ────────────────
    val sharedScope   = LocalSharedTransitionScope.current
    val animatedScope = LocalAnimatedContentScope.current
    // 进入横屏视频时禁用 SharedBounds（坐标系翻转会破坏动画）；
    // 退出横屏视频时由 landscapeExiting=true 临时开启，此时已旋转回竖屏，坐标系稳定。
    val landscapeExiting = LocalLandscapeExiting.current
    val sharedModifier: Modifier = if (sharedScope != null && animatedScope != null &&
        (!isLandscapeSource || landscapeExiting)) {
        with(sharedScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = "player-container-${videoUri}"),
                animatedVisibilityScope = animatedScope,
                // 延迟 160ms 再淡入黑色背景，让 sharedBounds 容器展开到位后背景才覆盖，
                // 消除"缩略图未展开就被黑屏盖住"的视觉撕裂
                enter = fadeIn(tween(durationMillis = 220, delayMillis = 160)),
                exit  = fadeOut(AppSpring.gentle()),   // 柔和退出
                // PlayerScreen 侧以直角（0dp）裁剪，对应卡片侧的 10dp 圆角
                clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(0.dp))
            )
        }
    } else Modifier

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(sharedModifier)   // sharedBounds 放在 background 之前
            .background(Color.Black)
    ) {
        // ── 视频画面 ───────────────────────────────────────────────────────────
        val currentPlayer = viewModel.player
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = currentPlayer
                    useController = false
                }
            },
            update = { it.player = currentPlayer },
            modifier = Modifier.fillMaxSize()
        )

        // ── 缓冲指示器 ─────────────────────────────────────────────────────────
        if ((viewModel.isBuffering || currentPlayer == null) && !connectionError) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
                strokeWidth = 2.dp
            )
        }

        if (connectionError) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.player_connection_error),
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp
                )
                TextButton(onClick = {
                    viewModel.clearConnectionError()
                    viewModel.retryConnection()
                }) {
                    Text(text = stringResource(R.string.action_retry), color = Color.White)
                }
            }
        }

        // ── 画中画模式下隐藏所有控件；方向未知时只显示黑屏+缓冲圈 ────────────
        if (videoSizeKnown && !isInPiP) {
            GestureOverlay(
                onToggleControls    = viewModel::toggleControls,
                onTogglePlayPause   = viewModel::togglePlayPause,
                onSeekBy            = viewModel::seekBy,
                onVolumeChanged     = viewModel::updateVolume,
                onBrightnessChanged = viewModel::updateBrightness,
                enabled             = !isLocked,
                modifier            = Modifier.fillMaxSize()
            )

            // ── 锁定按钮（锁定中或控制栏可见时显示）——spring 弹性进出 ──────────
            AnimatedVisibility(
                visible = viewModel.controlsVisible || isLocked,
                enter   = fadeIn(AppSpring.standard()) + scaleIn(AppSpring.press(), initialScale = 0.72f),
                exit    = fadeOut(AppSpring.gentle())  + scaleOut(AppSpring.gentle(), targetScale = 0.72f)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 20.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = if (isLocked) 0.40f else 0.25f))
                            .border(GlassDefaults.borderWidth, GlassDefaults.borderBrush, CircleShape)
                            .clickable { isLocked = !isLocked },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = if (isLocked) stringResource(R.string.cd_unlock) else stringResource(R.string.cd_lock),
                            tint     = lockTint,   // 弹簧颜色过渡
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // ── 顶部控件层（stagger 第 1 层，0ms）——spring 弹性进出 ────────────
            AnimatedVisibility(
                visible  = visTop,
                enter    = fadeIn(AppSpring.standard()) + slideInVertically(AppSpring.standard()) { -40 },
                exit     = fadeOut(AppSpring.gentle())  + slideOutVertically(AppSpring.gentle())  { -40 },
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // 顶部渐变遮罩（在顶部栏之下）
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isLandscapeMode) 90.dp else 130.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent)
                                )
                            )
                    )
                    // 顶部栏（在渐变之上）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
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
                                .border(GlassDefaults.borderWidth, GlassDefaults.borderBrush, CircleShape)
                                .clickable(onClick = handleBack),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                                tint     = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text          = stringResource(R.string.player_now_playing),
                                color         = Color.White.copy(alpha = 0.45f),
                                fontSize      = 10.sp,
                                letterSpacing = 0.4.sp
                            )
                            Text(
                                text       = viewModel.videoTitle,
                                color      = Color.White.copy(alpha = 0.92f),
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines   = 1,
                                overflow   = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        // 画中画按钮
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.28f))
                                .border(GlassDefaults.borderWidth, GlassDefaults.borderBrush, CircleShape)
                                .clickable {
                                    val w = viewModel.videoWidth
                                    val h = viewModel.videoHeight
                                    val ratio = if (w > 0 && h > 0) clampPipRatio(w, h) else Rational(16, 9)
                                    (context as Activity).enterPictureInPictureMode(
                                        PictureInPictureParams.Builder()
                                            .setAspectRatio(ratio)
                                            .setAutoEnterEnabled(true)
                                            .build()
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PictureInPicture,
                                contentDescription = stringResource(R.string.cd_pip),
                                tint     = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }

            // ── 中央控件层（stagger 第 2 层，55ms 后）——scale 弹出感 ────────────
            AnimatedVisibility(
                visible  = visCenter,
                enter    = fadeIn(AppSpring.standard()) + scaleIn(AppSpring.standard(), initialScale = 0.82f),
                exit     = fadeOut(AppSpring.gentle())  + scaleOut(AppSpring.gentle(),  targetScale  = 0.82f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = if (isLandscapeMode) 0.dp else 12.dp)
            ) {
                Row(
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
                        Icon(
                            Icons.Default.SkipPrevious,
                            stringResource(R.string.cd_skip_to_start),
                            tint     = Color.White.copy(alpha = 0.50f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    // 快退 10s
                    IconButton(
                        onClick  = { viewModel.seekBy(-10_000L) },
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(
                            Icons.Default.Replay10,
                            stringResource(R.string.cd_replay_10),
                            tint     = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    // 播放 / 暂停（主按钮）——AnimatedContent 图标变形（iOS SF Symbol 效果）
                    Box(
                        modifier = Modifier
                            .size(if (isLandscapeMode) 56.dp else 64.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f))
                            .border(GlassDefaults.borderWidth, GlassDefaults.borderBrush, CircleShape)
                            .clickable { viewModel.togglePlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState  = viewModel.isPlaying,
                            transitionSpec = {
                                // 新图标从小弹出，旧图标向外爆炸消失
                                (scaleIn(AppSpring.press(), initialScale = 0.50f) + fadeIn(AppSpring.press())) togetherWith
                                (scaleOut(AppSpring.press(), targetScale = 1.50f) + fadeOut(AppSpring.press()))
                            },
                            label = "PlayPauseIcon"
                        ) { playing ->
                            Icon(
                                imageVector        = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playing) stringResource(R.string.cd_pause) else stringResource(R.string.cd_play),
                                tint     = Color.White,
                                modifier = Modifier.size(if (isLandscapeMode) 28.dp else 32.dp)
                            )
                        }
                    }
                    // 快进 30s
                    IconButton(
                        onClick  = { viewModel.seekBy(30_000L) },
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(
                            Icons.Default.Forward30,
                            stringResource(R.string.cd_forward_30),
                            tint     = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    // 跳到末尾
                    IconButton(
                        onClick  = {
                            if (viewModel.duration > 0) viewModel.seekTo(viewModel.duration - 500)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.SkipNext,
                            stringResource(R.string.cd_skip_to_end),
                            tint     = Color.White.copy(alpha = 0.50f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // ── 底部控件层（stagger 第 3 层，110ms 后）——spring 上滑入场 ─────────
            AnimatedVisibility(
                visible  = visBottom,
                enter    = fadeIn(AppSpring.standard()) + slideInVertically(AppSpring.standard()) { 40 },
                exit     = fadeOut(AppSpring.gentle())  + slideOutVertically(AppSpring.gentle())  { 40 },
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // 底部渐变遮罩
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isLandscapeMode) 120.dp else 180.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                                )
                            )
                    )
                    // 进度条 + 时间 + 速度（在渐变之上）
                    // 独立 Composable，使高频 currentPosition 重组只波及此函数
                    PlayerProgressSection(
                        viewModel       = viewModel,
                        isLandscapeMode = isLandscapeMode
                    )
                }
            }
        }
    }
}

// ── 独立进度区 Composable ─────────────────────────────────────────────────────
// 将 currentPosition（每 200ms 更新一次）的读取限制在此函数内，
// 避免父级 PlayerScreen 因高频状态变化而产生级联重组。

/**
 * 将视频像素宽高换算成画中画可接受的 Rational。
 * Android 的 PiP 限制：0.41 <= w/h <= 2.39，超出会抛异常。
 * 同时把分数约简到 short 范围内，避免巨大分辨率溢出。
 */
private fun clampPipRatio(w: Int, h: Int): Rational {
    val min = 0.4184f   // 略高于 0.41 门槛
    val max = 2.3898f
    val raw = w.toFloat() / h.toFloat()
    val clamped = raw.coerceIn(min, max)
    // 用 1000 做分母，给足精度又不越界
    return Rational((clamped * 1000f).toInt().coerceAtLeast(1), 1000)
}

@Composable
private fun PlayerProgressSection(
    viewModel: PlayerViewModel,
    isLandscapeMode: Boolean,
    modifier: Modifier = Modifier
) {
    val speedText = when (viewModel.playbackSpeed) {
        0.5f  -> "0.5×"
        1.0f  -> "1×"
        1.25f -> "1.25×"
        1.5f  -> "1.5×"
        2.0f  -> "2×"
        else  -> "${viewModel.playbackSpeed}×"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
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

        // 时间文本 + 速度切换胶囊
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text     = "${formatTime(viewModel.currentPosition)} / ${formatTime(viewModel.duration)}",
                color    = Color.White.copy(alpha = 0.72f),
                fontSize = 11.sp
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .border(GlassDefaults.borderWidth, GlassDefaults.borderBrush, RoundedCornerShape(6.dp))
                    .clickable { viewModel.cycleSpeed() }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = speedText,
                    color      = Color.White,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
